with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_ui = """                                        OutlinedTextField(
                                            value = newHotelAdvanced,
                                            onValueChange = { newHotelAdvanced = it },
                                            label = { Text("Advanced Balance (₹)", color = AppTextColor().copy(alpha = 0.5f)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = AppTextColor(),
                                                unfocusedTextColor = AppTextColor(),
                                                focusedBorderColor = AccentCyan,
                                                unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        GlassButton(
                                            onClick = {
                                                val adv = newHotelAdvanced.toDoubleOrNull() ?: 0.0
                                                if (newHotelName.isNotBlank()) {
                                                    viewModel.addHotel(newHotelName, adv)
                                                    newHotelName = ""
                                                    newHotelAdvanced = ""
                                                }
                                            },"""

new_ui = """                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = newHotelAdvanced,
                                                onValueChange = { newHotelAdvanced = it },
                                                label = { Text("Advance (₹)", color = AppTextColor().copy(alpha = 0.5f)) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                                                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                                                ),
                                                modifier = Modifier.weight(0.4f)
                                            )
                                            OutlinedTextField(
                                                value = newHotelPhone,
                                                onValueChange = { newHotelPhone = it },
                                                label = { Text("Mobile Number", color = AppTextColor().copy(alpha = 0.5f)) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                                                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                                                ),
                                                modifier = Modifier.weight(0.6f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        GlassButton(
                                            onClick = {
                                                val adv = newHotelAdvanced.toDoubleOrNull() ?: 0.0
                                                if (newHotelName.isNotBlank()) {
                                                    viewModel.addHotel(newHotelName, adv, newHotelPhone)
                                                    newHotelName = ""
                                                    newHotelAdvanced = ""
                                                    newHotelPhone = ""
                                                }
                                            },"""

content = content.replace(old_ui, new_ui)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
