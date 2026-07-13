import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# 1. Update QR Code management
old_qr = """    var editQrCodeUri by remember { mutableStateOf(hotel.qrCodeUri) }
    var showAddMenu by remember { mutableStateOf(false) }

    val qrCodeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            editQrCodeUri = uri.toString()
        }
    }"""
new_qr = """    var editQrCodeUris by remember { mutableStateOf(hotel.qrCodeUri.split(",").filter { it.isNotBlank() }) }
    var showAddMenu by remember { mutableStateOf(false) }

    val qrCodeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            editQrCodeUris = editQrCodeUris + uris.map { it.toString() }
        }
    }"""
content = content.replace(old_qr, new_qr)

# 2. Update Location Maps link logic
old_loc = """                IconButton(
                    onClick = {
                        val query = if (editLocation.isNotBlank()) android.net.Uri.encode(editLocation) else "Restaurants"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$query"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.background(AccentCyan.copy(alpha=0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Open Maps", tint = AccentCyan)
                }"""

new_loc = """                IconButton(
                    onClick = {
                        val intent = if (editLocation.startsWith("http://") || editLocation.startsWith("https://")) {
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(editLocation))
                        } else {
                            val query = if (editLocation.isNotBlank()) android.net.Uri.encode(editLocation) else "Restaurants"
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$query"))
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.background(AccentCyan.copy(alpha=0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Open Maps", tint = AccentCyan)
                }"""
content = content.replace(old_loc, new_loc)

old_loc_label = """label = { Text("Location", color = AppTextColor().copy(alpha = 0.6f)) }"""
new_loc_label = """label = { Text("Maps Link / Location", color = AppTextColor().copy(alpha = 0.6f)) }"""
content = content.replace(old_loc_label, new_loc_label)

# 3. Update Save Button to use multiple QR codes
old_save = """                        val updatedHotel = hotel.copy(
                            name = editHotelName,
                            advancedPayment = adv,
                            phoneNumber = editPhone,
                            location = editLocation,
                            upiId = editUpiId,
                            qrCodeUri = editQrCodeUri
                        )"""
new_save = """                        val updatedHotel = hotel.copy(
                            name = editHotelName,
                            advancedPayment = adv,
                            phoneNumber = editPhone,
                            location = editLocation,
                            upiId = editUpiId,
                            qrCodeUri = editQrCodeUris.joinToString(",")
                        )"""
content = content.replace(old_save, new_save)

# 4. Update the UI for QR codes
old_qr_ui = """            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editQrCodeUri,
                    onValueChange = { editQrCodeUri = it },
                    label = { Text("QR Code URI", color = AppTextColor().copy(alpha = 0.6f)) },
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
                    Icon(Icons.Default.Image, contentDescription = "Select QR", tint = AccentCyan)
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


with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)

