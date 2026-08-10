import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target = """            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (hotel != null && hotelsCount > 1 && onDelete != null) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                        modifier = Modifier.testTag("delete_hotel_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (prefLanguage == "bn") "মুছে ফেলুন" else "Delete", color = Color.White)
                    }
                }
                Button(
                    onClick = {
                        if (hotelName.isNotBlank()) {
                            val adv = hotelAdv.toDoubleOrNull() ?: 0.0
                            onSave(hotelName, adv, hotelPhone, hotelLoc, hotelUpi, hotelQr)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier.testTag("save_hotel_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hotel == null) {
                            if (prefLanguage == "bn") "যোগ করুন" else "Register"
                        } else {
                            if (prefLanguage == "bn") "সংরক্ষণ" else "Save"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_hotel_btn")) {
                    Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AppTextColor())
                }
            }
        }
        }
    }
}"""

replacement = """            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (hotel != null && hotelsCount > 1 && onDelete != null) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                        modifier = Modifier.weight(1f).testTag("delete_hotel_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (prefLanguage == "bn") "মুছে ফেলুন" else "Delete", color = Color.White)
                    }
                }
                Button(
                    onClick = {
                        if (hotelName.isNotBlank()) {
                            val adv = hotelAdv.toDoubleOrNull() ?: 0.0
                            onSave(hotelName, adv, hotelPhone, hotelLoc, hotelUpi, hotelQr)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier.weight(1f).testTag("save_hotel_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hotel == null) {
                            if (prefLanguage == "bn") "যোগ করুন" else "Register"
                        } else {
                            if (prefLanguage == "bn") "সংরক্ষণ" else "Save"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("cancel_hotel_btn")) {
                Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AppTextColor())
            }
        }
    }
}"""

if target in content:
    content = content.replace(target, replacement)
    print("Replaced successfully")
else:
    print("Target not found")

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)

