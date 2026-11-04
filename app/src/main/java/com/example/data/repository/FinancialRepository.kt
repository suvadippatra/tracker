package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.model.BankAccountEntity
import com.example.data.model.ExtraExpenseEntity
import com.example.data.model.HotelEntity
import com.example.data.model.HotelTransactionEntity
import com.example.data.model.MenuEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.HotelPaymentEntity
import com.example.data.model.MeterReading
import com.example.data.model.StickyNoteEntity
import com.example.data.model.UtilityBill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FinancialRepository(private val db: AppDatabase) {

    private val userProfileDao = db.userProfileDao()
    private val hotelDao = db.hotelDao()
    private val menuDao = db.menuDao()
    private val transactionDao = db.transactionDao()
    private val bankAccountDao = db.bankAccountDao()
    private val extraExpenseDao = db.extraExpenseDao()
    private val meterDao = db.meterDao()
    private val categoryDao = db.transactionCategoryDao()
    private val attachmentDao = db.attachmentDao()
    private val ownerAccountDao = db.ownerAccountDao()
    private val hotelPaymentDao = db.hotelPaymentDao()
    private val stickyNoteDao = db.stickyNoteDao()

    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getUserProfile()
    val allHotels: Flow<List<HotelEntity>> = hotelDao.getAllHotels()
    val deletedHotels: Flow<List<HotelEntity>> = hotelDao.getDeletedHotels()
    val allBankAccounts: Flow<List<BankAccountEntity>> = bankAccountDao.getAllBankAccounts()
    val deletedBankAccounts: Flow<List<BankAccountEntity>> = bankAccountDao.getDeletedBankAccounts()
    val allExtraExpenses: Flow<List<ExtraExpenseEntity>> = extraExpenseDao.getAllExpenses()
    val allTransactions: Flow<List<HotelTransactionEntity>> = transactionDao.getAllTransactions()
    val allMenuItems: Flow<List<MenuEntity>> = menuDao.getAllMenuItems()
    val allCategories: Flow<List<com.example.data.model.TransactionCategoryEntity>> = categoryDao.getAllCategories()
    val allOwnerAccounts: Flow<List<com.example.data.model.OwnerAccountEntity>> = ownerAccountDao.getAllOwnerAccounts()
    val allHotelPayments: Flow<List<HotelPaymentEntity>> = hotelPaymentDao.getAllPayments()
    val allStickyNotes: Flow<List<StickyNoteEntity>> = stickyNoteDao.getAllStickyNotes()

    val deletedTransactions: Flow<List<HotelTransactionEntity>> = transactionDao.getDeletedTransactions()
    val deletedExtraExpenses: Flow<List<ExtraExpenseEntity>> = extraExpenseDao.getDeletedExpenses()
    val deletedMeterReadings: Flow<List<MeterReading>> = meterDao.getDeletedReadings()
    val deletedUtilityBills: Flow<List<UtilityBill>> = meterDao.getDeletedUtilityBills()
    val deletedStickyNotes: Flow<List<StickyNoteEntity>> = stickyNoteDao.getDeletedStickyNotes()

    suspend fun emptyDeletedTransactionsPermanently() = transactionDao.emptyDeletedTransactionsPermanently()
    suspend fun emptyDeletedExpensesPermanently() = extraExpenseDao.emptyDeletedExpensesPermanently()
    suspend fun emptyDeletedReadingsPermanently() = meterDao.emptyDeletedReadingsPermanently()
    suspend fun emptyDeletedUtilityBillsPermanently() = meterDao.emptyDeletedUtilityBillsPermanently()
    suspend fun emptyDeletedStickyNotesPermanently() = stickyNoteDao.emptyDeletedStickyNotesPermanently()
    suspend fun restoreReading(id: String) = meterDao.restoreReading(id)

    suspend fun insertStickyNote(note: StickyNoteEntity): Long = stickyNoteDao.insertStickyNote(note)
    suspend fun insertStickyNotes(notes: List<StickyNoteEntity>) = stickyNoteDao.insertStickyNotes(notes)
    suspend fun updateStickyNote(note: StickyNoteEntity) = stickyNoteDao.updateStickyNote(note)
    suspend fun updateStickyNotes(notes: List<StickyNoteEntity>) = stickyNoteDao.updateStickyNotes(notes)
    suspend fun softDeleteStickyNote(id: Int) = stickyNoteDao.softDeleteStickyNote(id)
    suspend fun restoreStickyNote(id: Int) = stickyNoteDao.restoreStickyNote(id)
    suspend fun deleteStickyNotePermanently(note: StickyNoteEntity) = stickyNoteDao.deleteStickyNote(note)

    fun getAttachmentsForExpense(expenseId: Int): Flow<List<com.example.data.model.AttachmentEntity>> = attachmentDao.getAttachmentsForExpense(expenseId)

    suspend fun insertAttachment(attachment: com.example.data.model.AttachmentEntity) = attachmentDao.insertAttachment(attachment)
    suspend fun deleteAttachment(attachment: com.example.data.model.AttachmentEntity) = attachmentDao.deleteAttachment(attachment)
    suspend fun deleteAttachmentsForExpense(expenseId: Int) = attachmentDao.deleteAttachmentsForExpense(expenseId)

    fun getPaymentsForHotel(hotelId: Int) = hotelPaymentDao.getPaymentsForHotel(hotelId)
    suspend fun insertHotelPayment(payment: com.example.data.model.HotelPaymentEntity) = hotelPaymentDao.insertPayment(payment)
    suspend fun deleteHotelPayment(payment: com.example.data.model.HotelPaymentEntity) = hotelPaymentDao.deletePayment(payment)

    suspend fun insertOwnerAccount(account: com.example.data.model.OwnerAccountEntity): Long = ownerAccountDao.insertOwnerAccount(account)
    suspend fun updateOwnerAccount(account: com.example.data.model.OwnerAccountEntity) = ownerAccountDao.updateOwnerAccount(account)
    suspend fun deleteOwnerAccount(account: com.example.data.model.OwnerAccountEntity) = ownerAccountDao.deleteOwnerAccount(account)
    suspend fun getAllOwnerAccountsSync(): List<com.example.data.model.OwnerAccountEntity> = ownerAccountDao.getAllOwnerAccountsSync()
    suspend fun getAllBankAccountsSync(): List<BankAccountEntity> = bankAccountDao.getAllBankAccountsSync()
    suspend fun getBankAccountByIdSync(id: Int): BankAccountEntity? = bankAccountDao.getBankAccountByIdSync(id)

    val allMeterReadings: Flow<List<MeterReading>> = meterDao.getAllReadings()
    val allMeterReadingsSorted: Flow<List<MeterReading>> = meterDao.getAllReadingsSorted()
    val allUtilityBills: Flow<List<UtilityBill>> = meterDao.getAllUtilityBills()
    val allElectricityRates: Flow<List<com.example.data.model.ElectricityRate>> = meterDao.getAllElectricityRates()
    val latestReading: Flow<MeterReading?> = meterDao.getLatestReading()

    fun getSumOfUnitsConsumed(start: Long, end: Long): Flow<Double> = meterDao.getSumOfUnitsConsumed(start, end)

    suspend fun updateReadingsPaidStatus(start: Long, end: Long, isPaid: Boolean) = meterDao.updateReadingsPaidStatus(start, end, isPaid)

    suspend fun insertReading(reading: MeterReading) = meterDao.insertReading(reading)
    suspend fun insertUtilityBill(bill: UtilityBill) = meterDao.insertUtilityBill(bill)
    suspend fun deleteReading(reading: MeterReading) = meterDao.deleteReading(reading)
    suspend fun softDeleteReading(id: String) = meterDao.softDeleteReading(id)
    suspend fun deleteUtilityBill(bill: UtilityBill) = meterDao.deleteUtilityBill(bill)
    suspend fun insertElectricityRate(rate: com.example.data.model.ElectricityRate) = meterDao.insertElectricityRate(rate)
    suspend fun deleteElectricityRate(rate: com.example.data.model.ElectricityRate) = meterDao.deleteElectricityRate(rate)

    suspend fun insertCategory(category: com.example.data.model.TransactionCategoryEntity) = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: com.example.data.model.TransactionCategoryEntity) = categoryDao.deleteCategory(category)

    fun getMenuForHotel(hotelId: Int): Flow<List<MenuEntity>> {
        return menuDao.getMenuForHotel(hotelId)
    }

    fun getDeletedMenuForHotel(hotelId: Int): Flow<List<MenuEntity>> {
        return menuDao.getDeletedMenuForHotel(hotelId)
    }

    fun getTransactionsForHotel(hotelId: Int): Flow<List<HotelTransactionEntity>> {
        return transactionDao.getTransactionsForHotel(hotelId)
    }

    fun getHotelById(hotelId: Int): Flow<HotelEntity?> {
        return hotelDao.getHotelById(hotelId)
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    suspend fun insertMenu(menu: MenuEntity) {
        withContext(Dispatchers.IO) {
            menuDao.insertMenu(menu)
        }
    }
    suspend fun updateMenu(menu: MenuEntity) {
        withContext(Dispatchers.IO) {
            menuDao.updateMenu(menu)
        }
    }
    suspend fun deleteMenu(menu: MenuEntity) {
        withContext(Dispatchers.IO) {
            menuDao.deleteMenu(menu)
        }
    }

    suspend fun insertHotel(hotel: HotelEntity): Long {
        return hotelDao.insertHotel(hotel)
    }

    suspend fun updateHotel(hotel: HotelEntity) {
        hotelDao.updateHotel(hotel)
    }

    suspend fun deleteHotel(hotel: HotelEntity) {
        hotelDao.deleteHotel(hotel)
    }

    suspend fun insertTransaction(transaction: HotelTransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: HotelTransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: HotelTransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun insertBankAccount(account: BankAccountEntity): Long {
        return bankAccountDao.insertBankAccount(account)
    }

    suspend fun updateBankAccount(account: BankAccountEntity) {
        bankAccountDao.updateBankAccount(account)
    }

    suspend fun deleteBankAccount(account: BankAccountEntity) {
        bankAccountDao.deleteBankAccount(account)
    }

    suspend fun insertExpense(expense: ExtraExpenseEntity): Long {
        return extraExpenseDao.insertExpense(expense)
    }

    suspend fun insertExpenseAndDeduct(expense: ExtraExpenseEntity, account: BankAccountEntity): Long {
        val id = extraExpenseDao.insertExpense(expense)
        val change = if (expense.type == "INCOME") expense.amount else -expense.amount
        val updatedAccount = account.copy(balance = account.balance + change)
        bankAccountDao.updateBankAccount(updatedAccount)
        return id
    }

    suspend fun softDeleteExpenseAndRevert(expense: ExtraExpenseEntity, accounts: List<BankAccountEntity>) {
        extraExpenseDao.insertExpense(expense.copy(isDeleted = true))
        accounts.find { it.id == expense.bankAccountId }?.let { account ->
            val change = if (expense.type == "INCOME") -expense.amount else expense.amount
            val updatedAccount = account.copy(balance = account.balance + change)
            bankAccountDao.updateBankAccount(updatedAccount)
        }
    }

    suspend fun restoreExpenseAndApply(expense: ExtraExpenseEntity, accounts: List<BankAccountEntity>) {
        extraExpenseDao.insertExpense(expense.copy(isDeleted = false))
        accounts.find { it.id == expense.bankAccountId }?.let { account ->
            val change = if (expense.type == "INCOME") expense.amount else -expense.amount
            val updatedAccount = account.copy(balance = account.balance + change)
            bankAccountDao.updateBankAccount(updatedAccount)
        }
    }

    suspend fun deleteExpensePermanently(expense: ExtraExpenseEntity) {
        extraExpenseDao.deleteExpense(expense)
    }

    suspend fun initializeDatabaseIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        try {
            val profile = userProfileDao.getUserProfileSync()
            if (profile == null) {
                userProfileDao.insertOrUpdateProfile(UserProfileEntity())
            }
        } catch (e: Exception) {
            Log.e("FinancialRepository", "Error initializing database", e)
        }
    }
}
