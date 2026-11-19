package com.example.ui.screens.settings.subscreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.components.*
import com.example.ui.screens.LegalInfoDialog
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.viewmodel.FinancialViewModel
import com.example.utils.GitHubUpdateManager
import com.example.utils.GitHubUpdateState

private data class HelpGuideQuickItem(
    val icon: ImageVector,
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String
)

private data class PublicRepoReference(
    val name: String,
    val category: String,
    val license: String,
    val urlOrSource: String
)

@Composable
fun AboutSettingsScreen(
    viewModel: FinancialViewModel,
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile.language
    val isBn = prefLanguage == "bn"
    val appVersion = BuildConfig.VERSION_NAME.ifBlank { "0.0.1" }
    val isLight = isAppLightMode()
    val appAccent = AppAccentColor()
    val updateState by GitHubUpdateManager.state.collectAsStateWithLifecycle()

    var activeLegalDialog by remember { mutableStateOf<String?>(null) }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (activeLegalDialog != null) {
            activeLegalDialog = null
        } else {
            onBack()
        }
    }

    if (activeLegalDialog != null) {
        LegalInfoDialog(
            type = activeLegalDialog!!,
            prefLanguage = prefLanguage,
            appVersion = appVersion,
            onDismiss = { activeLegalDialog = null }
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // If user just granted "Install unknown apps" permission, resume APK installation automatically
    DisposableEffect(lifecycleOwner, updateState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && updateState is GitHubUpdateState.PermissionRequired) {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ||
                    context.packageManager.canRequestPackageInstalls()
                ) {
                    GitHubUpdateManager.retryInstall(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val quickHelpItems = remember {
        listOf(
            HelpGuideQuickItem(
                icon = Icons.Default.Restaurant,
                titleEn = "Hotel Khata & Smart OCR",
                titleBn = "হোটেল খাতা ও স্মার্ট OCR",
                descEn = "Log daily meals, scan printed/handwritten pages offline (with EXIF & contrast auto-fix), and export or share monthly payment status with receipts.",
                descBn = "প্রতিদিনের খাবার হিসাব, অফলাইন খাতা স্ক্যানার এবং রসিদসহ মাসিক পেমেন্ট স্ট্যাটাস শেয়ার করুন।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.AccountBalanceWallet,
                titleEn = "Extras, UPI Auto-Popup & Voice Memo",
                titleBn = "এক্সট্রা হিসাব, UPI পপআপ ও ভয়েস মেমো",
                descEn = "Compact transaction cards, direct history editing, opt-in UPI/SMS payment popup, monthly reminders, and offline Voice Memo + Speech-to-Text synthesis.",
                descBn = "কম্প্যাক্ট ট্রানজাকশন কার্ড, সরাসরি এডিট, UPI অটো-পপআপ, মাসিক রিমাইন্ডার এবং ভয়েস টু টেক্সট।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.ElectricMeter,
                titleEn = "Electricity Log, Graph & Partial Bill Carry-Over",
                titleBn = "বিদ্যুৎ রিডিং, গ্রাফ ও আংশিক বিল ক্যারি-ওভার",
                descEn = "Quick dial reading entry card placed right above the interactive daily kWh & multi-rate region graph, plus partial bill payment with target-month residual transfer.",
                descBn = "রিডিং এন্ট্রি কার্ডের ঠিক নিচেই দৈনিক ইউনিট ও মাল্টি-রেট গ্রাফ এবং আংশিক বিল পেমেন্টে বকেয়া স্থানান্তর।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.Share,
                titleEn = "ZIP Media Backup, 5-Round QR & Zero-Lag Sticky Notes",
                titleBn = "ZIP মিডিয়া ব্যাকআপ, QR ও জিরো-ল্যাগ স্টিকি নোটস",
                descEn = "Export/Import .zip archives carrying all attached images & audio, 5-round auto-stop QR stream, and background-prewarmed Sticky Notes with contiguous SpanStyle runs & drawWithCache vector pins.",
                descBn = "ছবি ও অডিওসহ .zip ব্যাকআপ, ৫-রাউন্ড অটো-স্টপ QR এবং ব্যাকগ্রাউন্ড প্রি-ওয়ার্মড দ্রুত স্টিকি নোটস।"
            )
        )
    }

    val publicReferences = remember {
        listOf(
            PublicRepoReference(
                name = "Jetpack Compose & Material 3",
                category = "Adaptive Mobile/Tablet UI, drawWithCache Vector Graphics & Contiguous SpanStyle Text Engine",
                license = "Apache 2.0",
                urlOrSource = "androidx.compose.material3"
            ),
            PublicRepoReference(
                name = "ZXing & CameraX Optical Scanner",
                category = "Lossless Matrix Reconstructor, Fountain QR Streaming & CameraX Viewfinder",
                license = "Apache 2.0",
                urlOrSource = "com.google.zxing:core / androidx.camera"
            ),
            PublicRepoReference(
                name = "Android SpeechRecognizer, MediaRecorder & NotificationListener",
                category = "Offline-Capable Voice Dictation, Audio Memo Synthesis & UPI Detection",
                license = "Apache 2.0",
                urlOrSource = "android.speech / android.service.notification"
            ),
            PublicRepoReference(
                name = "Room SQLite, WorkManager & Java ZIP Stream",
                category = "Local Database (v37), On-Device UPI/SMS Deduplication & .zip Media Bundler",
                license = "Apache 2.0",
                urlOrSource = "androidx.room / androidx.work / java.util.zip"
            ),
            PublicRepoReference(
                name = "ZXing, CameraX, ExifInterface & Coil",
                category = "5-Round QR Stream, Full-Res Capture, EXIF Auto-Rotate & 2MB JPEG Compression",
                license = "Apache 2.0",
                urlOrSource = "com.google.zxing / androidx.camera / coil-kt"
            )
        )
    }

    val aboutListState = rememberPersistedLazyListState("settings_about")

    LazyColumn(
        state = aboutListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 120.dp + com.example.ui.components.rememberSplitScreenSafeBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hero Card with Static Non-Editable App Version Badge (UNCHANGED)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = getCardBackground(),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    SecretDeleteModeManager.registerLogoTap()
                                },
                            shape = CircleShape,
                            color = colorResource(id = com.example.R.color.ic_launcher_background)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.app_logo),
                                    contentDescription = "App Logo",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Traker",
                            color = AppTextColor(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Static read-only version pill (non-editable)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = appAccent.copy(alpha = if (isLight) 0.12f else 0.2f),
                            modifier = Modifier.testTag("app_version_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (prefLanguage == "bn") {
                                        "সংস্করণ $appVersion • মেটেরিয়াল ৩"
                                    } else {
                                        "Version $appVersion • Material 3"
                                    },
                                    color = appAccent,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (prefLanguage == "bn") {
                                "অফলাইন-ফার্স্ট স্মার্ট আর্থিক, হোটেল খাতা ও বিদ্যুৎ বিল ব্যবস্থাপনা"
                            } else {
                                "Offline-first smart financial, hotel khata & electricity bill tracker"
                            },
                            color = AppSecondaryTextColor(),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AboutFeatureBadge(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.CloudOff,
                                label = if (prefLanguage == "bn") "১০০% অফলাইন" else "100% Offline",
                                accent = appAccent,
                                isLight = isLight
                            )
                            AboutFeatureBadge(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Security,
                                label = if (prefLanguage == "bn") "জিরো API কী" else "Zero Secrets/Keys",
                                accent = appAccent,
                                isLight = isLight
                            )
                        }
                    }
                }
            }
        }

        // 1.5 In-App GitHub Release Updater (suvadippatra/tracker)
        item(key = "github_updater_card") {
            PixelSettingsGroup(
                title = if (isBn) "অ্যাপ আপডেট (GitHub)" else "App Updates"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (val st = updateState) {
                        is GitHubUpdateState.Idle -> {
                            Button(
                                onClick = { GitHubUpdateManager.checkAndDownloadLatest(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("check_github_update_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = appAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) "আপডেট খুঁজুন ও ডাউনলোড করুন" else "Check & Download Update",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }

                        is GitHubUpdateState.Checking -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = appAccent
                                )
                                Text(
                                    text = if (isBn) "সর্বশেষ কম্পাইলড রিলিজ খোঁজা হচ্ছে..." else "Checking GitHub for latest compiled APK...",
                                    color = AppTextColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        is GitHubUpdateState.Downloading -> {
                            val pctInt = (st.progress * 100f).toInt().coerceIn(0, 100)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "ডাউনলোড হচ্ছে ${st.versionTag}" else "Downloading ${st.versionTag}",
                                    color = AppTextColor(),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$pctInt%",
                                    color = appAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (st.totalBytes > 0L) {
                                LinearProgressIndicator(
                                    progress = { st.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = appAccent
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = appAccent
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${st.fileName} (${GitHubUpdateManager.formatBytes(st.downloadedBytes)}" +
                                        if (st.totalBytes > 0L) " / ${GitHubUpdateManager.formatBytes(st.totalBytes)})" else ")",
                                    color = AppSecondaryTextColor(),
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { GitHubUpdateManager.cancelDownload() },
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                                ) {
                                    Text(if (isBn) "বাতিল" else "Cancel", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        is GitHubUpdateState.ReadyToInstall -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isBn) {
                                        "${st.versionTag} ডাউনলোড সম্পূর্ণ (${st.fileName})"
                                    } else {
                                        "${st.versionTag} ready to install (${st.fileName})"
                                    },
                                    color = AppTextColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { GitHubUpdateManager.retryInstall(context) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("install_downloaded_apk_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isBn) "এখনই ইনস্টল করুন" else "Install APK", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { GitHubUpdateManager.checkAndDownloadLatest(context) },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Re-check", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        is GitHubUpdateState.PermissionRequired -> {
                            Text(
                                text = if (isBn) {
                                    "APK ইনস্টল করার জন্য 'Install unknown apps' অনুমতি চালু করুন (${st.fileName})"
                                } else {
                                    "Allow 'Install unknown apps' for Traker to install ${st.fileName}"
                                },
                                color = Color(0xFFE65100),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = { GitHubUpdateManager.openInstallPermissionSettings(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("grant_install_permission_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = appAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) "ইনস্টল অনুমতি দিন" else "Allow APK Installs",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        is GitHubUpdateState.UpToDate -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isBn) {
                                        "আপনার অ্যাপটি আপ-টু-ডেট (সর্বশেষ: ${st.latestVersion})"
                                    } else {
                                        "You are on the latest version (${st.latestVersion})"
                                    },
                                    color = AppTextColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { GitHubUpdateManager.downloadSpecificRelease(context, st.releaseInfo) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("download_latest_compiled_apk_btn"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBn) "সর্বশেষ APK ডাউনলোড" else "Download Latest APK",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = { GitHubUpdateManager.checkAndDownloadLatest(context) },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Re-check", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        is GitHubUpdateState.Error -> {
                            Text(
                                text = st.message,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Button(
                                onClick = { GitHubUpdateManager.checkAndDownloadLatest(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("retry_github_update_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = appAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isBn) "আবার চেষ্টা করুন" else "Retry Update Check", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Community & Feedback (Privacy-Safe Embedded Links — No Personal Name/ID/URL Displayed)
        item {
            PixelSettingsGroup(
                title = if (prefLanguage == "bn") "কমিউনিটি ও ফিডব্যাক" else "Community & Feedback"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                val reqUrl = listOf("https://github.com", "suvadippatra", "tracker", "").joinToString("/")
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(reqUrl)
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("star_on_github_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = appAccent.copy(alpha = 0.15f),
                            contentColor = appAccent
                        )
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (prefLanguage == "bn") "গিটহাবে স্টার দিন" else "Star on GitHub",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            try {
                                val issueUrl = listOf("https://github.com", "suvadippatra", "tracker", "issues", "new", "choose").joinToString("/")
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(issueUrl)
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("new_issue_or_feature_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AppTextColor().copy(alpha = 0.08f),
                            contentColor = AppTextColor()
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (prefLanguage == "bn") "সমস্যা বা ফিচার অনুরোধ" else "New Issue or Request Feature",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Concise Quick Feature Summary
        item {
            PixelSettingsGroup(
                title = if (prefLanguage == "bn") "মূল সুবিধাসমূহ (Quick Guide)" else "Quick Feature Guide"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickHelpItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(appAccent.copy(alpha = if (isLight) 0.12f else 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = appAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (prefLanguage == "bn") item.titleBn else item.titleEn,
                                    color = AppTextColor(),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (prefLanguage == "bn") item.descBn else item.descEn,
                                    color = AppSecondaryTextColor(),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        if (index < quickHelpItems.lastIndex) {
                            HorizontalDivider(
                                color = AppSecondaryTextColor().copy(alpha = 0.10f),
                                modifier = Modifier.padding(start = 46.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Compact Open-Source Stack & Libraries
        item {
            PixelSettingsGroup(
                title = if (prefLanguage == "bn") "ওপেন-সোর্স প্রযুক্তি ও লাইব্রেরি" else "Open-Source Stack & Libraries"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    publicReferences.forEachIndexed { idx, ref ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = ref.name,
                                    color = AppTextColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = ref.category,
                                    color = AppSecondaryTextColor(),
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = appAccent.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = ref.license,
                                    color = appAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (idx < publicReferences.lastIndex) {
                            HorizontalDivider(color = AppSecondaryTextColor().copy(alpha = 0.08f))
                        }
                    }
                }
            }
        }

        // 5. Legal & Privacy Dialog Links (Zero Personal Handle Shown)
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "আইনি ও গোপনীয়তা" else "Legal & Privacy") {
                PixelSettingsItem(
                    icon = Icons.Default.HelpOutline,
                    title = if (prefLanguage == "bn") "বিস্তারিত ব্যবহার নির্দেশিকা (Help)" else "Help & User Documentation",
                    subtitle = if (prefLanguage == "bn") "মডিউল, বাটন ও শর্টকাট গাইড" else "Interactive module guide & control reference",
                    onClick = onOpenHelp,
                    showDivider = true
                )
                PixelSettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = if (prefLanguage == "bn") "প্রাইভেসি পলিসি" else "Privacy Policy",
                    subtitle = if (prefLanguage == "bn") "১০০% অফলাইন ও লোকাল ডাটাবেস সুরক্ষা" else "100% offline local storage & zero cloud tracking",
                    onClick = { activeLegalDialog = "privacy" },
                    showDivider = true
                )
                PixelSettingsItem(
                    icon = Icons.Default.Gavel,
                    title = if (prefLanguage == "bn") "ওপেন-সোর্স লাইসেন্স" else "Open-Source Licenses",
                    subtitle = "Apache License 2.0",
                    onClick = { activeLegalDialog = "license" },
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun AboutFeatureBadge(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    accent: Color,
    isLight: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isLight) Color(0xFFF3F6F9) else Color(0xFF1A242D)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = AppTextColor(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

