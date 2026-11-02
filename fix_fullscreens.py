import re
import os

def fix_file(path):
    with open(path, "r") as f:
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
    
    # Also without com.example.ui.components prefix just in case
    content = content.replace(
"""    FullScreenOverlay(
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
                            .background(com.example.ui.components.getAppBackgroundGradient(themeMode, bgPreset)),
                        contentAlignment = Alignment.TopCenter
                    ) {""",
"""            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = Color.Transparent
            ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.example.ui.components.getDialogBackground(themeMode, bgPreset)),
                        contentAlignment = Alignment.TopCenter
                    ) {"""
    )
    
    # Simpler variant without themeMode, bgPreset
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
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = Color.Transparent
            ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(getDialogBackground()),
                        contentAlignment = Alignment.TopCenter
                    ) {"""
    )
    
    with open(path, "w") as f:
        f.write(content)

fix_file("app/src/main/java/com/example/ui/screens/OwnerProfileScreen.kt")
fix_file("app/src/main/java/com/example/ui/screens/AddTransactionScreen.kt")
print("Done fixing full screens")
