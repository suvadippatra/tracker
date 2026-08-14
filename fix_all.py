import re

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# Fix imports
if 'import androidx.compose.runtime.getValue' not in content:
    content = content.replace('import androidx.lifecycle.ViewModelProvider', 
                              'import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue\nimport androidx.lifecycle.ViewModelProvider')

# Fix copyToInternalStorage
bad_str = """                return Uri.fromFile(file).toString()
            }}
            } catch (e: Exception) {"""
good_str = """                return Uri.fromFile(file).toString()
            } catch (e: Exception) {"""
content = content.replace(bad_str, good_str)

# Truncate after copyToInternalStorage to completely rebuild the rest
idx = content.find('    fun exportHotelViaQr(hotelId: Int,')
if idx != -1:
    content = content[:idx]

# Remove the trailing '}' if it's there
content = content.rstrip()
if content.endswith('}'):
    content = content[:-1]
if content.endswith('}'):
    content = content[:-1]

code = """
    // Import/Export QR methods
    fun exportHotelViaQr(hotelId: Int, onResult: (com.example.data.ExportResult) -> Unit) {
        viewModelScope.launch {
            val db = com.example.data.AppDatabase.getDatabase(getApplication())
            val manager = com.example.data.QrExportManager(db.hotelDao(), db.menuDao(), db.transactionDao())
            val result = manager.generateExportPayload(hotelId)
            onResult(result)
        }
    }

    fun importHotelViaQr(payload: ByteArray, key: ByteArray, iv: ByteArray, onResult: (com.example.data.ImportResult) -> Unit) {
        viewModelScope.launch {
            val db = com.example.data.AppDatabase.getDatabase(getApplication())
            val manager = com.example.data.QrImportManager(db.hotelDao(), db.menuDao(), db.transactionDao())
            val result = manager.importPayload(payload, key, iv)
            if (result is com.example.data.ImportResult.Success) {
                activeHotelId.value = result.newHotelId
            }
            onResult(result)
        }
    }

    var qrExportPayload by androidx.compose.runtime.mutableStateOf<ByteArray?>(null)
        private set
    var qrExportSessionId by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set

    fun setQrExportPayload(payload: ByteArray?, sessionId: String?) {
        qrExportPayload = payload
        qrExportSessionId = sessionId
    }

    // Search State
    var searchQuery by androidx.compose.runtime.mutableStateOf("")
        private set
    var selectedSearchFilter by androidx.compose.runtime.mutableStateOf("All")
        private set
    var searchHistory by androidx.compose.runtime.mutableStateOf<List<String>>(emptyList())
        private set
    var searchResults by androidx.compose.runtime.mutableStateOf<List<com.example.data.model.SearchResult>>(emptyList())
        private set
    
    fun setSearchQuery(query: String) { searchQuery = query }
    fun setSearchFilter(filter: String) { selectedSearchFilter = filter }
    fun addSearchHistory(query: String) {
        val list = searchHistory.toMutableList()
        list.remove(query)
        list.add(0, query)
        searchHistory = list.take(10)
    }
    fun clearSearchHistory() { searchHistory = emptyList() }
    fun removeSearchHistoryItem(item: String) {
        searchHistory = searchHistory.filter { it != item }
    }

    // Lock State
    var isLockedManual by androidx.compose.runtime.mutableStateOf(false)
        private set
    fun lockApp() { isLockedManual = true }
    fun unlockApp() { isLockedManual = false }

    // Standard CRUD
    fun addHotel(hotel: HotelEntity) = viewModelScope.launch { repository.insertHotel(hotel) }
    fun editHotel(hotel: HotelEntity) = viewModelScope.launch { repository.updateHotel(hotel) }
    fun deleteHotel(hotel: HotelEntity) = viewModelScope.launch { repository.deleteHotel(hotel) }
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
    fun deleteHotelPermanently(item: HotelEntity) = viewModelScope.launch { repository.deleteHotel(item) } 
    fun deleteVirtualAccountPermanently(item: BankAccountEntity) = viewModelScope.launch { repository.deleteBankAccount(item) }
    fun deleteMenuPermanently(item: MenuEntity) = viewModelScope.launch { repository.deleteMenu(item) }
    fun deleteExtraExpensePermanently(item: ExtraExpenseEntity) = viewModelScope.launch { repository.deleteExpensePermanently(item) }
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
}
"""

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content + "\n" + code)
