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

    data class HotelConflict(
        override val conflictKey: String,
        override val title: String,
        override val subtitle: String,
        override val timestamp: Long,
        override val isImportedNewer: Boolean,
        val localHotel: HotelEntity,
        val importedHotel: HotelEntity
    ) : ConflictItem()

    data class BankAccountConflict(
        override val conflictKey: String,
        override val title: String,
        override val subtitle: String,
        override val timestamp: Long,
        override val isImportedNewer: Boolean,
        val localBankAccount: BankAccountEntity,
        val importedBankAccount: BankAccountEntity
    ) : ConflictItem()

    data class OwnerAccountConflict(
        override val conflictKey: String,
        override val title: String,
        override val subtitle: String,
        override val timestamp: Long,
        override val isImportedNewer: Boolean,
        val localOwnerAccount: OwnerAccountEntity,
        val importedOwnerAccount: OwnerAccountEntity
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
    val electricityRates: List<ElectricityRate> = emptyList(),
    val userProfile: UserProfileEntity? = null,
    val dataType: String = "",
    val stubHotelIds: Set<Int> = emptySet(),
    val stubBankAccountIds: Set<Int> = emptySet()
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
    val directNewOwnerAccounts: List<OwnerAccountEntity> = emptyList(),
    val directNewMenus: List<MenuEntity> = emptyList(),
    val directNewMeals: List<HotelTransactionEntity> = emptyList(),
    val directNewExpenses: List<ExtraExpenseEntity> = emptyList(),
    val directNewReadings: List<MeterReading> = emptyList(),
    val directNewBills: List<UtilityBill> = emptyList(),
    val directNewHotelPayments: List<HotelPaymentEntity> = emptyList(),
    val directNewStickyNotes: List<StickyNoteEntity> = emptyList(),
    val directNewElectricityRates: List<ElectricityRate> = emptyList(),
    val duplicateRecordsInFileCount: Int = 0,
    val duplicateRecordsInFileSummary: List<String> = emptyList(),
    val localDuplicateMealIdsToDelete: List<Int> = emptyList(),
    val localDuplicateExpenseIdsToDelete: List<Int> = emptyList(),
    val localDuplicateMenuIdsToDelete: List<Int> = emptyList(),
    val orphanHotelIdsToAdopt: Set<Int> = emptySet(),
    val orphanBankIdsToAdopt: Set<Int> = emptySet(),
    val skippedIdenticalCount: Int = 0,
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
    val ownerAccountsToUpdate: List<OwnerAccountEntity> = emptyList(),
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
    val electricityRatesToInsert: List<ElectricityRate> = emptyList(),
    val userProfileToUpdate: UserProfileEntity? = null,
    val duplicateRecordsInFileCount: Int = 0,
    val duplicateRecordsInFileSummary: List<String> = emptyList(),
    val localDuplicateMealIdsToDelete: List<Int> = emptyList(),
    val localDuplicateExpenseIdsToDelete: List<Int> = emptyList(),
    val localDuplicateMenuIdsToDelete: List<Int> = emptyList(),
    val orphanHotelIdsToAdopt: Set<Int> = emptySet(),
    val orphanBankIdsToAdopt: Set<Int> = emptySet(),
    val skippedIdenticalCount: Int = 0,
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
    private val stickyNoteDao = db.stickyNoteDao()
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
        val electricityRates = mutableListOf<ElectricityRate>()
        var userProfile: UserProfileEntity? = null
        val stubHotelIds = mutableSetOf<Int>()
        val stubBankAccountIds = mutableSetOf<Int>()

        // 1. User Profile
        if (root.has("userProfile")) {
            val pObj = root.getJSONObject("userProfile")
            val pName = pObj.optString("name", "")
            if (!pName.isBlank()) senderName = pName
            userProfile = UserProfileEntity(
                name = pName,
                contact = pObj.optString("contact", ""),
                avatar = pObj.optString("avatar", ""),
                currencySymbol = pObj.optString("currencySymbol", "₹"),
                language = pObj.optString("language", "en"),
                themeMode = pObj.optString("themeMode", "Auto"),
                enableSupplementaryAddons = pObj.optBoolean("enableSupplementaryAddons", true),
                bgPreset = pObj.optString("bgPreset", "Default"),
                enableLiquidGlassTheme = pObj.optBoolean("enableLiquidGlassTheme", false),
                disableHeavyEffects = pObj.optBoolean("disableHeavyEffects", true),
                mealGroupPosition = pObj.optString("mealGroupPosition", "BottomBar"),
                textSize = pObj.optString("textSize", "Medium"),
                isBoldTextEnabled = pObj.optBoolean("isBoldTextEnabled", false),
                stickyCardWidthPercent = pObj.optInt("stickyCardWidthPercent", 80),
                stickyPreviewLines = pObj.optInt("stickyPreviewLines", 10)
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
            val hId = hObj.optInt("id", 1)
            if (hObj.optBoolean("isStub", false)) stubHotelIds.add(hId)
            val hotel = HotelEntity(
                id = hId,
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
                val hId = hObj.optInt("id", i + 1)
                if (hObj.optBoolean("isStub", false)) stubHotelIds.add(hId)
                hotels.add(
                    HotelEntity(
                        id = hId,
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
            val bId = bObj.optInt("id", 1)
            if (bObj.optBoolean("isStub", false)) stubBankAccountIds.add(bId)
            val bal = bObj.optDouble("balance", 0.0)
            bankAccounts.add(
                BankAccountEntity(
                    id = bId,
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
                val bId = bObj.optInt("id", i + 1)
                if (bObj.optBoolean("isStub", false)) stubBankAccountIds.add(bId)
                val bal = bObj.optDouble("balance", 0.0)
                bankAccounts.add(
                    BankAccountEntity(
                        id = bId,
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
                        attachmentsJson = sObj.optString("attachmentsJson", "[]").ifBlank { "[]" },
                        tags = sObj.optString("tags", ""),
                        createdAt = sObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = sObj.optLong("updatedAt", sObj.optLong("lastUpdatedMillis", System.currentTimeMillis()))
                    )
                )
            }
        }

        // 15. Electricity Rates ("electricityRates")
        if (root.has("electricityRates")) {
            val rateArr = root.getJSONArray("electricityRates")
            for (i in 0 until rateArr.length()) {
                val rObj = rateArr.getJSONObject(i)
                val rVal = rObj.optDouble("ratePerUnit", 0.0)
                if (rVal > 0.0) {
                    electricityRates.add(
                        ElectricityRate(
                            id = UUID.randomUUID().toString(),
                            effectiveDate = rObj.optLong("effectiveDate", System.currentTimeMillis()),
                            ratePerUnit = rVal
                        )
                    )
                }
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
            electricityRates = electricityRates,
            userProfile = userProfile,
            dataType = root.optString("dataType", "IMPORT"),
            stubHotelIds = stubHotelIds,
            stubBankAccountIds = stubBankAccountIds
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

    private data class DeduplicatedPayloadResult(
        val payload: ParsedImportPayload,
        val duplicateCount: Int,
        val duplicateSummary: List<String>
    )

    /**
     * Detects and collapses duplicate entries inside the imported backup/ZIP/JSON payload itself,
     * while recording a detailed summary so the user is informed and duplicates are intelligently managed.
     */
    private fun deduplicateImportedPayload(raw: ParsedImportPayload): DeduplicatedPayloadResult {
        val summary = mutableListOf<String>()
        var totalDups = 0

        // 1. Hotels inside payload
        val hotelIdRemap = mutableMapOf<Int, Int>()
        val uniqueHotels = mutableListOf<HotelEntity>()
        raw.hotels.forEach { h ->
            val normName = normalizeName(h.name)
            val phone = h.phoneNumber.trim()
            val upi = h.upiId.trim().lowercase(Locale.ROOT)
            val existing = uniqueHotels.firstOrNull { u ->
                normalizeName(u.name) == normName ||
                    (phone.isNotBlank() && u.phoneNumber.trim() == phone) ||
                    (upi.isNotBlank() && u.upiId.trim().lowercase(Locale.ROOT) == upi)
            }
            if (existing != null) {
                hotelIdRemap[h.id] = existing.id
                totalDups++
            } else {
                val assignedId = if (h.id <= 0 || uniqueHotels.any { it.id == h.id }) (uniqueHotels.maxOfOrNull { it.id } ?: 0) + 1 else h.id
                hotelIdRemap[h.id] = assignedId
                uniqueHotels.add(h.copy(id = assignedId))
            }
        }
        val dupHotels = raw.hotels.size - uniqueHotels.size
        if (dupHotels > 0) summary.add("$dupHotels duplicate Hotel profile(s) in backup merged")

        // 2. Bank Accounts inside payload
        val bankIdRemap = mutableMapOf<Int, Int>()
        val uniqueBanks = mutableListOf<BankAccountEntity>()
        raw.bankAccounts.forEach { b ->
            val normName = normalizeName(b.name)
            val accNo = b.accountNumber.trim()
            val upi = b.upiId.trim().lowercase(Locale.ROOT)
            val existing = uniqueBanks.firstOrNull { u ->
                normalizeName(u.name) == normName ||
                    (accNo.isNotBlank() && u.accountNumber.trim() == accNo) ||
                    (upi.isNotBlank() && u.upiId.trim().lowercase(Locale.ROOT) == upi)
            }
            if (existing != null) {
                bankIdRemap[b.id] = existing.id
                totalDups++
            } else {
                val assignedId = if (b.id <= 0 || uniqueBanks.any { it.id == b.id }) (uniqueBanks.maxOfOrNull { it.id } ?: 0) + 1 else b.id
                bankIdRemap[b.id] = assignedId
                uniqueBanks.add(b.copy(id = assignedId))
            }
        }
        val dupBanks = raw.bankAccounts.size - uniqueBanks.size
        if (dupBanks > 0) summary.add("$dupBanks duplicate Bank Account(s) in backup merged")

        // 3. Menus inside payload
        val seenMenuKeys = mutableSetOf<String>()
        val uniqueMenus = mutableListOf<MenuEntity>()
        val defaultHotelId = uniqueHotels.firstOrNull()?.id ?: 1
        raw.menus.forEach { m ->
            val hId = hotelIdRemap[m.hotelId] ?: if (uniqueHotels.size == 1) defaultHotelId else m.hotelId
            val key = "${hId}|${normalizeName(m.englishName)}|${normalizeName(m.banglaName)}"
            if (seenMenuKeys.add(key)) {
                uniqueMenus.add(m.copy(hotelId = hId))
            } else {
                totalDups++
            }
        }
        val dupMenus = raw.menus.size - uniqueMenus.size
        if (dupMenus > 0) summary.add("$dupMenus duplicate Menu item(s) in backup removed")

        // 4. Hotel Transactions (Meal Orders) inside payload
        val seenMealKeys = mutableSetOf<String>()
        val uniqueMeals = mutableListOf<HotelTransactionEntity>()
        raw.transactions.forEach { tx ->
            val hId = hotelIdRemap[tx.hotelId] ?: if (uniqueHotels.size == 1) defaultHotelId else tx.hotelId
            val dayStart = startOfDay(tx.timestamp)
            val key = "${hId}|${tx.isNight}|${dayStart}|${"%.2f".format(Locale.US, tx.totalAmount)}|${tx.itemsSerialized.trim()}"
            if (seenMealKeys.add(key)) {
                uniqueMeals.add(tx.copy(hotelId = hId))
            } else {
                totalDups++
            }
        }
        val dupMeals = raw.transactions.size - uniqueMeals.size
        if (dupMeals > 0) summary.add("$dupMeals duplicate Hotel Meal Order(s) in backup removed")

        // 5. Extra Expenses inside payload
        val seenExpKeys = mutableSetOf<String>()
        val uniqueExpenses = mutableListOf<ExtraExpenseEntity>()
        val defaultBankId = uniqueBanks.firstOrNull()?.id ?: 1
        raw.expenses.forEach { exp ->
            val bId = bankIdRemap[exp.bankAccountId] ?: if (uniqueBanks.size == 1) defaultBankId else exp.bankAccountId
            val minuteBucket = exp.timestamp / (60 * 1000L)
            val key = "${bId}|${exp.type.uppercase(Locale.ROOT)}|${"%.2f".format(Locale.US, exp.amount)}|${normalizeName(exp.name)}|${normalizeName(exp.category)}|$minuteBucket"
            if (seenExpKeys.add(key)) {
                uniqueExpenses.add(exp.copy(bankAccountId = bId))
            } else {
                totalDups++
            }
        }
        val dupExps = raw.expenses.size - uniqueExpenses.size
        if (dupExps > 0) summary.add("$dupExps duplicate Extra Transaction(s) in backup removed")

        // 6. Hotel Payments inside payload
        val seenPayKeys = mutableSetOf<String>()
        val uniquePayments = mutableListOf<HotelPaymentEntity>()
        raw.hotelPayments.forEach { pay ->
            val hId = hotelIdRemap[pay.hotelId] ?: if (uniqueHotels.size == 1) defaultHotelId else pay.hotelId
            val key = "${hId}|${pay.year}|${pay.month}|${"%.2f".format(Locale.US, pay.amountPaid)}"
            if (seenPayKeys.add(key)) {
                uniquePayments.add(pay.copy(hotelId = hId))
            } else {
                totalDups++
            }
        }

        // If payload has menus or transactions or hotelPayments but ZERO hotels in payload,
        // synthesize a HotelEntity so child data is never orphaned!
        val finalHotels = if (uniqueHotels.isEmpty() && (uniqueMenus.isNotEmpty() || uniqueMeals.isNotEmpty() || uniquePayments.isNotEmpty())) {
            val inferredHotelId = uniqueMenus.firstOrNull()?.hotelId
                ?: uniqueMeals.firstOrNull()?.hotelId
                ?: uniquePayments.firstOrNull()?.hotelId
                ?: 1
            listOf(
                HotelEntity(
                    id = inferredHotelId,
                    name = raw.senderName?.let { "$it's Hotel" } ?: "Imported Hotel",
                    importedFrom = raw.senderName
                )
            )
        } else {
            uniqueHotels
        }

        // Similarly, if payload has expenses but ZERO bank accounts, synthesize a BankAccountEntity
        val finalBanks = if (uniqueBanks.isEmpty() && uniqueExpenses.isNotEmpty()) {
            val inferredBankId = uniqueExpenses.firstOrNull()?.bankAccountId ?: 1
            listOf(
                BankAccountEntity(
                    id = inferredBankId,
                    name = raw.senderName?.let { "$it's Account" } ?: "Imported Account",
                    importedFrom = raw.senderName
                )
            )
        } else {
            uniqueBanks
        }

        val remappedStubHotels = raw.stubHotelIds.mapNotNull { hotelIdRemap[it] ?: it }.toSet()
        val remappedStubBanks = raw.stubBankAccountIds.mapNotNull { bankIdRemap[it] ?: it }.toSet()

        return DeduplicatedPayloadResult(
            payload = raw.copy(
                hotels = finalHotels,
                bankAccounts = finalBanks,
                menus = uniqueMenus,
                transactions = uniqueMeals,
                expenses = uniqueExpenses,
                hotelPayments = uniquePayments,
                stubHotelIds = remappedStubHotels,
                stubBankAccountIds = remappedStubBanks
            ),
            duplicateCount = totalDups,
            duplicateSummary = summary
        )
    }

    private data class LocalDuplicateScanResult(
        val cleanMeals: List<HotelTransactionEntity>,
        val duplicateMealIds: List<Int>,
        val cleanExpenses: List<ExtraExpenseEntity>,
        val duplicateExpenseIds: List<Int>,
        val cleanMenus: List<MenuEntity>,
        val duplicateMenuIds: List<Int>,
        val orphanHotelIds: Set<Int>,
        val orphanBankIds: Set<Int>,
        val summaryLines: List<String>
    )

    private fun findLocalDuplicatesAndCleanLists(
        localHotels: List<HotelEntity>,
        localBanks: List<BankAccountEntity>,
        localMeals: List<HotelTransactionEntity>,
        localExpenses: List<ExtraExpenseEntity>,
        localMenus: List<MenuEntity>
    ): LocalDuplicateScanResult {
        val validHotelIds = localHotels.map { it.id }.toSet()
        val validBankIds = localBanks.map { it.id }.toSet()

        val orphanHotelIds = mutableSetOf<Int>()
        localMenus.forEach { if (!validHotelIds.contains(it.hotelId)) orphanHotelIds.add(it.hotelId) }
        localMeals.forEach { if (!validHotelIds.contains(it.hotelId)) orphanHotelIds.add(it.hotelId) }

        val orphanBankIds = mutableSetOf<Int>()
        localExpenses.forEach { if (!validBankIds.contains(it.bankAccountId)) orphanBankIds.add(it.bankAccountId) }

        // Deduplicate local meals (exact same hotelId/orphan, session, day, amount, serialized items)
        val seenMealSigs = mutableSetOf<String>()
        val cleanMeals = mutableListOf<HotelTransactionEntity>()
        val dupMealIds = mutableListOf<Int>()
        localMeals.sortedBy { it.id }.forEach { meal ->
            val effectiveHotelKey = if (validHotelIds.contains(meal.hotelId)) meal.hotelId else -1
            val sig = "$effectiveHotelKey|${meal.isNight}|${startOfDay(meal.timestamp)}|${"%.2f".format(Locale.US, meal.totalAmount)}|${meal.itemsSerialized.trim()}"
            if (seenMealSigs.add(sig)) {
                cleanMeals.add(meal)
            } else {
                dupMealIds.add(meal.id)
            }
        }

        // Deduplicate local expenses
        val seenExpSigs = mutableSetOf<String>()
        val cleanExpenses = mutableListOf<ExtraExpenseEntity>()
        val dupExpIds = mutableListOf<Int>()
        localExpenses.sortedBy { it.id }.forEach { exp ->
            val effectiveBankKey = if (validBankIds.contains(exp.bankAccountId)) exp.bankAccountId else -1
            val minuteBucket = exp.timestamp / (60 * 1000L)
            val sig = "$effectiveBankKey|${exp.type.uppercase(Locale.ROOT)}|${"%.2f".format(Locale.US, exp.amount)}|${normalizeName(exp.name)}|${normalizeName(exp.category)}|$minuteBucket"
            if (seenExpSigs.add(sig)) {
                cleanExpenses.add(exp)
            } else {
                dupExpIds.add(exp.id)
            }
        }

        // Deduplicate local menus
        val seenMenuSigs = mutableSetOf<String>()
        val cleanMenus = mutableListOf<MenuEntity>()
        val dupMenuIds = mutableListOf<Int>()
        localMenus.sortedBy { it.id }.forEach { menu ->
            val effectiveHotelKey = if (validHotelIds.contains(menu.hotelId)) menu.hotelId else -1
            val sig = "$effectiveHotelKey|${normalizeName(menu.englishName)}|${normalizeName(menu.banglaName)}"
            if (seenMenuSigs.add(sig)) {
                cleanMenus.add(menu)
            } else {
                dupMenuIds.add(menu.id)
            }
        }

        val summary = mutableListOf<String>()
        if (dupMealIds.isNotEmpty()) summary.add("${dupMealIds.size} existing duplicate Hotel Meal record(s) in app will be cleaned")
        if (dupExpIds.isNotEmpty()) summary.add("${dupExpIds.size} existing duplicate Extra Expense record(s) in app will be cleaned")
        if (dupMenuIds.isNotEmpty()) summary.add("${dupMenuIds.size} existing duplicate Menu item(s) in app will be cleaned")
        if (orphanHotelIds.isNotEmpty()) summary.add("Orphaned local Hotel Menus/Orders will be linked to the imported Hotel")

        return LocalDuplicateScanResult(
            cleanMeals = cleanMeals,
            duplicateMealIds = dupMealIds,
            cleanExpenses = cleanExpenses,
            duplicateExpenseIds = dupExpIds,
            cleanMenus = cleanMenus,
            duplicateMenuIds = dupMenuIds,
            orphanHotelIds = orphanHotelIds,
            orphanBankIds = orphanBankIds,
            summaryLines = summary
        )
    }

    /**
     * Startup self-healing routine:
     * 1. Restores or creates parent Hotel / BankAccount if orphaned child menus/orders/expenses exist in Room.
     * 2. Cleans up any exact duplicate rows in hotel_transactions, menus, and extra_expenses.
     * Returns the active/restored Hotel ID if one was healed, or null otherwise.
     */
    suspend fun autoHealOrphanedAndDuplicateData(): Int? = withContext(Dispatchers.IO) {
        var healedHotelId: Int? = null
        val allHotelsRaw = hotelDao.getAllHotelsSync()
        val activeHotels = allHotelsRaw.filter { !it.isDeleted }.toMutableList()
        val activeBanks = bankAccountDao.getAllBankAccountsSync().filter { !it.isDeleted }.toMutableList()
        val activeMenus = menuDao.getAllMenuItemsSync().filter { !it.isDeleted }
        val activeMeals = transactionDao.getAllTransactionsSync().filter { !it.isDeleted }
        val activePayments = hotelPaymentDao.getAllPaymentsSync().filter { !it.isDeleted }
        val activeExpenses = extraExpenseDao.getAllExpensesSync().filter { !it.isDeleted }

        val validHotelIds = activeHotels.map { it.id }.toMutableSet()
        val orphanHotelIds = (activeMenus.map { it.hotelId } + activeMeals.map { it.hotelId } + activePayments.map { it.hotelId })
            .toSet()
            .filter { !validHotelIds.contains(it) }

        if (orphanHotelIds.isNotEmpty()) {
            val targetHotelId = if (activeHotels.isNotEmpty()) {
                activeHotels.first().id
            } else {
                // Check if there is a soft-deleted hotel we can restore, or insert a new HotelEntity
                val deletedMatch = allHotelsRaw.firstOrNull { it.isDeleted && orphanHotelIds.contains(it.id) }
                if (deletedMatch != null) {
                    hotelDao.updateHotel(deletedMatch.copy(isDeleted = false))
                    validHotelIds.add(deletedMatch.id)
                    activeHotels.add(deletedMatch.copy(isDeleted = false))
                    deletedMatch.id
                } else {
                    val newId = hotelDao.insertHotel(
                        HotelEntity(
                            name = "Imported Hotel",
                            notes = "Restored hotel for existing menus and meal history"
                        )
                    ).toInt()
                    validHotelIds.add(newId)
                    activeHotels.add(HotelEntity(id = newId, name = "Imported Hotel"))
                    newId
                }
            }
            orphanHotelIds.forEach { orphanId ->
                if (orphanId != targetHotelId) {
                    db.syncDao().remapOrphanedMenusHotelId(orphanId, targetHotelId)
                    db.syncDao().remapOrphanedTransactionsHotelId(orphanId, targetHotelId)
                    db.syncDao().remapOrphanedPaymentsHotelId(orphanId, targetHotelId)
                }
            }
            healedHotelId = targetHotelId
        }

        val validBankIds = activeBanks.map { it.id }.toMutableSet()
        val orphanBankIds = activeExpenses.map { it.bankAccountId }.toSet().filter { !validBankIds.contains(it) }
        if (orphanBankIds.isNotEmpty()) {
            val targetBankId = if (activeBanks.isNotEmpty()) {
                activeBanks.first().id
            } else {
                val newBankId = bankAccountDao.insertBankAccount(
                    BankAccountEntity(
                        name = "Main Account",
                        details = "Restored account for existing transactions"
                    )
                ).toInt()
                validBankIds.add(newBankId)
                newBankId
            }
            orphanBankIds.forEach { orphanId ->
                if (orphanId != targetBankId) {
                    db.syncDao().remapOrphanedExpensesBankId(orphanId, targetBankId)
                }
            }
        }

        // Clean any exact duplicate rows already in local database
        val refreshedMeals = transactionDao.getAllTransactionsSync().filter { !it.isDeleted }
        val refreshedMenus = menuDao.getAllMenuItemsSync().filter { !it.isDeleted }
        val refreshedExpenses = extraExpenseDao.getAllExpensesSync().filter { !it.isDeleted }

        val scan = findLocalDuplicatesAndCleanLists(
            localHotels = hotelDao.getAllHotelsSync().filter { !it.isDeleted },
            localBanks = bankAccountDao.getAllBankAccountsSync().filter { !it.isDeleted },
            localMeals = refreshedMeals,
            localExpenses = refreshedExpenses,
            localMenus = refreshedMenus
        )
        if (scan.duplicateMealIds.isNotEmpty()) {
            db.syncDao().deleteTransactionIds(scan.duplicateMealIds)
        }
        if (scan.duplicateExpenseIds.isNotEmpty()) {
            db.syncDao().deleteExpenseIds(scan.duplicateExpenseIds)
        }
        if (scan.duplicateMenuIds.isNotEmpty()) {
            db.syncDao().deleteMenuIds(scan.duplicateMenuIds)
        }

        healedHotelId
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
        val rawPayload = parseJsonPayload(jsonString)
        val dedupResult = deduplicateImportedPayload(rawPayload)
        val payload = dedupResult.payload
        val senderName = explicitSenderName ?: payload.senderName

        val localHotels = hotelDao.getAllHotelsSync().filter { !it.isDeleted }
        val localBankAccounts = bankAccountDao.getAllBankAccountsSync().filter { !it.isDeleted }
        val localOwnerAccounts = ownerAccountDao.getAllOwnerAccountsSync().filter { !it.isDeleted }
        val rawLocalMenus = menuDao.getAllMenuItemsSync().filter { !it.isDeleted }
        val rawLocalMeals = transactionDao.getAllTransactionsSync().filter { !it.isDeleted }
        val rawLocalExpenses = extraExpenseDao.getAllExpensesSync().filter { !it.isDeleted }
        val localReadings = meterDao.getAllReadingsSync().filter { !it.isDeleted }
        val localBills = meterDao.getAllUtilityBillsSync().filter { !it.isDeleted }

        val localScan = findLocalDuplicatesAndCleanLists(
            localHotels = localHotels,
            localBanks = localBankAccounts,
            localMeals = rawLocalMeals,
            localExpenses = rawLocalExpenses,
            localMenus = rawLocalMenus
        )
        val localMenus = localScan.cleanMenus
        val localMeals = localScan.cleanMeals
        val localExpenses = localScan.cleanExpenses

        val autoLinkedHotelIds = mutableMapOf<Int, Int>() // importedId -> localId
        val unmappedParents = mutableListOf<UnmappedParent>()
        val parentConflicts = mutableListOf<ConflictItem>()
        var skippedIdenticalCount = 0

        // 1. HOTEL MAPPING & MISMATCH DETECTION
        if (payload.hotels.isNotEmpty()) {
            payload.hotels.forEach { impHotel ->
                val normalizedImp = normalizeName(impHotel.name)
                val impPhone = impHotel.phoneNumber.trim()
                val impUpi = impHotel.upiId.trim().lowercase(Locale.ROOT)
                val exactMatch = localHotels.firstOrNull { localH ->
                    normalizeName(localH.name) == normalizedImp ||
                        (impPhone.isNotBlank() && localH.phoneNumber.trim() == impPhone) ||
                        (impUpi.isNotBlank() && localH.upiId.trim().lowercase(Locale.ROOT) == impUpi)
                }
                if (exactMatch != null) {
                    autoLinkedHotelIds[impHotel.id] = exactMatch.id
                    if (impHotel.id in payload.stubHotelIds) {
                        // Lightweight parent reference stub — link children silently without raising profile conflict
                        skippedIdenticalCount++
                    } else {
                        val hasMismatch = normalizeName(exactMatch.name) != normalizedImp ||
                            exactMatch.location.trim() != impHotel.location.trim() ||
                            exactMatch.phoneNumber.trim() != impPhone ||
                            exactMatch.whatsappNumber.trim() != impHotel.whatsappNumber.trim() ||
                            exactMatch.upiId.trim().lowercase(Locale.ROOT) != impUpi ||
                            (impHotel.qrCodeUri.isNotBlank() && exactMatch.qrCodeUri != impHotel.qrCodeUri) ||
                            exactMatch.notes.trim() != impHotel.notes.trim() ||
                            abs(exactMatch.advancedPayment - impHotel.advancedPayment) > 0.01
                        if (hasMismatch) {
                            val isNewer = impHotel.lastUpdatedMillis >= exactMatch.lastUpdatedMillis
                            parentConflicts.add(
                                ConflictItem.HotelConflict(
                                    conflictKey = "hotel_${exactMatch.id}_${impHotel.id}",
                                    title = "Hotel: ${exactMatch.name}",
                                    subtitle = "Details or balance differ from local record",
                                    timestamp = impHotel.lastUpdatedMillis,
                                    isImportedNewer = isNewer,
                                    localHotel = exactMatch,
                                    importedHotel = impHotel.copy(
                                        id = exactMatch.id,
                                        qrCodeUri = impHotel.qrCodeUri.ifBlank { exactMatch.qrCodeUri },
                                        importedFrom = impHotel.importedFrom ?: exactMatch.importedFrom
                                    )
                                )
                            )
                        } else {
                            skippedIdenticalCount++
                        }
                    }
                } else if (targetHotelId != null && payload.hotels.size == 1 && localHotels.any { it.id == targetHotelId } && payload.dataType != "FULL_BACKUP") {
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
        } else if (targetHotelId != null && localHotels.any { it.id == targetHotelId }) {
            autoLinkedHotelIds[1] = targetHotelId
        } else if (localHotels.isNotEmpty()) {
            autoLinkedHotelIds[1] = localHotels.first().id
        }

        // 2. BANK ACCOUNT MAPPING & MISMATCH DETECTION
        val autoLinkedBankIds = mutableMapOf<Int, Int>()
        if (payload.bankAccounts.isNotEmpty()) {
            payload.bankAccounts.forEach { impAcc ->
                val normalizedImp = normalizeName(impAcc.name)
                val impAccNo = impAcc.accountNumber.trim()
                val impUpi = impAcc.upiId.trim().lowercase(Locale.ROOT)
                val exactMatch = localBankAccounts.firstOrNull { localB ->
                    normalizeName(localB.name) == normalizedImp ||
                        (impAccNo.isNotBlank() && localB.accountNumber.trim() == impAccNo) ||
                        (impUpi.isNotBlank() && localB.upiId.trim().lowercase(Locale.ROOT) == impUpi)
                }
                if (exactMatch != null) {
                    autoLinkedBankIds[impAcc.id] = exactMatch.id
                    if (impAcc.id in payload.stubBankAccountIds) {
                        // Lightweight parent reference stub — link transactions silently without raising account conflict
                        skippedIdenticalCount++
                    } else {
                        val hasMismatch = normalizeName(exactMatch.name) != normalizedImp ||
                            exactMatch.accountNumber.trim() != impAccNo ||
                            exactMatch.ifsc.trim().uppercase(Locale.ROOT) != impAcc.ifsc.trim().uppercase(Locale.ROOT) ||
                            exactMatch.branchName.trim() != impAcc.branchName.trim() ||
                            exactMatch.upiId.trim().lowercase(Locale.ROOT) != impUpi ||
                            exactMatch.details.trim() != impAcc.details.trim() ||
                            (impAcc.qrCodeUri.isNotBlank() && exactMatch.qrCodeUri != impAcc.qrCodeUri) ||
                            abs(exactMatch.balance - impAcc.balance) > 0.01
                        if (hasMismatch) {
                            val isNewer = impAcc.lastUpdatedMillis >= exactMatch.lastUpdatedMillis
                            parentConflicts.add(
                                ConflictItem.BankAccountConflict(
                                    conflictKey = "bank_${exactMatch.id}_${impAcc.id}",
                                    title = "Bank: ${exactMatch.name}",
                                    subtitle = "Balance or bank details differ from local account",
                                    timestamp = impAcc.lastUpdatedMillis,
                                    isImportedNewer = isNewer,
                                    localBankAccount = exactMatch,
                                    importedBankAccount = impAcc.copy(
                                        id = exactMatch.id,
                                        qrCodeUri = impAcc.qrCodeUri.ifBlank { exactMatch.qrCodeUri },
                                        importedFrom = impAcc.importedFrom ?: exactMatch.importedFrom
                                    )
                                )
                            )
                        } else {
                            skippedIdenticalCount++
                        }
                    }
                } else if (targetBankAccountId != null && payload.bankAccounts.size == 1 && localBankAccounts.any { it.id == targetBankAccountId } && payload.dataType != "FULL_BACKUP") {
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
        } else if (targetBankAccountId != null && localBankAccounts.any { it.id == targetBankAccountId }) {
            autoLinkedBankIds[1] = targetBankAccountId
        } else if (localBankAccounts.isNotEmpty()) {
            autoLinkedBankIds[1] = localBankAccounts.first().id
        }

        // 3. OWNER ACCOUNT MAPPING & MISMATCH DETECTION
        val autoLinkedOwnerIds = mutableMapOf<Int, Int>()
        payload.ownerAccounts.forEach { impOwner ->
            val normalizedOwner = normalizeName(impOwner.ownerName)
            val normalizedAcc = normalizeName(impOwner.accountName)
            val impAccNo = impOwner.accountNumber.trim()
            val impUpi = impOwner.upiId.trim().lowercase(Locale.ROOT)
            val exactMatch = localOwnerAccounts.firstOrNull { localO ->
                (impAccNo.isNotBlank() && localO.accountNumber.trim() == impAccNo) ||
                    (impUpi.isNotBlank() && localO.upiId.trim().lowercase(Locale.ROOT) == impUpi) ||
                    (normalizedAcc.isNotBlank() && normalizeName(localO.accountName) == normalizedAcc && normalizeName(localO.ownerName) == normalizedOwner) ||
                    (normalizedOwner.isNotBlank() && normalizeName(localO.ownerName) == normalizedOwner && localO.accountNumber.isBlank() && impAccNo.isBlank())
            }
            if (exactMatch != null) {
                autoLinkedOwnerIds[impOwner.id] = exactMatch.id
                val hasMismatch = normalizeName(exactMatch.ownerName) != normalizedOwner ||
                    normalizeName(exactMatch.accountName) != normalizedAcc ||
                    exactMatch.ownerMobile.trim() != impOwner.ownerMobile.trim() ||
                    exactMatch.accountNumber.trim() != impAccNo ||
                    exactMatch.ifscCode.trim().uppercase(Locale.ROOT) != impOwner.ifscCode.trim().uppercase(Locale.ROOT) ||
                    exactMatch.branchName.trim() != impOwner.branchName.trim() ||
                    exactMatch.upiId.trim().lowercase(Locale.ROOT) != impUpi ||
                    exactMatch.details.trim() != impOwner.details.trim() ||
                    (impOwner.qrCodeUri.isNotBlank() && exactMatch.qrCodeUri != impOwner.qrCodeUri)
                if (hasMismatch) {
                    val isNewer = impOwner.lastUpdatedMillis >= exactMatch.lastUpdatedMillis
                    parentConflicts.add(
                        ConflictItem.OwnerAccountConflict(
                            conflictKey = "owner_${exactMatch.id}_${impOwner.id}",
                            title = "Owner Account: ${exactMatch.accountName.ifBlank { exactMatch.ownerName }}",
                            subtitle = "Owner profile account details differ",
                            timestamp = impOwner.lastUpdatedMillis,
                            isImportedNewer = isNewer,
                            localOwnerAccount = exactMatch,
                            importedOwnerAccount = impOwner.copy(
                                id = exactMatch.id,
                                qrCodeUri = impOwner.qrCodeUri.ifBlank { exactMatch.qrCodeUri },
                                importedFrom = impOwner.importedFrom ?: exactMatch.importedFrom
                            )
                        )
                    )
                } else {
                    skippedIdenticalCount++
                }
            } else {
                unmappedParents.add(
                    UnmappedParent(
                        entityKey = "owner_${impOwner.id}",
                        entityType = ParentEntityType.OWNER_ACCOUNT,
                        originalId = impOwner.id,
                        importedName = impOwner.accountName.ifBlank { impOwner.ownerName },
                        importedSubtitle = listOfNotNull(impOwner.ownerName.ifBlank { null }, impOwner.branchName.ifBlank { null }, impOwner.accountNumber.ifBlank { null }).joinToString(" • "),
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
                    pendingNewMenuItems.add(
                        PendingNewMenuItem(
                            itemKey = "menu_new_${mappedHotelId}_${impMenu.englishName}_${impMenu.banglaName}",
                            targetHotelId = mappedHotelId,
                            targetHotelName = hotelName,
                            menuItem = impMenu.copy(hotelId = mappedHotelId)
                        )
                    )
                } else {
                    val priceChanged = abs(localExisting.price - impMenu.price) > 0.01
                    val flagsChanged = localExisting.isDayMenu != impMenu.isDayMenu ||
                        localExisting.isNightMenu != impMenu.isNightMenu ||
                        localExisting.isAddOn != impMenu.isAddOn ||
                        normalizeName(localExisting.banglaName) != impBangla ||
                        normalizeName(localExisting.englishName) != impEnglish
                    if (priceChanged || flagsChanged) {
                        val isNewer = impMenu.lastUpdatedMillis >= localExisting.lastUpdatedMillis
                        menuConflicts.add(
                            ConflictItem.MenuItemConflict(
                                conflictKey = "menu_${localExisting.id}_${impMenu.englishName}_${impMenu.banglaName}",
                                title = "${impMenu.englishName.ifBlank { impMenu.banglaName }} ($hotelName)",
                                subtitle = if (priceChanged) {
                                    "Price mismatch: Local ₹${localExisting.price} vs Incoming ₹${impMenu.price}"
                                } else {
                                    "Menu session/details differ from local item"
                                },
                                timestamp = impMenu.lastUpdatedMillis,
                                isImportedNewer = isNewer,
                                localMenuItem = localExisting,
                                importedMenuItem = impMenu.copy(id = localExisting.id, hotelId = mappedHotelId),
                                hotelName = hotelName
                            )
                        )
                    } else {
                        skippedIdenticalCount++
                    }
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

                val sameDayCandidates = localMeals.filter { localM ->
                    localM.hotelId == mappedHotelId &&
                        localM.isNight == impMeal.isNight &&
                        startOfDay(localM.timestamp) == impDayStart
                }
                val exactMealMatch = sameDayCandidates.firstOrNull { localM ->
                    abs(localM.totalAmount - impMeal.totalAmount) < 0.01 &&
                        localM.itemsSerialized.trim() == impMeal.itemsSerialized.trim()
                }
                val localMealMatch = exactMealMatch ?: sameDayCandidates.firstOrNull()

                if (localMealMatch != null) {
                    val isIdentical = exactMealMatch != null
                    if (!isIdentical) {
                        val isNewer = impMeal.lastUpdatedMillis >= localMealMatch.lastUpdatedMillis
                        val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(impMeal.timestamp))
                        val sessionName = if (impMeal.isNight) "Night" else "Day"

                        mealConflicts.add(
                            ConflictItem.MealConflict(
                                conflictKey = "meal_${localMealMatch.id}_${impMeal.timestamp}",
                                title = "$hotelName • $sessionName Session ($dateFormatted)",
                                subtitle = "Local ₹${localMealMatch.totalAmount} vs Incoming ₹${impMeal.totalAmount}",
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
                        skippedIdenticalCount++
                    }
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

                val windowCandidates = localExpenses.filter { localE ->
                    localE.bankAccountId == mappedBankId &&
                        localE.type.equals(impExp.type, ignoreCase = true) &&
                        abs(localE.amount - impExp.amount) < 0.01 &&
                        abs(localE.timestamp - impExp.timestamp) <= TRANSACTION_FUZZY_WINDOW_MS
                }
                val exactExpMatch = windowCandidates.firstOrNull { localE ->
                    normalizeName(localE.name) == normalizeName(impExp.name) &&
                        normalizeName(localE.category) == normalizeName(impExp.category) &&
                        localE.notes.trim() == impExp.notes.trim()
                }
                val localExpMatch = exactExpMatch ?: windowCandidates.firstOrNull()

                if (localExpMatch != null) {
                    val isIdentical = exactExpMatch != null
                    if (!isIdentical) {
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
                        skippedIdenticalCount++
                    }
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
                startOfDay(localR.readingDate) == impDayStart ||
                    (abs(localR.totalReading - impReading.totalReading) < 0.01 &&
                        abs(startOfDay(localR.readingDate) - impDayStart) <= 24L * 3600L * 1000L)
            }

            if (match != null) {
                val isIdentical = abs(match.totalReading - impReading.totalReading) < 0.01 &&
                    abs(match.unitsConsumed - impReading.unitsConsumed) < 0.01 &&
                    match.isPaid == impReading.isPaid
                if (!isIdentical) {
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
                    skippedIdenticalCount++
                }
            } else {
                directNewReadings.add(impReading.copy(id = UUID.randomUUID().toString()))
            }
        }

        // 8. UTILITY BILLS (with PSID / Partial Payment diff detection)
        val billConflicts = mutableListOf<ConflictItem>()
        val directNewBills = mutableListOf<UtilityBill>()

        payload.utilityBills.forEach { impBill ->
            val impStartDay = startOfDay(impBill.startDate)
            val impEndDay = startOfDay(impBill.endDate)

            val match = localBills.firstOrNull { localB ->
                startOfDay(localB.startDate) == impStartDay &&
                    startOfDay(localB.endDate) == impEndDay
            }

            if (match != null) {
                val isIdentical = abs(match.calculatedAmount - impBill.calculatedAmount) < 0.01 &&
                    abs(match.totalUnits - impBill.totalUnits) < 0.01 &&
                    match.isPaid == impBill.isPaid &&
                    (match.transactionId ?: "") == (impBill.transactionId ?: "") &&
                    match.note.trim() == impBill.note.trim()
                if (!isIdentical) {
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
                    skippedIdenticalCount++
                }
            } else {
                directNewBills.add(impBill.copy(id = UUID.randomUUID().toString()))
            }
        }

        val existingPayments = hotelPaymentDao.getAllPaymentsSync().filter { !it.isDeleted }
        val directNewHotelPayments = payload.hotelPayments.mapNotNull { pay ->
            val mappedHotelId = autoLinkedHotelIds[pay.hotelId] ?: return@mapNotNull null
            val alreadyExists = existingPayments.any { existing ->
                existing.hotelId == mappedHotelId && existing.year == pay.year && existing.month == pay.month &&
                    abs(existing.amountPaid - pay.amountPaid) < 0.01 && existing.note.trim() == pay.note.trim()
            }
            if (alreadyExists) {
                skippedIdenticalCount++
                null
            } else {
                pay.copy(id = 0, hotelId = mappedHotelId)
            }
        }

        val localStickyNotes = stickyNoteDao.getAllStickyNotesSync().filter { !it.isDeleted }
        val seenStickyKeys = localStickyNotes.mapTo(mutableSetOf()) {
            "${normalizeName(it.title)}|${normalizeName(it.subtitle)}|${it.bodyText.trim()}"
        }
        val directNewStickyNotes = payload.stickyNotes.mapNotNull { note ->
            val sig = "${normalizeName(note.title)}|${normalizeName(note.subtitle)}|${note.bodyText.trim()}"
            if (seenStickyKeys.add(sig)) {
                note.copy(id = 0)
            } else {
                skippedIdenticalCount++
                null
            }
        }

        val localRates = meterDao.getAllElectricityRatesSync().filter { !it.isDeleted }
        val directNewRates = mutableListOf<ElectricityRate>()
        payload.electricityRates.forEach { impRate ->
            val impDay = startOfDay(impRate.effectiveDate)
            val exists = localRates.any { local ->
                startOfDay(local.effectiveDate) == impDay && abs(local.ratePerUnit - impRate.ratePerUnit) < 0.01
            } || directNewRates.any { added ->
                startOfDay(added.effectiveDate) == impDay && abs(added.ratePerUnit - impRate.ratePerUnit) < 0.01
            }
            if (!exists) {
                directNewRates.add(impRate.copy(id = UUID.randomUUID().toString()))
            } else {
                skippedIdenticalCount++
            }
        }

        val allConflicts = (parentConflicts + menuConflicts + mealConflicts + expenseConflicts + readingConflicts + billConflicts)
            .sortedByDescending { it.timestamp }

        val combinedDupCount = dedupResult.duplicateCount +
            localScan.duplicateMealIds.size +
            localScan.duplicateExpenseIds.size +
            localScan.duplicateMenuIds.size
        val combinedDupSummary = dedupResult.duplicateSummary + localScan.summaryLines

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
            directNewOwnerAccounts = emptyList(),
            directNewMenus = directNewMenus,
            directNewMeals = directNewMeals,
            directNewExpenses = directNewExpenses,
            directNewReadings = directNewReadings,
            directNewBills = directNewBills,
            directNewHotelPayments = directNewHotelPayments,
            directNewStickyNotes = directNewStickyNotes,
            directNewElectricityRates = directNewRates,
            duplicateRecordsInFileCount = combinedDupCount,
            duplicateRecordsInFileSummary = combinedDupSummary,
            localDuplicateMealIdsToDelete = localScan.duplicateMealIds,
            localDuplicateExpenseIdsToDelete = localScan.duplicateExpenseIds,
            localDuplicateMenuIdsToDelete = localScan.duplicateMenuIds,
            orphanHotelIdsToAdopt = localScan.orphanHotelIds,
            orphanBankIdsToAdopt = localScan.orphanBankIds,
            skippedIdenticalCount = skippedIdenticalCount
        )
    }

    /**
     * Resolves unmapped parent decisions (from Phase 3 UI) and processes their child entities.
     * Note: We keep the original `id` on `directNewHotels` / `directNewBankAccounts` so `SyncDao.executeMergeTransaction`
     * can map `originalHotelId -> newlyInsertedRoomId` for all child menus, meal orders, payments, and expenses!
     * Furthermore, even when creating a new parent, we check against any orphaned local records so re-importing never duplicates history.
     */
    suspend fun resolveParentDecisions(
        previousAnalysis: SyncAnalysisResult,
        parentDecisions: Map<String, ParentResolution> // key e.g. "hotel_1" -> MapToExisting or CreateNew
    ): SyncAnalysisResult = withContext(Dispatchers.IO) {
        val payload = previousAnalysis.payload
        val senderName = previousAnalysis.senderName

        val localHotels = hotelDao.getAllHotelsSync().filter { !it.isDeleted }
        val localBankAccounts = bankAccountDao.getAllBankAccountsSync().filter { !it.isDeleted }
        val rawLocalMenus = menuDao.getAllMenuItemsSync().filter { !it.isDeleted }
        val rawLocalMeals = transactionDao.getAllTransactionsSync().filter { !it.isDeleted }
        val rawLocalExpenses = extraExpenseDao.getAllExpensesSync().filter { !it.isDeleted }

        val localScan = findLocalDuplicatesAndCleanLists(
            localHotels = localHotels,
            localBanks = localBankAccounts,
            localMeals = rawLocalMeals,
            localExpenses = rawLocalExpenses,
            localMenus = rawLocalMenus
        )
        val localMenus = localScan.cleanMenus
        val localMeals = localScan.cleanMeals
        val localExpenses = localScan.cleanExpenses

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
            if (!previousAnalysis.autoLinkedHotelIds.containsKey(impHotel.id)) {
                val key = "hotel_${impHotel.id}"
                when (val decision = parentDecisions[key]) {
                    is ParentResolution.MapToExisting -> {
                        finalHotelMap[impHotel.id] = decision.targetLocalId
                    }
                    is ParentResolution.CreateNew, null -> {
                        val effectiveSender = (decision as? ParentResolution.CreateNew)?.senderName ?: senderName
                        // Keep impHotel.id so SyncDao can map impHotel.id -> generated DB id!
                        directNewHotels.add(impHotel.copy(importedFrom = effectiveSender))
                        newParentHotelIds.add(impHotel.id)
                    }
                }
            }
        }

        // Apply decisions to bank accounts
        payload.bankAccounts.forEach { impAcc ->
            if (!previousAnalysis.autoLinkedBankIds.containsKey(impAcc.id)) {
                val key = "bank_${impAcc.id}"
                when (val decision = parentDecisions[key]) {
                    is ParentResolution.MapToExisting -> {
                        finalBankMap[impAcc.id] = decision.targetLocalId
                    }
                    is ParentResolution.CreateNew, null -> {
                        val effectiveSender = (decision as? ParentResolution.CreateNew)?.senderName ?: senderName
                        // Keep impAcc.id so SyncDao can map impAcc.id -> generated DB id!
                        directNewBankAccounts.add(impAcc.copy(importedFrom = effectiveSender))
                        newParentBankIds.add(impAcc.id)
                    }
                }
            }
        }

        // Apply decisions to owner accounts
        payload.ownerAccounts.forEach { impOwner ->
            if (!previousAnalysis.autoLinkedOwnerIds.containsKey(impOwner.id)) {
                val key = "owner_${impOwner.id}"
                when (val decision = parentDecisions[key]) {
                    is ParentResolution.MapToExisting -> {
                        finalOwnerMap[impOwner.id] = decision.targetLocalId
                    }
                    is ParentResolution.CreateNew, null -> {
                        val effectiveSender = (decision as? ParentResolution.CreateNew)?.senderName ?: senderName
                        directNewOwnerAccounts.add(impOwner.copy(id = 0, importedFrom = effectiveSender))
                    }
                }
            }
        }

        val pendingNewMenuItems = previousAnalysis.pendingNewMenuItems.toMutableList()
        val allConflicts = previousAnalysis.conflicts.toMutableList()
        val directNewMenus = previousAnalysis.directNewMenus.toMutableList()
        val directNewMeals = previousAnalysis.directNewMeals.toMutableList()
        val directNewExpenses = previousAnalysis.directNewExpenses.toMutableList()
        var skippedIdenticalCount = previousAnalysis.skippedIdenticalCount

        // Orphaned local records (e.g. from an earlier import where hotel was not created)
        val orphanedLocalMenus = localMenus.filter { localScan.orphanHotelIds.contains(it.hotelId) }
        val orphanedLocalMeals = localMeals.filter { localScan.orphanHotelIds.contains(it.hotelId) }
        val orphanedLocalExpenses = localExpenses.filter { localScan.orphanBankIds.contains(it.bankAccountId) }

        // Process Menus for newly decided hotels
        payload.menus.forEach { impMenu ->
            if (newParentHotelIds.contains(impMenu.hotelId)) {
                val impBangla = normalizeName(impMenu.banglaName)
                val impEnglish = normalizeName(impMenu.englishName)
                val orphanMatch = orphanedLocalMenus.firstOrNull { localM ->
                    (impEnglish.isNotBlank() && normalizeName(localM.englishName) == impEnglish) ||
                        (impBangla.isNotBlank() && normalizeName(localM.banglaName) == impBangla)
                }
                if (orphanMatch == null) {
                    directNewMenus.add(impMenu.copy(id = 0))
                } else {
                    skippedIdenticalCount++
                }
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
                } else {
                    val priceChanged = abs(localExisting.price - impMenu.price) > 0.01
                    val flagsChanged = localExisting.isDayMenu != impMenu.isDayMenu ||
                        localExisting.isNightMenu != impMenu.isNightMenu ||
                        localExisting.isAddOn != impMenu.isAddOn ||
                        normalizeName(localExisting.banglaName) != impBangla ||
                        normalizeName(localExisting.englishName) != impEnglish
                    if (priceChanged || flagsChanged) {
                        val isNewer = impMenu.lastUpdatedMillis >= localExisting.lastUpdatedMillis
                        allConflicts.add(
                            ConflictItem.MenuItemConflict(
                                conflictKey = "menu_${localExisting.id}_${impMenu.englishName}_${impMenu.banglaName}",
                                title = "${impMenu.englishName.ifBlank { impMenu.banglaName }} ($hotelName)",
                                subtitle = if (priceChanged) "Price mismatch: Local ₹${localExisting.price} vs Incoming ₹${impMenu.price}" else "Menu session/details differ from local item",
                                timestamp = impMenu.lastUpdatedMillis,
                                isImportedNewer = isNewer,
                                localMenuItem = localExisting,
                                importedMenuItem = impMenu.copy(id = localExisting.id, hotelId = mappedHotelId),
                                hotelName = hotelName
                            )
                        )
                    } else {
                        skippedIdenticalCount++
                    }
                }
            }
        }

        // Process Meals for newly decided hotels
        payload.transactions.forEach { impMeal ->
            if (newParentHotelIds.contains(impMeal.hotelId)) {
                val impDayStart = startOfDay(impMeal.timestamp)
                val orphanExact = orphanedLocalMeals.firstOrNull { localM ->
                    localM.isNight == impMeal.isNight &&
                        startOfDay(localM.timestamp) == impDayStart &&
                        abs(localM.totalAmount - impMeal.totalAmount) < 0.01 &&
                        localM.itemsSerialized.trim() == impMeal.itemsSerialized.trim()
                }
                if (orphanExact == null) {
                    directNewMeals.add(impMeal.copy(id = 0))
                } else {
                    skippedIdenticalCount++
                }
            } else if (finalHotelMap.containsKey(impMeal.hotelId) && !previousAnalysis.autoLinkedHotelIds.containsKey(impMeal.hotelId)) {
                val mappedHotelId = finalHotelMap[impMeal.hotelId]!!
                val hotelObj = localHotels.firstOrNull { it.id == mappedHotelId }
                val hotelName = hotelObj?.name ?: "Hotel #$mappedHotelId"
                val impDayStart = startOfDay(impMeal.timestamp)

                val sameDayCandidates = localMeals.filter { localM ->
                    localM.hotelId == mappedHotelId &&
                        localM.isNight == impMeal.isNight &&
                        startOfDay(localM.timestamp) == impDayStart
                }
                val exactMealMatch = sameDayCandidates.firstOrNull { localM ->
                    abs(localM.totalAmount - impMeal.totalAmount) < 0.01 &&
                        localM.itemsSerialized.trim() == impMeal.itemsSerialized.trim()
                }
                val localMealMatch = exactMealMatch ?: sameDayCandidates.firstOrNull()

                if (localMealMatch != null) {
                    val isIdentical = exactMealMatch != null
                    if (!isIdentical) {
                        val isNewer = impMeal.lastUpdatedMillis >= localMealMatch.lastUpdatedMillis
                        val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(impMeal.timestamp))
                        val sessionName = if (impMeal.isNight) "Night" else "Day"

                        allConflicts.add(
                            ConflictItem.MealConflict(
                                conflictKey = "meal_${localMealMatch.id}_${impMeal.timestamp}",
                                title = "$hotelName • $sessionName Session ($dateFormatted)",
                                subtitle = "Local ₹${localMealMatch.totalAmount} vs Incoming ₹${impMeal.totalAmount}",
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
                        skippedIdenticalCount++
                    }
                } else {
                    directNewMeals.add(impMeal.copy(id = 0, hotelId = mappedHotelId))
                }
            }
        }

        // Process Expenses for newly decided accounts
        payload.expenses.forEach { impExp ->
            if (newParentBankIds.contains(impExp.bankAccountId)) {
                val orphanExact = orphanedLocalExpenses.firstOrNull { localE ->
                    localE.type.equals(impExp.type, ignoreCase = true) &&
                        abs(localE.amount - impExp.amount) < 0.01 &&
                        abs(localE.timestamp - impExp.timestamp) <= TRANSACTION_FUZZY_WINDOW_MS &&
                        normalizeName(localE.name) == normalizeName(impExp.name)
                }
                if (orphanExact == null) {
                    directNewExpenses.add(impExp.copy(id = 0))
                } else {
                    skippedIdenticalCount++
                }
            } else if (finalBankMap.containsKey(impExp.bankAccountId) && !previousAnalysis.autoLinkedBankIds.containsKey(impExp.bankAccountId)) {
                val mappedBankId = finalBankMap[impExp.bankAccountId]!!
                val accObj = localBankAccounts.firstOrNull { it.id == mappedBankId }
                val accName = accObj?.name ?: "Account #$mappedBankId"

                val windowCandidates = localExpenses.filter { localE ->
                    localE.bankAccountId == mappedBankId &&
                        localE.type.equals(impExp.type, ignoreCase = true) &&
                        abs(localE.amount - impExp.amount) < 0.01 &&
                        abs(localE.timestamp - impExp.timestamp) <= TRANSACTION_FUZZY_WINDOW_MS
                }
                val exactExpMatch = windowCandidates.firstOrNull { localE ->
                    normalizeName(localE.name) == normalizeName(impExp.name) &&
                        normalizeName(localE.category) == normalizeName(impExp.category) &&
                        localE.notes.trim() == impExp.notes.trim()
                }
                val localExpMatch = exactExpMatch ?: windowCandidates.firstOrNull()

                if (localExpMatch != null) {
                    val isIdentical = exactExpMatch != null
                    if (!isIdentical) {
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
                        skippedIdenticalCount++
                    }
                } else {
                    directNewExpenses.add(impExp.copy(id = 0, bankAccountId = mappedBankId))
                }
            }
        }

        val existingPayments = hotelPaymentDao.getAllPaymentsSync().filter { payment -> !payment.isDeleted }
        val updatedHotelPayments = previousAnalysis.directNewHotelPayments.toMutableList()
        payload.hotelPayments.forEach { impPay ->
            if (newParentHotelIds.contains(impPay.hotelId)) {
                val alreadyExists = existingPayments.any { existing ->
                    existing.year == impPay.year && existing.month == impPay.month &&
                        abs(existing.amountPaid - impPay.amountPaid) < 0.01 && existing.note.trim() == impPay.note.trim()
                }
                if (!alreadyExists) {
                    updatedHotelPayments.add(impPay.copy(id = 0))
                } else {
                    skippedIdenticalCount++
                }
            } else if (!previousAnalysis.autoLinkedHotelIds.containsKey(impPay.hotelId) && finalHotelMap.containsKey(impPay.hotelId)) {
                val mappedHotelId = finalHotelMap[impPay.hotelId]!!
                val alreadyExists = existingPayments.any { existing ->
                    existing.hotelId == mappedHotelId && existing.year == impPay.year && existing.month == impPay.month &&
                        abs(existing.amountPaid - impPay.amountPaid) < 0.01 && existing.note.trim() == impPay.note.trim()
                }
                if (!alreadyExists) {
                    updatedHotelPayments.add(impPay.copy(id = 0, hotelId = mappedHotelId))
                } else {
                    skippedIdenticalCount++
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
            directNewOwnerAccounts = directNewOwnerAccounts,
            directNewMenus = directNewMenus,
            directNewMeals = directNewMeals,
            directNewExpenses = directNewExpenses,
            directNewHotelPayments = updatedHotelPayments,
            skippedIdenticalCount = skippedIdenticalCount
        )
    }

    fun autoResolveAllLatest(conflicts: List<ConflictItem>): Map<String, ConflictResolution> {
        return conflicts.associate { conflict ->
            val choice = if (conflict.isImportedNewer) ConflictResolution.TAKE_IMPORTED else ConflictResolution.KEEP_LOCAL
            conflict.conflictKey to choice
        }
    }

    fun autoResolveAllLocal(conflicts: List<ConflictItem>): Map<String, ConflictResolution> {
        return conflicts.associate { conflict ->
            conflict.conflictKey to ConflictResolution.KEEP_LOCAL
        }
    }

    fun buildFinalMergePlan(
        analysis: SyncAnalysisResult,
        acceptedMenuItemKeys: Set<String>,
        conflictResolutions: Map<String, ConflictResolution>
    ): MergePlan {
        val hotelsToInsert = analysis.directNewHotels.toMutableList()
        val hotelsToUpdate = mutableListOf<HotelEntity>()
        val bankAccountsToInsert = analysis.directNewBankAccounts.toMutableList()
        val bankAccountsToUpdate = mutableListOf<BankAccountEntity>()
        val ownerAccountsToInsert = analysis.directNewOwnerAccounts.toMutableList()
        val ownerAccountsToUpdate = mutableListOf<OwnerAccountEntity>()

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

        // 2. Resolve conflicts based on user decisions (always preserve local data by default)
        analysis.conflicts.forEach { conflict ->
            val decision = conflictResolutions[conflict.conflictKey] ?: ConflictResolution.KEEP_LOCAL

            when (conflict) {
                is ConflictItem.HotelConflict -> {
                    when (decision) {
                        ConflictResolution.KEEP_LOCAL -> { /* Local kept */ }
                        ConflictResolution.TAKE_IMPORTED -> hotelsToUpdate.add(conflict.importedHotel.copy(id = conflict.localHotel.id))
                        ConflictResolution.KEEP_BOTH -> hotelsToInsert.add(conflict.importedHotel.copy(id = 0))
                    }
                }
                is ConflictItem.BankAccountConflict -> {
                    when (decision) {
                        ConflictResolution.KEEP_LOCAL -> { /* Local kept */ }
                        ConflictResolution.TAKE_IMPORTED -> bankAccountsToUpdate.add(conflict.importedBankAccount.copy(id = conflict.localBankAccount.id))
                        ConflictResolution.KEEP_BOTH -> bankAccountsToInsert.add(conflict.importedBankAccount.copy(id = 0))
                    }
                }
                is ConflictItem.OwnerAccountConflict -> {
                    when (decision) {
                        ConflictResolution.KEEP_LOCAL -> { /* Local kept */ }
                        ConflictResolution.TAKE_IMPORTED -> ownerAccountsToUpdate.add(conflict.importedOwnerAccount.copy(id = conflict.localOwnerAccount.id))
                        ConflictResolution.KEEP_BOTH -> ownerAccountsToInsert.add(conflict.importedOwnerAccount.copy(id = 0))
                    }
                }
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
        val electricityRatesToInsert = analysis.directNewElectricityRates

        val totalCount = hotelsToInsert.size + hotelsToUpdate.size +
                bankAccountsToInsert.size + bankAccountsToUpdate.size +
                ownerAccountsToInsert.size + ownerAccountsToUpdate.size +
                menusToInsert.size + menusToUpdate.size +
                mealsToInsert.size + mealsToUpdate.size +
                expensesToInsert.size + expensesToUpdate.size +
                readingsToInsert.size + readingsToUpdate.size +
                billsToInsert.size + billsToUpdate.size +
                hotelPaymentsToInsert.size + stickyNotesToInsert.size +
                electricityRatesToInsert.size +
                analysis.localDuplicateMealIdsToDelete.size +
                analysis.localDuplicateExpenseIdsToDelete.size +
                analysis.localDuplicateMenuIdsToDelete.size

        // Protect the receiver's local user profile & preferences during multi-domain sync;
        // apply profile/settings restore when explicitly importing Settings or Profile-only custom backups.
        val isProfileOrSettingsOnly = analysis.payload.dataType == "SETTINGS" ||
            (analysis.payload.dataType == "CUSTOM_SELECTION" &&
                analysis.payload.userProfile != null &&
                analysis.payload.hotels.isEmpty() &&
                analysis.payload.bankAccounts.isEmpty() &&
                analysis.payload.meterReadings.isEmpty() &&
                analysis.payload.utilityBills.isEmpty() &&
                analysis.payload.stickyNotes.isEmpty())
        val safeProfileUpdate = if (isProfileOrSettingsOnly) {
            analysis.payload.userProfile
        } else {
            null
        }

        return MergePlan(
            senderName = analysis.senderName,
            hotelsToInsert = hotelsToInsert,
            hotelsToUpdate = hotelsToUpdate,
            bankAccountsToInsert = bankAccountsToInsert,
            bankAccountsToUpdate = bankAccountsToUpdate,
            ownerAccountsToInsert = ownerAccountsToInsert,
            ownerAccountsToUpdate = ownerAccountsToUpdate,
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
            electricityRatesToInsert = electricityRatesToInsert,
            userProfileToUpdate = safeProfileUpdate,
            duplicateRecordsInFileCount = analysis.duplicateRecordsInFileCount,
            duplicateRecordsInFileSummary = analysis.duplicateRecordsInFileSummary,
            localDuplicateMealIdsToDelete = analysis.localDuplicateMealIdsToDelete,
            localDuplicateExpenseIdsToDelete = analysis.localDuplicateExpenseIdsToDelete,
            localDuplicateMenuIdsToDelete = analysis.localDuplicateMenuIdsToDelete,
            orphanHotelIdsToAdopt = analysis.orphanHotelIdsToAdopt,
            orphanBankIdsToAdopt = analysis.orphanBankIdsToAdopt,
            skippedIdenticalCount = analysis.skippedIdenticalCount,
            totalAffectedCount = totalCount
        )
    }

    suspend fun executeMergePlan(plan: MergePlan): Int? = withContext(Dispatchers.IO) {
        db.syncDao().executeMergeTransaction(plan)
    }
}
