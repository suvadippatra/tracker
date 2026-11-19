package com.example.service.upi

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.dao.UpiTransactionDao
import com.example.data.model.UpiTransactionEntity
import com.example.data.model.UpiTransactionType
import com.example.service.DetectedUpiPayment
import com.example.service.DetectedUpiPaymentBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class CaptureSource {
    NOTIFICATION,
    SMS
}

data class DeduplicationOutcome(
    val entity: UpiTransactionEntity,
    val wasMerged: Boolean
)

/**
 * Local Room Repository & 2-Minute Deduplication / Enrichment Engine.
 *
 * When both a UPI app notification (e.g., PhonePe / Google Pay) and a bank SMS
 * (e.g., HDFC / SBI) arrive within a 2-minute window for the same amount and direction
 * (or matching 12-digit UPI RRN), they merge into a single record—enriching the UPI app
 * entry with the bank's exact VPA and 12-digit RRN instead of creating a duplicate.
 */
class UpiTransactionRepository(
    private val dao: UpiTransactionDao
) {
    val allTransactions: Flow<List<UpiTransactionEntity>> = dao.getAllUpiTransactions()

    suspend fun ingestTransaction(
        context: Context?,
        parsed: ParsedUpiTransaction,
        source: CaptureSource,
        emitToBus: Boolean = true
    ): DeduplicationOutcome = ingestMutex.withLock {
        val txTypeStr = parsed.transactionType.name
        val minTs = parsed.timestamp - DEDUP_WINDOW_MS
        val maxTs = parsed.timestamp + DEDUP_WINDOW_MS

        // 1. Check exact 12-digit UPI RRN match first
        val existingByRef = parsed.upiRefNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { dao.findByUpiRefNumber(it) }

        // 2. Otherwise check 2-minute window by (amount, transactionType)
        val existing = existingByRef ?: dao.findDuplicateInWindow(
            amount = parsed.amount,
            transactionType = txTypeStr,
            minTimestamp = minTs,
            maxTimestamp = maxTs,
            targetTimestamp = parsed.timestamp
        )

        if (existing != null) {
            val enrichedCounterparty = chooseRicherCounterparty(
                existingCounterparty = existing.counterpartyOrReceiverId,
                incomingCounterparty = parsed.counterpartyOrReceiverId,
                incomingVpa = parsed.vpa
            )
            val enrichedRef = existing.upiRefNumber?.takeIf { it.isNotBlank() }
                ?: parsed.upiRefNumber?.takeIf { it.isNotBlank() }
            val preferredSourceLabel = choosePreferredSourceLabel(
                existingSource = existing.sourceAppOrBank,
                existingCapture = existing.captureSource,
                incomingSource = parsed.sourceAppOrBank,
                incomingCapture = source
            )
            val mergedRaw = if (existing.rawMessage.contains(parsed.rawMessage.take(30))) {
                existing.rawMessage
            } else {
                "${existing.rawMessage} | ${parsed.rawMessage}".take(480)
            }
            val mergedCapture = if (existing.captureSource != source.name) "MERGED" else existing.captureSource

            val updated = existing.copy(
                counterpartyOrReceiverId = enrichedCounterparty,
                sourceAppOrBank = preferredSourceLabel,
                upiRefNumber = enrichedRef,
                rawMessage = mergedRaw,
                captureSource = mergedCapture
            )
            dao.update(updated)

            if (emitToBus && context != null) {
                DetectedUpiPaymentBus.enrichPendingPayment(
                    context = context,
                    roomRecordId = updated.id,
                    amount = updated.amount,
                    transactionType = toExtrasType(parsed.transactionType),
                    enrichedCounterparty = updated.counterpartyOrReceiverId,
                    enrichedAppName = updated.sourceAppOrBank,
                    enrichedUpiRef = updated.upiRefNumber.orEmpty(),
                    enrichedVpa = parsed.vpa.orEmpty()
                )
            }

            return@withLock DeduplicationOutcome(entity = updated, wasMerged = true)
        }

        val entityToInsert = UpiTransactionEntity(
            amount = parsed.amount,
            transactionType = txTypeStr,
            counterpartyOrReceiverId = parsed.counterpartyOrReceiverId,
            sourceAppOrBank = parsed.sourceAppOrBank,
            upiRefNumber = parsed.upiRefNumber,
            rawMessage = parsed.rawMessage,
            captureSource = source.name,
            timestamp = parsed.timestamp,
            isImportedToExtras = false
        )
        val insertedId = dao.insert(entityToInsert)
        val saved = entityToInsert.copy(id = insertedId)

        if (emitToBus && context != null) {
            DetectedUpiPaymentBus.postDetectedPayment(
                context = context,
                payment = DetectedUpiPayment(
                    id = "upi_$insertedId",
                    appName = saved.sourceAppOrBank,
                    packageName = source.name,
                    amount = saved.amount,
                    transactionType = toExtrasType(parsed.transactionType),
                    counterparty = saved.counterpartyOrReceiverId,
                    rawText = saved.rawMessage,
                    timestamp = saved.timestamp,
                    upiRefNumber = saved.upiRefNumber.orEmpty(),
                    vpa = parsed.vpa.orEmpty(),
                    roomRecordId = insertedId
                )
            )
        }

        DeduplicationOutcome(entity = saved, wasMerged = false)
    }

    suspend fun markImportedToExtras(recordId: Long) {
        if (recordId > 0L) {
            dao.markImportedToExtras(recordId)
        }
    }

    private fun toExtrasType(type: UpiTransactionType): String {
        return if (type == UpiTransactionType.CREDIT) "INCOME" else "EXPENSE"
    }

    private fun chooseRicherCounterparty(
        existingCounterparty: String,
        incomingCounterparty: String,
        incomingVpa: String?
    ): String {
        if (existingCounterparty.contains("@")) return existingCounterparty
        if (!incomingVpa.isNullOrBlank()) return incomingVpa
        if (incomingCounterparty.contains("@")) return incomingCounterparty
        val existingGeneric = existingCounterparty.endsWith("UPI") || existingCounterparty.endsWith("Payment")
        val incomingGeneric = incomingCounterparty.endsWith("UPI") || incomingCounterparty.endsWith("Payment")
        return when {
            existingGeneric && !incomingGeneric && incomingCounterparty.isNotBlank() -> incomingCounterparty
            existingCounterparty.isBlank() -> incomingCounterparty
            else -> existingCounterparty
        }
    }

    private fun choosePreferredSourceLabel(
        existingSource: String,
        existingCapture: String,
        incomingSource: String,
        incomingCapture: CaptureSource
    ): String {
        // Keep the UPI app name (e.g. PhonePe / Google Pay) when merging with a bank SMS
        return when {
            existingCapture == CaptureSource.NOTIFICATION.name && incomingCapture == CaptureSource.SMS -> existingSource
            existingCapture == CaptureSource.SMS.name && incomingCapture == CaptureSource.NOTIFICATION -> incomingSource
            else -> existingSource.ifBlank { incomingSource }
        }
    }

    companion object {
        const val DEDUP_WINDOW_MS: Long = 2L * 60L * 1000L // 2 minutes
        private val ingestMutex = Mutex()

        fun create(context: Context): UpiTransactionRepository {
            val db = AppDatabase.getDatabase(context.applicationContext)
            return UpiTransactionRepository(db.upiTransactionDao())
        }
    }
}
