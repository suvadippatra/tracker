with open("app/src/main/java/com/example/data/repository/FinancialRepository.kt", "r") as f:
    content = f.read()

content = content.replace("val allMenuItems: Flow<List<MenuEntity>> = menuDao.getAllMenuItems()", "val allMenuItems: Flow<List<MenuEntity>> = menuDao.getAllMenuItems()\n\n    fun getMenuForHotel(hotelId: Int): Flow<List<MenuEntity>> {\n        return menuDao.getMenuForHotel(hotelId)\n    }")

# Also update insertMenu to duplicate menu when adding a hotel
# We have hotelDao.insertHotel(hotel) inside repository?
# No, hotelDao is exposed. Wait, let's see how insertHotel works.

with open("app/src/main/java/com/example/data/repository/FinancialRepository.kt", "w") as f:
    f.write(content)
