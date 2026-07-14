import sys
with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
    lines = f.readlines()
count = 0
for i in range(3248, 3880):
    old_count = count
    count += lines[i].count('{')
    count -= lines[i].count('}')
    if count != old_count:
        print(f"Line {i+1}: {old_count} -> {count} | {lines[i].strip()}")
