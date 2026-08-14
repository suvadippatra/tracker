with open('app/src/main/java/com/example/ui/components/QrExportDialog.kt', 'r') as f:
    content = f.read()

target = """        currentBitmap = generateQrCode(packetBytes, 800)"""

repl = """        currentBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            generateQrCode(packetBytes, 800)
        }"""

content = content.replace(target, repl)

with open('app/src/main/java/com/example/ui/components/QrExportDialog.kt', 'w') as f:
    f.write(content)
