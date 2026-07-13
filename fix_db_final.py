with open("app/src/main/java/com/example/data/AppDatabase.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if i == 41 - 1: # Line 41
        new_lines.append("    companion object {\n")
    if i == 42 - 1 or i == 43 - 1: # Lines 42, 43
        continue
    new_lines.append(line)

with open("app/src/main/java/com/example/data/AppDatabase.kt", "w") as f:
    f.writelines(new_lines)
