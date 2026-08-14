import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if "HotelDetailsBottomSheet(" in line and "fun HotelDetailsBottomSheet" not in line:
        print(f"Caller at line {i+1}:")
        for j in range(i, min(i+10, len(lines))):
            print(f"  {lines[j]}")

