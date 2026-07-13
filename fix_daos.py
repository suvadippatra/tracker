import re

with open("app/src/main/java/com/example/data/dao/Daos.kt", "r") as f:
    content = f.read()

dao_methods = """
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenu(menu: MenuEntity)

    @Update
    suspend fun updateMenu(menu: MenuEntity)

    @Delete
    suspend fun deleteMenu(menu: MenuEntity)
"""
content = content.replace("interface MenuDao {", "interface MenuDao {" + dao_methods)
with open("app/src/main/java/com/example/data/dao/Daos.kt", "w") as f:
    f.write(content)

print("Daos updated")
