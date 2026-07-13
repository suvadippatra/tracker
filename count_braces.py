with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    s = f.read()

def check_func(name):
    start = s.find(f"fun {name}")
    if start == -1: return
    idx = start
    depth = 0
    while idx < len(s):
        if s[idx] == '{': depth += 1
        elif s[idx] == '}': depth -= 1
        if depth == 0 and s[idx] == '}':
            print(f"{name} ends at idx {idx}, valid")
            return
        idx += 1

check_func("MealHistoryModal")
check_func("AddMealFlowModal")
check_func("AddExpenseDialog")
