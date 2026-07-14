import sys
with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "if (hotels.isNotEmpty()) {" in line and i > 800 and i < 830:
        pass
    new_lines.append(line)
