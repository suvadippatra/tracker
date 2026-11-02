import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Replace Scaffold( \n modifier = Modifier.fillMaxSize().systemBarsPadding(), \n modifier = Modifier.fillMaxSize(),
# with Scaffold( \n modifier = Modifier.fillMaxSize().systemBarsPadding(),
content = re.sub(
    r"Scaffold\(\s*modifier = Modifier\.fillMaxSize\(\)\.systemBarsPadding\(\),\s*modifier = Modifier\.fillMaxSize\(\),",
    "Scaffold(\n                    modifier = Modifier.fillMaxSize().systemBarsPadding(),",
    content
)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
