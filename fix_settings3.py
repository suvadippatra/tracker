with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_show_adv = """                                    GlassSwitch(
                                        checked = prefShowAdvancedPayment,
                                        onCheckedChange = { prefShowAdvancedPayment = it },
                                        activeText = if (prefLanguage == "bn") "হ্যাঁ" else "YES",
                                        inactiveText = if (prefLanguage == "bn") "না" else "NO"
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))"""

new_show_adv = """                                    GlassSwitch(
                                        checked = prefShowAdvancedPayment,
                                        onCheckedChange = { prefShowAdvancedPayment = it },
                                        activeText = if (prefLanguage == "bn") "হ্যাঁ" else "YES",
                                        inactiveText = if (prefLanguage == "bn") "না" else "NO"
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (prefLanguage == "bn") "স্টার অ্যানিমেশন" else "Star Animation",
                                            color = AppTextColor(),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (prefLanguage == "bn") "ব্যাকগ্রাউন্ডে তারা দেখানো হবে" else "Enable moving stars in background",
                                            color = AppTextColor().copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    GlassSwitch(
                                        checked = prefEnableStarAnimation,
                                        onCheckedChange = { prefEnableStarAnimation = it },
                                        activeText = if (prefLanguage == "bn") "হ্যাঁ" else "ON",
                                        inactiveText = if (prefLanguage == "bn") "না" else "OFF"
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))"""

content = content.replace(old_show_adv, new_show_adv)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
