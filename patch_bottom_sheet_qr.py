import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

# First, define state inside HotelDetailsBottomSheet
target_state = """    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var previewQrUri by remember { mutableStateOf<String?>(null) }"""

repl_state = """    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var previewQrUri by remember { mutableStateOf<String?>(null) }
    var qrExportPayload by remember { mutableStateOf<ByteArray?>(null) }
    var qrExportSessionId by remember { mutableStateOf("") }"""

content = content.replace(target_state, repl_state)

# Replace the click handler logic
target_button = """            // Export via QR Button
            Button(
                onClick = { 
                    viewModel.exportHotelViaQr(hotel.id) { result ->
                        when(result) {
                            is com.example.data.ExportResult.Success -> {
                                android.widget.Toast.makeText(context, "Export packaged: ${result.compressedSize} bytes. AES Key ready.", android.widget.Toast.LENGTH_LONG).show()
                                android.util.Log.d("QRExport", "Key: ${result.key.contentToString()}, Payload: ${result.payload.size} bytes")
                            }
                            is com.example.data.ExportResult.Error -> {
                                android.widget.Toast.makeText(context, "Export failed: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },"""

repl_button = """            // Export via QR Button
            Button(
                onClick = { 
                    viewModel.exportHotelViaQr(hotel.id) { result ->
                        when(result) {
                            is com.example.data.ExportResult.Success -> {
                                // Start the QR stream
                                qrExportSessionId = String.format("%06X", kotlin.random.Random.nextInt(0xFFFFFF))
                                qrExportPayload = result.payload
                                // TODO: We also need to share the AES key with the receiver, which happens via the first QR,
                                // wait, the key isn't in the packet! The AES key must be transferred separately or displayed to the user!
                            }
                            is com.example.data.ExportResult.Error -> {
                                android.widget.Toast.makeText(context, "Export failed: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },"""

content = content.replace(target_button, repl_button)

# Finally, add the dialog render logic at the end of HotelDetailsBottomSheet
target_end = """    if (previewQrUri != null) {
        com.example.ui.components.FullScreenQRPreviewDialog(
            qrUri = previewQrUri!!,
            title = "Hotel Payment QR",
            onDismiss = { previewQrUri = null }
        )
    }
}"""

repl_end = """    if (previewQrUri != null) {
        com.example.ui.components.FullScreenQRPreviewDialog(
            qrUri = previewQrUri!!,
            title = "Hotel Payment QR",
            onDismiss = { previewQrUri = null }
        )
    }
    
    if (qrExportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            payload = qrExportPayload!!,
            sessionId = qrExportSessionId,
            onDismiss = { qrExportPayload = null }
        )
    }
}"""

content = content.replace(target_end, repl_end)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
