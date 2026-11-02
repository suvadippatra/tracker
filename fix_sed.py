with open("app/src/main/java/com/example/ui/screens/AddTransactionScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if line == "        }\n" and lines[i-1] == "        }\n":
        pass
    else:
        new_lines.append(line)
with open("app/src/main/java/com/example/ui/screens/AddTransactionScreen.kt", "w") as f:
    f.writelines(new_lines)
