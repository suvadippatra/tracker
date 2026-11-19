package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unified, modular export selection descriptor covering Full Backup and Custom (All or Individual)
 * exports across all 6 application data domains.
 */
data class ExportSelection(
    val isFullBackup: Boolean = true,
    // 1. Hotels Domain
    val includeHotelsDomain: Boolean = true,
    val selectedHotelIds: Set<Int> = emptySet(), // empty = all active hotels
    val includeHotelInfo: Boolean = true,
    val includeHotelMenus: Boolean = true,
    val includeHotelOrders: Boolean = true,
    // 2. Extras & Accounts Domain
    val includeExtrasDomain: Boolean = true,
    val selectedAccountIds: Set<Int> = emptySet(), // empty = all active bank accounts
    val includeBankAccounts: Boolean = true,
    val includeTransactions: Boolean = true,
    // 3. Electricity Domain
    val includeElectricityDomain: Boolean = true,
    val includeMeterReadings: Boolean = true,
    val includeUtilityBills: Boolean = true,
    val includeElectricityRates: Boolean = true,
    // 4. Sticky Notes Domain
    val includeStickyDomain: Boolean = true,
    val selectedStickyTags: Set<String> = emptySet(), // empty = all tags/notes
    val selectedStickyNoteIds: Set<Int> = emptySet(), // empty = all matching notes
    // 5. Profile & Owner Accounts Domain
    val includeProfileDomain: Boolean = true,
    val includeOwnerAccounts: Boolean = true,
    // 6. App Settings Domain
    val includeSettingsDomain: Boolean = true
) {
    fun hasAnySelection(): Boolean {
        if (isFullBackup) return true
        val hotelsSelected = includeHotelsDomain && (includeHotelInfo || includeHotelMenus || includeHotelOrders)
        val extrasSelected = includeExtrasDomain && (includeBankAccounts || includeTransactions)
        val elecSelected = includeElectricityDomain && (includeMeterReadings || includeUtilityBills || includeElectricityRates)
        val stickySelected = includeStickyDomain
        val profileSelected = includeProfileDomain || includeOwnerAccounts
        val settingsSelected = includeSettingsDomain
        return hotelsSelected || extrasSelected || elecSelected || stickySelected || profileSelected || settingsSelected
    }

    fun toDisplayTitle(
        hotels: List<HotelEntity> = emptyList(),
        accounts: List<BankAccountEntity> = emptyList()
    ): String {
        if (isFullBackup) return "Full Backup"
        val parts = mutableListOf<String>()
        if (includeHotelsDomain && (includeHotelInfo || includeHotelMenus || includeHotelOrders)) {
            val hLabel = when {
                selectedHotelIds.size == 1 -> hotels.find { it.id == selectedHotelIds.first() }?.name ?: "1 Hotel"
                selectedHotelIds.isNotEmpty() -> "${selectedHotelIds.size} Hotels"
                else -> "Hotels"
            }
            parts.add(hLabel)
        }
        if (includeExtrasDomain && (includeBankAccounts || includeTransactions)) {
            val aLabel = when {
                selectedAccountIds.size == 1 -> accounts.find { it.id == selectedAccountIds.first() }?.name ?: "1 Account"
                selectedAccountIds.isNotEmpty() -> "${selectedAccountIds.size} Accounts"
                else -> "Extras"
            }
            parts.add(aLabel)
        }
        if (includeElectricityDomain && (includeMeterReadings || includeUtilityBills || includeElectricityRates)) {
            parts.add("Electricity")
        }
        if (includeStickyDomain) parts.add("Notes")
        if (includeProfileDomain || includeOwnerAccounts) parts.add("Profile")
        if (includeSettingsDomain) parts.add("Settings")
        return if (parts.isEmpty()) "Custom Backup" else parts.joinToString(" + ")
    }

    fun toFilePrefix(): String {
        if (isFullBackup) return "traker_full_backup"
        val parts = mutableListOf<String>()
        if (includeHotelsDomain && (includeHotelInfo || includeHotelMenus || includeHotelOrders)) parts.add("hotels")
        if (includeExtrasDomain && (includeBankAccounts || includeTransactions)) parts.add("extras")
        if (includeElectricityDomain && (includeMeterReadings || includeUtilityBills || includeElectricityRates)) parts.add("electricity")
        if (includeStickyDomain) parts.add("notes")
        if (includeProfileDomain || includeOwnerAccounts) parts.add("profile")
        if (includeSettingsDomain) parts.add("settings")
        return "traker_" + (if (parts.isEmpty()) "custom" else parts.take(3).joinToString("_"))
    }
}

