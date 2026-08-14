with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target1 = """                                                            DropdownMenuItem(
                                                                text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                                onClick = {
                                                                    pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                                    openDropdownHotelId = null
                                                                }
                                                            )"""
repl1 = """                                                            DropdownMenuItem(
                                                                text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                                onClick = {
                                                                    pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                                    openDropdownHotelId = null
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text(if (isBengali) "কিউআর মাধ্যমে এক্সপোর্ট" else "Export via QR", color = AccentCyan) },
                                                                onClick = {
                                                                    openDropdownHotelId = null
                                                                    viewModel.generateQrExportPayload(hotel.id) { result ->
                                                                        if (result != null) {
                                                                            viewModel.setQrExportPayload(result, java.util.UUID.randomUUID().toString())
                                                                        } else {
                                                                            android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }
                                                                }
                                                            )"""
content = content.replace(target1, repl1)

target2 = """                                                        DropdownMenuItem(
                                                            text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                            onClick = {
                                                                pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                                openDropdownHotelId = null
                                                            }
                                                        )"""
repl2 = """                                                        DropdownMenuItem(
                                                            text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                            onClick = {
                                                                pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                                openDropdownHotelId = null
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text(if (isBengali) "কিউআর মাধ্যমে এক্সপোর্ট" else "Export via QR", color = AccentCyan) },
                                                            onClick = {
                                                                openDropdownHotelId = null
                                                                viewModel.generateQrExportPayload(hotel.id) { result ->
                                                                    if (result != null) {
                                                                        viewModel.setQrExportPayload(result, java.util.UUID.randomUUID().toString())
                                                                    } else {
                                                                        android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                        )"""
content = content.replace(target2, repl2)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
