import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

# Fix Settings crash
old_settings = """    var profileName by remember(userProfile) { mutableStateOf(userProfile.name) }
    var profileContact by remember(userProfile) { mutableStateOf(userProfile.contact) }
    var prefLanguage by remember(userProfile) { mutableStateOf(userProfile.language) }
    var prefThemeMode by remember(userProfile) { mutableStateOf(userProfile.themeMode) }
    var prefShowAdvancedPayment by remember(userProfile) { mutableStateOf(userProfile.showAdvancedPayment) }
    var prefAppLockEnabled by remember(userProfile) { mutableStateOf(userProfile.appLockEnabled) }
    var prefAppLockPin by remember(userProfile) { mutableStateOf(userProfile.appLockPin) }
    var prefDefaultHotelId by remember(userProfile) { mutableStateOf(userProfile.defaultHotelId) }"""

new_settings = """    var profileName by remember(userProfile) { mutableStateOf(userProfile?.name ?: "") }
    var profileContact by remember(userProfile) { mutableStateOf(userProfile?.contact ?: "") }
    var prefLanguage by remember(userProfile) { mutableStateOf(userProfile?.language ?: "en") }
    var prefThemeMode by remember(userProfile) { mutableStateOf(userProfile?.themeMode ?: "Auto") }
    var prefShowAdvancedPayment by remember(userProfile) { mutableStateOf(userProfile?.showAdvancedPayment ?: false) }
    var prefAppLockEnabled by remember(userProfile) { mutableStateOf(userProfile?.appLockEnabled ?: false) }
    var prefAppLockPin by remember(userProfile) { mutableStateOf(userProfile?.appLockPin ?: "") }
    var prefDefaultHotelId by remember(userProfile) { mutableStateOf(userProfile?.defaultHotelId ?: 1) }"""

code = code.replace(old_settings, new_settings)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)

print("Settings crash fixed")
