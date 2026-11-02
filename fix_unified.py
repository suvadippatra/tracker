import re

with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "r") as f:
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

old_scaffold = """                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0.dp),
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 14.dp),"""
new_scaffold = """                Scaffold(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0.dp),
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),"""
content = content.replace(old_scaffold, new_scaffold)

with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "w") as f:
    f.write(content)

print("Fixed UnifiedHotelDialog")
