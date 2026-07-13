with open("app/src/main/java/com/example/data/dao/Daos.kt", "r") as f:
    content = f.read()

if "getMenuForHotel" not in content:
    content = content.replace("fun getAllMenuItems(): Flow<List<MenuEntity>>", "fun getAllMenuItems(): Flow<List<MenuEntity>>\n\n    @Query(\"SELECT * FROM menu_items WHERE hotelId = :hotelId ORDER BY id ASC\")\n    fun getMenuForHotel(hotelId: Int): Flow<List<MenuEntity>>")
    with open("app/src/main/java/com/example/data/dao/Daos.kt", "w") as f:
        f.write(content)
