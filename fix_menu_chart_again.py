import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("val menuItems by viewModel.menuItems.collectAsState(initial = emptyList())", "val menuItems by viewModel.allMenuItems.collectAsState(initial = emptyList())")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)

print("Fixed menuItems")
