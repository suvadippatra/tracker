import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("CircleShape.bounceClick()", "CircleShape).bounceClick()")
content = content.replace("getGlassIconButtonBg(), CircleShape).bounceClick()", "getGlassIconButtonBg(), CircleShape).bounceClick()")
content = content.replace("AccentCyan, CircleShape).bounceClick()", "AccentCyan, CircleShape).bounceClick()")
content = content.replace("RoundedCornerShape(8.dp).bounceClick()", "RoundedCornerShape(8.dp)).bounceClick()")
content = content.replace("RoundedCornerShape(20.dp).bounceClick()", "RoundedCornerShape(20.dp)).bounceClick()")

# Any remaining .bounceClick() inside a parenthesis might exist. Let's just do a regex if needed.
content = re.sub(r'CircleShape\)\.bounceClick\(\)', r'CircleShape).bounceClick()', content)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
