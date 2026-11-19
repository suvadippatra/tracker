package com.example.service.upi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.service.DetectedUpiPaymentBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Secondary Fallback Engine: Captures incoming transactional Indian Bank SMS alerts
 * (e.g., VM-HDFCBK, AD-SBIUPI, JD-ICICIB, AX-AXISBK, BK-KOTAKB) strictly on-device.
 * Gated by the user's opt-in Settings toggle and runtime SMS permissions.
 */
class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val appContext = context.applicationContext

        if (!DetectedUpiPaymentBus.syncEnabledState(appContext)) return
        if (!PermissionAndBatteryHelper.isSmsFallbackEnabled(appContext)) return
        if (!PermissionAndBatteryHelper.hasSmsPermissions(appContext)) return

        val messages = runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent) }.getOrNull()
            ?: return
        if (messages.isEmpty()) return

        // Concatenate multi-part SMS segments per sender
        val groupedBySender = LinkedHashMap<String, Pair<StringBuilder, Long>>()
        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: continue
            val bodyPart = sms.displayMessageBody ?: sms.messageBody ?: continue
            val ts = sms.timestampMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
            val entry = groupedBySender[sender]
            if (entry == null) {
                groupedBySender[sender] = StringBuilder(bodyPart) to ts
            } else {
                entry.first.append(bodyPart)
            }
        }

        val parsedItems = groupedBySender.mapNotNull { (sender, pair) ->
            TransactionParser.parseBankSms(
                sender = sender,
                body = pair.first.toString(),
                timestamp = pair.second
            )
        }
        if (parsedItems.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo = UpiTransactionRepository.create(appContext)
                for (parsed in parsedItems) {
                    repo.ingestTransaction(
                        context = appContext,
                        parsed = parsed,
                        source = CaptureSource.SMS,
                        emitToBus = true
                    )
                }
            } catch (_: Exception) {
                // Keep receiver crash-proof
            } finally {
                pendingResult.finish()
            }
        }
    }
}
