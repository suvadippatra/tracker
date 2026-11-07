@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
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
import com.example.data.model.HotelPaymentEntity
import com.example.data.repository.FinancialRepository
import com.example.data.model.SearchResult
import com.example.data.model.MeterReading
import com.example.data.model.UtilityBill
import com.example.data.model.StickyNoteEntity
import com.example.data.model.OwnerAccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
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


sealed class RootOverlayState {
    object None : RootOverlayState()
    object AddMeal : RootOverlayState()
    data class ManageMenu(val hotel: com.example.data.model.HotelEntity, val returnToEditHotel: Boolean = false) : RootOverlayState()
    data class EditHotel(val hotel: com.example.data.model.HotelEntity?) : RootOverlayState()
    object OwnerProfile : RootOverlayState()
    object AccountManager : RootOverlayState()
    data class AddOwnerAccount(val account: com.example.data.model.OwnerAccountEntity?) : RootOverlayState()
    data class EditBankAccount(val account: com.example.data.model.BankAccountEntity?) : RootOverlayState()
}

class FinancialViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val _rootOverlayState = MutableStateFlow<RootOverlayState>(RootOverlayState.None)
    val rootOverlayState: StateFlow<RootOverlayState> = _rootOverlayState

    fun showRootOverlay(state: RootOverlayState) {
        _rootOverlayState.value = state
    }

    fun closeRootOverlay() {
        _rootOverlayState.value = RootOverlayState.None
    }


    private val repository: FinancialRepository

    val isInitialized = MutableStateFlow(false)

    // UI Tab State
    val activeTab = MutableStateFlow(savedStateHandle.get<String>("KEY_ACTIVE_TAB") ?: "Hotels")
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
    val activeHotelId = MutableStateFlow(savedStateHandle.get<Int>("KEY_ACTIVE_HOTEL_ID") ?: 1)
    val activeBankAccountId = MutableStateFlow<Int?>(savedStateHandle.get<Int?>("KEY_ACTIVE_BANK_ACCOUNT_ID"))

    private val initialProfile = com.example.ui.theme.ThemePreferences.getInitialProfile(application)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinancialRepository(db)
        viewModelScope.launch {
            activeTab.collect { tab ->
                savedStateHandle["KEY_ACTIVE_TAB"] = tab
            }
        }
        viewModelScope.launch {
            activeHotelId.collect { id ->
                savedStateHandle["KEY_ACTIVE_HOTEL_ID"] = id
            }
        }
        viewModelScope.launch {
            activeBankAccountId.collect { id ->
                if (id != null) {
                    savedStateHandle["KEY_ACTIVE_BANK_ACCOUNT_ID"] = id
                }
            }
        }
        viewModelScope.launch {
            repository.initializeDatabaseIfNeeded(application)
            val profile = repository.userProfile.first()
            if (profile != null) {
                com.example.ui.theme.ThemePreferences.save(application, profile)
            }
            val hotels = repository.allHotels.first()
            if (hotels.isNotEmpty()) {
                val targetHotelId = profile?.defaultHotelId ?: hotels.first().id
                if (hotels.any { it.id == targetHotelId }) {
                    activeHotelId.value = targetHotelId
                } else {
                    val savedHotelId = savedStateHandle.get<Int>("KEY_ACTIVE_HOTEL_ID")
                    if (savedHotelId != null && hotels.any { it.id == savedHotelId }) {
                        activeHotelId.value = savedHotelId
                    } else {
                        activeHotelId.value = hotels.first().id
                    }
                }
            }
            val accounts = repository.allBankAccounts.first()
            if (accounts.isNotEmpty()) {
                val targetAccountId = profile?.defaultBankAccountId ?: accounts.first().id
                if (accounts.any { it.id == targetAccountId }) {
                    activeBankAccountId.value = targetAccountId
                } else {
                    val savedAccId = savedStateHandle.get<Int>("KEY_ACTIVE_BANK_ACCOUNT_ID")
                    if (savedAccId != null && accounts.any { it.id == savedAccId }) {
                        activeBankAccountId.value = savedAccId
                    } else {
                        activeBankAccountId.value = accounts.first().id
                    }
                }
            }
            isInitialized.value = true
        }
        viewModelScope.launch {
            repository.allHotels.collect { updatedHotels ->
                if (updatedHotels.isNotEmpty() && updatedHotels.none { it.id == activeHotelId.value }) {
                    val defId = userProfile.value.defaultHotelId
                    activeHotelId.value = updatedHotels.find { it.id == defId }?.id ?: updatedHotels.first().id
                }
            }
        }
        viewModelScope.launch {
            repository.allBankAccounts.collect { accounts ->
                if (accounts.isNotEmpty() && (activeBankAccountId.value == null || accounts.none { it.id == activeBankAccountId.value })) {
                    val defId = userProfile.value.defaultBankAccountId
                    activeBankAccountId.value = accounts.find { it.id == defId }?.id ?: accounts.first().id
                }
            }
        }
    }

    // Streams from Repository (Eagerly shared for zero-delay tab/page transitions)
    val userProfile: StateFlow<UserProfileEntity> = repository.userProfile
        .map { profile ->
            val p = profile ?: initialProfile
            com.example.ui.theme.ThemePreferences.save(getApplication(), p)
            p
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialProfile)

    val selectedStickyTag = MutableStateFlow("ALL")

    val allHotels: StateFlow<List<HotelEntity>> = combine(repository.allHotels, userProfile) { hotels, profile ->
        val defId = profile.defaultHotelId
        hotels.sortedWith(compareByDescending<HotelEntity> { it.id == defId }.thenBy { it.id })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedHotels: StateFlow<List<HotelEntity>> = repository.deletedHotels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBankAccounts: StateFlow<List<BankAccountEntity>> = combine(repository.allBankAccounts, userProfile) { accounts, profile ->
        val defId = profile.defaultBankAccountId
        accounts.sortedWith(compareByDescending<BankAccountEntity> { it.id == defId }.thenBy { it.id })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTransactions: StateFlow<List<HotelTransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allHotelMenus: StateFlow<List<MenuEntity>> = repository.allMenuItems
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allMeterReadings: StateFlow<List<MeterReading>> = repository.allMeterReadings
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allUtilityBills: StateFlow<List<UtilityBill>> = repository.allUtilityBills
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedBankAccounts: StateFlow<List<BankAccountEntity>> = repository.deletedBankAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedTransactions: StateFlow<List<HotelTransactionEntity>> = repository.deletedTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedExtraExpenses: StateFlow<List<ExtraExpenseEntity>> = repository.deletedExtraExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedMeterReadings: StateFlow<List<MeterReading>> = repository.deletedMeterReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deletedUtilityBills: StateFlow<List<UtilityBill>> = repository.deletedUtilityBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deletedElectricityRates: StateFlow<List<com.example.data.model.ElectricityRate>> = repository.deletedElectricityRates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deletedOwnerAccounts: StateFlow<List<com.example.data.model.OwnerAccountEntity>> = repository.deletedOwnerAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExtraExpenses: StateFlow<List<ExtraExpenseEntity>> = repository.allExtraExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCategories: StateFlow<List<com.example.data.model.TransactionCategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allHotelPayments: StateFlow<List<HotelPaymentEntity>> = repository.allHotelPayments
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allOwnerAccounts: StateFlow<List<com.example.data.model.OwnerAccountEntity>> = combine(repository.allOwnerAccounts, userProfile) { accounts, profile ->
        val defId = profile.defaultOwnerAccountId
        accounts.sortedWith(compareByDescending<com.example.data.model.OwnerAccountEntity> { it.id == defId }.thenBy { it.id })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allStickyNotes: StateFlow<List<StickyNoteEntity>> = repository.allStickyNotes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedStickyNotes: StateFlow<List<StickyNoteEntity>> = repository.deletedStickyNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getAttachmentsForExpense(expenseId: Int): Flow<List<com.example.data.model.AttachmentEntity>> {
        return repository.getAttachmentsForExpense(expenseId)
    }

    fun addOwnerAccount(account: com.example.data.model.OwnerAccountEntity, linkToExtras: Boolean = false) {
        viewModelScope.launch {
            if (linkToExtras) {
                val bankEntity = BankAccountEntity(
                    name = account.accountName.ifBlank { account.ownerName },
                    balance = 0.0,
                    initialBalance = 0.0,
                    details = account.details,
                    qrCodeUri = account.qrCodeUri,
                    upiId = account.upiId,
                    accountNumber = account.accountNumber,
                    ifsc = account.ifscCode,
                    branchName = account.branchName,
                    notes = account.notes,
                    phoneNumber = account.ownerMobile,
                    whatsappNumber = account.whatsappNumber,
                    email = account.email
                )
                val newBankId = repository.insertBankAccount(bankEntity).toInt()
                repository.insertOwnerAccount(account.copy(linkedBankAccountId = newBankId))
            } else {
                repository.insertOwnerAccount(account)
            }
        }
    }

    fun updateOwnerAccount(account: com.example.data.model.OwnerAccountEntity) {
        viewModelScope.launch {
            repository.updateOwnerAccount(account)
            val linkedBankId = account.linkedBankAccountId
            if (linkedBankId != null) {
                val existingBank = allBankAccounts.value.find { it.id == linkedBankId }
                if (existingBank != null) {
                    repository.updateBankAccount(
                        existingBank.copy(
                            name = account.accountName.ifBlank { existingBank.name },
                            details = account.details,
                            qrCodeUri = account.qrCodeUri,
                            upiId = account.upiId,
                            accountNumber = account.accountNumber,
                            ifsc = account.ifscCode,
                            branchName = account.branchName,
                            notes = account.notes,
                            phoneNumber = account.ownerMobile,
                            whatsappNumber = account.whatsappNumber,
                            email = account.email
                        )
                    )
                }
            }
        }
    }

    fun linkOwnerAccountToExtras(account: com.example.data.model.OwnerAccountEntity) {
        viewModelScope.launch {
            if (account.linkedBankAccountId != null) return@launch
            val bankEntity = BankAccountEntity(
                name = account.accountName.ifBlank { account.ownerName },
                balance = 0.0,
                initialBalance = 0.0,
                details = account.details,
                qrCodeUri = account.qrCodeUri,
                upiId = account.upiId,
                accountNumber = account.accountNumber,
                ifsc = account.ifscCode,
                branchName = account.branchName,
                notes = account.notes,
                phoneNumber = account.ownerMobile,
                whatsappNumber = account.whatsappNumber,
                email = account.email
            )
            val newBankId = repository.insertBankAccount(bankEntity).toInt()
            repository.updateOwnerAccount(account.copy(linkedBankAccountId = newBankId))
        }
    }

    fun delinkOwnerAccountFromExtras(
        account: com.example.data.model.OwnerAccountEntity,
        moveExtrasAccountToBin: Boolean
    ) {
        viewModelScope.launch {
            val linkedBankId = account.linkedBankAccountId
            repository.updateOwnerAccount(account.copy(linkedBankAccountId = null))
            if (moveExtrasAccountToBin && linkedBankId != null) {
                val existingBank = allBankAccounts.value.find { it.id == linkedBankId }
                if (existingBank != null) {
                    repository.updateBankAccount(existingBank.copy(isDeleted = true))
                }
                allExtraExpenses.value.filter { it.bankAccountId == linkedBankId && !it.isDeleted }.forEach { exp ->
                    repository.softDeleteExpenseAndRevert(exp, allBankAccounts.value)
                }
            }
        }
    }

    fun deleteOwnerAccount(account: com.example.data.model.OwnerAccountEntity) {
        viewModelScope.launch { repository.updateOwnerAccount(account.copy(isDeleted = true, lastUpdatedMillis = System.currentTimeMillis())) }
    }

    // Sticky Notes CRUD
    fun saveStickyNote(note: StickyNoteEntity, onSaved: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            if (note.id == 0) {
                val maxOrder = allStickyNotes.value.maxOfOrNull { it.sortOrder } ?: -1
                val newId = repository.insertStickyNote(
                    note.copy(
                        sortOrder = maxOrder + 1,
                        updatedAt = System.currentTimeMillis()
                    )
                ).toInt()
                onSaved?.invoke(newId)
            } else {
                repository.updateStickyNote(note.copy(updatedAt = System.currentTimeMillis()))
                onSaved?.invoke(note.id)
            }
        }
    }

    fun softDeleteStickyNote(note: StickyNoteEntity) {
        viewModelScope.launch {
            repository.softDeleteStickyNote(note.id)
        }
    }

    fun restoreStickyNote(note: StickyNoteEntity) {
        viewModelScope.launch {
            repository.restoreStickyNote(note.id)
        }
    }

    fun deleteStickyNotePermanently(note: StickyNoteEntity) {
        viewModelScope.launch {
            repository.deleteStickyNotePermanently(note)
        }
    }

    fun emptyDeletedStickyNotesPermanently() {
        viewModelScope.launch {
            repository.emptyDeletedStickyNotesPermanently()
        }
    }

    fun reorderStickyNotes(orderedNotes: List<StickyNoteEntity>) {
        viewModelScope.launch {
            val updated = orderedNotes.mapIndexed { index, note ->
                note.copy(sortOrder = index)
            }
            repository.insertStickyNotes(updated)
        }
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

    fun getMenuForHotel(hotelId: Int): Flow<List<MenuEntity>> = repository.getMenuForHotel(hotelId)

    val deletedMenuItems: StateFlow<List<MenuEntity>> = repository.allDeletedMenuItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Hotel Transactions
    val activeHotelTransactions: StateFlow<List<HotelTransactionEntity>> = activeHotel
        .flatMapLatest { hotel ->
            if (hotel != null) repository.getTransactionsForHotel(hotel.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val earliestTransactionTimestamp: StateFlow<Long> = activeHotelTransactions
        .map { txList -> txList.minOfOrNull { it.timestamp } ?: System.currentTimeMillis() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    // Active Month total expenditure for active hotel
    val activeMonthTotal: StateFlow<Double> = activeHotelTransactions
        .map { txList ->
            if (txList.isEmpty()) return@map 0.0
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfMonth = calendar.timeInMillis

            var total = 0.0
            for (i in txList.indices) {
                val tx = txList[i]
                if (tx.timestamp in startOfMonth..endOfMonth) {
                    total += tx.totalAmount
                }
            }
            total
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Behavioral Analytics Stream: Frequently ordered dishes
    val popularMeals: StateFlow<List<Pair<String, Int>>> = activeHotelTransactions
        .map { txList ->
            if (txList.isEmpty()) return@map emptyList()
            val counts = mutableMapOf<String, Int>()
            for (i in txList.indices) {
                val tx = txList[i]
                val items = TransactionSerializer.deserialize(tx.itemsSerialized)
                for (j in items.indices) {
                    val item = items[j]
                    counts[item.name] = (counts[item.name] ?: 0) + item.quantity
                }
            }
            counts.toList().sortedByDescending { it.second }.take(4)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


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

    fun addCustomMealGroup(customTitle: String, customAmount: Double): Boolean {
        if (customAmount <= 0.0) return false
        val safeTitle = customTitle.trim().ifBlank { "Custom Meal" }
        val customItem = OrderedItem(
            name = safeTitle,
            quantity = 1,
            price = customAmount
        )
        val newGroup = com.example.data.model.MealGroup(items = listOf(customItem))
        _committedMealGroups.value = _committedMealGroups.value + newGroup
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

    fun updateExtraExpense(
        originalExpense: ExtraExpenseEntity,
        name: String,
        amount: Double,
        bankAccountId: Int,
        timestamp: Long,
        receiptUri: String?,
        type: String = "EXPENSE",
        category: String = "General",
        notes: String = "",
        attachments: List<Pair<String, String>> = emptyList()
    ) {
        val accounts = allBankAccounts.value
        val selectedAccount = accounts.find { it.id == bankAccountId } ?: return

        viewModelScope.launch {
            // 1. Revert original expense impact on its bank account
            val oldAccount = accounts.find { it.id == originalExpense.bankAccountId }
            val revertChange = if (originalExpense.type == "INCOME") -originalExpense.amount else originalExpense.amount
            val newChange = if (type == "INCOME") amount else -amount

            if (oldAccount != null && oldAccount.id == selectedAccount.id) {
                val updatedAcc = selectedAccount.copy(balance = selectedAccount.balance + revertChange + newChange)
                repository.updateBankAccount(updatedAcc)
            } else {
                if (oldAccount != null) {
                    repository.updateBankAccount(oldAccount.copy(balance = oldAccount.balance + revertChange))
                }
                repository.updateBankAccount(selectedAccount.copy(balance = selectedAccount.balance + newChange))
            }

            // 2. Update the expense record itself
            val updatedExpense = originalExpense.copy(
                timestamp = timestamp,
                name = name,
                amount = amount,
                bankAccountId = bankAccountId,
                receiptUri = receiptUri,
                type = type,
                category = category,
                notes = notes
            )
            repository.insertExpense(updatedExpense)

            // 3. Replace attachments for this expense
            repository.deleteAttachmentsForExpense(originalExpense.id)
            attachments.forEach { (uriString, attType) ->
                val secureUri = copyToInternalStorage(uriString, attType)
                repository.insertAttachment(
                    com.example.data.model.AttachmentEntity(
                        expenseId = originalExpense.id,
                        uri = secureUri,
                        type = attType
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
                    "VOICE" -> if (uriString.endsWith(".m4a", ignoreCase = true)) "m4a" else "amr"
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

    fun importHotelViaQr(payload: ByteArray, key: ByteArray, iv: ByteArray, targetHotelId: Int = 1, targetBankAccountId: Int = 1, onResult: (com.example.data.ImportResult) -> Unit) {
        viewModelScope.launch {
            val db = com.example.data.AppDatabase.getDatabase(getApplication())
            val manager = com.example.data.QrImportManager(db.hotelDao(), db.menuDao(), db.transactionDao(), db)
            val result = manager.importPayload(payload, key, iv, targetHotelId = targetHotelId, targetBankAccountId = targetBankAccountId)
            if (result is com.example.data.ImportResult.Success && result.newHotelId > 0) {
                activeHotelId.value = result.newHotelId
            }
            onResult(result)
        }
    }

    fun importAccountViaQr(payload: ByteArray, key: ByteArray, iv: ByteArray, onResult: (com.example.data.ImportResult) -> Unit) {
        viewModelScope.launch {
            val db = com.example.data.AppDatabase.getDatabase(getApplication())
            val manager = com.example.data.QrImportManager(db.hotelDao(), db.menuDao(), db.transactionDao(), db)
            val result = manager.importPayload(payload, key, iv, targetBankAccountId = activeBankAccountId.value ?: 1)
            onResult(result)
        }
    }

    fun exportViaQr(type: com.example.data.BackupDataType, hotelId: Int = 1, bankAccountId: Int? = null, onResult: (com.example.data.ExportResult) -> Unit) {
        viewModelScope.launch {
            val db = com.example.data.AppDatabase.getDatabase(getApplication())
            val manager = com.example.data.QrExportManager(db.hotelDao(), db.menuDao(), db.transactionDao(), db)
            val result = when (type) {
                com.example.data.BackupDataType.HOTEL_WHOLE -> manager.exportWholeHotel(hotelId)
                com.example.data.BackupDataType.HOTEL_INFO -> manager.exportHotelInfo(hotelId)
                com.example.data.BackupDataType.HOTEL_MENUS -> manager.exportHotelMenus(hotelId)
                com.example.data.BackupDataType.HOTEL_HISTORY -> manager.exportHotelHistory(hotelId)
                com.example.data.BackupDataType.WALLET_WHOLE -> manager.exportWalletWhole(bankAccountId)
                com.example.data.BackupDataType.SINGLE_BANK_ACCOUNT -> manager.exportSingleBankAccount(bankAccountId ?: activeBankAccountId.value ?: 1)
                com.example.data.BackupDataType.BANK_ACCOUNTS -> manager.exportBankAccounts(bankAccountId)
                com.example.data.BackupDataType.TRANSACTION_HISTORY -> manager.exportTransactionHistory(bankAccountId)
                com.example.data.BackupDataType.ELECTRICITY_WHOLE -> manager.exportElectricityWhole()
                com.example.data.BackupDataType.METER_READINGS -> manager.exportMeterReadings()
                com.example.data.BackupDataType.UTILITY_BILLS -> manager.exportUtilityBills()
                com.example.data.BackupDataType.ELECTRICITY_RATES -> manager.exportElectricityRates()
                com.example.data.BackupDataType.SETTINGS -> manager.exportSettings()
                com.example.data.BackupDataType.FULL_BACKUP -> manager.exportFullBackup()
            }
            onResult(result)
        }
    }

    fun exportDataJson(type: com.example.data.BackupDataType, hotelId: Int = 1, bankAccountId: Int? = null, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val db = com.example.data.AppDatabase.getDatabase(getApplication())
                val dtm = com.example.data.DataTransferManager(db)
                val json = when (type) {
                    com.example.data.BackupDataType.HOTEL_WHOLE -> dtm.exportWholeHotelJson(hotelId)
                    com.example.data.BackupDataType.HOTEL_INFO -> dtm.exportHotelInfoJson(hotelId)
                    com.example.data.BackupDataType.HOTEL_MENUS -> dtm.exportHotelMenusJson(hotelId)
                    com.example.data.BackupDataType.HOTEL_HISTORY -> dtm.exportHotelHistoryJson(hotelId)
                    com.example.data.BackupDataType.WALLET_WHOLE -> dtm.exportWalletWholeJson(bankAccountId)
                    com.example.data.BackupDataType.SINGLE_BANK_ACCOUNT -> dtm.exportSingleBankAccountJson(bankAccountId ?: activeBankAccountId.value ?: 1)
                    com.example.data.BackupDataType.BANK_ACCOUNTS -> dtm.exportBankAccountsJson(bankAccountId)
                    com.example.data.BackupDataType.TRANSACTION_HISTORY -> dtm.exportTransactionHistoryJson(bankAccountId)
                    com.example.data.BackupDataType.ELECTRICITY_WHOLE -> dtm.exportElectricityWholeJson()
                    com.example.data.BackupDataType.METER_READINGS -> dtm.exportMeterReadingsJson()
                    com.example.data.BackupDataType.UTILITY_BILLS -> dtm.exportUtilityBillsJson()
                    com.example.data.BackupDataType.ELECTRICITY_RATES -> dtm.exportElectricityRatesJson()
                    com.example.data.BackupDataType.SETTINGS -> dtm.exportSettingsJson()
                    com.example.data.BackupDataType.FULL_BACKUP -> dtm.exportFullBackupJson()
                }
                onResult(json)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult("{}")
            }
        }
    }

    fun exportDataCsv(type: com.example.data.BackupDataType, hotelId: Int = 1, bankAccountId: Int? = null, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val db = com.example.data.AppDatabase.getDatabase(getApplication())
                val dtm = com.example.data.DataTransferManager(db)
                val csv = when (type) {
                    com.example.data.BackupDataType.HOTEL_WHOLE -> dtm.exportWholeHotelCsv(hotelId)
                    com.example.data.BackupDataType.HOTEL_INFO -> dtm.exportHotelInfoCsv(hotelId)
                    com.example.data.BackupDataType.HOTEL_MENUS -> dtm.exportMenusCsv(hotelId)
                    com.example.data.BackupDataType.HOTEL_HISTORY -> dtm.exportHotelHistoryCsv(hotelId)
                    com.example.data.BackupDataType.WALLET_WHOLE -> dtm.exportWalletWholeCsv(bankAccountId)
                    com.example.data.BackupDataType.SINGLE_BANK_ACCOUNT -> dtm.exportSingleBankAccountCsv(bankAccountId ?: activeBankAccountId.value ?: 1)
                    com.example.data.BackupDataType.BANK_ACCOUNTS -> dtm.exportBankAccountsCsv(bankAccountId)
                    com.example.data.BackupDataType.TRANSACTION_HISTORY -> dtm.exportTransactionHistoryCsv(bankAccountId)
                    com.example.data.BackupDataType.ELECTRICITY_WHOLE -> dtm.exportElectricityWholeCsv()
                    com.example.data.BackupDataType.METER_READINGS -> dtm.exportMeterReadingsCsv()
                    com.example.data.BackupDataType.UTILITY_BILLS -> dtm.exportUtilityBillsCsv()
                    com.example.data.BackupDataType.ELECTRICITY_RATES -> dtm.exportElectricityRatesCsv()
                    com.example.data.BackupDataType.SETTINGS -> dtm.exportSettingsCsv()
                    com.example.data.BackupDataType.FULL_BACKUP -> dtm.exportFullBackupCsv()
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

    fun importKhataTransactions(transactions: List<HotelTransactionEntity>, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                transactions.forEach { tx ->
                    repository.insertTransaction(tx)
                }
                updateWidgets()
                onResult(transactions.size)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(0)
            }
        }
    }


    // Import Analysis & Smart Sync Engine State
    private val syncEngine by lazy { com.example.utils.SyncEngine(com.example.data.AppDatabase.getDatabase(getApplication())) }

    private val _syncAnalysis = MutableStateFlow<com.example.utils.SyncAnalysisResult?>(null)
    val syncAnalysis = _syncAnalysis.asStateFlow()

    private val _isAnalyzingImport = MutableStateFlow(false)
    val isAnalyzingImport = _isAnalyzingImport.asStateFlow()

    private val _parentDecisions = MutableStateFlow<Map<String, com.example.utils.ParentResolution>>(emptyMap())
    val parentDecisions = _parentDecisions.asStateFlow()

    private val _acceptedMenuItemKeys = MutableStateFlow<Set<String>>(emptySet())
    val acceptedMenuItemKeys = _acceptedMenuItemKeys.asStateFlow()

    private val _conflictDecisions = MutableStateFlow<Map<String, com.example.utils.ConflictResolution>>(emptyMap())
    val conflictDecisions = _conflictDecisions.asStateFlow()

    private val _finalMergePlan = MutableStateFlow<com.example.utils.MergePlan?>(null)
    val finalMergePlan = _finalMergePlan.asStateFlow()

    sealed class ImportUiFlowState {
        object Idle : ImportUiFlowState()
        object Analyzing : ImportUiFlowState()
        data class ParentMappingRequired(val unmappedParents: List<com.example.utils.UnmappedParent>) : ImportUiFlowState()
        data class MenuConfirmationRequired(val pendingMenuItems: List<com.example.utils.PendingNewMenuItem>) : ImportUiFlowState()
        data class ConflictsDetected(val conflicts: List<com.example.utils.ConflictItem>) : ImportUiFlowState()
        data class ReadyToMerge(val plan: com.example.utils.MergePlan) : ImportUiFlowState()
        object Merging : ImportUiFlowState()
    }

    private val _importUiFlowState = MutableStateFlow<ImportUiFlowState>(ImportUiFlowState.Idle)
    val importUiFlowState = _importUiFlowState.asStateFlow()

    fun startSmartImportFlow(
        payloadText: String,
        targetHotelId: Int? = null,
        targetBankAccountId: Int? = null,
        explicitSenderName: String? = null,
        onDirectComplete: ((com.example.data.ImportOutcome) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                _isAnalyzingImport.value = true
                _importUiFlowState.value = ImportUiFlowState.Analyzing

                val analysis = syncEngine.analyzeImport(
                    jsonString = payloadText,
                    targetHotelId = targetHotelId,
                    targetBankAccountId = targetBankAccountId,
                    explicitSenderName = explicitSenderName
                )

                _syncAnalysis.value = analysis

                // Default all new menu items to accepted
                _acceptedMenuItemKeys.value = analysis.pendingNewMenuItems.map { it.itemKey }.toSet()

                // Default all conflicts to "Take Latest"
                _conflictDecisions.value = syncEngine.autoResolveAllLatest(analysis.conflicts)

                // Initialize parent decisions if any
                val initParentDecisions = mutableMapOf<String, com.example.utils.ParentResolution>()
                analysis.unmappedParents.forEach { parent ->
                    initParentDecisions[parent.entityKey] = com.example.utils.ParentResolution.CreateNew(analysis.senderName)
                }
                _parentDecisions.value = initParentDecisions

                _isAnalyzingImport.value = false
                transitionToNextImportFlowStep(analysis, onDirectComplete)
            } catch (e: Exception) {
                _isAnalyzingImport.value = false
                _importUiFlowState.value = ImportUiFlowState.Idle
                e.printStackTrace()
                onDirectComplete?.invoke(com.example.data.ImportOutcome.Error("Import analysis failed: ${e.message}"))
            }
        }
    }

    private fun transitionToNextImportFlowStep(
        analysis: com.example.utils.SyncAnalysisResult? = _syncAnalysis.value,
        onDirectComplete: ((com.example.data.ImportOutcome) -> Unit)? = null
    ) {
        val currAnalysis = analysis ?: run {
            _importUiFlowState.value = ImportUiFlowState.Idle
            return
        }

        // 1. If unmapped parents exist, show Parent Mapping dialog
        if (currAnalysis.unmappedParents.isNotEmpty()) {
            _importUiFlowState.value = ImportUiFlowState.ParentMappingRequired(currAnalysis.unmappedParents)
            return
        }

        // 2. If new menu items exist, show Menu Confirmation dialog
        if (currAnalysis.pendingNewMenuItems.isNotEmpty()) {
            _importUiFlowState.value = ImportUiFlowState.MenuConfirmationRequired(currAnalysis.pendingNewMenuItems)
            return
        }

        // 3. If conflicts exist, show Conflicts resolution
        if (currAnalysis.conflicts.isNotEmpty()) {
            _importUiFlowState.value = ImportUiFlowState.ConflictsDetected(currAnalysis.conflicts)
            return
        }

        // 4. Otherwise, ready to merge directly
        val plan = generateMergePlan()
        if (plan != null) {
            _importUiFlowState.value = ImportUiFlowState.ReadyToMerge(plan)
            // If onDirectComplete is provided and no UI steps were required, execute directly
            if (onDirectComplete != null) {
                executeMergePlan(plan, onDirectComplete)
            }
        } else {
            _importUiFlowState.value = ImportUiFlowState.Idle
        }
    }

    fun setParentResolution(entityKey: String, resolution: com.example.utils.ParentResolution) {
        val updated = _parentDecisions.value.toMutableMap()
        updated[entityKey] = resolution
        _parentDecisions.value = updated
        recalculateWithDecisions()
    }

    fun recalculateWithDecisions() {
        val currentAnalysis = _syncAnalysis.value ?: return
        viewModelScope.launch {
            try {
                val updatedAnalysis = syncEngine.resolveParentDecisions(
                    previousAnalysis = currentAnalysis,
                    parentDecisions = _parentDecisions.value
                )
                _syncAnalysis.value = updatedAnalysis

                // Preserve or update conflict decisions
                val latestConflictMap = syncEngine.autoResolveAllLatest(updatedAnalysis.conflicts)
                val mergedConflicts = latestConflictMap.toMutableMap()
                _conflictDecisions.value.forEach { (k, v) ->
                    if (mergedConflicts.containsKey(k)) {
                        mergedConflicts[k] = v
                    }
                }
                _conflictDecisions.value = mergedConflicts

                // Update accepted menu item keys
                val newKeys = updatedAnalysis.pendingNewMenuItems.map { it.itemKey }.toSet()
                _acceptedMenuItemKeys.value = _acceptedMenuItemKeys.value.intersect(newKeys).ifEmpty { newKeys }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun confirmParentMappings(onDirectComplete: ((com.example.data.ImportOutcome) -> Unit)? = null) {
        val currAnalysis = _syncAnalysis.value ?: return
        if (currAnalysis.pendingNewMenuItems.isNotEmpty()) {
            _importUiFlowState.value = ImportUiFlowState.MenuConfirmationRequired(currAnalysis.pendingNewMenuItems)
        } else if (currAnalysis.conflicts.isNotEmpty()) {
            _importUiFlowState.value = ImportUiFlowState.ConflictsDetected(currAnalysis.conflicts)
        } else {
            val plan = generateMergePlan()
            if (plan != null) {
                _importUiFlowState.value = ImportUiFlowState.ReadyToMerge(plan)
                if (onDirectComplete != null) {
                    executeMergePlan(plan, onDirectComplete)
                }
            } else {
                _importUiFlowState.value = ImportUiFlowState.Idle
            }
        }
    }

    fun setMenuItemAccepted(itemKey: String, accepted: Boolean) {
        val current = _acceptedMenuItemKeys.value.toMutableSet()
        if (accepted) {
            current.add(itemKey)
        } else {
            current.remove(itemKey)
        }
        _acceptedMenuItemKeys.value = current
    }

    fun setAllMenuItemsAccepted(accepted: Boolean) {
        val currentAnalysis = _syncAnalysis.value ?: return
        if (accepted) {
            _acceptedMenuItemKeys.value = currentAnalysis.pendingNewMenuItems.map { it.itemKey }.toSet()
        } else {
            _acceptedMenuItemKeys.value = emptySet()
        }
    }

    fun confirmMenuItems(onDirectComplete: ((com.example.data.ImportOutcome) -> Unit)? = null) {
        val currAnalysis = _syncAnalysis.value ?: return
        if (currAnalysis.conflicts.isNotEmpty()) {
            _importUiFlowState.value = ImportUiFlowState.ConflictsDetected(currAnalysis.conflicts)
        } else {
            val plan = generateMergePlan()
            if (plan != null) {
                _importUiFlowState.value = ImportUiFlowState.ReadyToMerge(plan)
                if (onDirectComplete != null) {
                    executeMergePlan(plan, onDirectComplete)
                }
            } else {
                _importUiFlowState.value = ImportUiFlowState.Idle
            }
        }
    }

    fun setConflictResolution(conflictKey: String, resolution: com.example.utils.ConflictResolution) {
        val current = _conflictDecisions.value.toMutableMap()
        current[conflictKey] = resolution
        _conflictDecisions.value = current
    }

    fun resolveAllConflictsLatest() {
        val currentAnalysis = _syncAnalysis.value ?: return
        _conflictDecisions.value = syncEngine.autoResolveAllLatest(currentAnalysis.conflicts)
    }

    fun resolveAllConflictsLocal() {
        val currentAnalysis = _syncAnalysis.value ?: return
        _conflictDecisions.value = syncEngine.autoResolveAllLocal(currentAnalysis.conflicts)
    }

    fun generateMergePlan(): com.example.utils.MergePlan? {
        val currentAnalysis = _syncAnalysis.value ?: return null
        val plan = syncEngine.buildFinalMergePlan(
            analysis = currentAnalysis,
            acceptedMenuItemKeys = _acceptedMenuItemKeys.value,
            conflictResolutions = _conflictDecisions.value
        )
        _finalMergePlan.value = plan
        return plan
    }

    fun executeMergePlan(
        plan: com.example.utils.MergePlan,
        onResult: (com.example.data.ImportOutcome) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _importUiFlowState.value = ImportUiFlowState.Merging
                syncEngine.executeMergePlan(plan)
                _importUiFlowState.value = ImportUiFlowState.Idle
                clearSyncAnalysis()
                val totalInserted = plan.hotelsToInsert.size + plan.bankAccountsToInsert.size + plan.ownerAccountsToInsert.size + plan.menusToInsert.size + plan.mealsToInsert.size + plan.hotelPaymentsToInsert.size + plan.expensesToInsert.size + plan.readingsToInsert.size + plan.billsToInsert.size + plan.stickyNotesToInsert.size
                val totalUpdated = plan.hotelsToUpdate.size + plan.bankAccountsToUpdate.size + plan.menusToUpdate.size + plan.mealsToUpdate.size + plan.expensesToUpdate.size + plan.readingsToUpdate.size + plan.billsToUpdate.size
                updateWidgets()
                onResult(com.example.data.ImportOutcome.Success("Data synchronized successfully! ($totalInserted new records added, $totalUpdated records updated)", count = totalInserted + totalUpdated))
            } catch (e: Exception) {
                _importUiFlowState.value = ImportUiFlowState.Idle
                e.printStackTrace()
                onResult(com.example.data.ImportOutcome.Error("Import error: ${e.message}"))
            }
        }
    }

    fun cancelImportFlow() {
        _importUiFlowState.value = ImportUiFlowState.Idle
        clearSyncAnalysis()
    }

    fun analyzeImportPayload(
        jsonString: String,
        targetHotelId: Int? = null,
        targetBankAccountId: Int? = null,
        explicitSenderName: String? = null,
        onResult: (com.example.utils.SyncAnalysisResult) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isAnalyzingImport.value = true
                val result = syncEngine.analyzeImport(
                    jsonString = jsonString,
                    targetHotelId = targetHotelId,
                    targetBankAccountId = targetBankAccountId,
                    explicitSenderName = explicitSenderName
                )
                _syncAnalysis.value = result
                _isAnalyzingImport.value = false
                onResult(result)
            } catch (e: Exception) {
                _isAnalyzingImport.value = false
                e.printStackTrace()
            }
        }
    }

    fun clearSyncAnalysis() {
        _syncAnalysis.value = null
        _parentDecisions.value = emptyMap()
        _acceptedMenuItemKeys.value = emptySet()
        _conflictDecisions.value = emptyMap()
        _finalMergePlan.value = null
    }

    private val _qrExportPayload = MutableStateFlow<com.example.data.ExportResult.Success?>(null)
    val qrExportPayload = _qrExportPayload.asStateFlow()

    private val _qrExportSessionId = MutableStateFlow<String?>(null)
    val qrExportSessionId = _qrExportSessionId.asStateFlow()

    private val _qrExportItemName = MutableStateFlow<String?>(null)
    val qrExportItemName = _qrExportItemName.asStateFlow()

    fun setQrExportPayload(payload: com.example.data.ExportResult.Success?, sessionId: String?, itemName: String? = null) {
        _qrExportPayload.value = payload
        _qrExportSessionId.value = sessionId
        _qrExportItemName.value = itemName
    }

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedSearchFilter = MutableStateFlow("All")
    val selectedSearchFilter = _selectedSearchFilter.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory = _searchHistory.asStateFlow()

    @Suppress("UNCHECKED_CAST")
    val searchResults: StateFlow<List<com.example.data.model.SearchResult>> = combine(
        _searchQuery,
        _selectedSearchFilter,
        allHotels,
        allExtraExpenses,
        allBankAccounts,
        allTransactions,
        allStickyNotes
    ) { args: Array<Any?> ->
        val query = args[0] as String
        val filter = args[1] as String
        val hotels = args[2] as List<HotelEntity>
        val expenses = args[3] as List<ExtraExpenseEntity>
        val accounts = args[4] as List<BankAccountEntity>
        val txs = args[5] as List<HotelTransactionEntity>
        val stickies = args[6] as List<StickyNoteEntity>
        if (query.isBlank()) return@combine emptyList()
        val q = query.trim().lowercase()

        val results = mutableListOf<com.example.data.model.SearchResult>()

        if (filter == "All" || filter == "Hotels") {
            for (i in hotels.indices) {
                val h = hotels[i]
                if (!h.isDeleted && (h.name.lowercase().contains(q) || h.location.lowercase().contains(q) || h.phoneNumber.contains(q) || h.notes.lowercase().contains(q))) {
                    results.add(
                        com.example.data.model.SearchResult(
                            id = h.id,
                            title = h.name,
                            type = "Hotel",
                            subtitle = if (h.location.isNotBlank()) h.location else h.phoneNumber.ifBlank { h.notes },
                            hotelId = h.id
                        )
                    )
                }
            }
        }

        if (filter == "All" || filter == "Expenses") {
            for (i in expenses.indices) {
                val e = expenses[i]
                if (!e.isDeleted && (e.name.lowercase().contains(q) || e.category.lowercase().contains(q) || e.notes.lowercase().contains(q))) {
                    results.add(
                        com.example.data.model.SearchResult(
                            id = e.id,
                            title = e.name,
                            type = if (e.type == "INCOME") "Income" else "Expense",
                            subtitle = e.category,
                            amount = e.amount,
                            category = e.category
                        )
                    )
                }
            }
        }

        if (filter == "All" || filter == "Accounts") {
            for (i in accounts.indices) {
                val a = accounts[i]
                if (!a.isDeleted && (a.name.lowercase().contains(q) || a.accountNumber.contains(q) || a.branchName.lowercase().contains(q) || a.notes.lowercase().contains(q))) {
                    results.add(
                        com.example.data.model.SearchResult(
                            id = a.id,
                            title = a.name,
                            type = "Account",
                            subtitle = if (a.accountNumber.isNotBlank()) "Acc: ${a.accountNumber}" else a.notes.ifBlank { "Bank Account" },
                            amount = a.balance
                        )
                    )
                }
            }
        }

        if (filter == "All" || filter == "Meals & Menus") {
            for (i in txs.indices) {
                val tx = txs[i]
                if (!tx.isDeleted) {
                    val items = TransactionSerializer.deserialize(tx.itemsSerialized)
                    val matched = items.find { it.name.lowercase().contains(q) }
                    if (matched != null) {
                        results.add(
                            com.example.data.model.SearchResult(
                                id = tx.id,
                                title = matched.name,
                                type = "Meal",
                                subtitle = "Qty: ${matched.quantity}",
                                amount = tx.totalAmount,
                                hotelId = tx.hotelId
                            )
                        )
                    }
                }
            }
        }

        if (filter == "All" || filter == "Sticky") {
            for (i in stickies.indices) {
                val sn = stickies[i]
                if (!sn.isDeleted && (sn.title.lowercase().contains(q) || sn.subtitle.lowercase().contains(q) || sn.bodyText.lowercase().contains(q))) {
                    val displayTitle = sn.title.ifBlank { sn.subtitle.ifBlank { sn.bodyText.lines().firstOrNull()?.take(40) ?: "Sticky Note" } }
                    val displaySub = sn.subtitle.ifBlank { sn.bodyText.replace("\n", " ").take(60) }
                    results.add(
                        com.example.data.model.SearchResult(
                            id = sn.id,
                            title = displayTitle,
                            type = "Sticky",
                            subtitle = displaySub
                        )
                    )
                }
            }
        }

        results
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
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
    fun addHotel(
        name: String,
        advancedPayment: Double,
        phoneNumber: String,
        location: String,
        upiId: String,
        qrCodeUri: String,
        notes: String = "",
        whatsappNumber: String = ""
    ) = viewModelScope.launch {
        repository.insertHotel(
            com.example.data.model.HotelEntity(
                name = name,
                advancedPayment = advancedPayment,
                phoneNumber = phoneNumber,
                location = location,
                upiId = upiId,
                qrCodeUri = qrCodeUri,
                notes = notes,
                whatsappNumber = whatsappNumber
            )
        )
    }
    fun editHotel(hotel: com.example.data.model.HotelEntity) = viewModelScope.launch { repository.updateHotel(hotel) }
    fun deleteHotel(hotel: com.example.data.model.HotelEntity) = viewModelScope.launch {
        repository.updateHotel(hotel.copy(isDeleted = true, lastUpdatedMillis = System.currentTimeMillis()))
    }
    fun addVirtualAccount(
        name: String,
        balance: Double,
        details: String = "",
        qrCodeUri: String = "",
        upiId: String = "",
        accountNumber: String = "",
        ifsc: String = "",
        branchName: String = "",
        notes: String = "",
        phoneNumber: String = "",
        whatsappNumber: String = "",
        email: String = ""
    ) = viewModelScope.launch {
        repository.insertBankAccount(
            com.example.data.model.BankAccountEntity(
                name = name,
                balance = balance,
                initialBalance = balance,
                details = details,
                upiId = upiId,
                qrCodeUri = qrCodeUri,
                accountNumber = accountNumber,
                ifsc = ifsc,
                branchName = branchName,
                notes = notes,
                phoneNumber = phoneNumber,
                whatsappNumber = whatsappNumber,
                email = email
            )
        )
    }
    fun editVirtualAccount(account: com.example.data.model.BankAccountEntity) = viewModelScope.launch {
        repository.updateBankAccount(account)
        val linkedOwner = allOwnerAccounts.value.find { it.linkedBankAccountId == account.id && !it.isDeleted }
        if (linkedOwner != null) {
            repository.updateOwnerAccount(
                linkedOwner.copy(
                    accountName = account.name,
                    details = account.details,
                    qrCodeUri = account.qrCodeUri,
                    upiId = account.upiId,
                    accountNumber = account.accountNumber,
                    ifscCode = account.ifsc,
                    branchName = account.branchName,
                    notes = account.notes,
                    ownerMobile = account.phoneNumber.ifBlank { linkedOwner.ownerMobile },
                    whatsappNumber = account.whatsappNumber,
                    email = account.email
                )
            )
        }
    }
    fun deleteVirtualAccount(account: com.example.data.model.BankAccountEntity) = viewModelScope.launch { repository.updateBankAccount(account.copy(isDeleted = true)) }
    fun addMenu(menu: com.example.data.model.MenuEntity) = viewModelScope.launch { repository.insertMenu(menu) }
    fun updateMenu(menu: com.example.data.model.MenuEntity, effectiveDateMillis: Long = System.currentTimeMillis()) = viewModelScope.launch {
        val oldItem = allHotelMenus.value.find { it.id == menu.id } ?: allMenuItems.value.find { it.id == menu.id }
        if (oldItem != null) {
            val oldName = oldItem.englishName.ifBlank { oldItem.banglaName }
            val newName = menu.englishName.ifBlank { menu.banglaName }
            val priceChanged = kotlin.math.abs(oldItem.price - menu.price) > 0.001
            val nameChanged = oldName != newName || oldItem.banglaName != menu.banglaName
            val typeChanged = oldItem.type != menu.type
            if (priceChanged || nameChanged || typeChanged) {
                val changes = mutableListOf<String>()
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                val effectiveStr = runCatching { sdf.format(java.util.Date(effectiveDateMillis)) }.getOrDefault("")
                if (priceChanged) changes.add("Price: ₹${oldItem.price.toInt()} → ₹${menu.price.toInt()} (Effective: $effectiveStr)")
                if (nameChanged) changes.add("Name: $oldName → $newName")
                if (typeChanged) changes.add("Type: ${oldItem.type} → ${menu.type}")
                repository.insertMenuHistory(
                    com.example.data.model.MenuModificationHistoryEntity(
                        menuItemId = menu.id,
                        hotelId = menu.hotelId,
                        itemName = newName.ifBlank { "Menu Item #${menu.id}" },
                        oldPrice = oldItem.price,
                        newPrice = menu.price,
                        oldType = oldItem.type,
                        newType = menu.type,
                        changeSummary = changes.joinToString(" • "),
                        modifiedAt = effectiveDateMillis
                    )
                )
            }
        }
        repository.updateMenu(menu.copy(lastUpdatedMillis = effectiveDateMillis))
    }
    fun deleteMenu(menu: com.example.data.model.MenuEntity) = viewModelScope.launch {
        repository.updateMenu(menu.copy(isDeleted = true, lastUpdatedMillis = System.currentTimeMillis()))
    }
    fun getHistoryForMenuItem(menuItemId: Int): Flow<List<com.example.data.model.MenuModificationHistoryEntity>> =
        repository.getHistoryForMenuItem(menuItemId)
    fun getHistoryForHotel(hotelId: Int): Flow<List<com.example.data.model.MenuModificationHistoryEntity>> =
        repository.getHistoryForHotel(hotelId)
    fun deleteExtraExpense(expense: com.example.data.model.ExtraExpenseEntity) = viewModelScope.launch { repository.softDeleteExpenseAndRevert(expense, allBankAccounts.value) }
    fun addCategory(name: String, type: String) = viewModelScope.launch { repository.insertCategory(com.example.data.model.TransactionCategoryEntity(name = name, type = type)) }

    // Bin Restore
    fun restoreHotel(item: com.example.data.model.HotelEntity) = viewModelScope.launch { repository.updateHotel(item.copy(isDeleted = false)) }
    fun restoreVirtualAccount(item: com.example.data.model.BankAccountEntity) = viewModelScope.launch { repository.updateBankAccount(item.copy(isDeleted = false)) }
    fun restoreOwnerAccount(item: com.example.data.model.OwnerAccountEntity) = viewModelScope.launch { repository.updateOwnerAccount(item.copy(isDeleted = false)) }
    fun restoreMenu(item: com.example.data.model.MenuEntity) = viewModelScope.launch { repository.updateMenu(item.copy(isDeleted = false)) }
    fun restoreExtraExpense(item: com.example.data.model.ExtraExpenseEntity) = viewModelScope.launch { repository.restoreExpenseAndApply(item, allBankAccounts.value) }
    fun restoreMeterReading(item: com.example.data.model.MeterReading) = viewModelScope.launch { repository.restoreReading(item.id) }
    fun restoreUtilityBill(item: com.example.data.model.UtilityBill) = viewModelScope.launch { repository.restoreUtilityBill(item.id) }
    fun restoreElectricityRate(item: com.example.data.model.ElectricityRate) = viewModelScope.launch { repository.restoreElectricityRate(item.id) }

    // Bin Hard Delete
    fun deleteHotelPermanently(item: com.example.data.model.HotelEntity) = viewModelScope.launch { repository.deleteHotel(item) }
    fun deleteVirtualAccountPermanently(item: com.example.data.model.BankAccountEntity) = viewModelScope.launch { repository.deleteBankAccount(item) }
    fun deleteOwnerAccountPermanently(item: com.example.data.model.OwnerAccountEntity) = viewModelScope.launch { repository.deleteOwnerAccount(item) }
    fun deleteMenuPermanently(item: com.example.data.model.MenuEntity) = viewModelScope.launch { repository.deleteMenu(item) }
    fun deleteExtraExpensePermanently(item: com.example.data.model.ExtraExpenseEntity) = viewModelScope.launch { repository.deleteExpensePermanently(item) }
    fun deleteUtilityBillPermanently(item: com.example.data.model.UtilityBill) = viewModelScope.launch { repository.deleteUtilityBill(item) }
    fun deleteElectricityRatePermanently(item: com.example.data.model.ElectricityRate) = viewModelScope.launch { repository.deleteElectricityRate(item) }
    fun deleteReading(item: com.example.data.model.MeterReading) = viewModelScope.launch { repository.deleteReading(item) }

    // Bin Empty All
    fun emptyDeletedHotelsPermanently() = viewModelScope.launch {
        deletedHotels.value.forEach { hotel -> repository.deleteHotel(hotel) }
    }
    fun emptyDeletedBankAccountsPermanently() = viewModelScope.launch {
        deletedBankAccounts.value.forEach { account ->
            repository.deleteBankAccount(account)
        }
        deletedOwnerAccounts.value.forEach { ownerAcc ->
            repository.deleteOwnerAccount(ownerAcc)
        }
    }
    fun emptyDeletedMenuItemsPermanently() = viewModelScope.launch { repository.emptyDeletedMenuItemsPermanently() }
    fun emptyDeletedTransactionsPermanently() = viewModelScope.launch { repository.emptyDeletedTransactionsPermanently() }
    fun emptyDeletedExpensesPermanently() = viewModelScope.launch { repository.emptyDeletedExpensesPermanently() }
    fun emptyDeletedReadingsPermanently() = viewModelScope.launch { repository.emptyDeletedReadingsPermanently() }
    fun emptyDeletedUtilityBillsPermanently() = viewModelScope.launch { repository.emptyDeletedUtilityBillsPermanently() }
    fun emptyDeletedElectricityRatesPermanently() = viewModelScope.launch { repository.emptyDeletedElectricityRatesPermanently() }

    // Profiles
    fun saveUserProfile(profile: com.example.data.model.UserProfileEntity) = viewModelScope.launch {
        com.example.ui.theme.ThemePreferences.save(getApplication(), profile)
        repository.saveUserProfile(profile)
    }

    fun updateProfile(name: String, contact: String, avatar: String) = viewModelScope.launch {
        val current = userProfile.value
        val updated = current.copy(name = name, contact = contact, avatar = avatar)
        com.example.ui.theme.ThemePreferences.save(getApplication(), updated)
        repository.saveUserProfile(updated)
    }
    
    fun updateCustomisation(mealGroupPosition: String, bgPreset: String, customBgUri: String) = viewModelScope.launch {
        val current = userProfile.value
        val updated = current.copy(mealGroupPosition = mealGroupPosition, bgPreset = bgPreset, customBgUri = customBgUri)
        com.example.ui.theme.ThemePreferences.save(getApplication(), updated)
        repository.saveUserProfile(updated)
    }
    
    fun saveHotelPayment(payment: HotelPaymentEntity) = viewModelScope.launch {
        repository.insertHotelPayment(payment)
        com.example.widget.WidgetUpdater.updateAllWidgets(getApplication<Application>())
    }

    fun deleteHotelPayment(payment: HotelPaymentEntity) = viewModelScope.launch {
        repository.deleteHotelPayment(payment)
        com.example.widget.WidgetUpdater.updateAllWidgets(getApplication<Application>())
    }

    fun setHotelMonthlyPayment(
        hotelId: Int,
        year: Int,
        month: Int,
        amountPaid: Double,
        paymentDate: Long,
        note: String,
        isPaid: Boolean
    ) = viewModelScope.launch {
        val existing = allHotelPayments.value.find {
            it.hotelId == hotelId && it.year == year && it.month == month && !it.isDeleted
        }
        if (!isPaid) {
            if (existing != null) {
                repository.deleteHotelPayment(existing)
            }
        } else {
            val payment = (existing ?: HotelPaymentEntity(
                hotelId = hotelId,
                year = year,
                month = month,
                amountPaid = amountPaid,
                paymentDate = paymentDate,
                note = note
            )).copy(
                amountPaid = amountPaid,
                paymentDate = paymentDate,
                note = note
            )
            repository.insertHotelPayment(payment)
        }
        com.example.widget.WidgetUpdater.updateAllWidgets(getApplication<Application>())
    }

    fun setDefaultHotel(hotelId: Int) = viewModelScope.launch {
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        val updated = current.copy(defaultHotelId = hotelId)
        com.example.ui.theme.ThemePreferences.save(getApplication(), updated)
        repository.saveUserProfile(updated)
        activeHotelId.value = hotelId
        com.example.widget.WidgetUpdater.updateAllWidgets(getApplication<Application>())
    }

    fun setDefaultBankAccount(accountId: Int) = viewModelScope.launch {
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        val updated = current.copy(defaultBankAccountId = accountId)
        com.example.ui.theme.ThemePreferences.save(getApplication(), updated)
        repository.saveUserProfile(updated)
        activeBankAccountId.value = accountId
        com.example.widget.WidgetUpdater.updateAllWidgets(getApplication<Application>())
    }

    fun setDefaultOwnerAccount(accountId: Int) = viewModelScope.launch {
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        val updated = current.copy(defaultOwnerAccountId = accountId)
        com.example.ui.theme.ThemePreferences.save(getApplication(), updated)
        repository.saveUserProfile(updated)
        com.example.widget.WidgetUpdater.updateAllWidgets(getApplication<Application>())
    }

    fun updateAppVersion(newVersion: String) = viewModelScope.launch {
        val clean = newVersion.trim().ifBlank { "2.4.0" }
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        val updated = current.copy(appVersion = clean)
        com.example.ui.theme.ThemePreferences.save(getApplication(), updated)
        repository.saveUserProfile(updated)
    }

    fun updateDefaultOwnerAccount(accountId: Int) {
        setDefaultOwnerAccount(accountId)
    }

    fun updateVisibleBankAccountsOnExtra(visibleAccountIds: String) = viewModelScope.launch {
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        repository.saveUserProfile(current.copy(visibleBankAccountsOnExtra = visibleAccountIds))
    }

    fun toggleLiquidGlassTheme(enabled: Boolean) = viewModelScope.launch {
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        val updated = current.copy(
            enableLiquidGlassTheme = enabled,
            disableHeavyEffects = if (enabled) false else current.disableHeavyEffects
        )
        com.example.ui.theme.ThemePreferences.save(getApplication(), updated)
        repository.saveUserProfile(updated)
    }

    fun toggleDisableHeavyEffects(disabled: Boolean) = viewModelScope.launch {
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        val updated = current.copy(
            disableHeavyEffects = disabled,
            enableLiquidGlassTheme = if (disabled) false else current.enableLiquidGlassTheme,
            enableStarAnimation = if (disabled) false else current.enableStarAnimation
        )
        com.example.ui.theme.ThemePreferences.save(getApplication(), updated)
        repository.saveUserProfile(updated)
    }

    fun toggleBoldText(enabled: Boolean) = viewModelScope.launch {
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        val updated = current.copy(isBoldTextEnabled = enabled)
        com.example.ui.theme.ThemePreferences.save(getApplication(), updated)
        repository.saveUserProfile(updated)
    }

    fun updateStickyPreferences(widthPercent: Int, previewLines: Int) = viewModelScope.launch {
        val current = userProfile.value ?: com.example.data.model.UserProfileEntity()
        val updated = current.copy(
            stickyCardWidthPercent = widthPercent.coerceIn(50, 80),
            stickyPreviewLines = previewLines.coerceIn(4, 10)
        )
        com.example.ui.theme.ThemePreferences.save(getApplication(), updated)
        repository.saveUserProfile(updated)
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
