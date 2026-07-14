import sys
with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "fun ExtrasManagementTab(" in line:
        print(f"Extras starts at line {i+1}")
