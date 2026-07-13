import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Move showAddMenu
content = content.replace("    var editUpiId by remember { mutableStateOf(hotel.upiId) }\n", "    var editUpiId by remember { mutableStateOf(hotel.upiId) }\n    var showAddMenu by remember { mutableStateOf(false) }\n")

content = content.replace("                var showAddMenu by remember { mutableStateOf(false) }\n", "")

# Fix Spacer(Modifier.width(16.dp))
content = content.replace("Spacer(Modifier.width(16.dp))", "Spacer(modifier = Modifier.width(16.dp))")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
print("Fixed scope")
