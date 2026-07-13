import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

code = code.replace(
    'text = if (language == "bn") "স্মৃতি পকেট লক করা আছে" else "Smriti Pocket is Locked",',
    'text = if (language == "bn") "${userProfile.name.ifBlank { "ব্যবহারকারী" }} এর পকেট লক করা আছে" else "${userProfile.name.ifBlank { "User" }}\'s Pocket is Locked",'
)

code = code.replace(
    'text = if (userProfile.language == "bn") "স্মৃতি পকেট চালু হচ্ছে..." else "Initializing Smriti Pocket...",',
    'text = if (userProfile.language == "bn") "${userProfile.name.ifBlank { "ব্যবহারকারী" }} এর পকেট চালু হচ্ছে..." else "Initializing ${userProfile.name.ifBlank { "User" }}\'s Pocket...",'
)

# And in localized strings:
code = code.replace(
    '"init_app" to "Initializing Smriti Pocket...",',
    '"init_app" to "Initializing Pocket...",'
)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)

print("Updated lock screen and splash screen strings")

