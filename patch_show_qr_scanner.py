with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target = '    var showCreateHotelModal by remember { mutableStateOf(false) }'
repl = '    var showCreateHotelModal by remember { mutableStateOf(false) }\n    var showQrScanner by remember { mutableStateOf(false) }'
content = content.replace(target, repl)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
