import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = re.sub(r'CircleShape\)\.bounceClick\(\)\)\s*\)\s*\{', r'CircleShape).bounceClick()\n                    ) {', content)
content = re.sub(r'\)\.bounceClick\(\)\)\s*\)\s*\{', r').bounceClick()\n                    ) {', content)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
