package com.example.utils

import com.example.data.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * SyncEngine: The in-memory analysis and conflict detection brain for importing data.
 * Performs deep semantic inspection, parent mapping, menu differential checks,
 * and fuzzy/strict deduplication without mutating the database.
 */

enum class ConflictResolution {
    KEEP_LOCAL,
    TAKE_IMPORTED,
    KEEP_BOTH
}

enum class ParentEntityType {
    HOTEL,
    BANK_ACCOUNT,
    OWNER_ACCOUNT
}

sealed class ParentResolution {
    data class MapToExisting(val targetLocalId: Int) : ParentResolution()
    data class CreateNew(val senderName: String?) : ParentResolution()
}

data class UnmappedParent(
    val entityKey: String, // e.g. "hotel_1", "bank_2"
    val entityType: ParentEntityType,
    val originalId: Int,
    val importedName: String,
    val importedSubtitle: String = "",
    val senderName: String? = null,
    val rawHotel: HotelEntity? = null,
    val rawBankAccount: BankAccountEntity? = null,
    val rawOwnerAccount: OwnerAccountEntity? = null
)

data class PendingNewMenuItem(
    val itemKey: String,
    val targetHotelId: Int,
    val targetHotelName: String,
    val menuItem: MenuEntity
)

sealed class ConflictItem {
    abstract val conflictKey: String
    abstract val title: String
    abstract val subtitle: String
    abstract val timestamp: Long
    abstract val isImportedNewer: Boolean

    data class TransactionConflict(
        override val conflictKey: String,
        override val title: String,
        override val subtitle: String,
        override val timestamp: Long,
        override val isImportedNewer: Boolean,
        val localExpense: ExtraExpenseEntity,
        val importedExpense: ExtraExpenseEntity,
        val timeDiffMinutes: Long,
        val accountName: String
    ) : ConflictItem()

    data class MealConflict(
        override val conflictKey: String,
        override val title: String,
        override val subtitle: String,
        override val timestamp: Long,
        override val isImportedNewer: Boolean,
        val localMeal: HotelTransactionEntity,
        val importedMeal: HotelTransactionEntity,
        val hotelName: String,
        val localItemsSummary: String,
        val importedItemsSummary: String
    ) : ConflictItem()

    data class MeterReadingConflict(
        override val conflictKey: String,
        override val title: String,
        override val subtitle: String,
        override val timestamp: Long,
        override val isImportedNewer: Boolean,
        val localReading: MeterReading,
        val importedReading: MeterReading
    ) : ConflictItem()

    data class UtilityBillConflict(
        override val conflictKey: String,
        override val title: String,
        override val subtitle: String,
        override val timestamp: Long,
        override val isImportedNewer: Boolean,
        val localBill: UtilityBill,
        val importedBill: UtilityBill
    ) : ConflictItem()

    data class MenuItemConflict(
        override val conflictKey: String,
        override val title: String,
        override val subtitle: String,
        override val timestamp: Long,
        override val isImportedNewer: Boolean,
        val localMenuItem: MenuEntity,
        val importedMenuItem: MenuEntity,
        val hotelName: String
    ) : ConflictItem()
}

data class ParsedImportPayload(
    val senderName: String? = null,
    val hotels: List<HotelEntity> = emptyList(),
    val menus: List<MenuEntity> = emptyList(),
    val transactions: List<HotelTransactionEntity> = emptyList(),
    val hotelPayments: List<HotelPaymentEntity> = emptyList(),
    val stickyNotes: List<StickyNoteEntity> = emptyList(),
    val bankAccounts: List<BankAccountEntity> = emptyList(),
    val expenses: List<ExtraExpenseEntity> = emptyList(),
    val ownerAccounts: List<OwnerAccountEntity> = emptyList(),
    val meterReadings: List<MeterReading> = emptyList(),
    val utilityBills: List<UtilityBill> = emptyList(),
    val userProfile: UserProfileEntity? = null,
    val dataType: String = ""
)

data class SyncAnalysisResult(
    val payload: ParsedImportPayload,
    val senderName: String?,
    val autoLinkedHotelIds: Map<Int, Int> = emptyMap(), // importedOrigId -> localId
    val autoLinkedBankIds: Map<Int, Int> = emptyMap(),
    val autoLinkedOwnerIds: Map<Int, Int> = emptyMap(),
    val unmappedParents: List<UnmappedParent> = emptyList(),
    val pendingNewMenuItems: List<PendingNewMenuItem> = emptyList(),
    val conflicts: List<ConflictItem> = emptyList(),
    val directNewHotels: List<HotelEntity> = emptyList(),
    val directNewBankAccounts: List<BankAccountEntity> = emptyList(),
    val directNewMenus: List<MenuEntity> = emptyList(),
    val directNewMeals: List<HotelTransactionEntity> = emptyList(),
    val directNewExpenses: List<ExtraExpenseEntity> = emptyList(),
    val directNewReadings: List<MeterReading> = emptyList(),
    val directNewBills: List<UtilityBill> = emptyList(),
    val directNewHotelPayments: List<HotelPaymentEntity> = emptyList(),
    val directNewStickyNotes: List<StickyNoteEntity> = emptyList(),
    val hasUnresolvedParents: Boolean = unmappedParents.isNotEmpty(),
    val hasPendingMenuItems: Boolean = pendingNewMenuItems.isNotEmpty(),
    val hasConflicts: Boolean = conflicts.isNotEmpty()
)

data class MergePlan(
    val senderName: String?,
    val hotelsToInsert: List<HotelEntity> = emptyList(),
    val hotelsToUpdate: List<HotelEntity> = emptyList(),
    val bankAccountsToInsert: List<BankAccountEntity> = emptyList(),
    val bankAccountsToUpdate: List<BankAccountEntity> = emptyList(),
    val ownerAccountsToInsert: List<OwnerAccountEntity> = emptyList(),
    val menusToInsert: List<MenuEntity> = emptyList(),
    val menusToUpdate: List<MenuEntity> = emptyList(),
    val mealsToInsert: List<HotelTransactionEntity> = emptyList(),
    val mealsToUpdate: List<HotelTransactionEntity> = emptyList(),
    val expensesToInsert: List<ExtraExpenseEntity> = emptyList(),
    val expensesToUpdate: List<ExtraExpenseEntity> = emptyList(),
    val readingsToInsert: List<MeterReading> = emptyList(),
    val readingsToUpdate: List<MeterReading> = emptyList(),
    val billsToInsert: List<UtilityBill> = emptyList(),
    val billsToUpdate: List<UtilityBill> = emptyList(),
    val hotelPaymentsToInsert: List<HotelPaymentEntity> = emptyList(),
    val stickyNotesToInsert: List<StickyNoteEntity> = emptyList(),
    val userProfileToUpdate: UserProfileEntity? = null,
    val totalAffectedCount: Int = 0
)

class SyncEngine(private val db: AppDatabase) {

    private val hotelDao = db.hotelDao()
    private val hotelPaymentDao = db.hotelPaymentDao()
    private val menuDao = db.menuDao()
    private val transactionDao = db.transactionDao()
    private val bankAccountDao = db.bankAccountDao()
    private val extraExpenseDao = db.extraExpenseDao()
    private val ownerAccountDao = db.ownerAccountDao()
    private val meterDao = db.meterDao()
    private val userProfileDao = db.userProfileDao()

