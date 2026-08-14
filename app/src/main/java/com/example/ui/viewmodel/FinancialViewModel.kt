package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.BankAccountEntity
import com.example.data.model.ExtraExpenseEntity
import com.example.data.model.HotelEntity
import com.example.data.model.HotelTransactionEntity
import com.example.data.model.MenuEntity
import com.example.data.model.OrderedItem
import com.example.data.model.TransactionSerializer
import com.example.data.model.UserProfileEntity
import com.example.data.repository.FinancialRepository
import com.example.data.model.SearchResult
import com.example.data.model.MeterReading
import com.example.data.model.UtilityBill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

private data class SearchCombineTuple5(
    val query: String,
    val filter: String,
    val hotels: List<HotelEntity>,
    val expenses: List<ExtraExpenseEntity>,
    val accounts: List<BankAccountEntity>
)

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinancialRepository

    val isInitialized = MutableStateFlow(false)

    // UI Tab State
    val activeTab = MutableStateFlow("Hotels")
    val isBottomBarVisible = MutableStateFlow(true)
    private val activeSubScreens = MutableStateFlow<Set<String>>(emptySet())
    val quickActionTrigger = MutableStateFlow<String?>(null)

    fun triggerQuickAction(action: String) {
        when (action) {
            "ADD_EXPENSE" -> {
                activeTab.value = "Extras"
                quickActionTrigger.value = "ADD_EXPENSE"
            }
            "ADD_MEAL_LOG", "ADD_HOTEL_TX" -> {
                activeTab.value = "Hotels"
                quickActionTrigger.value = "ADD_MEAL_LOG"
            }
            "ADD_HOTEL" -> {
                activeTab.value = "Hotels"
                quickActionTrigger.value = "ADD_HOTEL"
            }
            "ADD_ACCOUNT" -> {
                activeTab.value = "Extras"
                quickActionTrigger.value = "ADD_ACCOUNT"
            }
        }
    }

    fun clearQuickActionTrigger() {
        quickActionTrigger.value = null
    }

    fun registerSubScreen(key: String, isActive: Boolean) {
        val current = activeSubScreens.value.toMutableSet()
        if (isActive) {
            current.add(key)
        } else {
            current.remove(key)
        }
        activeSubScreens.value = current
        isBottomBarVisible.value = current.isEmpty()
    }

    // Active Hotel Selection
    val activeHotelId = MutableStateFlow(1)
    val activeBankAccountId = MutableStateFlow<Int?>(null)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinancialRepository(db)
        viewModelScope.launch {
            repository.initializeDatabaseIfNeeded(application)
            isInitialized.value = true
        }
        viewModelScope.launch {
            repository.allHotels.collect { updatedHotels ->
                if (updatedHotels.isNotEmpty() && updatedHotels.none { it.id == activeHotelId.value }) {
                    activeHotelId.value = updatedHotels.first().id
                }
            }
        }
        viewModelScope.launch {
            repository.allBankAccounts.collect { accounts ->
                if (accounts.isNotEmpty() && activeBankAccountId.value == null) {
                    activeBankAccountId.value = accounts.first().id
                }
            }
        }
    }

    // Streams from Repository
    val userProfile: StateFlow<UserProfileEntity> = repository.userProfile
        .map { it ?: UserProfileEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfileEntity())

    val allHotels: StateFlow<List<HotelEntity>> = repository.allHotels
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedHotels: StateFlow<List<HotelEntity>> = repository.deletedHotels
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allBankAccounts: StateFlow<List<BankAccountEntity>> = repository.allBankAccounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTransactions: StateFlow<List<HotelTransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allMeterReadings: StateFlow<List<MeterReading>> = repository.allMeterReadings
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allUtilityBills: StateFlow<List<UtilityBill>> = repository.allUtilityBills
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedBankAccounts: StateFlow<List<BankAccountEntity>> = repository.deletedBankAccounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedTransactions: StateFlow<List<HotelTransactionEntity>> = repository.deletedTransactions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedExtraExpenses: StateFlow<List<ExtraExpenseEntity>> = repository.deletedExtraExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedMeterReadings: StateFlow<List<MeterReading>> = repository.deletedMeterReadings
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val deletedUtilityBills: StateFlow<List<UtilityBill>> = repository.deletedUtilityBills
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allExtraExpenses: StateFlow<List<ExtraExpenseEntity>> = repository.allExtraExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCategories: StateFlow<List<com.example.data.model.TransactionCategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allOwnerAccounts: StateFlow<List<com.example.data.model.OwnerAccountEntity>> = repository.allOwnerAccounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun getAttachmentsForExpense(expenseId: Int): Flow<List<com.example.data.model.AttachmentEntity>> {
        return repository.getAttachmentsForExpense(expenseId)
    }

    fun addOwnerAccount(account: com.example.data.model.OwnerAccountEntity) {
        viewModelScope.launch { repository.insertOwnerAccount(account) }
    }

    fun updateOwnerAccount(account: com.example.data.model.OwnerAccountEntity) {
        viewModelScope.launch { repository.updateOwnerAccount(account) }
    }

    fun deleteOwnerAccount(account: com.example.data.model.OwnerAccountEntity) {
        viewModelScope.launch { repository.deleteOwnerAccount(account) }
    }

    // Active Hotel Stream
    val activeHotel: StateFlow<HotelEntity?> = combine(allHotels, activeHotelId) { hotels, selectedId ->
        hotels.find { it.id == selectedId } ?: hotels.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allMenuItems: StateFlow<List<MenuEntity>> = activeHotel
        .flatMapLatest { hotel ->
            if (hotel != null) repository.getMenuForHotel(hotel.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedMenuItems: StateFlow<List<MenuEntity>> = activeHotel
        .flatMapLatest { hotel ->
            if (hotel != null) repository.getDeletedMenuForHotel(hotel.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Active Hotel Transactions
    val activeHotelTransactions: StateFlow<List<HotelTransactionEntity>> = activeHotel
        .flatMapLatest { hotel ->
            if (hotel != null) repository.getTransactionsForHotel(hotel.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val earliestTransactionTimestamp: StateFlow<Long> = activeHotelTransactions
        .map { txList -> txList.minOfOrNull { it.timestamp } ?: System.currentTimeMillis() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, System.currentTimeMillis())

    // Active Month total expenditure for active hotel
    val activeMonthTotal: StateFlow<Double> = activeHotelTransactions
        .map { txList ->
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            txList.filter { tx ->
                calendar.timeInMillis = tx.timestamp
                calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
            }.sumOf { it.totalAmount }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Behavioral Analytics Stream: Frequently ordered dishes
    val popularMeals: StateFlow<List<Pair<String, Int>>> = activeHotelTransactions
        .map { txList ->
            val counts = mutableMapOf<String, Int>()
            txList.forEach { tx ->
                val items = TransactionSerializer.deserialize(tx.itemsSerialized)
                items.forEach { item ->
                    counts[item.name] = (counts[item.name] ?: 0) + item.quantity
                }
            }
            counts.toList().sortedByDescending { it.second }.take(4)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    // ==========================================
    // ADD MEAL FLOW (STATE & CONTROLS)
    // ==========================================
    private val _addMealDate = MutableStateFlow(System.currentTimeMillis())
    val addMealDate = _addMealDate.asStateFlow()
    fun setAddMealDate(date: Long) {
        _addMealDate.value = date
    }


    private val _addMealIsNight = MutableStateFlow(false)
    val addMealIsNight = _addMealIsNight.asStateFlow()

    // Map of Menu Item ID to quantity
    private val _orderQuantities = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val orderQuantities = _orderQuantities.asStateFlow()

    // Highlighted main dish to render its add-on tray below it
    private val _selectedMainDishId = MutableStateFlow<Int?>(null)
    val selectedMainDishId = _selectedMainDishId.asStateFlow()

    // Committed meal groups for multi-person / multi-meal transactions
    private val _committedMealGroups = MutableStateFlow<List<com.example.data.model.MealGroup>>(emptyList())
    val committedMealGroups = _committedMealGroups.asStateFlow()

    // Active editing transaction ID if modifying an existing meal ledger entry
    private val _editingTransactionId = MutableStateFlow<Int?>(null)
    val editingTransactionId = _editingTransactionId.asStateFlow()

    fun resetOrderFlow() {
        _addMealDate.value = System.currentTimeMillis()
        // Auto-detect Day/Night from system clock
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        _addMealIsNight.value = hour < 6 || hour >= 18
        _orderQuantities.value = emptyMap()
        _selectedMainDishId.value = null
        _committedMealGroups.value = emptyList()
        _editingTransactionId.value = null
    }

    fun setOrderDate(timestamp: Long) {
        _addMealDate.value = timestamp
    }

    fun toggleDayNight() {
        _addMealIsNight.value = !_addMealIsNight.value
        // Reset draft quantities when switching menu modes, as available dishes change
        _orderQuantities.value = emptyMap()
        _selectedMainDishId.value = null
    }

    fun selectMainDish(menuId: Int) {
        _selectedMainDishId.value = menuId
        // Automatically increment quantity by 1 if not ordered yet
        val currentQty = _orderQuantities.value[menuId] ?: 0
        if (currentQty == 0) {
            updateItemQuantity(menuId, 1)
        }
    }

    fun updateItemQuantity(menuId: Int, delta: Int) {
        val current = _orderQuantities.value.toMutableMap()
        val qty = (current[menuId] ?: 0) + delta
        if (qty <= 0) {
            current.remove(menuId)
            // If the deleted item was the active main dish, clear focus
            if (_selectedMainDishId.value == menuId) {
                _selectedMainDishId.value = null
            }
        } else {
            current[menuId] = qty
        }
        _orderQuantities.value = current
    }

    fun commitCurrentSelectionGroup(): Boolean {
        val menuList = allMenuItems.value
        val orderedItems = _orderQuantities.value.mapNotNull { (menuId, qty) ->
            val menu = menuList.find { it.id == menuId }
            if (menu != null && qty > 0) {
                OrderedItem(
                    name = menu.englishName,
                    quantity = qty,
                    price = menu.price
                )
            } else null
        }
        if (orderedItems.isEmpty()) return false

        val newGroup = com.example.data.model.MealGroup(items = orderedItems)
        _committedMealGroups.value = _committedMealGroups.value + newGroup
        // Clear draft selection for next group entry
        _orderQuantities.value = emptyMap()
        _selectedMainDishId.value = null
        return true
    }

    fun removeMealGroup(groupId: String) {
        _committedMealGroups.value = _committedMealGroups.value.filter { it.id != groupId }
    }

    fun loadTransactionForEditing(transaction: HotelTransactionEntity) {
        _editingTransactionId.value = transaction.id
        _addMealDate.value = transaction.timestamp
        _addMealIsNight.value = transaction.isNight
        _committedMealGroups.value = TransactionSerializer.deserializeGroups(transaction.itemsSerialized)
        _orderQuantities.value = emptyMap()
        _selectedMainDishId.value = null
    }

    private fun updateWidgets() {
        com.example.widget.WidgetUpdater.updateAllWidgets(getApplication())
    }

    fun saveHotelTransaction() {
        val targetHotel = activeHotel.value
        val hotelId = targetHotel?.id ?: activeHotelId.value
        val isNight = _addMealIsNight.value
        val timestamp = _addMealDate.value
        
        // Auto-commit current dish selection if uncommitted
        if (_orderQuantities.value.values.any { it > 0 }) {
            commitCurrentSelectionGroup()
        }
        
        val groupsToSave = _committedMealGroups.value
        if (groupsToSave.isEmpty()) return

        val total = groupsToSave.sumOf { it.subtotal }
        val serializedData = TransactionSerializer.serializeGroups(groupsToSave)
        val editId = _editingTransactionId.value

        viewModelScope.launch {
            if (editId != null) {
                val existingTx = activeHotelTransactions.value.find { it.id == editId }
                if (existingTx != null) {
                    repository.updateTransaction(
                        existingTx.copy(
                            hotelId = hotelId,
                            timestamp = timestamp,
                            isNight = isNight,
                            itemsSerialized = serializedData,
                            totalAmount = total
                        )
                    )
                }
            } else {
                val transaction = HotelTransactionEntity(
                    hotelId = hotelId,
                    timestamp = timestamp,
                    isNight = isNight,
                    itemsSerialized = serializedData,
                    totalAmount = total
                )
                repository.insertTransaction(transaction)
            }
            resetOrderFlow()
            updateWidgets()
        }
    }

    fun deleteTransaction(transaction: HotelTransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction.copy(isDeleted = true))
            updateWidgets()
        }
    }

    fun restoreTransaction(transaction: HotelTransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction.copy(isDeleted = false))
            updateWidgets()
        }
    }

    fun deleteTransactionPermanently(transaction: HotelTransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            updateWidgets()
        }
    }

    // ==========================================
    // EXTRA EXPENSE FLOW (STATE & CONTROLS)
    // ==========================================
    fun saveExtraExpense(
        name: String,
        amount: Double,
        bankAccountId: Int,
        timestamp: Long,
        receiptUri: String?,
        type: String = "EXPENSE",
        category: String = "General",
        notes: String = "",
        attachments: List<Pair<String, String>> = emptyList() // uri to type
    ) {
        val accounts = allBankAccounts.value
        val selectedAccount = accounts.find { it.id == bankAccountId } ?: return

        viewModelScope.launch {
            val expense = ExtraExpenseEntity(
                timestamp = timestamp,
                name = name,
                amount = amount,
                bankAccountId = bankAccountId,
                receiptUri = receiptUri,
                type = type,
                category = category,
                notes = notes
            )
            val id = repository.insertExpenseAndDeduct(expense, selectedAccount)
            
            attachments.forEach { (uriString, type) ->
                val secureUri = copyToInternalStorage(uriString, type)
                repository.insertAttachment(
                    com.example.data.model.AttachmentEntity(
                        expenseId = id.toInt(),
                        uri = secureUri,
                        type = type
                    )
                )
            }
            updateWidgets()
        }
    }

    private fun copyToInternalStorage(uriString: String, type: String): String {
        if (uriString.startsWith("file:///android_asset/") || uriString.startsWith("content://")) {
            try {
                val context = getApplication<Application>()
                val uri = android.net.Uri.parse(uriString)
                val extension = when(type) {
                    "IMAGE" -> "jpg"
                    "PDF" -> "pdf"
                    "VOICE" -> "amr"
                    else -> "dat"
                }
                val fileName = "secure_${System.currentTimeMillis()}_${(0..1000).random()}.$extension"
                val file = java.io.File(context.filesDir, "attachments/$fileName")
                file.parentFile?.mkdirs()
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return android.net.Uri.fromFile(file).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return uriString
    }

    // Import/Export QR methods
    fun exportHotelViaQr(hotelId: Int, onResult: (com.example.data.ExportResult) -> Unit) {
        viewModelScope.launch {
            val db = com.example.data.AppDatabase.getDatabase(getApplication())
            val manager = com.example.data.QrExportManager(db.hotelDao(), db.menuDao(), db.transactionDao(), db)
            val result = manager.exportWholeHotel(hotelId)
            onResult(result)
        }
    }

    fun importHotelViaQr(payload: ByteArray, key: ByteArray, iv: ByteArray, onResult: (com.example.data.ImportResult) -> Unit) {
        viewModelScope.launch {
            val db = com.example.data.AppDatabase.getDatabase(getApplication())
            val manager = com.example.data.QrImportManager(db.hotelDao(), db.menuDao(), db.transactionDao(), db)
            val result = manager.importPayload(payload, key, iv)
            if (result is com.example.data.ImportResult.Success && result.newHotelId > 0) {
                activeHotelId.value = result.newHotelId
            }
            onResult(result)
        }
    }

    fun exportViaQr(type: com.example.data.BackupDataType, hotelId: Int = 1, onResult: (com.example.data.ExportResult) -> Unit) {
        viewModelScope.launch {
            val db = com.example.data.AppDatabase.getDatabase(getApplication())
            val manager = com.example.data.QrExportManager(db.hotelDao(), db.menuDao(), db.transactionDao(), db)
            val result = when (type) {
                com.example.data.BackupDataType.HOTEL_WHOLE -> manager.exportWholeHotel(hotelId)
                com.example.data.BackupDataType.HOTEL_MENUS -> manager.exportHotelMenus(hotelId)
                com.example.data.BackupDataType.HOTEL_HISTORY -> manager.exportHotelHistory(hotelId)
                com.example.data.BackupDataType.BANK_ACCOUNTS -> manager.exportBankAccounts()
                com.example.data.BackupDataType.TRANSACTION_HISTORY -> manager.exportTransactionHistory()
                com.example.data.BackupDataType.METER_READINGS -> manager.exportMeterReadings()
                com.example.data.BackupDataType.UTILITY_BILLS -> manager.exportUtilityBills()
                com.example.data.BackupDataType.FULL_BACKUP -> manager.exportFullBackup()
            }
            onResult(result)
        }
    }

    fun exportDataJson(type: com.example.data.BackupDataType, hotelId: Int = 1, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val db = com.example.data.AppDatabase.getDatabase(getApplication())
                val dtm = com.example.data.DataTransferManager(db)
                val json = when (type) {
                    com.example.data.BackupDataType.HOTEL_WHOLE -> dtm.exportWholeHotelJson(hotelId)
                    com.example.data.BackupDataType.HOTEL_MENUS -> dtm.exportHotelMenusJson(hotelId)
                    com.example.data.BackupDataType.HOTEL_HISTORY -> dtm.exportHotelHistoryJson(hotelId)
                    com.example.data.BackupDataType.BANK_ACCOUNTS -> dtm.exportBankAccountsJson()
                    com.example.data.BackupDataType.TRANSACTION_HISTORY -> dtm.exportTransactionHistoryJson()
                    com.example.data.BackupDataType.METER_READINGS -> dtm.exportMeterReadingsJson()
                    com.example.data.BackupDataType.UTILITY_BILLS -> dtm.exportUtilityBillsJson()
                    com.example.data.BackupDataType.FULL_BACKUP -> dtm.exportFullBackupJson()
                }
                onResult(json)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult("{}")
            }
        }
    }

    fun exportDataCsv(type: com.example.data.BackupDataType, hotelId: Int = 1, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val db = com.example.data.AppDatabase.getDatabase(getApplication())
                val dtm = com.example.data.DataTransferManager(db)
                val csv = when (type) {
                    com.example.data.BackupDataType.HOTEL_WHOLE -> dtm.exportMenusCsv(hotelId)
                    com.example.data.BackupDataType.HOTEL_MENUS -> dtm.exportMenusCsv(hotelId)
                    com.example.data.BackupDataType.HOTEL_HISTORY -> dtm.exportHotelHistoryCsv(hotelId)
                    com.example.data.BackupDataType.BANK_ACCOUNTS -> dtm.exportBankAccountsCsv()
                    com.example.data.BackupDataType.TRANSACTION_HISTORY -> dtm.exportTransactionHistoryCsv()
                    com.example.data.BackupDataType.METER_READINGS -> dtm.exportMeterReadingsCsv()
                    com.example.data.BackupDataType.UTILITY_BILLS -> dtm.exportUtilityBillsCsv()
                    com.example.data.BackupDataType.FULL_BACKUP -> dtm.exportMenusCsv(null)
                }
                onResult(csv)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult("")
            }
        }
    }

    fun importUniversal(content: String, type: com.example.data.BackupDataType, hotelId: Int = 1, onResult: (com.example.data.ImportOutcome) -> Unit) {
        viewModelScope.launch {
            val db = com.example.data.AppDatabase.getDatabase(getApplication())
            val dtm = com.example.data.DataTransferManager(db)
            val outcome = dtm.importUniversalText(content, type, hotelId)
            onResult(outcome)
        }
    }


    private val _qrExportPayload = MutableStateFlow<com.example.data.ExportResult.Success?>(null)
    val qrExportPayload = _qrExportPayload.asStateFlow()

    private val _qrExportSessionId = MutableStateFlow<String?>(null)
    val qrExportSessionId = _qrExportSessionId.asStateFlow()

    fun setQrExportPayload(payload: com.example.data.ExportResult.Success?, sessionId: String?) {
        _qrExportPayload.value = payload
        _qrExportSessionId.value = sessionId
    }

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedSearchFilter = MutableStateFlow("All")
    val selectedSearchFilter = _selectedSearchFilter.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory = _searchHistory.asStateFlow()

    private val _searchResults = MutableStateFlow<List<com.example.data.model.SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()
    
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSearchFilter(filter: String) { _selectedSearchFilter.value = filter }
    fun addSearchHistory(query: String) {
        val list = _searchHistory.value.toMutableList()
        list.remove(query)
        list.add(0, query)
        _searchHistory.value = list.take(10)
    }
    fun clearSearchHistory() { _searchHistory.value = emptyList() }
    fun removeSearchHistoryItem(item: String) {
        _searchHistory.value = _searchHistory.value.filter { it != item }
    }

    // Lock State
    private val _isLockedManual = MutableStateFlow(false)
    val isLockedManual = _isLockedManual.asStateFlow()
    fun lockApp() { _isLockedManual.value = true }
    fun unlockApp() { _isLockedManual.value = false }


    // Standard CRUD
    fun addHotel(name: String, advancedPayment: Double, phoneNumber: String, location: String, upiId: String, qrCodeUri: String) = viewModelScope.launch { repository.insertHotel(com.example.data.model.HotelEntity(name = name, advancedPayment = advancedPayment, phoneNumber = phoneNumber, location = location, upiId = upiId, qrCodeUri = qrCodeUri)) }
    fun editHotel(hotel: com.example.data.model.HotelEntity) = viewModelScope.launch { repository.updateHotel(hotel) }
    fun deleteHotel(hotel: com.example.data.model.HotelEntity) = viewModelScope.launch { repository.deleteHotel(hotel) }
    fun addVirtualAccount(name: String, balance: Double, upiId: String, qrCodeUri: String, accountNumber: String, ifsc: String, branchName: String) = viewModelScope.launch { repository.insertBankAccount(com.example.data.model.BankAccountEntity(name = name, balance = balance, upiId = upiId, qrCodeUri = qrCodeUri, accountNumber = accountNumber, ifsc = ifsc, branchName = branchName)) }
    fun editVirtualAccount(account: com.example.data.model.BankAccountEntity) = viewModelScope.launch { repository.updateBankAccount(account) }
    fun deleteVirtualAccount(account: com.example.data.model.BankAccountEntity) = viewModelScope.launch { repository.deleteBankAccount(account) }
    fun addMenu(menu: com.example.data.model.MenuEntity) = viewModelScope.launch { repository.insertMenu(menu) }
    fun updateMenu(menu: com.example.data.model.MenuEntity) = viewModelScope.launch { repository.updateMenu(menu) }
    fun deleteMenu(menu: com.example.data.model.MenuEntity) = viewModelScope.launch { repository.deleteMenu(menu) }
    fun deleteExtraExpense(expense: com.example.data.model.ExtraExpenseEntity) = viewModelScope.launch { repository.softDeleteExpenseAndRevert(expense, allBankAccounts.value) }
    fun addCategory(name: String, type: String) = viewModelScope.launch { repository.insertCategory(com.example.data.model.TransactionCategoryEntity(name = name, type = type)) }

    // Bin Restore
    fun restoreHotel(item: com.example.data.model.HotelEntity) = viewModelScope.launch { repository.updateHotel(item.copy(isDeleted = false)) }
    fun restoreVirtualAccount(item: com.example.data.model.BankAccountEntity) = viewModelScope.launch { repository.updateBankAccount(item.copy(isDeleted = false)) }
    fun restoreMenu(item: com.example.data.model.MenuEntity) = viewModelScope.launch { repository.updateMenu(item.copy(isDeleted = false)) }
    fun restoreExtraExpense(item: com.example.data.model.ExtraExpenseEntity) = viewModelScope.launch { repository.restoreExpenseAndApply(item, allBankAccounts.value) }
    fun restoreMeterReading(item: com.example.data.model.MeterReading) = viewModelScope.launch { repository.restoreReading(item.id) }
    
    // Bin Hard Delete
    fun deleteHotelPermanently(item: com.example.data.model.HotelEntity) = viewModelScope.launch { repository.deleteHotel(item) } 
    fun deleteVirtualAccountPermanently(item: com.example.data.model.BankAccountEntity) = viewModelScope.launch { repository.deleteBankAccount(item) }
    fun deleteMenuPermanently(item: com.example.data.model.MenuEntity) = viewModelScope.launch { repository.deleteMenu(item) }
    fun deleteExtraExpensePermanently(item: com.example.data.model.ExtraExpenseEntity) = viewModelScope.launch { repository.deleteExpensePermanently(item) }
    fun deleteUtilityBillPermanently(item: com.example.data.model.UtilityBill) = viewModelScope.launch { repository.deleteUtilityBill(item) }
    fun deleteReading(item: com.example.data.model.MeterReading) = viewModelScope.launch { repository.deleteReading(item) }

    // Bin Empty All
    fun emptyDeletedHotelsPermanently() = viewModelScope.launch { }
    fun emptyDeletedBankAccountsPermanently() = viewModelScope.launch { }
    fun emptyDeletedMenuItemsPermanently() = viewModelScope.launch { }
    fun emptyDeletedTransactionsPermanently() = viewModelScope.launch { repository.emptyDeletedTransactionsPermanently() }
    fun emptyDeletedExpensesPermanently() = viewModelScope.launch { repository.emptyDeletedExpensesPermanently() }
    fun emptyDeletedReadingsPermanently() = viewModelScope.launch { repository.emptyDeletedReadingsPermanently() }
    fun emptyDeletedUtilityBillsPermanently() = viewModelScope.launch { repository.emptyDeletedUtilityBillsPermanently() }

    // Profiles
    fun saveUserProfile(profile: com.example.data.model.UserProfileEntity) = viewModelScope.launch {
        repository.saveUserProfile(profile)
    }

    fun updateProfile(name: String, contact: String, avatar: String) = viewModelScope.launch {
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        repository.saveUserProfile(current.copy(name = name, contact = contact, avatar = avatar))
    }
    
    fun updateCustomisation(mealGroupPosition: String, bgPreset: String, customBgUri: String) = viewModelScope.launch {
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        repository.saveUserProfile(current.copy(mealGroupPosition = mealGroupPosition, bgPreset = bgPreset, customBgUri = customBgUri))
    }
    
    fun updatePreferences(
        language: String,
        themeMode: String,
        showAdvancedPayment: Boolean,
        appLockEnabled: Boolean,
        appLockPin: String,
        biometricEnabled: Boolean,
        securityQuestion: String,
        securityAnswer: String,
        defaultHotelId: Int,
        enableStarAnimation: Boolean,
        textSize: String
    ) = viewModelScope.launch {
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        repository.saveUserProfile(current.copy(
            language = language,
            themeMode = themeMode,
            showAdvancedPayment = showAdvancedPayment,
            appLockEnabled = appLockEnabled,
            appLockPin = appLockPin,
            biometricEnabled = biometricEnabled,
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer,
            defaultHotelId = defaultHotelId,
            enableStarAnimation = enableStarAnimation,
            textSize = textSize
        ))
    }

    // Import / Export
    fun exportAllDataToJson(): String { return "{}" }
    fun exportMenusToCsv(): String { return "" }
    fun exportHotelTransactionsToCsv(): String { return "" }
    fun exportExtraExpensesToCsv(): String { return "" }
    fun exportMeterReadingsToCsv(): String { return "" }
    fun importAllDataFromJson(json: String) {}
    fun importMenusFromCsv(csv: String, hotelId: Int) {}
    fun importHotelTransactionsFromCsv(csv: String, hotelId: Int) {}
    fun importExtraExpensesFromCsv(csv: String) {}
    fun importMeterReadingsFromCsv(csv: String) {}
}
