package com.example.service.upi

import android.content.Context
import android.provider.Telephony
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.service.DetectedUpiPaymentBus
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager fallback that scans the last 30 minutes of SMS inbox messages
 * to recover any transactional Indian bank alerts missed when OEM Doze/battery restrictions
 * throttled BroadcastReceiver delivery. Strictly 100% on-device.
 */
class BankSmsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!DetectedUpiPaymentBus.syncEnabledState(ctx)) return Result.success()
        if (!PermissionAndBatteryHelper.isSmsFallbackEnabled(ctx)) return Result.success()
        if (!PermissionAndBatteryHelper.hasSmsPermissions(ctx)) return Result.success()

        return runCatching {
            val cutoffMillis = System.currentTimeMillis() - LOOKBACK_WINDOW_MS
            val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            val selection = "${Telephony.Sms.DATE} >= ?"
            val selectionArgs = arrayOf(cutoffMillis.toString())
            val sortOrder = "${Telephony.Sms.DATE} ASC"

            val repo = UpiTransactionRepository.create(ctx)

            ctx.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val sender = if (addrIdx >= 0) cursor.getString(addrIdx).orEmpty() else ""
                    val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()

                    val parsed = TransactionParser.parseBankSms(sender, body, date) ?: continue
                    repo.ingestTransaction(
                        context = ctx,
                        parsed = parsed,
                        source = CaptureSource.SMS,
                        emitToBus = true
                    )
                }
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "traker_bank_sms_fallback_sync"
        private const val LOOKBACK_WINDOW_MS = 30L * 60L * 1000L // 30 minutes

        fun schedule(context: Context) {
            runCatching {
                val request = PeriodicWorkRequestBuilder<BankSmsSyncWorker>(15, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }
        }

        fun cancel(context: Context) {
            runCatching {
                WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
            }
        }
    }
}
