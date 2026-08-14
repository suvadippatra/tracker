with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

last_brace = content.rfind('}')

func = """
    fun setQrExportPayload(payload: ByteArray, sessionId: String) {
        // This is handled via state variables in UI, but we can store it temporarily or just return via callback
    }

    fun generateQrExportPayload(hotelId: Int, onResult: (ByteArray?) -> Unit) {
        viewModelScope.launch {
            try {
                val db = com.example.data.AppDatabase.getDatabase(getApplication())
                val manager = com.example.data.QrExportManager(db.hotelDao(), db.menuDao(), db.transactionDao())
                val result = manager.generateExportPayload(hotelId)
                onResult(result)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
"""
content = content[:last_brace] + func + "\n}\n"

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
