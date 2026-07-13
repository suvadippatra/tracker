import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Replace the whole onClick for Export transactions
start_marker = "                                        val sb = java.lang.StringBuilder()"
end_marker = "                                        showHotelDropdown = false"

fixed_export = """                                        val sb = java.lang.StringBuilder()
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
                                        context.startActivity(android.content.Intent.createChooser(intent, "Export Transactions"))
                                        showHotelDropdown = false"""

content = re.sub(r'val sb = java\.lang\.StringBuilder\(\).*?showHotelDropdown = false', fixed_export, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
print("Fixed final issues 3")