    companion object {
        const val TRANSACTION_FUZZY_WINDOW_MS = 5 * 60 * 1000L // ±5 minutes window

        fun startOfDay(timestampMillis: Long): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = timestampMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun normalizeName(name: String): String {
            return name.trim().lowercase(Locale.getDefault()).replace("\\s+".toRegex(), " ")
        }
    }

    /**
     * Parses arbitrary JSON payload into structured in-memory entities.
     */
    fun parseJsonPayload(jsonString: String): ParsedImportPayload {
        val root = JSONObject(jsonString.trim())
        var senderName: String? = null
        if (root.has("senderName")) {
            senderName = root.optString("senderName", "").ifBlank { null }
        } else if (root.has("ownerName")) {
            senderName = root.optString("ownerName", "").ifBlank { null }
        }

        val hotels = mutableListOf<HotelEntity>()
        val menus = mutableListOf<MenuEntity>()
        val transactions = mutableListOf<HotelTransactionEntity>()
        val hotelPayments = mutableListOf<HotelPaymentEntity>()
        val stickyNotes = mutableListOf<StickyNoteEntity>()
        val bankAccounts = mutableListOf<BankAccountEntity>()
        val expenses = mutableListOf<ExtraExpenseEntity>()
        val ownerAccounts = mutableListOf<OwnerAccountEntity>()
        val meterReadings = mutableListOf<MeterReading>()
        val utilityBills = mutableListOf<UtilityBill>()
        var userProfile: UserProfileEntity? = null

        // 1. User Profile
        if (root.has("userProfile")) {
            val pObj = root.getJSONObject("userProfile")
            val pName = pObj.optString("name", "")
            if (!pName.isBlank()) senderName = pName
            userProfile = UserProfileEntity(
                name = pName,
                contact = pObj.optString("contact", ""),
                currencySymbol = pObj.optString("currencySymbol", "₹"),
                language = pObj.optString("language", "en"),
                themeMode = pObj.optString("themeMode", "Auto"),
                enableSupplementaryAddons = pObj.optBoolean("enableSupplementaryAddons", true)
            )
        }

        // 2. Legacy Hotel format ("h", "m", "t")
        if (root.has("h") && root.has("m")) {
            val hJson = root.getJSONObject("h")
            val hotel = HotelEntity(
                id = 1,
                name = hJson.getString("n"),
                location = hJson.optString("loc", ""),
                advancedPayment = hJson.optDouble("adv", 0.0),
                importedFrom = senderName
            )
            hotels.add(hotel)

            val mArray = root.getJSONArray("m")
            for (i in 0 until mArray.length()) {
                val mJson = mArray.getJSONObject(i)
                menus.add(
                    MenuEntity(
                        hotelId = 1,
                        banglaName = mJson.optString("bn", ""),
                        englishName = mJson.optString("en", ""),
                        price = mJson.optDouble("pr", 0.0),
                        isDayMenu = mJson.optInt("dy", 1) == 1,
                        isNightMenu = mJson.optInt("nt", 1) == 1,
                        isAddOn = mJson.optInt("ad", 0) == 1,
                        emoji = "🍽️",
                        type = "Common"
                    )
                )
            }

            if (root.has("t")) {
                val tArray = root.getJSONArray("t")
                for (i in 0 until tArray.length()) {
                    val tJson = tArray.getJSONObject(i)
                    transactions.add(
                        HotelTransactionEntity(
                            hotelId = 1,
                            timestamp = tJson.optLong("ts", System.currentTimeMillis()),
                            isNight = tJson.optInt("n", 0) == 1,
                            totalAmount = tJson.optDouble("tot", 0.0),
                            itemsSerialized = tJson.optString("it", "")
                        )
                    )
                }
            }

            return ParsedImportPayload(
                senderName = senderName,
                hotels = hotels,
                menus = menus,
                transactions = transactions,
                dataType = "LEGACY_HOTEL"
            )
        }

        // 3. Single Hotel ("hotel")
        if (root.has("hotel")) {
            val hObj = root.getJSONObject("hotel")
            val hotel = HotelEntity(
                id = hObj.optInt("id", 1),
                name = hObj.optString("name", "Imported Hotel"),
                advancedPayment = hObj.optDouble("advancedPayment", 0.0),
                phoneNumber = hObj.optString("phoneNumber", ""),
                location = hObj.optString("location", ""),
                upiId = hObj.optString("upiId", ""),
                qrCodeUri = com.example.data.QrMediaCodec.resolveImportedQrUri(hObj, com.example.data.AppDatabase.getAppContext()),
                notes = hObj.optString("notes", ""),
                whatsappNumber = hObj.optString("whatsappNumber", ""),
                importedFrom = senderName
            )
            hotels.add(hotel)
        }

        // 4. Hotels array ("hotels")
        if (root.has("hotels")) {
            val hArr = root.getJSONArray("hotels")
            for (i in 0 until hArr.length()) {
                val hObj = hArr.getJSONObject(i)
                hotels.add(
                    HotelEntity(
                        id = hObj.optInt("id", i + 1),
                        name = hObj.optString("name", "Hotel ${i + 1}"),
                        advancedPayment = hObj.optDouble("advancedPayment", 0.0),
                        phoneNumber = hObj.optString("phoneNumber", ""),
                        location = hObj.optString("location", ""),
                        upiId = hObj.optString("upiId", ""),
                        qrCodeUri = com.example.data.QrMediaCodec.resolveImportedQrUri(hObj, com.example.data.AppDatabase.getAppContext()),
                        notes = hObj.optString("notes", ""),
                        whatsappNumber = hObj.optString("whatsappNumber", ""),
                        importedFrom = senderName
                    )
                )
            }
        }

        // 5. Menus array ("menus")
        if (root.has("menus")) {
            val mArr = root.getJSONArray("menus")
            val defaultHotelId = root.optInt("hotelId", hotels.firstOrNull()?.id ?: 1)
            for (i in 0 until mArr.length()) {
                val mObj = mArr.getJSONObject(i)
                menus.add(
                    MenuEntity(
                        id = mObj.optInt("id", 0),
                        hotelId = mObj.optInt("hotelId", defaultHotelId),
                        banglaName = mObj.optString("banglaName", mObj.optString("name", "")),
                        englishName = mObj.optString("englishName", mObj.optString("name", "")),
                        price = mObj.optDouble("price", 0.0),
                        isDayMenu = mObj.optBoolean("isDayMenu", true),
                        isNightMenu = mObj.optBoolean("isNightMenu", true),
                        isAddOn = mObj.optBoolean("isAddOn", false),
                        emoji = mObj.optString("emoji", "🍽️"),
                        type = mObj.optString("type", "Common"),
                        imageUri = mObj.optString("imageUri", "").ifBlank { null }
                    )
                )
            }
        }

        // 6. Transactions array ("transactions")
        if (root.has("transactions")) {
            val tArr = root.getJSONArray("transactions")
            val defaultHotelId = root.optInt("hotelId", hotels.firstOrNull()?.id ?: 1)
            for (i in 0 until tArr.length()) {
                val tObj = tArr.getJSONObject(i)
                val serialized = extractSerializedItems(tObj)
                transactions.add(
                    HotelTransactionEntity(
                        id = tObj.optInt("id", 0),
                        hotelId = tObj.optInt("hotelId", defaultHotelId),
                        timestamp = tObj.optLong("timestamp", System.currentTimeMillis()),
                        isNight = tObj.optBoolean("isNight", false),
                        itemsSerialized = serialized,
                        totalAmount = tObj.optDouble("totalAmount", 0.0)
                    )
                )
            }
        }

        // 7. Single Bank Account ("bankAccount")
        if (root.has("bankAccount")) {
            val bObj = root.getJSONObject("bankAccount")
            val bal = bObj.optDouble("balance", 0.0)
            bankAccounts.add(
                BankAccountEntity(
                    id = bObj.optInt("id", 1),
                    name = bObj.optString("name", "Imported Account"),
                    balance = bal,
                    initialBalance = bObj.optDouble("initialBalance", bal),
                    details = bObj.optString("details", ""),
                    qrCodeUri = com.example.data.QrMediaCodec.resolveImportedQrUri(bObj, com.example.data.AppDatabase.getAppContext()),
                    upiId = bObj.optString("upiId", ""),
                    accountNumber = bObj.optString("accountNumber", ""),
                    ifsc = bObj.optString("ifsc", ""),
                    branchName = bObj.optString("branchName", ""),
                    importedFrom = senderName
                )
            )
        }

        // 8. Bank accounts array ("bankAccounts")
        if (root.has("bankAccounts")) {
            val bArr = root.getJSONArray("bankAccounts")
            for (i in 0 until bArr.length()) {
                val bObj = bArr.getJSONObject(i)
                val bal = bObj.optDouble("balance", 0.0)
                bankAccounts.add(
                    BankAccountEntity(
                        id = bObj.optInt("id", i + 1),
                        name = bObj.optString("name", "Account ${i + 1}"),
                        balance = bal,
                        initialBalance = bObj.optDouble("initialBalance", bal),
                        details = bObj.optString("details", ""),
                        qrCodeUri = com.example.data.QrMediaCodec.resolveImportedQrUri(bObj, com.example.data.AppDatabase.getAppContext()),
                        upiId = bObj.optString("upiId", ""),
                        accountNumber = bObj.optString("accountNumber", ""),
                        ifsc = bObj.optString("ifsc", ""),
                        branchName = bObj.optString("branchName", ""),
                        importedFrom = senderName
                    )
                )
            }
        }

        // 9. Expenses array ("expenses")
        if (root.has("expenses")) {
            val eArr = root.getJSONArray("expenses")
            val defaultBankId = root.optInt("bankAccountId", bankAccounts.firstOrNull()?.id ?: 1)
            for (i in 0 until eArr.length()) {
                val eObj = eArr.getJSONObject(i)
                expenses.add(
                    ExtraExpenseEntity(
                        id = eObj.optInt("id", 0),
                        timestamp = eObj.optLong("timestamp", System.currentTimeMillis()),
                        name = eObj.optString("name", "Expense"),
                        amount = eObj.optDouble("amount", 0.0),
                        bankAccountId = eObj.optInt("bankAccountId", defaultBankId),
                        type = eObj.optString("type", "EXPENSE"),
                        category = eObj.optString("category", "General"),
                        notes = eObj.optString("notes", "")
                    )
                )
            }
        }

        // 10. Owner accounts ("ownerAccounts")
        if (root.has("ownerAccounts")) {
            val oArr = root.getJSONArray("ownerAccounts")
            for (i in 0 until oArr.length()) {
                val oObj = oArr.getJSONObject(i)
                ownerAccounts.add(
                    OwnerAccountEntity(
                        id = oObj.optInt("id", i + 1),
                        ownerName = oObj.optString("ownerName", oObj.optString("name", "Owner Account")),
                        ownerMobile = oObj.optString("ownerMobile", ""),
                        accountName = oObj.optString("accountName", ""),
                        accountNumber = oObj.optString("accountNumber", ""),
                        ifscCode = oObj.optString("ifscCode", oObj.optString("ifsc", "")),
                        branchName = oObj.optString("branchName", oObj.optString("branch", "")),
                        upiId = oObj.optString("upiId", ""),
                        qrCodeUri = com.example.data.QrMediaCodec.resolveImportedQrUri(oObj, com.example.data.AppDatabase.getAppContext()),
                        details = oObj.optString("details", ""),
                        importedFrom = senderName
                    )
                )
            }
        }

        // 11. Meter readings ("meterReadings")
        if (root.has("meterReadings")) {
            val rArr = root.getJSONArray("meterReadings")
            for (i in 0 until rArr.length()) {
                val rObj = rArr.getJSONObject(i)
                meterReadings.add(
                    MeterReading(
                        id = rObj.optString("id", UUID.randomUUID().toString()),
                        readingDate = rObj.optLong("readingDate", System.currentTimeMillis()),
                        totalReading = rObj.optDouble("totalReading", 0.0),
                        unitsConsumed = rObj.optDouble("unitsConsumed", 0.0),
                        isPaid = rObj.optBoolean("isPaid", false)
                    )
                )
            }
        }

        // 12. Utility bills ("utilityBills")
        if (root.has("utilityBills")) {
            val uArr = root.getJSONArray("utilityBills")
            for (i in 0 until uArr.length()) {
                val uObj = uArr.getJSONObject(i)
                val endDate = uObj.optLong("endDate", System.currentTimeMillis())
                utilityBills.add(
                    UtilityBill(
                        id = uObj.optString("id", UUID.randomUUID().toString()),
                        startDate = uObj.optLong("startDate", endDate - (30L * 24 * 3600 * 1000)),
                        endDate = endDate,
                        totalUnits = uObj.optDouble("totalUnits", 0.0),
                        calculatedAmount = uObj.optDouble("calculatedAmount", 0.0),
                        dueDate = uObj.optLong("dueDate", endDate),
                        isPaid = uObj.optBoolean("isPaid", true),
                        transactionId = uObj.optString("transactionId", "").ifBlank { null },
                        note = uObj.optString("note", "")
                    )
                )
            }
        }

        // 13. Hotel Payments ("hotelPayments")
        if (root.has("hotelPayments")) {
            val payArr = root.getJSONArray("hotelPayments")
            val defaultHotelId = root.optInt("hotelId", hotels.firstOrNull()?.id ?: 1)
            for (i in 0 until payArr.length()) {
                val pObj = payArr.getJSONObject(i)
                val payDate = pObj.optLong("paymentDate", pObj.optLong("timestamp", System.currentTimeMillis()))
                val cal = Calendar.getInstance().apply { timeInMillis = payDate }
                hotelPayments.add(
                    HotelPaymentEntity(
                        hotelId = pObj.optInt("hotelId", defaultHotelId),
                        year = pObj.optInt("year", cal.get(Calendar.YEAR)),
                        month = pObj.optInt("month", cal.get(Calendar.MONTH)),
                        amountPaid = pObj.optDouble("amountPaid", pObj.optDouble("amount", 0.0)),
                        paymentDate = payDate,
                        note = pObj.optString("note", "")
                    )
                )
            }
        }

        // 14. Sticky Notes ("stickyNotes")
        if (root.has("stickyNotes")) {
            val sArr = root.getJSONArray("stickyNotes")
            for (i in 0 until sArr.length()) {
                val sObj = sArr.getJSONObject(i)
                stickyNotes.add(
                    StickyNoteEntity(
                        title = sObj.optString("title", ""),
                        subtitle = sObj.optString("subtitle", ""),
                        bodyText = sObj.optString("bodyText", sObj.optString("content", "")),
                        textAlignment = sObj.optString("textAlignment", sObj.optString("alignment", "START")),
                        colorIndex = sObj.optInt("colorIndex", 0),
                        sortOrder = sObj.optInt("sortOrder", i),
                        createdAt = sObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = sObj.optLong("updatedAt", sObj.optLong("lastUpdatedMillis", System.currentTimeMillis()))
                    )
                )
            }
        }

        return ParsedImportPayload(
            senderName = senderName,
            hotels = hotels,
            menus = menus,
            transactions = transactions,
            hotelPayments = hotelPayments,
            stickyNotes = stickyNotes,
            bankAccounts = bankAccounts,
            expenses = expenses,
            ownerAccounts = ownerAccounts,
            meterReadings = meterReadings,
            utilityBills = utilityBills,
            userProfile = userProfile,
            dataType = root.optString("dataType", "IMPORT")
        )
    }

