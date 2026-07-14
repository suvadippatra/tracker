package com.example.ui.viewmodel
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

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinancialRepository

    val isInitialized = MutableStateFlow(false)

    // UI Tab State
    val activeTab = MutableStateFlow("Hotels")

    // Active Hotel Selection
    val activeHotelId = MutableStateFlow(1)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinancialRepository(db)
        viewModelScope.launch {
            repository.initializeDatabaseIfNeeded(application)
            // Retrieve first hotel to set as active initially
            repository.allHotels.collect { hotels ->
                if (hotels.isNotEmpty() && activeHotelId.value == 1) {
                    activeHotelId.value = hotels.first().id
                }
                isInitialized.value = true
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

    val allExtraExpenses: StateFlow<List<ExtraExpenseEntity>> = repository.allExtraExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCategories: StateFlow<List<com.example.data.model.TransactionCategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allMenuItems: StateFlow<List<MenuEntity>> = activeHotelId
        .flatMapLatest { id -> repository.getMenuForHotel(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedMenuItems: StateFlow<List<MenuEntity>> = activeHotelId
        .flatMapLatest { id -> repository.getDeletedMenuForHotel(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Active Hotel Stream
    val activeHotel: StateFlow<HotelEntity?> = activeHotelId
        .flatMapLatest { id -> repository.getHotelById(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Active Hotel Transactions
    val activeHotelTransactions: StateFlow<List<HotelTransactionEntity>> = activeHotelId
        .flatMapLatest { id -> repository.getTransactionsForHotel(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    fun resetOrderFlow() {
        _addMealDate.value = System.currentTimeMillis()
        // Auto-detect Day/Night from system clock
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        _addMealIsNight.value = hour < 6 || hour >= 18
        _orderQuantities.value = emptyMap()
        _selectedMainDishId.value = null
    }

    fun setOrderDate(timestamp: Long) {
        _addMealDate.value = timestamp
    }

    fun toggleDayNight() {
        _addMealIsNight.value = !_addMealIsNight.value
        // Reset quantities when switching menu modes, as available dishes change
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

    fun saveHotelTransaction() {
        val hotelId = activeHotelId.value
        val isNight = _addMealIsNight.value
        val timestamp = _addMealDate.value
        val menuList = allMenuItems.value

        val orderedItems = _orderQuantities.value.mapNotNull { (menuId, qty) ->
            val menu = menuList.find { it.id == menuId }
            if (menu != null) {
                OrderedItem(
                    name = menu.englishName,
                    quantity = qty,
                    price = menu.price
                )
            } else null
        }

        if (orderedItems.isEmpty()) return

        val total = orderedItems.sumOf { it.total }

        viewModelScope.launch {
            val transaction = HotelTransactionEntity(
                hotelId = hotelId,
                timestamp = timestamp,
                isNight = isNight,
                itemsSerialized = TransactionSerializer.serialize(orderedItems),
                totalAmount = total
            )
            repository.insertTransaction(transaction)
            resetOrderFlow()
        }
    }

    fun deleteTransaction(transaction: HotelTransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
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
        notes: String = ""
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
            repository.insertExpenseAndDeduct(expense, selectedAccount)
        }
    }

    fun deleteExtraExpense(expense: ExtraExpenseEntity) {
        val accounts = allBankAccounts.value
        viewModelScope.launch {
            repository.deleteExpenseAndRevert(expense, accounts)
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
    fun addVirtualAccount(name: String, startingBalance: Double, upiId: String = "", qrCodeUri: String = "") {
        viewModelScope.launch {
            repository.insertBankAccount(BankAccountEntity(name = name, balance = startingBalance, upiId = upiId, qrCodeUri = qrCodeUri))
        }
    }

    fun editVirtualAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.updateBankAccount(account)
        }
    }

    fun deleteVirtualAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.updateBankAccount(account.copy(isDeleted = true))
        }
    }

    fun restoreVirtualAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.updateBankAccount(account.copy(isDeleted = false))
        }
    }

    fun deleteVirtualAccountPermanently(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.deleteBankAccount(account)
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
        }
    }

    fun editHotel(hotel: HotelEntity) {
        viewModelScope.launch {
            repository.updateHotel(hotel)
        }
    }

    fun deleteHotel(hotel: HotelEntity) {
        viewModelScope.launch {
            repository.updateHotel(hotel.copy(isDeleted = true))
        }
    }

    fun restoreHotel(hotel: HotelEntity) {
        viewModelScope.launch {
            repository.updateHotel(hotel.copy(isDeleted = false))
        }
    }

    fun deleteHotelPermanently(hotel: HotelEntity) {
        viewModelScope.launch {
            repository.deleteHotel(hotel)
        }
    }

    fun updateProfile(name: String, contact: String, avatar: String) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(current.copy(name = name, contact = contact, avatar = avatar))
        }
    }

    fun updatePreferences(
        language: String,
        themeMode: String,
        showAdvancedPayment: Boolean,
        appLockEnabled: Boolean,
        appLockPin: String,
        defaultHotelId: Int,
        enableStarAnimation: Boolean
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
                        enableStarAnimation = enableStarAnimation
                    )
                )
        }
    }
}
