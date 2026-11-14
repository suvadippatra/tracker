package com.example.data

import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

sealed class ImportOutcome {
    data class Success(val message: String, val count: Int = 0, val newHotelId: Int? = null) : ImportOutcome()
    data class Error(val message: String) : ImportOutcome()
}

enum class BackupDataType(val displayName: String, val category: String) {
    HOTEL_WHOLE("Whole Hotel Data", "Hotel"),
    HOTEL_INFO("Hotel Profile Info", "Hotel"),
    HOTEL_MENUS("Hotel Menus", "Hotel"),
    HOTEL_HISTORY("Hotel Order History", "Hotel"),
    WALLET_WHOLE("Whole Accounts & Transactions", "Transactions"),
    SINGLE_BANK_ACCOUNT("Single Bank Account Complete Data", "Transactions"),
    BANK_ACCOUNTS("Bank Account Profiles", "Transactions"),
    TRANSACTION_HISTORY("Transaction History (Expenses / Income)", "Transactions"),
    ELECTRICITY_WHOLE("Whole Electricity Data", "Electricity"),
    METER_READINGS("Electricity Datewise Readings", "Electricity"),
    UTILITY_BILLS("Electricity Paid Bill List", "Electricity"),
    ELECTRICITY_RATES("Electricity Rate Policies", "Electricity"),
    SETTINGS("Settings & Preferences Only", "Settings"),
    FULL_BACKUP("Full Application Backup", "Full")
}

class DataTransferManager(private val db: AppDatabase) {

    private val hotelDao = db.hotelDao()
    private val menuDao = db.menuDao()
    private val transactionDao = db.transactionDao()
    private val bankAccountDao = db.bankAccountDao()
    private val extraExpenseDao = db.extraExpenseDao()
    private val meterDao = db.meterDao()
    private val userProfileDao = db.userProfileDao()
    private val ownerAccountDao = db.ownerAccountDao()
    private val transactionCategoryDao = db.transactionCategoryDao()
    private val hotelPaymentDao = db.hotelPaymentDao()
    private val stickyNoteDao = db.stickyNoteDao()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // ==========================================
    // JSON EXPORTS
    // ==========================================

    suspend fun exportWholeHotelJson(hotelId: Int, includeQrMedia: Boolean = true): String = withContext(Dispatchers.IO) {
        val hotel = hotelDao.getHotelByIdSync(hotelId) ?: throw IllegalArgumentException("Hotel not found")
        val menus = menuDao.getMenusForHotelSync(hotelId)
        val transactions = transactionDao.getTransactionsForHotelSync(hotelId)
        val payments = hotelPaymentDao.getAllPaymentsSync().filter { it.hotelId == hotelId && !it.isDeleted }

        val profile = userProfileDao.getUserProfileSync()
        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.HOTEL_WHOLE.name)
            put("exportDate", System.currentTimeMillis())
            if (!profile?.name.isNullOrBlank()) {
                put("senderName", profile?.name)
            }
            put("hotel", JSONObject().apply {
                put("id", hotel.id)
                put("name", hotel.name)
                put("advancedPayment", hotel.advancedPayment)
                put("phoneNumber", hotel.phoneNumber)
                put("location", hotel.location)
                put("upiId", hotel.upiId)
                put("qrCodeUri", hotel.qrCodeUri)
                QrMediaCodec.appendQrDataToJson(this, AppDatabase.getAppContext(), hotel.qrCodeUri, includeQrPayloads = includeQrMedia)
                put("notes", hotel.notes)
                put("whatsappNumber", hotel.whatsappNumber)
            })

            val menuArray = JSONArray()
            menus.forEach { m ->
                menuArray.put(JSONObject().apply {
                    put("banglaName", m.banglaName)
                    put("englishName", m.englishName)
                    put("price", m.price)
                    put("isDayMenu", m.isDayMenu)
                    put("isNightMenu", m.isNightMenu)
                    put("isAddOn", m.isAddOn)
                    put("emoji", m.emoji)
                    put("type", m.type)
                })
            }
            put("menus", menuArray)

            val txArray = JSONArray()
            transactions.forEach { t ->
                txArray.put(formatTransactionToJson(t))
            }
            put("transactions", txArray)

