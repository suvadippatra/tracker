import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target_state = 'var qrExportPayload by remember { mutableStateOf<ByteArray?>(null) }'
repl_state = 'var qrExportPayload by remember { mutableStateOf<com.example.data.ExportResult.Success?>(null) }'
content = content.replace(target_state, repl_state)

target_assign = 'qrExportPayload = result.payload'
repl_assign = 'qrExportPayload = result'
content = content.replace(target_assign, repl_assign)

target_dialog = """        com.example.ui.components.QrExportStreamDialog(
            payload = qrExportPayload!!,
            sessionId = qrExportSessionId,
            onDismiss = { qrExportPayload = null }
        )"""
repl_dialog = """        com.example.ui.components.QrExportStreamDialog(
            exportData = qrExportPayload!!,
            sessionId = qrExportSessionId,
            onDismiss = { qrExportPayload = null }
        )"""
content = content.replace(target_dialog, repl_dialog)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
