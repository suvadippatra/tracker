import re

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

func_code = """
    fun exportHotelViaQr(hotelId: Int, onResult: (com.example.data.ExportResult) -> Unit) {
        viewModelScope.launch {
            val db = com.example.data.AppDatabase.getDatabase(getApplication())
            val manager = com.example.data.QrExportManager(db.hotelDao(), db.menuDao(), db.transactionDao())
            val result = manager.generateExportPayload(hotelId)
            onResult(result)
        }
    }
"""

# Insert before the last closing brace
last_brace_idx = content.rfind('}')
if last_brace_idx != -1:
    new_content = content[:last_brace_idx] + func_code + content[last_brace_idx:]
    with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
        f.write(new_content)
    print("Updated FinancialViewModel.kt")
else:
    print("Could not find closing brace")
