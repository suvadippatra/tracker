with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    text = f.read()

lines = text.split('\n')
start_idx = -1
for i, line in enumerate(lines):
    if "fun HotelManagementTab(" in line:
        start_idx = i
        break

count = 0
for i in range(start_idx, len(lines)):
    line = lines[i]
    count += line.count('{') - line.count('}')
    if count == 0 and i > start_idx + 5:
        print(f"Function closed at line {i+1}: {line.strip()}")
        break
