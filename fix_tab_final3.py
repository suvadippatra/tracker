with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

bad = """
    if (exportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = exportPayload!!,
            sessionId = exportSessionId,
            onDismiss = { viewModel.setQrExportPayload(null, null) }
        )
    }
"""

good = """
    if (exportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = com.example.data.ExportResult.Success(exportPayload!!, ByteArray(0), ByteArray(0), 0, 0, 0),
            sessionId = exportSessionId ?: "",
            onDismiss = { viewModel.setQrExportPayload(null, null) }
        )
    }
"""

content = content.replace(bad.strip(), good.strip())

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
