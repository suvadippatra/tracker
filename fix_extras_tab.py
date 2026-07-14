import sys

with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
    lines = f.readlines()

out = []
in_extras_lc = False
lc_braces = 0

for i, line in enumerate(lines):
    if "fun ExtraExpensesTab(" in line:
        out.append(line)
        continue
    
    out.append(line)

