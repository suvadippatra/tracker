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

import android.app.Notification
import com.example.data.model.UpiTransactionType
import com.example.service.upi.CaptureSource
import com.example.service.upi.TransactionParser
import com.example.service.upi.UpiTransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

data class DetectedUpiPayment(
    val id: String = UUID.randomUUID().toString(),
    val appName: String,
    val packageName: String,
    val amount: Double,
    val transactionType: String, // "EXPENSE" or "INCOME"
    val counterparty: String,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val upiRefNumber: String = "",
    val vpa: String = "",
    val roomRecordId: Long = 0L
)

object DetectedUpiPaymentBus {
    private const val PREFS_NAME = "traker_upi_detect_prefs"
    private const val KEY_AUTO_DETECT_ENABLED = "upi_auto_detect_enabled"
    private const val KEY_PENDING_PAYMENTS = "pending_upi_payments_json"
    private const val CHANNEL_UPI_DETECT = "traker_upi_auto_detect"

    @Volatile
    private var cachedAppContext: Context? = null

    private val _pendingPayments = MutableStateFlow<List<DetectedUpiPayment>>(emptyList())
    val pendingPayments: StateFlow<List<DetectedUpiPayment>> = _pendingPayments.asStateFlow()

    private val _isAutoDetectEnabledFlow = MutableStateFlow(true)
    val isAutoDetectEnabledFlow: StateFlow<Boolean> = _isAutoDetectEnabledFlow.asStateFlow()

    private val recentSignatures = LinkedHashMap<String, Long>()

    private fun resolveContext(context: Context?): Context? {
        if (context != null) {
            cachedAppContext = context.applicationContext
            return cachedAppContext
        }
        return cachedAppContext ?: runCatching { com.example.data.AppDatabase.getAppContext() }.getOrNull()
    }

