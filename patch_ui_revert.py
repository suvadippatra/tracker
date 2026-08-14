import re
with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

content = re.sub(r'qrExportPayload = result\s+qrExportSessionId = java\.util\.UUID\.randomUUID\(\)\.toString\(\)',
                 r'viewModel.setQrExportPayload(result, java.util.UUID.randomUUID().toString())',
                 content)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
