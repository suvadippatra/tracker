package com.example.data.model

import androidx.room.Entity
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
    val enableStarAnimation: Boolean = true,
    val textSize: String = "Medium", // "Small", "Medium", "Large"
    val biometricEnabled: Boolean = false,
    val securityQuestion: String = "",
    val securityAnswer: String = "",
    val mealGroupPosition: String = "BottomBar", // "BottomBar" or "PageBottom"
    val bgPreset: String = "Default", // "Default", "Midnight Blue", "Emerald Dark", "Sunset Warm", "Purple Twilight", "Soft Minimal Light"
    val customBgUri: String = ""
)

@Entity(tableName = "hotels")
data class HotelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val advancedPayment: Double = 0.0,
    val phoneNumber: String = "",
    val location: String = "",
    val upiId: String = "",
    val qrCodeUri: String = "",
    val isDeleted: Boolean = false
)

@Entity(tableName = "menu_items")
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
    val imageUri: String? = null
)

@Entity(tableName = "hotel_transactions")
data class HotelTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hotelId: Int,
    val timestamp: Long,
    val isNight: Boolean,
    val itemsSerialized: String, // format: "Name:quantity:price;Name:quantity:price"
    val totalAmount: Double,
    val isDeleted: Boolean = false
)

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double,
    val upiId: String = "",
    val qrCodeUri: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val branchName: String = "",
    val isDeleted: Boolean = false
)

@Entity(tableName = "transaction_categories")
data class TransactionCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String = "EXPENSE", // "INCOME" or "EXPENSE"
    val isSystem: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "extra_expenses")
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
    val isDeleted: Boolean = false
)

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    val uri: String,
    val type: String, // "IMAGE", "PDF", "VOICE"
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
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
    val isDeleted: Boolean = false
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
    fun serialize(items: List<OrderedItem>): String {
        return items.joinToString(";") { "${it.name}:${it.quantity}:${it.price}" }
    }

    fun serializeGroups(groups: List<MealGroup>): String {
        return groups.joinToString("||") { serialize(it.items) }
    }

    fun deserialize(serialized: String): List<OrderedItem> {
        if (serialized.isBlank()) return emptyList()
        return try {
            serialized.split("||").flatMap { groupStr ->
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

    fun deserializeGroups(serialized: String): List<MealGroup> {
        if (serialized.isBlank()) return emptyList()
        return try {
            val groupStrings = serialized.split("||")
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

@Entity(tableName = "hotel_payments")
data class HotelPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hotelId: Int,
    val year: Int,
    val month: Int, // 0-11
    val amountPaid: Double,
    val paymentDate: Long,
    val note: String = "",
    val isDeleted: Boolean = false
)

@Entity(tableName = "meter_readings")
data class MeterReading(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val readingDate: Long,
    val totalReading: Double,
    val unitsConsumed: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "utility_bills")
data class UtilityBill(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val startDate: Long,
    val endDate: Long,
    val totalUnits: Double,
    val calculatedAmount: Double,
    val dueDate: Long,
    val isPaid: Boolean,
    val transactionId: String? = null,
    val isDeleted: Boolean = false
)

@Entity(tableName = "electricity_rates")
data class ElectricityRate(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val effectiveDate: Long,
    val ratePerUnit: Double,
    val isDeleted: Boolean = false
)
