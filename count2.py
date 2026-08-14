with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    text = f.read()

count = 0
for i, line in enumerate(text.split('\n')):
    count += line.count('{') - line.count('}')
    if count < 0:
        print(f"Braces negative at line {i+1}: {line.strip()}")
        break
