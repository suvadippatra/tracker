with open("app/src/main/java/com/example/ui/screens/meals/AddMealScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if line.strip() == ") { innerPadding ->" and "}" not in lines[i-1]:
        # we need to insert closing braces before this
        pass # wait, I will just do exact matching

