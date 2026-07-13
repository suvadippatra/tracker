import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    text = f.read()

settings_dialog = re.search(r'fun SettingsDialog.*?^}', text, re.DOTALL | re.MULTILINE)
if settings_dialog:
    print("Found SettingsDialog length: ", len(settings_dialog.group(0)))
    with open("settings_code.txt", "w") as out:
        out.write(settings_dialog.group(0))
