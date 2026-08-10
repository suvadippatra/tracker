package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BankAccountEntity
import com.example.data.model.ExtraExpenseEntity
import com.example.data.model.HotelEntity
import com.example.data.model.HotelTransactionEntity
import com.example.data.model.MenuEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.MeterReading
import com.example.data.model.HotelPaymentEntity
import com.example.data.model.TransactionCategoryEntity
import com.example.data.model.UtilityBill
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileSync(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)
}

@Dao
interface HotelDao {
    @Query("SELECT * FROM hotels WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllHotels(): Flow<List<HotelEntity>>

    @Query("SELECT * FROM hotels WHERE isDeleted = 0 ORDER BY id ASC")
    suspend fun getAllHotelsSync(): List<HotelEntity>

    @Query("SELECT * FROM hotels WHERE isDeleted = 1 ORDER BY id ASC")
    fun getDeletedHotels(): Flow<List<HotelEntity>>

    @Query("SELECT * FROM hotels WHERE id = :id LIMIT 1")
    fun getHotelById(id: Int): Flow<HotelEntity?>

    @Query("SELECT * FROM hotels WHERE id = :id LIMIT 1")
    suspend fun getHotelByIdSync(id: Int): HotelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHotel(hotel: HotelEntity): Long

    @Update
    suspend fun updateHotel(hotel: HotelEntity)

    @Delete
    suspend fun deleteHotel(hotel: HotelEntity)
}

@Dao
interface MenuDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenu(menu: MenuEntity)

    @Update
    suspend fun updateMenu(menu: MenuEntity)

    @Delete
    suspend fun deleteMenu(menu: MenuEntity)

    @Query("SELECT * FROM menu_items ORDER BY id ASC")
    fun getAllMenuItems(): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menu_items WHERE hotelId = :hotelId AND isDeleted = 0 ORDER BY id ASC")
    fun getMenuForHotel(hotelId: Int): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menu_items WHERE hotelId = :hotelId AND isDeleted = 0 ORDER BY id ASC")
    suspend fun getMenusForHotelSync(hotelId: Int): List<MenuEntity>

    @Query("SELECT * FROM menu_items WHERE hotelId = :hotelId AND isDeleted = 1 ORDER BY id ASC")
    fun getDeletedMenuForHotel(hotelId: Int): Flow<List<MenuEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(menuItems: List<MenuEntity>)

    @Query("DELETE FROM menu_items")
    suspend fun deleteAllMenuItems()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM hotel_transactions WHERE hotelId = :hotelId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsForHotel(hotelId: Int): Flow<List<HotelTransactionEntity>>
    
