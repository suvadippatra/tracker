import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

# Make getAppBackgroundGradient a Composable
old_func = """fun getAppBackgroundGradient(themeMode: String): Brush {
    return if (themeMode == "Light") LightBackgroundGradient else DarkBackgroundGradient
}

val LocalAppTheme = compositionLocalOf { "Dark" }

@Composable
fun AppTextColor(): Color {
    return if (LocalAppTheme.current == "Light") Color(0xFF1F1F1F) else Color.White
}"""

new_func = """@Composable
fun getAppBackgroundGradient(themeMode: String): Brush {
    val isLight = if (themeMode == "Auto") !androidx.compose.foundation.isSystemInDarkTheme() else themeMode == "Light"
    return if (isLight) LightBackgroundGradient else DarkBackgroundGradient
}

val LocalAppTheme = compositionLocalOf { "Auto" }

@Composable
fun AppTextColor(): Color {
    val themeMode = LocalAppTheme.current
    val isLight = if (themeMode == "Auto") !androidx.compose.foundation.isSystemInDarkTheme() else themeMode == "Light"
    return if (isLight) Color(0xFF1F1F1F) else Color.White
}"""

if old_func in code:
    code = code.replace(old_func, new_func)
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(code)
    print("Success replacing theme functions")
else:
    print("Could not find theme functions")
