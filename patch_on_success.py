with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target = '            onSuccess = { payload, key, iv ->'
repl = '            onSuccess = { payload: ByteArray, key: ByteArray, iv: ByteArray ->'
content = content.replace(target, repl)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
