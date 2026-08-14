import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

bad_str = """
                                                                    viewModel.exportHotelViaQr(hotel.id) { result ->
                                                                        if (result is com.example.data.ExportResult.Success) {
                                                                            viewModel.setQrExportPayload(result.data, java.util.UUID.randomUUID().toString())
                                                                        } else {
                                                                            android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    } else {
                                                                            android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }
                                                                }
                                                            )
"""

good_str = """
                                                                    viewModel.exportHotelViaQr(hotel.id) { result ->
                                                                        if (result is com.example.data.ExportResult.Success) {
                                                                            viewModel.setQrExportPayload(result.data, java.util.UUID.randomUUID().toString())
                                                                        } else {
                                                                            android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }
                                                                }
                                                            )
"""
if bad_str in content:
    content = content.replace(bad_str, good_str)
else:
    # try one-liner regex if needed, but it seems there's formatting. Wait, let's use a simpler regex.
    pass

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
