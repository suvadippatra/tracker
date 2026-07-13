import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

# Replace the Theme Toggle Row switch
old_theme_switch = """                                    GlassSwitch(
                                        checked = prefThemeMode == "Light",
                                        onCheckedChange = { isLight ->
                                            prefThemeMode = if (isLight) "Light" else "Dark"
                                        },
                                        activeText = if (prefLanguage == "bn") "লাইট" else "Light",
                                        inactiveText = if (prefLanguage == "bn") "ডার্ক" else "Dark"
                                    )"""

new_theme_switch = """                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(17.dp))
                                            .background(Color(0x1F000000))
                                            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(17.dp))
                                            .clickable {
                                                prefThemeMode = when (prefThemeMode) {
                                                    "Dark" -> "Auto"
                                                    "Auto" -> "Light"
                                                    else -> "Dark"
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (prefThemeMode) {
                                                "Dark" -> if (prefLanguage == "bn") "ডার্ক (Dark)" else "Dark"
                                                "Light" -> if (prefLanguage == "bn") "লাইট (Light)" else "Light"
                                                else -> if (prefLanguage == "bn") "অটো (Auto)" else "Auto"
                                            },
                                            color = AppTextColor(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }"""

if old_theme_switch in code:
    code = code.replace(old_theme_switch, new_theme_switch)
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(code)
    print("Replaced Theme switch")
else:
    print("Could not find Theme switch")
