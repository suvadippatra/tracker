import sys
with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
    lines = f.readlines()
count = 0
for i, line in enumerate(lines):
    count += line.count('{')
    count -= line.count('}')
    if i >= 4010:
        print(f"Line {i+1}: count={count} | {line.strip()}")
