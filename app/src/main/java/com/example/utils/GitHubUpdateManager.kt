package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class GitHubReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val updatedAt: String
)

data class GitHubReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val publishedAt: String,
    val htmlUrl: String,
    val selectedApk: GitHubReleaseAsset
)

sealed class GitHubUpdateState {
    data object Idle : GitHubUpdateState()
    data object Checking : GitHubUpdateState()
    data class Downloading(
        val versionTag: String,
        val fileName: String,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : GitHubUpdateState()

    data class ReadyToInstall(
        val versionTag: String,
        val fileName: String,
        val apkFile: File,
        val releaseNotes: String
    ) : GitHubUpdateState()

    data class PermissionRequired(
        val versionTag: String,
        val fileName: String,
        val apkFile: File,
        val releaseNotes: String
    ) : GitHubUpdateState()

    data class UpToDate(
        val currentVersion: String,
        val latestVersion: String,
        val releaseInfo: GitHubReleaseInfo
    ) : GitHubUpdateState()

    data class Error(val message: String) : GitHubUpdateState()
}

/**
 * In-app GitHub Releases updater targeting https://github.com/suvadippatra/tracker.
 *
 * Fetches compiled release assets from GitHub's public Releases API, automatically picks the
 * latest compiled compatible APK (preferring the device ABI -> universal -> newest updated_at),
 * immediately downloads it with live progress, and hands it off to Android's package installer
 * via FileProvider.
 */
object GitHubUpdateManager {
    private const val REPO_OWNER = "suvadippatra"
    private const val REPO_NAME = "tracker"
    private const val RELEASES_API_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases?per_page=15"
    private const val LATEST_RELEASE_API_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    private const val USER_AGENT = "Traker-Android-Updater/${BuildConfig.VERSION_NAME}"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    private val _state = MutableStateFlow<GitHubUpdateState>(GitHubUpdateState.Idle)
    val state: StateFlow<GitHubUpdateState> = _state.asStateFlow()

    fun checkAndDownloadLatest(
        context: Context,
        forceDownloadEvenIfUpToDate: Boolean = false
    ) {
        val appContext = context.applicationContext
        activeJob?.cancel()
        activeJob = scope.launch {
            _state.value = GitHubUpdateState.Checking
            try {
                val release = fetchLatestCompiledRelease()
                if (release == null) {
                    _state.value = GitHubUpdateState.Error(
                        "No compiled APK release found on GitHub ($REPO_OWNER/$REPO_NAME)."
                    )
                    return@launch
                }

                val currentVersion = BuildConfig.VERSION_NAME
                val isNewer = isRemoteVersionNewer(currentVersion, release.tagName)

                if (!isNewer && !forceDownloadEvenIfUpToDate) {
                    _state.value = GitHubUpdateState.UpToDate(
                        currentVersion = currentVersion,
                        latestVersion = release.tagName,
                        releaseInfo = release
                    )
                    return@launch
                }

                downloadAndTriggerInstall(appContext, release)
            } catch (ce: CancellationException) {
                _state.value = GitHubUpdateState.Idle
                throw ce
            } catch (e: Exception) {
                val msg = e.message?.takeIf { it.isNotBlank() }
                    ?: "Could not reach GitHub. Check your internet connection."
                _state.value = GitHubUpdateState.Error(msg)
            }
        }
    }

    fun downloadSpecificRelease(context: Context, release: GitHubReleaseInfo) {
        val appContext = context.applicationContext
        activeJob?.cancel()
        activeJob = scope.launch {
            try {
                downloadAndTriggerInstall(appContext, release)
            } catch (ce: CancellationException) {
                _state.value = GitHubUpdateState.Idle
                throw ce
            } catch (e: Exception) {
                _state.value = GitHubUpdateState.Error(
                    e.message?.takeIf { it.isNotBlank() } ?: "APK download failed."
                )
            }
        }
    }

    fun cancelDownload() {
        activeJob?.cancel()
        activeJob = null
        _state.value = GitHubUpdateState.Idle
    }

    fun resetState() {
        activeJob?.cancel()
        activeJob = null
        _state.value = GitHubUpdateState.Idle
    }

