import re

file_path = "app/src/main/java/com/example/ui/screens/ExtraExpensesTab.kt"
with open(file_path, "r") as f:
    content = f.read()

# Revert my last change first just in case
content = content.replace("containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E1E),", "containerColor = getDialogBackground(),")

# Replace with theme aware color
new_code = "containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color(0xFFFFFFFF),"
content = content.replace("containerColor = getDialogBackground(),", new_code)

with open(file_path, "w") as f:
    f.write(content)
