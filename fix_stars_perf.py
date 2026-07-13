import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("val starCount = if (isLight) 100 else 200", "val starCount = if (isLight) 30 else 60")
content = content.replace("val starCount = if (isLight) 20 else 50", "val starCount = if (isLight) 30 else 60")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
