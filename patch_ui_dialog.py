import re
with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

content = content.replace("if (qrExportPayload != null) {", "if (viewModel.qrExportPayload != null) {")
content = content.replace("exportData = qrExportPayload!!,", "exportData = viewModel.qrExportPayload!!,")
content = content.replace("sessionId = qrExportSessionId,", "sessionId = viewModel.qrExportSessionId,")
content = content.replace("onDismiss = { qrExportPayload = null }", "onDismiss = { viewModel.setQrExportPayload(null, null) }")

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
