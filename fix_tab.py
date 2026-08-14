import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

bad_str = """
    if (viewModel.qrExportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = viewModel.qrExportPayload!!,
            sessionId = viewModel.qrExportSessionId,
            onDismiss = { viewModel.setQrExportPayload(null, null) }
        )
    }
"""

good_str = """
    val exportPayload by viewModel.qrExportPayload.collectAsStateWithLifecycle()
    val exportSessionId by viewModel.qrExportSessionId.collectAsStateWithLifecycle()

    if (exportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = exportPayload!!,
            sessionId = exportSessionId,
            onDismiss = { viewModel.setQrExportPayload(null, null) }
        )
    }
"""

content = content.replace(bad_str.strip(), good_str.strip())

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
