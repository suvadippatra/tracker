import sys
with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
    lines = f.readlines()
count = 1
for i in range(813, len(lines)): # starting at 814 (index 813)
    line = lines[i]
    old = count
    count += line.count('{')
    count -= line.count('}')
    if old == 1 and count == 2:
        print(f"Item starts at line {i+1}")
    elif old == 2 and count == 1:
        print(f"Item ends at line {i+1}")
    elif count == 0:
        print(f"LazyColumn ends at line {i+1}")
        break
