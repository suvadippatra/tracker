with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Replace the "Filtered dishes" logic
old_filter = """    // Filter menu dynamically
    val filteredDishes = menuItems.filter {
        if (isNight) it.isNightMenu else it.isDayMenu
    }
    val mainDishes = filteredDishes.filter { !it.isAddOn }
    val addOnDishes = filteredDishes.filter { it.isAddOn }"""

new_filter = """    // Filter menu dynamically
    val currentPeriodDishes = menuItems.filter {
        if (isNight) it.isNightMenu else it.isDayMenu
    }
    val mainDishes = currentPeriodDishes.filter { !it.isAddOn }
    val addOnDishes = currentPeriodDishes.filter { it.isAddOn }
    
    val otherPeriodDishes = menuItems.filter {
        if (isNight) !it.isNightMenu else !it.isDayMenu
    }.filter { !it.isAddOn }"""

content = content.replace(old_filter, new_filter)

# Create a generic DishCard composable to reuse, or just do a regex replace for the Box.
