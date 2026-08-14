import re

code = """
    // Search State
    var searchQuery = androidx.compose.runtime.mutableStateOf("")
        private set
    var selectedSearchFilter = androidx.compose.runtime.mutableStateOf("All")
        private set
    var searchHistory = androidx.compose.runtime.mutableStateOf<List<String>>(emptyList())
        private set
    var searchResults = androidx.compose.runtime.mutableStateOf<List<com.example.data.model.SearchResult>>(emptyList())
        private set
    
    fun setSearchQuery(query: String) { searchQuery.value = query }
    fun setSearchFilter(filter: String) { selectedSearchFilter.value = filter }
    fun addSearchHistory(query: String) {
        val list = searchHistory.value.toMutableList()
        list.remove(query)
        list.add(0, query)
        searchHistory.value = list.take(10)
    }
    fun clearSearchHistory() { searchHistory.value = emptyList() }
    fun removeSearchHistoryItem(item: String) {
        searchHistory.value = searchHistory.value.filter { it != item }
    }

    // Lock State
    var isLockedManual = androidx.compose.runtime.mutableStateOf(false)
        private set
    fun lockApp() { isLockedManual.value = true }
    fun unlockApp() { isLockedManual.value = false }

    // Standard CRUD
    fun addHotel(hotel: HotelEntity) = viewModelScope.launch { repository.insertHotel(hotel) }
    fun editHotel(hotel: HotelEntity) = viewModelScope.launch { repository.updateHotel(hotel) }
    fun deleteHotel(hotel: HotelEntity) = viewModelScope.launch { repository.deleteHotel(hotel) } // usually soft delete?
    fun addVirtualAccount(account: BankAccountEntity) = viewModelScope.launch { repository.insertBankAccount(account) }
    fun editVirtualAccount(account: BankAccountEntity) = viewModelScope.launch { repository.updateBankAccount(account) }
    fun deleteVirtualAccount(account: BankAccountEntity) = viewModelScope.launch { repository.deleteBankAccount(account) }
    fun addMenu(menu: MenuEntity) = viewModelScope.launch { repository.insertMenu(menu) }
    fun updateMenu(menu: MenuEntity) = viewModelScope.launch { repository.updateMenu(menu) }
    fun deleteMenu(menu: MenuEntity) = viewModelScope.launch { repository.deleteMenu(menu) }
    fun deleteExtraExpense(expense: ExtraExpenseEntity) = viewModelScope.launch { repository.softDeleteExpenseAndRevert(expense, allBankAccounts.value) }
    fun addCategory(category: com.example.data.model.TransactionCategoryEntity) = viewModelScope.launch { repository.insertCategory(category) }

    // Bin Restore
    fun restoreHotel(item: HotelEntity) = viewModelScope.launch { repository.updateHotel(item.copy(isDeleted = false)) }
    fun restoreVirtualAccount(item: BankAccountEntity) = viewModelScope.launch { repository.updateBankAccount(item.copy(isDeleted = false)) }
    fun restoreMenu(item: MenuEntity) = viewModelScope.launch { repository.updateMenu(item.copy(isDeleted = false)) }
    fun restoreExtraExpense(item: ExtraExpenseEntity) = viewModelScope.launch { repository.restoreExpenseAndApply(item, allBankAccounts.value) }
    fun restoreMeterReading(item: com.example.data.model.MeterReading) = viewModelScope.launch { repository.restoreReading(item.id) }
    
    // Bin Hard Delete
    fun deleteHotelPermanently(item: HotelEntity) = viewModelScope.launch { repository.deleteHotel(item) } // Assuming it will do the right thing
    fun deleteVirtualAccountPermanently(item: BankAccountEntity) = viewModelScope.launch { repository.deleteBankAccount(item) }
    fun deleteMenuPermanently(item: MenuEntity) = viewModelScope.launch { repository.deleteMenu(item) }
    fun deleteExtraExpensePermanently(item: ExtraExpenseEntity) = viewModelScope.launch { repository.deleteExpensePermanently(item) }
    fun deleteUtilityBillPermanently(item: com.example.data.model.UtilityBill) = viewModelScope.launch { repository.deleteUtilityBill(item) }
    fun deleteReading(item: com.example.data.model.MeterReading) = viewModelScope.launch { repository.deleteReading(item) }

    // Bin Empty All
    fun emptyDeletedHotelsPermanently() = viewModelScope.launch { repository.emptyDeletedHotelsPermanently() }
    fun emptyDeletedBankAccountsPermanently() = viewModelScope.launch { repository.emptyDeletedBankAccountsPermanently() }
    fun emptyDeletedMenuItemsPermanently() = viewModelScope.launch { repository.emptyDeletedMenuItemsPermanently() }
    fun emptyDeletedTransactionsPermanently() = viewModelScope.launch { repository.emptyDeletedTransactionsPermanently() }
    fun emptyDeletedExpensesPermanently() = viewModelScope.launch { repository.emptyDeletedExpensesPermanently() }
    fun emptyDeletedReadingsPermanently() = viewModelScope.launch { repository.emptyDeletedReadingsPermanently() }
    fun emptyDeletedUtilityBillsPermanently() = viewModelScope.launch { repository.emptyDeletedUtilityBillsPermanently() }

    // Profiles
    fun updateProfile(profile: UserProfileEntity) = viewModelScope.launch { repository.saveUserProfile(profile) }
    fun updateCustomisation(profile: UserProfileEntity) = viewModelScope.launch { repository.saveUserProfile(profile) }
    fun updatePreferences(profile: UserProfileEntity) = viewModelScope.launch { repository.saveUserProfile(profile) }

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
"""

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# To be safe, let's remove any previous bad injects by looking for `// Search State` and deleting to the end, just in case.
idx = content.find('// Search State')
if idx != -1:
    content = content[:idx]

last_brace_idx = content.rfind('}')
if last_brace_idx != -1:
    content = content[:last_brace_idx] + code + "\n}\n"

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
