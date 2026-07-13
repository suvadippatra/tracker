with open("app/src/main/java/com/example/data/dao/Daos.kt", "r") as f:
    content = f.read()

content = content.replace("fun getAllMenu(): Flow<List<MenuEntity>>", "fun getAllMenu(): Flow<List<MenuEntity>>\n\n    @Query(\"SELECT * FROM menu_items WHERE hotelId = :hotelId ORDER BY id ASC\")\n    fun getMenuForHotel(hotelId: Int): Flow<List<MenuEntity>>")

with open("app/src/main/java/com/example/data/dao/Daos.kt", "w") as f:
    f.write(content)
