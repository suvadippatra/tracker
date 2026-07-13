import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

# Replace Color.White with AppTextColor()
code = code.replace("Color.White", "AppTextColor()")
# Fix modifiers if any
# We also want to replace DeepBackgroundGradient if it exists
code = code.replace('themeMode == "Bright"', 'themeMode == "Light"')
code = code.replace('"Bright"', '"Light"')
code = code.replace('"Bright Aurora"', '"Light Mode"')
code = code.replace('"উজ্জ্বল অরোরা"', '"লাইট মোড"')
code = code.replace('"উজ্জ্বল"', '"Light"')
code = code.replace('"Bright"', '"Light"')

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)
