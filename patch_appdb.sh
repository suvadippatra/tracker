sed -i '101i\
        val MIGRATION_22_23 = object : Migration(22, 23) {\
            override fun migrate(database: SupportSQLiteDatabase) {\
                database.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `amount` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `note` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL DEFAULT 0)")\
            }\
        }' app/src/main/java/com/example/data/AppDatabase.kt
