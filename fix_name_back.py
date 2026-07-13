import re

# Update strings.xml
with open("app/src/main/res/values/strings.xml", "r") as f:
    strings = f.read()
strings = strings.replace("Tracker", "Smriti Pocket")
with open("app/src/main/res/values/strings.xml", "w") as f:
    f.write(strings)

# Update Kotlin UI File
with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()
code = code.replace("Initializing Tracker...", "Initializing Smriti Pocket...")
code = code.replace("ট্র্যাকার চালু হচ্ছে...", "স্মৃতি পকেট চালু হচ্ছে...")
code = code.replace("ট্র্যাকার লক করা আছে", "স্মৃতি পকেট লক করা আছে")
code = code.replace("Tracker is Locked", "Smriti Pocket is Locked")
code = code.replace("\"ট্র্যাকার\"", "\"স্মৃতি পকেট\"")
code = code.replace("\"Tracker\"", "\"Smriti Pocket\"")
with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)

with open("metadata.json", "r") as f:
    import json
    data = json.load(f)
data["name"] = "Smriti Pocket"
with open("metadata.json", "w") as f:
    json.dump(data, f, indent=2)

with open("settings.gradle.kts", "r") as f:
    sett = f.read()
sett = sett.replace("Tracker", "Smriti Pocket")
with open("settings.gradle.kts", "w") as f:
    f.write(sett)

print("Reverted to Smriti Pocket")
