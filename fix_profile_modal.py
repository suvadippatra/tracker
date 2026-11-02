import re

with open("app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt", "r") as f:
    content = f.read()

old_dialog = """    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = true
        )
    ) {"""
new_dialog = """    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()"""

content = content.replace(old_dialog, new_dialog)

old_surface = """            Surface(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 24.dp), shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
                Box(
                    modifier = Modifier.fillMaxSize().background(com.example.ui.components.getDialogBackground()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0.dp),"""
new_surface = """            Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                Box(
                    modifier = Modifier.fillMaxSize().background(com.example.ui.components.getAppBackgroundGradient(themeMode, bgPreset)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize().systemBarsPadding(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0.dp),"""
content = content.replace(old_surface, new_surface)

with open("app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt", "w") as f:
    f.write(content)

print("Fixed EditProfileModal")
