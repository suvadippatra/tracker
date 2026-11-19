package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UpiTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UpiTransactionDao {
    @Query("SELECT * FROM upi_transactions ORDER BY timestamp DESC")
    fun getAllUpiTransactions(): Flow<List<UpiTransactionEntity>>

    @Query("SELECT * FROM upi_transactions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentUpiTransactionsSync(limit: Int = 50): List<UpiTransactionEntity>

    @Query("SELECT * FROM upi_transactions WHERE upiRefNumber = :refNumber LIMIT 1")
    suspend fun findByUpiRefNumber(refNumber: String): UpiTransactionEntity?

    @Query(
        """
        SELECT * FROM upi_transactions
        WHERE ABS(amount - :amount) < 0.01
          AND transactionType = :transactionType
          AND timestamp BETWEEN :minTimestamp AND :maxTimestamp
        ORDER BY ABS(timestamp - :targetTimestamp) ASC
        LIMIT 1
        """
    )
    suspend fun findDuplicateInWindow(
        amount: Double,
        transactionType: String,
        minTimestamp: Long,
        maxTimestamp: Long,
        targetTimestamp: Long
    ): UpiTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UpiTransactionEntity): Long

    @Update
    suspend fun update(entity: UpiTransactionEntity)

    @Query("UPDATE upi_transactions SET isImportedToExtras = 1 WHERE id = :id")
    suspend fun markImportedToExtras(id: Long)

    @Query("DELETE FROM upi_transactions WHERE timestamp < :cutoffTimestamp AND isImportedToExtras = 1")
    suspend fun pruneOldImported(cutoffTimestamp: Long)
}
