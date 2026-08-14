with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target1 = """    val exportPayload by viewModel.qrExportPayload.collectAsStateWithLifecycle()
    val exportSessionId by viewModel.qrExportSessionId.collectAsStateWithLifecycle()

    if (exportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = exportPayload!!,
            sessionId = exportSessionId ?: "",
            onDismiss = { viewModel.setQrExportPayload(null, null) }
        )
    }"""

if target1 in content:
    content = content.replace(target1, "")
else:
    print("Warning: target1 not found!")

target2 = """    if (showQrScanner) {
        val context = androidx.compose.ui.platform.LocalContext.current
        com.example.ui.components.QrScannerDialog("""

repl2 = """    val exportPayload by viewModel.qrExportPayload.collectAsStateWithLifecycle()
    val exportSessionId by viewModel.qrExportSessionId.collectAsStateWithLifecycle()

    if (exportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = exportPayload!!,
            sessionId = exportSessionId ?: "",
            onDismiss = { viewModel.setQrExportPayload(null, null) }
        )
    }

    if (showQrScanner) {
        val context = androidx.compose.ui.platform.LocalContext.current
        com.example.ui.components.QrScannerDialog("""

if target2 in content:
    content = content.replace(target2, repl2)
else:
    print("Warning: target2 not found!")

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
