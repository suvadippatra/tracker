import re

path = "app/src/main/java/com/example/ui/screens/profile/EditProfileScreen.kt"
with open(path, "r") as f:
    content = f.read()

# Replace Dialog with Box
dialog_start = """    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {"""

new_start = """    Box(modifier = Modifier.fillMaxSize()) {"""

content = content.replace(dialog_start, new_start)
content = content.replace("com.example.ui.components.DialogEdgeToEdge()", "")

# The actual screen OwnerProfileScreen also used a Dialog? Let's check!
dialog_start2 = """    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {"""
content = content.replace(dialog_start2, new_start)

with open(path, "w") as f:
    f.write(content)
