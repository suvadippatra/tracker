import re

code = """
    var searchQuery = androidx.compose.runtime.mutableStateOf("")
        private set
    var selectedSearchFilter = androidx.compose.runtime.mutableStateOf("All")
        private set
    var searchHistory = androidx.compose.runtime.mutableStateOf<List<String>>(emptyList())
        private set
    var searchResults = androidx.compose.runtime.mutableStateOf<List<SearchResult>>(emptyList())
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

    var isLockedManual = androidx.compose.runtime.mutableStateOf(false)
        private set
    fun lockApp() { isLockedManual.value = true }
    fun unlockApp() { isLockedManual.value = false }

    fun addHotel(hotel: HotelEntity) = viewModelScope.launch { repository.insertHotel(hotel) }
    fun editHotel(hotel: HotelEntity) = viewModelScope.launch { repository.updateHotel(hotel) }
    fun deleteHotel(hotel: HotelEntity) = viewModelScope.launch { repository.deleteHotel(hotel) }

    fun addVirtualAccount(account: BankAccountEntity) = viewModelScope.launch { repository.insertBankAccount(account) }
    fun editVirtualAccount(account: BankAccountEntity) = viewModelScope.launch { repository.updateBankAccount(account) }
    fun deleteVirtualAccount(account: BankAccountEntity) = viewModelScope.launch { repository.deleteBankAccount(account) }

    fun addMenu(menu: MenuEntity) = viewModelScope.launch { repository.insertMenu(menu) }
    fun updateMenu(menu: MenuEntity) = viewModelScope.launch { repository.updateMenu(menu) }
    fun deleteMenu(menu: MenuEntity) = viewModelScope.launch { repository.deleteMenu(menu) }

    fun deleteExtraExpense(expense: ExtraExpenseEntity) = viewModelScope.launch { repository.softDeleteExpenseAndRevert(expense, accounts.value) }

    fun addCategory(category: com.example.data.model.TransactionCategoryEntity) = viewModelScope.launch { repository.insertCategory(category) }

    fun restoreHotel(id: Int) = viewModelScope.launch { repository.restoreHotel(id) }
    fun restoreVirtualAccount(id: Int) = viewModelScope.launch { repository.restoreVirtualAccount(id) }
    fun restoreMenu(id: Int) = viewModelScope.launch { repository.restoreMenu(id) }
    fun restoreExtraExpense(id: Int) = viewModelScope.launch { repository.restoreExtraExpense(id) }
    fun restoreMeterReading(id: String) = viewModelScope.launch { repository.restoreReading(id) }

    fun deleteHotelPermanently(id: Int) = viewModelScope.launch { repository.deleteHotelPermanently(id) }
    fun deleteVirtualAccountPermanently(id: Int) = viewModelScope.launch { repository.deleteVirtualAccountPermanently(id) }
    fun deleteMenuPermanently(id: Int) = viewModelScope.launch { repository.deleteMenuPermanently(id) }
    fun deleteExtraExpensePermanently(id: Int) = viewModelScope.launch { repository.deleteExpensePermanently(id) }
    fun deleteUtilityBillPermanently(id: String) = viewModelScope.launch { repository.deleteUtilityBillPermanently(id) }
    fun deleteReading(id: String) = viewModelScope.launch { repository.deleteReadingPermanently(id) }

    fun emptyDeletedHotelsPermanently() = viewModelScope.launch { repository.emptyDeletedHotelsPermanently() }
    fun emptyDeletedBankAccountsPermanently() = viewModelScope.launch { repository.emptyDeletedBankAccountsPermanently() }
    fun emptyDeletedMenuItemsPermanently() = viewModelScope.launch { repository.emptyDeletedMenuItemsPermanently() }
    fun emptyDeletedTransactionsPermanently() = viewModelScope.launch { repository.emptyDeletedTransactionsPermanently() }
    fun emptyDeletedExpensesPermanently() = viewModelScope.launch { repository.emptyDeletedExpensesPermanently() }
    fun emptyDeletedReadingsPermanently() = viewModelScope.launch { repository.emptyDeletedReadingsPermanently() }
    fun emptyDeletedUtilityBillsPermanently() = viewModelScope.launch { repository.emptyDeletedUtilityBillsPermanently() }

    fun updateProfile(profile: UserProfileEntity) = viewModelScope.launch { repository.saveUserProfile(profile) }
    fun updateCustomisation(profile: UserProfileEntity) = viewModelScope.launch { repository.saveUserProfile(profile) }
    fun updatePreferences(profile: UserProfileEntity) = viewModelScope.launch { repository.saveUserProfile(profile) }

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

# I will append this just before the last brace
last_brace_idx = content.rfind('}')
if last_brace_idx != -1:
    content = content[:last_brace_idx] + code + "\n}\n"

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
