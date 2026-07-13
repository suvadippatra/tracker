import re

# Update strings.xml
with open("app/src/main/res/values/strings.xml", "r") as f:
    strings = f.read()
strings = strings.replace("Smriti Pocket", "Tracker")
with open("app/src/main/res/values/strings.xml", "w") as f:
    f.write(strings)

# Update Kotlin UI File
with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()
code = code.replace("Initializing Smriti Pocket...", "Initializing Tracker...")
code = code.replace("স্মৃতি পকেট চালু হচ্ছে...", "ট্র্যাকার চালু হচ্ছে...")
code = code.replace("স্মৃতি পকেট লক করা আছে", "ট্র্যাকার লক করা আছে")
code = code.replace("Smriti Pocket is Locked", "Tracker is Locked")
code = code.replace("\"স্মৃতি পকেট\"", "\"ট্র্যাকার\"")
code = code.replace("\"Smriti Pocket\"", "\"Tracker\"")
with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)

print("Updated app name to Tracker")
