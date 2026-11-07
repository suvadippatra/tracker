package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class DetectedUpiPayment(
    val id: String = UUID.randomUUID().toString(),
    val appName: String,
    val packageName: String,
    val amount: Double,
    val transactionType: String, // "EXPENSE" or "INCOME"
    val counterparty: String,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
)

object DetectedUpiPaymentBus {
    private const val PREFS_NAME = "traker_upi_detect_prefs"
    private const val KEY_AUTO_DETECT_ENABLED = "upi_auto_detect_enabled"

    private val _pendingPayments = MutableStateFlow<List<DetectedUpiPayment>>(emptyList())
    val pendingPayments: StateFlow<List<DetectedUpiPayment>> = _pendingPayments.asStateFlow()

    private val _isAutoDetectEnabledFlow = MutableStateFlow(true)
    val isAutoDetectEnabledFlow: StateFlow<Boolean> = _isAutoDetectEnabledFlow.asStateFlow()

    private val recentSignatures = LinkedHashMap<String, Long>()

    fun syncEnabledState(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_AUTO_DETECT_ENABLED, true)
        _isAutoDetectEnabledFlow.value = enabled
        return enabled
    }

    fun setAutoDetectEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_DETECT_ENABLED, enabled).apply()
        _isAutoDetectEnabledFlow.value = enabled
        if (!enabled) {
            _pendingPayments.value = emptyList()
        }
    }

    fun postDetectedPayment(payment: DetectedUpiPayment) {
        if (!_isAutoDetectEnabledFlow.value) return
        val now = System.currentTimeMillis()
        val sig = "${payment.amount}|${payment.transactionType}|${payment.counterparty.lowercase(Locale.ROOT).take(12)}"
        synchronized(recentSignatures) {
            val prev = recentSignatures[sig]
            if (prev != null && (now - prev) < 45_000L) return
            recentSignatures[sig] = now
            if (recentSignatures.size > 50) {
                val oldestKey = recentSignatures.keys.firstOrNull()
                if (oldestKey != null) recentSignatures.remove(oldestKey)
            }
        }
        _pendingPayments.value = (_pendingPayments.value + payment).takeLast(10)
    }

    fun dismissPayment(id: String) {
        _pendingPayments.value = _pendingPayments.value.filterNot { it.id == id }
    }

    fun isDeviceCapableOfUpiDetection(context: Context): Boolean {
        return runCatching {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.resolveActivity(context.packageManager) != null
        }.getOrDefault(false)
    }

    fun hasTelephonyOrSimSupport(context: Context): Boolean {
        return runCatching {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        }.getOrDefault(false)
    }

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

    fun openOfflineSpeechSettings(context: Context) {
        val intents = listOf(
            Intent("com.google.android.voicesearch.INSTALL_VOICE_DATA"),
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (i in intents) {
            val ok = runCatching {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                true
            }.getOrDefault(false)
            if (ok) break
        }
    }
}

class PaymentNotificationListenerService : NotificationListenerService() {

