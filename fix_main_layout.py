import re

path = "app/src/main/java/com/example/ui/screens/MainAppLayout.kt"
with open(path, "r") as f:
    content = f.read()

# Remove the dialogs that are now handled by TrakerApp.kt Root Overlay
content = re.sub(r'var showOwnerProfile by remember.*?$', '', content, flags=re.MULTILINE)
content = re.sub(r'if \(showOwnerProfile\) \{.*?OwnerProfileScreen.*?\}', '', content, flags=re.DOTALL)

# Replace assignments
content = content.replace("showOwnerProfile = true", "viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.OwnerProfile)")
content = content.replace("showOwnerProfile = false", "viewModel.closeRootOverlay()")

with open(path, "w") as f:
    f.write(content)
