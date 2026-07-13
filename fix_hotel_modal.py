import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Add editQrCode to EditHotelModal
old_edit_upi = "var editUpiId by remember { mutableStateOf(hotel.upiId) }"
new_edit_upi = """var editUpiId by remember { mutableStateOf(hotel.upiId) }
    var editQrCodeUri by remember { mutableStateOf(hotel.qrCodeUri) }"""

content = content.replace(old_edit_upi, new_edit_upi)

old_upi_textfield = """OutlinedTextField(
                value = editUpiId,
                onValueChange = { editUpiId = it },
                label = { Text("UPI ID", color = AppTextColor().copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )"""

new_upi_textfield = """OutlinedTextField(
                value = editUpiId,
                onValueChange = { editUpiId = it },
                label = { Text("UPI ID", color = AppTextColor().copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = editQrCodeUri,
                onValueChange = { editQrCodeUri = it },
                label = { Text("QR Code/UPI Scan URI", color = AppTextColor().copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )"""
            
content = content.replace(old_upi_textfield, new_upi_textfield)

# Update the save button for Hotel
old_save_hotel = """TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AccentPink)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        val advanced = editHotelAdvanced.toDoubleOrNull() ?: hotel.advancedPayment
                        viewModel.updateHotel(hotel.copy(
                            name = editHotelName,
                            advancedPayment = advanced,
                            phoneNumber = editPhone,
                            location = editLocation,
                            upiId = editUpiId
                        ))
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("Save Changes", color = Color.Black)
                }"""
                
new_save_hotel = """TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AccentPink)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        val advanced = editHotelAdvanced.toDoubleOrNull() ?: hotel.advancedPayment
                        viewModel.updateHotel(hotel.copy(
                            name = editHotelName,
                            advancedPayment = advanced,
                            phoneNumber = editPhone,
                            location = editLocation,
                            upiId = editUpiId,
                            qrCodeUri = editQrCodeUri
                        ))
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("Save Changes", color = Color.Black)
                }"""

content = content.replace(old_save_hotel, new_save_hotel)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
