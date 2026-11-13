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
import com.example.data.dao.StickyNoteDao
import com.example.data.model.BankAccountEntity
import com.example.data.model.ExtraExpenseEntity
import com.example.data.model.HotelEntity
import com.example.data.model.HotelTransactionEntity
import com.example.data.model.MenuEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.MeterReading
import com.example.data.model.UtilityBill
import com.example.data.model.StickyNoteEntity
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
        OwnerAccountEntity::class,
        com.example.data.model.HotelPaymentEntity::class,
        com.example.data.model.ExpenseEntity::class,
        StickyNoteEntity::class,
        com.example.data.model.MenuModificationHistoryEntity::class
    ],
    version = 36,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): com.example.data.dao.ExpenseDao
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
    abstract fun hotelPaymentDao(): com.example.data.dao.HotelPaymentDao
    abstract fun stickyNoteDao(): StickyNoteDao
    abstract fun syncDao(): com.example.data.dao.SyncDao

    companion object {

        val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN appVersion TEXT NOT NULL DEFAULT '2.4.0'")
                db.execSQL("ALTER TABLE sticky_notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `menu_modification_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `menuItemId` INTEGER NOT NULL,
                        `hotelId` INTEGER NOT NULL,
                        `itemName` TEXT NOT NULL,
                        `oldPrice` REAL NOT NULL,
                        `newPrice` REAL NOT NULL,
                        `oldType` TEXT NOT NULL DEFAULT '',
                        `newType` TEXT NOT NULL DEFAULT '',
                        `changeSummary` TEXT NOT NULL DEFAULT '',
                        `modifiedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_menu_modification_history_menuItemId` ON `menu_modification_history` (`menuItemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_menu_modification_history_hotelId` ON `menu_modification_history` (`hotelId`)")
            }
        }

        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN isBoldTextEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN stickyCardWidthPercent INTEGER NOT NULL DEFAULT 80")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN stickyPreviewLines INTEGER NOT NULL DEFAULT 10")

                db.execSQL("ALTER TABLE hotels ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE hotels ADD COLUMN whatsappNumber TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN phoneNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN whatsappNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN email TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE owner_accounts ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE owner_accounts ADD COLUMN whatsappNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE owner_accounts ADD COLUMN email TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE owner_accounts ADD COLUMN linkedBankAccountId INTEGER DEFAULT NULL")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sticky_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL DEFAULT '',
                        `subtitle` TEXT NOT NULL DEFAULT '',
                        `content` TEXT NOT NULL DEFAULT '',
                        `alignment` TEXT NOT NULL DEFAULT 'START',
                        `colorIndex` INTEGER NOT NULL DEFAULT 0,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        `attachmentsJson` TEXT NOT NULL DEFAULT '[]',
                        `isDeleted` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `lastUpdatedMillis` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sticky_notes_isDeleted` ON `sticky_notes` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sticky_notes_sortOrder` ON `sticky_notes` (`sortOrder`)")
            }
        }

        val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN disableHeavyEffects INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hotels_isDeleted` ON `hotels` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_menu_items_hotelId` ON `menu_items` (`hotelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_menu_items_isDeleted` ON `menu_items` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_menu_items_hotelId_isDeleted` ON `menu_items` (`hotelId`, `isDeleted`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hotel_transactions_hotelId` ON `hotel_transactions` (`hotelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hotel_transactions_timestamp` ON `hotel_transactions` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hotel_transactions_isDeleted` ON `hotel_transactions` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hotel_transactions_hotelId_isDeleted_timestamp` ON `hotel_transactions` (`hotelId`, `isDeleted`, `timestamp`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bank_accounts_isDeleted` ON `bank_accounts` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_categories_isDeleted` ON `transaction_categories` (`isDeleted`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_extra_expenses_bankAccountId` ON `extra_expenses` (`bankAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_extra_expenses_timestamp` ON `extra_expenses` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_extra_expenses_isDeleted` ON `extra_expenses` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_extra_expenses_category` ON `extra_expenses` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_extra_expenses_type` ON `extra_expenses` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_extra_expenses_bankAccountId_isDeleted` ON `extra_expenses` (`bankAccountId`, `isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_extra_expenses_isDeleted_timestamp` ON `extra_expenses` (`isDeleted`, `timestamp`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_expenseId` ON `attachments` (`expenseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_isDeleted` ON `attachments` (`isDeleted`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hotel_payments_hotelId` ON `hotel_payments` (`hotelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hotel_payments_paymentDate` ON `hotel_payments` (`paymentDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hotel_payments_isDeleted` ON `hotel_payments` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hotel_payments_hotelId_isDeleted` ON `hotel_payments` (`hotelId`, `isDeleted`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meter_readings_isDeleted` ON `meter_readings` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meter_readings_readingDate` ON `meter_readings` (`readingDate`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_utility_bills_isDeleted` ON `utility_bills` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_utility_bills_startDate` ON `utility_bills` (`startDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_utility_bills_endDate` ON `utility_bills` (`endDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_utility_bills_isPaid` ON `utility_bills` (`isPaid`)")
            }
        }

        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hotels ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hotels ADD COLUMN importedFrom TEXT DEFAULT NULL")

                db.execSQL("ALTER TABLE menu_items ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE hotel_transactions ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN importedFrom TEXT DEFAULT NULL")

                db.execSQL("ALTER TABLE owner_accounts ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE owner_accounts ADD COLUMN importedFrom TEXT DEFAULT NULL")

                db.execSQL("ALTER TABLE extra_expenses ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE attachments ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE hotel_payments ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE meter_readings ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE utility_bills ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE electricity_rates ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE expenses ADD COLUMN lastUpdatedMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN enableLiquidGlassTheme INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN visibleBankAccountsOnExtra TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE owner_accounts ADD COLUMN details TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN details TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN initialBalance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("UPDATE bank_accounts SET initialBalance = balance WHERE initialBalance = 0.0")
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN defaultOwnerAccountId INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN defaultBankAccountId INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN enableSupplementaryAddons INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE utility_bills ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN textSize TEXT NOT NULL DEFAULT 'Medium'")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE menu_items ADD COLUMN imageUri TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE menu_items ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE menu_items ADD COLUMN hotelId INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN enableStarAnimation INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE BankAccountEntity ADD COLUMN accountNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE BankAccountEntity ADD COLUMN ifsc TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE BankAccountEntity ADD COLUMN branchName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `amount` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `note` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL DEFAULT 0)")
            }
        }
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN mealGroupPosition TEXT NOT NULL DEFAULT 'BottomBar'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN bgPreset TEXT NOT NULL DEFAULT 'Default'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN customBgUri TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var APP_CONTEXT: Context? = null

        fun getAppContext(): Context? = APP_CONTEXT

        fun getDatabase(context: Context): AppDatabase {
            APP_CONTEXT = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                APP_CONTEXT = context.applicationContext
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "traker_database"
                )
                .fallbackToDestructiveMigration(true)
                .addMigrations(
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_16_17, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23,
                    MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27,
                    MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31,
                    MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35,
                    MIGRATION_35_36
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
