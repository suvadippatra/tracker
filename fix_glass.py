import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    text = f.read()

helper = """@Composable
fun isAppLightMode(): Boolean {
    val themeMode = LocalAppTheme.current
    return if (themeMode == "Auto") !androidx.compose.foundation.isSystemInDarkTheme() else themeMode == "Light"
}

@Composable
fun AppTextColor(): Color {
    val isLight = isAppLightMode()
    
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as android.app.Activity).window
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
        }
    }
    
    return if (isLight) Color(0xFF1F1F1F) else Color.White
}"""

old_apptextcolor = re.search(r'@Composable\nfun AppTextColor\(\): Color \{.*?\n\}', text, re.DOTALL)
if old_apptextcolor:
    text = text.replace(old_apptextcolor.group(0), helper)
    print("Replaced AppTextColor")

glass_card_old = """fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x2BFFFFFF), // 17% white
                        Color(0x12FFFFFF)  // 7% white
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassBorderTop,
                        GlassBorderBottom
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .drawBehind {
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset.Zero,
                    size = size
                )
            }
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}"""

glass_card_new = """fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val isLight = isAppLightMode()
    val bgColors = if (isLight) {
        listOf(Color(0xE6FFFFFF), Color(0xCCFFFFFF)) // High contrast white glass
    } else {
        listOf(Color(0x2BFFFFFF), Color(0x12FFFFFF))
    }
    val borderColors = if (isLight) {
        listOf(Color(0xFFFFFFFF), Color(0x66FFFFFF))
    } else {
        listOf(GlassBorderTop, GlassBorderBottom)
    }

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(colors = bgColors),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(colors = borderColors),
                shape = RoundedCornerShape(cornerRadius)
            )
            .drawBehind {
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset.Zero,
                    size = size
                )
            }
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}"""
text = text.replace(glass_card_old, glass_card_new)

glass_button_old = re.search(r'fun GlassButton\(.*?content: @Composable RowScope\.\(\) -> Unit\n\) \{.*?\n\}', text, re.DOTALL)
if glass_button_old:
    button_code = glass_button_old.group(0)
    button_code_new = button_code.replace("""borderBrush: Brush = Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom)),""",
                                           """borderBrush: Brush? = null,""")
    button_code_new = button_code_new.replace("""            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x3DFFFFFF), // 24% white
                        Color(0x18FFFFFF)  // 9% white
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = 0.5.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(cornerRadius)
            )""", """            .background(
                brush = Brush.verticalGradient(
                    colors = if (isAppLightMode()) listOf(Color(0xE6FFFFFF), Color(0xCCFFFFFF))
                             else listOf(Color(0x3DFFFFFF), Color(0x18FFFFFF))
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = 1.dp,
                brush = borderBrush ?: Brush.verticalGradient(
                    colors = if (isAppLightMode()) listOf(Color(0xFFFFFFFF), Color(0x99FFFFFF))
                             else listOf(GlassBorderTop, GlassBorderBottom)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )""")
    text = text.replace(button_code, button_code_new)
    print("Replaced GlassButton")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(text)
