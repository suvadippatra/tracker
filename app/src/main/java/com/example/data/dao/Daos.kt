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
import com.example.data.model.StickyNoteEntity
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

    @Query("SELECT * FROM menu_items WHERE isDeleted = 0 ORDER BY id ASC")
    suspend fun getAllMenuItemsSync(): List<MenuEntity>

    @Query("SELECT * FROM menu_items WHERE hotelId = :hotelId AND isDeleted = 0 ORDER BY id ASC")
    fun getMenuForHotel(hotelId: Int): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menu_items WHERE hotelId = :hotelId AND isDeleted = 0 ORDER BY id ASC")
    suspend fun getMenusForHotelSync(hotelId: Int): List<MenuEntity>

    @Query("SELECT * FROM menu_items WHERE hotelId = :hotelId AND isDeleted = 1 ORDER BY id ASC")
    fun getDeletedMenuForHotel(hotelId: Int): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menu_items WHERE isDeleted = 1 ORDER BY id DESC")
    fun getAllDeletedMenuItems(): Flow<List<MenuEntity>>

    @Query("DELETE FROM menu_items WHERE isDeleted = 1")
    suspend fun emptyDeletedMenuItemsPermanently()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(menuItems: List<MenuEntity>)

    @Query("DELETE FROM menu_items")
    suspend fun deleteAllMenuItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuHistory(history: com.example.data.model.MenuModificationHistoryEntity): Long

    @Delete
    suspend fun deleteMenuHistory(history: com.example.data.model.MenuModificationHistoryEntity)

    @Query("SELECT * FROM menu_modification_history WHERE menuItemId = :menuItemId ORDER BY modifiedAt DESC")
    fun getHistoryForMenuItem(menuItemId: Int): Flow<List<com.example.data.model.MenuModificationHistoryEntity>>

    @Query("SELECT * FROM menu_modification_history WHERE hotelId = :hotelId ORDER BY modifiedAt DESC")
    fun getHistoryForHotel(hotelId: Int): Flow<List<com.example.data.model.MenuModificationHistoryEntity>>
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
    @Query("DELETE FROM hotel_transactions WHERE isDeleted = 1")
    suspend fun emptyDeletedTransactionsPermanently()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: HotelTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<HotelTransactionEntity>)

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

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    suspend fun getBankAccountByIdSync(id: Int): BankAccountEntity?

    @Query("SELECT * FROM bank_accounts WHERE isDeleted = 1 ORDER BY id ASC")
    fun getDeletedBankAccounts(): Flow<List<BankAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(account: BankAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccounts(accounts: List<BankAccountEntity>)

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
    @Query("DELETE FROM extra_expenses WHERE isDeleted = 1")
    suspend fun emptyDeletedExpensesPermanently()
    
    @Query("SELECT * FROM extra_expenses WHERE isDeleted = 0 AND timestamp >= :start AND timestamp <= :end")
    suspend fun getExpensesForDateRange(start: Long, end: Long): List<ExtraExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExtraExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExtraExpenseEntity>)

    @Delete
    suspend fun deleteExpense(expense: ExtraExpenseEntity)
}

@Dao
interface HotelPaymentDao {
    @Query("SELECT * FROM hotel_payments WHERE hotelId = :hotelId ORDER BY paymentDate DESC")
    fun getPaymentsForHotel(hotelId: Int): Flow<List<HotelPaymentEntity>>

    @Query("SELECT * FROM hotel_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<HotelPaymentEntity>>

    @Query("SELECT * FROM hotel_payments ORDER BY paymentDate DESC")
    suspend fun getAllPaymentsSync(): List<HotelPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: HotelPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<HotelPaymentEntity>)

    @Delete
    suspend fun deletePayment(payment: HotelPaymentEntity)
}

