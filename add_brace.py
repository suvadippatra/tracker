with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Add a closing brace before MealHistoryModal
content = content.replace("// ==========================================\n// CHRONOLOGICAL VIEW MEAL HISTORY MODAL", "}\n\n// ==========================================\n// CHRONOLOGICAL VIEW MEAL HISTORY MODAL")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
