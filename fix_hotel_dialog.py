import re

with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "r") as f:
    content = f.read()

target = """    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB3000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(24.dp),
            color = getDialogBackground()
        ) {"""

replacement = """    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            if (window != null) {
                window.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT)
                window.setBackgroundDrawableResource(android.R.color.transparent)
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                window.setFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            }
            onDispose {}
        }
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getAppBackgroundGradient())
            ) {"""

if target in content:
    with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "w") as f:
        f.write(content.replace(target, replacement))
    print("Success")
else:
    print("Failed")