    fun syncEnabledState(context: Context): Boolean {
        val ctx = resolveContext(context) ?: context
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_AUTO_DETECT_ENABLED, true)
        _isAutoDetectEnabledFlow.value = enabled
        return enabled
    }

    fun loadPendingPayments(context: Context): List<DetectedUpiPayment> {
        val ctx = resolveContext(context) ?: context
        if (!syncEnabledState(ctx)) {
            _pendingPayments.value = emptyList()
            return emptyList()
        }
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PENDING_PAYMENTS, "[]") ?: "[]"
        val loaded = mutableListOf<DetectedUpiPayment>()
        runCatching {
            val arr = JSONArray(raw)
            val cutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000L)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val ts = obj.optLong("timestamp", System.currentTimeMillis())
                if (ts < cutoff) continue
                val amt = obj.optDouble("amount", 0.0)
                if (amt <= 0.0) continue
                loaded.add(
                    DetectedUpiPayment(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        appName = obj.optString("appName", "UPI"),
                        packageName = obj.optString("packageName", ""),
                        amount = amt,
                        transactionType = obj.optString("transactionType", "EXPENSE"),
                        counterparty = obj.optString("counterparty", ""),
                        rawText = obj.optString("rawText", ""),
                        timestamp = ts,
                        upiRefNumber = obj.optString("upiRefNumber", ""),
                        vpa = obj.optString("vpa", ""),
                        roomRecordId = obj.optLong("roomRecordId", 0L)
                    )
                )
            }
        }
        val merged = (loaded + _pendingPayments.value)
            .distinctBy { it.id }
            .sortedBy { it.timestamp }
            .takeLast(10)
        _pendingPayments.value = merged
        return merged
    }

    private fun persistPendingPayments(context: Context?, list: List<DetectedUpiPayment>) {
        val ctx = resolveContext(context) ?: return
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("appName", p.appName)
                put("packageName", p.packageName)
                put("amount", p.amount)
                put("transactionType", p.transactionType)
                put("counterparty", p.counterparty)
                put("rawText", p.rawText)
                put("timestamp", p.timestamp)
                put("upiRefNumber", p.upiRefNumber)
                put("vpa", p.vpa)
                put("roomRecordId", p.roomRecordId)
            })
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING_PAYMENTS, arr.toString())
            .apply()
    }

    fun setAutoDetectEnabled(context: Context, enabled: Boolean) {
        val ctx = resolveContext(context) ?: context
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_AUTO_DETECT_ENABLED, enabled)
            if (!enabled) putString(KEY_PENDING_PAYMENTS, "[]")
        }.apply()
        _isAutoDetectEnabledFlow.value = enabled
        if (!enabled) {
            _pendingPayments.value = emptyList()
        } else {
            ensureListenerBound(ctx)
        }
    }

    fun ensureListenerBound(context: Context) {
        val ctx = resolveContext(context) ?: context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isNotificationListenerEnabled(ctx)) {
            runCatching {
                NotificationListenerService.requestRebind(
                    ComponentName(ctx, PaymentNotificationListenerService::class.java)
                )
            }
        }
    }

    fun postDetectedPayment(payment: DetectedUpiPayment) {
        postDetectedPayment(resolveContext(null), payment)
    }

    fun postDetectedPayment(context: Context?, payment: DetectedUpiPayment) {
        val ctx = resolveContext(context)
        if (ctx != null && !syncEnabledState(ctx)) return
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
        if (ctx != null && _pendingPayments.value.isEmpty()) {
            loadPendingPayments(ctx)
        }
        val updated = (_pendingPayments.value.filterNot { it.id == payment.id } + payment).takeLast(10)
        _pendingPayments.value = updated
        persistPendingPayments(ctx, updated)
        if (ctx != null) {
            showUpiDetectedNotification(ctx, payment)
        }
    }

    fun enrichPendingPayment(
        context: Context?,
        roomRecordId: Long,
        amount: Double,
        transactionType: String,
        enrichedCounterparty: String,
        enrichedAppName: String,
        enrichedUpiRef: String,
        enrichedVpa: String
    ) {
        val ctx = resolveContext(context)
        val current = _pendingPayments.value
        if (current.isEmpty()) return
        var matched = false
        val updated = current.map { item ->
            val isMatch = (roomRecordId > 0L && item.roomRecordId == roomRecordId) ||
                (item.amount == amount && item.transactionType == transactionType &&
                    kotlin.math.abs(System.currentTimeMillis() - item.timestamp) <= 180_000L)
            if (isMatch) {
                matched = true
                item.copy(
                    appName = enrichedAppName.ifBlank { item.appName },
                    counterparty = enrichedCounterparty.ifBlank { item.counterparty },
                    upiRefNumber = enrichedUpiRef.ifBlank { item.upiRefNumber },
                    vpa = enrichedVpa.ifBlank { item.vpa },
                    roomRecordId = if (roomRecordId > 0L) roomRecordId else item.roomRecordId
                )
            } else {
                item
            }
        }
        if (matched) {
            _pendingPayments.value = updated
            persistPendingPayments(ctx, updated)
        }
    }

    fun dismissPayment(id: String, context: Context? = null) {
        val ctx = resolveContext(context)
        val updated = _pendingPayments.value.filterNot { it.id == id }
        _pendingPayments.value = updated
        persistPendingPayments(ctx, updated)
        if (ctx != null) {
            runCatching {
                val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(id.hashCode())
            }
        }
    }

    private fun showUpiDetectedNotification(context: Context, payment: DetectedUpiPayment) {
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(
                    CHANNEL_UPI_DETECT,
                    "UPI & Bank Auto-Detection",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Quick notification when a UPI or bank payment is detected"
                }
                nm.createNotificationChannel(ch)
            }
            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = "OPEN_UPI_DETECT"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            val pi = PendingIntent.getActivity(context, payment.id.hashCode(), openIntent, flags)

            val verb = if (payment.transactionType == "INCOME") "Received" else "Paid"
            val amtText = if (payment.amount % 1.0 == 0.0) {
                "₹${payment.amount.toLong()}"
            } else {
                String.format(Locale.US, "₹%.2f", payment.amount)
            }
            val cpText = payment.counterparty.ifBlank { payment.appName }

            val notification = NotificationCompat.Builder(context, CHANNEL_UPI_DETECT)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("$verb $amtText • ${payment.appName}")
                .setContentText("Tap to save record for $cpText (Add voice memo or screenshot)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()

            nm.notify(payment.id.hashCode(), notification)
        }
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        fun parseUpiNotification(packageName: String, title: String, text: String): DetectedUpiPayment? {
            val parsed = TransactionParser.parseNotification(
                packageName = packageName,
                title = title,
                text = text,
                bigText = null
            ) ?: return null
            return DetectedUpiPayment(
                appName = parsed.sourceAppOrBank,
                packageName = packageName,
                amount = parsed.amount,
                transactionType = if (parsed.transactionType == UpiTransactionType.CREDIT) "INCOME" else "EXPENSE",
                counterparty = parsed.counterpartyOrReceiverId,
                rawText = parsed.rawMessage,
                timestamp = parsed.timestamp,
                upiRefNumber = parsed.upiRefNumber.orEmpty(),
                vpa = parsed.vpa.orEmpty()
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        DetectedUpiPaymentBus.syncEnabledState(applicationContext)
        DetectedUpiPaymentBus.loadPendingPayments(applicationContext)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        DetectedUpiPaymentBus.syncEnabledState(applicationContext)
        DetectedUpiPaymentBus.loadPendingPayments(applicationContext)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                requestRebind(ComponentName(this, PaymentNotificationListenerService::class.java))
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        runCatching {
            if (!DetectedUpiPaymentBus.syncEnabledState(applicationContext)) return
            val pkg = sbn.packageName ?: return
            if (!TransactionParser.isAllowedNotificationPackage(pkg)) return

            val notification = sbn.notification ?: return
            // Ignore ongoing/sticky system banners and group summary notifications
            if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return
            if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

            val extras = notification.extras ?: return
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            val plainText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.mapNotNull { it?.toString()?.trim() }
                ?.filter { it.isNotEmpty() }
                ?.joinToString(" ")

            val secondaryText = listOfNotNull(plainText, textLines, subText)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(" ")

            val parsed = TransactionParser.parseNotification(
                packageName = pkg,
                title = title,
                text = secondaryText,
                bigText = bigText,
                timestamp = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()
            ) ?: return

            val appContext = applicationContext
            serviceScope.launch {
                runCatching {
                    UpiTransactionRepository.create(appContext).ingestTransaction(
                        context = appContext,
                        parsed = parsed,
                        source = CaptureSource.NOTIFICATION,
                        emitToBus = true
                    )
                }
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
