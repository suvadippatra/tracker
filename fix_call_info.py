import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Add showInfoDialog
content = content.replace("    var showEditHotel by remember { mutableStateOf<HotelEntity?>(null) }\n", "    var showEditHotel by remember { mutableStateOf<HotelEntity?>(null) }\n    var showInfoDialog by remember { mutableStateOf<HotelEntity?>(null) }\n")

# Call Logic
call_logic = """if (h.phoneNumber.isNotBlank()) {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${h.phoneNumber}"))
                                                        context.startActivity(intent)
                                                    } else {
                                                        android.widget.Toast.makeText(context, "No phone number added", android.widget.Toast.LENGTH_SHORT).show()
                                                    }"""
content = content.replace("// TODO: Call hotel logic", call_logic)

# Info Logic
content = content.replace('modifier = Modifier.size(24.dp).padding(end = 4.dp)', 'modifier = Modifier.size(24.dp).padding(end = 4.dp).clickable { showInfoDialog = h }')

info_dialog_code = """    showInfoDialog?.let { h ->
        AlertDialog(
            onDismissRequest = { showInfoDialog = null },
            title = { Text(text = "${h.name} Info", fontWeight = FontWeight.Bold, color = AppTextColor()) },
            text = {
                Column {
                    Text("Location: ${if (h.location.isNotBlank()) h.location else "N/A"}", color = AppTextColor())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Advance Payment: ₹${h.advancedPayment}", color = AppTextColor())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("UPI ID: ${if (h.upiId.isNotBlank()) h.upiId else "N/A"}", color = AppTextColor())
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = null }) {
                    Text("Close", color = AccentCyan)
                }
            },
            containerColor = getGlassCardBg()
        )
    }"""
content = content.replace("    showEditHotel?.let { h ->", info_dialog_code + "\n    showEditHotel?.let { h ->")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
print("Added info logic")
