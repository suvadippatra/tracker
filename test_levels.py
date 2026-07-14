import sys
with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
    lines = f.readlines()
stack = []
for i, line in enumerate(lines):
    if i >= 4020:
        break
    if i < 3170:
        continue
    for char in line:
        if char == '{':
            stack.append(f"Line {i+1}: {line.strip()}")
        elif char == '}':
            if stack:
                stack.pop()
print("Open blocks at line 3886:")
for item in stack:
    print(item)
