import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

bad_apptextcolor = """@Composable
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

good_apptextcolor = """@Composable
fun AppTextColor(): Color {
    val isLight = isAppLightMode()
    
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is android.app.Activity) break
                context = context.baseContext
            }
            if (context is android.app.Activity) {
                val window = context.window
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
            }
        }
    }
    
    return if (isLight) Color(0xFF1F1F1F) else Color.White
}"""

if bad_apptextcolor in code:
    code = code.replace(bad_apptextcolor, good_apptextcolor)
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(code)
    print("Fixed ClassCastException in AppTextColor")
else:
    print("Could not find bad AppTextColor")

