package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val avatar: String = "avatar_premium", // Visual avatar selection
    val contact: String = "",
    val currencySymbol: String = "₹",
    val language: String = "en", // "en" or "bn"
    val themeMode: String = "Auto", // "Auto", "Dark" or "Light"
    val showAdvancedPayment: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val defaultHotelId: Int = 1,
    val defaultBankAccountId: Int = 1,
    val defaultOwnerAccountId: Int = 1,
    val enableStarAnimation: Boolean = true,
    val textSize: String = "Medium", // "Small", "Medium", "Large"
    val biometricEnabled: Boolean = false,
    val securityQuestion: String = "",
    val securityAnswer: String = "",
    val mealGroupPosition: String = "BottomBar", // "BottomBar" or "PageBottom"
    val bgPreset: String = "Default", // "Default", "Midnight Blue", "Emerald Dark", "Sunset Warm", "Purple Twilight", "Soft Minimal Light"
    val customBgUri: String = "",
    val enableSupplementaryAddons: Boolean = true,
    val visibleBankAccountsOnExtra: String = "",
    val enableLiquidGlassTheme: Boolean = false
)

@Entity(tableName = "hotels", indices = [Index("isDeleted")])
data class HotelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val advancedPayment: Double = 0.0,
    val phoneNumber: String = "",
    val location: String = "",
    val upiId: String = "",
    val qrCodeUri: String = "",
    val isDeleted: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val importedFrom: String? = null
)

@Entity(
    tableName = "menu_items",
    indices = [Index("hotelId"), Index("isDeleted"), Index(value = ["hotelId", "isDeleted"])]
)
data class MenuEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hotelId: Int = 1,
    val banglaName: String,
    val englishName: String,
    val price: Double,
    val isDayMenu: Boolean,
    val isNightMenu: Boolean,
    val isAddOn: Boolean,
    val emoji: String = "🍽️",
    val type: String = "Common",
    val isDeleted: Boolean = false,
    val imageUri: String? = null,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "hotel_transactions",
    indices = [
        Index("hotelId"),
        Index("timestamp"),
        Index("isDeleted"),
        Index(value = ["hotelId", "isDeleted", "timestamp"])
    ]
)
data class HotelTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hotelId: Int,
    val timestamp: Long,
    val isNight: Boolean,
    val itemsSerialized: String, // format: "Name:quantity:price;Name:quantity:price"
    val totalAmount: Double,
    val isDeleted: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "bank_accounts", indices = [Index("isDeleted")])
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double = 0.0,
    val initialBalance: Double = 0.0,
    val details: String = "",
    val upiId: String = "",
    val qrCodeUri: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val branchName: String = "",
    val isDeleted: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val importedFrom: String? = null
)

@Entity(tableName = "transaction_categories", indices = [Index("isDeleted")])
data class TransactionCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String = "EXPENSE", // "INCOME" or "EXPENSE"
    val isSystem: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "extra_expenses",
    indices = [
        Index("bankAccountId"),
        Index("timestamp"),
        Index("isDeleted"),
        Index("category"),
        Index("type"),
        Index(value = ["bankAccountId", "isDeleted"]),
        Index(value = ["isDeleted", "timestamp"])
    ]
)
data class ExtraExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val name: String,
    val amount: Double,
    val bankAccountId: Int,
    val receiptUri: String? = null,
    val type: String = "EXPENSE", // "INCOME" or "EXPENSE"
    val category: String = "General",
    val notes: String = "",
    val isDeleted: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "attachments", indices = [Index("expenseId"), Index("isDeleted")])
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    val uri: String,
    val type: String, // "IMAGE", "PDF", "VOICE"
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "owner_accounts")
data class OwnerAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerName: String,
    val ownerMobile: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val branchName: String = "",
    val upiId: String = "",
    val qrCodeUri: String = "",
    val details: String = "",
    val isDeleted: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val importedFrom: String? = null
)

// Inline simple model for ordered items in a transaction
data class OrderedItem(
    val name: String,
    val quantity: Int,
    val price: Double
) {
    val total: Double get() = quantity * price
}

data class MealGroup(
    val id: String = java.util.UUID.randomUUID().toString(),
    val items: List<OrderedItem>
) {
    val subtotal: Double get() = items.sumOf { it.total }
}

data class SearchResult(
    val id: Int,
    val title: String,
    val type: String, // "Hotel", "Expense", "Income", "Meal", "Menu", "Account", "Meter"
    val subtitle: String,
    val dateOrExtra: String = "",
    val amount: Double? = null,
    val hotelId: Int? = null,
    val category: String = ""
)

object TransactionSerializer {
    private val itemsCache = java.util.concurrent.ConcurrentHashMap<String, List<OrderedItem>>()
    private val groupsCache = java.util.concurrent.ConcurrentHashMap<String, List<MealGroup>>()

    fun serialize(items: List<OrderedItem>): String {
        return items.joinToString(";") { "${it.name}:${it.quantity}:${it.price}" }
    }

    fun serializeGroups(groups: List<MealGroup>): String {
        return groups.joinToString("||") { serialize(it.items) }
    }

    fun deserialize(serialized: String): List<OrderedItem> {
        if (serialized.isBlank()) return emptyList()
        return itemsCache.computeIfAbsent(serialized) { raw ->
            try {
                raw.split("||").flatMap { groupStr ->
                    if (groupStr.isBlank()) emptyList()
                    else groupStr.split(";").mapNotNull { itemStr ->
                        val parts = itemStr.split(":")
                        if (parts.size == 3) {
                            OrderedItem(
                                name = parts[0],
                                quantity = parts[1].toIntOrNull() ?: 1,
                                price = parts[2].toDoubleOrNull() ?: 0.0
                            )
                        } else {
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun deserializeGroups(serialized: String): List<MealGroup> {
        if (serialized.isBlank()) return emptyList()
        return groupsCache.computeIfAbsent(serialized) { raw ->
            try {
                val groupStrings = raw.split("||")
                groupStrings.mapNotNull { groupStr ->
                    val items = deserialize(groupStr)
                    if (items.isNotEmpty()) {
                        MealGroup(items = items)
                    } else null
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

@Entity(
    tableName = "hotel_payments",
    indices = [
        Index("hotelId"),
        Index("paymentDate"),
        Index("isDeleted"),
        Index(value = ["hotelId", "isDeleted"])
    ]
)
data class HotelPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hotelId: Int,
    val year: Int,
    val month: Int, // 0-11
    val amountPaid: Double,
    val paymentDate: Long,
    val note: String = "",
    val isDeleted: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "meter_readings", indices = [Index("isDeleted"), Index("readingDate")])
data class MeterReading(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val readingDate: Long,
    val totalReading: Double,
    val unitsConsumed: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val isDeleted: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "utility_bills",
    indices = [Index("isDeleted"), Index("startDate"), Index("endDate"), Index("isPaid")]
)
data class UtilityBill(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val startDate: Long,
    val endDate: Long,
    val totalUnits: Double,
    val calculatedAmount: Double,
    val dueDate: Long,
    val isPaid: Boolean,
    val transactionId: String? = null,
    val isDeleted: Boolean = false,
    val note: String = "",
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "electricity_rates")
data class ElectricityRate(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val effectiveDate: Long,
    val ratePerUnit: Double,
    val isDeleted: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val amount: Double,
    val timestamp: Long,
    val note: String,
    val isDeleted: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)
