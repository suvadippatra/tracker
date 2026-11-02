import re

with open("app/src/main/java/com/example/ui/screens/OwnerProfileScreen.kt", "r") as f:
    text = f.read()

# Replace EditProfileDialog's Dialog
text = text.replace("androidx.compose.ui.window.Dialog(\n        onDismissRequest = onDismiss,\n        properties = androidx.compose.ui.window.DialogProperties(\n            usePlatformDefaultWidth = false,\n            decorFitsSystemWindows = false\n        )\n    ) {\n        com.example.ui.components.DialogEdgeToEdge()", "com.example.ui.components.FullScreenOverlay(\n        viewModel = viewModel,\n        onDismiss = onDismiss\n    ) {")

# Replace AddOwnerAccountDialog's Dialog
# Wait, they all have the EXACT same string! The above replace will replace all 3 of them!
# (EditProfileDialog, AddOwnerAccountDialog, OwnerQRViewerDialog)

# Replace ViewAccountDialog's Dialog
text = text.replace("Dialog(\n        onDismissRequest = onDismiss,\n        properties = DialogProperties(\n            usePlatformDefaultWidth = false,\n            decorFitsSystemWindows = false,\n            dismissOnClickOutside = true,\n            dismissOnBackPress = true\n        )\n    ) {\n        com.example.ui.components.DialogEdgeToEdge()", "com.example.ui.components.FullScreenOverlay(\n        viewModel = viewModel,\n        onDismiss = onDismiss\n    ) {")

with open("app/src/main/java/com/example/ui/screens/OwnerProfileScreen.kt", "w") as f:
    f.write(text)