    fun retryInstall(context: Context): Boolean {
        val current = _state.value
        val (versionTag, fileName, apkFile, notes) = when (current) {
            is GitHubUpdateState.ReadyToInstall -> Quad(current.versionTag, current.fileName, current.apkFile, current.releaseNotes)
            is GitHubUpdateState.PermissionRequired -> Quad(current.versionTag, current.fileName, current.apkFile, current.releaseNotes)
            else -> return false
        }
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            _state.value = GitHubUpdateState.Error("Downloaded APK file is missing. Please download again.")
            return false
        }
        return triggerSystemInstaller(context, versionTag, fileName, apkFile, notes)
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    private suspend fun downloadAndTriggerInstall(
        appContext: Context,
        release: GitHubReleaseInfo
    ) {
        val asset = release.selectedApk
        val updatesDir = File(appContext.cacheDir, "updates").apply { mkdirs() }
        // Clean previous cached APKs to save storage
        updatesDir.listFiles()?.forEach { old ->
            runCatching { old.delete() }
        }

        val safeFileName = asset.name
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .let { if (it.endsWith(".apk", ignoreCase = true)) it else "$it.apk" }
        val tempFile = File(updatesDir, "$safeFileName.part")
        val finalFile = File(updatesDir, safeFileName)

        _state.value = GitHubUpdateState.Downloading(
            versionTag = release.tagName,
            fileName = asset.name,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = asset.sizeBytes.coerceAtLeast(0L)
        )

        val conn = openConnectionFollowingRedirects(asset.downloadUrl, accept = "application/octet-stream")
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("GitHub download failed (HTTP $code).")
            }
            val contentLength = conn.contentLengthLong.takeIf { it > 0L } ?: asset.sizeBytes
            var downloaded = 0L
            var lastEmitTime = 0L

            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                            throw CancellationException("Download cancelled")
                        }
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime >= 120L) {
                            lastEmitTime = now
                            val pct = if (contentLength > 0L) {
                                (downloaded.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            _state.value = GitHubUpdateState.Downloading(
                                versionTag = release.tagName,
                                fileName = asset.name,
                                progress = pct,
                                downloadedBytes = downloaded,
                                totalBytes = contentLength
                            )
                        }
                    }
                    output.flush()
                }
            }

            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            triggerSystemInstaller(
                context = appContext,
                versionTag = release.tagName,
                fileName = asset.name,
                apkFile = finalFile,
                releaseNotes = release.releaseNotes
            )
        } catch (e: Exception) {
            runCatching { tempFile.delete() }
            throw e
        } finally {
            conn.disconnect()
        }
    }

    private fun triggerSystemInstaller(
        context: Context,
        versionTag: String,
        fileName: String,
        apkFile: File,
        releaseNotes: String
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            _state.value = GitHubUpdateState.PermissionRequired(
                versionTag = versionTag,
                fileName = fileName,
                apkFile = apkFile,
                releaseNotes = releaseNotes
            )
            return false
        }

        _state.value = GitHubUpdateState.ReadyToInstall(
            versionTag = versionTag,
            fileName = fileName,
            apkFile = apkFile,
            releaseNotes = releaseNotes
        )

        return runCatching {
            val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        }.onFailure { err ->
            _state.value = GitHubUpdateState.Error(
                "APK downloaded, but installer could not start: ${err.message ?: "Unknown error"}"
            )
        }.getOrDefault(false)
    }

    private fun fetchLatestCompiledRelease(): GitHubReleaseInfo? {
        val candidates = mutableListOf<GitHubReleaseInfo>()

        val listJson = httpGetJson(RELEASES_API_URL)
        if (listJson != null) {
            val arr = JSONArray(listJson)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                parseReleaseObject(obj)?.let { candidates.add(it) }
            }
        }

        if (candidates.isEmpty()) {
            val singleJson = httpGetJson(LATEST_RELEASE_API_URL)
            if (singleJson != null) {
                parseReleaseObject(JSONObject(singleJson))?.let { candidates.add(it) }
            }
        }

        if (candidates.isEmpty()) return null

        // Choose the release containing the latest compiled APK asset (updatedAt ISO-8601 descending)
        return candidates.maxWithOrNull(
            compareBy<GitHubReleaseInfo>(
                { it.selectedApk.updatedAt },
                { it.publishedAt }
            )
        )
    }

    private fun parseReleaseObject(obj: JSONObject): GitHubReleaseInfo? {
        if (obj.optBoolean("draft", false)) return null
        val assetsArr = obj.optJSONArray("assets") ?: return null
        val apkAssets = mutableListOf<GitHubReleaseAsset>()
        for (j in 0 until assetsArr.length()) {
            val a = assetsArr.optJSONObject(j) ?: continue
            val name = a.optString("name", "")
            val url = a.optString("browser_download_url", "")
            if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                apkAssets.add(
                    GitHubReleaseAsset(
                        name = name,
                        downloadUrl = url,
                        sizeBytes = a.optLong("size", 0L),
                        updatedAt = a.optString("updated_at", obj.optString("published_at", ""))
                    )
                )
            }
        }
        if (apkAssets.isEmpty()) return null

        val bestApk = selectBestCompiledApk(apkAssets)
        return GitHubReleaseInfo(
            tagName = obj.optString("tag_name", "").ifBlank { obj.optString("name", "Latest") },
            releaseName = obj.optString("name", "").ifBlank { obj.optString("tag_name", "Latest Release") },
            releaseNotes = obj.optString("body", "").trim(),
            publishedAt = obj.optString("published_at", ""),
            htmlUrl = obj.optString("html_url", "https://github.com/$REPO_OWNER/$REPO_NAME/releases"),
            selectedApk = bestApk
        )
    }

    /**
     * Picks the latest compiled APK asset compatible with this device's CPU ABI.
     * Priority:
     * 1. Exact device ABI match (arm64-v8a, armeabi-v7a, x86_64, x86) with newest updatedAt
     * 2. Universal / non-ABI-restricted release APK with newest updatedAt
     * 3. Newest compiled APK overall
     */
    internal fun selectBestCompiledApk(
        assets: List<GitHubReleaseAsset>,
        supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList()
    ): GitHubReleaseAsset {
        val sortedByNewest = assets.sortedByDescending { it.updatedAt }
        val knownAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

        // Prefer matching device ABI, ordered by newest compiled timestamp first
        for (abi in supportedAbis) {
            val match = sortedByNewest.firstOrNull {
                it.name.contains(abi, ignoreCase = true)
            }
            if (match != null) return match
        }

        // Next prefer universal / generic release APKs (no conflicting ABI tag)
        val universal = sortedByNewest.firstOrNull { asset ->
            val lower = asset.name.lowercase(Locale.ROOT)
            lower.contains("universal") || knownAbis.none { lower.contains(it) }
        }
        return universal ?: sortedByNewest.first()
    }

    internal fun isRemoteVersionNewer(currentVersion: String, remoteTag: String): Boolean {
        val curr = parseSemverParts(currentVersion)
        val rem = parseSemverParts(remoteTag)
        val maxLen = maxOf(curr.size, rem.size)
        for (i in 0 until maxLen) {
            val c = curr.getOrElse(i) { 0 }
            val r = rem.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    private fun parseSemverParts(raw: String): List<Int> {
        val cleaned = raw.trim().removePrefix("v").removePrefix("V")
        return Regex("""\d+""")
            .findAll(cleaned)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()
            .ifEmpty { listOf(0) }
    }

    private fun httpGetJson(urlStr: String): String? {
        val conn = openConnectionFollowingRedirects(urlStr, accept = "application/vnd.github+json")
        return try {
            val code = conn.responseCode
            when {
                code in 200..299 -> conn.inputStream.bufferedReader().use { it.readText() }
                code == 403 -> throw IllegalStateException("GitHub API rate limit reached. Try again in a few minutes.")
                code == 404 -> null
                else -> throw IllegalStateException("GitHub returned HTTP $code.")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun openConnectionFollowingRedirects(urlStr: String, accept: String): HttpURLConnection {
        var currentUrl = urlStr
        var redirects = 0
        while (redirects < 5) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 25_000
                instanceFollowRedirects = false
                setRequestProperty("Accept", accept)
                setRequestProperty("User-Agent", USER_AGENT)
            }
            val code = conn.responseCode
            if (code in 301..308) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("Redirect missing target URL.")
                }
                currentUrl = location
                redirects++
            } else {
                return conn
            }
        }
        throw IllegalStateException("Too many redirects while contacting GitHub.")
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
