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
    val enableStarAnimation: Boolean = true
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
    val totalAmount: Double
)

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double,
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
    val notes: String = ""
)

// Inline simple model for ordered items in a transaction
data class OrderedItem(
    val name: String,
    val quantity: Int,
    val price: Double
) {
    val total: Double get() = quantity * price
}

object TransactionSerializer {
    fun serialize(items: List<OrderedItem>): String {
        return items.joinToString(";") { "${it.name}:${it.quantity}:${it.price}" }
    }

    fun deserialize(serialized: String): List<OrderedItem> {
        if (serialized.isBlank()) return emptyList()
        return try {
            serialized.split(";").mapNotNull { itemStr ->
                val parts = itemStr.split(":")
                if (parts.size == 3) {
                    OrderedItem(parts[0], parts[1].toInt(), parts[2].toDouble())
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Entity(tableName = "meter_readings")
data class MeterReading(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val readingDate: Long,
    val totalReading: Double,
    val unitsConsumed: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false
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
    val transactionId: String? = null
)

@Entity(tableName = "electricity_rates")
data class ElectricityRate(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val effectiveDate: Long,
    val ratePerUnit: Double
)
