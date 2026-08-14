import re

files = [
    "app/src/main/java/com/example/ui/screens/SettingsScreen.kt",
    "app/src/main/java/com/example/ui/screens/settings/components/SettingsComponents.kt",
    "app/src/main/java/com/example/ui/screens/settings/subscreens/AppearanceSettingsScreen.kt",
    "app/src/main/java/com/example/ui/screens/settings/subscreens/DataBackupSettingsScreen.kt"
]

for file in files:
    with open(file, "r") as f:
        content = f.read()
        
    content = content.replace("import com.example.ui.screens.AppTextColor", "import com.example.ui.components.*")
    content = content.replace("import com.example.ui.screens.AccentCyan", "import com.example.ui.components.*")
    content = content.replace("import com.example.ui.components.GlassCard", "import com.example.ui.components.*")
    
    if "SettingsScreen.kt" in file:
        content = re.sub(r"enum class SecurityPinMode.*?\}?", "", content, flags=re.DOTALL)
        # Also need to add import com.example.ui.screens.Constants.* if needed, but wait, SecurityPinMode is in com.example.ui.screens.Constants
        # It's in the same package (com.example.ui.screens).
    
    with open(file, "w") as f:
        f.write(content)

