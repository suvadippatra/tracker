import re

with open("app/src/main/java/com/example/ui/screens/ExtraExpensesTab.kt", "r") as f:
    content = f.read()

target = """                            ModalBottomSheet(
                                onDismissRequest = { showQRDialog = false },
                                containerColor = com.example.ui.components.getDialogBackground(),
                                dragHandle = { BottomSheetDefaults.DragHandle() },
                                
                            ) {"""

replacement = """                            val isLight = com.example.ui.components.isAppLightMode()
                            val solidPopupBg = if (isLight) Color.White else Color(0xFF1E222D)
                            ModalBottomSheet(
                                onDismissRequest = { showQRDialog = false },
                                containerColor = solidPopupBg,
                                dragHandle = { BottomSheetDefaults.DragHandle() },
                                
                            ) {"""

if target in content:
    with open("app/src/main/java/com/example/ui/screens/ExtraExpensesTab.kt", "w") as f:
        f.write(content.replace(target, replacement))
    print("Success")
else:
    print("Failed")
