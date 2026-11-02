import re

with open("app/src/main/java/com/example/ui/screens/ExtraExpensesTab.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if i == 613: # line 614 is index 613
        # if (page < accounts.size) {
        continue
    if i >= 1075 and i <= 1152: # from } else { up to closing }
        continue
    new_lines.append(line)

with open("app/src/main/java/com/example/ui/screens/ExtraExpensesTab.kt", "w") as f:
    f.writelines(new_lines)