@Dao
interface MeterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReading)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<MeterReading>)

    @Query("SELECT * FROM meter_readings WHERE isDeleted = 0 ORDER BY readingDate DESC")
    fun getAllReadings(): Flow<List<MeterReading>>

    @Query("SELECT * FROM meter_readings WHERE isDeleted = 0 ORDER BY readingDate DESC")
    suspend fun getAllReadingsSync(): List<MeterReading>

    @Query("SELECT COALESCE(SUM(unitsConsumed), 0.0) FROM meter_readings WHERE isDeleted = 0 AND readingDate > :start AND readingDate <= :end")
    fun getSumOfUnitsConsumed(start: Long, end: Long): Flow<Double>

    @Query("SELECT * FROM meter_readings WHERE isDeleted = 0 ORDER BY readingDate ASC")
    fun getAllReadingsSorted(): Flow<List<MeterReading>>

    @Query("UPDATE meter_readings SET isPaid = :isPaid WHERE isDeleted = 0 AND readingDate >= :start AND readingDate <= :end")
    suspend fun updateReadingsPaidStatus(start: Long, end: Long, isPaid: Boolean)

    @Query("SELECT * FROM meter_readings WHERE isDeleted = 0 ORDER BY readingDate DESC LIMIT 1")
    fun getLatestReading(): Flow<MeterReading?>

    @Query("UPDATE meter_readings SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteReading(id: String)
    @Query("UPDATE meter_readings SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreReading(id: String)
    @Query("DELETE FROM meter_readings WHERE isDeleted = 1")
    suspend fun emptyDeletedReadingsPermanently()
    @Query("DELETE FROM utility_bills WHERE isDeleted = 1")
    suspend fun emptyDeletedUtilityBillsPermanently()
    @Query("SELECT * FROM meter_readings WHERE isDeleted = 1 ORDER BY readingDate DESC")
    fun getDeletedReadings(): Flow<List<MeterReading>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtilityBill(bill: UtilityBill)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtilityBills(bills: List<UtilityBill>)

    @Query("SELECT * FROM utility_bills WHERE isDeleted = 0 ORDER BY startDate DESC")
    fun getAllUtilityBills(): Flow<List<UtilityBill>>

    @Query("SELECT * FROM utility_bills WHERE isDeleted = 0 ORDER BY startDate DESC")
    suspend fun getAllUtilityBillsSync(): List<UtilityBill>

    @Query("SELECT * FROM utility_bills WHERE isDeleted = 1 ORDER BY startDate DESC")
    fun getDeletedUtilityBills(): Flow<List<UtilityBill>>

    @Query("UPDATE utility_bills SET isDeleted = 1, lastUpdatedMillis = :ts WHERE id = :id")
    suspend fun softDeleteUtilityBill(id: String, ts: Long = System.currentTimeMillis())

    @Query("UPDATE utility_bills SET isDeleted = 0, lastUpdatedMillis = :ts WHERE id = :id")
    suspend fun restoreUtilityBill(id: String, ts: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteReading(reading: MeterReading)

    @Delete
    suspend fun deleteUtilityBill(bill: UtilityBill)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElectricityRate(rate: com.example.data.model.ElectricityRate)

    @Query("SELECT * FROM electricity_rates WHERE isDeleted = 0 ORDER BY effectiveDate DESC")
    fun getAllElectricityRates(): Flow<List<com.example.data.model.ElectricityRate>>

    @Query("SELECT * FROM electricity_rates WHERE isDeleted = 0 ORDER BY effectiveDate DESC")
    suspend fun getAllElectricityRatesSync(): List<com.example.data.model.ElectricityRate>

    @Query("SELECT * FROM electricity_rates WHERE isDeleted = 1 ORDER BY effectiveDate DESC")
    fun getDeletedElectricityRates(): Flow<List<com.example.data.model.ElectricityRate>>

    @Query("UPDATE electricity_rates SET isDeleted = 1, lastUpdatedMillis = :ts WHERE id = :id")
    suspend fun softDeleteElectricityRate(id: String, ts: Long = System.currentTimeMillis())

    @Query("UPDATE electricity_rates SET isDeleted = 0, lastUpdatedMillis = :ts WHERE id = :id")
    suspend fun restoreElectricityRate(id: String, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM electricity_rates WHERE isDeleted = 1")
    suspend fun emptyDeletedElectricityRatesPermanently()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElectricityRates(rates: List<com.example.data.model.ElectricityRate>)

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
    @Query("SELECT * FROM owner_accounts WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllOwnerAccounts(): Flow<List<com.example.data.model.OwnerAccountEntity>>

    @Query("SELECT * FROM owner_accounts WHERE isDeleted = 0 ORDER BY id ASC")
    suspend fun getAllOwnerAccountsSync(): List<com.example.data.model.OwnerAccountEntity>

    @Query("SELECT * FROM owner_accounts WHERE isDeleted = 1 ORDER BY id DESC")
    fun getDeletedOwnerAccounts(): Flow<List<com.example.data.model.OwnerAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnerAccount(account: com.example.data.model.OwnerAccountEntity): Long

    @Update
    suspend fun updateOwnerAccount(account: com.example.data.model.OwnerAccountEntity)

    @Delete
    suspend fun deleteOwnerAccount(account: com.example.data.model.OwnerAccountEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesFlow(): Flow<List<com.example.data.model.ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: com.example.data.model.ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: com.example.data.model.ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: com.example.data.model.ExpenseEntity)
}

@Dao
interface StickyNoteDao {
    @Query("SELECT * FROM sticky_notes WHERE isDeleted = 0 ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllStickyNotes(): Flow<List<StickyNoteEntity>>

    @Query("SELECT * FROM sticky_notes WHERE isDeleted = 0 ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllStickyNotesSync(): List<StickyNoteEntity>

    @Query("SELECT * FROM sticky_notes WHERE isDeleted = 1 ORDER BY lastUpdatedMillis DESC")
    fun getDeletedStickyNotes(): Flow<List<StickyNoteEntity>>

    @Query("DELETE FROM sticky_notes WHERE isDeleted = 1")
    suspend fun emptyDeletedStickyNotesPermanently()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickyNote(note: StickyNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickyNotes(notes: List<StickyNoteEntity>)

    @Update
    suspend fun updateStickyNote(note: StickyNoteEntity)

    @Update
    suspend fun updateStickyNotes(notes: List<StickyNoteEntity>)

    @Query("UPDATE sticky_notes SET isDeleted = 1, lastUpdatedMillis = :timestamp WHERE id = :id")
    suspend fun softDeleteStickyNote(id: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sticky_notes SET isDeleted = 0, lastUpdatedMillis = :timestamp WHERE id = :id")
    suspend fun restoreStickyNote(id: Int, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteStickyNote(note: StickyNoteEntity)
}

@Dao
abstract class SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertHotel(hotel: HotelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertHotels(hotels: List<HotelEntity>): List<Long>

    @Update
    abstract suspend fun updateHotel(hotel: HotelEntity)

    @Update
    abstract suspend fun updateHotels(hotels: List<HotelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertBankAccount(account: BankAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertBankAccounts(accounts: List<BankAccountEntity>): List<Long>

    @Update
    abstract suspend fun updateBankAccount(account: BankAccountEntity)

    @Update
    abstract suspend fun updateBankAccounts(accounts: List<BankAccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOwnerAccount(account: com.example.data.model.OwnerAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOwnerAccounts(accounts: List<com.example.data.model.OwnerAccountEntity>): List<Long>

    @Update
    abstract suspend fun updateOwnerAccount(account: com.example.data.model.OwnerAccountEntity)

    @Update
    abstract suspend fun updateOwnerAccounts(accounts: List<com.example.data.model.OwnerAccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMenuItems(items: List<MenuEntity>)

    @Update
    abstract suspend fun updateMenuItems(items: List<MenuEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransactions(txs: List<HotelTransactionEntity>)

    @Update
    abstract suspend fun updateTransactions(txs: List<HotelTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertExpenses(expenses: List<ExtraExpenseEntity>)

    @Update
    abstract suspend fun updateExpenses(expenses: List<ExtraExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReadings(readings: List<MeterReading>)

    @Update
    abstract suspend fun updateReadings(readings: List<MeterReading>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUtilityBills(bills: List<UtilityBill>)

    @Update
    abstract suspend fun updateUtilityBills(bills: List<UtilityBill>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertHotelPayments(payments: List<HotelPaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertStickyNotes(notes: List<StickyNoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertElectricityRates(rates: List<com.example.data.model.ElectricityRate>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserProfile(profile: UserProfileEntity)

    @androidx.room.Transaction
    open suspend fun executeMergeTransaction(plan: com.example.utils.MergePlan) {
        if (plan.hotelsToInsert.isNotEmpty()) insertHotels(plan.hotelsToInsert)
        if (plan.hotelsToUpdate.isNotEmpty()) updateHotels(plan.hotelsToUpdate)

        if (plan.bankAccountsToInsert.isNotEmpty()) insertBankAccounts(plan.bankAccountsToInsert)
        if (plan.bankAccountsToUpdate.isNotEmpty()) updateBankAccounts(plan.bankAccountsToUpdate)

        if (plan.ownerAccountsToInsert.isNotEmpty()) insertOwnerAccounts(plan.ownerAccountsToInsert)
        if (plan.ownerAccountsToUpdate.isNotEmpty()) updateOwnerAccounts(plan.ownerAccountsToUpdate)

        if (plan.menusToInsert.isNotEmpty()) insertMenuItems(plan.menusToInsert)
        if (plan.menusToUpdate.isNotEmpty()) updateMenuItems(plan.menusToUpdate)

        if (plan.mealsToInsert.isNotEmpty()) insertTransactions(plan.mealsToInsert)
        if (plan.mealsToUpdate.isNotEmpty()) updateTransactions(plan.mealsToUpdate)

        if (plan.expensesToInsert.isNotEmpty()) insertExpenses(plan.expensesToInsert)
        if (plan.expensesToUpdate.isNotEmpty()) updateExpenses(plan.expensesToUpdate)

        if (plan.readingsToInsert.isNotEmpty()) insertReadings(plan.readingsToInsert)
        if (plan.readingsToUpdate.isNotEmpty()) updateReadings(plan.readingsToUpdate)

        if (plan.billsToInsert.isNotEmpty()) insertUtilityBills(plan.billsToInsert)
        if (plan.billsToUpdate.isNotEmpty()) updateUtilityBills(plan.billsToUpdate)

        if (plan.hotelPaymentsToInsert.isNotEmpty()) insertHotelPayments(plan.hotelPaymentsToInsert)
        if (plan.stickyNotesToInsert.isNotEmpty()) insertStickyNotes(plan.stickyNotesToInsert)
        if (plan.electricityRatesToInsert.isNotEmpty()) insertElectricityRates(plan.electricityRatesToInsert)

        if (plan.userProfileToUpdate != null) insertUserProfile(plan.userProfileToUpdate)
    }
}