    @Query("SELECT * FROM hotel_transactions WHERE hotelId = :hotelId AND isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getTransactionsForHotelSync(hotelId: Int): List<HotelTransactionEntity>
    
    @Query("SELECT * FROM hotel_transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSync(): List<HotelTransactionEntity>
    
    @Query("SELECT * FROM hotel_transactions WHERE hotelId = :hotelId AND isDeleted = 0 AND timestamp >= :start AND timestamp <= :end")
    suspend fun getTransactionsForHotelAndDateRange(hotelId: Int, start: Long, end: Long): List<HotelTransactionEntity>

    @Query("SELECT * FROM hotel_transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<HotelTransactionEntity>>
    @Query("SELECT * FROM hotel_transactions WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedTransactions(): Flow<List<HotelTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: HotelTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: HotelTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: HotelTransactionEntity)
}

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllBankAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE isDeleted = 0 ORDER BY id ASC")
    suspend fun getAllBankAccountsSync(): List<BankAccountEntity>

    @Query("SELECT * FROM bank_accounts WHERE isDeleted = 1 ORDER BY id ASC")
    fun getDeletedBankAccounts(): Flow<List<BankAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(account: BankAccountEntity): Long

    @Update
    suspend fun updateBankAccount(account: BankAccountEntity)

    @Delete
    suspend fun deleteBankAccount(account: BankAccountEntity)
}

@Dao
interface ExtraExpenseDao {
    @Query("SELECT * FROM extra_expenses WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExtraExpenseEntity>>
    @Query("SELECT * FROM extra_expenses WHERE isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getAllExpensesSync(): List<ExtraExpenseEntity>
    @Query("SELECT * FROM extra_expenses WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedExpenses(): Flow<List<ExtraExpenseEntity>>
    
    @Query("SELECT * FROM extra_expenses WHERE isDeleted = 0 AND timestamp >= :start AND timestamp <= :end")
    suspend fun getExpensesForDateRange(start: Long, end: Long): List<ExtraExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExtraExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExtraExpenseEntity)
}

@Dao
interface HotelPaymentDao {
    @Query("SELECT * FROM hotel_payments WHERE hotelId = :hotelId ORDER BY paymentDate DESC")
    fun getPaymentsForHotel(hotelId: Int): Flow<List<HotelPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: HotelPaymentEntity)

    @Delete
    suspend fun deletePayment(payment: HotelPaymentEntity)
}

@Dao
interface MeterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReading)

    @Query("SELECT * FROM meter_readings WHERE isDeleted = 0 ORDER BY readingDate DESC")
    fun getAllReadings(): Flow<List<MeterReading>>

    @Query("SELECT COALESCE(SUM(unitsConsumed), 0.0) FROM meter_readings WHERE isDeleted = 0 AND readingDate > :start AND readingDate <= :end")
    fun getSumOfUnitsConsumed(start: Long, end: Long): Flow<Double>

    @Query("SELECT * FROM meter_readings WHERE isDeleted = 0 ORDER BY readingDate ASC")
    fun getAllReadingsSorted(): Flow<List<MeterReading>>

    @Query("UPDATE meter_readings SET isPaid = :isPaid WHERE isDeleted = 0 AND readingDate > :start AND readingDate <= :end")
    suspend fun updateReadingsPaidStatus(start: Long, end: Long, isPaid: Boolean)

    @Query("SELECT * FROM meter_readings WHERE isDeleted = 0 ORDER BY readingDate DESC LIMIT 1")
    fun getLatestReading(): Flow<MeterReading?>

    @Query("UPDATE meter_readings SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteReading(id: String)
    @Query("SELECT * FROM meter_readings WHERE isDeleted = 1 ORDER BY readingDate DESC")
    fun getDeletedReadings(): Flow<List<MeterReading>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtilityBill(bill: UtilityBill)

    @Query("SELECT * FROM utility_bills WHERE isDeleted = 0 ORDER BY startDate DESC")
    fun getAllUtilityBills(): Flow<List<UtilityBill>>
    @Query("SELECT * FROM utility_bills WHERE isDeleted = 1 ORDER BY startDate DESC")
    fun getDeletedUtilityBills(): Flow<List<UtilityBill>>

    @Delete
    suspend fun deleteReading(reading: MeterReading)

    @Delete
    suspend fun deleteUtilityBill(bill: UtilityBill)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElectricityRate(rate: com.example.data.model.ElectricityRate)

    @Query("SELECT * FROM electricity_rates ORDER BY effectiveDate DESC")
    fun getAllElectricityRates(): Flow<List<com.example.data.model.ElectricityRate>>

    @Delete
    suspend fun deleteElectricityRate(rate: com.example.data.model.ElectricityRate)
}

@Dao
interface TransactionCategoryDao {
    @Query("SELECT * FROM transaction_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<TransactionCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: TransactionCategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: TransactionCategoryEntity)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE expenseId = :expenseId ORDER BY timestamp ASC")
    fun getAttachmentsForExpense(expenseId: Int): Flow<List<com.example.data.model.AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: com.example.data.model.AttachmentEntity): Long

    @Delete
    suspend fun deleteAttachment(attachment: com.example.data.model.AttachmentEntity)

    @Query("DELETE FROM attachments WHERE expenseId = :expenseId")
    suspend fun deleteAttachmentsForExpense(expenseId: Int)
}

@Dao
interface OwnerAccountDao {
    @Query("SELECT * FROM owner_accounts ORDER BY id ASC")
    fun getAllOwnerAccounts(): Flow<List<com.example.data.model.OwnerAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnerAccount(account: com.example.data.model.OwnerAccountEntity): Long

    @Update
    suspend fun updateOwnerAccount(account: com.example.data.model.OwnerAccountEntity)

    @Delete
    suspend fun deleteOwnerAccount(account: com.example.data.model.OwnerAccountEntity)
}
