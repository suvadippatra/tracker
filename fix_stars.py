with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("MovingStarsBackground(isLight = isAppLightMode())", "if (userProfile.enableStarAnimation) { MovingStarsBackground(isLight = isAppLightMode()) }")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
