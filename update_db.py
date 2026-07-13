with open("app/src/main/java/com/example/data/AppDatabase.kt", "r") as f:
    content = f.read()

content = content.replace("version = 6", "version = 7")

mig_6_7 = """        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE menu_items ADD COLUMN hotelId INTEGER NOT NULL DEFAULT 1")
            }
        }"""

content = content.replace("val MIGRATION_5_6 = object : Migration(5, 6) {", mig_6_7 + "\n        val MIGRATION_5_6 = object : Migration(5, 6) {")
content = content.replace(".addMigrations(MIGRATION_5_6)", ".addMigrations(MIGRATION_5_6, MIGRATION_6_7)")

with open("app/src/main/java/com/example/data/AppDatabase.kt", "w") as f:
    f.write(content)
