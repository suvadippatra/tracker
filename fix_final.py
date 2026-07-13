import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Fix h to hotel
content = content.replace("if (h.phoneNumber.isNotBlank())", "if (hotel.phoneNumber.isNotBlank())")
content = content.replace("tel:${h.phoneNumber}", "tel:${hotel.phoneNumber}")
content = content.replace(".clickable { showInfoDialog = h }", ".clickable { showInfoDialog = hotel }")

# Fix Export logic by completely replacing the onClick block for export
start_export = """                                        val txs = viewModel.activeHotelTransactions.value"""
end_export = """                                        showHotelDropdown = false
                                    }"""

new_export = """                                        val txs = viewModel.activeHotelTransactions.value
                                        val sb = java.lang.StringBuilder()
                                        sb.append("Date,Type,Total\\n")
                                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy hh:mm a", java.util.Locale.getDefault())
                                        for (t in txs) {
                                            sb.append(sdf.format(java.util.Date(t.timestamp))).append(",")
                                            sb.append(if (t.isNight) "Night" else "Day").append(",")
                                            sb.append(t.totalAmount).append("\\n")
                                        }
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "${hotel.name} Transactions")
                                            putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                                        }
                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        context.startActivity(android.content.Intent.createChooser(intent, "Export Transactions"))
                                        showHotelDropdown = false
                                    }"""
content = re.sub(r'val txs = viewModel\.activeHotelTransactions\.value.*?showHotelDropdown = false\n\s*}', new_export, content, flags=re.DOTALL)

# Also fix the call context
content = content.replace("context.startActivity(intent)", "androidx.compose.ui.platform.LocalContext.current.startActivity(intent)")
content = content.replace("android.widget.Toast.makeText(context, ", "android.widget.Toast.makeText(androidx.compose.ui.platform.LocalContext.current, ")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
print("Fixed final issues")
