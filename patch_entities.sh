cat << 'INNER_EOF' >> app/src/main/java/com/example/data/model/Entities.kt

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val amount: Double,
    val timestamp: Long,
    val note: String,
    val isDeleted: Boolean = false
)
INNER_EOF
