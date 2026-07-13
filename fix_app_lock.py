import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

old_app_lock = """@Composable
fun AppLockScreen(
    correctPin: String,
    language: String,
    onUnlock: () -> Unit
) {"""

new_app_lock = """@Composable
fun AppLockScreen(
    correctPin: String,
    language: String,
    userName: String,
    onUnlock: () -> Unit
) {"""

code = code.replace(old_app_lock, new_app_lock)

old_app_lock_call = """                AppLockScreen(
                    correctPin = userProfile.appLockPin,
                    language = userProfile.language,
                    onUnlock = { isUnlocked = true }
                )"""

new_app_lock_call = """                AppLockScreen(
                    correctPin = userProfile.appLockPin,
                    language = userProfile.language,
                    userName = userProfile.name,
                    onUnlock = { isUnlocked = true }
                )"""

code = code.replace(old_app_lock_call, new_app_lock_call)

old_lock_text = """        Text(
            text = if (language == "bn") "${userProfile.name.ifBlank { "ব্যবহারকারী" }} এর পকেট লক করা আছে" else "${userProfile.name.ifBlank { "User" }}'s Pocket is Locked","""

new_lock_text = """        Text(
            text = if (language == "bn") "${userName.ifBlank { "ব্যবহারকারী" }} এর পকেট লক করা আছে" else "${userName.ifBlank { "User" }}'s Pocket is Locked","""

code = code.replace(old_lock_text, new_lock_text)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)

print("AppLockScreen fixed")

