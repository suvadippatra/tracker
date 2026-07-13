import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Replace the whole menu management section
old_menu_section = """            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Menu Chart", color = AppTextColor(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { csvImportLauncher.launch("text/csv") }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import CSV", tint = AppTextColor())
                    }
                    IconButton(onClick = { csvExportLauncher.launch("menu_${hotel.name.replace(" ", "_")}.csv") }) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = AppTextColor())
                    }
                    IconButton(onClick = { showAddMenu = !showAddMenu }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Menu", tint = AccentCyan)
                    }
                }
            }
            Text("Edit menu chart and items. You can add Day, Night, or Common meals.", color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                if (menuItems.isEmpty()) {
                    Text("No items added yet. Click '+' or import via CSV.", color = AppTextColor().copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                }
                menuItems.forEach { menuItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(getDialogBackground(), RoundedCornerShape(8.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${menuItem.emoji} ${menuItem.englishName}", color = AppTextColor(), fontWeight = FontWeight.Bold)
                            Text("₹${menuItem.price} • ${menuItem.type}", color = AppTextColor().copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentPink, modifier = Modifier.size(24.dp).clickable { viewModel.deleteMenu(menuItem) })
                    }
                }
            }
            
            if (showAddMenu) {
                var newName by remember { mutableStateOf("") }
                var newPrice by remember { mutableStateOf("") }
                var newEmoji by remember { mutableStateOf("🍽️") }
                var newType by remember { mutableStateOf("Day") } // Day, Night, Common
                
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add New Menu Item", color = AccentCyan, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newEmoji, onValueChange = { newEmoji = it },
                                label = { Text("Emoji", color = AppTextColor().copy(alpha = 0.5f)) },
                                modifier = Modifier.weight(0.3f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                            )
                            OutlinedTextField(
                                value = newName, onValueChange = { newName = it },
                                label = { Text("Name", color = AppTextColor().copy(alpha = 0.5f)) },
                                modifier = Modifier.weight(0.7f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newPrice, onValueChange = { newPrice = it },
                                label = { Text("Price (₹)", color = AppTextColor().copy(alpha = 0.5f)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.5f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                            )
                            Row(modifier = Modifier.weight(0.5f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Day", "Night", "Common").forEach { typeOpt ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (newType == typeOpt) AccentCyan else Color.Transparent)
                                            .border(1.dp, AccentCyan, RoundedCornerShape(8.dp))
                                            .clickable { newType = typeOpt }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(typeOpt, color = if (newType == typeOpt) Color.Black else AccentCyan, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val p = newPrice.toDoubleOrNull() ?: 0.0
                                if (newName.isNotBlank()) {
                                    viewModel.addMenu(com.example.data.model.MenuEntity(
                                        banglaName = newName,
                                        englishName = newName,
                                        price = p,
                                        isDayMenu = newType == "Day" || newType == "Common",
                                        isNightMenu = newType == "Night" || newType == "Common",
                                        isAddOn = false,
                                        emoji = newEmoji,
                                        type = newType
                                    ))
                                    newName = ""
                                    newPrice = ""
                                    showAddMenu = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text("Add Item", color = Color.Black)
                        }
                    }
                }
            }"""

new_menu_section = """            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Dish Management", color = AppTextColor(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { csvImportLauncher.launch("text/csv") }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import CSV", tint = AppTextColor())
                    }
                    IconButton(onClick = { csvExportLauncher.launch("menu_${hotel.name.replace(" ", "_")}.csv") }) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = AppTextColor())
                    }
                }
            }
            Text("Simplified list-based menu. Add dishes directly below.", color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                if (menuItems.isEmpty()) {
                    Text("No dishes added yet.", color = AppTextColor().copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                }
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(menuItems.size) { index ->
                        val menuItem = menuItems[index]
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(getDialogBackground(), RoundedCornerShape(8.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${menuItem.emoji} ${menuItem.englishName}", color = AppTextColor(), fontWeight = FontWeight.Bold)
                                Text("₹${menuItem.price} • ${menuItem.type}", color = AppTextColor().copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                            IconButton(onClick = { viewModel.deleteMenu(menuItem) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentPink)
                            }
                        }
                    }
                }
                
                // Inline Add Row
                var newName by remember { mutableStateOf("") }
                var newPrice by remember { mutableStateOf("") }
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().background(getDialogBackground(), RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it },
                        placeholder = { Text("Dish Name", color = AppTextColor().copy(alpha = 0.5f), fontSize = 14.sp) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(), unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedTextField(
                        value = newPrice, onValueChange = { newPrice = it },
                        placeholder = { Text("₹ Price", color = AppTextColor().copy(alpha = 0.5f), fontSize = 14.sp) },
                        modifier = Modifier.width(80.dp).height(50.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(), unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            val p = newPrice.toDoubleOrNull() ?: 0.0
                            if (newName.isNotBlank()) {
                                viewModel.addMenu(com.example.data.model.MenuEntity(
                                    banglaName = newName,
                                    englishName = newName,
                                    price = p,
                                    isDayMenu = true,
                                    isNightMenu = true,
                                    isAddOn = false,
                                    emoji = "🍽️",
                                    type = "Common"
                                ))
                                newName = ""
                                newPrice = ""
                            }
                        },
                        modifier = Modifier.size(40.dp).background(AccentCyan.copy(alpha=0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Dish", tint = AccentCyan)
                    }
                }
            }"""

content = content.replace(old_menu_section, new_menu_section)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
