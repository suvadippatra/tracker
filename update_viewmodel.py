import re
with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# Update importMenusFromCsv
menu_old = r'fun importMenusFromCsv\(csvText: String\) \{[\s\S]*?hotelId = parts\[0\]\.toIntOrNull\(\) \?: 1,'
menu_new = """fun importMenusFromCsv(csvText: String, targetHotelId: Int? = null) {
        viewModelScope.launch {
            val lines = csvText.lines()
            if (lines.size <= 1) return@launch
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = parseCsvLine(line)
                if (parts.size >= 8) {
                    try {
                        val parsedHotelId = parts[0].toIntOrNull()
                        val hId = targetHotelId ?: parsedHotelId ?: 1
                        val pOffset = if (parsedHotelId != null && parts.size >= 9) 1 else 0
                        
                        val menu = MenuEntity(
                            hotelId = hId,
                            banglaName = parts[pOffset],
                            englishName = parts[pOffset + 1],
                            price = parts[pOffset + 2].toDoubleOrNull() ?: 0.0,
                            isDayMenu = parts[pOffset + 3].toBoolean(),
                            isNightMenu = parts[pOffset + 4].toBoolean(),
                            isAddOn = parts[pOffset + 5].toBoolean(),
                            emoji = parts[pOffset + 6],
                            type = parts[pOffset + 7],
                            isDeleted = if (parts.size > pOffset + 8) parts[pOffset + 8].toBoolean() else false,
                            imageUri = if (parts.size > pOffset + 9 && parts[pOffset + 9].isNotBlank()) parts[pOffset + 9] else null
                        )"""
content = re.sub(r'fun importMenusFromCsv\(csvText: String\) \{[\s\S]*?imageUri = if \(parts\.size >= 11 && parts\[10\]\.isNotBlank\(\)\) parts\[10\] else null\s*\)', menu_new, content)

# Update importHotelTransactionsFromCsv
tx_old = r'fun importHotelTransactionsFromCsv\(csvText: String\) \{[\s\S]*?hotelId = parts\[0\]\.toIntOrNull\(\) \?: 1,'
tx_new = """fun importHotelTransactionsFromCsv(csvText: String, targetHotelId: Int? = null) {
        viewModelScope.launch {
            val lines = csvText.lines()
            if (lines.size <= 1) return@launch
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = parseCsvLine(line)
                if (parts.size >= 4) {
                    try {
                        val parsedHotelId = parts[0].toIntOrNull()
                        val hId = targetHotelId ?: parsedHotelId ?: 1
                        val pOffset = if (parsedHotelId != null && parts.size >= 5) 1 else 0
                        
                        val tx = HotelTransactionEntity(
                            hotelId = hId,
                            timestamp = parts[pOffset].toLongOrNull() ?: System.currentTimeMillis(),
                            isNight = parts[pOffset + 1].toBoolean(),
                            itemsSerialized = parts[pOffset + 2],
                            totalAmount = parts[pOffset + 3].toDoubleOrNull() ?: 0.0
                        )"""
content = re.sub(r'fun importHotelTransactionsFromCsv\(csvText: String\) \{[\s\S]*?totalAmount = parts\[4\]\.toDoubleOrNull\(\) \?: 0\.0\s*\)', tx_new, content)

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
print("Updated ViewModel")
