import re

path = "app/src/main/java/com/example/ui/screens/hotel/EditHotelScreen.kt"
with open(path, "r") as f:
    content = f.read()

# Replace Dialog with Box
dialog_start = """    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {"""

new_start = """    Box(modifier = Modifier.fillMaxSize()) {"""

content = content.replace(dialog_start, new_start)
content = content.replace("com.example.ui.components.DialogEdgeToEdge()", "")

# Instead of just Box, let's give it a Scaffold or solid background so it's fully opaque over the app
# The dialog content currently is Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent)
# Let's replace the first Box in the content with the proper background
content = content.replace("getAppBackgroundGradient(themeMode, bgPreset)", "getAppBackgroundGradient(themeMode, bgPreset)")

with open(path, "w") as f:
    f.write(content)
