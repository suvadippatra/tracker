import re

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# I will replace the state variables injected previously
new_code = """
    private val _qrExportPayload = MutableStateFlow<ByteArray?>(null)
    val qrExportPayload = _qrExportPayload.asStateFlow()

    private val _qrExportSessionId = MutableStateFlow<String?>(null)
    val qrExportSessionId = _qrExportSessionId.asStateFlow()

    fun setQrExportPayload(payload: ByteArray?, sessionId: String?) {
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
"""

# The chunk to replace is from `var qrExportPayload` to `fun unlockApp() { isLockedManual = false }`
start_str = "    var qrExportPayload by androidx.compose.runtime.mutableStateOf<ByteArray?>(null)"
end_str = "fun unlockApp() { isLockedManual = false }"

start_idx = content.find(start_str)
end_idx = content.find(end_str)

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + new_code + content[end_idx + len(end_str):]
else:
    print("Could not find replacement window")

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)