/**
 * Unified export engine generating clean JSON (with parent reference stubs for orphan-free child sync),
 * scoped ZIP media file lists, QR entity lists, and Excel-ready CSV snapshots.
 */
class UnifiedExportEngine(private val db: AppDatabase) {

    private val hotelDao = db.hotelDao()
    private val menuDao = db.menuDao()
    private val transactionDao = db.transactionDao()
    private val hotelPaymentDao = db.hotelPaymentDao()
    private val bankAccountDao = db.bankAccountDao()
    private val extraExpenseDao = db.extraExpenseDao()
    private val attachmentDao = db.attachmentDao()
    private val meterDao = db.meterDao()
    private val stickyNoteDao = db.stickyNoteDao()
    private val ownerAccountDao = db.ownerAccountDao()
    private val userProfileDao = db.userProfileDao()

    private data class ResolvedExportData(
        val hotels: List<HotelEntity>,
        val exportFullHotels: Boolean,
        val menus: List<MenuEntity>,
        val transactions: List<HotelTransactionEntity>,
        val hotelPayments: List<HotelPaymentEntity>,
        val bankAccounts: List<BankAccountEntity>,
        val exportFullBankAccounts: Boolean,
        val expenses: List<ExtraExpenseEntity>,
        val readings: List<MeterReading>,
        val utilityBills: List<UtilityBill>,
        val electricityRates: List<ElectricityRate>,
        val stickyNotes: List<StickyNoteEntity>,
        val ownerAccounts: List<OwnerAccountEntity>,
        val userProfile: UserProfileEntity?,
        val includeProfileIdentity: Boolean,
        val includeAppSettings: Boolean
    )

