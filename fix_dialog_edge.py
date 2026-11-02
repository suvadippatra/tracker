import re

def fix_dialog(filename):
    with open(filename, 'r') as f:
        content = f.read()
    
    target = """                window.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT)
                window.setBackgroundDrawableResource(android.R.color.transparent)"""
    
    replacement = """                window.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT)
                window.setBackgroundDrawableResource(android.R.color.transparent)
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                window.setFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)"""
    
    new_content = content.replace(target, replacement)
    
    with open(filename, 'w') as f:
        f.write(new_content)

fix_dialog("app/src/main/java/com/example/ui/screens/EditHotelModal.kt")
fix_dialog("app/src/main/java/com/example/ui/screens/SettingsScreen.kt")
