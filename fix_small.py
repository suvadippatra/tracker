with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("showDatePickerModal = true", "/* showDatePickerModal = true (removed) */")
content = content.replace("AccentOrange", "AccentGold")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