    private suspend fun resolveData(selection: ExportSelection): ResolvedExportData = withContext(Dispatchers.IO) {
        val full = selection.isFullBackup

        val wantHotelsDomain = full || selection.includeHotelsDomain
        val wantHotelInfo = full || (wantHotelsDomain && selection.includeHotelInfo)
        val wantMenus = full || (wantHotelsDomain && selection.includeHotelMenus)
        val wantOrders = full || (wantHotelsDomain && selection.includeHotelOrders)
        val allActiveHotels = if (wantHotelInfo || wantMenus || wantOrders) {
            hotelDao.getAllHotelsSync().filter { !it.isDeleted }
        } else emptyList()
        val scopedHotels = if (full || selection.selectedHotelIds.isEmpty()) {
            allActiveHotels
        } else {
            allActiveHotels.filter { it.id in selection.selectedHotelIds }
        }
        val scopedHotelIds = scopedHotels.map { it.id }.toSet()

        val menus = if (wantMenus && scopedHotelIds.isNotEmpty()) {
            menuDao.getAllMenuItemsSync().filter { !it.isDeleted && it.hotelId in scopedHotelIds }
        } else emptyList()
        val transactions = if (wantOrders && scopedHotelIds.isNotEmpty()) {
            transactionDao.getAllTransactionsSync().filter { !it.isDeleted && it.hotelId in scopedHotelIds }
        } else emptyList()
        val hotelPayments = if (wantOrders && scopedHotelIds.isNotEmpty()) {
            hotelPaymentDao.getAllPaymentsSync().filter { !it.isDeleted && it.hotelId in scopedHotelIds }
        } else emptyList()

        val wantExtrasDomain = full || selection.includeExtrasDomain
        val wantBankInfo = full || (wantExtrasDomain && selection.includeBankAccounts)
        val wantExpenses = full || (wantExtrasDomain && selection.includeTransactions)
        val allActiveBanks = if (wantBankInfo || wantExpenses) {
            bankAccountDao.getAllBankAccountsSync().filter { !it.isDeleted }
        } else emptyList()
        val scopedBanks = if (full || selection.selectedAccountIds.isEmpty()) {
            allActiveBanks
        } else {
            allActiveBanks.filter { it.id in selection.selectedAccountIds }
        }
        val scopedBankIds = scopedBanks.map { it.id }.toSet()

        val expenses = if (wantExpenses && scopedBankIds.isNotEmpty()) {
            extraExpenseDao.getAllExpensesSync().filter { !it.isDeleted && it.bankAccountId in scopedBankIds }
        } else emptyList()

        val wantElecDomain = full || selection.includeElectricityDomain
        val readings = if (full || (wantElecDomain && selection.includeMeterReadings)) {
            meterDao.getAllReadingsSync().filter { !it.isDeleted }
        } else emptyList()
        val bills = if (full || (wantElecDomain && selection.includeUtilityBills)) {
            meterDao.getAllUtilityBillsSync().filter { !it.isDeleted }
        } else emptyList()
        val rates = if (full || (wantElecDomain && selection.includeElectricityRates)) {
            meterDao.getAllElectricityRatesSync().filter { !it.isDeleted }
        } else emptyList()

        val stickyNotes = if (full || selection.includeStickyDomain) {
            val allNotes = stickyNoteDao.getAllStickyNotesSync().filter { !it.isDeleted }
            when {
                full -> allNotes
                selection.selectedStickyNoteIds.isNotEmpty() -> allNotes.filter { it.id in selection.selectedStickyNoteIds }
                selection.selectedStickyTags.isNotEmpty() -> {
                    val lowerTags = selection.selectedStickyTags.map { it.trim().lowercase(Locale.ROOT) }.toSet()
                    allNotes.filter { note ->
                        val noteTags = note.tags.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
                        noteTags.any { it in lowerTags }
                    }
                }
                else -> allNotes
            }
        } else emptyList()

        val wantProfileIdentity = full || selection.includeProfileDomain
        val wantOwnerAccounts = full || selection.includeOwnerAccounts || selection.includeProfileDomain
        val ownerAccounts = if (wantOwnerAccounts) {
            ownerAccountDao.getAllOwnerAccountsSync().filter { !it.isDeleted }
        } else emptyList()

        val wantSettings = full || selection.includeSettingsDomain
        val profile = userProfileDao.getUserProfileSync()

        ResolvedExportData(
            hotels = scopedHotels,
            exportFullHotels = wantHotelInfo,
            menus = menus,
            transactions = transactions,
            hotelPayments = hotelPayments,
            bankAccounts = scopedBanks,
            exportFullBankAccounts = wantBankInfo,
            expenses = expenses,
            readings = readings,
            utilityBills = bills,
            electricityRates = rates,
            stickyNotes = stickyNotes,
            ownerAccounts = ownerAccounts,
            userProfile = profile,
            includeProfileIdentity = wantProfileIdentity,
            includeAppSettings = wantSettings
        )
    }

    suspend fun resolveEntitiesForQr(
        selection: ExportSelection
    ): Triple<List<HotelEntity>, List<BankAccountEntity>, List<OwnerAccountEntity>> = withContext(Dispatchers.IO) {
        val data = resolveData(selection)
        Triple(
            if (data.exportFullHotels) data.hotels else emptyList(),
            if (data.exportFullBankAccounts) data.bankAccounts else emptyList(),
            data.ownerAccounts
        )
    }

