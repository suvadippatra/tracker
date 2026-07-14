import sys
with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
    lines = f.readlines()
count = 0
for i, line in enumerate(lines):
    count += line.count('{')
    count -= line.count('}')
    if count < 0:
        print(f"Negative count {count} at line {i+1}: {line.strip()}")
print(f"Final count: {count}")
