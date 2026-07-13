import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# For IconButton with modifier:
content = re.sub(r'IconButton\(\s*onClick\s*=\s*(.*?),\s*modifier\s*=\s*Modifier(.*)\)', r'IconButton(onClick = \1, modifier = Modifier\2.bounceClick())', content)
# For IconButton without modifier:
content = re.sub(r'IconButton\(\s*onClick\s*=\s*([^,]*?)\s*\)\s*\{', r'IconButton(onClick = \1, modifier = Modifier.bounceClick()) {', content)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
