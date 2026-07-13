with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("var prefDefaultHotelId by remember(userProfile) { mutableStateOf(userProfile?.defaultHotelId ?: 1) }", 
"var prefDefaultHotelId by remember(userProfile) { mutableStateOf(userProfile?.defaultHotelId ?: 1) }\n    var prefEnableStarAnimation by remember(userProfile) { mutableStateOf(userProfile?.enableStarAnimation ?: true) }")

content = content.replace(
    "LaunchedEffect(prefLanguage, prefThemeMode, prefShowAdvancedPayment, prefAppLockEnabled, prefAppLockPin, prefDefaultHotelId) {\n        viewModel.updatePreferences(\n            language = prefLanguage,\n            themeMode = prefThemeMode,\n            showAdvancedPayment = prefShowAdvancedPayment,\n            appLockEnabled = prefAppLockEnabled,\n            appLockPin = prefAppLockPin,\n            defaultHotelId = prefDefaultHotelId\n        )",
    "LaunchedEffect(prefLanguage, prefThemeMode, prefShowAdvancedPayment, prefAppLockEnabled, prefAppLockPin, prefDefaultHotelId, prefEnableStarAnimation) {\n        viewModel.updatePreferences(\n            language = prefLanguage,\n            themeMode = prefThemeMode,\n            showAdvancedPayment = prefShowAdvancedPayment,\n            appLockEnabled = prefAppLockEnabled,\n            appLockPin = prefAppLockPin,\n            defaultHotelId = prefDefaultHotelId,\n            enableStarAnimation = prefEnableStarAnimation\n        )"
)

content = content.replace(
    "                                        viewModel.updatePreferences(\n                                            language = prefLanguage,\n                                            themeMode = prefThemeMode,\n                                            showAdvancedPayment = prefShowAdvancedPayment,\n                                            appLockEnabled = prefAppLockEnabled,\n                                            appLockPin = if (prefAppLockPin.length == 4) prefAppLockPin else \"1234\",\n                                            defaultHotelId = prefDefaultHotelId\n                                        )",
    "                                        viewModel.updatePreferences(\n                                            language = prefLanguage,\n                                            themeMode = prefThemeMode,\n                                            showAdvancedPayment = prefShowAdvancedPayment,\n                                            appLockEnabled = prefAppLockEnabled,\n                                            appLockPin = if (prefAppLockPin.length == 4) prefAppLockPin else \"1234\",\n                                            defaultHotelId = prefDefaultHotelId,\n                                            enableStarAnimation = prefEnableStarAnimation\n                                        )"
)

# And now we need to add a toggle for Star Animation in the Preferences tab
preferences_toggle = """                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Show Advanced Payment", color = AppTextColor(), fontSize = 16.sp)
                                    GlassSwitch(checked = prefShowAdvancedPayment, onCheckedChange = { prefShowAdvancedPayment = it })
                                }"""

preferences_toggle_new = """                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Show Advanced Payment", color = AppTextColor(), fontSize = 16.sp)
                                    GlassSwitch(checked = prefShowAdvancedPayment, onCheckedChange = { prefShowAdvancedPayment = it })
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Star Animation", color = AppTextColor(), fontSize = 16.sp)
                                    GlassSwitch(checked = prefEnableStarAnimation, onCheckedChange = { prefEnableStarAnimation = it })
                                }"""

content = content.replace(preferences_toggle, preferences_toggle_new)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
