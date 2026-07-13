with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_qr_ui = """            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editQrCodeUri,
                    onValueChange = { editQrCodeUri = it },
                    label = { Text("QR Code/UPI Scan URI", color = AppTextColor().copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { qrCodeLauncher.launch("image/*") },
                    modifier = Modifier.background(AccentCyan.copy(alpha=0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = "Pick QR Code", tint = AccentCyan)
                }
            }"""

new_qr_ui = """            Spacer(modifier = Modifier.height(12.dp))
            Text("QR Codes (${editQrCodeUris.size})", color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                editQrCodeUris.forEachIndexed { index, uri ->
                    Box(modifier = Modifier.height(60.dp).background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QR ${index + 1}", color = AppTextColor(), fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { editQrCodeUris = editQrCodeUris.toMutableList().apply { removeAt(index) } }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = AccentPink)
                            }
                        }
                    }
                }
                Box(modifier = Modifier.height(60.dp).width(60.dp).clickable { qrCodeLauncher.launch("image/*") }.background(AccentCyan.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Add QR", tint = AccentCyan)
                }
            }"""
content = content.replace(old_qr_ui, new_qr_ui)


old_save = """                        viewModel.editHotel(hotel.copy(
                            name = editHotelName,
                            advancedPayment = advanced,
                            phoneNumber = editPhone,
                            location = editLocation,
                            upiId = editUpiId,
                            qrCodeUri = editQrCodeUri
                        ))"""
new_save = """                        viewModel.editHotel(hotel.copy(
                            name = editHotelName,
                            advancedPayment = advanced,
                            phoneNumber = editPhone,
                            location = editLocation,
                            upiId = editUpiId,
                            qrCodeUri = editQrCodeUris.joinToString(",")
                        ))"""
content = content.replace(old_save, new_save)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
