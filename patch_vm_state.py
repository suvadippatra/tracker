with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

state_vars = """
    var qrExportPayload by androidx.compose.runtime.mutableStateOf<ByteArray?>(null)
        private set
    var qrExportSessionId by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set

    fun setQrExportPayload(payload: ByteArray?, sessionId: String?) {
        qrExportPayload = payload
        qrExportSessionId = sessionId
    }
"""

last_brace = content.rfind('}')

func = """
    fun generateQrExportPayload(hotelId: Int, onResult: (ByteArray?) -> Unit) {
        viewModelScope.launch {
            try {
                val db = com.example.data.AppDatabase.getDatabase(getApplication())
                val manager = com.example.data.QrExportManager(db.hotelDao(), db.menuDao(), db.transactionDao())
                val result = manager.generateExportPayload(hotelId)
                if (result is com.example.data.ExportResult.Success) {
                    onResult(result.data)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
"""

# replace the old setQrExportPayload and generateQrExportPayload
import re
content = re.sub(r'fun setQrExportPayload.*?fun generateQrExportPayload.*?}', '', content, flags=re.DOTALL)
last_brace = content.rfind('}')
content = content[:last_brace] + state_vars + func + "\n}\n"

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
