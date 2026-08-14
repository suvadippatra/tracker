with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target = """    if (qrExportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = qrExportPayload!!,
            sessionId = qrExportSessionId,
            onDismiss = { qrExportPayload = null }
        )
    }
}"""

repl = """    if (qrExportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = qrExportPayload!!,
            sessionId = qrExportSessionId,
            onDismiss = { qrExportPayload = null }
        )
    }
    
    if (showQrScanner) {
        com.example.ui.components.QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onSuccess = { payload, key, iv ->
                showQrScanner = false
                viewModel.importHotelViaQr(payload, key, iv) { result ->
                    when (result) {
                        is com.example.data.ImportResult.Success -> {
                            android.widget.Toast.makeText(context, "Hotel imported successfully!", android.widget.Toast.LENGTH_LONG).show()
                        }
                        is com.example.data.ImportResult.Error -> {
                            android.widget.Toast.makeText(context, "Import failed: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }
}"""

content = content.replace(target, repl)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