    suspend fun exportJson(selection: ExportSelection, includeQrMedia: Boolean = true): String = withContext(Dispatchers.IO) {
        val data = resolveData(selection)
        val ctx = AppDatabase.getAppContext()
        val root = JSONObject().apply {
            put("version", 4)
            put("dataType", if (selection.isFullBackup) BackupDataType.FULL_BACKUP.name else "CUSTOM_SELECTION")
            put("exportDate", System.currentTimeMillis())
            if (!data.userProfile?.name.isNullOrBlank()) {
                put("senderName", data.userProfile?.name)
            }

            if ((data.includeProfileIdentity || data.includeAppSettings) && data.userProfile != null) {
                val p = data.userProfile
                put("userProfile", JSONObject().apply {
                    put("partialProfileOnly", data.includeProfileIdentity && !data.includeAppSettings)
                    put("partialSettingsOnly", data.includeAppSettings && !data.includeProfileIdentity)
                    if (data.includeProfileIdentity) {
                        put("name", p.name)
                        put("contact", p.contact)
                        put("avatar", p.avatar)
                    }
                    if (data.includeAppSettings) {
                        put("currencySymbol", p.currencySymbol)
                        put("language", p.language)
                        put("themeMode", p.themeMode)
                        put("enableSupplementaryAddons", p.enableSupplementaryAddons)
                        put("bgPreset", p.bgPreset)
                        put("enableLiquidGlassTheme", p.enableLiquidGlassTheme)
                        put("disableHeavyEffects", p.disableHeavyEffects)
                        put("mealGroupPosition", p.mealGroupPosition)
                        put("textSize", p.textSize)
                        put("isBoldTextEnabled", p.isBoldTextEnabled)
                        put("stickyCardWidthPercent", p.stickyCardWidthPercent)
                        put("stickyPreviewLines", p.stickyPreviewLines)
                        put("appVersion", p.appVersion)
                        put("defaultHotelId", p.defaultHotelId)
                        put("defaultBankAccountId", p.defaultBankAccountId)
                    }
                })
            }

            // Hotels (full profiles OR lightweight parent reference stubs when only menus/orders exported)
            if (data.hotels.isNotEmpty() && (data.exportFullHotels || data.menus.isNotEmpty() || data.transactions.isNotEmpty() || data.hotelPayments.isNotEmpty())) {
                val hotelsArr = JSONArray()
                data.hotels.forEach { h ->
                    hotelsArr.put(JSONObject().apply {
                        put("id", h.id)
                        put("name", h.name)
                        if (!data.exportFullHotels) {
                            put("isStub", true)
                        } else {
                            put("advancedPayment", h.advancedPayment)
                            put("phoneNumber", h.phoneNumber)
                            put("location", h.location)
                            put("upiId", h.upiId)
                            put("qrCodeUri", h.qrCodeUri)
                            QrMediaCodec.appendQrDataToJson(this, ctx, h.qrCodeUri, includeQrPayloads = includeQrMedia)
                            put("notes", h.notes)
                            put("whatsappNumber", h.whatsappNumber)
                        }
                    })
                }
                put("hotels", hotelsArr)
            }

            if (data.menus.isNotEmpty()) {
                val arr = JSONArray()
                data.menus.forEach { m ->
                    arr.put(JSONObject().apply {
                        put("hotelId", m.hotelId)
                        put("banglaName", m.banglaName)
                        put("englishName", m.englishName)
                        put("price", m.price)
                        put("isDayMenu", m.isDayMenu)
                        put("isNightMenu", m.isNightMenu)
                        put("isAddOn", m.isAddOn)
                        put("emoji", m.emoji)
                        put("type", m.type)
                        if (!m.imageUri.isNullOrBlank()) put("imageUri", m.imageUri)
                    })
                }
                put("menus", arr)
            }

            if (data.transactions.isNotEmpty()) {
                val arr = JSONArray()
                data.transactions.forEach { t ->
                    arr.put(JSONObject().apply {
                        put("id", t.id)
                        put("hotelId", t.hotelId)
                        put("timestamp", t.timestamp)
                        put("isNight", t.isNight)
                        put("totalAmount", t.totalAmount)
                        put("itemsSerialized", t.itemsSerialized)
                    })
                }
                put("transactions", arr)
            }

            if (data.hotelPayments.isNotEmpty()) {
                val arr = JSONArray()
                data.hotelPayments.forEach { p ->
                    arr.put(JSONObject().apply {
                        put("hotelId", p.hotelId)
                        put("year", p.year)
                        put("month", p.month)
                        put("amountPaid", p.amountPaid)
                        put("paymentDate", p.paymentDate)
                        put("note", p.note)
                    })
                }
                put("hotelPayments", arr)
            }

            // Bank Accounts (full profiles OR lightweight parent reference stubs when only transactions exported)
            if (data.bankAccounts.isNotEmpty() && (data.exportFullBankAccounts || data.expenses.isNotEmpty())) {
                val arr = JSONArray()
                data.bankAccounts.forEach { b ->
                    arr.put(JSONObject().apply {
                        put("id", b.id)
                        put("name", b.name)
                        if (!data.exportFullBankAccounts) {
                            put("isStub", true)
                        } else {
                            put("initialBalance", b.initialBalance)
                            put("balance", b.balance)
                            put("details", b.details)
                            put("qrCodeUri", b.qrCodeUri)
                            QrMediaCodec.appendQrDataToJson(this, ctx, b.qrCodeUri, includeQrPayloads = includeQrMedia)
                            put("upiId", b.upiId)
                            put("accountNumber", b.accountNumber)
                            put("ifsc", b.ifsc)
                            put("branchName", b.branchName)
                            put("notes", b.notes)
                            put("phoneNumber", b.phoneNumber)
                            put("whatsappNumber", b.whatsappNumber)
                            put("email", b.email)
                        }
                    })
                }
                put("bankAccounts", arr)
            }

            if (data.expenses.isNotEmpty()) {
                val arr = JSONArray()
                data.expenses.forEach { e ->
                    arr.put(JSONObject().apply {
                        put("timestamp", e.timestamp)
                        put("name", e.name)
                        put("amount", e.amount)
                        put("bankAccountId", e.bankAccountId)
                        put("type", e.type)
                        put("category", e.category)
                        put("notes", e.notes)
                        if (!e.receiptUri.isNullOrBlank()) put("receiptUri", e.receiptUri)
                    })
                }
                put("expenses", arr)
            }

            if (data.ownerAccounts.isNotEmpty()) {
                val arr = JSONArray()
                data.ownerAccounts.forEach { oa ->
                    arr.put(JSONObject().apply {
                        put("id", oa.id)
                        put("ownerName", oa.ownerName)
                        put("ownerMobile", oa.ownerMobile)
                        put("accountName", oa.accountName)
                        put("accountNumber", oa.accountNumber)
                        put("ifscCode", oa.ifscCode)
                        put("branchName", oa.branchName)
                        put("upiId", oa.upiId)
                        put("qrCodeUri", oa.qrCodeUri)
                        QrMediaCodec.appendQrDataToJson(this, ctx, oa.qrCodeUri, includeQrPayloads = includeQrMedia)
                        put("details", oa.details)
                        put("notes", oa.notes)
                        put("whatsappNumber", oa.whatsappNumber)
                        put("email", oa.email)
                        if (oa.linkedBankAccountId != null) put("linkedBankAccountId", oa.linkedBankAccountId)
                    })
                }
                put("ownerAccounts", arr)
            }

            if (data.stickyNotes.isNotEmpty()) {
                val arr = JSONArray()
                data.stickyNotes.forEach { sn ->
                    arr.put(JSONObject().apply {
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
                put("stickyNotes", arr)
            }

            if (data.readings.isNotEmpty()) {
                val arr = JSONArray()
                data.readings.forEach { r ->
                    arr.put(JSONObject().apply {
                        put("id", r.id)
                        put("readingDate", r.readingDate)
                        put("totalReading", r.totalReading)
                        put("unitsConsumed", r.unitsConsumed)
                        put("isPaid", r.isPaid)
                    })
                }
                put("meterReadings", arr)
            }

            if (data.utilityBills.isNotEmpty()) {
                val arr = JSONArray()
                data.utilityBills.forEach { b ->
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
            }

            if (data.electricityRates.isNotEmpty()) {
                val arr = JSONArray()
                data.electricityRates.forEach { r ->
                    arr.put(JSONObject().apply {
                        put("ratePerUnit", r.ratePerUnit)
                        put("effectiveDate", r.effectiveDate)
                    })
                }
                put("electricityRates", arr)
            }
        }
        root.toString()
    }

    /**
     * Returns ONLY the local media files referenced by the entities inside [selection],
     * mapped to their archive entry names (`attachments/...`, `qr_codes/...`, `root_files/...`).
     */
    suspend fun collectScopedMediaEntries(context: Context, selection: ExportSelection): List<Pair<String, File>> = withContext(Dispatchers.IO) {
        val data = resolveData(selection)
        val rawUriStrings = mutableSetOf<String>()

        if (data.exportFullHotels) {
            data.hotels.forEach { h ->
                h.qrCodeUri.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { rawUriStrings.add(it) }
            }
        }
        data.menus.forEach { m ->
            if (!m.imageUri.isNullOrBlank()) rawUriStrings.add(m.imageUri)
        }
        if (data.exportFullBankAccounts) {
            data.bankAccounts.forEach { b ->
                b.qrCodeUri.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { rawUriStrings.add(it) }
            }
        }
        if (data.expenses.isNotEmpty()) {
            val expIds = data.expenses.map { it.id }.toSet()
            data.expenses.forEach { e ->
                if (!e.receiptUri.isNullOrBlank()) rawUriStrings.add(e.receiptUri)
            }
            attachmentDao.getAllAttachmentsSync()
                .filter { it.expenseId in expIds && it.uri.isNotBlank() }
                .forEach { rawUriStrings.add(it.uri) }
        }
        data.ownerAccounts.forEach { oa ->
            oa.qrCodeUri.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { rawUriStrings.add(it) }
        }
        data.stickyNotes.forEach { sn ->
            if (sn.attachmentsJson.isNotBlank() && sn.attachmentsJson != "[]") {
                runCatching {
                    val arr = JSONArray(sn.attachmentsJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        val uriStr = obj?.optString("uri", "") ?: arr.optString(i, "")
                        if (uriStr.isNotBlank()) rawUriStrings.add(uriStr)
                    }
                }
            }
        }
        if (data.includeProfileIdentity && data.userProfile != null) {
            if (data.userProfile.avatar.isNotBlank()) rawUriStrings.add(data.userProfile.avatar)
        }
        if (data.includeAppSettings && data.userProfile != null) {
            if (data.userProfile.customBgUri.isNotBlank()) rawUriStrings.add(data.userProfile.customBgUri)
        }

        val filesDir = context.filesDir
        val attachmentsDir = File(filesDir, "attachments")
        val qrCodesDir = File(filesDir, "qr_codes")
        val seenEntries = mutableSetOf<String>()
        val result = mutableListOf<Pair<String, File>>()

        fun addFile(file: File) {
            if (!file.exists() || !file.isFile || !file.canRead()) return
            val absPath = file.absolutePath
            val entryName = when {
                absPath.startsWith(attachmentsDir.absolutePath) -> "attachments/${file.name}"
                absPath.startsWith(qrCodesDir.absolutePath) -> "qr_codes/${file.name}"
                else -> "root_files/${file.name}"
            }
            if (seenEntries.add(entryName)) {
                result.add(entryName to file)
            }
        }

        rawUriStrings.forEach { raw ->
            val path = if (raw.startsWith("file://")) Uri.parse(raw).path else raw
            if (!path.isNullOrBlank()) {
                val directFile = File(path)
                if (directFile.exists() && directFile.isFile) {
                    addFile(directFile)
                } else {
                    // Fallback by filename lookup inside app directories
                    val name = directFile.name
                    if (name.isNotBlank()) {
                        listOf(File(attachmentsDir, name), File(qrCodesDir, name), File(filesDir, name))
                            .firstOrNull { it.exists() && it.isFile }
                            ?.let { addFile(it) }
                    }
                }
            }
        }
        result
    }

    /**
     * Generates a clean, Excel-friendly CSV snapshot for the selected domains/items (export-only).
     */
    suspend fun exportCsv(selection: ExportSelection): String = withContext(Dispatchers.IO) {
        val data = resolveData(selection)
        val dateOnlyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeOnlyFmt = SimpleDateFormat("HH:mm", Locale.US)
        val sb = StringBuilder()

        fun esc(s: String): String = s.replace("\"", "\"\"").replace("\n", " ").replace("\r", "")

        val hotelNameMap = data.hotels.associate { it.id to it.name }
        val bankNameMap = data.bankAccounts.associate { it.id to it.name }

        if (data.exportFullHotels && data.hotels.isNotEmpty()) {
            sb.append("# HOTELS\n")
            sb.append("Hotel ID,Hotel Name,Advance Payment,Phone,WhatsApp,Location,UPI ID,Notes\n")
            data.hotels.forEach { h ->
                sb.append("${h.id},\"${esc(h.name)}\",${h.advancedPayment},\"${esc(h.phoneNumber)}\",\"${esc(h.whatsappNumber)}\",\"${esc(h.location)}\",\"${esc(h.upiId)}\",\"${esc(h.notes)}\"\n")
            }
            sb.append("\n")
        }

        if (data.menus.isNotEmpty()) {
            sb.append("# MENU ITEMS\n")
            sb.append("Hotel,Bangla Name,English Name,Price,Day,Night,Add-on,Category\n")
            data.menus.forEach { m ->
                val hName = hotelNameMap[m.hotelId] ?: "Hotel #${m.hotelId}"
                sb.append("\"${esc(hName)}\",\"${esc(m.banglaName)}\",\"${esc(m.englishName)}\",${m.price},${if (m.isDayMenu) "Yes" else "No"},${if (m.isNightMenu) "Yes" else "No"},${if (m.isAddOn) "Yes" else "No"},\"${esc(m.type)}\"\n")
            }
            sb.append("\n")
        }

        if (data.transactions.isNotEmpty()) {
            val dtm = DataTransferManager(db)
            sb.append("# HOTEL MEAL ORDERS\n")
            sb.append("Hotel,Date,Time,Session,Items,Order Total\n")
            data.transactions.forEach { t ->
                val hName = hotelNameMap[t.hotelId] ?: "Hotel #${t.hotelId}"
                val itemsStr = dtm.formatMealGroupsCompact(t.itemsSerialized)
                sb.append("\"${esc(hName)}\",\"${dateOnlyFmt.format(Date(t.timestamp))}\",\"${timeOnlyFmt.format(Date(t.timestamp))}\",\"${if (t.isNight) "Night" else "Day"}\",\"${esc(itemsStr)}\",${t.totalAmount}\n")
            }
            sb.append("\n")
        }

        if (data.hotelPayments.isNotEmpty()) {
            sb.append("# HOTEL MONTHLY PAYMENTS\n")
            sb.append("Hotel,Year,Month,Amount Paid,Payment Date,Note\n")
            data.hotelPayments.forEach { p ->
                val hName = hotelNameMap[p.hotelId] ?: "Hotel #${p.hotelId}"
                sb.append("\"${esc(hName)}\",${p.year},${p.month + 1},${p.amountPaid},\"${dateOnlyFmt.format(Date(p.paymentDate))}\",\"${esc(p.note)}\"\n")
            }
            sb.append("\n")
        }

        if (data.exportFullBankAccounts && data.bankAccounts.isNotEmpty()) {
            sb.append("# BANK ACCOUNTS\n")
            sb.append("Account ID,Account Name,Initial Balance,Current Balance,UPI ID,Account Number,IFSC,Branch,Phone,Email\n")
            data.bankAccounts.forEach { b ->
                sb.append("${b.id},\"${esc(b.name)}\",${b.initialBalance},${b.balance},\"${esc(b.upiId)}\",\"${esc(b.accountNumber)}\",\"${esc(b.ifsc)}\",\"${esc(b.branchName)}\",\"${esc(b.phoneNumber)}\",\"${esc(b.email)}\"\n")
            }
            sb.append("\n")
        }

        if (data.expenses.isNotEmpty()) {
            sb.append("# EXTRA TRANSACTIONS\n")
            sb.append("Account,Date,Time,Type,Category,Title,Amount,Notes\n")
            data.expenses.forEach { e ->
                val bName = bankNameMap[e.bankAccountId] ?: "Account #${e.bankAccountId}"
                sb.append("\"${esc(bName)}\",\"${dateOnlyFmt.format(Date(e.timestamp))}\",\"${timeOnlyFmt.format(Date(e.timestamp))}\",\"${esc(e.type)}\",\"${esc(e.category)}\",\"${esc(e.name)}\",${e.amount},\"${esc(e.notes)}\"\n")
            }
            sb.append("\n")
        }

        if (data.readings.isNotEmpty()) {
            sb.append("# ELECTRICITY METER READINGS\n")
            sb.append("Date,Total Reading (kWh),Units Consumed,Status\n")
            data.readings.forEach { r ->
                sb.append("\"${dateOnlyFmt.format(Date(r.readingDate))}\",${r.totalReading},${r.unitsConsumed},\"${if (r.isPaid) "Paid" else "Unpaid"}\"\n")
            }
            sb.append("\n")
        }

        if (data.utilityBills.isNotEmpty()) {
            sb.append("# ELECTRICITY BILLS\n")
            sb.append("Start Date,End Date,Total Units,Amount,Status,Transaction ID,Note\n")
            data.utilityBills.forEach { b ->
                sb.append("\"${dateOnlyFmt.format(Date(b.startDate))}\",\"${dateOnlyFmt.format(Date(b.endDate))}\",${b.totalUnits},${b.calculatedAmount},\"${if (b.isPaid) "Paid" else "Unpaid"}\",\"${esc(b.transactionId ?: "")}\",\"${esc(b.note)}\"\n")
            }
            sb.append("\n")
        }

        if (data.electricityRates.isNotEmpty()) {
            sb.append("# ELECTRICITY RATES\n")
            sb.append("Effective Date,Rate Per Unit\n")
            data.electricityRates.forEach { r ->
                sb.append("\"${dateOnlyFmt.format(Date(r.effectiveDate))}\",${r.ratePerUnit}\n")
            }
            sb.append("\n")
        }

        if (data.stickyNotes.isNotEmpty()) {
            sb.append("# STICKY NOTES\n")
            sb.append("Title,Subtitle,Tags,Content,Updated Date\n")
            data.stickyNotes.forEach { n ->
                sb.append("\"${esc(n.title)}\",\"${esc(n.subtitle)}\",\"${esc(n.tags)}\",\"${esc(n.bodyText)}\",\"${dateOnlyFmt.format(Date(n.updatedAt))}\"\n")
            }
            sb.append("\n")
        }

        if (data.ownerAccounts.isNotEmpty()) {
            sb.append("# OWNER ACCOUNTS\n")
            sb.append("Owner Name,Account Name,Mobile,Account Number,IFSC,Branch,UPI ID,Email\n")
            data.ownerAccounts.forEach { o ->
                sb.append("\"${esc(o.ownerName)}\",\"${esc(o.accountName)}\",\"${esc(o.ownerMobile)}\",\"${esc(o.accountNumber)}\",\"${esc(o.ifscCode)}\",\"${esc(o.branchName)}\",\"${esc(o.upiId)}\",\"${esc(o.email)}\"\n")
            }
            sb.append("\n")
        }

        if ((data.includeProfileIdentity || data.includeAppSettings) && data.userProfile != null) {
            val p = data.userProfile
            sb.append("# PROFILE & SETTINGS\n")
            sb.append("Name,Contact,Currency,Language,Theme Mode,Text Size,Bold Text\n")
            sb.append("\"${esc(p.name)}\",\"${esc(p.contact)}\",\"${esc(p.currencySymbol)}\",\"${esc(p.language)}\",\"${esc(p.themeMode)}\",\"${esc(p.textSize)}\",${p.isBoldTextEnabled}\n")
        }

        sb.toString().ifBlank { "# No data in current selection\n" }
    }
}
