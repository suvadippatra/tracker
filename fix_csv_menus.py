import re
with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# Replace importMenusFromCsv with a robust version
old_func = r'fun importMenusFromCsv\(csvText: String, targetHotelId: Int\? = null\) \{[\s\S]*?\} catch \(e: Exception\) \{\s*e\.printStackTrace\(\)\s*\}\s*\}\s*\}\s*\}'
new_func = """fun importMenusFromCsv(csvText: String, targetHotelId: Int? = null) {
        viewModelScope.launch {
            val lines = csvText.lines()
            if (lines.size <= 1) return@launch
            val header = lines[0].lowercase()
            val isFullExport = header.contains("isdaymenu")
            val isSettingsFormat = header.contains("banglaname") && !isFullExport
            
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = parseCsvLine(line).map { it.trim() }
                try {
                    if (isFullExport && parts.size >= 8) {
                        val parsedHotelId = parts[0].toIntOrNull()
                        val hId = targetHotelId ?: parsedHotelId ?: 1
                        val pOffset = if (parsedHotelId != null && parts.size >= 9) 1 else 0
                        val menu = MenuEntity(
                            hotelId = hId,
                            banglaName = parts[pOffset],
                            englishName = parts[pOffset + 1],
                            price = parts[pOffset + 2].toDoubleOrNull() ?: 0.0,
                            isDayMenu = parts[pOffset + 3].equals("true", ignoreCase = true),
                            isNightMenu = parts[pOffset + 4].equals("true", ignoreCase = true),
                            isAddOn = parts[pOffset + 5].equals("true", ignoreCase = true),
                            emoji = parts[pOffset + 6],
                            type = parts[pOffset + 7],
                            isDeleted = if (parts.size > pOffset + 8) parts[pOffset + 8].equals("true", ignoreCase = true) else false,
                            imageUri = if (parts.size > pOffset + 9 && parts[pOffset + 9].isNotBlank()) parts[pOffset + 9] else null
                        )
                        repository.insertMenu(menu)
                    } else if (isSettingsFormat && parts.size >= 5) {
                        // englishName, banglaName, price, emoji, type
                        val typeStr = parts[4]
                        repository.insertMenu(MenuEntity(
                            hotelId = targetHotelId ?: 1,
                            englishName = parts[0],
                            banglaName = parts[1],
                            price = parts[2].toDoubleOrNull() ?: 0.0,
                            emoji = parts[3],
                            type = typeStr,
                            isDayMenu = typeStr.equals("Day", ignoreCase = true) || typeStr.equals("Common", ignoreCase = true),
                            isNightMenu = typeStr.equals("Night", ignoreCase = true) || typeStr.equals("Common", ignoreCase = true),
                            isAddOn = false
                        ))
                    } else if (parts.size >= 4) {
                        // Name, Price, Type, Emoji (Menu Management Screen format)
                        val name = parts[0]
                        val price = parts[1].toDoubleOrNull() ?: 0.0
                        val typeStr = parts[2]
                        val emoji = parts[3]
                        repository.insertMenu(MenuEntity(
                            hotelId = targetHotelId ?: 1,
                            banglaName = name,
                            englishName = name,
                            price = price,
                            type = typeStr,
                            emoji = emoji,
                            isDayMenu = typeStr.equals("Day", ignoreCase = true) || typeStr.equals("Common", ignoreCase = true),
                            isNightMenu = typeStr.equals("Night", ignoreCase = true) || typeStr.equals("Common", ignoreCase = true),
                            isAddOn = false
                        ))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }"""
content = re.sub(old_func, new_func, content)

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
print("done")
