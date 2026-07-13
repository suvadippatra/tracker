with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    s = f.read()

start = s.find("fun AddMealFlowModal")
depth = 0
for i in range(start, len(s)):
    if s[i] == '{': depth += 1
    elif s[i] == '}': depth -= 1
    
    if depth == 0 and s[i] == '}':
        print(f"Closed at {i}")
        break

print("Depth at MealHistoryModal:", depth)
