import re

with open("app/src/main/java/com/example/ui/screens/EditHotelModal.kt", "r") as f:
    content = f.read()

# Replace FullScreenOverlay with Dialog
content = content.replace(
    """    com.example.ui.components.FullScreenOverlay(
        viewModel = viewModel,
        onDismiss = onDismiss
    ) {""",
    """    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = true
        )
    ) {"""
)

# And replace the background and Scaffold usage
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
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent
            ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.example.ui.components.getAppBackgroundGradient(themeMode, bgPreset)),
                        contentAlignment = Alignment.TopCenter
                    ) {"""
)

# The scaffold inside
content = content.replace(
"""                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0.dp),""",
"""                Scaffold(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp),
                    containerColor = Color.Transparent,"""
)


with open("app/src/main/java/com/example/ui/screens/EditHotelModal.kt", "w") as f:
    f.write(content)
