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
import com.example.data.model.MeterReading
import com.example.data.model.UtilityBill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

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

    fun getAttachmentsForExpense(expenseId: Int): Flow<List<com.example.data.model.AttachmentEntity>> = attachmentDao.getAttachmentsForExpense(expenseId)

    suspend fun insertAttachment(attachment: com.example.data.model.AttachmentEntity) = attachmentDao.insertAttachment(attachment)
    suspend fun deleteAttachment(attachment: com.example.data.model.AttachmentEntity) = attachmentDao.deleteAttachment(attachment)
    suspend fun deleteAttachmentsForExpense(expenseId: Int) = attachmentDao.deleteAttachmentsForExpense(expenseId)

    fun getPaymentsForHotel(hotelId: Int) = hotelPaymentDao.getPaymentsForHotel(hotelId)
    suspend fun insertHotelPayment(payment: com.example.data.model.HotelPaymentEntity) = hotelPaymentDao.insertPayment(payment)
    suspend fun deleteHotelPayment(payment: com.example.data.model.HotelPaymentEntity) = hotelPaymentDao.deletePayment(payment)

    suspend fun insertOwnerAccount(account: com.example.data.model.OwnerAccountEntity) = ownerAccountDao.insertOwnerAccount(account)
    suspend fun updateOwnerAccount(account: com.example.data.model.OwnerAccountEntity) = ownerAccountDao.updateOwnerAccount(account)
    suspend fun deleteOwnerAccount(account: com.example.data.model.OwnerAccountEntity) = ownerAccountDao.deleteOwnerAccount(account)

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
            // 1. Verify User Profile
            val profile = userProfileDao.getUserProfileSync()
            if (profile == null) {
                userProfileDao.insertOrUpdateProfile(UserProfileEntity())
            }
        } catch (e: Exception) {
            Log.e("FinancialRepository", "Error initializing database", e)
        }
    }

    private fun normalizeName(name: String): String {
        val lower = name.lowercase().trim()
        if (lower.startsWith("thali rice & veg")) return "thali rice & veg"
        return lower
    }

    private fun extractEnglishName(text: String): String {
        val openParen = text.indexOf('(')
        val closeParen = text.indexOf(')')
        if (openParen != -1 && closeParen != -1 && closeParen > openParen) {
            return text.substring(openParen + 1, closeParen).trim()
        }
        return text.trim()
    }

    private fun getEmojiAndTypeForName(englishName: String): Pair<String, String> {
        val lower = englishName.lowercase()
        return when {
            lower.contains("plain rice") || lower.contains("only rice") -> Pair("🍚", "Rice")
            lower.contains("chicken") -> Pair("🍗", "Chicken")
            lower.contains("fish") -> Pair("🐟", "Fish")
            lower.contains("egg") || lower.contains("omelette") -> Pair("🍳", "Egg")
            lower.contains("thali") || lower.contains("veg thali") -> Pair("🍛", "Thali")
            lower.contains("veg") -> Pair("🥗", "Veg")
            lower.contains("curd") -> Pair("🥛", "Curd")
            lower.contains("khichuri") -> Pair("🥣", "Khichuri")
            lower.contains("pulao") -> Pair("🍚", "Pulao")
            lower.contains("roti") || lower.contains("luchi") || lower.contains("paratha") -> Pair("🫓", "Breads")
            else -> Pair("🍽️", "Common")
        }
    }

    private suspend fun parseCsvMenu(context: Context): List<MenuEntity> = withContext(Dispatchers.IO) {
        val menuItems = mutableListOf<MenuEntity>()
        val generalItems = mutableMapOf<String, Triple<String, String, Double>>() // NormalizedName -> (Bangla, English, Price)
        val dayItems = mutableSetOf<String>() // Normalized Names
        val nightItems = mutableSetOf<String>() // Normalized Names

        try {
            context.assets.open("Smriti_Hotel_Menu.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    var line: String?
                    var currentSection = 0 // 0 = General, 1 = Day Menu, 2 = Night Menu

                    while (reader.readLine().also { line = it } != null) {
                        val row = line ?: continue
                        if (row.isBlank()) continue

                        val parts = splitCsvRow(row)
                        if (parts.isEmpty()) continue

                        val firstCell = parts.getOrNull(0)?.trim() ?: ""
                        val secondCell = parts.getOrNull(1)?.trim() ?: ""
                        val thirdCell = parts.getOrNull(2)?.trim() ?: ""

                        // Detect sections
                        if (firstCell.contains("Day Menu") || secondCell.contains("Day Menu")) {
                            currentSection = 1
                        } else if (firstCell.contains("Night Menu") || secondCell.contains("Night Menu")) {
                            currentSection = 2
                        }

                        when (currentSection) {
                            0 -> {
                                // Parse general items: বাংলা,English,Price (₹),,,,
                                // e.g. নর্মাল ভাত,Plain Rice,35,,,,
                                if (firstCell.isNotBlank() && secondCell.isNotBlank() && !firstCell.startsWith("বাংলা")) {
                                    val price = thirdCell.toDoubleOrNull()
                                    if (price != null) {
                                        generalItems[normalizeName(secondCell)] = Triple(firstCell, secondCell, price)
                                    }
                                }
                            }
                            1 -> {
                                // Day Menu: ,নর্মাল ভাত (Plain Rice),,,,,,
                                val itemCell = if (secondCell.isNotBlank()) secondCell else firstCell
                                if (itemCell.isNotBlank() && !itemCell.contains("Day Menu")) {
                                    val extracted = extractEnglishName(itemCell)
                                    if (extracted.isNotBlank()) {
                                        dayItems.add(normalizeName(extracted))
                                    }
                                }
                            }
                            2 -> {
                                // Night Menu: ,রুটি (Roti),,,,,,
                                val itemCell = if (secondCell.isNotBlank()) secondCell else firstCell
                                if (itemCell.isNotBlank() && !itemCell.contains("Night Menu")) {
                                    val extracted = extractEnglishName(itemCell)
                                    if (extracted.isNotBlank()) {
                                        nightItems.add(normalizeName(extracted))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Map general items to their categorized day/night/add-on roles
            val addOnNames = setOf(
                "chicken piece", "fish piece", "egg curry", "omelette",
                "thali extras", "thali vegs", "normal vegs", "curd"
            )

            // Combine all unique item names from Day and Night lists
            val allUniqueNames = (dayItems + nightItems).toSet()

            for (normName in allUniqueNames) {
                val generalInfo = generalItems[normName] ?: generalItems[normalizeName(normName)]
                if (generalInfo != null) {
                    val bangla = generalInfo.first
                    val english = generalInfo.second
                    val price = generalInfo.third
                    val isDay = dayItems.contains(normName) || dayItems.contains(normalizeName(english))
                    val isNight = nightItems.contains(normName) || nightItems.contains(normalizeName(english))
                    val isAddOn = addOnNames.contains(normalizeName(english))
                    val (emoji, type) = getEmojiAndTypeForName(english)

                    menuItems.add(
                        MenuEntity(
                            banglaName = bangla,
                            englishName = english,
                            price = price,
                            isDayMenu = isDay,
                            isNightMenu = isNight,
                            isAddOn = isAddOn,
                            emoji = emoji,
                            type = type
                        )
                    )
                }
            }

            // In case some items from general list are missing in sections, let's include them based on basic rules
            if (menuItems.isEmpty() && generalItems.isNotEmpty()) {
                for ((norm, triple) in generalItems) {
                    val isAddOn = addOnNames.contains(norm)
                    val (emoji, type) = getEmojiAndTypeForName(triple.second)
                    // Defaults
                    menuItems.add(
                        MenuEntity(
                            banglaName = triple.first,
                            englishName = triple.second,
                            price = triple.third,
                            isDayMenu = !norm.contains("roti") && !norm.contains("luchi") && !norm.contains("aloo paratha"),
                            isNightMenu = norm.contains("roti") || norm.contains("luchi") || norm.contains("aloo paratha") || norm.contains("chicken") || norm.contains("fish") || norm.contains("egg") || norm.contains("omelette"),
                            isAddOn = isAddOn,
                            emoji = emoji,
                            type = type
                        )
                    )
                }
            }

        } catch (e: Exception) {
            Log.e("FinancialRepository", "Error parsing menu CSV", e)
        }

        return@withContext if (menuItems.isEmpty()) getFallbackMenu() else menuItems
    }

    private fun splitCsvRow(row: String): List<String> {
        val result = mutableListOf<String>()
        val current = java.lang.StringBuilder()
        var inQuotes = false
        for (ch in row) {
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    private fun getFallbackMenu(): List<MenuEntity> {
        return listOf(
            MenuEntity(banglaName = "নর্মাল ভাত", englishName = "Plain Rice", price = 35.0, isDayMenu = true, isNightMenu = false, isAddOn = false),
            MenuEntity(banglaName = "চিকেন পিস", englishName = "Chicken Piece", price = 20.0, isDayMenu = true, isNightMenu = true, isAddOn = true),
            MenuEntity(banglaName = "মাছ পিস", englishName = "Fish Piece", price = 20.0, isDayMenu = true, isNightMenu = true, isAddOn = true),
            MenuEntity(banglaName = "ডিম কারি", englishName = "Egg Curry", price = 15.0, isDayMenu = true, isNightMenu = true, isAddOn = true),
            MenuEntity(banglaName = "ওমলেট", englishName = "Omelette", price = 15.0, isDayMenu = false, isNightMenu = true, isAddOn = true),
            MenuEntity(banglaName = "নর্মাল ভাত + থালি ডাল-সবজি", englishName = "Rice + Thali Vegs", price = 50.0, isDayMenu = true, isNightMenu = false, isAddOn = false),
            MenuEntity(banglaName = "থালি ভাত + থালি ডাল-সবজি", englishName = "Thali Rice & Veg", price = 60.0, isDayMenu = true, isNightMenu = false, isAddOn = false),
            MenuEntity(banglaName = "ভেজ থালি", englishName = "Veg Thali", price = 70.0, isDayMenu = true, isNightMenu = false, isAddOn = false),
            MenuEntity(banglaName = "থালির ভাজাভুজি", englishName = "Thali Extras", price = 20.0, isDayMenu = true, isNightMenu = false, isAddOn = true),
            MenuEntity(banglaName = "থালি সবজি", englishName = "Thali Vegs", price = 20.0, isDayMenu = true, isNightMenu = false, isAddOn = true),
            MenuEntity(banglaName = "নর্মাল সবজি", englishName = "Normal Vegs", price = 10.0, isDayMenu = true, isNightMenu = false, isAddOn = true),
            MenuEntity(banglaName = "দই", englishName = "Curd", price = 20.0, isDayMenu = true, isNightMenu = false, isAddOn = true),
            MenuEntity(banglaName = "খিচুড়ি", englishName = "Khichuri", price = 40.0, isDayMenu = true, isNightMenu = false, isAddOn = false),
            MenuEntity(banglaName = "পোলাউ", englishName = "Pulao", price = 60.0, isDayMenu = true, isNightMenu = true, isAddOn = false),
            MenuEntity(banglaName = "ফ্রাইড রাইস + চিকেন", englishName = "Fried Rice + Chicken", price = 80.0, isDayMenu = true, isNightMenu = true, isAddOn = false),
            MenuEntity(banglaName = "রুটি", englishName = "Roti", price = 5.0, isDayMenu = false, isNightMenu = true, isAddOn = false),
            MenuEntity(banglaName = "নুচি", englishName = "Luchi", price = 5.0, isDayMenu = false, isNightMenu = true, isAddOn = false),
            MenuEntity(banglaName = "আলু পরোটা", englishName = "Aloo Paratha", price = 15.0, isDayMenu = false, isNightMenu = true, isAddOn = false)
        )
    }
}
