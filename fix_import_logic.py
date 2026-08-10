import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # We want to replace the logic:
    # isDayMenu = typeStr.equals("Day", ignoreCase = true) || typeStr.equals("Common", ignoreCase = true),
    # isNightMenu = typeStr.equals("Night", ignoreCase = true) || typeStr.equals("Common", ignoreCase = true),
    
    old_logic = """                            isDayMenu = typeStr.equals("Day", ignoreCase = true) || typeStr.equals("Common", ignoreCase = true),
                            isNightMenu = typeStr.equals("Night", ignoreCase = true) || typeStr.equals("Common", ignoreCase = true),"""
    
    new_logic = """                            isDayMenu = typeStr.contains("Day", ignoreCase = true) || typeStr.contains("Common", ignoreCase = true) || (!typeStr.contains("Night", ignoreCase = true) && !typeStr.contains("Day", ignoreCase = true)),
                            isNightMenu = typeStr.contains("Night", ignoreCase = true) || typeStr.contains("Common", ignoreCase = true) || (!typeStr.contains("Night", ignoreCase = true) && !typeStr.contains("Day", ignoreCase = true)),"""
    
    # Let's check what's actually there.
    
    # Let's also ensure `isFullExport` defaults to true if neither is parsed correctly
    old_full_export = """                        val menu = MenuEntity(
                            hotelId = hId,
                            banglaName = parts[pOffset],
                            englishName = parts[pOffset + 1],
                            price = parts[pOffset + 2].toDoubleOrNull() ?: 0.0,
                            isDayMenu = parts[pOffset + 3].equals("true", ignoreCase = true),
                            isNightMenu = parts[pOffset + 4].equals("true", ignoreCase = true),
                            isAddOn = parts[pOffset + 5].equals("true", ignoreCase = true),"""
                            
    new_full_export = """                        val isDayRaw = parts[pOffset + 3].equals("true", ignoreCase = true)
                        val isNightRaw = parts[pOffset + 4].equals("true", ignoreCase = true)
                        val menu = MenuEntity(
                            hotelId = hId,
                            banglaName = parts[pOffset],
                            englishName = parts[pOffset + 1],
                            price = parts[pOffset + 2].toDoubleOrNull() ?: 0.0,
                            isDayMenu = if (!isDayRaw && !isNightRaw) true else isDayRaw,
                            isNightMenu = if (!isDayRaw && !isNightRaw) true else isNightRaw,
                            isAddOn = parts[pOffset + 5].equals("true", ignoreCase = true),"""
    
    if old_logic in content:
        content = content.replace(old_logic, new_logic)
    
    if old_full_export in content:
        content = content.replace(old_full_export, new_full_export)
        
    # Also for basic format:
    old_basic = """                        val typeStr = parts[2]
                        val emoji = parts[3]
                        repository.insertMenu(MenuEntity(
                            hotelId = targetHotelId ?: 1,
                            englishName = name,
                            banglaName = name,
                            price = price,
                            emoji = emoji,
                            type = typeStr,
                            isDayMenu = typeStr.equals("Day", ignoreCase = true) || typeStr.equals("Common", ignoreCase = true),
                            isNightMenu = typeStr.equals("Night", ignoreCase = true) || typeStr.equals("Common", ignoreCase = true),"""
    
    new_basic = """                        val typeStr = parts[2]
                        val emoji = parts[3]
                        repository.insertMenu(MenuEntity(
                            hotelId = targetHotelId ?: 1,
                            englishName = name,
                            banglaName = name,
                            price = price,
                            emoji = emoji,
                            type = typeStr,
                            isDayMenu = typeStr.contains("Day", ignoreCase = true) || typeStr.contains("Common", ignoreCase = true) || (!typeStr.contains("Night", ignoreCase = true) && !typeStr.contains("Day", ignoreCase = true)),
                            isNightMenu = typeStr.contains("Night", ignoreCase = true) || typeStr.contains("Common", ignoreCase = true) || (!typeStr.contains("Night", ignoreCase = true) && !typeStr.contains("Day", ignoreCase = true)),"""

    if old_basic in content:
        content = content.replace(old_basic, new_basic)

    with open(filepath, 'w') as f:
        f.write(content)

modify_file('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt')
