with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    lines = f.readlines()

old_content = lines[:1979] + lines[3530:]

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.writelines(old_content)
print("Restored file")
