import re
with open("app/src/main/java/com/example/ui/screens/AddTransactionScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
"""        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.example.ui.components.getAppBackgroundGradient(userProfile.themeMode))
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {""",
"""        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.ui.components.getDialogBackground())
                    .imePadding(),
                contentAlignment = Alignment.TopCenter
            ) {"""
)
content = content.replace(
"""        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.example.ui.components.getAppBackgroundGradient(userProfile?.themeMode ?: "Auto"))
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {""",
"""        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.ui.components.getDialogBackground())
                    .imePadding(),
                contentAlignment = Alignment.TopCenter
            ) {"""
)

# And we also need to close the Surface block
# Wait, replacing the end is harder. Let me just add the closing brace manually.
# Actually, it's easier to use sed or awk.
