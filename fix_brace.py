with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

content += "\n}\n"

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
