with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

target = """    CompositionLocalProvider(LocalAppTheme provides userProfile.themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(userProfile.themeMode))
        ) {"""

replacement = """    CompositionLocalProvider(LocalAppTheme provides userProfile.themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(userProfile.themeMode))
        ) {
            MovingStarsBackground(isLight = isAppLightMode())"""

if target in code:
    code = code.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(code)
    print("Stars injected")
else:
    print("Could not find target")

