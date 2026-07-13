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

    @Query("SELECT * FROM menu_items WHERE hotelId = :hotelId AND isDeleted = 1 ORDER BY id ASC")
    fun getDeletedMenuForHotel(hotelId: Int): Flow<List<MenuEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(menuItems: List<MenuEntity>)

    @Query("DELETE FROM menu_items")
    suspend fun deleteAllMenuItems()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM hotel_transactions WHERE hotelId = :hotelId ORDER BY timestamp DESC")
    fun getTransactionsForHotel(hotelId: Int): Flow<List<HotelTransactionEntity>>
    
    @Query("SELECT * FROM hotel_transactions WHERE hotelId = :hotelId AND timestamp >= :start AND timestamp <= :end")
    suspend fun getTransactionsForHotelAndDateRange(hotelId: Int, start: Long, end: Long): List<HotelTransactionEntity>

    @Query("SELECT * FROM hotel_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<HotelTransactionEntity>>

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
    @Query("SELECT * FROM extra_expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExtraExpenseEntity>>
    
    @Query("SELECT * FROM extra_expenses WHERE timestamp >= :start AND timestamp <= :end")
    suspend fun getExpensesForDateRange(start: Long, end: Long): List<ExtraExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExtraExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExtraExpenseEntity)
}

@Dao
interface MeterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReading)

    @Query("SELECT * FROM meter_readings ORDER BY readingDate DESC")
    fun getAllReadings(): Flow<List<MeterReading>>

    @Query("SELECT COALESCE(SUM(unitsConsumed), 0.0) FROM meter_readings WHERE readingDate >= :start AND readingDate <= :end")
    fun getSumOfUnitsConsumed(start: Long, end: Long): Flow<Double>

    @Query("UPDATE meter_readings SET isPaid = :isPaid WHERE readingDate >= :start AND readingDate <= :end")
    suspend fun updateReadingsPaidStatus(start: Long, end: Long, isPaid: Boolean)

    @Query("SELECT * FROM meter_readings ORDER BY readingDate DESC LIMIT 1")
    fun getLatestReading(): Flow<MeterReading?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtilityBill(bill: UtilityBill)

    @Query("SELECT * FROM utility_bills ORDER BY startDate DESC")
    fun getAllUtilityBills(): Flow<List<UtilityBill>>

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
