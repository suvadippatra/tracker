import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

# Fix SettingsDialog to use prefThemeMode
old_dialog = """        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(userProfile.themeMode))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {"""

new_dialog = """        CompositionLocalProvider(LocalAppTheme provides prefThemeMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getAppBackgroundGradient(prefThemeMode))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {"""

if old_dialog in code:
    code = code.replace(old_dialog, new_dialog, 1) # only in SettingsDialog (first occurrence after start)
    # also we need to close CompositionLocalProvider at the end of SettingsDialog...
    # let's just do it carefully.
    print("Found Box in SettingsDialog")
else:
    print("Could not find Box in SettingsDialog")
