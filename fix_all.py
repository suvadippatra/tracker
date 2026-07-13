with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "r") as f:
    content = f.read()

old_add = """    fun addHotel(name: String, advancedPayment: Double, phoneNumber: String = "") {
        viewModelScope.launch {
            val id = repository.insertHotel(HotelEntity(name = name, advancedPayment = advancedPayment, phoneNumber = phoneNumber))
            // Copy menu from hotel 1 to the new hotel
            val baseMenu = kotlinx.coroutines.flow.first(repository.getMenuForHotel(1))
            baseMenu.forEach { 
                repository.insertMenu(it.copy(id = 0, hotelId = id.toInt()))
            }
        }
    }"""

new_add = """    fun addHotel(name: String, advancedPayment: Double, phoneNumber: String = "") {
        viewModelScope.launch {
            val id = repository.insertHotel(HotelEntity(name = name, advancedPayment = advancedPayment, phoneNumber = phoneNumber))
            // Copy menu from hotel 1 to the new hotel
            val baseMenu = kotlinx.coroutines.flow.first(repository.getMenuForHotel(1))
            baseMenu.forEach { 
                repository.insertMenu(it.copy(id = 0, hotelId = id.toInt()))
            }
        }
    }"""

# Wait, `first` is an extension function. I should just use `import kotlinx.coroutines.flow.first` or `import kotlinx.coroutines.flow.*` at the top.
# Or better, just get it normally.
# Actually I can replace `kotlinx.coroutines.flow.first(repository.getMenuForHotel(1))` with `repository.getMenuForHotel(1).first()` and add the import if needed. But wait! `first()` requires `import kotlinx.coroutines.flow.first`.

if "import kotlinx.coroutines.flow.first" not in content:
    content = "import kotlinx.coroutines.flow.first\n" + content

content = content.replace("kotlinx.coroutines.flow.first(repository.getMenuForHotel(1))", "repository.getMenuForHotel(1).first()")

with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "w") as f:
    f.write(content)


with open("app/src/main/java/com/example/data/repository/FinancialRepository.kt", "r") as f:
    content = f.read()

old_insert = """            // 4. Ingest Menu from CSV
            val menu = allMenuItems.first()
            if (menu.isEmpty()) {
                val menuList = parseCsvMenu(context)
                if (menuList.isNotEmpty()) {
                    menuDao.insertMenuItems(menuList)
                    Log.d("FinancialRepository", "Successfully ingested ${menuList.size} menu items from CSV.")
                } else {
                    // Fallback default list if CSV reading fails
                    val fallback = getFallbackMenu()
                    menuDao.insertMenuItems(fallback)
                    Log.d("FinancialRepository", "Ingested fallback menu.")
                }
            }"""

new_insert = """            // 4. Ingest Menu from CSV
            val menu = allMenuItems.first()
            if (menu.isEmpty()) {
                val menuList = parseCsvMenu(context)
                val itemsToInsert = if (menuList.isNotEmpty()) menuList else getFallbackMenu()
                
                // Insert for Hotel 1
                menuDao.insertMenuItems(itemsToInsert.map { it.copy(hotelId = 1) })
                
                // Insert for Hotel 2
                menuDao.insertMenuItems(itemsToInsert.map { it.copy(id = 0, hotelId = 2) })
                
                Log.d("FinancialRepository", "Successfully ingested menus for default hotels.")
            }"""

if old_insert in content:
    content = content.replace(old_insert, new_insert)
    with open("app/src/main/java/com/example/data/repository/FinancialRepository.kt", "w") as f:
        f.write(content)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_presets = """            if (popularMeals.isEmpty()) {
                // Informational Preset Recommendations
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        PresetMealCard(
                            title = "Veg Thali",
                            description = "Most Ordered Afternoon Meal",
                            tag = "Lunch Choice",
                            color = AccentCyan
                        )
                    }
                    item {
                        PresetMealCard(
                            title = "Roti & Chicken",
                            description = "High Frequency Night Choice",
                            tag = "Dinner Peak",
                            color = AccentPink
                        )
                    }
                    item {
                        PresetMealCard(
                            title = "Alu Paratha",
                            description = "Sunday Special Recommendation",
                            tag = "Weekend Fav",
                            color = AccentGold
                        )
                    }
                }
            }"""

new_presets = """            if (popularMeals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No frequently ordered meals yet. Add some meals to see insights!", color = AppTextColor().copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }"""

if old_presets in content:
    content = content.replace(old_presets, new_presets)
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(content)
