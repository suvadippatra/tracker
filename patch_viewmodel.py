with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

target = '    fun setActiveHotel(hotelId: Int) {'
repl = """    fun importHotelViaQr(payload: ByteArray, key: ByteArray, iv: ByteArray, onResult: (com.example.data.ImportResult) -> Unit) {
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

    fun setActiveHotel(hotelId: Int) {"""

content = content.replace(target, repl)

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