    private fun extractSerializedItems(tObj: JSONObject): String {
        if (tObj.has("itemsSerialized")) return tObj.getString("itemsSerialized")
        if (tObj.has("mealGroups")) {
            val mgArr = tObj.getJSONArray("mealGroups")
            val groupsList = mutableListOf<MealGroup>()
            for (g in 0 until mgArr.length()) {
                val gObj = mgArr.getJSONObject(g)
                val itArr = gObj.optJSONArray("items") ?: JSONArray()
                val items = mutableListOf<OrderedItem>()
                for (it in 0 until itArr.length()) {
                    val item = itArr.getJSONObject(it)
                    items.add(
                        OrderedItem(
                            name = item.optString("name", "Item"),
                            quantity = item.optInt("quantity", 1),
                            price = item.optDouble("price", 0.0)
                        )
                    )
                }
                groupsList.add(MealGroup(items = items))
            }
            return TransactionSerializer.serializeGroups(groupsList)
        }
        return ""
    }

    /**
     * Primary entry-point for in-memory sync analysis.
     * Takes raw imported JSON and cross-references Room database state to generate a full analysis report.
     */
    suspend fun analyzeImport(
        jsonString: String,
        targetHotelId: Int? = null,
        targetBankAccountId: Int? = null,
        explicitSenderName: String? = null
    ): SyncAnalysisResult = withContext(Dispatchers.IO) {
        val payload = parseJsonPayload(jsonString)
        val senderName = explicitSenderName ?: payload.senderName

        val localHotels = hotelDao.getAllHotelsSync().filter { !it.isDeleted }
        val localBankAccounts = bankAccountDao.getAllBankAccountsSync().filter { !it.isDeleted }
        val localOwnerAccounts = ownerAccountDao.getAllOwnerAccountsSync().filter { !it.isDeleted }
        val localMenus = menuDao.getAllMenuItemsSync().filter { !it.isDeleted }
        val localMeals = transactionDao.getAllTransactionsSync().filter { !it.isDeleted }
        val localExpenses = extraExpenseDao.getAllExpensesSync().filter { !it.isDeleted }
        val localReadings = meterDao.getAllReadingsSync().filter { !it.isDeleted }
        val localBills = meterDao.getAllUtilityBillsSync().filter { !it.isDeleted }

        val autoLinkedHotelIds = mutableMapOf<Int, Int>() // importedId -> localId
        val unmappedParents = mutableListOf<UnmappedParent>()

        // 1. HOTEL MAPPING
        if (payload.hotels.isNotEmpty()) {
            payload.hotels.forEach { impHotel ->
                val normalizedImp = normalizeName(impHotel.name)
                val exactMatch = localHotels.firstOrNull { normalizeName(it.name) == normalizedImp }
                if (exactMatch != null) {
                    autoLinkedHotelIds[impHotel.id] = exactMatch.id
                } else if (targetHotelId != null && payload.hotels.size == 1) {
                    // Explicit target override provided (e.g. from screen context)
                    autoLinkedHotelIds[impHotel.id] = targetHotelId
                } else {
                    unmappedParents.add(
                        UnmappedParent(
                            entityKey = "hotel_${impHotel.id}",
                            entityType = ParentEntityType.HOTEL,
                            originalId = impHotel.id,
                            importedName = impHotel.name,
                            importedSubtitle = listOfNotNull(impHotel.location.ifBlank { null }, impHotel.phoneNumber.ifBlank { null }).joinToString(" • "),
                            senderName = senderName,
                            rawHotel = impHotel
                        )
                    )
                }
            }
        } else if (targetHotelId != null) {
            autoLinkedHotelIds[1] = targetHotelId
        } else if (localHotels.isNotEmpty()) {
            autoLinkedHotelIds[1] = localHotels.first().id
        }

        // 2. BANK ACCOUNT MAPPING
        val autoLinkedBankIds = mutableMapOf<Int, Int>()
        if (payload.bankAccounts.isNotEmpty()) {
            payload.bankAccounts.forEach { impAcc ->
                val normalizedImp = normalizeName(impAcc.name)
                val exactMatch = localBankAccounts.firstOrNull {
                    normalizeName(it.name) == normalizedImp ||
                            (it.accountNumber.isNotBlank() && it.accountNumber == impAcc.accountNumber)
                }
                if (exactMatch != null) {
                    autoLinkedBankIds[impAcc.id] = exactMatch.id
                } else if (targetBankAccountId != null && payload.bankAccounts.size == 1) {
                    autoLinkedBankIds[impAcc.id] = targetBankAccountId
                } else {
                    unmappedParents.add(
                        UnmappedParent(
                            entityKey = "bank_${impAcc.id}",
                            entityType = ParentEntityType.BANK_ACCOUNT,
                            originalId = impAcc.id,
                            importedName = impAcc.name,
                            importedSubtitle = listOfNotNull(impAcc.branchName.ifBlank { null }, impAcc.accountNumber.ifBlank { null }).joinToString(" • "),
                            senderName = senderName,
                            rawBankAccount = impAcc
                        )
                    )
                }
            }
        } else if (targetBankAccountId != null) {
            autoLinkedBankIds[1] = targetBankAccountId
        } else if (localBankAccounts.isNotEmpty()) {
            autoLinkedBankIds[1] = localBankAccounts.first().id
        }

        // 3. OWNER ACCOUNT MAPPING
        val autoLinkedOwnerIds = mutableMapOf<Int, Int>()
        payload.ownerAccounts.forEach { impOwner ->
            val normalizedImp = normalizeName(impOwner.ownerName)
            val exactMatch = localOwnerAccounts.firstOrNull {
                normalizeName(it.ownerName) == normalizedImp ||
                        (it.accountNumber.isNotBlank() && it.accountNumber == impOwner.accountNumber)
            }
            if (exactMatch != null) {
                autoLinkedOwnerIds[impOwner.id] = exactMatch.id
            } else {
                unmappedParents.add(
                    UnmappedParent(
                        entityKey = "owner_${impOwner.id}",
                        entityType = ParentEntityType.OWNER_ACCOUNT,
                        originalId = impOwner.id,
                        importedName = impOwner.ownerName,
                        importedSubtitle = listOfNotNull(impOwner.branchName.ifBlank { null }, impOwner.accountNumber.ifBlank { null }).joinToString(" • "),
                        senderName = senderName,
                        rawOwnerAccount = impOwner
                    )
                )
            }
        }

        // 4. MENU ANALYSIS FOR AUTO-MAPPED HOTELS
        val pendingNewMenuItems = mutableListOf<PendingNewMenuItem>()
        val menuConflicts = mutableListOf<ConflictItem>()
        val directNewMenus = mutableListOf<MenuEntity>()

        payload.menus.forEach { impMenu ->
            val mappedHotelId = autoLinkedHotelIds[impMenu.hotelId]
            if (mappedHotelId != null) {
                val hotelObj = localHotels.firstOrNull { it.id == mappedHotelId }
                val hotelName = hotelObj?.name ?: "Hotel #$mappedHotelId"
                val hotelMenus = localMenus.filter { it.hotelId == mappedHotelId }

                val impBangla = normalizeName(impMenu.banglaName)
                val impEnglish = normalizeName(impMenu.englishName)

                val localExisting = hotelMenus.firstOrNull { localM ->
                    (impEnglish.isNotBlank() && normalizeName(localM.englishName) == impEnglish) ||
                            (impBangla.isNotBlank() && normalizeName(localM.banglaName) == impBangla)
                }

                if (localExisting == null) {
                    // New item for existing hotel -> Queue for user confirmation
                    pendingNewMenuItems.add(
                        PendingNewMenuItem(
                            itemKey = "menu_new_${mappedHotelId}_${impMenu.englishName}_${impMenu.banglaName}",
                            targetHotelId = mappedHotelId,
                            targetHotelName = hotelName,
                            menuItem = impMenu.copy(hotelId = mappedHotelId)
                        )
                    )
                } else if (abs(localExisting.price - impMenu.price) > 0.01) {
                    // Price changed -> conflict
                    val isNewer = impMenu.lastUpdatedMillis >= localExisting.lastUpdatedMillis
                    menuConflicts.add(
                        ConflictItem.MenuItemConflict(
                            conflictKey = "menu_price_${localExisting.id}_${impMenu.englishName}",
                            title = "${impMenu.englishName.ifBlank { impMenu.banglaName }} (${hotelName})",
                            subtitle = "Price mismatch: Local ₹${localExisting.price} vs Imported ₹${impMenu.price}",
                            timestamp = impMenu.lastUpdatedMillis,
                            isImportedNewer = isNewer,
                            localMenuItem = localExisting,
                            importedMenuItem = impMenu.copy(id = localExisting.id, hotelId = mappedHotelId),
                            hotelName = hotelName
                        )
                    )
                }
            }
        }

        // 5. CHILD DATA DEDUPLICATION (Hotel Transactions / Meals)
        val mealConflicts = mutableListOf<ConflictItem>()
        val directNewMeals = mutableListOf<HotelTransactionEntity>()

        payload.transactions.forEach { impMeal ->
            val mappedHotelId = autoLinkedHotelIds[impMeal.hotelId]
            if (mappedHotelId != null) {
                val hotelObj = localHotels.firstOrNull { it.id == mappedHotelId }
                val hotelName = hotelObj?.name ?: "Hotel #$mappedHotelId"
                val impDayStart = startOfDay(impMeal.timestamp)

                // Match: Hotel ID + Day + Session (isNight) + TotalAmount
                val localMealMatch = localMeals.firstOrNull { localM ->
                    localM.hotelId == mappedHotelId &&
                            localM.isNight == impMeal.isNight &&
                            startOfDay(localM.timestamp) == impDayStart &&
                            abs(localM.totalAmount - impMeal.totalAmount) < 0.01
                }

                if (localMealMatch != null) {
                    val isNewer = impMeal.lastUpdatedMillis >= localMealMatch.lastUpdatedMillis
                    val timeDiff = abs(impMeal.timestamp - localMealMatch.timestamp)
                    val isExact = timeDiff == 0L
                    val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(impMeal.timestamp))
                    val sessionName = if (impMeal.isNight) "Night" else "Day"

                    mealConflicts.add(
                        ConflictItem.MealConflict(
                            conflictKey = "meal_${localMealMatch.id}_${impMeal.timestamp}",
                            title = "$hotelName • $sessionName Session ($dateFormatted)",
                            subtitle = "Total: ₹${impMeal.totalAmount}",
                            timestamp = impMeal.timestamp,
                            isImportedNewer = isNewer,
                            localMeal = localMealMatch,
                            importedMeal = impMeal.copy(hotelId = mappedHotelId),
                            hotelName = hotelName,
                            localItemsSummary = localMealMatch.itemsSerialized,
                            importedItemsSummary = impMeal.itemsSerialized
                        )
                    )
                } else {
                    directNewMeals.add(impMeal.copy(id = 0, hotelId = mappedHotelId))
                }
            }
        }

