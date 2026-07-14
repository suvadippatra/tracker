import sys
with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
    lines = f.readlines()
stack = []
for i, line in enumerate(lines):
    if i < 3170: continue
    if i >= 3886: break
    for char in line:
        if char == '{':
            stack.append(i+1)
        elif char == '}':
            if stack:
                opened_line = stack.pop()
                if opened_line == 3242:
                    print(f"AnimatedVisibility opened at 3242 is closed at {i+1}!")
