import re

code = """@Composable
fun AppTextColor(): Color {
    val themeMode = LocalAppTheme.current
    val isLight = if (themeMode == "Auto") !androidx.compose.foundation.isSystemInDarkTheme() else themeMode == "Light"
    
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

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    text = f.read()

import re
old_text = re.search(r'@Composable\nfun AppTextColor\(\): Color \{.*?\n\}', text, re.DOTALL)
if old_text:
    text = text.replace(old_text.group(0), code)
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(text)
    print("Replaced AppTextColor")
else:
    print("Failed")