            if (payments.isNotEmpty()) {
                val payArray = JSONArray()
                payments.forEach { p ->
                    payArray.put(JSONObject().apply {
                        put("hotelId", p.hotelId)
                        put("year", p.year)
                        put("month", p.month)
                        put("amountPaid", p.amountPaid)
                        put("paymentDate", p.paymentDate)
                        put("note", p.note)
                    })
                }
                put("hotelPayments", payArray)
            }
        }
        root.toString()
    }

    suspend fun exportHotelInfoJson(hotelId: Int, includeQrMedia: Boolean = true): String = withContext(Dispatchers.IO) {
        val hotel = hotelDao.getHotelByIdSync(hotelId) ?: throw IllegalArgumentException("Hotel not found")
        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.HOTEL_INFO.name)
            put("exportDate", System.currentTimeMillis())
            put("hotel", JSONObject().apply {
                put("id", hotel.id)
                put("name", hotel.name)
                put("advancedPayment", hotel.advancedPayment)
                put("phoneNumber", hotel.phoneNumber)
                put("location", hotel.location)
                put("upiId", hotel.upiId)
                put("qrCodeUri", hotel.qrCodeUri)
                QrMediaCodec.appendQrDataToJson(this, AppDatabase.getAppContext(), hotel.qrCodeUri, includeQrPayloads = includeQrMedia)
                put("notes", hotel.notes)
                put("whatsappNumber", hotel.whatsappNumber)
            })
        }
        root.toString(2)
    }

    suspend fun exportHotelMenusJson(hotelId: Int): String = withContext(Dispatchers.IO) {
        val menus = menuDao.getMenusForHotelSync(hotelId)
        val root = JSONObject().apply {
            put("version", 2)
            put("dataType", BackupDataType.HOTEL_MENUS.name)
            put("hotelId", hotelId)
            put("exportDate", System.currentTimeMillis())
            val arr = JSONArray()
            menus.forEach { m ->
                arr.put(JSONObject().apply {
                    put("banglaName", m.banglaName)
                    put("englishName", m.englishName)
                    put("price", m.price)
                    put("isDayMenu", m.isDayMenu)
                    put("isNightMenu", m.isNightMenu)
                    put("isAddOn", m.isAddOn)
                    put("emoji", m.emoji)
                    put("type", m.type)
                })
            }
            put("menus", arr)
        }
        root.toString(2)
    }

    suspend fun exportHotelHistoryJson(hotelId: Int): String = withContext(Dispatchers.IO) {
        val transactions = transactionDao.getTransactionsForHotelSync(hotelId)
        val payments = hotelPaymentDao.getAllPaymentsSync().filter { it.hotelId == hotelId && !it.isDeleted }
        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.HOTEL_HISTORY.name)
            put("hotelId", hotelId)
            put("exportDate", System.currentTimeMillis())
            val arr = JSONArray()
            transactions.forEach { t ->
                arr.put(formatTransactionToJson(t))
            }
            put("transactions", arr)
            if (payments.isNotEmpty()) {
                val payArray = JSONArray()
                payments.forEach { p ->
                    payArray.put(JSONObject().apply {
                        put("hotelId", p.hotelId)
                        put("year", p.year)
                        put("month", p.month)
                        put("amountPaid", p.amountPaid)
                        put("paymentDate", p.paymentDate)
                        put("note", p.note)
                    })
                }
                put("hotelPayments", payArray)
            }
        }
        root.toString(2)
    }

    private fun formatTransactionToJson(t: HotelTransactionEntity): JSONObject {
        // Export only independent base fields; derived date/time strings and expanded mealGroups
        // subtotals are calculated by the app from timestamp and itemsSerialized.
        return JSONObject().apply {
            put("id", t.id)
            put("hotelId", t.hotelId)
            put("timestamp", t.timestamp)
            put("isNight", t.isNight)
            put("totalAmount", t.totalAmount)
            put("itemsSerialized", t.itemsSerialized)
        }
    }

    suspend fun exportWalletWholeJson(bankAccountId: Int? = null, includeQrMedia: Boolean = true): String = withContext(Dispatchers.IO) {
        if (bankAccountId != null) {
            return@withContext exportSingleBankAccountJson(bankAccountId, includeQrMedia = includeQrMedia)
        }
        val accounts = bankAccountDao.getAllBankAccountsSync()
        val expenses = extraExpenseDao.getAllExpensesSync()
        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.WALLET_WHOLE.name)
            put("exportDate", System.currentTimeMillis())
            val bankArr = JSONArray()
            accounts.forEach { b ->
                bankArr.put(JSONObject().apply {
                    put("id", b.id)
                    put("name", b.name)
                    put("initialBalance", b.initialBalance)
                    put("balance", b.balance)
                    put("details", b.details)
                    put("qrCodeUri", b.qrCodeUri)
                    QrMediaCodec.appendQrDataToJson(this, AppDatabase.getAppContext(), b.qrCodeUri, includeQrPayloads = includeQrMedia)
                    put("upiId", b.upiId)
                    put("accountNumber", b.accountNumber)
                    put("ifsc", b.ifsc)
                    put("branchName", b.branchName)
                    put("notes", b.notes)
                    put("phoneNumber", b.phoneNumber)
                    put("whatsappNumber", b.whatsappNumber)
                    put("email", b.email)
                })
            }
            put("bankAccounts", bankArr)

            val expArr = JSONArray()
            expenses.forEach { e ->
                expArr.put(JSONObject().apply {
                    put("timestamp", e.timestamp)
                    put("name", e.name)
                    put("amount", e.amount)
                    put("bankAccountId", e.bankAccountId)
                    put("type", e.type)
                    put("category", e.category)
                    put("notes", e.notes)
                })
            }
            put("expenses", expArr)
        }
        root.toString()
    }

    suspend fun exportBankAccountsJson(bankAccountId: Int? = null, includeQrMedia: Boolean = true): String = withContext(Dispatchers.IO) {
        val accounts = if (bankAccountId != null) {
            bankAccountDao.getBankAccountByIdSync(bankAccountId)?.let { listOf(it) } ?: emptyList()
        } else {
            bankAccountDao.getAllBankAccountsSync()
        }
        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.BANK_ACCOUNTS.name)
            if (bankAccountId != null) put("bankAccountId", bankAccountId)
            put("exportDate", System.currentTimeMillis())
            val arr = JSONArray()
            accounts.forEach { a ->
                arr.put(JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("initialBalance", a.initialBalance)
                    put("balance", a.balance)
                    put("details", a.details)
                    put("qrCodeUri", a.qrCodeUri)
                    QrMediaCodec.appendQrDataToJson(this, AppDatabase.getAppContext(), a.qrCodeUri, includeQrPayloads = includeQrMedia)
                    put("upiId", a.upiId)
                    put("accountNumber", a.accountNumber)
                    put("ifsc", a.ifsc)
                    put("branchName", a.branchName)
                    put("notes", a.notes)
                    put("phoneNumber", a.phoneNumber)
                    put("whatsappNumber", a.whatsappNumber)
                    put("email", a.email)
                })
            }
            put("bankAccounts", arr)
        }
        root.toString()
    }

    suspend fun exportSingleBankAccountJson(bankAccountId: Int, includeQrMedia: Boolean = true): String = withContext(Dispatchers.IO) {
        val account = bankAccountDao.getBankAccountByIdSync(bankAccountId) ?: throw IllegalArgumentException("Bank account not found")
        val expenses = extraExpenseDao.getAllExpensesSync().filter { it.bankAccountId == bankAccountId }
        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.SINGLE_BANK_ACCOUNT.name)
            put("exportDate", System.currentTimeMillis())
            put("bankAccount", JSONObject().apply {
                put("id", account.id)
                put("name", account.name)
                put("initialBalance", account.initialBalance)
                put("balance", account.balance)
                put("details", account.details)
                put("qrCodeUri", account.qrCodeUri)
                QrMediaCodec.appendQrDataToJson(this, AppDatabase.getAppContext(), account.qrCodeUri, includeQrPayloads = includeQrMedia)
                put("upiId", account.upiId)
                put("accountNumber", account.accountNumber)
                put("ifsc", account.ifsc)
                put("branchName", account.branchName)
                put("notes", account.notes)
                put("phoneNumber", account.phoneNumber)
                put("whatsappNumber", account.whatsappNumber)
                put("email", account.email)
            })
            val arr = JSONArray()
            expenses.forEach { e ->
                arr.put(JSONObject().apply {
                    put("timestamp", e.timestamp)
                    put("name", e.name)
                    put("amount", e.amount)
                    put("bankAccountId", e.bankAccountId)
                    put("type", e.type)
                    put("category", e.category)
                    put("notes", e.notes)
                })
            }
            put("expenses", arr)
        }
        root.toString()
    }

    suspend fun exportTransactionHistoryJson(bankAccountId: Int? = null): String = withContext(Dispatchers.IO) {
        val allExpenses = extraExpenseDao.getAllExpensesSync()
        val expenses = if (bankAccountId != null) allExpenses.filter { it.bankAccountId == bankAccountId } else allExpenses
        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.TRANSACTION_HISTORY.name)
            if (bankAccountId != null) put("bankAccountId", bankAccountId)
            put("exportDate", System.currentTimeMillis())
            val arr = JSONArray()
            expenses.forEach { e ->
                arr.put(JSONObject().apply {
                    put("timestamp", e.timestamp)
                    put("name", e.name)
                    put("amount", e.amount)
                    put("bankAccountId", e.bankAccountId)
                    put("type", e.type)
                    put("category", e.category)
                    put("notes", e.notes)
                })
            }
            put("expenses", arr)
        }
        root.toString()
    }

    suspend fun exportElectricityWholeJson(): String = withContext(Dispatchers.IO) {
        val readings = meterDao.getAllReadingsSync()
        val bills = meterDao.getAllUtilityBillsSync()
        val rates = meterDao.getAllElectricityRatesSync()

        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.ELECTRICITY_WHOLE.name)
            put("exportDate", System.currentTimeMillis())

            val readArr = JSONArray()
            readings.forEach { r ->
                readArr.put(JSONObject().apply {
                    put("id", r.id)
                    put("readingDate", r.readingDate)
                    put("totalReading", r.totalReading)
                    put("isPaid", r.isPaid)
                })
            }
            put("meterReadings", readArr)

            val billArr = JSONArray()
            bills.forEach { b ->
                billArr.put(JSONObject().apply {
                    put("id", b.id)
                    put("startDate", b.startDate)
                    put("endDate", b.endDate)
                    put("totalUnits", b.totalUnits)
                    put("calculatedAmount", b.calculatedAmount)
                    put("dueDate", b.dueDate)
                    put("isPaid", b.isPaid)
                    put("transactionId", b.transactionId ?: "")
                    put("note", b.note)
                })
            }
            put("utilityBills", billArr)

            val rateArr = JSONArray()
            rates.forEach { r ->
                rateArr.put(JSONObject().apply {
                    put("ratePerUnit", r.ratePerUnit)
                    put("effectiveDate", r.effectiveDate)
                })
            }
            put("electricityRates", rateArr)
        }
        root.toString()
    }

    suspend fun exportMeterReadingsJson(): String = withContext(Dispatchers.IO) {
        val readings = meterDao.getAllReadingsSync()
        val rates = meterDao.getAllElectricityRatesSync()
        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.METER_READINGS.name)
            put("exportDate", System.currentTimeMillis())
            val arr = JSONArray()
            readings.forEach { r ->
                arr.put(JSONObject().apply {
                    put("id", r.id)
                    put("readingDate", r.readingDate)
                    put("totalReading", r.totalReading)
                    put("isPaid", r.isPaid)
                })
            }
            put("meterReadings", arr)

            val rateArr = JSONArray()
            rates.forEach { r ->
                rateArr.put(JSONObject().apply {
                    put("ratePerUnit", r.ratePerUnit)
                    put("effectiveDate", r.effectiveDate)
                })
            }
            put("electricityRates", rateArr)
        }
        root.toString()
    }

    suspend fun exportUtilityBillsJson(): String = withContext(Dispatchers.IO) {
        val bills = meterDao.getAllUtilityBillsSync()
        val rates = meterDao.getAllElectricityRatesSync()
        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.UTILITY_BILLS.name)
            put("exportDate", System.currentTimeMillis())
            val arr = JSONArray()
            bills.forEach { b ->
                arr.put(JSONObject().apply {
                    put("id", b.id)
                    put("startDate", b.startDate)
                    put("endDate", b.endDate)
                    put("totalUnits", b.totalUnits)
                    put("calculatedAmount", b.calculatedAmount)
                    put("dueDate", b.dueDate)
                    put("isPaid", b.isPaid)
                    put("transactionId", b.transactionId ?: "")
                    put("note", b.note)
                })
            }
            put("utilityBills", arr)

            val rateArr = JSONArray()
            rates.forEach { r ->
                rateArr.put(JSONObject().apply {
                    put("ratePerUnit", r.ratePerUnit)
                    put("effectiveDate", r.effectiveDate)
                })
            }
            put("electricityRates", rateArr)
        }
        root.toString()
    }

    suspend fun exportElectricityRatesJson(): String = withContext(Dispatchers.IO) {
        val rates = meterDao.getAllElectricityRatesSync()
        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.ELECTRICITY_RATES.name)
            put("exportDate", System.currentTimeMillis())
            val rateArr = JSONArray()
            rates.forEach { r ->
                rateArr.put(JSONObject().apply {
                    put("ratePerUnit", r.ratePerUnit)
                    put("effectiveDate", r.effectiveDate)
                })
            }
            put("electricityRates", rateArr)
        }
        root.toString(2)
    }

    suspend fun exportSettingsJson(): String = withContext(Dispatchers.IO) {
        val profile = userProfileDao.getUserProfileSync() ?: UserProfileEntity()
        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.SETTINGS.name)
            put("exportDate", System.currentTimeMillis())
            put("userProfile", JSONObject().apply {
                put("name", profile.name)
                put("contact", profile.contact)
                put("currencySymbol", profile.currencySymbol)
                put("language", profile.language)
                put("themeMode", profile.themeMode)
                put("enableSupplementaryAddons", profile.enableSupplementaryAddons)
                put("bgPreset", profile.bgPreset)
                put("enableLiquidGlassTheme", profile.enableLiquidGlassTheme)
                put("disableHeavyEffects", profile.disableHeavyEffects)
                put("mealGroupPosition", profile.mealGroupPosition)
                put("textSize", profile.textSize)
                put("isBoldTextEnabled", profile.isBoldTextEnabled)
                put("stickyCardWidthPercent", profile.stickyCardWidthPercent)
                put("stickyPreviewLines", profile.stickyPreviewLines)
                put("appVersion", profile.appVersion)
                if (profile.defaultHotelId != null) put("defaultHotelId", profile.defaultHotelId)
                if (profile.defaultBankAccountId != null) put("defaultBankAccountId", profile.defaultBankAccountId)
            })
        }
        root.toString(2)
    }

    suspend fun exportFullBackupJson(includeQrMedia: Boolean = true): String = withContext(Dispatchers.IO) {
        val hotels = hotelDao.getAllHotelsSync()
        val menus = menuDao.getAllMenuItemsSync()
        val transactions = transactionDao.getAllTransactionsSync()
        val hotelPayments = hotelPaymentDao.getAllPaymentsSync().filter { !it.isDeleted }
        val bankAccounts = bankAccountDao.getAllBankAccountsSync()
        val expenses = extraExpenseDao.getAllExpensesSync()
        val ownerAccounts = ownerAccountDao.getAllOwnerAccountsSync().filter { !it.isDeleted }
        val readings = meterDao.getAllReadingsSync()
        val bills = meterDao.getAllUtilityBillsSync()
        val rates = meterDao.getAllElectricityRatesSync()
        val stickyNotes = stickyNoteDao.getAllStickyNotesSync()
        val profile = userProfileDao.getUserProfileSync()

        val root = JSONObject().apply {
            put("version", 3)
            put("dataType", BackupDataType.FULL_BACKUP.name)
            put("exportDate", System.currentTimeMillis())

            if (profile != null) {
                put("userProfile", JSONObject().apply {
                    put("name", profile.name)
                    put("contact", profile.contact)
                    put("currencySymbol", profile.currencySymbol)
                    put("language", profile.language)
                    put("themeMode", profile.themeMode)
                    put("enableSupplementaryAddons", profile.enableSupplementaryAddons)
                    put("bgPreset", profile.bgPreset)
                    put("enableLiquidGlassTheme", profile.enableLiquidGlassTheme)
                    put("disableHeavyEffects", profile.disableHeavyEffects)
                    put("mealGroupPosition", profile.mealGroupPosition)
                    put("textSize", profile.textSize)
                    put("isBoldTextEnabled", profile.isBoldTextEnabled)
                    put("stickyCardWidthPercent", profile.stickyCardWidthPercent)
                    put("stickyPreviewLines", profile.stickyPreviewLines)
                    put("appVersion", profile.appVersion)
                })
            }

            val hotelsArr = JSONArray()
            hotels.forEach { h ->
                hotelsArr.put(JSONObject().apply {
                    put("id", h.id)
                    put("name", h.name)
                    put("advancedPayment", h.advancedPayment)
                    put("phoneNumber", h.phoneNumber)
                    put("location", h.location)
                    put("upiId", h.upiId)
                    put("qrCodeUri", h.qrCodeUri)
                    QrMediaCodec.appendQrDataToJson(this, AppDatabase.getAppContext(), h.qrCodeUri, includeQrPayloads = includeQrMedia)
                    put("notes", h.notes)
                    put("whatsappNumber", h.whatsappNumber)
                })
            }
            put("hotels", hotelsArr)

            val menusArr = JSONArray()
            menus.forEach { m ->
                menusArr.put(JSONObject().apply {
                    put("hotelId", m.hotelId)
                    put("banglaName", m.banglaName)
                    put("englishName", m.englishName)
                    put("price", m.price)
                    put("isDayMenu", m.isDayMenu)
                    put("isNightMenu", m.isNightMenu)
                    put("isAddOn", m.isAddOn)
                    put("emoji", m.emoji)
                    put("type", m.type)
                })
            }
            put("menus", menusArr)

            val txArr = JSONArray()
            transactions.forEach { t ->
                txArr.put(formatTransactionToJson(t))
            }
            put("transactions", txArr)

            if (hotelPayments.isNotEmpty()) {
                val payArr = JSONArray()
                hotelPayments.forEach { p ->
                    payArr.put(JSONObject().apply {
                        put("hotelId", p.hotelId)
                        put("year", p.year)
                        put("month", p.month)
                        put("amountPaid", p.amountPaid)
                        put("paymentDate", p.paymentDate)
                        put("note", p.note)
                    })
                }
                put("hotelPayments", payArr)
            }

            val bankArr = JSONArray()
            bankAccounts.forEach { b ->
                bankArr.put(JSONObject().apply {
                    put("id", b.id)
                    put("name", b.name)
                    put("initialBalance", b.initialBalance)
                    put("balance", b.balance)
                    put("details", b.details)
                    put("qrCodeUri", b.qrCodeUri)
                    QrMediaCodec.appendQrDataToJson(this, AppDatabase.getAppContext(), b.qrCodeUri, includeQrPayloads = includeQrMedia)
                    put("upiId", b.upiId)
                    put("accountNumber", b.accountNumber)
                    put("ifsc", b.ifsc)
                    put("branchName", b.branchName)
                    put("notes", b.notes)
                    put("phoneNumber", b.phoneNumber)
                    put("whatsappNumber", b.whatsappNumber)
                    put("email", b.email)
                })
            }
            put("bankAccounts", bankArr)

            val expArr = JSONArray()
            expenses.forEach { e ->
                expArr.put(JSONObject().apply {
                    put("timestamp", e.timestamp)
                    put("name", e.name)
                    put("amount", e.amount)
                    put("bankAccountId", e.bankAccountId)
                    put("type", e.type)
                    put("category", e.category)
                    put("notes", e.notes)
                })
            }
            put("expenses", expArr)

            if (ownerAccounts.isNotEmpty()) {
                val ownerArr = JSONArray()
                ownerAccounts.forEach { oa ->
                    ownerArr.put(JSONObject().apply {
                        put("id", oa.id)
                        put("ownerName", oa.ownerName)
                        put("ownerMobile", oa.ownerMobile)
                        put("accountName", oa.accountName)
                        put("accountNumber", oa.accountNumber)
                        put("ifscCode", oa.ifscCode)
                        put("branchName", oa.branchName)
                        put("upiId", oa.upiId)
                        put("qrCodeUri", oa.qrCodeUri)
                        QrMediaCodec.appendQrDataToJson(this, AppDatabase.getAppContext(), oa.qrCodeUri, includeQrPayloads = includeQrMedia)
                        put("details", oa.details)
                        put("notes", oa.notes)
                        put("whatsappNumber", oa.whatsappNumber)
                        put("email", oa.email)
                        if (oa.linkedBankAccountId != null) {
                            put("linkedBankAccountId", oa.linkedBankAccountId)
                        }
                    })
                }
                put("ownerAccounts", ownerArr)
            }

            if (stickyNotes.isNotEmpty()) {
                val stickyArr = JSONArray()
                stickyNotes.forEach { sn ->
                    stickyArr.put(JSONObject().apply {
                        put("title", sn.title)
                        put("subtitle", sn.subtitle)
                        put("bodyText", sn.bodyText)
                        put("tags", sn.tags)
                        put("textAlignment", sn.textAlignment)
                        put("colorIndex", sn.colorIndex)
                        put("sortOrder", sn.sortOrder)
                        put("attachmentsJson", sn.attachmentsJson)
                        put("createdAt", sn.createdAt)
                        put("updatedAt", sn.updatedAt)
                    })
                }
                put("stickyNotes", stickyArr)
            }

            val readArr = JSONArray()
            readings.forEach { r ->
                readArr.put(JSONObject().apply {
                    put("id", r.id)
                    put("readingDate", r.readingDate)
                    put("totalReading", r.totalReading)
                    put("isPaid", r.isPaid)
                })
            }
            put("meterReadings", readArr)

            val billArr = JSONArray()
            bills.forEach { b ->
                billArr.put(JSONObject().apply {
                    put("id", b.id)
                    put("startDate", b.startDate)
                    put("endDate", b.endDate)
                    put("totalUnits", b.totalUnits)
                    put("calculatedAmount", b.calculatedAmount)
                    put("dueDate", b.dueDate)
                    put("isPaid", b.isPaid)
                    put("transactionId", b.transactionId ?: "")
                    put("note", b.note)
                })
            }
            put("utilityBills", billArr)

            val rateArr = JSONArray()
            rates.forEach { r ->
                rateArr.put(JSONObject().apply {
                    put("ratePerUnit", r.ratePerUnit)
                    put("effectiveDate", r.effectiveDate)
                })
            }
            put("electricityRates", rateArr)
        }
        root.toString()
    }

    fun formatMealGroupsCompact(itemsSerialized: String): String {
        val groups = TransactionSerializer.deserializeGroups(itemsSerialized)
        if (groups.isNotEmpty()) {
            return groups.mapIndexed { idx, group ->
                val groupItems = group.items.joinToString(", ") { item ->
                    val priceStr = if (item.price % 1.0 == 0.0) item.price.toInt().toString() else item.price.toString()
                    "${item.name} (${item.quantity}x$priceStr)"
                }
                val subtotal = group.items.sumOf { it.quantity * it.price }
                val subtotalStr = if (subtotal % 1.0 == 0.0) subtotal.toInt().toString() else String.format(Locale.US, "%.2f", subtotal)
                "${idx + 1}. ${groupItems.ifBlank { "-" }} [Subtotal: $subtotalStr]"
            }.joinToString(" | ")
        }
        val items = TransactionSerializer.deserialize(itemsSerialized)
        if (items.isNotEmpty()) {
            val itemStr = items.joinToString(", ") { item ->
                val priceStr = if (item.price % 1.0 == 0.0) item.price.toInt().toString() else item.price.toString()
                "${item.name} (${item.quantity}x$priceStr)"
            }
            return "1. $itemStr"
        }
        return "-"
    }

    private fun generateRateBreakdownString(startDate: Long, endDate: Long, totalUnits: Double, rates: List<com.example.data.model.ElectricityRate>): String {
        if (rates.isEmpty()) return "Standard Rate"
        val startDay = getDayStart(startDate)
        val endDay = getDayStart(endDate)
        if (startDay >= endDay) {
            val r = rates.lastOrNull()?.ratePerUnit ?: 0.0
            return "Flat @ ₹$r/unit"
        }

        val totalDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(endDay - startDay).coerceAtLeast(1)
        val dailyUnits = totalUnits / totalDays.toDouble()

        val breakpoints = mutableListOf<Long>()
        breakpoints.add(startDay)
        rates.forEach { rate ->
            val rateDay = getDayStart(rate.effectiveDate)
            if (rateDay > startDay && rateDay < endDay) {
                breakpoints.add(rateDay)
            }
        }
        if (!breakpoints.contains(endDay)) {
            breakpoints.add(endDay)
        }
        breakpoints.sort()

        if (breakpoints.size <= 2) {
            val activeRate = rates.filter { getDayStart(it.effectiveDate) <= startDay }.maxByOrNull { it.effectiveDate }?.ratePerUnit ?: rates.firstOrNull()?.ratePerUnit ?: 0.0
            return "Flat @ ₹$activeRate/unit"
        }

        val segments = mutableListOf<String>()
        for (i in 0 until breakpoints.size - 1) {
            val pStart = breakpoints[i]
            val pEnd = breakpoints[i + 1]
            val pDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(pEnd - pStart).coerceAtLeast(0)
            val pUnits = dailyUnits * pDays
            val activeRate = rates.filter { getDayStart(it.effectiveDate) <= pStart }.maxByOrNull { it.effectiveDate }?.ratePerUnit ?: 0.0
            val pStartStr = dateOnlyFormat.format(Date(pStart))
            val pEndStr = dateOnlyFormat.format(Date(pEnd))
            val unitsStr = String.format(Locale.US, "%.1f", pUnits)
            segments.add("$pStartStr to $pEndStr @ ₹$activeRate/unit (${unitsStr} units)")
        }
        return segments.joinToString("; ")
    }

    private fun getDayStart(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // ==========================================
    // CSV EXPORTS (Group-wise and cleanly structured)
    // ==========================================

    suspend fun exportFullBackupCsv(): String = withContext(Dispatchers.IO) {
        val hotels = hotelDao.getAllHotelsSync()
        val profile = userProfileDao.getUserProfileSync()
        val sb = StringBuilder()
        sb.append("# FULL APPLICATION BACKUP\n\n")

        if (profile != null) {
            sb.append("# USER PROFILE\n")
            sb.append("Name,Contact,Currency,Language,Theme Mode\n")
            sb.append("\"${escapeCsv(profile.name)}\",\"${escapeCsv(profile.contact)}\",\"${escapeCsv(profile.currencySymbol)}\",\"${escapeCsv(profile.language)}\",\"${escapeCsv(profile.themeMode)}\"\n\n")
        }

        sb.append("# HOTELS\n")
        sb.append("Hotel ID,Hotel Name,Advance Payment,Phone Number,Location,UPI ID\n")
        hotels.forEach { h ->
            sb.append("${h.id},\"${escapeCsv(h.name)}\",${h.advancedPayment},\"${escapeCsv(h.phoneNumber)}\",\"${escapeCsv(h.location)}\",\"${escapeCsv(h.upiId)}\"\n")
        }
        sb.append("\n")

        sb.append("# MENU ITEMS\n")
        sb.append(exportMenusCsv(null))
        sb.append("\n")

        sb.append("# ORDER HISTORY (GROUP-WISE COMPACT)\n")
        sb.append(exportHotelHistoryCsv(null, includePaymentsSection = false))
        sb.append("\n")

        val allHotelPayments = hotelPaymentDao.getAllPaymentsSync().filter { !it.isDeleted }
        if (allHotelPayments.isNotEmpty()) {
            sb.append("# HOTEL MONTHLY PAYMENTS\n")
            sb.append("Hotel ID,Year,Month,Amount Paid,Payment Date,Note\n")
            allHotelPayments.forEach { p ->
                sb.append("${p.hotelId},${p.year},${p.month},${p.amountPaid},${p.paymentDate},\"${escapeCsv(p.note)}\"\n")
            }
            sb.append("\n")
        }

        sb.append("# BANK ACCOUNTS\n")
        sb.append(exportBankAccountsCsv(null))
        sb.append("\n")

        sb.append("# TRANSACTION HISTORY\n")
        sb.append(exportTransactionHistoryCsv(null))
        sb.append("\n")

        sb.append("# METER READINGS\n")
        sb.append(exportMeterReadingsCsv())
        sb.append("\n")

        sb.append("# UTILITY BILLS\n")
        sb.append(exportUtilityBillsCsv())

        sb.toString()
    }

    suspend fun exportMenusCsv(hotelId: Int? = null): String = withContext(Dispatchers.IO) {
        val menus = if (hotelId != null) menuDao.getMenusForHotelSync(hotelId) else menuDao.getAllMenuItemsSync()
        val sb = StringBuilder()
        sb.append("Bangla Name,English Name,Price,Day Available,Night Available,Is Add-on,Category\n")
        menus.forEach { m ->
            val dayText = if (m.isDayMenu) "Yes" else "No"
            val nightText = if (m.isNightMenu) "Yes" else "No"
            val addOnText = if (m.isAddOn) "Yes" else "No"
            sb.append("\"${escapeCsv(m.banglaName)}\",\"${escapeCsv(m.englishName)}\",${m.price},$dayText,$nightText,$addOnText,\"${escapeCsv(m.type)}\"\n")
        }
        sb.toString()
    }

    suspend fun exportHotelHistoryCsv(hotelId: Int? = null, includePaymentsSection: Boolean = true): String = withContext(Dispatchers.IO) {
        val txs = if (hotelId != null) transactionDao.getTransactionsForHotelSync(hotelId) else transactionDao.getAllTransactionsSync()
        val sb = StringBuilder()
        sb.append("Order ID,Date,Time,Session,Meal Groups & Items,Total Items,Order Total,Timestamp\n")
        
        txs.forEach { t ->
            val dateStr = dateOnlyFormat.format(Date(t.timestamp))
            val timeStr = timeOnlyFormat.format(Date(t.timestamp))
            val sessionName = if (t.isNight) "Night" else "Day"
            val groups = TransactionSerializer.deserializeGroups(t.itemsSerialized)
            val totalItems = if (groups.isNotEmpty()) groups.sumOf { g -> g.items.sumOf { it.quantity } } else {
                TransactionSerializer.deserialize(t.itemsSerialized).sumOf { it.quantity }
            }
            val compactGroupsStr = formatMealGroupsCompact(t.itemsSerialized)

            sb.append("${t.id},\"$dateStr\",\"$timeStr\",\"$sessionName\",\"${escapeCsv(compactGroupsStr)}\",$totalItems,${t.totalAmount},${t.timestamp}\n")
        }

        if (includePaymentsSection) {
            val payments = hotelPaymentDao.getAllPaymentsSync().filter {
                !it.isDeleted && (hotelId == null || it.hotelId == hotelId)
            }
            if (payments.isNotEmpty()) {
                sb.append("\n# HOTEL MONTHLY PAYMENTS\n")
                sb.append("Hotel ID,Year,Month,Amount Paid,Payment Date,Note\n")
                payments.forEach { p ->
                    sb.append("${p.hotelId},${p.year},${p.month},${p.amountPaid},${p.paymentDate},\"${escapeCsv(p.note)}\"\n")
                }
            }
        }
        sb.toString()
    }

    suspend fun exportWholeHotelCsv(hotelId: Int): String = withContext(Dispatchers.IO) {
        val hotel = hotelDao.getHotelByIdSync(hotelId)
        val sb = StringBuilder()
        sb.append("# HOTEL DETAILS\n")
        sb.append("Hotel Name,Advance Payment,Phone Number,Location,UPI ID\n")
        if (hotel != null) {
            sb.append("\"${escapeCsv(hotel.name)}\",${hotel.advancedPayment},\"${escapeCsv(hotel.phoneNumber)}\",\"${escapeCsv(hotel.location)}\",\"${escapeCsv(hotel.upiId)}\"\n\n")
        } else {
            sb.append("\"Hotel #$hotelId\",0.0,\"\",\"\",\"\"\n\n")
        }

        sb.append("# MENU ITEMS\n")
        sb.append(exportMenusCsv(hotelId))
        sb.append("\n# ORDER HISTORY (GROUP-WISE COMPACT)\n")
        sb.append(exportHotelHistoryCsv(hotelId, includePaymentsSection = false))
        val payments = hotelPaymentDao.getAllPaymentsSync().filter { it.hotelId == hotelId && !it.isDeleted }
        if (payments.isNotEmpty()) {
            sb.append("\n# HOTEL MONTHLY PAYMENTS\n")
            sb.append("Hotel ID,Year,Month,Amount Paid,Payment Date,Note\n")
            payments.forEach { p ->
                sb.append("${p.hotelId},${p.year},${p.month},${p.amountPaid},${p.paymentDate},\"${escapeCsv(p.note)}\"\n")
            }
        }
        sb.toString()
    }

    suspend fun exportHotelInfoCsv(hotelId: Int): String = withContext(Dispatchers.IO) {
        val hotel = hotelDao.getHotelByIdSync(hotelId)
        val sb = StringBuilder()
        sb.append("# HOTEL DETAILS\n")
        sb.append("Hotel Name,Advance Payment,Phone Number,Location,UPI ID,Notes\n")
        if (hotel != null) {
            sb.append("\"${escapeCsv(hotel.name)}\",${hotel.advancedPayment},\"${escapeCsv(hotel.phoneNumber)}\",\"${escapeCsv(hotel.location)}\",\"${escapeCsv(hotel.upiId)}\",\"${escapeCsv(hotel.notes)}\"\n")
        }
        sb.toString()
    }

    suspend fun exportSingleBankAccountCsv(bankAccountId: Int): String = withContext(Dispatchers.IO) {
        val account = bankAccountDao.getBankAccountByIdSync(bankAccountId)
        val sb = StringBuilder()
        sb.append("# BANK ACCOUNT DETAILS\n")
        sb.append("Account Name,Current Balance,UPI ID,Account Number,IFSC Code,Branch Name\n")
        if (account != null) {
            sb.append("\"${escapeCsv(account.name)}\",${account.balance},\"${escapeCsv(account.upiId)}\",\"${escapeCsv(account.accountNumber)}\",\"${escapeCsv(account.ifsc)}\",\"${escapeCsv(account.branchName)}\"\n\n")
        }
        sb.append("# TRANSACTION HISTORY\n")
        sb.append(exportTransactionHistoryCsv(bankAccountId))
        sb.toString()
    }

    suspend fun exportWalletWholeCsv(bankAccountId: Int? = null): String = withContext(Dispatchers.IO) {
        if (bankAccountId != null) {
            return@withContext exportSingleBankAccountCsv(bankAccountId)
        }
        val sb = StringBuilder()
        sb.append("# BANK ACCOUNTS\n")
        sb.append(exportBankAccountsCsv(null))
        sb.append("\n# TRANSACTION HISTORY\n")
        sb.append(exportTransactionHistoryCsv(null))
        sb.toString()
    }

    suspend fun exportBankAccountsCsv(bankAccountId: Int? = null): String = withContext(Dispatchers.IO) {
        val accounts = if (bankAccountId != null) {
            bankAccountDao.getBankAccountByIdSync(bankAccountId)?.let { listOf(it) } ?: emptyList()
        } else {
            bankAccountDao.getAllBankAccountsSync()
        }
        val sb = StringBuilder()
        sb.append("Account ID,Account Name,Current Balance,Initial Balance,UPI ID,Account Number,IFSC Code,Branch Name,Details\n")
        accounts.forEach { a ->
            sb.append("${a.id},\"${escapeCsv(a.name)}\",${a.balance},${a.initialBalance},\"${escapeCsv(a.upiId)}\",\"${escapeCsv(a.accountNumber)}\",\"${escapeCsv(a.ifsc)}\",\"${escapeCsv(a.branchName)}\",\"${escapeCsv(a.details)}\"\n")
        }
        sb.toString()
    }

    suspend fun exportTransactionHistoryCsv(bankAccountId: Int? = null): String = withContext(Dispatchers.IO) {
        val allExpenses = extraExpenseDao.getAllExpensesSync()
        val expenses = if (bankAccountId != null) allExpenses.filter { it.bankAccountId == bankAccountId } else allExpenses
        val sb = StringBuilder()
        sb.append("Timestamp,Date,Time,Transaction Type,Category,Description,Amount,Bank Account ID,Notes\n")
        expenses.forEach { e ->
            val dateStr = dateOnlyFormat.format(Date(e.timestamp))
            val timeStr = timeOnlyFormat.format(Date(e.timestamp))
            val typeStr = if (e.type.equals("INCOME", ignoreCase = true)) "Income" else "Expense"
            sb.append("${e.timestamp},\"$dateStr\",\"$timeStr\",\"$typeStr\",\"${escapeCsv(e.category)}\",\"${escapeCsv(e.name)}\",${e.amount},${e.bankAccountId},\"${escapeCsv(e.notes)}\"\n")
        }
        sb.toString()
    }

    suspend fun exportElectricityWholeCsv(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append("# METER READINGS\n")
        sb.append(exportMeterReadingsCsv())
        sb.append("\n# UTILITY BILLS\n")
        sb.append(exportUtilityBillsCsv())
        sb.toString()
    }

    suspend fun exportMeterReadingsCsv(): String = withContext(Dispatchers.IO) {
        val readings = meterDao.getAllReadingsSync()
        val sb = StringBuilder()
        sb.append("Reading Timestamp,Date,Total Reading (kWh),Units Consumed,Payment Status\n")
        readings.forEach { r ->
            val dateStr = dateOnlyFormat.format(Date(r.readingDate))
            val status = if (r.isPaid) "Paid" else "Pending"
            sb.append("${r.readingDate},\"$dateStr\",${r.totalReading},${r.unitsConsumed},\"$status\"\n")
        }
        sb.toString()
    }

    suspend fun exportUtilityBillsCsv(): String = withContext(Dispatchers.IO) {
        val bills = meterDao.getAllUtilityBillsSync()
        val rates = meterDao.getAllElectricityRatesSync().sortedBy { it.effectiveDate }
        val sb = StringBuilder()
        sb.append("Bill ID,Billing Start Date,Billing End Date,Total Units,Effective Rate (₹/unit),Calculated Amount (₹),Due Date,Payment Status,Transaction Reference,Variable Rate Breakdown,Start Timestamp,End Timestamp,Due Timestamp,Note\n")
        bills.forEach { b ->
            val startStr = dateOnlyFormat.format(Date(b.startDate))
            val endStr = dateOnlyFormat.format(Date(b.endDate))
            val dueStr = dateOnlyFormat.format(Date(b.dueDate))
            val status = if (b.isPaid) "Paid" else "Pending"
            val avgRate = if (b.totalUnits > 0) String.format(Locale.US, "%.2f", b.calculatedAmount / b.totalUnits) else "0.00"
            val rateBreakdown = generateRateBreakdownString(b.startDate, b.endDate, b.totalUnits, rates)
            sb.append("\"${escapeCsv(b.id)}\",\"$startStr\",\"$endStr\",${b.totalUnits},$avgRate,${b.calculatedAmount},\"$dueStr\",$status,\"${escapeCsv(b.transactionId ?: "")}\",\"${escapeCsv(rateBreakdown)}\",${b.startDate},${b.endDate},${b.dueDate},\"${escapeCsv(b.note)}\"\n")
        }
        sb.toString()
    }

    suspend fun exportElectricityRatesCsv(): String = withContext(Dispatchers.IO) {
        val rates = meterDao.getAllElectricityRatesSync().sortedByDescending { it.effectiveDate }
        val sb = StringBuilder()
        sb.append("Effective Timestamp,Effective Date,Rate Per Unit (₹/kWh)\n")
        rates.forEach { r ->
            val dateStr = dateOnlyFormat.format(Date(r.effectiveDate))
            sb.append("${r.effectiveDate},\"$dateStr\",${r.ratePerUnit}\n")
        }
        sb.toString()
    }

    suspend fun exportSettingsCsv(): String = withContext(Dispatchers.IO) {
        val p = userProfileDao.getUserProfileSync() ?: UserProfileEntity()
        val sb = StringBuilder()
        sb.append("# USER PROFILE & SETTINGS\n")
        sb.append("Name,Contact,Currency,Language,Theme Mode,Text Size,Bold Text,Sticky Card Width %,Sticky Preview Lines,App Version\n")
        sb.append("\"${escapeCsv(p.name)}\",\"${escapeCsv(p.contact)}\",\"${escapeCsv(p.currencySymbol)}\",\"${escapeCsv(p.language)}\",\"${escapeCsv(p.themeMode)}\",\"${escapeCsv(p.textSize)}\",${p.isBoldTextEnabled},${p.stickyCardWidthPercent},${p.stickyPreviewLines},\"${escapeCsv(p.appVersion)}\"\n")
        sb.toString()
    }

    fun injectScannedPaymentQrsIntoJson(jsonString: String, scannedQrMap: Map<String, String>): String {
        if (scannedQrMap.isEmpty()) return jsonString
        return try {
            val root = JSONObject(jsonString.trim())
            val grouped = mutableMapOf<Pair<String, Int>, MutableList<Pair<Int, String>>>()

            scannedQrMap.forEach { (key, uri) ->
                if (uri.isNotBlank()) {
                    val parts = key.split(":")
                    val entityType = parts.getOrNull(0)?.uppercase() ?: return@forEach
                    val origId = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach
                    val slotIdx = parts.getOrNull(2)?.toIntOrNull() ?: 0
                    grouped.getOrPut(entityType to origId) { mutableListOf() }.add(slotIdx to uri)
                }
            }

            fun resolvedUriFor(type: String, id: Int): String? {
                val list = grouped[type to id] ?: return null
                return list.sortedBy { it.first }.joinToString(",") { it.second }.ifBlank { null }
            }

            root.optJSONObject("hotel")?.let { hObj ->
                val hid = hObj.optInt("id", -1)
                val uri = resolvedUriFor("HOTEL", hid)
                    ?: if (grouped.keys.count { it.first == "HOTEL" } == 1) {
                        grouped.entries.firstOrNull { it.key.first == "HOTEL" }
                            ?.value?.sortedBy { it.first }?.joinToString(",") { it.second }
                    } else null
                if (!uri.isNullOrBlank()) hObj.put("qrCodeUri", uri)
            }

            root.optJSONArray("hotels")?.let { hArr ->
                for (i in 0 until hArr.length()) {
                    val hObj = hArr.optJSONObject(i) ?: continue
                    val uri = resolvedUriFor("HOTEL", hObj.optInt("id", -1))
                    if (!uri.isNullOrBlank()) hObj.put("qrCodeUri", uri)
                }
            }

            root.optJSONObject("bankAccount")?.let { bObj ->
                val bid = bObj.optInt("id", -1)
                val uri = resolvedUriFor("BANK", bid)
                    ?: if (grouped.keys.count { it.first == "BANK" } == 1) {
                        grouped.entries.firstOrNull { it.key.first == "BANK" }
                            ?.value?.sortedBy { it.first }?.joinToString(",") { it.second }
                    } else null
                if (!uri.isNullOrBlank()) bObj.put("qrCodeUri", uri)
            }

            root.optJSONArray("bankAccounts")?.let { bArr ->
                for (i in 0 until bArr.length()) {
                    val bObj = bArr.optJSONObject(i) ?: continue
                    val uri = resolvedUriFor("BANK", bObj.optInt("id", -1))
                    if (!uri.isNullOrBlank()) bObj.put("qrCodeUri", uri)
                }
            }

            root.optJSONArray("ownerAccounts")?.let { oArr ->
                for (i in 0 until oArr.length()) {
                    val oObj = oArr.optJSONObject(i) ?: continue
                    val uri = resolvedUriFor("OWNER", oObj.optInt("id", -1))
                    if (!uri.isNullOrBlank()) oObj.put("qrCodeUri", uri)
                }
            }

            root.toString()
        } catch (_: Exception) {
            jsonString
        }
    }

    // ==========================================
    // JSON IMPORTER (Auto-detects format)
    // ==========================================

    suspend fun importJson(jsonString: String, targetHotelId: Int = 1, targetBankAccountId: Int = 1): ImportOutcome = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString.trim())

            // Case 1: Legacy QrExportManager format (keys "h", "m", "t")
            if (root.has("h") && root.has("m")) {
                val hJson = root.getJSONObject("h")
                val name = hJson.getString("n")
                val location = hJson.optString("loc", "")
                val advancedPayment = hJson.optDouble("adv", 0.0)

                val newHotel = HotelEntity(
                    name = name,
                    location = location,
                    advancedPayment = advancedPayment
                )
                val newHotelId = hotelDao.insertHotel(newHotel).toInt()

                val mArray = root.getJSONArray("m")
                val menus = mutableListOf<MenuEntity>()
                for (i in 0 until mArray.length()) {
                    val mJson = mArray.getJSONObject(i)
                    menus.add(
                        MenuEntity(
                            hotelId = newHotelId,
                            banglaName = mJson.getString("bn"),
                            englishName = mJson.getString("en"),
                            price = mJson.getDouble("pr"),
                            isDayMenu = mJson.getInt("dy") == 1,
                            isNightMenu = mJson.getInt("nt") == 1,
                            isAddOn = mJson.getInt("ad") == 1,
                            emoji = "🍽️",
                            type = "Common"
                        )
                    )
                }
                menuDao.insertMenuItems(menus)

                var txCount = 0
                if (root.has("t")) {
                    val tArray = root.getJSONArray("t")
                    val txs = mutableListOf<HotelTransactionEntity>()
                    for (i in 0 until tArray.length()) {
                        val tJson = tArray.getJSONObject(i)
                        txs.add(
                            HotelTransactionEntity(
                                hotelId = newHotelId,
                                timestamp = tJson.getLong("ts"),
                                isNight = tJson.getInt("n") == 1,
                                totalAmount = tJson.getDouble("tot"),
                                itemsSerialized = tJson.getString("it")
                            )
                        )
                    }
                    transactionDao.insertTransactions(txs)
                    txCount = txs.size
                }
                return@withContext ImportOutcome.Success(
                    message = "Hotel '$name' restored with ${menus.size} menus & $txCount orders",
                    count = menus.size + txCount,
                    newHotelId = newHotelId
                )
            }

            val dataType = root.optString("dataType", "")

            when {
                dataType == BackupDataType.FULL_BACKUP.name || root.has("hotels") || root.has("userProfile") ||
                        (root.has("menus") && root.has("bankAccounts")) || (root.has("hotel") && root.has("bankAccounts")) -> {
                    var totalCount = 0

                    if (root.has("userProfile")) {
                        val pObj = root.getJSONObject("userProfile")
                        val existing = userProfileDao.getUserProfileSync() ?: UserProfileEntity()
                        val profile = existing.copy(
                            name = pObj.optString("name", existing.name),
                            contact = pObj.optString("contact", existing.contact),
                            currencySymbol = pObj.optString("currencySymbol", existing.currencySymbol),
                            language = pObj.optString("language", existing.language),
                            themeMode = pObj.optString("themeMode", existing.themeMode),
                            enableSupplementaryAddons = pObj.optBoolean("enableSupplementaryAddons", existing.enableSupplementaryAddons),
                            bgPreset = pObj.optString("bgPreset", existing.bgPreset),
                            enableLiquidGlassTheme = pObj.optBoolean("enableLiquidGlassTheme", existing.enableLiquidGlassTheme),
                            disableHeavyEffects = pObj.optBoolean("disableHeavyEffects", existing.disableHeavyEffects),
                            mealGroupPosition = pObj.optString("mealGroupPosition", existing.mealGroupPosition),
                            textSize = pObj.optString("textSize", existing.textSize),
                            isBoldTextEnabled = pObj.optBoolean("isBoldTextEnabled", existing.isBoldTextEnabled),
                            stickyCardWidthPercent = pObj.optInt("stickyCardWidthPercent", existing.stickyCardWidthPercent),
                            stickyPreviewLines = pObj.optInt("stickyPreviewLines", existing.stickyPreviewLines),
                            appVersion = pObj.optString("appVersion", existing.appVersion)
                        )
                        userProfileDao.insertOrUpdateProfile(profile)
                        if (dataType == BackupDataType.SETTINGS.name) {
                            return@withContext ImportOutcome.Success("Settings & preferences restored successfully", count = 1)
                        }
                    }

                    val hotelIdMap = mutableMapOf<Int, Int>()
                    if (root.has("hotels")) {
                        val hArr = root.getJSONArray("hotels")
                        for (i in 0 until hArr.length()) {
                            val hObj = hArr.getJSONObject(i)
                            val origId = hObj.optInt("id", -1)
                            val newId = hotelDao.insertHotel(
                                HotelEntity(
                                    name = hObj.getString("name"),
                                    advancedPayment = hObj.optDouble("advancedPayment", 0.0),
                                    phoneNumber = hObj.optString("phoneNumber", ""),
                                    location = hObj.optString("location", ""),
                                    upiId = hObj.optString("upiId", ""),
                                    qrCodeUri = QrMediaCodec.resolveImportedQrUri(hObj, AppDatabase.getAppContext()),
                                    notes = hObj.optString("notes", ""),
                                    whatsappNumber = hObj.optString("whatsappNumber", "")
                                )
                            ).toInt()
                            if (origId != -1) hotelIdMap[origId] = newId
                            totalCount++
                        }
                    }

                    if (root.has("menus")) {
                        val mArr = root.getJSONArray("menus")
                        val list = mutableListOf<MenuEntity>()
                        for (i in 0 until mArr.length()) {
                            val item = mArr.getJSONObject(i)
                            val origHotelId = item.optInt("hotelId", 1)
                            val mappedHotelId = hotelIdMap[origHotelId] ?: origHotelId
                            list.add(
                                MenuEntity(
                                    hotelId = mappedHotelId,
                                    banglaName = item.optString("banglaName", ""),
                                    englishName = item.optString("englishName", ""),
                                    price = item.optDouble("price", 0.0),
                                    isDayMenu = item.optBoolean("isDayMenu", true),
                                    isNightMenu = item.optBoolean("isNightMenu", true),
                                    isAddOn = item.optBoolean("isAddOn", false),
                                    emoji = item.optString("emoji", "🍽️"),
                                    type = item.optString("type", "Common")
                                )
                            )
                        }
                        menuDao.insertMenuItems(list)
                        totalCount += list.size
                    }

                    if (root.has("transactions")) {
                        val tArr = root.getJSONArray("transactions")
                        val list = mutableListOf<HotelTransactionEntity>()
                        for (i in 0 until tArr.length()) {
                            val item = tArr.getJSONObject(i)
                            val origHotelId = item.optInt("hotelId", 1)
                            val mappedHotelId = hotelIdMap[origHotelId] ?: origHotelId
                            val serialized = extractSerializedItemsFromJson(item)
                            list.add(
                                HotelTransactionEntity(
                                    hotelId = mappedHotelId,
                                    timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                    isNight = item.optBoolean("isNight", false),
                                    itemsSerialized = serialized,
                                    totalAmount = item.optDouble("totalAmount", 0.0)
                                )
                            )
                        }
                        transactionDao.insertTransactions(list)
                        totalCount += list.size
                    }

                    if (root.has("hotelPayments")) {
                        val payArr = root.getJSONArray("hotelPayments")
                        val list = mutableListOf<HotelPaymentEntity>()
                        for (i in 0 until payArr.length()) {
                            val item = payArr.getJSONObject(i)
                            val origHotelId = item.optInt("hotelId", 1)
                            val mappedHotelId = hotelIdMap[origHotelId] ?: origHotelId
                            val payDate = item.optLong("paymentDate", item.optLong("timestamp", System.currentTimeMillis()))
                            val cal = Calendar.getInstance().apply { timeInMillis = payDate }
                            list.add(
                                HotelPaymentEntity(
                                    hotelId = mappedHotelId,
                                    year = item.optInt("year", cal.get(Calendar.YEAR)),
                                    month = item.optInt("month", cal.get(Calendar.MONTH)),
                                    amountPaid = item.optDouble("amountPaid", item.optDouble("amount", 0.0)),
                                    paymentDate = payDate,
                                    note = item.optString("note", "")
                                )
                            )
                        }
                        hotelPaymentDao.insertPayments(list)
                        totalCount += list.size
                    }

                    val bankIdMap = mutableMapOf<Int, Int>()
                    val importedAccounts = mutableMapOf<Int, BankAccountEntity>()
                    if (root.has("bankAccounts")) {
                        val bArr = root.getJSONArray("bankAccounts")
                        for (i in 0 until bArr.length()) {
                            val item = bArr.getJSONObject(i)
                            val origId = item.optInt("id", -1)
                            val bal = item.optDouble("balance", 0.0)
                            val initBal = item.optDouble("initialBalance", bal)
                            val entity = BankAccountEntity(
                                name = item.getString("name"),
                                balance = if (item.has("balance")) bal else initBal,
                                initialBalance = initBal,
                                details = item.optString("details", ""),
                                qrCodeUri = QrMediaCodec.resolveImportedQrUri(item, AppDatabase.getAppContext()),
                                upiId = item.optString("upiId", ""),
                                accountNumber = item.optString("accountNumber", ""),
                                ifsc = item.optString("ifsc", ""),
                                branchName = item.optString("branchName", ""),
                                notes = item.optString("notes", ""),
                                phoneNumber = item.optString("phoneNumber", ""),
                                whatsappNumber = item.optString("whatsappNumber", ""),
                                email = item.optString("email", "")
                            )
                            val newId = bankAccountDao.insertBankAccount(entity).toInt()
                            if (origId != -1) bankIdMap[origId] = newId
                            importedAccounts[newId] = entity.copy(id = newId)
                            totalCount++
                        }
                    }

                    if (root.has("expenses")) {
                        val eArr = root.getJSONArray("expenses")
                        val list = mutableListOf<ExtraExpenseEntity>()
                        for (i in 0 until eArr.length()) {
                            val item = eArr.getJSONObject(i)
                            val origBankId = item.optInt("bankAccountId", 1)
                            val mappedBankId = bankIdMap[origBankId] ?: origBankId
                            list.add(
                                ExtraExpenseEntity(
                                    timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                    name = item.getString("name"),
                                    amount = item.getDouble("amount"),
                                    bankAccountId = mappedBankId,
                                    type = item.optString("type", "EXPENSE"),
                                    category = item.optString("category", "General"),
                                    notes = item.optString("notes", "")
                                )
                            )
                        }
                        extraExpenseDao.insertExpenses(list)
                        totalCount += list.size

                        // Recompute derived bank account balance from initialBalance + income - expense
                        for ((accId, accEntity) in importedAccounts) {
                            val accTxs = list.filter { it.bankAccountId == accId }
                            if (accTxs.isNotEmpty()) {
                                val netChange = accTxs.sumOf {
                                    if (it.type.equals("INCOME", ignoreCase = true)) it.amount else -it.amount
                                }
                                bankAccountDao.updateBankAccount(
                                    accEntity.copy(balance = accEntity.initialBalance + netChange)
                                )
                            }
                        }
                    }

                    if (root.has("ownerAccounts")) {
                        val oArr = root.getJSONArray("ownerAccounts")
                        for (i in 0 until oArr.length()) {
                            val item = oArr.getJSONObject(i)
                            ownerAccountDao.insertOwnerAccount(
                                OwnerAccountEntity(
                                    ownerName = item.optString("ownerName", item.optString("accountHolderName", "")),
                                    ownerMobile = item.optString("ownerMobile", item.optString("phoneNumber", "")),
                                    accountName = item.optString("accountName", item.optString("bankName", "")),
                                    accountNumber = item.optString("accountNumber", ""),
                                    ifscCode = item.optString("ifscCode", ""),
                                    branchName = item.optString("branchName", ""),
                                    upiId = item.optString("upiId", ""),
                                    qrCodeUri = QrMediaCodec.resolveImportedQrUri(item, AppDatabase.getAppContext()),
                                    details = item.optString("details", item.optString("notes", "")),
                                    notes = item.optString("notes", ""),
                                    whatsappNumber = item.optString("whatsappNumber", ""),
                                    email = item.optString("email", ""),
                                    linkedBankAccountId = if (item.has("linkedBankAccountId")) {
                                        val origLinked = item.optInt("linkedBankAccountId", -1)
                                        if (origLinked != -1) (bankIdMap[origLinked] ?: origLinked) else null
                                    } else null
                                )
                            )
                            totalCount++
                        }
                    }

                    if (root.has("stickyNotes")) {
                        val sArr = root.getJSONArray("stickyNotes")
                        val stickyList = mutableListOf<StickyNoteEntity>()
                        for (i in 0 until sArr.length()) {
                            val item = sArr.getJSONObject(i)
                            stickyList.add(
                                StickyNoteEntity(
                                    title = item.optString("title", ""),
                                    subtitle = item.optString("subtitle", ""),
                                    bodyText = item.optString("bodyText", item.optString("content", "")),
                                    tags = item.optString("tags", ""),
                                    textAlignment = item.optString("textAlignment", item.optString("alignment", "START")),
                                    colorIndex = item.optInt("colorIndex", 0),
                                    sortOrder = item.optInt("sortOrder", i),
                                    attachmentsJson = item.optString("attachmentsJson", "[]").ifBlank { "[]" },
                                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                                )
                            )
                        }
                        stickyNoteDao.insertStickyNotes(stickyList)
                        totalCount += stickyList.size
                    }

                    if (root.has("customCategories")) {
                        val cArr = root.getJSONArray("customCategories")
                        for (i in 0 until cArr.length()) {
                            val item = cArr.getJSONObject(i)
                            val catName = item.optString("name", "")
                            if (catName.isNotBlank()) {
                                transactionCategoryDao.insertCategory(
                                    TransactionCategoryEntity(
                                        name = catName,
                                        type = item.optString("type", "EXPENSE")
                                    )
                                )
                                totalCount++
                            }
                        }
                    }

                    if (root.has("meterReadings")) {
                        val rArr = root.getJSONArray("meterReadings")
                        val rawList = mutableListOf<MeterReading>()
                        for (i in 0 until rArr.length()) {
                            val item = rArr.getJSONObject(i)
                            rawList.add(
                                MeterReading(
                                    id = item.optString("id", UUID.randomUUID().toString()),
                                    readingDate = item.getLong("readingDate"),
                                    totalReading = item.getDouble("totalReading"),
                                    unitsConsumed = item.optDouble("unitsConsumed", -1.0),
                                    isPaid = item.optBoolean("isPaid", false)
                                )
                            )
                        }
                        val list = recomputeMeterUnitsConsumed(rawList)
                        meterDao.insertReadings(list)
                        totalCount += list.size
                    }

                    if (root.has("utilityBills")) {
                        val bArr = root.getJSONArray("utilityBills")
                        val list = mutableListOf<UtilityBill>()
                        for (i in 0 until bArr.length()) {
                            val item = bArr.getJSONObject(i)
                            list.add(
                                UtilityBill(
                                    id = item.optString("id", UUID.randomUUID().toString()),
                                    startDate = item.getLong("startDate"),
                                    endDate = item.getLong("endDate"),
                                    totalUnits = item.getDouble("totalUnits"),
                                    calculatedAmount = item.getDouble("calculatedAmount"),
                                    dueDate = item.optLong("dueDate", item.getLong("endDate")),
                                    isPaid = item.optBoolean("isPaid", true),
                                    transactionId = item.optString("transactionId", "").ifBlank { null },
                                    note = item.optString("note", "")
                                )
                            )
                        }
                        meterDao.insertUtilityBills(list)
                        totalCount += list.size

                        if (!root.has("electricityRates") && meterDao.getAllElectricityRatesSync().isEmpty()) {
                            val latestValidBill = list.filter { it.totalUnits > 0.0 && it.calculatedAmount > 0.0 }.maxByOrNull { it.endDate }
                            if (latestValidBill != null) {
                                val derivedRate = Math.round((latestValidBill.calculatedAmount / latestValidBill.totalUnits) * 100.0) / 100.0
                                if (derivedRate > 0.0) {
                                    meterDao.insertElectricityRate(
                                        ElectricityRate(
                                            effectiveDate = latestValidBill.startDate,
                                            ratePerUnit = derivedRate
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (root.has("electricityRates")) {
                        val rateArr = root.getJSONArray("electricityRates")
                        for (i in 0 until rateArr.length()) {
                            val item = rateArr.getJSONObject(i)
                            val rVal = item.optDouble("ratePerUnit", 0.0)
                            if (rVal > 0.0) {
                                meterDao.insertElectricityRate(
                                    ElectricityRate(
                                        effectiveDate = item.optLong("effectiveDate", System.currentTimeMillis()),
                                        ratePerUnit = rVal
                                    )
                                )
                                totalCount++
                            }
                        }
                    }

                    val paidHotelMonths = root.optJSONArray("hotelPayments")?.length() ?: 0
                    val paidBills = root.optJSONArray("utilityBills")?.let { arr ->
                        (0 until arr.length()).count { arr.getJSONObject(it).optBoolean("isPaid", false) }
                    } ?: 0
                    val paidReadings = root.optJSONArray("meterReadings")?.let { arr ->
                        (0 until arr.length()).count { arr.getJSONObject(it).optBoolean("isPaid", false) }
                    } ?: 0
                    val psidParts = mutableListOf<String>()
                    if (paidHotelMonths > 0) psidParts.add("$paidHotelMonths hotel paid month(s)")
                    if (paidBills > 0) psidParts.add("$paidBills paid bill(s)")
                    if (paidReadings > 0) psidParts.add("$paidReadings paid reading(s)")
                    val psidInfo = if (psidParts.isNotEmpty()) " • Paid status: ${psidParts.joinToString(", ")}" else ""

                    return@withContext ImportOutcome.Success(
                        message = "Full application backup restored ($totalCount records$psidInfo)",
                        count = totalCount
                    )
                }

                dataType == BackupDataType.HOTEL_WHOLE.name || dataType == BackupDataType.HOTEL_INFO.name || root.has("hotel") -> {
                    val hObj = root.getJSONObject("hotel")
                    val newHotel = HotelEntity(
                        name = hObj.optString("name", "Imported Hotel"),
                        advancedPayment = hObj.optDouble("advancedPayment", 0.0),
                        phoneNumber = hObj.optString("phoneNumber", ""),
                        location = hObj.optString("location", ""),
                        upiId = hObj.optString("upiId", ""),
                        qrCodeUri = QrMediaCodec.resolveImportedQrUri(hObj, AppDatabase.getAppContext()),
                        notes = hObj.optString("notes", ""),
                        whatsappNumber = hObj.optString("whatsappNumber", "")
                    )
                    val newHotelId = hotelDao.insertHotel(newHotel).toInt()

                    var menuCount = 0
                    if (root.has("menus")) {
                        val mArr = root.getJSONArray("menus")
                        val list = mutableListOf<MenuEntity>()
                        for (i in 0 until mArr.length()) {
                            val item = mArr.getJSONObject(i)
                            list.add(
                                MenuEntity(
                                    hotelId = newHotelId,
                                    banglaName = item.optString("banglaName", item.optString("name", "")),
                                    englishName = item.optString("englishName", item.optString("name", "")),
                                    price = item.optDouble("price", 0.0),
                                    isDayMenu = item.optBoolean("isDayMenu", true),
                                    isNightMenu = item.optBoolean("isNightMenu", true),
                                    isAddOn = item.optBoolean("isAddOn", false),
                                    emoji = item.optString("emoji", "🍽️"),
                                    type = item.optString("type", "Common")
                                )
                            )
                        }
                        menuDao.insertMenuItems(list)
                        menuCount = list.size
                    }

                    var txCount = 0
                    if (root.has("transactions")) {
                        val tArr = root.getJSONArray("transactions")
                        val list = mutableListOf<HotelTransactionEntity>()
                        for (i in 0 until tArr.length()) {
                            val item = tArr.getJSONObject(i)
                            val serialized = extractSerializedItemsFromJson(item)
                            list.add(
                                HotelTransactionEntity(
                                    hotelId = newHotelId,
                                    timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                    isNight = item.optBoolean("isNight", false),
                                    itemsSerialized = serialized,
                                    totalAmount = item.optDouble("totalAmount", 0.0)
                                )
                            )
                        }
                        transactionDao.insertTransactions(list)
                        txCount = list.size
                    }

                    var paidMonthCount = 0
                    if (root.has("hotelPayments")) {
                        val payArr = root.getJSONArray("hotelPayments")
                        val payList = mutableListOf<HotelPaymentEntity>()
                        for (i in 0 until payArr.length()) {
                            val item = payArr.getJSONObject(i)
                            val payDate = item.optLong("paymentDate", item.optLong("timestamp", System.currentTimeMillis()))
                            val cal = Calendar.getInstance().apply { timeInMillis = payDate }
                            payList.add(
                                HotelPaymentEntity(
                                    hotelId = newHotelId,
                                    year = item.optInt("year", cal.get(Calendar.YEAR)),
                                    month = item.optInt("month", cal.get(Calendar.MONTH)),
                                    amountPaid = item.optDouble("amountPaid", item.optDouble("amount", 0.0)),
                                    paymentDate = payDate,
                                    note = item.optString("note", "")
                                )
                            )
                        }
                        hotelPaymentDao.insertPayments(payList)
                        paidMonthCount = payList.size
                    }

                    val paidSuffix = if (paidMonthCount > 0) " • $paidMonthCount paid month(s)" else ""
                    return@withContext ImportOutcome.Success(
                        message = "Hotel '${newHotel.name}' created with $menuCount menus, $txCount orders$paidSuffix",
                        count = menuCount + txCount + paidMonthCount,
                        newHotelId = newHotelId
                    )
                }

                dataType == BackupDataType.HOTEL_MENUS.name || root.has("menus") -> {
                    val mArr = root.getJSONArray("menus")
                    val list = mutableListOf<MenuEntity>()
                    for (i in 0 until mArr.length()) {
                        val item = mArr.getJSONObject(i)
                        list.add(
                            MenuEntity(
                                hotelId = targetHotelId,
                                banglaName = item.optString("banglaName", item.optString("name", "")),
                                englishName = item.optString("englishName", item.optString("name", "")),
                                price = item.optDouble("price", 0.0),
                                isDayMenu = item.optBoolean("isDayMenu", true),
                                isNightMenu = item.optBoolean("isNightMenu", true),
                                isAddOn = item.optBoolean("isAddOn", false),
                                emoji = item.optString("emoji", "🍽️"),
                                type = item.optString("type", "Common")
                            )
                        )
                    }
                    menuDao.insertMenuItems(list)
                    return@withContext ImportOutcome.Success(
                        message = "Imported ${list.size} menus into Hotel #$targetHotelId",
                        count = list.size
                    )
                }

                dataType == BackupDataType.HOTEL_HISTORY.name || root.has("transactions") -> {
                    val tArr = root.getJSONArray("transactions")
                    val list = mutableListOf<HotelTransactionEntity>()
                    for (i in 0 until tArr.length()) {
                        val item = tArr.getJSONObject(i)
                        val serialized = extractSerializedItemsFromJson(item)
                        list.add(
                            HotelTransactionEntity(
                                hotelId = targetHotelId,
                                timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                isNight = item.optBoolean("isNight", false),
                                itemsSerialized = serialized,
                                totalAmount = item.optDouble("totalAmount", 0.0)
                            )
                        )
                    }
                    transactionDao.insertTransactions(list)
                    var payCount = 0
                    if (root.has("hotelPayments")) {
                        val payArr = root.getJSONArray("hotelPayments")
                        val payList = mutableListOf<HotelPaymentEntity>()
                        for (i in 0 until payArr.length()) {
                            val item = payArr.getJSONObject(i)
                            val payDate = item.optLong("paymentDate", item.optLong("timestamp", System.currentTimeMillis()))
                            val cal = Calendar.getInstance().apply { timeInMillis = payDate }
                            payList.add(
                                HotelPaymentEntity(
                                    hotelId = targetHotelId,
                                    year = item.optInt("year", cal.get(Calendar.YEAR)),
                                    month = item.optInt("month", cal.get(Calendar.MONTH)),
                                    amountPaid = item.optDouble("amountPaid", item.optDouble("amount", 0.0)),
                                    paymentDate = payDate,
                                    note = item.optString("note", "")
                                )
                            )
                        }
                        hotelPaymentDao.insertPayments(payList)
                        payCount = payList.size
                    }
                    return@withContext ImportOutcome.Success(
                        message = "Imported ${list.size} order transactions" + (if (payCount > 0) " & $payCount payment statuses" else "") + " into Hotel #$targetHotelId",
                        count = list.size + payCount
                    )
                }

                dataType == BackupDataType.WALLET_WHOLE.name || (root.has("bankAccounts") && root.has("expenses")) -> {
                    var totalCount = 0
                    val idMap = mutableMapOf<Int, Int>()
                    val importedAccounts = mutableMapOf<Int, BankAccountEntity>()
                    if (root.has("bankAccounts")) {
                        val bArr = root.getJSONArray("bankAccounts")
                        for (i in 0 until bArr.length()) {
                            val item = bArr.getJSONObject(i)
                            val origId = item.optInt("id", -1)
                            val bal = item.optDouble("balance", 0.0)
                            val initBal = item.optDouble("initialBalance", bal)
                            val newAccount = BankAccountEntity(
                                name = item.getString("name"),
                                balance = if (item.has("balance")) bal else initBal,
                                initialBalance = initBal,
                                details = item.optString("details", ""),
                                qrCodeUri = QrMediaCodec.resolveImportedQrUri(item, AppDatabase.getAppContext()),
                                upiId = item.optString("upiId", ""),
                                accountNumber = item.optString("accountNumber", ""),
                                ifsc = item.optString("ifsc", ""),
                                branchName = item.optString("branchName", "")
                            )
                            val newId = bankAccountDao.insertBankAccount(newAccount).toInt()
                            if (origId != -1) idMap[origId] = newId
                            importedAccounts[newId] = newAccount.copy(id = newId)
                            totalCount++
                        }
                    }
                    if (root.has("expenses")) {
                        val eArr = root.getJSONArray("expenses")
                        val list = mutableListOf<ExtraExpenseEntity>()
                        for (i in 0 until eArr.length()) {
                            val item = eArr.getJSONObject(i)
                            val origBankId = item.optInt("bankAccountId", targetBankAccountId)
                            val targetId = idMap[origBankId] ?: origBankId
                            list.add(
                                ExtraExpenseEntity(
                                    timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                    name = item.optString("name", "Expense"),
                                    amount = item.optDouble("amount", 0.0),
                                    bankAccountId = targetId,
                                    type = item.optString("type", "EXPENSE"),
                                    category = item.optString("category", "General"),
                                    notes = item.optString("notes", "")
                                )
                            )
                        }
                        extraExpenseDao.insertExpenses(list)
                        totalCount += list.size

                        for ((accId, accEntity) in importedAccounts) {
                            val accTxs = list.filter { it.bankAccountId == accId }
                            if (accTxs.isNotEmpty()) {
                                val netChange = accTxs.sumOf {
                                    if (it.type.equals("INCOME", ignoreCase = true)) it.amount else -it.amount
                                }
                                bankAccountDao.updateBankAccount(
                                    accEntity.copy(balance = accEntity.initialBalance + netChange)
                                )
                            }
                        }
                    }
                    return@withContext ImportOutcome.Success(
                        message = "Imported wallet data ($totalCount accounts & transactions)",
                        count = totalCount
                    )
                }

                dataType == BackupDataType.SINGLE_BANK_ACCOUNT.name || root.has("bankAccount") -> {
                    val bObj = root.getJSONObject("bankAccount")
                    val bal = bObj.optDouble("balance", 0.0)
                    val initBal = bObj.optDouble("initialBalance", bal)
                    val newAccount = BankAccountEntity(
                        name = bObj.getString("name"),
                        balance = if (bObj.has("balance")) bal else initBal,
                        initialBalance = initBal,
                        details = bObj.optString("details", ""),
                        qrCodeUri = QrMediaCodec.resolveImportedQrUri(bObj, AppDatabase.getAppContext()),
                        upiId = bObj.optString("upiId", ""),
                        accountNumber = bObj.optString("accountNumber", ""),
                        ifsc = bObj.optString("ifsc", ""),
                        branchName = bObj.optString("branchName", "")
                    )
                    val newAccountId = bankAccountDao.insertBankAccount(newAccount).toInt()
                    var txCount = 0
                    if (root.has("expenses")) {
                        val eArr = root.getJSONArray("expenses")
                        val list = mutableListOf<ExtraExpenseEntity>()
                        for (i in 0 until eArr.length()) {
                            val item = eArr.getJSONObject(i)
                            list.add(
                                ExtraExpenseEntity(
                                    timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                    name = item.optString("name", "Expense"),
                                    amount = item.optDouble("amount", 0.0),
                                    bankAccountId = newAccountId,
                                    type = item.optString("type", "EXPENSE"),
                                    category = item.optString("category", "General"),
                                    notes = item.optString("notes", "")
                                )
                            )
                        }
                        extraExpenseDao.insertExpenses(list)
                        txCount = list.size

                        val netChange = list.sumOf {
                            if (it.type.equals("INCOME", ignoreCase = true)) it.amount else -it.amount
                        }
                        bankAccountDao.updateBankAccount(
                            newAccount.copy(id = newAccountId, balance = newAccount.initialBalance + netChange)
                        )
                    }
                    return@withContext ImportOutcome.Success(
                        message = "Imported account '${newAccount.name}' with $txCount transactions",
                        count = 1 + txCount
                    )
                }

                dataType == BackupDataType.BANK_ACCOUNTS.name || root.has("bankAccounts") -> {
                    val bArr = root.getJSONArray("bankAccounts")
                    val list = mutableListOf<BankAccountEntity>()
                    for (i in 0 until bArr.length()) {
                        val item = bArr.getJSONObject(i)
                        val bal = item.optDouble("balance", 0.0)
                        val initBal = item.optDouble("initialBalance", bal)
                        list.add(
                            BankAccountEntity(
                                name = item.getString("name"),
                                balance = bal,
                                initialBalance = initBal,
                                details = item.optString("details", ""),
                                qrCodeUri = QrMediaCodec.resolveImportedQrUri(item, AppDatabase.getAppContext()),
                                upiId = item.optString("upiId", ""),
                                accountNumber = item.optString("accountNumber", ""),
                                ifsc = item.optString("ifsc", ""),
                                branchName = item.optString("branchName", "")
                            )
                        )
                    }
                    bankAccountDao.insertBankAccounts(list)
                    return@withContext ImportOutcome.Success(
                        message = "Imported ${list.size} bank accounts successfully",
                        count = list.size
                    )
                }

                dataType == BackupDataType.TRANSACTION_HISTORY.name || root.has("expenses") -> {
                    val fallbackAccId = root.optInt("bankAccountId", targetBankAccountId)
                    val eArr = root.getJSONArray("expenses")
                    val list = mutableListOf<ExtraExpenseEntity>()
                    for (i in 0 until eArr.length()) {
                        val item = eArr.getJSONObject(i)
                        list.add(
                            ExtraExpenseEntity(
                                timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                name = item.getString("name"),
                                amount = item.getDouble("amount"),
                                bankAccountId = item.optInt("bankAccountId", fallbackAccId),
                                type = item.optString("type", "EXPENSE"),
                                category = item.optString("category", "General"),
                                notes = item.optString("notes", "")
                            )
                        )
                    }
                    extraExpenseDao.insertExpenses(list)
                    return@withContext ImportOutcome.Success(
                        message = "Imported ${list.size} transactions into history",
                        count = list.size
                    )
                }

                dataType == BackupDataType.ELECTRICITY_WHOLE.name || (root.has("meterReadings") && root.has("utilityBills")) -> {
                    var totalCount = 0
                    if (root.has("meterReadings")) {
                        val rArr = root.getJSONArray("meterReadings")
                        val rawList = mutableListOf<MeterReading>()
                        for (i in 0 until rArr.length()) {
                            val item = rArr.getJSONObject(i)
                            rawList.add(
                                MeterReading(
                                    id = item.optString("id", UUID.randomUUID().toString()),
                                    readingDate = item.getLong("readingDate"),
                                    totalReading = item.getDouble("totalReading"),
                                    unitsConsumed = item.optDouble("unitsConsumed", -1.0),
                                    isPaid = item.optBoolean("isPaid", false)
                                )
                            )
                        }
                        val list = recomputeMeterUnitsConsumed(rawList)
                        meterDao.insertReadings(list)
                        totalCount += list.size
                    }
                    if (root.has("utilityBills")) {
                        val bArr = root.getJSONArray("utilityBills")
                        val list = mutableListOf<UtilityBill>()
                        for (i in 0 until bArr.length()) {
                            val item = bArr.getJSONObject(i)
                            list.add(
                                UtilityBill(
                                    id = item.optString("id", UUID.randomUUID().toString()),
                                    startDate = item.getLong("startDate"),
                                    endDate = item.getLong("endDate"),
                                    totalUnits = item.getDouble("totalUnits"),
                                    calculatedAmount = item.getDouble("calculatedAmount"),
                                    dueDate = item.optLong("dueDate", item.getLong("endDate")),
                                    isPaid = item.optBoolean("isPaid", true),
                                    transactionId = item.optString("transactionId", "").ifBlank { null },
                                    note = item.optString("note", "")
                                )
                            )
                        }
                        meterDao.insertUtilityBills(list)
                        totalCount += list.size

                        if (!root.has("electricityRates") && meterDao.getAllElectricityRatesSync().isEmpty()) {
                            val latestValidBill = list.filter { it.totalUnits > 0.0 && it.calculatedAmount > 0.0 }.maxByOrNull { it.endDate }
                            if (latestValidBill != null) {
                                val derivedRate = Math.round((latestValidBill.calculatedAmount / latestValidBill.totalUnits) * 100.0) / 100.0
                                if (derivedRate > 0.0) {
                                    meterDao.insertElectricityRate(
                                        ElectricityRate(
                                            effectiveDate = latestValidBill.startDate,
                                            ratePerUnit = derivedRate
                                        )
                                    )
                                }
                            }
                        }
                    }
                    if (root.has("electricityRates")) {
                        val rateArr = root.getJSONArray("electricityRates")
                        for (i in 0 until rateArr.length()) {
                            val item = rateArr.getJSONObject(i)
                            val rVal = item.optDouble("ratePerUnit", 0.0)
                            if (rVal > 0.0) {
                                meterDao.insertElectricityRate(
                                    ElectricityRate(
                                        effectiveDate = item.optLong("effectiveDate", System.currentTimeMillis()),
                                        ratePerUnit = rVal
                                    )
                                )
                                totalCount++
                            }
                        }
                    }
                    val paidBillCount = root.optJSONArray("utilityBills")?.let { arr ->
                        (0 until arr.length()).count { arr.getJSONObject(it).optBoolean("isPaid", false) }
                    } ?: 0
                    val paidReadCount = root.optJSONArray("meterReadings")?.let { arr ->
                        (0 until arr.length()).count { arr.getJSONObject(it).optBoolean("isPaid", false) }
                    } ?: 0
                    return@withContext ImportOutcome.Success(
                        message = "Imported electricity data ($totalCount items • $paidReadCount paid readings, $paidBillCount paid bills)",
                        count = totalCount
                    )
                }

                dataType == BackupDataType.METER_READINGS.name || root.has("meterReadings") -> {
                    val rArr = root.getJSONArray("meterReadings")
                    val rawList = mutableListOf<MeterReading>()
                    for (i in 0 until rArr.length()) {
                        val item = rArr.getJSONObject(i)
                        rawList.add(
                            MeterReading(
                                id = item.optString("id", UUID.randomUUID().toString()),
                                readingDate = item.getLong("readingDate"),
                                totalReading = item.getDouble("totalReading"),
                                unitsConsumed = item.optDouble("unitsConsumed", -1.0),
                                isPaid = item.optBoolean("isPaid", false)
                            )
                        )
                    }
                    val list = recomputeMeterUnitsConsumed(rawList)
                    meterDao.insertReadings(list)
                    if (root.has("electricityRates")) {
                        val rateArr = root.getJSONArray("electricityRates")
                        for (i in 0 until rateArr.length()) {
                            val item = rateArr.getJSONObject(i)
                            val rVal = item.optDouble("ratePerUnit", 0.0)
                            if (rVal > 0.0) {
                                meterDao.insertElectricityRate(
                                    ElectricityRate(
                                        effectiveDate = item.optLong("effectiveDate", System.currentTimeMillis()),
                                        ratePerUnit = rVal
                                    )
                                )
                            }
                        }
                    }
                    val paidCount = list.count { it.isPaid }
                    return@withContext ImportOutcome.Success(
                        message = "Imported ${list.size} meter readings ($paidCount paid)",
                        count = list.size
                    )
                }

                dataType == BackupDataType.UTILITY_BILLS.name || root.has("utilityBills") -> {
                    val bArr = root.getJSONArray("utilityBills")
                    val list = mutableListOf<UtilityBill>()
                    for (i in 0 until bArr.length()) {
                        val item = bArr.getJSONObject(i)
                        list.add(
                            UtilityBill(
                                id = item.optString("id", UUID.randomUUID().toString()),
                                startDate = item.getLong("startDate"),
                                endDate = item.getLong("endDate"),
                                totalUnits = item.getDouble("totalUnits"),
                                calculatedAmount = item.getDouble("calculatedAmount"),
                                dueDate = item.optLong("dueDate", item.getLong("endDate")),
                                isPaid = item.optBoolean("isPaid", true),
                                transactionId = item.optString("transactionId", "").ifBlank { null },
                                note = item.optString("note", "")
                            )
                        )
                    }
                    meterDao.insertUtilityBills(list)
                    if (root.has("electricityRates")) {
                        val rateArr = root.getJSONArray("electricityRates")
                        for (i in 0 until rateArr.length()) {
                            val item = rateArr.getJSONObject(i)
                            val rVal = item.optDouble("ratePerUnit", 0.0)
                            if (rVal > 0.0) {
                                meterDao.insertElectricityRate(
                                    ElectricityRate(
                                        effectiveDate = item.optLong("effectiveDate", System.currentTimeMillis()),
                                        ratePerUnit = rVal
                                    )
                                )
                            }
                        }
                    }
                    val paidCount = list.count { it.isPaid }
                    return@withContext ImportOutcome.Success(
                        message = "Imported ${list.size} electricity bills ($paidCount paid)",
                        count = list.size
                    )
                }

                dataType == BackupDataType.ELECTRICITY_RATES.name || root.has("electricityRates") -> {
                    val rateArr = root.getJSONArray("electricityRates")
                    var count = 0
                    for (i in 0 until rateArr.length()) {
                        val item = rateArr.getJSONObject(i)
                        val rVal = item.optDouble("ratePerUnit", 0.0)
                        if (rVal > 0.0) {
                            meterDao.insertElectricityRate(
                                ElectricityRate(
                                    effectiveDate = item.optLong("effectiveDate", System.currentTimeMillis()),
                                    ratePerUnit = rVal
                                )
                            )
                            count++
                        }
                    }
                    return@withContext ImportOutcome.Success(
                        message = "Imported $count electricity rate policy(s)",
                        count = count
                    )
                }

                else -> {
                    return@withContext ImportOutcome.Error("Unrecognized JSON backup format")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext ImportOutcome.Error(e.message ?: "Failed to parse JSON")
        }
    }

    private fun recomputeMeterUnitsConsumed(readings: List<MeterReading>): List<MeterReading> {
        if (readings.isEmpty()) return readings
        val sorted = readings.sortedBy { it.readingDate }
        val result = ArrayList<MeterReading>(sorted.size)
        for (i in sorted.indices) {
            val current = sorted[i]
            val derivedUnits = if (current.unitsConsumed >= 0.0) {
                current.unitsConsumed
            } else if (i == 0) {
                0.0
            } else {
                val delta = (current.totalReading - sorted[i - 1].totalReading).coerceAtLeast(0.0)
                Math.round(delta * 100.0) / 100.0
            }
            result.add(current.copy(unitsConsumed = derivedUnits))
        }
        return result
    }

    private fun extractSerializedItemsFromJson(itemObj: JSONObject): String {
        val directSerialized = itemObj.optString("itemsSerialized", "")
        if (directSerialized.isNotBlank()) return directSerialized

        if (itemObj.has("mealGroups")) {
            val groupsArr = itemObj.getJSONArray("mealGroups")
            val groups = mutableListOf<MealGroup>()
            for (g in 0 until groupsArr.length()) {
                val gObj = groupsArr.getJSONObject(g)
                val itemsArr = gObj.optJSONArray("items") ?: JSONArray()
                val items = mutableListOf<OrderedItem>()
                for (k in 0 until itemsArr.length()) {
                    val iObj = itemsArr.getJSONObject(k)
                    val name = iObj.optString("name", "")
                    val qty = iObj.optInt("quantity", 1)
                    val price = iObj.optDouble("price", 0.0)
                    if (name.isNotBlank() && name != "-") {
                        items.add(OrderedItem(name = name, quantity = qty, price = price))
                    }
                }
                if (items.isNotEmpty()) {
                    groups.add(MealGroup(items = items))
                }
            }
            return TransactionSerializer.serializeGroups(groups)
        }
        return ""
    }

    fun parseCompactMealGroups(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed == "-") return ""

        // If it's already in raw serialized format like "Rice:2:30;Dal:2:20#Chicken:1:120"
        if (trimmed.contains(":") && !trimmed.contains("Group", ignoreCase = true) && !trimmed.contains("(") && !trimmed.contains("x") && !trimmed.contains("@")) {
            return trimmed
        }

        // Split groups by | or // or ; or newlines
        val groupRawList = trimmed.split(Regex("[|//\\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val mealGroups = mutableListOf<MealGroup>()

        for (groupText in groupRawList) {
            // Strip "Group X:" or "[Group X:" or "1." prefix
            var cleanGroup = groupText
                .replace(Regex("^\\[?Group\\s*\\d*\\s*:\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^\\d+\\.\\s+"), "")
            // Strip trailing brackets like "[Subtotal: 100]" or "]"
            cleanGroup = cleanGroup.replace(Regex("\\[Subtotal[^\\]]*\\]", RegexOption.IGNORE_CASE), "")
            cleanGroup = cleanGroup.replace(Regex("\\(Subtotal[^\\)]*\\)", RegexOption.IGNORE_CASE), "")
            cleanGroup = cleanGroup.trim().removeSuffix("]").trim()

            if (cleanGroup.isBlank() || cleanGroup == "-") continue

            // Split items by comma or semicolon
            val itemTokens = cleanGroup.split(Regex("[,;]+")).map { it.trim() }.filter { it.isNotBlank() && it != "-" }
            val items = mutableListOf<OrderedItem>()

            for (token in itemTokens) {
                val parsedItem = parseSingleItemToken(token)
                if (parsedItem != null) {
                    items.add(parsedItem)
                }
            }

            if (items.isNotEmpty()) {
                mealGroups.add(MealGroup(items = items))
            }
        }

        return if (mealGroups.isNotEmpty()) TransactionSerializer.serializeGroups(mealGroups) else ""
    }

    private fun parseSingleItemToken(token: String): OrderedItem? {
        val t = token.trim()
        if (t.isBlank() || t == "-") return null

        // Pattern 1: Name:Qty:Price (e.g. Rice:2:30 or Rice:2:30.0)
        val colonMatch = Regex("^([^:]+):(\\d+)(?::([0-9.]+))?$").find(t)
        if (colonMatch != null) {
            val name = colonMatch.groupValues[1].trim()
            val qty = colonMatch.groupValues[2].toIntOrNull() ?: 1
            val price = colonMatch.groupValues.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            return OrderedItem(name = name, quantity = qty, price = price)
        }

        // Pattern 2: Name (Qty x Price) or Name (Qty*Price) or Name (Qty x Price = Total) or Name (Qty)
        val parenMatch = Regex("^(.+?)\\s*\\(\\s*(\\d+)(?:\\s*[xX*@✕]\\s*([0-9.]+))?(?:\\s*=\\s*[0-9.]+)?\\s*\\)$").find(t)
        if (parenMatch != null) {
            val name = parenMatch.groupValues[1].trim()
            val qty = parenMatch.groupValues[2].toIntOrNull() ?: 1
            val price = parenMatch.groupValues.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            return OrderedItem(name = name, quantity = qty, price = price)
        }

        // Pattern 3: Name xQty @ Price or Name x Qty
        val xMatch = Regex("^(.+?)\\s+[xX✕](\\d+)(?:\\s*[@at]+\\s*([0-9.]+))?$").find(t)
        if (xMatch != null) {
            val name = xMatch.groupValues[1].trim()
            val qty = xMatch.groupValues[2].toIntOrNull() ?: 1
            val price = xMatch.groupValues.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            return OrderedItem(name = name, quantity = qty, price = price)
        }

        // Pattern 4: Plain Name
        return OrderedItem(name = t, quantity = 1, price = 0.0)
    }

    // ==========================================
    // CSV IMPORTERS
    // ==========================================

    suspend fun importMenusCsv(csv: String, hotelId: Int): Int = withContext(Dispatchers.IO) {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.isEmpty()) return@withContext 0

        val menuList = mutableListOf<MenuEntity>()
        for ((index, line) in lines.withIndex()) {
            val cols = splitCsvRow(line)
            if (cols.isEmpty()) continue

            // Skip header row if present
            val first = cols[0].lowercase()
            if (index == 0 && (first.contains("bangla") || first.contains("name") || first.contains("item") || first.contains("id"))) {
                continue
            }

            val bangla = cols.getOrElse(0) { "" }
            val english = cols.getOrElse(1) { bangla }
            val price = cols.getOrElse(2) { "0" }.toDoubleOrNull() ?: 0.0
            val isDay = parseBoolean(cols.getOrElse(3) { "true" }, default = true)
            val isNight = parseBoolean(cols.getOrElse(4) { "true" }, default = true)
            val isAddOn = parseBoolean(cols.getOrElse(5) { "false" }, default = false)
            val type = cols.getOrElse(6) { "Common" }

            if (bangla.isNotBlank() || english.isNotBlank()) {
                menuList.add(
                    MenuEntity(
                        hotelId = hotelId,
                        banglaName = bangla.ifBlank { english },
                        englishName = english.ifBlank { bangla },
                        price = price,
                        isDayMenu = isDay,
                        isNightMenu = isNight,
                        isAddOn = isAddOn,
                        emoji = "🍽️",
                        type = type.ifBlank { "Common" }
                    )
                )
            }
        }
        if (menuList.isNotEmpty()) {
            menuDao.insertMenuItems(menuList)
        }
        menuList.size
    }

    suspend fun importHotelHistoryCsv(csv: String, hotelId: Int): Int = withContext(Dispatchers.IO) {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return@withContext 0

        // Extract lines specifically from order history section (stop before # HOTEL MONTHLY PAYMENTS if present)
        val historyLines = if (lines.any { it.startsWith("# ORDER HISTORY", ignoreCase = true) }) {
            lines.dropWhile { !it.startsWith("# ORDER HISTORY", ignoreCase = true) }
                .drop(1)
                .takeWhile { !it.startsWith("#") }
                .filter { it.isNotBlank() }
        } else {
            lines.takeWhile { !it.startsWith("# HOTEL MONTHLY PAYMENTS", ignoreCase = true) }
                .filter { !it.startsWith("#") && it.isNotBlank() }
        }

        var importedPaymentsCount = 0
        if (lines.any { it.startsWith("# HOTEL MONTHLY PAYMENTS", ignoreCase = true) }) {
            val payLines = lines.dropWhile { !it.startsWith("# HOTEL MONTHLY PAYMENTS", ignoreCase = true) }
                .drop(1)
                .takeWhile { !it.startsWith("#") }
                .filter { it.isNotBlank() && !it.startsWith("Hotel ID", ignoreCase = true) }
            val payList = mutableListOf<HotelPaymentEntity>()
            for (line in payLines) {
                val cols = splitCsvRow(line)
                if (cols.size >= 4) {
                    val yr = cols.getOrElse(1) { "0" }.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                    val mo = cols.getOrElse(2) { "0" }.toIntOrNull() ?: Calendar.getInstance().get(Calendar.MONTH)
                    val amt = cols.getOrElse(3) { "0" }.toDoubleOrNull() ?: 0.0
                    val pDate = cols.getOrElse(4) { "0" }.toLongOrNull() ?: System.currentTimeMillis()
                    val note = cols.getOrElse(5) { "" }
                    payList.add(
                        HotelPaymentEntity(
                            hotelId = hotelId,
                            year = yr,
                            month = mo,
                            amountPaid = amt,
                            paymentDate = pDate,
                            note = note
                        )
                    )
                }
            }
            if (payList.isNotEmpty()) {
                hotelPaymentDao.insertPayments(payList)
                importedPaymentsCount = payList.size
            }
        }

        if (historyLines.isEmpty()) return@withContext importedPaymentsCount

        val firstLineCols = splitCsvRow(historyLines[0])
        val isHeader = firstLineCols.any { col ->
            val c = col.lowercase()
            c.contains("order") || c.contains("timestamp") || c.contains("date") || c.contains("group") || c.contains("item") || c.contains("session")
        }

        val dataRows = if (isHeader) historyLines.drop(1) else historyLines
        if (dataRows.isEmpty()) return@withContext 0

        val firstDataCols = splitCsvRow(dataRows[0])
        val isMultiRowGroupFormat = firstDataCols.size >= 10 && firstLineCols.any { it.contains("unit price", ignoreCase = true) || it.contains("item total", ignoreCase = true) }
        val isCompactFormat = !isMultiRowGroupFormat && (firstLineCols.any { it.contains("meal group", ignoreCase = true) || it.contains("breakdown", ignoreCase = true) || it.contains("items", ignoreCase = true) } ||
                firstDataCols.any { it.contains("Group", ignoreCase = true) || it.contains("x") || it.contains("@") })

        val txList = mutableListOf<HotelTransactionEntity>()

        if (isCompactFormat) {
            // Compact 1-row format: Order ID, Date, Time, Session, Meal Groups & Items, Total Items, Order Total, Timestamp
            for (line in dataRows) {
                val cols = splitCsvRow(line)
                if (cols.isEmpty()) continue

                val orderId = cols.getOrElse(0) { "" }
                val dateStr = cols.getOrElse(1) { "" }
                val timeStr = cols.getOrElse(2) { "" }
                val sessionStr = cols.getOrElse(3) { "" }
                val groupsStr = cols.getOrElse(4) { "" }
                val totalItems = cols.getOrElse(5) { "0" }.toIntOrNull() ?: 0
                val orderTotal = cols.getOrElse(6) { "0" }.toDoubleOrNull() ?: 0.0
                val tsRaw = cols.getOrElse(7) { "" }

                val timestamp = parseTimestamp(tsRaw, "$dateStr $timeStr")
                val isNight = sessionStr.equals("Night", ignoreCase = true) || parseBoolean(sessionStr, default = false)

                val serialized = parseCompactMealGroups(groupsStr)
                val calculatedTotal = if (orderTotal > 0.0) {
                    orderTotal
                } else {
                    val groups = TransactionSerializer.deserializeGroups(serialized)
                    if (groups.isNotEmpty()) groups.sumOf { it.subtotal } else {
                        TransactionSerializer.deserialize(serialized).sumOf { it.total }
                    }
                }

                txList.add(
                    HotelTransactionEntity(
                        hotelId = hotelId,
                        timestamp = timestamp,
                        isNight = isNight,
                        itemsSerialized = serialized,
                        totalAmount = calculatedTotal
                    )
                )
            }
        } else if (isMultiRowGroupFormat) {
            // Group-wise expanded format:
            // Order ID (0), Timestamp (1), Date (2), Time (3), Session (4), Meal Group (5), Item Name (6), Quantity (7), Unit Price (8), Item Total (9), Order Total (10)
            data class RawHistoryRow(
                val orderId: String,
                val timestamp: Long,
                val dateStr: String,
                val timeStr: String,
                val sessionStr: String,
                val groupStr: String,
                val itemName: String,
                val quantity: Int,
                val unitPrice: Double,
                val itemTotal: Double,
                val orderTotal: Double
            )

            val parsedRows = mutableListOf<RawHistoryRow>()
            for (line in dataRows) {
                val cols = splitCsvRow(line)
                if (cols.isEmpty()) continue

                val orderId = cols.getOrElse(0) { "" }
                val ts = parseTimestamp(cols.getOrElse(1) { "" }, cols.getOrElse(2) { "" })
                val dateStr = cols.getOrElse(2) { "" }
                val timeStr = cols.getOrElse(3) { "" }
                val sessionStr = cols.getOrElse(4) { "" }
                val groupStr = cols.getOrElse(5) { "Group 1" }
                val itemName = cols.getOrElse(6) { "" }
                val qty = cols.getOrElse(7) { "1" }.toIntOrNull() ?: 1
                val unitPrice = cols.getOrElse(8) { "0" }.toDoubleOrNull() ?: 0.0
                val itemTotal = cols.getOrElse(9) { "0" }.toDoubleOrNull() ?: (qty * unitPrice)
                val orderTotal = cols.getOrElse(10) { "0" }.toDoubleOrNull() ?: 0.0

                parsedRows.add(
                    RawHistoryRow(
                        orderId = orderId,
                        timestamp = ts,
                        dateStr = dateStr,
                        timeStr = timeStr,
                        sessionStr = sessionStr,
                        groupStr = groupStr,
                        itemName = itemName,
                        quantity = qty,
                        unitPrice = unitPrice,
                        itemTotal = itemTotal,
                        orderTotal = orderTotal
                    )
                )
            }

            // Group by unique order: timestamp or orderId
            val groupedOrders = parsedRows.groupBy {
                if (it.timestamp > 0) it.timestamp.toString() else "${it.orderId}_${it.dateStr}_${it.timeStr}_${it.sessionStr}"
            }

            for ((_, rowsInOrder) in groupedOrders) {
                if (rowsInOrder.isEmpty()) continue
                val first = rowsInOrder[0]
                val isNight = first.sessionStr.equals("Night", ignoreCase = true) || parseBoolean(first.sessionStr, default = false)
                val timestamp = first.timestamp

                // Group by Meal Group
                val groupsMap = rowsInOrder.groupBy { it.groupStr.ifBlank { "Group 1" } }
                val mealGroups = mutableListOf<MealGroup>()
                var calculatedTotal = 0.0

                for ((_, itemRows) in groupsMap) {
                    val items = mutableListOf<OrderedItem>()
                    for (row in itemRows) {
                        if (row.itemName.isNotBlank() && row.itemName != "-") {
                            items.add(
                                OrderedItem(
                                    name = row.itemName,
                                    quantity = row.quantity,
                                    price = row.unitPrice
                                )
                            )
                            calculatedTotal += (row.quantity * row.unitPrice)
                        }
                    }
                    if (items.isNotEmpty()) {
                        mealGroups.add(MealGroup(items = items))
                    }
                }

                val finalTotal = if (first.orderTotal > 0.0) first.orderTotal else calculatedTotal
                val serialized = TransactionSerializer.serializeGroups(mealGroups)

                txList.add(
                    HotelTransactionEntity(
                        hotelId = hotelId,
                        timestamp = timestamp,
                        isNight = isNight,
                        itemsSerialized = serialized,
                        totalAmount = finalTotal
                    )
                )
            }
        } else {
            // Legacy / Single-row format: Timestamp, Date, Is Night, Total Amount, Items
            for (line in dataRows) {
                val cols = splitCsvRow(line)
                if (cols.isEmpty()) continue

                val timestamp = parseTimestamp(cols.getOrElse(0) { "" }, cols.getOrElse(1) { "" })
                val isNight = parseBoolean(cols.getOrElse(2) { "false" }, default = false)
                val total = cols.getOrElse(3) { "0" }.toDoubleOrNull() ?: 0.0
                val items = cols.getOrElse(4) { "" }

                val serialized = parseCompactMealGroups(items).ifBlank { items }

                txList.add(
                    HotelTransactionEntity(
                        hotelId = hotelId,
                        timestamp = timestamp,
                        isNight = isNight,
                        itemsSerialized = serialized,
                        totalAmount = total
                    )
                )
            }
        }

        if (txList.isNotEmpty()) {
            transactionDao.insertTransactions(txList)
        }
        txList.size + importedPaymentsCount
    }

    suspend fun importWholeHotelCsv(csv: String, targetHotelId: Int? = null): ImportOutcome = withContext(Dispatchers.IO) {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return@withContext ImportOutcome.Error("CSV is empty")

        // 1. Hotel Profile
        var hotelIdToUse = targetHotelId
        var hotelName = "Imported Hotel"

        if (lines.any { it.startsWith("# HOTEL DETAILS", ignoreCase = true) }) {
            val hotelLines = lines.dropWhile { !it.startsWith("# HOTEL DETAILS", ignoreCase = true) }.drop(1).takeWhile { !it.startsWith("#") }.filter { it.isNotBlank() }
            if (hotelLines.isNotEmpty()) {
                val dataLine = if (hotelLines[0].lowercase().contains("name")) hotelLines.getOrNull(1) else hotelLines[0]
                if (dataLine != null) {
                    val cols = splitCsvRow(dataLine)
                    hotelName = cols.getOrElse(0) { "Imported Hotel" }
                    val advance = cols.getOrElse(1) { "0" }.toDoubleOrNull() ?: 0.0
                    val phone = cols.getOrElse(2) { "" }
                    val location = cols.getOrElse(3) { "" }
                    val upi = cols.getOrElse(4) { "" }

                    val newHotel = HotelEntity(
                        name = hotelName,
                        advancedPayment = advance,
                        phoneNumber = phone,
                        location = location,
                        upiId = upi
                    )
                    hotelIdToUse = hotelDao.insertHotel(newHotel).toInt()
                }
            }
        }

        val resolvedHotelId = hotelIdToUse ?: 1

        // 2. Menus
        var menuCount = 0
        if (lines.any { it.startsWith("# MENU ITEMS", ignoreCase = true) }) {
            val menuSection = lines.dropWhile { !it.startsWith("# MENU ITEMS", ignoreCase = true) }.drop(1).takeWhile { !it.startsWith("#") }.joinToString("\n")
            menuCount = importMenusCsv(menuSection, resolvedHotelId)
        } else {
            menuCount = importMenusCsv(csv, resolvedHotelId)
        }

        // 3. Order History
        var txCount = 0
        if (lines.any { it.startsWith("# ORDER HISTORY", ignoreCase = true) }) {
            val historySection = lines.dropWhile { !it.startsWith("# ORDER HISTORY", ignoreCase = true) }.drop(1).takeWhile { !it.startsWith("#") }.joinToString("\n")
            txCount = importHotelHistoryCsv(historySection, resolvedHotelId)
        }

        // 4. Monthly Payments
        if (lines.any { it.startsWith("# HOTEL MONTHLY PAYMENTS", ignoreCase = true) }) {
            val payLines = lines.dropWhile { !it.startsWith("# HOTEL MONTHLY PAYMENTS", ignoreCase = true) }.drop(1).takeWhile { !it.startsWith("#") }.filter { it.isNotBlank() && !it.startsWith("Hotel ID", ignoreCase = true) }
            val payList = mutableListOf<HotelPaymentEntity>()
            for (line in payLines) {
                val cols = splitCsvRow(line)
                if (cols.size >= 4) {
                    val yr = cols.getOrElse(1) { "0" }.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                    val mo = cols.getOrElse(2) { "0" }.toIntOrNull() ?: Calendar.getInstance().get(Calendar.MONTH)
                    val amt = cols.getOrElse(3) { "0" }.toDoubleOrNull() ?: 0.0
                    val pDate = cols.getOrElse(4) { "0" }.toLongOrNull() ?: System.currentTimeMillis()
                    val note = cols.getOrElse(5) { "" }
                    payList.add(
                        HotelPaymentEntity(
                            hotelId = resolvedHotelId,
                            year = yr,
                            month = mo,
                            amountPaid = amt,
                            paymentDate = pDate,
                            note = note
                        )
                    )
                }
            }
            if (payList.isNotEmpty()) {
                hotelPaymentDao.insertPayments(payList)
            }
        }

        ImportOutcome.Success(
            message = "Hotel '$hotelName' imported with $menuCount menus & $txCount orders",
            count = menuCount + txCount,
            newHotelId = resolvedHotelId
        )
    }

    suspend fun importBankAccountsCsv(csv: String): Int = withContext(Dispatchers.IO) {
        val lines = csv.lines().map { it.trim().removePrefix("\uFEFF").trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.isEmpty()) return@withContext 0

        val list = mutableListOf<BankAccountEntity>()
        for ((index, line) in lines.withIndex()) {
            val cols = splitCsvRow(line)
            if (cols.isEmpty()) continue

            val first = cols[0].lowercase()
            if (index == 0 && (first.contains("name") || first.contains("account") || first.contains("bank") || first.contains("id"))) {
                continue
            }

            // Check if column 0 is ID (numeric) or Name
            val isIdFirst = cols[0].toIntOrNull() != null && cols.size > 2
            val name = if (isIdFirst) cols.getOrElse(1) { "" } else cols.getOrElse(0) { "" }
            if (name.isBlank() || name.equals("Account Name", ignoreCase = true)) continue

            val balIndex = if (isIdFirst) 2 else 1
            val hasInitBal = if (isIdFirst) (cols.size > 3 && cols[3].toDoubleOrNull() != null) else (cols.size > 2 && cols[2].toDoubleOrNull() != null)
            val initBalIndex = if (hasInitBal) (if (isIdFirst) 3 else 2) else balIndex
            val upiIndex = if (isIdFirst) (if (hasInitBal) 4 else 3) else (if (hasInitBal) 3 else 2)
            val accNumIndex = upiIndex + 1
            val ifscIndex = upiIndex + 2
            val branchIndex = upiIndex + 3
            val detailsIndex = upiIndex + 4

            val balance = cols.getOrElse(balIndex) { "0" }.toDoubleOrNull() ?: 0.0
            val initBal = cols.getOrElse(initBalIndex) { balance.toString() }.toDoubleOrNull() ?: balance
            val upiId = cols.getOrElse(upiIndex) { "" }
            val accNum = cols.getOrElse(accNumIndex) { "" }
            val ifsc = cols.getOrElse(ifscIndex) { "" }
            val branch = cols.getOrElse(branchIndex) { "" }
            val details = cols.getOrElse(detailsIndex) { "" }

            list.add(
                BankAccountEntity(
                    name = name,
                    balance = balance,
                    initialBalance = initBal,
                    upiId = upiId,
                    accountNumber = accNum,
                    ifsc = ifsc,
                    branchName = branch,
                    details = details
                )
            )
        }
        if (list.isNotEmpty()) {
            bankAccountDao.insertBankAccounts(list)
        }
        list.size
    }

    suspend fun importTransactionHistoryCsv(csv: String): Int = withContext(Dispatchers.IO) {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.isEmpty()) return@withContext 0

        val list = mutableListOf<ExtraExpenseEntity>()
        for ((index, line) in lines.withIndex()) {
            val cols = splitCsvRow(line)
            if (cols.isEmpty()) continue

            val first = cols[0].lowercase()
            if (index == 0 && (first.contains("timestamp") || first.contains("date") || first.contains("desc") || first.contains("name") || first.contains("type"))) {
                continue
            }

            val timestamp = parseTimestamp(cols.getOrElse(0) { "" }, cols.getOrElse(1) { "" })
            
            // Check if column 3 is type (new format) or description (legacy format)
            val col3 = cols.getOrElse(3) { "" }
            val isNewFormat = col3.equals("Income", ignoreCase = true) || col3.equals("Expense", ignoreCase = true)

            val type: String
            val category: String
            val desc: String
            val amount: Double
            val bankId: Int
            val notes: String

            if (isNewFormat) {
                type = if (col3.contains("INC", ignoreCase = true)) "INCOME" else "EXPENSE"
                category = cols.getOrElse(4) { "General" }
                desc = cols.getOrElse(5) { "Transaction" }
                amount = cols.getOrElse(6) { "0" }.toDoubleOrNull() ?: 0.0
                bankId = cols.getOrElse(7) { "1" }.toIntOrNull() ?: 1
                notes = cols.getOrElse(8) { "" }
            } else {
                desc = cols.getOrElse(2) { "Expense" }
                amount = cols.getOrElse(3) { "0" }.toDoubleOrNull() ?: 0.0
                val rawType = cols.getOrElse(4) { "EXPENSE" }.uppercase().trim()
                type = if (rawType.contains("INC")) "INCOME" else "EXPENSE"
                category = cols.getOrElse(5) { "General" }
                bankId = cols.getOrElse(6) { "1" }.toIntOrNull() ?: 1
                notes = cols.getOrElse(7) { "" }
            }

            list.add(
                ExtraExpenseEntity(
                    timestamp = timestamp,
                    name = desc.ifBlank { "Transaction" },
                    amount = amount,
                    bankAccountId = bankId,
                    type = type,
                    category = category.ifBlank { "General" },
                    notes = notes
                )
            )
        }
        if (list.isNotEmpty()) {
            extraExpenseDao.insertExpenses(list)
        }
        list.size
    }

    suspend fun importMeterReadingsCsv(csv: String): Int = withContext(Dispatchers.IO) {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.isEmpty()) return@withContext 0

        val list = mutableListOf<MeterReading>()
        for ((index, line) in lines.withIndex()) {
            val cols = splitCsvRow(line)
            if (cols.isEmpty()) continue

            val first = cols[0].lowercase()
            if (index == 0 && (first.contains("reading") || first.contains("date") || first.contains("timestamp") || first.contains("total"))) {
                continue
            }

            val date = parseTimestamp(cols.getOrElse(0) { "" }, cols.getOrElse(1) { "" })
            val total = cols.getOrElse(2) { "0" }.toDoubleOrNull() ?: 0.0
            val units = cols.getOrElse(3) { "0" }.toDoubleOrNull() ?: 0.0
            val isPaid = parseBoolean(cols.getOrElse(4) { "false" }, default = false)

            list.add(
                MeterReading(
                    id = UUID.randomUUID().toString(),
                    readingDate = date,
                    totalReading = total,
                    unitsConsumed = units,
                    isPaid = isPaid
                )
            )
        }
        if (list.isNotEmpty()) {
            meterDao.insertReadings(list)
        }
        list.size
    }

    suspend fun importUtilityBillsCsv(csv: String): Int = withContext(Dispatchers.IO) {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.isEmpty()) return@withContext 0

        val firstLineCols = splitCsvRow(lines[0])
        val isHeader = firstLineCols.any { col ->
            val c = col.lowercase()
            c.contains("bill") || c.contains("start") || c.contains("date") || c.contains("unit") || c.contains("amount") || c.contains("rate")
        }

        val dataRows = if (isHeader) lines.drop(1) else lines
        if (dataRows.isEmpty()) return@withContext 0

        val isEnhancedFormat = firstLineCols.any { it.contains("effective", ignoreCase = true) || it.contains("rate", ignoreCase = true) || it.contains("bill id", ignoreCase = true) } ||
                splitCsvRow(dataRows[0]).size >= 10

        val list = mutableListOf<UtilityBill>()
        for (line in dataRows) {
            val cols = splitCsvRow(line)
            if (cols.isEmpty()) continue

            if (isEnhancedFormat) {
                // Enhanced format: Bill ID (0), Billing Start Date (1), Billing End Date (2), Total Units (3), Effective Rate (4), Calculated Amount (5), Due Date (6), Payment Status (7), Transaction Ref (8), Breakdown (9), Start Timestamp (10), End Timestamp (11), Due Timestamp (12)
                val billId = cols.getOrElse(0) { "" }.ifBlank { UUID.randomUUID().toString() }
                val start = parseTimestamp(cols.getOrElse(10) { "" }, cols.getOrElse(1) { "" })
                val end = parseTimestamp(cols.getOrElse(11) { "" }, cols.getOrElse(2) { "" })
                val units = cols.getOrElse(3) { "0" }.toDoubleOrNull() ?: 0.0
                val effectiveRate = cols.getOrElse(4) { "0" }.toDoubleOrNull() ?: 0.0
                val amountRaw = cols.getOrElse(5) { "0" }.toDoubleOrNull() ?: 0.0
                val amount = if (amountRaw > 0.0) amountRaw else (units * effectiveRate)
                val due = parseTimestamp(cols.getOrElse(12) { "" }, cols.getOrElse(6) { "" })
                val isPaid = parseBoolean(cols.getOrElse(7) { "false" }, default = false)
                val txnId = cols.getOrElse(8) { "" }
                val note = cols.getOrElse(13) { "" }

                list.add(
                    UtilityBill(
                        id = billId,
                        startDate = start,
                        endDate = if (end > 0) end else System.currentTimeMillis(),
                        totalUnits = units,
                        calculatedAmount = amount,
                        dueDate = if (due > 0) due else end,
                        isPaid = isPaid,
                        transactionId = txnId.ifBlank { null },
                        note = note
                    )
                )
            } else {
                // Standard / Legacy format: Start Timestamp (0), End Timestamp (1), Billing Start Date (2), Billing End Date (3), Total Units (4), Calculated Amount (5), Due Date (6), Payment Status (7), Transaction Reference (8)
                val start = parseTimestamp(cols.getOrElse(0) { "" }, cols.getOrElse(2) { "" })
                val end = parseTimestamp(cols.getOrElse(1) { "" }, cols.getOrElse(3) { "" })
                val units = cols.getOrElse(4) { "0" }.toDoubleOrNull() ?: 0.0
                val amount = cols.getOrElse(5) { "0" }.toDoubleOrNull() ?: 0.0
                val due = parseTimestamp(cols.getOrElse(6) { "" }, "")
                val isPaid = parseBoolean(cols.getOrElse(7) { "true" }, default = true)
                val txnId = cols.getOrElse(8) { "" }

                list.add(
                    UtilityBill(
                        id = UUID.randomUUID().toString(),
                        startDate = start,
                        endDate = if (end > 0) end else System.currentTimeMillis(),
                        totalUnits = units,
                        calculatedAmount = amount,
                        dueDate = if (due > 0) due else end,
                        isPaid = isPaid,
                        transactionId = txnId.ifBlank { null }
                    )
                )
            }
        }
        if (list.isNotEmpty()) {
            meterDao.insertUtilityBills(list)
            if (meterDao.getAllElectricityRatesSync().isEmpty()) {
                val latestValidBill = list.filter { it.totalUnits > 0.0 && it.calculatedAmount > 0.0 }.maxByOrNull { it.endDate }
                if (latestValidBill != null) {
                    val derivedRate = Math.round((latestValidBill.calculatedAmount / latestValidBill.totalUnits) * 100.0) / 100.0
                    if (derivedRate > 0.0) {
                        meterDao.insertElectricityRate(
                            ElectricityRate(
                                effectiveDate = latestValidBill.startDate,
                                ratePerUnit = derivedRate
                            )
                        )
                    }
                }
            }
        }
        list.size
    }

    suspend fun importWalletWholeCsv(csv: String, fallbackBankAccountId: Int = 1): ImportOutcome = withContext(Dispatchers.IO) {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return@withContext ImportOutcome.Error("CSV is empty")

        var accCount = 0
        if (lines.any { it.startsWith("# BANK ACCOUNTS", ignoreCase = true) || it.startsWith("# BANK ACCOUNT DETAILS", ignoreCase = true) }) {
            val accSection = lines.dropWhile { !it.startsWith("# BANK", ignoreCase = true) }.drop(1).takeWhile { !it.startsWith("#") }.joinToString("\n")
            accCount = importBankAccountsCsv(accSection)
        }

        var txCount = 0
        if (lines.any { it.startsWith("# TRANSACTION HISTORY", ignoreCase = true) }) {
            val txSection = lines.dropWhile { !it.startsWith("# TRANSACTION HISTORY", ignoreCase = true) }.drop(1).takeWhile { !it.startsWith("#") }.joinToString("\n")
            txCount = importTransactionHistoryCsv(txSection)
        } else if (accCount == 0) {
            txCount = importTransactionHistoryCsv(csv)
        }

        ImportOutcome.Success("Imported $accCount bank accounts & $txCount transactions", count = accCount + txCount)
    }

    suspend fun importElectricityWholeCsv(csv: String): ImportOutcome = withContext(Dispatchers.IO) {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return@withContext ImportOutcome.Error("CSV is empty")

        var readingsCount = 0
        if (lines.any { it.startsWith("# METER READINGS", ignoreCase = true) }) {
            val readSection = lines.dropWhile { !it.startsWith("# METER READINGS", ignoreCase = true) }.drop(1).takeWhile { !it.startsWith("#") }.joinToString("\n")
            readingsCount = importMeterReadingsCsv(readSection)
        }

        var billsCount = 0
        if (lines.any { it.startsWith("# UTILITY BILLS", ignoreCase = true) }) {
            val billSection = lines.dropWhile { !it.startsWith("# UTILITY BILLS", ignoreCase = true) }.drop(1).takeWhile { !it.startsWith("#") }.joinToString("\n")
            billsCount = importUtilityBillsCsv(billSection)
        }

        if (readingsCount == 0 && billsCount == 0) {
            readingsCount = importMeterReadingsCsv(csv)
        }

        ImportOutcome.Success("Imported $readingsCount meter readings & $billsCount utility bills", count = readingsCount + billsCount)
    }

    suspend fun importFullBackupCsv(csv: String): ImportOutcome = withContext(Dispatchers.IO) {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return@withContext ImportOutcome.Error("CSV is empty")

        var totalCount = 0

        // 1. User Profile Section
        if (lines.any { it.startsWith("# USER PROFILE", ignoreCase = true) }) {
            val profSection = lines.dropWhile { !it.startsWith("# USER PROFILE", ignoreCase = true) }
                .drop(1).takeWhile { !it.startsWith("#") }
            val profDataLines = profSection.filter { !it.startsWith("Name,", ignoreCase = true) }
            if (profDataLines.isNotEmpty()) {
                val cols = splitCsvRow(profDataLines.first())
                if (cols.isNotEmpty()) {
                    val p = UserProfileEntity(
                        name = cols.getOrElse(0) { "" },
                        contact = cols.getOrElse(1) { "" },
                        currencySymbol = cols.getOrElse(2) { "₹" },
                        language = cols.getOrElse(3) { "en" },
                        themeMode = cols.getOrElse(4) { "Auto" }
                    )
                    userProfileDao.insertOrUpdateProfile(p)
                    totalCount++
                }
            }
        }

        // 2. Hotels Section
        val hotelIdMap = mutableMapOf<Int, Int>()
        if (lines.any { it.startsWith("# HOTELS", ignoreCase = true) }) {
            val hotelSection = lines.dropWhile { !it.startsWith("# HOTELS", ignoreCase = true) }
                .drop(1).takeWhile { !it.startsWith("#") }
            val hotelDataLines = hotelSection.filter { !it.startsWith("Hotel ID,", ignoreCase = true) }
            hotelDataLines.forEach { line ->
                val cols = splitCsvRow(line)
                if (cols.size >= 2) {
                    val origId = cols[0].toIntOrNull() ?: -1
                    val h = HotelEntity(
                        name = cols[1],
                        advancedPayment = cols.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                        phoneNumber = cols.getOrElse(3) { "" },
                        location = cols.getOrElse(4) { "" },
                        upiId = cols.getOrElse(5) { "" }
                    )
                    val newId = hotelDao.insertHotel(h).toInt()
                    if (origId != -1) hotelIdMap[origId] = newId
                    totalCount++
                }
            }
        }

        // 3. Menus Section
        if (lines.any { it.startsWith("# MENU ITEMS", ignoreCase = true) }) {
            val menuSection = lines.dropWhile { !it.startsWith("# MENU ITEMS", ignoreCase = true) }
                .drop(1).takeWhile { !it.startsWith("#") }.joinToString("\n")
            val targetHId = hotelIdMap.values.firstOrNull() ?: 1
            totalCount += importMenusCsv(menuSection, targetHId)
        }

        // 4. Order History Section
        if (lines.any { it.startsWith("# ORDER HISTORY", ignoreCase = true) }) {
            val histSection = lines.dropWhile { !it.startsWith("# ORDER HISTORY", ignoreCase = true) }
                .drop(1).takeWhile { !it.startsWith("#") }.joinToString("\n")
            val targetHId = hotelIdMap.values.firstOrNull() ?: 1
            totalCount += importHotelHistoryCsv(histSection, targetHId)
        }

        // 4b. Hotel Monthly Payments Section
        if (lines.any { it.startsWith("# HOTEL MONTHLY PAYMENTS", ignoreCase = true) }) {
            val payLines = lines.dropWhile { !it.startsWith("# HOTEL MONTHLY PAYMENTS", ignoreCase = true) }
                .drop(1).takeWhile { !it.startsWith("#") }
                .filter { it.isNotBlank() && !it.startsWith("Hotel ID", ignoreCase = true) }
            val payList = mutableListOf<HotelPaymentEntity>()
            val fallbackHId = hotelIdMap.values.firstOrNull() ?: 1
            for (line in payLines) {
                val cols = splitCsvRow(line)
                if (cols.size >= 4) {
                    val origHId = cols[0].toIntOrNull() ?: fallbackHId
                    val mappedHId = hotelIdMap[origHId] ?: fallbackHId
                    val yr = cols.getOrElse(1) { "0" }.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                    val mo = cols.getOrElse(2) { "0" }.toIntOrNull() ?: Calendar.getInstance().get(Calendar.MONTH)
                    val amt = cols.getOrElse(3) { "0" }.toDoubleOrNull() ?: 0.0
                    val pDate = cols.getOrElse(4) { "0" }.toLongOrNull() ?: System.currentTimeMillis()
                    val note = cols.getOrElse(5) { "" }
                    payList.add(
                        HotelPaymentEntity(
                            hotelId = mappedHId,
                            year = yr,
                            month = mo,
                            amountPaid = amt,
                            paymentDate = pDate,
                            note = note
                        )
                    )
                }
            }
            if (payList.isNotEmpty()) {
                hotelPaymentDao.insertPayments(payList)
                totalCount += payList.size
            }
        }

        // 5. Bank Accounts Section
        val bankIdMap = mutableMapOf<Int, Int>()
        if (lines.any { it.startsWith("# BANK ACCOUNTS", ignoreCase = true) }) {
            val bankSection = lines.dropWhile { !it.startsWith("# BANK ACCOUNTS", ignoreCase = true) }
                .drop(1).takeWhile { !it.startsWith("#") }
            val bankDataLines = bankSection.filter { 
                !it.startsWith("Account ID,", ignoreCase = true) && 
                !it.startsWith("Account Name,", ignoreCase = true) &&
                !it.startsWith("ID,", ignoreCase = true)
            }
            bankDataLines.forEach { line ->
                val cols = splitCsvRow(line)
                if (cols.isNotEmpty()) {
                    val isIdFirst = cols[0].toIntOrNull() != null && cols.size > 2
                    val origId = if (isIdFirst) cols[0].toIntOrNull() ?: -1 else -1
                    val name = if (isIdFirst) cols.getOrElse(1) { "" } else cols.getOrElse(0) { "" }
                    if (name.isNotBlank()) {
                        val balIndex = if (isIdFirst) 2 else 1
                        val hasInitBal = if (isIdFirst) (cols.size > 3 && cols[3].toDoubleOrNull() != null) else (cols.size > 2 && cols[2].toDoubleOrNull() != null)
                        val initBalIndex = if (hasInitBal) (if (isIdFirst) 3 else 2) else balIndex
                        val upiIndex = if (isIdFirst) (if (hasInitBal) 4 else 3) else (if (hasInitBal) 3 else 2)
                        val accNumIndex = upiIndex + 1
                        val ifscIndex = upiIndex + 2
                        val branchIndex = upiIndex + 3
                        val detailsIndex = upiIndex + 4

                        val balance = cols.getOrElse(balIndex) { "0" }.toDoubleOrNull() ?: 0.0
                        val initBal = cols.getOrElse(initBalIndex) { balance.toString() }.toDoubleOrNull() ?: balance
                        val upiId = cols.getOrElse(upiIndex) { "" }
                        val accNum = cols.getOrElse(accNumIndex) { "" }
                        val ifsc = cols.getOrElse(ifscIndex) { "" }
                        val branch = cols.getOrElse(branchIndex) { "" }
                        val details = cols.getOrElse(detailsIndex) { "" }

                        val b = BankAccountEntity(
                            name = name,
                            balance = balance,
                            initialBalance = initBal,
                            upiId = upiId,
                            accountNumber = accNum,
                            ifsc = ifsc,
                            branchName = branch,
                            details = details
                        )
                        val newId = bankAccountDao.insertBankAccount(b).toInt()
                        if (origId != -1) bankIdMap[origId] = newId
                        totalCount++
                    }
                }
            }
        }

        // 6. Transaction History Section
        if (lines.any { it.startsWith("# TRANSACTION HISTORY", ignoreCase = true) }) {
            val txSection = lines.dropWhile { !it.startsWith("# TRANSACTION HISTORY", ignoreCase = true) }
                .drop(1).takeWhile { !it.startsWith("#") }.joinToString("\n")
            totalCount += importTransactionHistoryCsv(txSection)
        }

        // 7. Meter Readings Section
        if (lines.any { it.startsWith("# METER READINGS", ignoreCase = true) }) {
            val rSection = lines.dropWhile { !it.startsWith("# METER READINGS", ignoreCase = true) }
                .drop(1).takeWhile { !it.startsWith("#") }.joinToString("\n")
            totalCount += importMeterReadingsCsv(rSection)
        }

        // 8. Utility Bills Section
        if (lines.any { it.startsWith("# UTILITY BILLS", ignoreCase = true) }) {
            val bSection = lines.dropWhile { !it.startsWith("# UTILITY BILLS", ignoreCase = true) }
                .drop(1).takeWhile { !it.startsWith("#") }.joinToString("\n")
            totalCount += importUtilityBillsCsv(bSection)
        }

        ImportOutcome.Success("Full application backup restored successfully ($totalCount records)", count = totalCount)
    }

    // ==========================================
    // UNIVERSAL IMPORT (Handles JSON or CSV)
    // ==========================================

    suspend fun importUniversalText(text: String, contextType: BackupDataType, hotelId: Int = 1): ImportOutcome = withContext(Dispatchers.IO) {
        val trimmed = text.trim().removePrefix("\uFEFF").trim()
        if (trimmed.isEmpty()) {
            return@withContext ImportOutcome.Error("Input text is empty")
        }

        // 1. If JSON structure detected, try JSON importer
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return@withContext importJson(trimmed, targetHotelId = hotelId)
        }

        // 2. Check if this is a Full App Backup CSV
        if (trimmed.contains("# FULL APPLICATION BACKUP", ignoreCase = true) || trimmed.contains("# USER PROFILE", ignoreCase = true) ||
            (trimmed.contains("# HOTELS", ignoreCase = true) && (trimmed.contains("# BANK ACCOUNTS", ignoreCase = true) || trimmed.contains("# METER READINGS", ignoreCase = true)))) {
            return@withContext importFullBackupCsv(trimmed)
        }

        // 3. Check if this is a whole hotel multi-section CSV
        if (trimmed.contains("# HOTEL DETAILS", ignoreCase = true) || (trimmed.contains("# MENU ITEMS", ignoreCase = true) && trimmed.contains("# ORDER HISTORY", ignoreCase = true))) {
            return@withContext importWholeHotelCsv(trimmed, targetHotelId = hotelId)
        }

        // 4. Check if this is a wallet whole multi-section CSV
        if (trimmed.contains("# BANK ACCOUNTS", ignoreCase = true) || trimmed.contains("# BANK ACCOUNT DETAILS", ignoreCase = true) ||
            (trimmed.contains("# TRANSACTION HISTORY", ignoreCase = true) && trimmed.contains("# BANK", ignoreCase = true))) {
            return@withContext importWalletWholeCsv(trimmed)
        }

        // 5. Check if this is an electricity whole multi-section CSV
        if (trimmed.contains("# METER READINGS", ignoreCase = true) && trimmed.contains("# UTILITY BILLS", ignoreCase = true)) {
            return@withContext importElectricityWholeCsv(trimmed)
        }

        // 6. Header auto-detection for standalone CSV exports (matches exact exported headers)
        val firstFewLines = trimmed.lines().take(5).joinToString("\n").lowercase()
        when {
            firstFewLines.contains("bangla name") || firstFewLines.contains("day available") ||
            firstFewLines.contains("is add-on") || firstFewLines.contains("english name") -> {
                val count = importMenusCsv(trimmed, hotelId)
                return@withContext ImportOutcome.Success("Imported $count menu item(s)", count = count)
            }
            firstFewLines.contains("meal groups") || firstFewLines.contains("order items") ||
            firstFewLines.contains("meal session") || firstFewLines.contains("order total") ||
            (firstFewLines.contains("order id") && firstFewLines.contains("session")) -> {
                val count = importHotelHistoryCsv(trimmed, hotelId)
                return@withContext ImportOutcome.Success("Imported $count order history record(s)", count = count)
            }
            firstFewLines.contains("current balance") || firstFewLines.contains("initial balance") ||
            (firstFewLines.contains("account name") && (firstFewLines.contains("ifsc") || firstFewLines.contains("upi id") || firstFewLines.contains("branch"))) -> {
                val count = importBankAccountsCsv(trimmed)
                return@withContext ImportOutcome.Success("Imported $count bank account(s)", count = count)
            }
            firstFewLines.contains("transaction type") || firstFewLines.contains("expense/income") ||
            firstFewLines.contains("bank account id") || (firstFewLines.contains("category") && firstFewLines.contains("amount")) -> {
                val count = importTransactionHistoryCsv(trimmed)
                return@withContext ImportOutcome.Success("Imported $count transaction(s)", count = count)
            }
            firstFewLines.contains("total reading") || firstFewLines.contains("units consumed") ||
            firstFewLines.contains("reading timestamp") || firstFewLines.contains("reading (kwh)") -> {
                val count = importMeterReadingsCsv(trimmed)
                return@withContext ImportOutcome.Success("Imported $count meter reading(s)", count = count)
            }
            firstFewLines.contains("billing start") || firstFewLines.contains("billing end") ||
            firstFewLines.contains("effective rate") || firstFewLines.contains("calculated amount") ||
            (firstFewLines.contains("bill id") && firstFewLines.contains("units")) -> {
                val count = importUtilityBillsCsv(trimmed)
                return@withContext ImportOutcome.Success("Imported $count utility bill(s)", count = count)
            }
        }

        // 7. Otherwise handle as CSV based on context type
        try {
            val count = when (contextType) {
                BackupDataType.HOTEL_MENUS -> importMenusCsv(trimmed, hotelId)
                BackupDataType.HOTEL_HISTORY -> importHotelHistoryCsv(trimmed, hotelId)
                BackupDataType.HOTEL_INFO -> {
                    return@withContext importWholeHotelCsv(trimmed, hotelId)
                }
                BackupDataType.HOTEL_WHOLE -> {
                    if (trimmed.contains("# HOTEL DETAILS", ignoreCase = true) || trimmed.contains("# ORDER HISTORY", ignoreCase = true)) {
                        return@withContext importWholeHotelCsv(trimmed, hotelId)
                    } else {
                        importMenusCsv(trimmed, hotelId)
                    }
                }
                BackupDataType.WALLET_WHOLE -> {
                    if (trimmed.contains("# BANK", ignoreCase = true) || trimmed.contains("# TRANSACTION", ignoreCase = true)) {
                        return@withContext importWalletWholeCsv(trimmed)
                    } else {
                        importTransactionHistoryCsv(trimmed)
                    }
                }
                BackupDataType.BANK_ACCOUNTS -> importBankAccountsCsv(trimmed)
                BackupDataType.SINGLE_BANK_ACCOUNT -> {
                    if (trimmed.contains("# BANK", ignoreCase = true) || trimmed.contains("# TRANSACTION", ignoreCase = true)) {
                        return@withContext importWalletWholeCsv(trimmed)
                    } else {
                        importBankAccountsCsv(trimmed)
                    }
                }
                BackupDataType.TRANSACTION_HISTORY -> importTransactionHistoryCsv(trimmed)
                BackupDataType.ELECTRICITY_WHOLE -> {
                    if (trimmed.contains("# METER", ignoreCase = true) || trimmed.contains("# UTILITY", ignoreCase = true)) {
                        return@withContext importElectricityWholeCsv(trimmed)
                    } else {
                        importMeterReadingsCsv(trimmed)
                    }
                }
                BackupDataType.METER_READINGS -> importMeterReadingsCsv(trimmed)
                BackupDataType.UTILITY_BILLS -> importUtilityBillsCsv(trimmed)
                BackupDataType.ELECTRICITY_RATES -> {
                    val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
                    var c = 0
                    for ((idx, line) in lines.withIndex()) {
                        val cols = splitCsvRow(line)
                        if (idx == 0 && cols.firstOrNull()?.contains("effective", ignoreCase = true) == true) continue
                        val ts = parseTimestamp(cols.getOrElse(0) { "" }, cols.getOrElse(1) { "" })
                        val rate = cols.lastOrNull()?.toDoubleOrNull() ?: 0.0
                        if (rate > 0.0) {
                            meterDao.insertElectricityRate(ElectricityRate(effectiveDate = ts, ratePerUnit = rate))
                            c++
                        }
                    }
                    c
                }
                BackupDataType.SETTINGS, BackupDataType.FULL_BACKUP -> {
                    return@withContext importFullBackupCsv(trimmed)
                }
            }
            ImportOutcome.Success("Successfully imported $count record(s)", count = count)
        } catch (e: Exception) {
            ImportOutcome.Error(e.message ?: "Failed to import CSV data")
        }
    }

    // ==========================================
    // UTILITIES
    // ==========================================

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun splitCsvRow(row: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < row.length) {
            val ch = row[i]
            if (ch == '"') {
                if (inQuotes && i + 1 < row.length && row[i + 1] == '"') {
                    current.append('"')
                    i++ // skip the escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(ch)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    private fun parseBoolean(value: String, default: Boolean): Boolean {
        val clean = value.lowercase().trim()
        return when (clean) {
            "true", "1", "yes", "y", "t", "on", "paid" -> true
            "false", "0", "no", "n", "f", "off", "unpaid", "pending" -> false
            else -> default
        }
    }

    private fun parseTimestamp(col1: String, col2: String): Long {
        col1.toLongOrNull()?.let { if (it > 1000000000L) return it }
        col2.toLongOrNull()?.let { if (it > 1000000000L) return it }

        // Try date formats without returning 0L when dateFormat returns null
        for (str in listOf(col2, col1)) {
            val trimmed = str.trim()
            if (trimmed.isNotBlank()) {
                try {
                    val parsedDateTime = dateFormat.parse(trimmed)?.time
                    if (parsedDateTime != null && parsedDateTime > 0L) return parsedDateTime
                } catch (_: Exception) {}
                try {
                    val parsedDateOnly = dateOnlyFormat.parse(trimmed)?.time
                    if (parsedDateOnly != null && parsedDateOnly > 0L) return parsedDateOnly
                } catch (_: Exception) {}
            }
        }
        return System.currentTimeMillis()
    }
}
