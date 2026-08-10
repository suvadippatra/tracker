package com.example.ui.viewmodel
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

    val deletedBankAccounts: StateFlow<List<BankAccountEntity>> = repository.deletedBankAccounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedTransactions: StateFlow<List<HotelTransactionEntity>> = repository.deletedTransactions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedExtraExpenses: StateFlow<List<ExtraExpenseEntity>> = repository.deletedExtraExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedMeterReadings: StateFlow<List<MeterReading>> = repository.deletedMeterReadings
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
                val uri = Uri.parse(uriString)
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
                return Uri.fromFile(file).toString()
            } catch (e: Exception) {
                Log.e("FinancialViewModel", "Failed to copy to internal storage", e)
            }
        }
        return uriString
    }

    fun deleteExtraExpense(expense: ExtraExpenseEntity) {
        val accounts = allBankAccounts.value
        viewModelScope.launch {
            repository.softDeleteExpenseAndRevert(expense, accounts)
            updateWidgets()
        }
    }

    fun restoreExtraExpense(expense: ExtraExpenseEntity) {
        val accounts = allBankAccounts.value
        viewModelScope.launch {
            repository.restoreExpenseAndApply(expense, accounts)
            updateWidgets()
        }
    }

    fun deleteExtraExpensePermanently(expense: ExtraExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpensePermanently(expense)
            updateWidgets()
        }
    }

    fun addCategory(name: String, type: String = "EXPENSE") {
        viewModelScope.launch {
            repository.insertCategory(com.example.data.model.TransactionCategoryEntity(name = name, type = type))
        }
    }

    fun deleteCategory(category: com.example.data.model.TransactionCategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // ==========================================
    // SETTINGS / CONFIGURATION ACTIONS
    // ==========================================
    fun addVirtualAccount(name: String, startingBalance: Double, upiId: String = "", qrCodeUri: String = "", accountNumber: String = "", ifsc: String = "", branchName: String = "") {
        viewModelScope.launch {
            repository.insertBankAccount(BankAccountEntity(name = name, balance = startingBalance, upiId = upiId, qrCodeUri = qrCodeUri, accountNumber = accountNumber, ifsc = ifsc, branchName = branchName))
            updateWidgets()
        }
    }

    fun editVirtualAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.updateBankAccount(account)
            updateWidgets()
        }
    }

    fun deleteVirtualAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.updateBankAccount(account.copy(isDeleted = true))
            updateWidgets()
        }
    }

    fun restoreVirtualAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.updateBankAccount(account.copy(isDeleted = false))
            updateWidgets()
        }
    }

    fun deleteVirtualAccountPermanently(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.deleteBankAccount(account)
            updateWidgets()
        }
    }

    
    fun addMenu(menu: com.example.data.model.MenuEntity) {
        viewModelScope.launch {
            repository.insertMenu(menu)
        }
    }
    fun updateMenu(menu: com.example.data.model.MenuEntity) {
        viewModelScope.launch {
            repository.updateMenu(menu)
        }
    }
    fun deleteMenu(menu: com.example.data.model.MenuEntity) {
        viewModelScope.launch {
            repository.updateMenu(menu.copy(isDeleted = true))
        }
    }

    fun restoreMenu(menu: com.example.data.model.MenuEntity) {
        viewModelScope.launch {
            repository.updateMenu(menu.copy(isDeleted = false))
        }
    }

    fun deleteMenuPermanently(menu: com.example.data.model.MenuEntity) {
        viewModelScope.launch {
            repository.deleteMenu(menu)
        }
    }

    fun addHotel(
        name: String,
        advancedPayment: Double,
        phoneNumber: String = "",
        location: String = "",
        upiId: String = "",
        qrCodeUri: String = ""
    ) {
        viewModelScope.launch {
            repository.insertHotel(
                HotelEntity(
                    name = name,
                    advancedPayment = advancedPayment,
                    phoneNumber = phoneNumber,
                    location = location,
                    upiId = upiId,
                    qrCodeUri = qrCodeUri
                )
            )
            updateWidgets()
        }
    }

    fun editHotel(hotel: HotelEntity) {
        viewModelScope.launch {
            repository.updateHotel(hotel)
            updateWidgets()
        }
    }

    fun deleteHotel(hotel: HotelEntity) {
        viewModelScope.launch {
            repository.updateHotel(hotel.copy(isDeleted = true))
            updateWidgets()
        }
    }

    fun restoreHotel(hotel: HotelEntity) {
        viewModelScope.launch {
            repository.updateHotel(hotel.copy(isDeleted = false))
            updateWidgets()
        }
    }

    fun deleteHotelPermanently(hotel: HotelEntity) {
        viewModelScope.launch {
            repository.deleteHotel(hotel)
            updateWidgets()
        }
    }

    fun deleteReading(reading: MeterReading) {
        viewModelScope.launch {
            repository.deleteReading(reading)
            updateWidgets()
        }
    }

    fun emptyDeletedHotelsPermanently() {
        viewModelScope.launch {
            deletedHotels.value.forEach {
                repository.deleteHotel(it)
            }
            updateWidgets()
        }
    }

    fun emptyDeletedBankAccountsPermanently() {
        viewModelScope.launch {
            deletedBankAccounts.value.forEach {
                repository.deleteBankAccount(it)
            }
            updateWidgets()
        }
    }

    fun emptyDeletedMenuItemsPermanently() {
        viewModelScope.launch {
            deletedMenuItems.value.forEach {
                repository.deleteMenu(it)
            }
        }
    }

    fun emptyDeletedReadingsPermanently() {
        viewModelScope.launch {
            repository.emptyDeletedReadingsPermanently()
            updateWidgets()
        }
    }

    fun emptyDeletedTransactionsPermanently() {
        viewModelScope.launch {
            repository.emptyDeletedTransactionsPermanently()
            updateWidgets()
        }
    }

    fun emptyDeletedExpensesPermanently() {
        viewModelScope.launch {
            repository.emptyDeletedExpensesPermanently()
            updateWidgets()
        }
    }

    fun restoreMeterReading(id: String) {
        viewModelScope.launch {
            repository.restoreReading(id)
            updateWidgets()
        }
    }

    // Global Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val selectedSearchFilter = MutableStateFlow("All")
    val searchHistory = MutableStateFlow<List<String>>(emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchFilter(filter: String) {
        selectedSearchFilter.value = filter
    }

    fun addSearchHistory(term: String) {
        val trimmed = term.trim()
        if (trimmed.isNotBlank()) {
            val current = searchHistory.value.toMutableList()
            current.remove(trimmed)
            current.add(0, trimmed)
            if (current.size > 10) current.removeAt(current.lastIndex)
            searchHistory.value = current
        }
    }

    fun clearSearchHistory() {
        searchHistory.value = emptyList()
    }

    fun removeSearchHistoryItem(term: String) {
        searchHistory.value = searchHistory.value.filter { it != term }
    }

    private fun formatSearchDate(timestamp: Long): String {
        return try {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    val allTransactions: StateFlow<List<HotelTransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allMeterReadings: StateFlow<List<MeterReading>> = repository.allMeterReadings
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val searchResults: StateFlow<List<SearchResult>> = combine(
        _searchQuery.debounce(200L),
        selectedSearchFilter,
        allHotels,
        allExtraExpenses,
        allBankAccounts
    ) { query, filter, hotels, expenses, accounts ->
        SearchCombineTuple5(query, filter, hotels, expenses, accounts)
    }.combine(allTransactions) { tuple, transactions ->
        tuple to transactions
    }.combine(allMenuItems) { (tuple, transactions), menus ->
        Triple(tuple, transactions, menus)
    }.combine(allMeterReadings) { (tuple, transactions, menus), meterReadings ->
        val query = tuple.query
        val filter = tuple.filter
        val hotels = tuple.hotels
        val expenses = tuple.expenses
        val accounts = tuple.accounts

        if (query.isBlank()) return@combine emptyList<SearchResult>()

        val results = mutableListOf<SearchResult>()
        val q = query.trim().lowercase()

        // 1. Search Hotels
        hotels.filter { 
            !it.isDeleted && (it.name.lowercase().contains(q) || it.location.lowercase().contains(q) || it.phoneNumber.contains(q) || it.upiId.lowercase().contains(q))
        }.forEach {
            results.add(SearchResult(it.id, it.name, "Hotel", if (it.location.isNotBlank()) "Location: ${it.location}" else "Hotel Record", dateOrExtra = it.phoneNumber, hotelId = it.id))
        }

        // 2. Search Extra Expenses & Income
        expenses.filter {
            !it.isDeleted && (it.name.lowercase().contains(q) || it.category.lowercase().contains(q) || it.notes.lowercase().contains(q) || it.amount.toString().contains(q))
        }.forEach {
            results.add(
                SearchResult(
                    id = it.id,
                    title = it.name,
                    type = if (it.type == "INCOME") "Income" else "Expense",
                    subtitle = "${it.category} • ₹${it.amount}${if (it.notes.isNotBlank()) " (${it.notes})" else ""}",
                    dateOrExtra = formatSearchDate(it.timestamp),
                    amount = it.amount,
                    category = it.category
                )
            )
        }

        // 3. Search Meal Transactions
        transactions.filter { !it.isDeleted }.forEach { tx ->
            val items = com.example.data.model.TransactionSerializer.deserialize(tx.itemsSerialized)
            val itemNames = items.joinToString(", ") { it.name }
            val dateStr = formatSearchDate(tx.timestamp).lowercase()
            val matches = itemNames.lowercase().contains(q) || tx.totalAmount.toString().contains(q) || dateStr.contains(q)
            if (matches) {
                results.add(
                    SearchResult(
                        id = tx.id,
                        title = if (itemNames.isNotBlank()) itemNames else "Meal Order #${tx.id}",
                        type = "Meal",
                        subtitle = "${if (tx.isNight) "Night Meal" else "Day Meal"} • ₹${tx.totalAmount}",
                        dateOrExtra = formatSearchDate(tx.timestamp),
                        amount = tx.totalAmount,
                        hotelId = tx.hotelId
                    )
                )
            }
        }

        // 4. Search Menu Items
        menus.filter { !it.isDeleted && (it.englishName.lowercase().contains(q) || it.banglaName.lowercase().contains(q) || it.type.lowercase().contains(q)) }
            .forEach {
                results.add(
                    SearchResult(
                        id = it.id,
                        title = "${it.englishName} (${it.banglaName})",
                        type = "Menu",
                        subtitle = "${it.type} Item • ₹${it.price}",
                        amount = it.price,
                        hotelId = it.hotelId
                    )
                )
            }

        // 5. Search Accounts
        accounts.filter { !it.isDeleted && (it.name.lowercase().contains(q) || it.upiId.lowercase().contains(q) || it.balance.toString().contains(q)) }
            .forEach {
                results.add(
                    SearchResult(
                        id = it.id,
                        title = it.name,
                        type = "Account",
                        subtitle = "Account Balance: ₹${it.balance}",
                        dateOrExtra = if (it.upiId.isNotBlank()) "UPI: ${it.upiId}" else "",
                        amount = it.balance
                    )
                )
            }

        // 6. Search Meter Readings
        meterReadings.filter { !it.isDeleted && (it.totalReading.toString().contains(q) || it.unitsConsumed.toString().contains(q) || formatSearchDate(it.readingDate).lowercase().contains(q)) }
            .forEach {
                results.add(
                    SearchResult(
                        id = it.id.hashCode(),
                        title = "Electric Meter: ${it.unitsConsumed} Units",
                        type = "Meter",
                        subtitle = "Total Reading: ${it.totalReading} • ${if (it.isPaid) "Paid" else "Unpaid"}",
                        dateOrExtra = formatSearchDate(it.readingDate),
                        amount = it.unitsConsumed
                    )
                )
            }

        // Apply Category Filter
        when (filter) {
            "Expenses" -> results.filter { it.type in listOf("Expense", "Income") }
            "Meals & Menus" -> results.filter { it.type in listOf("Meal", "Menu") }
            "Hotels" -> results.filter { it.type == "Hotel" }
            "Accounts" -> results.filter { it.type == "Account" }
            "Bills" -> results.filter { it.type == "Meter" }
            else -> results
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateProfile(name: String, contact: String, avatar: String) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(current.copy(name = name, contact = contact, avatar = avatar))
        }
    }

    val isLockedManual = MutableStateFlow(false)

    fun lockApp() {
        isLockedManual.value = true
    }

    fun unlockApp() {
        isLockedManual.value = false
    }

    fun updatePreferences(
        language: String,
        themeMode: String,
        showAdvancedPayment: Boolean,
        appLockEnabled: Boolean,
        appLockPin: String,
        defaultHotelId: Int,
        enableStarAnimation: Boolean,
        textSize: String,
        biometricEnabled: Boolean = false,
        securityQuestion: String = "",
        securityAnswer: String = ""
    ) {
        viewModelScope.launch {
            val current = userProfile.value
            
            repository.saveUserProfile(
                current.copy(
                    language = language,
                    themeMode = themeMode,
                    showAdvancedPayment = showAdvancedPayment,
                    appLockEnabled = appLockEnabled,
                    appLockPin = appLockPin,
                    defaultHotelId = defaultHotelId,
                    enableStarAnimation = enableStarAnimation,
                    textSize = textSize,
                    biometricEnabled = biometricEnabled,
                    securityQuestion = securityQuestion,
                    securityAnswer = securityAnswer
                )
            )
            updateWidgets()
        }
    }

    fun updateCustomisation(mealGroupPosition: String, bgPreset: String, customBgUri: String) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(
                current.copy(
                    mealGroupPosition = mealGroupPosition,
                    bgPreset = bgPreset,
                    customBgUri = customBgUri
                )
            )
        }
    }

    // CSV Export & Import Backups
    fun exportMenusToCsv(): String {
        val sb = StringBuilder()
        sb.append("hotelId,banglaName,englishName,price,isDayMenu,isNightMenu,isAddOn,emoji,type,isDeleted,imageUri\n")
        allMenuItems.value.forEach { menu ->
            sb.append("${menu.hotelId},")
                .append("${escapeCsv(menu.banglaName)},")
                .append("${escapeCsv(menu.englishName)},")
                .append("${menu.price},")
                .append("${menu.isDayMenu},")
                .append("${menu.isNightMenu},")
                .append("${menu.isAddOn},")
                .append("${escapeCsv(menu.emoji)},")
                .append("${escapeCsv(menu.type)},")
                .append("${menu.isDeleted},")
                .append("${escapeCsv(menu.imageUri ?: "")}\n")
        }
        return sb.toString()
    }

    fun exportHotelTransactionsToCsv(): String {
        val sb = StringBuilder()
        sb.append("hotelId,timestamp,isNight,itemsSerialized,totalAmount\n")
        allTransactions.value.forEach { tx ->
            sb.append("${tx.hotelId},")
                .append("${tx.timestamp},")
                .append("${tx.isNight},")
                .append("${escapeCsv(tx.itemsSerialized)},")
                .append("${tx.totalAmount}\n")
        }
        return sb.toString()
    }

    fun exportExtraExpensesToCsv(): String {
        val sb = StringBuilder()
        sb.append("timestamp,name,amount,bankAccountId,receiptUri,type,category,notes\n")
        allExtraExpenses.value.forEach { ex ->
            sb.append("${ex.timestamp},")
                .append("${escapeCsv(ex.name)},")
                .append("${ex.amount},")
                .append("${ex.bankAccountId},")
                .append("${escapeCsv(ex.receiptUri ?: "")},")
                .append("${escapeCsv(ex.type)},")
                .append("${escapeCsv(ex.category)},")
                .append("${escapeCsv(ex.notes)}\n")
        }
        return sb.toString()
    }

    fun exportMeterReadingsToCsv(): String {
        val sb = StringBuilder()
        sb.append("id,readingDate,totalReading,unitsConsumed,timestamp,isPaid,isDeleted\n")
        allMeterReadings.value.forEach { r ->
            sb.append("${escapeCsv(r.id)},")
                .append("${r.readingDate},")
                .append("${r.totalReading},")
                .append("${r.unitsConsumed},")
                .append("${r.timestamp},")
                .append("${r.isPaid},")
                .append("${r.isDeleted}\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    fun importMenusFromCsv(csvText: String, targetHotelId: Int? = null) {
        viewModelScope.launch {
            val lines = csvText.lines()
            if (lines.size <= 1) return@launch
            val header = lines[0].lowercase()
            val isFullExport = header.contains("isdaymenu")
            val isSettingsFormat = header.contains("banglaname") && !isFullExport
            
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = parseCsvLine(line).map { it.trim() }
                try {
                    if (isFullExport && parts.size >= 8) {
                        val parsedHotelId = parts[0].toIntOrNull()
                        val hId = targetHotelId ?: parsedHotelId ?: 1
                        val pOffset = if (parsedHotelId != null && parts.size >= 9) 1 else 0
                        val isDayRaw = parts[pOffset + 3].equals("true", ignoreCase = true)
                        val isNightRaw = parts[pOffset + 4].equals("true", ignoreCase = true)
                        val menu = MenuEntity(
                            hotelId = hId,
                            banglaName = parts[pOffset],
                            englishName = parts[pOffset + 1],
                            price = parts[pOffset + 2].toDoubleOrNull() ?: 0.0,
                            isDayMenu = if (!isDayRaw && !isNightRaw) true else isDayRaw,
                            isNightMenu = if (!isDayRaw && !isNightRaw) true else isNightRaw,
                            isAddOn = parts[pOffset + 5].equals("true", ignoreCase = true),
                            emoji = parts[pOffset + 6],
                            type = parts[pOffset + 7],
                            isDeleted = if (parts.size > pOffset + 8) parts[pOffset + 8].equals("true", ignoreCase = true) else false,
                            imageUri = if (parts.size > pOffset + 9 && parts[pOffset + 9].isNotBlank()) parts[pOffset + 9] else null
                        )
                        repository.insertMenu(menu)
                    } else if (isSettingsFormat && parts.size >= 5) {
                        // englishName, banglaName, price, emoji, type
                        val typeStr = parts[4]
                        repository.insertMenu(MenuEntity(
                            hotelId = targetHotelId ?: 1,
                            englishName = parts[0],
                            banglaName = parts[1],
                            price = parts[2].toDoubleOrNull() ?: 0.0,
                            emoji = parts[3],
                            type = typeStr,
                            isDayMenu = typeStr.contains("Day", ignoreCase = true) || typeStr.contains("Common", ignoreCase = true) || (!typeStr.contains("Night", ignoreCase = true) && !typeStr.contains("Day", ignoreCase = true)),
                            isNightMenu = typeStr.contains("Night", ignoreCase = true) || typeStr.contains("Common", ignoreCase = true) || (!typeStr.contains("Night", ignoreCase = true) && !typeStr.contains("Day", ignoreCase = true)),
                            isAddOn = false
                        ))
                    } else if (parts.size >= 4) {
                        // Name, Price, Type, Emoji (Menu Management Screen format)
                        val name = parts[0]
                        val price = parts[1].toDoubleOrNull() ?: 0.0
                        val typeStr = parts[2]
                        val emoji = parts[3]
                        repository.insertMenu(MenuEntity(
                            hotelId = targetHotelId ?: 1,
                            banglaName = name,
                            englishName = name,
                            price = price,
                            type = typeStr,
                            emoji = emoji,
                            isDayMenu = typeStr.contains("Day", ignoreCase = true) || typeStr.contains("Common", ignoreCase = true) || (!typeStr.contains("Night", ignoreCase = true) && !typeStr.contains("Day", ignoreCase = true)),
                            isNightMenu = typeStr.contains("Night", ignoreCase = true) || typeStr.contains("Common", ignoreCase = true) || (!typeStr.contains("Night", ignoreCase = true) && !typeStr.contains("Day", ignoreCase = true)),
                            isAddOn = false
                        ))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } } } } 
    fun importHotelTransactionsFromCsv(csvText: String, targetHotelId: Int? = null) {
        viewModelScope.launch {
            val lines = csvText.lines()
            if (lines.size <= 1) return@launch
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = parseCsvLine(line)
                if (parts.size >= 4) {
                    try {
                        val parsedHotelId = parts[0].toIntOrNull()
                        val hId = targetHotelId ?: parsedHotelId ?: 1
                        val pOffset = if (parsedHotelId != null && parts.size >= 5) 1 else 0
                        
                        val tx = HotelTransactionEntity(
                            hotelId = hId,
                            timestamp = parts[pOffset].toLongOrNull() ?: System.currentTimeMillis(),
                            isNight = parts[pOffset + 1].toBoolean(),
                            itemsSerialized = parts[pOffset + 2],
                            totalAmount = parts[pOffset + 3].toDoubleOrNull() ?: 0.0
                        )
                        repository.insertTransaction(tx)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun importExtraExpensesFromCsv(csvText: String) {
        viewModelScope.launch {
            val lines = csvText.lines()
            if (lines.size <= 1) return@launch
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = parseCsvLine(line)
                if (parts.size >= 8) {
                    try {
                        val ex = ExtraExpenseEntity(
                            timestamp = parts[0].toLongOrNull() ?: System.currentTimeMillis(),
                            name = parts[1],
                            amount = parts[2].toDoubleOrNull() ?: 0.0,
                            bankAccountId = parts[3].toIntOrNull() ?: 1,
                            receiptUri = if (parts[4].isNotBlank()) parts[4] else null,
                            type = parts[5],
                            category = parts[6],
                            notes = parts[7]
                        )
                        repository.insertExpense(ex)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun importMeterReadingsFromCsv(csvText: String) {
        viewModelScope.launch {
            val lines = csvText.lines()
            if (lines.size <= 1) return@launch
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = parseCsvLine(line)
                if (parts.size >= 7) {
                    try {
                        val r = MeterReading(
                            id = parts[0].ifBlank { java.util.UUID.randomUUID().toString() },
                            readingDate = parts[1].toLongOrNull() ?: System.currentTimeMillis(),
                            totalReading = parts[2].toDoubleOrNull() ?: 0.0,
                            unitsConsumed = parts[3].toDoubleOrNull() ?: 0.0,
                            timestamp = parts[4].toLongOrNull() ?: System.currentTimeMillis(),
                            isPaid = parts[5].toBoolean(),
                            isDeleted = parts[6].toBoolean()
                        )
                        repository.insertReading(r)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun exportAllDataToJson(): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"hotels\": [").append(allHotels.value.joinToString(",") { serializeHotel(it) }).append("],")
        sb.append("\"menus\": [").append(allMenuItems.value.joinToString(",") { serializeMenu(it) }).append("],")
        sb.append("\"transactions\": [").append(allTransactions.value.joinToString(",") { serializeTransaction(it) }).append("],")
        sb.append("\"expenses\": [").append(allExtraExpenses.value.joinToString(",") { serializeExpense(it) }).append("],")
        sb.append("\"meterReadings\": [").append(allMeterReadings.value.joinToString(",") { serializeReading(it) }).append("],")
        sb.append("\"accounts\": [").append(allBankAccounts.value.joinToString(",") { serializeAccount(it) }).append("]")
        sb.append("}")
        return sb.toString()
    }

    private fun serializeHotel(h: HotelEntity) = "{\"id\":${h.id},\"name\":\"${escapeJson(h.name)}\",\"advancedPayment\":${h.advancedPayment},\"phoneNumber\":\"${escapeJson(h.phoneNumber)}\",\"location\":\"${escapeJson(h.location)}\",\"upiId\":\"${escapeJson(h.upiId)}\",\"qrCodeUri\":\"${escapeJson(h.qrCodeUri)}\",\"isDeleted\":${h.isDeleted}}"
    private fun serializeMenu(m: MenuEntity) = "{\"id\":${m.id},\"hotelId\":${m.hotelId},\"banglaName\":\"${escapeJson(m.banglaName)}\",\"englishName\":\"${escapeJson(m.englishName)}\",\"price\":${m.price},\"isDayMenu\":${m.isDayMenu},\"isNightMenu\":${m.isNightMenu},\"isAddOn\":${m.isAddOn},\"emoji\":\"${escapeJson(m.emoji)}\",\"type\":\"${escapeJson(m.type)}\",\"isDeleted\":${m.isDeleted}}"
    private fun serializeTransaction(tx: HotelTransactionEntity) = "{\"id\":${tx.id},\"hotelId\":${tx.hotelId},\"timestamp\":${tx.timestamp},\"isNight\":${tx.isNight},\"itemsSerialized\":\"${escapeJson(tx.itemsSerialized)}\",\"totalAmount\":${tx.totalAmount},\"isDeleted\":${tx.isDeleted}}"
    private fun serializeExpense(ex: ExtraExpenseEntity) = "{\"id\":${ex.id},\"timestamp\":${ex.timestamp},\"name\":\"${escapeJson(ex.name)}\",\"amount\":${ex.amount},\"bankAccountId\":${ex.bankAccountId},\"type\":\"${escapeJson(ex.type)}\",\"category\":\"${escapeJson(ex.category)}\",\"notes\":\"${escapeJson(ex.notes)}\",\"isDeleted\":${ex.isDeleted}}"
    private fun serializeReading(r: MeterReading) = "{\"id\":\"${r.id}\",\"readingDate\":${r.readingDate},\"totalReading\":${r.totalReading},\"unitsConsumed\":${r.unitsConsumed},\"timestamp\":${r.timestamp},\"isPaid\":${r.isPaid},\"isDeleted\":${r.isDeleted}}"
    private fun serializeAccount(a: BankAccountEntity) = "{\"id\":${a.id},\"name\":\"${escapeJson(a.name)}\",\"balance\":${a.balance},\"upiId\":\"${escapeJson(a.upiId)}\",\"qrCodeUri\":\"${escapeJson(a.qrCodeUri)}\",\"accountNumber\":\"${escapeJson(a.accountNumber)}\",\"ifsc\":\"${escapeJson(a.ifsc)}\",\"branchName\":\"${escapeJson(a.branchName)}\",\"isDeleted\":${a.isDeleted}}"

    private fun escapeJson(s: String): String = s.replace("\"", "\\\"").replace("\n", "\\n")

    fun importAllDataFromJson(json: String) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(json)

                // Import Hotels
                if (root.has("hotels")) {
                    val hotels = root.getJSONArray("hotels")
                    for (i in 0 until hotels.length()) {
                        val h = hotels.getJSONObject(i)
                        repository.insertHotel(HotelEntity(
                            id = h.optInt("id", 0),
                            name = h.optString("name"),
                            advancedPayment = h.optDouble("advancedPayment", 0.0),
                            phoneNumber = h.optString("phoneNumber"),
                            location = h.optString("location"),
                            upiId = h.optString("upiId"),
                            qrCodeUri = h.optString("qrCodeUri"),
                            isDeleted = h.optBoolean("isDeleted")
                        ))
                    }
                }
                
                // Import Menus
                if (root.has("menus")) {
                    val menus = root.getJSONArray("menus")
                    for (i in 0 until menus.length()) {
                        val m = menus.getJSONObject(i)
                        repository.insertMenu(MenuEntity(
                            id = m.optInt("id", 0),
                            hotelId = m.optInt("hotelId", 1),
                            banglaName = m.optString("banglaName"),
                            englishName = m.optString("englishName"),
                            price = m.optDouble("price"),
                            isDayMenu = m.optBoolean("isDayMenu"),
                            isNightMenu = m.optBoolean("isNightMenu"),
                            isAddOn = m.optBoolean("isAddOn", false),
                            emoji = m.optString("emoji", "🍽️"),
                            type = m.optString("type", "Common"),
                            isDeleted = m.optBoolean("isDeleted", false)
                        ))
                    }
                }

                // Import Transactions
                if (root.has("transactions")) {
                    val txs = root.getJSONArray("transactions")
                    for (i in 0 until txs.length()) {
                        val tx = txs.getJSONObject(i)
                        repository.insertTransaction(HotelTransactionEntity(
                            id = tx.optInt("id", 0),
                            hotelId = tx.optInt("hotelId"),
                            timestamp = tx.optLong("timestamp"),
                            isNight = tx.optBoolean("isNight"),
                            itemsSerialized = tx.optString("itemsSerialized"),
                            totalAmount = tx.optDouble("totalAmount"),
                            isDeleted = tx.optBoolean("isDeleted")
                        ))
                    }
                }

                // Import Expenses
                if (root.has("expenses")) {
                    val exs = root.getJSONArray("expenses")
                    for (i in 0 until exs.length()) {
                        val ex = exs.getJSONObject(i)
                        repository.insertExpense(ExtraExpenseEntity(
                            id = ex.optInt("id", 0),
                            timestamp = ex.optLong("timestamp"),
                            name = ex.optString("name"),
                            amount = ex.optDouble("amount"),
                            bankAccountId = ex.optInt("bankAccountId"),
                            type = ex.optString("type"),
                            category = ex.optString("category"),
                            notes = ex.optString("notes"),
                            isDeleted = ex.optBoolean("isDeleted")
                        ))
                    }
                }

                // Import Meter Readings
                if (root.has("meterReadings")) {
                    val readings = root.getJSONArray("meterReadings")
                    for (i in 0 until readings.length()) {
                        val r = readings.getJSONObject(i)
                        repository.insertReading(MeterReading(
                            id = r.optString("id", java.util.UUID.randomUUID().toString()),
                            readingDate = r.optLong("readingDate"),
                            totalReading = r.optDouble("totalReading", 0.0),
                            unitsConsumed = r.optDouble("unitsConsumed", 0.0),
                            timestamp = r.optLong("timestamp"),
                            isPaid = r.optBoolean("isPaid"),
                            isDeleted = r.optBoolean("isDeleted")
                        ))
                    }
                }

                // Import Bank Accounts
                if (root.has("accounts")) {
                    val accs = root.getJSONArray("accounts")
                    for (i in 0 until accs.length()) {
                        val a = accs.getJSONObject(i)
                        repository.insertBankAccount(BankAccountEntity(
                            id = a.optInt("id", 0),
                            name = a.optString("name"),
                            balance = a.optDouble("balance"),
                            upiId = a.optString("upiId"),
                            qrCodeUri = a.optString("qrCodeUri"),
                            isDeleted = a.optBoolean("isDeleted")
                        ))
                    }
                }
                updateWidgets()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
