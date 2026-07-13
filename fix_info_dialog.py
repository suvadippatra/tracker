import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Add showInfoDialog handling
dialog_placeholder = "if (showMealHistory) {"

info_dialog_code = """
    if (showInfoDialog != null) {
        val h = showInfoDialog!!
        Dialog(onDismissRequest = { showInfoDialog = null }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text(text = "Hotel Details", color = AccentCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Name: ${h.name}", color = AppTextColor(), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Location: ${h.location.ifBlank { "Not provided" }}", color = AppTextColor(), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Advanced Payment: ₹${h.advancedPayment}", color = AppTextColor(), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Phone Number: ${h.phoneNumber.ifBlank { "Not provided" }}", color = AppTextColor(), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "UPI ID: ${h.upiId.ifBlank { "Not provided" }}", color = AppTextColor(), fontSize = 16.sp)
                    if (h.qrCodeUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "QR/URI: ${h.qrCodeUri}", color = AccentCyan, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showInfoDialog = null }, modifier = Modifier.align(Alignment.End), colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)) {
                        Text("Close", color = Color.Black)
                    }
                }
            }
        }
    }
"""

content = content.replace("if (showMealHistory) {", info_dialog_code + "\n    if (showMealHistory) {")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
