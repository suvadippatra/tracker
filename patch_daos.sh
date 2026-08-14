cat << 'INNER_EOF' >> app/src/main/java/com/example/data/dao/Daos.kt

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesFlow(): Flow<com.example.data.model.ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: com.example.data.model.ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: com.example.data.model.ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: com.example.data.model.ExpenseEntity)
}
INNER_EOF
