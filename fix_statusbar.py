import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

app_code = """@Composable
fun SmritiPocketApp(viewModel: FinancialViewModel) {
    val isInitialized by viewModel.isInitialized.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    var isUnlocked by remember { mutableStateOf(false) }

    // Synchronize locked state on setup"""

app_code_new = """@Composable
fun SmritiPocketApp(viewModel: FinancialViewModel) {
    val isInitialized by viewModel.isInitialized.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    var isUnlocked by remember { mutableStateOf(false) }

    val isLight = if (userProfile.themeMode == "Auto") !androidx.compose.foundation.isSystemInDarkTheme() else userProfile.themeMode == "Light"
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

    // Synchronize locked state on setup"""

if app_code in code:
    code = code.replace(app_code, app_code_new)
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(code)
    print("Fixed status bar logic")
else:
    print("Could not find SmritiPocketApp")

