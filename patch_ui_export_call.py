with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target = """                            is com.example.data.ExportResult.Success -> {
                                // Start the QR stream
                                qrExportSessionId = String.format("%06X", kotlin.random.Random.nextInt(0xFFFFFF))
                                qrExportPayload = result
                                // TODO: We also need to share the AES key with the receiver, which happens via the first QR,
                                // wait, the key isn't in the packet! The AES key must be transferred separately or displayed to the user!
                            }"""

repl = """                            is com.example.data.ExportResult.Success -> {
                                // Start the QR stream
                                viewModel.setQrExportPayload(result, java.util.UUID.randomUUID().toString())
                            }"""

content = content.replace(target, repl)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
