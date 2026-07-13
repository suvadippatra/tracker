import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

effect = """
    LaunchedEffect(prefLanguage, prefThemeMode, prefShowAdvancedPayment, prefAppLockEnabled, prefAppLockPin, prefDefaultHotelId) {
        viewModel.updatePreferences(
            language = prefLanguage,
            themeMode = prefThemeMode,
            showAdvancedPayment = prefShowAdvancedPayment,
            appLockEnabled = prefAppLockEnabled,
            appLockPin = prefAppLockPin,
            defaultHotelId = prefDefaultHotelId
        )
    }
"""

# Remove the incorrectly placed effect
content = content.replace(effect, "")

# Find SettingsDialog
settings_start = content.find("fun SettingsDialog(")
if settings_start != -1:
    dialog_start = content.find("    Dialog(\n        onDismissRequest", settings_start)
    if dialog_start != -1:
        content = content[:dialog_start] + effect + content[dialog_start:]

# Also fix the Icons.Filled.ArrowBack unresolved reference if we made a typo.
# Icons.AutoMirrored.Filled.ArrowBack requires `androidx.compose.material.icons.automirrored.filled.ArrowBack`.
# Actually, the error says: "Unresolved reference. None of the following candidates is applicable because of a receiver type mismatch: val Icons.Filled.ArrowBack: ImageVector"
# Ah, I replaced `Icons.Default.ArrowBack` with `Icons.AutoMirrored.Filled.ArrowBack`. Wait, `Icons.Default` doesn't have `AutoMirrored`. 
# It's `Icons.AutoMirrored.Filled.ArrowBack` which is an extension on `Icons.AutoMirrored.Filled` ! Wait, maybe we just need to import it, or just use `Icons.AutoMirrored.Filled.ArrowBack`.
# Wait, why was there an error for Icons.Filled.ArrowBack in lines 1433, 1911? Let's search for "ArrowBack"

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