    companion object {
        private val SUPPORTED_UPI_PACKAGES = mapOf(
            "com.google.android.apps.nbu.paisa.user" to "Google Pay",
            "com.phonepe.app" to "PhonePe",
            "net.one97.paytm" to "Paytm",
            "com.bharatpe.app" to "BharatPe",
            "in.org.npci.upiapp" to "BHIM UPI",
            "in.amazon.mShop.android.shopping" to "Amazon Pay",
            "com.whatsapp" to "WhatsApp Pay",
            "com.dreamplug.androidapp" to "CRED UPI",
            "com.mobikwik_new" to "MobiKwik",
            "com.freecharge.android" to "Freecharge",
            "com.csam.icici.bank.imobile" to "iMobile Pay",
            "com.sbi.lotusintouch" to "YONO SBI",
            "com.naviapp" to "Navi UPI",
            "money.super.payments" to "super.money",
            "indwin.c3.shareapp" to "Slice UPI",
            "money.jupiter" to "Jupiter",
            "com.epifi.paisa" to "Fi Money",
            "com.google.android.apps.messaging" to "Bank SMS Alert",
            "com.android.mms" to "Bank SMS Alert",
            "com.samsung.android.messaging" to "Bank SMS Alert"
        )

        private val AMOUNT_REGEX = Regex(
            """(?:₹|Rs\.?|INR)\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
            RegexOption.IGNORE_CASE
        )

        fun parseUpiNotification(packageName: String, title: String, text: String): DetectedUpiPayment? {
            if (packageName == "com.tracker.suva" || packageName == "com.example") return null

            val combined = "$title $text".trim()
            if (combined.length < 6) return null
            val lower = combined.lowercase(Locale.ROOT)

            val appName = SUPPORTED_UPI_PACKAGES[packageName] ?: when {
                packageName.contains("paisa", ignoreCase = true) || packageName.contains("gpay", ignoreCase = true) -> "Google Pay"
                packageName.contains("phonepe", ignoreCase = true) -> "PhonePe"
                packageName.contains("paytm", ignoreCase = true) -> "Paytm"
                packageName.contains("bharatpe", ignoreCase = true) -> "BharatPe"
                packageName.contains("upi", ignoreCase = true) -> "UPI App"
                packageName.contains("messaging", ignoreCase = true) || packageName.contains("mms", ignoreCase = true) || packageName.contains("sms", ignoreCase = true) -> "Bank SMS Alert"
                lower.contains("upi") && (lower.contains("debited") || lower.contains("credited") || lower.contains("paid") || lower.contains("sent")) -> "UPI / Bank Alert"
                else -> return null
            }

            val isPaid = lower.contains("paid") || lower.contains("sent") || lower.contains("debited") || lower.contains("payment of") || lower.contains("spent") || lower.contains("trf to")
            val isReceived = lower.contains("received") || lower.contains("credited") || lower.contains("added to your") || lower.contains("deposited")
            if (!isPaid && !isReceived) return null
            if (lower.contains("failed") || lower.contains("declined") || lower.contains("pending") || lower.contains("request") || lower.contains("otp") || lower.contains("due on")) return null

            val match = AMOUNT_REGEX.find(combined) ?: return null
            val rawAmt = match.groupValues[1].replace(",", "")
            val amount = rawAmt.toDoubleOrNull() ?: return null
            if (amount <= 0.0) return null

            val txType = if (isReceived && !isPaid) "INCOME" else "EXPENSE"

            var counterparty = ""
            val toFromRegex = Regex("""(?:paid to|sent to|trf to|to vpa|received from|from|to)\s+([A-Za-z0-9 .@&_'-]{2,38})""", RegexOption.IGNORE_CASE)
            val cpMatch = toFromRegex.find(combined)
            if (cpMatch != null) {
                counterparty = cpMatch.groupValues[1]
                    .substringBefore(" on ")
                    .substringBefore(" via ")
                    .substringBefore(" using ")
                    .substringBefore(" Ref")
                    .substringBefore(" UTR")
                    .substringBefore(" UPI")
                    .substringBefore(" Avl")
                    .trim()
            }
            if (counterparty.isBlank()) {
                counterparty = title
                    .replace(AMOUNT_REGEX, "")
                    .replace("Paid", "", ignoreCase = true)
                    .replace("Sent", "", ignoreCase = true)
                    .trim()
                    .ifBlank { "$appName Payment" }
            }

            return DetectedUpiPayment(
                appName = appName,
                packageName = packageName,
                amount = amount,
                transactionType = txType,
                counterparty = counterparty.take(40),
                rawText = combined.take(240)
            )
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        runCatching {
            if (!DetectedUpiPaymentBus.syncEnabledState(applicationContext)) return
            val pkg = sbn.packageName ?: return
            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.bigText")?.toString()
                ?: extras.getCharSequence("android.text")?.toString()
                ?: ""
            val detected = parseUpiNotification(pkg, title, text)
            if (detected != null) {
                DetectedUpiPaymentBus.postDetectedPayment(detected)
            }
        }
    }
}

data class MonthlyPaymentReminder(
    val id: String = UUID.randomUUID().toString(),
    val heading: String,
    val description: String,
    val amount: Double?, // Optional
    val accountId: String?, // Optional
    val accountName: String?, // Optional
    val dayOfMonth: Int, // 1..31
    val hourOfDay: Int, // 0..23
    val minute: Int, // 0..59
    val isEnabled: Boolean = true,
    val lastTriggeredMonthKey: String = "" // e.g., "2026-11"
)

object MonthlyReminderManager {
    private const val PREFS_NAME = "traker_monthly_reminders_prefs"
    private const val KEY_REMINDERS = "monthly_reminders_json"
    private const val CHANNEL_ID = "traker_monthly_payment_reminders"

    private val _remindersFlow = MutableStateFlow<List<MonthlyPaymentReminder>>(emptyList())
    val remindersFlow: StateFlow<List<MonthlyPaymentReminder>> = _remindersFlow.asStateFlow()

    private val _dueRemindersFlow = MutableStateFlow<List<MonthlyPaymentReminder>>(emptyList())
    val dueRemindersFlow: StateFlow<List<MonthlyPaymentReminder>> = _dueRemindersFlow.asStateFlow()

    fun loadReminders(context: Context): List<MonthlyPaymentReminder> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_REMINDERS, "[]") ?: "[]"
        val list = mutableListOf<MonthlyPaymentReminder>()
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val hasAmount = obj.has("amount") && !obj.isNull("amount")
                val amt = if (hasAmount) obj.optDouble("amount", 0.0).takeIf { it > 0.0 } else null
                list.add(
                    MonthlyPaymentReminder(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        heading = obj.optString("heading", "Monthly Payment"),
                        description = obj.optString("description", ""),
                        amount = amt,
                        accountId = obj.optString("accountId", "").ifBlank { null },
                        accountName = obj.optString("accountName", "").ifBlank { null },
                        dayOfMonth = obj.optInt("dayOfMonth", 1).coerceIn(1, 31),
                        hourOfDay = obj.optInt("hourOfDay", 9).coerceIn(0, 23),
                        minute = obj.optInt("minute", 0).coerceIn(0, 59),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        lastTriggeredMonthKey = obj.optString("lastTriggeredMonthKey", "")
                    )
                )
            }
        }
        _remindersFlow.value = list
        return list
    }

    fun saveReminders(context: Context, list: List<MonthlyPaymentReminder>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("heading", item.heading)
                put("description", item.description)
                if (item.amount != null && item.amount > 0.0) put("amount", item.amount)
                if (!item.accountId.isNullOrBlank()) put("accountId", item.accountId)
                if (!item.accountName.isNullOrBlank()) put("accountName", item.accountName)
                put("dayOfMonth", item.dayOfMonth)
                put("hourOfDay", item.hourOfDay)
                put("minute", item.minute)
                put("isEnabled", item.isEnabled)
                put("lastTriggeredMonthKey", item.lastTriggeredMonthKey)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_REMINDERS, arr.toString()).apply()
        _remindersFlow.value = list
        scheduleAllAlarms(context, list)
        checkDueRemindersNow(context)
    }

    fun addOrUpdateReminder(context: Context, reminder: MonthlyPaymentReminder) {
        val current = loadReminders(context).toMutableList()
        val idx = current.indexOfFirst { it.id == reminder.id }
        if (idx >= 0) current[idx] = reminder else current.add(0, reminder)
        saveReminders(context, current)
    }

    fun deleteReminder(context: Context, id: String) {
        val current = loadReminders(context).filterNot { it.id == id }
        saveReminders(context, current)
    }

    fun markReminderCompletedForCurrentMonth(context: Context, id: String) {
        val cal = Calendar.getInstance()
        val monthKey = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val updated = loadReminders(context).map {
            if (it.id == id) it.copy(lastTriggeredMonthKey = monthKey) else it
        }
        saveReminders(context, updated)
        _dueRemindersFlow.value = _dueRemindersFlow.value.filterNot { it.id == id }
    }

    fun checkDueRemindersNow(context: Context) {
        val all = loadReminders(context)
        val cal = Calendar.getInstance()
        val monthKey = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDayInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMin = cal.get(Calendar.MINUTE)

        val due = all.filter { r ->
            if (!r.isEnabled || r.lastTriggeredMonthKey == monthKey) return@filter false
            val targetDay = r.dayOfMonth.coerceAtMost(maxDayInMonth)
            if (currentDay > targetDay) true
            else if (currentDay == targetDay) {
                (currentHour > r.hourOfDay) || (currentHour == r.hourOfDay && currentMin >= r.minute)
            } else false
        }
        _dueRemindersFlow.value = due
    }

    private fun scheduleAllAlarms(context: Context, list: List<MonthlyPaymentReminder>) {
        runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            list.forEach { reminder ->
                val intent = Intent(context, MonthlyReminderReceiver::class.java).apply {
                    putExtra("reminder_id", reminder.id)
                    putExtra("heading", reminder.heading)
                    putExtra("description", reminder.description)
                    putExtra("amount", reminder.amount ?: -1.0)
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                val pi = PendingIntent.getBroadcast(context, reminder.id.hashCode(), intent, flags)
                if (!reminder.isEnabled) {
                    alarmManager.cancel(pi)
                } else {
                    val nextTime = computeNextTriggerMillis(reminder)
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pi)
                }
            }
        }
    }

    private fun computeNextTriggerMillis(reminder: MonthlyPaymentReminder): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, reminder.dayOfMonth.coerceAtMost(maxDay))
            set(Calendar.HOUR_OF_DAY, reminder.hourOfDay)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.MONTH, 1)
            val maxDayNext = target.getActualMaximum(Calendar.DAY_OF_MONTH)
            target.set(Calendar.DAY_OF_MONTH, reminder.dayOfMonth.coerceAtMost(maxDayNext))
        }
        return target.timeInMillis
    }

    fun showReminderNotification(context: Context, heading: String, description: String, amount: Double?) {
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "Monthly Payment Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
                nm.createNotificationChannel(ch)
            }
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            val pi = PendingIntent.getActivity(context, heading.hashCode(), openIntent, flags)

            val amtText = if (amount != null && amount > 0.0) " • ₹${String.format(Locale.US, "%.0f", amount)}" else ""
            val body = if (description.isNotBlank()) "$description$amtText" else "Tap to record or review your monthly payment$amtText"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Reminder: $heading")
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()

            nm.notify(heading.hashCode(), notification)
        }
    }
}

class MonthlyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val heading = intent.getStringExtra("heading") ?: "Monthly Payment Reminder"
        val description = intent.getStringExtra("description") ?: ""
        val rawAmt = intent.getDoubleExtra("amount", -1.0)
        val amount = if (rawAmt > 0.0) rawAmt else null
        MonthlyReminderManager.showReminderNotification(context, heading, description, amount)
        MonthlyReminderManager.checkDueRemindersNow(context)
    }
}