        // 6. CHILD DATA DEDUPLICATION (Extra Expenses / Wallet Transactions)
        val expenseConflicts = mutableListOf<ConflictItem>()
        val directNewExpenses = mutableListOf<ExtraExpenseEntity>()

        payload.expenses.forEach { impExp ->
            val mappedBankId = autoLinkedBankIds[impExp.bankAccountId]
            if (mappedBankId != null) {
                val accObj = localBankAccounts.firstOrNull { it.id == mappedBankId }
                val accName = accObj?.name ?: "Account #$mappedBankId"

                // Rule: AccountID + TransactionType (Debit/Credit) + Exact Amount + Window <= 5 mins (300,000 ms)
                val localExpMatch = localExpenses.firstOrNull { localE ->
                    localE.bankAccountId == mappedBankId &&
                            localE.type.equals(impExp.type, ignoreCase = true) &&
                            abs(localE.amount - impExp.amount) < 0.01 &&
                            abs(localE.timestamp - impExp.timestamp) <= TRANSACTION_FUZZY_WINDOW_MS
                }

                if (localExpMatch != null) {
                    val isNewer = impExp.lastUpdatedMillis >= localExpMatch.lastUpdatedMillis
                    val diffMins = abs(impExp.timestamp - localExpMatch.timestamp) / (60 * 1000L)
                    val dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(impExp.timestamp))

                    expenseConflicts.add(
                        ConflictItem.TransactionConflict(
                            conflictKey = "expense_${localExpMatch.id}_${impExp.timestamp}",
                            title = "${impExp.name} (₹${impExp.amount})",
                            subtitle = "$accName • $dateFormatted • Type: ${impExp.type}",
                            timestamp = impExp.timestamp,
                            isImportedNewer = isNewer,
                            localExpense = localExpMatch,
                            importedExpense = impExp.copy(bankAccountId = mappedBankId),
                            timeDiffMinutes = diffMins,
                            accountName = accName
                        )
                    )
                } else {
                    directNewExpenses.add(impExp.copy(id = 0, bankAccountId = mappedBankId))
                }
            }
        }

        // 7. METER READINGS
        val readingConflicts = mutableListOf<ConflictItem>()
        val directNewReadings = mutableListOf<MeterReading>()

        payload.meterReadings.forEach { impReading ->
            val impDayStart = startOfDay(impReading.readingDate)
            val match = localReadings.firstOrNull { localR ->
                startOfDay(localR.readingDate) == impDayStart &&
                        (abs(localR.totalReading - impReading.totalReading) < 0.01 ||
                                abs(localR.unitsConsumed - impReading.unitsConsumed) < 0.01)
            }

            if (match != null) {
                val isNewer = impReading.lastUpdatedMillis >= match.lastUpdatedMillis
                val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(impReading.readingDate))
                readingConflicts.add(
                    ConflictItem.MeterReadingConflict(
                        conflictKey = "reading_${match.id}_${impReading.readingDate}",
                        title = "Reading on $dateFormatted",
                        subtitle = "Local: ${match.totalReading} kWh vs Imported: ${impReading.totalReading} kWh",
                        timestamp = impReading.readingDate,
                        isImportedNewer = isNewer,
                        localReading = match,
                        importedReading = impReading
                    )
                )
            } else {
                directNewReadings.add(impReading.copy(id = UUID.randomUUID().toString()))
            }
        }

        // 8. UTILITY BILLS
        val billConflicts = mutableListOf<ConflictItem>()
        val directNewBills = mutableListOf<UtilityBill>()

        payload.utilityBills.forEach { impBill ->
            val impStartDay = startOfDay(impBill.startDate)
            val impEndDay = startOfDay(impBill.endDate)

            val match = localBills.firstOrNull { localB ->
                startOfDay(localB.startDate) == impStartDay &&
                        startOfDay(localB.endDate) == impEndDay &&
                        abs(localB.calculatedAmount - impBill.calculatedAmount) < 0.01
            }

            if (match != null) {
                val isNewer = impBill.lastUpdatedMillis >= match.lastUpdatedMillis
                val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())
                val rangeStr = "${dateFmt.format(Date(impBill.startDate))} - ${dateFmt.format(Date(impBill.endDate))}"
                billConflicts.add(
                    ConflictItem.UtilityBillConflict(
                        conflictKey = "bill_${match.id}_${impBill.startDate}",
                        title = "Electricity Bill ($rangeStr)",
                        subtitle = "Amount: ₹${impBill.calculatedAmount} (${impBill.totalUnits} units)",
                        timestamp = impBill.endDate,
                        isImportedNewer = isNewer,
                        localBill = match,
                        importedBill = impBill
                    )
                )
            } else {
                directNewBills.add(impBill.copy(id = UUID.randomUUID().toString()))
            }
        }

        val directNewHotelPayments = payload.hotelPayments.map { pay ->
            val mappedHotelId = autoLinkedHotelIds[pay.hotelId] ?: pay.hotelId
            pay.copy(id = 0, hotelId = mappedHotelId)
        }
        val directNewStickyNotes = payload.stickyNotes.map { note ->
            note.copy(id = 0, attachmentsJson = "[]")
        }

        val allConflicts = (menuConflicts + mealConflicts + expenseConflicts + readingConflicts + billConflicts)
            .sortedByDescending { it.timestamp }

        SyncAnalysisResult(
            payload = payload,
            senderName = senderName,
            autoLinkedHotelIds = autoLinkedHotelIds,
            autoLinkedBankIds = autoLinkedBankIds,
            autoLinkedOwnerIds = autoLinkedOwnerIds,
            unmappedParents = unmappedParents,
            pendingNewMenuItems = pendingNewMenuItems,
            conflicts = allConflicts,
            directNewHotels = emptyList(),
            directNewBankAccounts = emptyList(),
            directNewMenus = directNewMenus,
            directNewMeals = directNewMeals,
            directNewExpenses = directNewExpenses,
            directNewReadings = directNewReadings,
            directNewBills = directNewBills,
            directNewHotelPayments = directNewHotelPayments,
            directNewStickyNotes = directNewStickyNotes
        )
    }

    /**
     * Resolves unmapped parent decisions (from Phase 3 UI) and processes their child entities.
     * Rule: If a parent is resolved as "CreateNew", all its child menus/transactions/expenses bypass duplicate check!
     */
    suspend fun resolveParentDecisions(
        previousAnalysis: SyncAnalysisResult,
        parentDecisions: Map<String, ParentResolution> // key e.g. "hotel_1" -> MapToExisting or CreateNew
    ): SyncAnalysisResult = withContext(Dispatchers.IO) {
        val payload = previousAnalysis.payload
        val senderName = previousAnalysis.senderName

        val localHotels = hotelDao.getAllHotelsSync().filter { !it.isDeleted }
        val localBankAccounts = bankAccountDao.getAllBankAccountsSync().filter { !it.isDeleted }
        val localOwnerAccounts = ownerAccountDao.getAllOwnerAccountsSync().filter { !it.isDeleted }
        val localMenus = menuDao.getAllMenuItemsSync().filter { !it.isDeleted }
        val localMeals = transactionDao.getAllTransactionsSync().filter { !it.isDeleted }
        val localExpenses = extraExpenseDao.getAllExpensesSync().filter { !it.isDeleted }

        val finalHotelMap = previousAnalysis.autoLinkedHotelIds.toMutableMap()
        val finalBankMap = previousAnalysis.autoLinkedBankIds.toMutableMap()
        val finalOwnerMap = previousAnalysis.autoLinkedOwnerIds.toMutableMap()

        val directNewHotels = mutableListOf<HotelEntity>()
        val directNewBankAccounts = mutableListOf<BankAccountEntity>()
        val directNewOwnerAccounts = mutableListOf<OwnerAccountEntity>()

        val newParentHotelIds = mutableSetOf<Int>()
        val newParentBankIds = mutableSetOf<Int>()

        // Apply decisions to hotels
        payload.hotels.forEach { impHotel ->
            val key = "hotel_${impHotel.id}"
            when (val decision = parentDecisions[key]) {
                is ParentResolution.MapToExisting -> {
                    finalHotelMap[impHotel.id] = decision.targetLocalId
                }
                is ParentResolution.CreateNew, null -> {
                    if (decision is ParentResolution.CreateNew) {
                        directNewHotels.add(impHotel.copy(id = 0, importedFrom = decision.senderName ?: senderName))
                        newParentHotelIds.add(impHotel.id)
                    }
                }
            }
        }

        // Apply decisions to bank accounts
        payload.bankAccounts.forEach { impAcc ->
            val key = "bank_${impAcc.id}"
            when (val decision = parentDecisions[key]) {
                is ParentResolution.MapToExisting -> {
                    finalBankMap[impAcc.id] = decision.targetLocalId
                }
                is ParentResolution.CreateNew, null -> {
                    if (decision is ParentResolution.CreateNew) {
                        directNewBankAccounts.add(impAcc.copy(id = 0, importedFrom = decision.senderName ?: senderName))
                        newParentBankIds.add(impAcc.id)
                    }
                }
            }
        }

        // Apply decisions to owner accounts
        payload.ownerAccounts.forEach { impOwner ->
            val key = "owner_${impOwner.id}"
            when (val decision = parentDecisions[key]) {
                is ParentResolution.MapToExisting -> {
                    finalOwnerMap[impOwner.id] = decision.targetLocalId
                }
                is ParentResolution.CreateNew, null -> {
                    if (decision is ParentResolution.CreateNew) {
                        directNewOwnerAccounts.add(impOwner.copy(id = 0, importedFrom = decision.senderName ?: senderName))
                    }
                }
            }
        }

        val pendingNewMenuItems = previousAnalysis.pendingNewMenuItems.toMutableList()
        val allConflicts = previousAnalysis.conflicts.toMutableList()
        val directNewMenus = previousAnalysis.directNewMenus.toMutableList()
        val directNewMeals = previousAnalysis.directNewMeals.toMutableList()
        val directNewExpenses = previousAnalysis.directNewExpenses.toMutableList()

        // Process Menus for newly decided hotels
        payload.menus.forEach { impMenu ->
            if (newParentHotelIds.contains(impMenu.hotelId)) {
                // BYPASS RULE: If parent is newly created, its children are 100% new!
                directNewMenus.add(impMenu.copy(id = 0))
            } else if (finalHotelMap.containsKey(impMenu.hotelId) && !previousAnalysis.autoLinkedHotelIds.containsKey(impMenu.hotelId)) {
                val mappedHotelId = finalHotelMap[impMenu.hotelId]!!
                val hotelObj = localHotels.firstOrNull { it.id == mappedHotelId }
                val hotelName = hotelObj?.name ?: "Hotel #$mappedHotelId"
                val hotelMenus = localMenus.filter { it.hotelId == mappedHotelId }

                val impBangla = normalizeName(impMenu.banglaName)
                val impEnglish = normalizeName(impMenu.englishName)

                val localExisting = hotelMenus.firstOrNull { localM ->
                    (impEnglish.isNotBlank() && normalizeName(localM.englishName) == impEnglish) ||
                            (impBangla.isNotBlank() && normalizeName(localM.banglaName) == impBangla)
                }

                if (localExisting == null) {
                    pendingNewMenuItems.add(
                        PendingNewMenuItem(
                            itemKey = "menu_new_${mappedHotelId}_${impMenu.englishName}_${impMenu.banglaName}",
                            targetHotelId = mappedHotelId,
                            targetHotelName = hotelName,
                            menuItem = impMenu.copy(hotelId = mappedHotelId)
                        )
                    )
                }
            }
        }

        // Process Meals for newly decided hotels
        payload.transactions.forEach { impMeal ->
            if (newParentHotelIds.contains(impMeal.hotelId)) {
                // BYPASS RULE: If parent is newly created, its meals are directly new!
                directNewMeals.add(impMeal.copy(id = 0))
            } else if (finalHotelMap.containsKey(impMeal.hotelId) && !previousAnalysis.autoLinkedHotelIds.containsKey(impMeal.hotelId)) {
                val mappedHotelId = finalHotelMap[impMeal.hotelId]!!
                val hotelObj = localHotels.firstOrNull { it.id == mappedHotelId }
                val hotelName = hotelObj?.name ?: "Hotel #$mappedHotelId"
                val impDayStart = startOfDay(impMeal.timestamp)

                val localMealMatch = localMeals.firstOrNull { localM ->
                    localM.hotelId == mappedHotelId &&
                            localM.isNight == impMeal.isNight &&
                            startOfDay(localM.timestamp) == impDayStart &&
                            abs(localM.totalAmount - impMeal.totalAmount) < 0.01
                }

                if (localMealMatch != null) {
                    val isNewer = impMeal.lastUpdatedMillis >= localMealMatch.lastUpdatedMillis
                    val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(impMeal.timestamp))
                    val sessionName = if (impMeal.isNight) "Night" else "Day"

                    allConflicts.add(
                        ConflictItem.MealConflict(
                            conflictKey = "meal_${localMealMatch.id}_${impMeal.timestamp}",
                            title = "$hotelName • $sessionName Session ($dateFormatted)",
                            subtitle = "Total: ₹${impMeal.totalAmount}",
                            timestamp = impMeal.timestamp,
                            isImportedNewer = isNewer,
                            localMeal = localMealMatch,
                            importedMeal = impMeal.copy(hotelId = mappedHotelId),
                            hotelName = hotelName,
                            localItemsSummary = localMealMatch.itemsSerialized,
                            importedItemsSummary = impMeal.itemsSerialized
                        )
                    )
                } else {
                    directNewMeals.add(impMeal.copy(id = 0, hotelId = mappedHotelId))
                }
            }
        }

        // Process Expenses for newly decided accounts
        payload.expenses.forEach { impExp ->
            if (newParentBankIds.contains(impExp.bankAccountId)) {
                // BYPASS RULE: If parent account is newly created, its expenses are 100% new!
                directNewExpenses.add(impExp.copy(id = 0))
            } else if (finalBankMap.containsKey(impExp.bankAccountId) && !previousAnalysis.autoLinkedBankIds.containsKey(impExp.bankAccountId)) {
                val mappedBankId = finalBankMap[impExp.bankAccountId]!!
                val accObj = localBankAccounts.firstOrNull { it.id == mappedBankId }
                val accName = accObj?.name ?: "Account #$mappedBankId"

                val localExpMatch = localExpenses.firstOrNull { localE ->
                    localE.bankAccountId == mappedBankId &&
                            localE.type.equals(impExp.type, ignoreCase = true) &&
                            abs(localE.amount - impExp.amount) < 0.01 &&
                            abs(localE.timestamp - impExp.timestamp) <= TRANSACTION_FUZZY_WINDOW_MS
                }

                if (localExpMatch != null) {
                    val isNewer = impExp.lastUpdatedMillis >= localExpMatch.lastUpdatedMillis
                    val diffMins = abs(impExp.timestamp - localExpMatch.timestamp) / (60 * 1000L)
                    val dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(impExp.timestamp))

                    allConflicts.add(
                        ConflictItem.TransactionConflict(
                            conflictKey = "expense_${localExpMatch.id}_${impExp.timestamp}",
                            title = "${impExp.name} (₹${impExp.amount})",
                            subtitle = "$accName • $dateFormatted • Type: ${impExp.type}",
                            timestamp = impExp.timestamp,
                            isImportedNewer = isNewer,
                            localExpense = localExpMatch,
                            importedExpense = impExp.copy(bankAccountId = mappedBankId),
                            timeDiffMinutes = diffMins,
                            accountName = accName
                        )
                    )
                } else {
                    directNewExpenses.add(impExp.copy(id = 0, bankAccountId = mappedBankId))
                }
            }
        }

        val existingPayments = hotelPaymentDao.getAllPaymentsSync().filter { payment -> !payment.isDeleted }
        val updatedHotelPayments = previousAnalysis.directNewHotelPayments.toMutableList()
        payload.hotelPayments.forEach { impPay ->
            if (!previousAnalysis.autoLinkedHotelIds.containsKey(impPay.hotelId) && finalHotelMap.containsKey(impPay.hotelId)) {
                val mappedHotelId = finalHotelMap[impPay.hotelId]!!
                val alreadyExists = existingPayments.any { existing ->
                    existing.hotelId == mappedHotelId && existing.year == impPay.year && existing.month == impPay.month
                }
                if (!alreadyExists) {
                    updatedHotelPayments.add(impPay.copy(id = 0, hotelId = mappedHotelId))
                }
            }
        }

        previousAnalysis.copy(
            autoLinkedHotelIds = finalHotelMap,
            autoLinkedBankIds = finalBankMap,
            autoLinkedOwnerIds = finalOwnerMap,
            unmappedParents = emptyList(),
            pendingNewMenuItems = pendingNewMenuItems,
            conflicts = allConflicts.sortedByDescending { it.timestamp },
            directNewHotels = directNewHotels,
            directNewBankAccounts = directNewBankAccounts,
            directNewMenus = directNewMenus,
            directNewMeals = directNewMeals,
            directNewExpenses = directNewExpenses,
            directNewHotelPayments = updatedHotelPayments
        )
    }

    /**
     * Pre-calculates resolution map for "Take All Latest".
     */
    fun autoResolveAllLatest(conflicts: List<ConflictItem>): Map<String, ConflictResolution> {
        return conflicts.associate { conflict ->
            val choice = if (conflict.isImportedNewer) ConflictResolution.TAKE_IMPORTED else ConflictResolution.KEEP_LOCAL
            conflict.conflictKey to choice
        }
    }

    /**
     * Pre-calculates resolution map for "Keep All Local".
     */
    fun autoResolveAllLocal(conflicts: List<ConflictItem>): Map<String, ConflictResolution> {
        return conflicts.associate { conflict ->
            conflict.conflictKey to ConflictResolution.KEEP_LOCAL
        }
    }

    /**
     * Builds the final concrete MergePlan ready for Room transaction execution.
     */
    fun buildFinalMergePlan(
        analysis: SyncAnalysisResult,
        acceptedMenuItemKeys: Set<String>,
        conflictResolutions: Map<String, ConflictResolution>
    ): MergePlan {
        val hotelsToInsert = analysis.directNewHotels.toMutableList()
        val bankAccountsToInsert = analysis.directNewBankAccounts.toMutableList()
        val ownerAccountsToInsert = mutableListOf<OwnerAccountEntity>()

        val menusToInsert = analysis.directNewMenus.toMutableList()
        val menusToUpdate = mutableListOf<MenuEntity>()

        val mealsToInsert = analysis.directNewMeals.toMutableList()
        val mealsToUpdate = mutableListOf<HotelTransactionEntity>()

        val expensesToInsert = analysis.directNewExpenses.toMutableList()
        val expensesToUpdate = mutableListOf<ExtraExpenseEntity>()

        val readingsToInsert = analysis.directNewReadings.toMutableList()
        val readingsToUpdate = mutableListOf<MeterReading>()

        val billsToInsert = analysis.directNewBills.toMutableList()
        val billsToUpdate = mutableListOf<UtilityBill>()

        // 1. Add accepted new menu items
        analysis.pendingNewMenuItems.forEach { pending ->
            if (acceptedMenuItemKeys.contains(pending.itemKey)) {
                menusToInsert.add(pending.menuItem.copy(id = 0))
            }
        }

        // 2. Resolve conflicts based on user decisions
        analysis.conflicts.forEach { conflict ->
            val decision = conflictResolutions[conflict.conflictKey] ?: if (conflict.isImportedNewer) ConflictResolution.TAKE_IMPORTED else ConflictResolution.KEEP_LOCAL

            when (conflict) {
                is ConflictItem.TransactionConflict -> {
                    when (decision) {
                        ConflictResolution.KEEP_LOCAL -> { /* Do nothing, local is kept */ }
                        ConflictResolution.TAKE_IMPORTED -> {
                            expensesToUpdate.add(conflict.importedExpense.copy(id = conflict.localExpense.id))
                        }
                        ConflictResolution.KEEP_BOTH -> {
                            expensesToInsert.add(conflict.importedExpense.copy(id = 0))
                        }
                    }
                }
                is ConflictItem.MealConflict -> {
                    when (decision) {
                        ConflictResolution.KEEP_LOCAL -> { /* Local kept */ }
                        ConflictResolution.TAKE_IMPORTED -> {
                            mealsToUpdate.add(conflict.importedMeal.copy(id = conflict.localMeal.id))
                        }
                        ConflictResolution.KEEP_BOTH -> {
                            mealsToInsert.add(conflict.importedMeal.copy(id = 0))
                        }
                    }
                }
                is ConflictItem.MenuItemConflict -> {
                    when (decision) {
                        ConflictResolution.KEEP_LOCAL -> { /* Local kept */ }
                        ConflictResolution.TAKE_IMPORTED -> {
                            menusToUpdate.add(conflict.importedMenuItem.copy(id = conflict.localMenuItem.id))
                        }
                        ConflictResolution.KEEP_BOTH -> {
                            menusToInsert.add(conflict.importedMenuItem.copy(id = 0))
                        }
                    }
                }
                is ConflictItem.MeterReadingConflict -> {
                    when (decision) {
                        ConflictResolution.KEEP_LOCAL -> { /* Local kept */ }
                        ConflictResolution.TAKE_IMPORTED -> {
                            readingsToUpdate.add(conflict.importedReading.copy(id = conflict.localReading.id))
                        }
                        ConflictResolution.KEEP_BOTH -> {
                            readingsToInsert.add(conflict.importedReading.copy(id = UUID.randomUUID().toString()))
                        }
                    }
                }
                is ConflictItem.UtilityBillConflict -> {
                    when (decision) {
                        ConflictResolution.KEEP_LOCAL -> { /* Local kept */ }
                        ConflictResolution.TAKE_IMPORTED -> {
                            billsToUpdate.add(conflict.importedBill.copy(id = conflict.localBill.id))
                        }
                        ConflictResolution.KEEP_BOTH -> {
                            billsToInsert.add(conflict.importedBill.copy(id = UUID.randomUUID().toString()))
                        }
                    }
                }
            }
        }

        val hotelPaymentsToInsert = analysis.directNewHotelPayments
        val stickyNotesToInsert = analysis.directNewStickyNotes

        val totalCount = hotelsToInsert.size + bankAccountsToInsert.size + ownerAccountsToInsert.size +
                menusToInsert.size + menusToUpdate.size +
                mealsToInsert.size + mealsToUpdate.size +
                expensesToInsert.size + expensesToUpdate.size +
                readingsToInsert.size + readingsToUpdate.size +
                billsToInsert.size + billsToUpdate.size +
                hotelPaymentsToInsert.size + stickyNotesToInsert.size

        return MergePlan(
            senderName = analysis.senderName,
            hotelsToInsert = hotelsToInsert,
            bankAccountsToInsert = bankAccountsToInsert,
            ownerAccountsToInsert = ownerAccountsToInsert,
            menusToInsert = menusToInsert,
            menusToUpdate = menusToUpdate,
            mealsToInsert = mealsToInsert,
            mealsToUpdate = mealsToUpdate,
            expensesToInsert = expensesToInsert,
            expensesToUpdate = expensesToUpdate,
            readingsToInsert = readingsToInsert,
            readingsToUpdate = readingsToUpdate,
            billsToInsert = billsToInsert,
            billsToUpdate = billsToUpdate,
            hotelPaymentsToInsert = hotelPaymentsToInsert,
            stickyNotesToInsert = stickyNotesToInsert,
            userProfileToUpdate = analysis.payload.userProfile,
            totalAffectedCount = totalCount
        )
    }

    suspend fun executeMergePlan(plan: MergePlan) = withContext(Dispatchers.IO) {
        db.syncDao().executeMergeTransaction(plan)
    }
}
