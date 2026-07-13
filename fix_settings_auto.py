import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Make ProfileName and ProfileContact update viewModel immediately and remove Apply button.
old_profile_fields = """                                OutlinedTextField(
                                    value = profileName,
                                    onValueChange = { profileName = it },
                                    label = { Text("Display Name", color = AppTextColor().copy(alpha = 0.5f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = AppTextColor(),
                                        unfocusedTextColor = AppTextColor(),
                                        focusedBorderColor = AccentCyan,
                                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = profileContact,
                                    onValueChange = { profileContact = it },
                                    label = { Text("Contact Number", color = AppTextColor().copy(alpha = 0.5f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = AppTextColor(),
                                        unfocusedTextColor = AppTextColor(),
                                        focusedBorderColor = AccentCyan,
                                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                GlassButton(
                                    onClick = {
                                        viewModel.updateProfile(profileName, profileContact, "avatar_premium")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Apply Profile Updates", color = AccentGreen, fontWeight = FontWeight.Bold)
                                }"""

new_profile_fields = """                                OutlinedTextField(
                                    value = profileName,
                                    onValueChange = { 
                                        profileName = it 
                                        viewModel.updateProfile(it, profileContact, "avatar_premium")
                                    },
                                    label = { Text("Display Name", color = AppTextColor().copy(alpha = 0.5f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = AppTextColor(),
                                        unfocusedTextColor = AppTextColor(),
                                        focusedBorderColor = AccentCyan,
                                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = profileContact,
                                    onValueChange = { 
                                        profileContact = it 
                                        viewModel.updateProfile(profileName, it, "avatar_premium")
                                    },
                                    label = { Text("Contact Number", color = AppTextColor().copy(alpha = 0.5f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = AppTextColor(),
                                        unfocusedTextColor = AppTextColor(),
                                        focusedBorderColor = AccentCyan,
                                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(20.dp))
"""
content = content.replace(old_profile_fields, new_profile_fields)

# Same for Preferences (Theme, Language, etc.). They already update their local states, but let's make them update viewModel as well.
# Wait, prefThemeMode updates like `prefThemeMode = ...`
# Let's see how Preferences are saved.
old_save_pref = """                                GlassButton(
                                    onClick = {
                                        viewModel.updatePreferences(
                                            language = prefLanguage,
                                            themeMode = prefThemeMode,
                                            showAdvancedPayment = prefShowAdvancedPayment,
                                            appLockEnabled = prefAppLockEnabled,
                                            appLockPin = prefAppLockPin,
                                            defaultHotelId = prefDefaultHotelId
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (prefLanguage == "bn") "পছন্দ সংরক্ষণ করুন" else "Save Preferences",
                                        color = AccentGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }"""

# Instead of removing the button immediately, maybe we can use LaunchedEffect to sync preferences?
# Let's insert a LaunchedEffect inside the SettingsDialog!
effect = """
    LaunchedEffect(prefLanguage, prefThemeMode, prefShowAdvancedPayment, prefAppLockEnabled, prefAppLockPin, prefDefaultHotelId) {
        viewModel.updatePreferences(
            language = prefLanguage,
            themeMode = prefThemeMode,
            showAdvancedPayment = prefShowAdvancedPayment,
            appLockEnabled = prefAppLockEnabled,
            appLockPin = prefAppLockPin,
            defaultHotelId = prefDefaultHotelId
        )
    }
"""

# Let's find "Dialog(" and put it before that.
dialog_idx = content.find("Dialog(\n        onDismissRequest")
content = content[:dialog_idx] + effect + content[dialog_idx:]
content = content.replace(old_save_pref, "")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
