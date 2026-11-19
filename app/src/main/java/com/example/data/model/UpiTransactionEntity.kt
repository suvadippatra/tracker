package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UpiTransactionType {
    DEBIT,
    CREDIT
}

@Entity(
    tableName = "upi_transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["upiRefNumber"]),
        Index(value = ["amount", "transactionType", "timestamp"])
    ]
)
data class UpiTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amount: Double,
    val transactionType: String, // "DEBIT" or "CREDIT"
    val counterpartyOrReceiverId: String,
    val sourceAppOrBank: String,
    val upiRefNumber: String? = null,
    val rawMessage: String = "",
    val captureSource: String = "NOTIFICATION", // "NOTIFICATION", "SMS", "MERGED"
    val timestamp: Long = System.currentTimeMillis(),
    val isImportedToExtras: Boolean = false
)
