import re

def fix_edit_hotel():
    with open("app/src/main/java/com/example/ui/screens/EditHotelModal.kt", "r") as f:
        content = f.read()
    
    old_dialog = """    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = true
        )
    ) {"""
    new_dialog = """    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()"""
    content = content.replace(old_dialog, new_dialog)

    old_surface = """            Surface(
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
                            .background(com.example.ui.components.getDialogBackground()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                Scaffold(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp),
                    containerColor = Color.Transparent,"""
    new_surface = """            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(com.example.ui.components.getAppBackgroundGradient(themeMode, bgPreset)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    containerColor = Color.Transparent,"""
    content = content.replace(old_surface, new_surface)

    with open("app/src/main/java/com/example/ui/screens/EditHotelModal.kt", "w") as f:
        f.write(content)

fix_edit_hotel()
print("Fixed EditHotelModal")
