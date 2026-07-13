with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "rb") as f:
    lines = f.readlines()
for i in range(1040, 1055):
    print(f"{i+1}: {lines[i]}")
