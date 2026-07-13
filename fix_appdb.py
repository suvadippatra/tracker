with open("app/src/main/java/com/example/data/AppDatabase.kt", "r") as f:
    content = f.read()

content = content.replace('version = 5,', 'version = 6,')

migration_import = "import androidx.room.migration.Migration\nimport androidx.sqlite.db.SupportSQLiteDatabase\n"
content = content.replace("import com.example.data.model.UserProfileEntity\n", "import com.example.data.model.UserProfileEntity\n" + migration_import)

migration_code = """
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN enableStarAnimation INTEGER NOT NULL DEFAULT 1")
            }
        }
"""
content = content.replace('private var INSTANCE: AppDatabase? = null', migration_code + '\n        @Volatile\n        private var INSTANCE: AppDatabase? = null')
content = content.replace('.fallbackToDestructiveMigration(true)', '.fallbackToDestructiveMigration(true)\n                .addMigrations(MIGRATION_5_6)')

with open("app/src/main/java/com/example/data/AppDatabase.kt", "w") as f:
    f.write(content)
