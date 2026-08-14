with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("private val _qrExportPayload = MutableStateFlow<ByteArray?>(null)", "private val _qrExportPayload = MutableStateFlow<com.example.data.ExportResult.Success?>(null)")
content = content.replace("val qrExportPayload = _qrExportPayload.asStateFlow()", "val qrExportPayload = _qrExportPayload.asStateFlow()")
content = content.replace("fun setQrExportPayload(payload: ByteArray?, sessionId: String?)", "fun setQrExportPayload(payload: com.example.data.ExportResult.Success?, sessionId: String?)")

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

bad = """
    if (exportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = com.example.data.ExportResult.Success(exportPayload!!, ByteArray(0), ByteArray(0), 0, 0, 0),
            sessionId = exportSessionId ?: "",
            onDismiss = { viewModel.setQrExportPayload(null, null) }
        )
    }
"""

good = """
    if (exportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = exportPayload!!,
            sessionId = exportSessionId ?: "",
            onDismiss = { viewModel.setQrExportPayload(null, null) }
        )
    }
"""

content = content.replace(bad.strip(), good.strip())

# Need to fix how it's called
content = content.replace("viewModel.setQrExportPayload(result.payload, java.util.UUID.randomUUID().toString())", "viewModel.setQrExportPayload(result, java.util.UUID.randomUUID().toString())")

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)

