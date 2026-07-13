with open("app/src/main/java/com/example/data/AppDatabase.kt", "r") as f:
    content = f.read()

content = content.replace("        @Volatile\n                val MIGRATION_5_6", "        val MIGRATION_5_6")

with open("app/src/main/java/com/example/data/AppDatabase.kt", "w") as f:
    f.write(content)
