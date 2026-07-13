import re

with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "r") as f:
    content = f.read()

new_methods = """
    fun addMenu(menu: com.example.data.model.MenuEntity) {
        viewModelScope.launch {
            repository.insertMenu(menu)
        }
    }
    fun updateMenu(menu: com.example.data.model.MenuEntity) {
        viewModelScope.launch {
            repository.updateMenu(menu)
        }
    }
    fun deleteMenu(menu: com.example.data.model.MenuEntity) {
        viewModelScope.launch {
            repository.deleteMenu(menu)
        }
    }
"""
content = content.replace("fun addHotel(", new_methods + "\n    fun addHotel(")
with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/data/repository/FinancialRepository.kt", "r") as f:
    content = f.read()

new_repo_methods = """
    suspend fun insertMenu(menu: MenuEntity) {
        withContext(Dispatchers.IO) {
            menuDao.insertMenu(menu)
        }
    }
    suspend fun updateMenu(menu: MenuEntity) {
        withContext(Dispatchers.IO) {
            menuDao.updateMenu(menu)
        }
    }
    suspend fun deleteMenu(menu: MenuEntity) {
        withContext(Dispatchers.IO) {
            menuDao.deleteMenu(menu)
        }
    }
"""
content = content.replace("suspend fun insertHotel(", new_repo_methods + "\n    suspend fun insertHotel(")

# Wait, MenuDao needs insertMenu (single), updateMenu, deleteMenu
with open("app/src/main/java/com/example/data/repository/FinancialRepository.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/data/dao/MenuDao.kt", "r") as f:
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
with open("app/src/main/java/com/example/data/dao/MenuDao.kt", "w") as f:
    f.write(content)

print("ViewModel, Repo, Dao updated for Menu")
