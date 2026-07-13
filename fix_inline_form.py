import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

start_marker = "            // TODO Add / Edit Menu Inline form if toggled"

inline_form = """            if (showAddMenu) {
                var newName by remember { mutableStateOf("") }
                var newPrice by remember { mutableStateOf("") }
                var newEmoji by remember { mutableStateOf("🍽️") }
                var newType by remember { mutableStateOf("Day") } // Day, Night, Common
                
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
                                    showAddMenu = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text("Save Menu Item", color = Color.Black)
                        }
                    }
                }
            }"""

content = content.replace(start_marker, inline_form)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)

print("Fixed inline form")
