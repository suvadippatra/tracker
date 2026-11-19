package com.example.service.upi

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.service.PaymentNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Background Survival & Permissions Helper for Notification Listener access,
 * optional Bank SMS fallback permissions, and OEM battery optimization exemptions
 * (Xiaomi/Redmi/POCO, Oppo/Realme, Vivo/iQOO, OnePlus, Samsung).
 */
object PermissionAndBatteryHelper {

    private const val PREFS_NAME = "traker_upi_detect_prefs"
    private const val KEY_SMS_FALLBACK_ENABLED = "bank_sms_fallback_enabled"

    private val _isSmsFallbackEnabledFlow = MutableStateFlow(false)
    val isSmsFallbackEnabledFlow: StateFlow<Boolean> = _isSmsFallbackEnabledFlow.asStateFlow()

    val SMS_PERMISSIONS: Array<String> = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    )

    fun isNotificationListenerEnabled(context: Context): Boolean {
        return runCatching {
            val cn = ComponentName(context, PaymentNotificationListenerService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            flat != null && flat.contains(cn.flattenToString())
        }.getOrDefault(false)
    }

    fun openNotificationListenerSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun hasSmsPermissions(context: Context): Boolean {
        val receiveGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        return receiveGranted && readGranted
    }

    fun isSmsFallbackEnabled(context: Context): Boolean {
        val enabled = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SMS_FALLBACK_ENABLED, false)
        _isSmsFallbackEnabledFlow.value = enabled
        return enabled
    }

    fun setSmsFallbackEnabled(context: Context, enabled: Boolean) {
        val appCtx = context.applicationContext
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SMS_FALLBACK_ENABLED, enabled)
            .apply()
        _isSmsFallbackEnabledFlow.value = enabled
        if (enabled && hasSmsPermissions(appCtx)) {
            BankSmsSyncWorker.schedule(appCtx)
        } else {
            BankSmsSyncWorker.cancel(appCtx)
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        if (isIgnoringBatteryOptimizations(context)) return true
        val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching {
            context.startActivity(directIntent)
            true
        }.getOrDefault(false)
        if (launched) return true

        return runCatching {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            true
        }.getOrDefault(false)
    }

    fun getManufacturerLabel(): String {
        val m = Build.MANUFACTURER.orEmpty().trim()
        return m.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    /**
     * Opens OEM-specific Auto-Start / Background Power Management settings for
     * Xiaomi/Redmi/POCO, Oppo/Realme, Vivo/iQOO, OnePlus, and Samsung devices,
     * falling back safely to standard Android Battery Optimization / App Details settings.
     */
    fun openOemBackgroundSettings(context: Context): Boolean {
        val brand = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase(Locale.ROOT)
        val candidates = mutableListOf<Intent>()

        when {
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> {
                candidates += componentIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                candidates += componentIntent("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity").apply {
                    putExtra("package_name", context.packageName)
                    putExtra("package_label", "Traker")
                }
            }
            brand.contains("oppo") || brand.contains("realme") -> {
                candidates += componentIntent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                candidates += componentIntent("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity")
                candidates += componentIntent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
            }
            brand.contains("vivo") || brand.contains("iqoo") -> {
                candidates += componentIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
                candidates += componentIntent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                candidates += componentIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
            }
            brand.contains("oneplus") -> {
                candidates += componentIntent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
                candidates += componentIntent("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity")
            }
            brand.contains("samsung") -> {
                candidates += componentIntent("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")
                candidates += componentIntent("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.ram.AutoRunActivity")
                candidates += componentIntent("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
            }
        }

        for (intent in candidates) {
            val resolved = runCatching {
                context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
            }.getOrDefault(false)
            if (resolved) {
                val ok = runCatching {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    true
                }.getOrDefault(false)
                if (ok) return true
            }
        }

        // Fallback: Standard Android Battery Optimization or App Details
        if (!isIgnoringBatteryOptimizations(context) && requestIgnoreBatteryOptimizations(context)) {
            return true
        }
        return runCatching {
            val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appDetails)
            true
        }.getOrDefault(false)
    }

    private fun componentIntent(pkg: String, cls: String): Intent {
        return Intent().setComponent(ComponentName(pkg, cls))
    }
}
