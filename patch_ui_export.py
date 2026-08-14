import re
with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

content = content.replace("viewModel.setQrExportPayload(result, java.util.UUID.randomUUID().toString())",
                          "qrExportPayload = result\n                                                                            qrExportSessionId = java.util.UUID.randomUUID().toString()")

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
