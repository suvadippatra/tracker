import re
with open('app/src/main/java/com/example/ui/screens/EditHotelModal.kt', 'r') as f:
    content = f.read()

# 1. Add bottomBar back to Scaffold
top_bar_regex = r'(topBar\s*=\s*\{\s*Row\([\s\S]*?\}\s*\)\s*\}\s*\})'
bottom_bar_str = """\\1,
            bottomBar = {
                Surface(
                    color = getDialogBackground(),
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismiss, modifier = Modifier.bounceClick()) {
                            Text("Cancel", color = AccentPink, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                val adv = editHotelAdvanced.toDoubleOrNull() ?: 0.0
                                if (editHotelName.isNotBlank()) {
                                    if (hotel == null) {
                                        viewModel.addHotel(
                                            name = editHotelName,
                                            advancedPayment = adv,
                                            phoneNumber = editPhone,
                                            location = editLocation,
                                            upiId = editUpiId,
                                            qrCodeUri = editQrCodeUris.joinToString(",")
                                        )
                                    } else {
                                        viewModel.editHotel(hotel.copy(
                                            name = editHotelName,
                                            advancedPayment = adv,
                                            phoneNumber = editPhone,
                                            location = editLocation,
                                            upiId = editUpiId,
                                            qrCodeUri = editQrCodeUris.joinToString(",")
                                        ))
                                    }
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp).bounceClick()
                        ) {
                            Text(if (hotel == null) "Add Hotel" else "Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }"""
content = re.sub(top_bar_regex, bottom_bar_str, content)

# 2. Remove inline save button
inline_btn_regex = r'Spacer\(modifier = Modifier\.height\(32\.dp\)\)\s*Button\(\s*onClick = \{\s*// Save logic[\s\S]*?Text\(if \(hotel == null\) "Add Hotel" else "Save Changes", color = Color\.Black, fontWeight = FontWeight\.Bold, fontSize = 16\.sp\)\s*\}'
content = re.sub(inline_btn_regex, 'Spacer(modifier = Modifier.height(32.dp))', content)

with open('app/src/main/java/com/example/ui/screens/EditHotelModal.kt', 'w') as f:
    f.write(content)
print("done")
