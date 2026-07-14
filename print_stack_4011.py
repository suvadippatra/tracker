import sys
with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
    lines = f.readlines()
stack = []
for i, line in enumerate(lines):
    if i < 3170: continue
    if i >= 4011: break
    for char in line:
        if char == '{':
            stack.append((i+1, line.strip()))
        elif char == '}':
            if stack:
                stack.pop()
print("STILL OPEN at 4011:")
for item in stack:
    print(item)
