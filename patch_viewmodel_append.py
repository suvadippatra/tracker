with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# find the last closing brace
last_brace = content.rfind('}')

func = """
    fun importHotelViaQr(payload: ByteArray, key: ByteArray, iv: ByteArray, onResult: (com.example.data.ImportResult) -> Unit) {
        androidx.lifecycle.viewModelScope.launch {
            val db = com.example.data.AppDatabase.getDatabase(getApplication())
            val manager = com.example.data.QrImportManager(db.hotelDao(), db.menuDao(), db.transactionDao())
            val result = manager.importPayload(payload, key, iv)
            if (result is com.example.data.ImportResult.Success) {
                activeHotelId.value = result.newHotelId
            }
            onResult(result)
        }
    }
"""
content = content[:last_brace] + func + "\n}\n"

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
