import re
with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

append = """            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return uriString
    }

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
}
"""

if "fun exportHotelViaQr" not in content:
    content = content[:-2] + append

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
