import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Replace FullScreenOverlay with Dialog
content = content.replace(
"""    com.example.ui.components.FullScreenOverlay(
        viewModel = viewModel,
        onDismiss = onDismiss
    ) {""",
"""    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {"""
)

# Change Surface and Box sizing inside UnifiedAccountDialog
content = content.replace(
"""            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(getAppBackgroundGradient()),
                        contentAlignment = Alignment.TopCenter
                    ) {""",
"""            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent
            ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(getDialogBackground()),
                        contentAlignment = Alignment.TopCenter
                    ) {"""
)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
print("Fixed UnifiedAccountDialog")
