import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

start_marker = "            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {"
end_marker = "            Spacer(modifier = Modifier.height(32.dp))\n        }\n    }\n}"

new_content = """            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Menu Chart", color = AppTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                var showAddMenu by remember { mutableStateOf(false) }
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Menu",
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp).clickable { showAddMenu = !showAddMenu }
                )
            }
            Text("Edit menu chart and items. You can add Day, Night, or Common meals.", color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Menu Items List
            val menuItems by viewModel.menuItems.collectAsState(initial = emptyList())
            
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                menuItems.forEach { menuItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${menuItem.emoji} ${menuItem.englishName}", color = AppTextColor(), fontWeight = FontWeight.Bold)
                            Text("₹${menuItem.price} • ${menuItem.type}", color = AppTextColor().copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                        Row {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentCyan, modifier = Modifier.size(20.dp).clickable { /* TODO: inline edit */ })
                            Spacer(Modifier.width(16.dp))
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentPink, modifier = Modifier.size(20.dp).clickable { viewModel.deleteMenu(menuItem) })
                        }
                    }
                }
            }
            
            // TODO Add / Edit Menu Inline form if toggled

            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AppTextColor().copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val adv = editHotelAdvanced.toDoubleOrNull() ?: 0.0
                        viewModel.editHotel(hotel.copy(
                            name = editHotelName,
                            advancedPayment = adv,
                            phoneNumber = editPhone,
                            location = editLocation,
                            upiId = editUpiId
                        ))
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("Save Changes", color = Color.Black)
                }
            }"""

if start_marker in content:
    pre = content.split(start_marker)[0]
    post = content.split(end_marker, 1)[1]
    
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(pre + new_content + end_marker + post)
    print("Replaced Menu Chart section")
else:
    print("Markers not found")
