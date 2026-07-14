package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BankAccountDao
import com.example.data.dao.ExtraExpenseDao
import com.example.data.dao.HotelDao
import com.example.data.dao.MenuDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.UserProfileDao
import com.example.data.dao.MeterDao
import com.example.data.dao.TransactionCategoryDao
import com.example.data.model.BankAccountEntity
import com.example.data.model.ExtraExpenseEntity
import com.example.data.model.HotelEntity
import com.example.data.model.HotelTransactionEntity
import com.example.data.model.MenuEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.MeterReading
import com.example.data.model.UtilityBill
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.example.data.model.AttachmentEntity
import com.example.data.model.OwnerAccountEntity
import com.example.data.dao.AttachmentDao
import com.example.data.dao.OwnerAccountDao

@Database(
    entities = [
        UserProfileEntity::class,
        HotelEntity::class,
        MenuEntity::class,
        HotelTransactionEntity::class,
        BankAccountEntity::class,
        ExtraExpenseEntity::class,
        MeterReading::class,
        UtilityBill::class,
        com.example.data.model.ElectricityRate::class,
        com.example.data.model.TransactionCategoryEntity::class,
        AttachmentEntity::class,
        OwnerAccountEntity::class
    ],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun hotelDao(): HotelDao
    abstract fun menuDao(): MenuDao
    abstract fun transactionDao(): TransactionDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun extraExpenseDao(): ExtraExpenseDao
    abstract fun meterDao(): MeterDao
    abstract fun transactionCategoryDao(): TransactionCategoryDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun ownerAccountDao(): OwnerAccountDao
    companion object {

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN textSize TEXT NOT NULL DEFAULT 'Medium'")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE menu_items ADD COLUMN imageUri TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE menu_items ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE menu_items ADD COLUMN hotelId INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN enableStarAnimation INTEGER NOT NULL DEFAULT 1")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "traker_database"
                )
                .fallbackToDestructiveMigration(true)
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_16_17)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
