import re

file_path = "app/src/main/java/com/example/ui/screens/settings/subscreens/DataBackupSettingsScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# Change "Single Account ($accName)" to "Bank Accounts"
old_title = 'title = "Single Account ($accName)",'
new_title = 'title = "Bank Accounts",'
content = content.replace(old_title, new_title)

# Change subtitle
old_subtitle = 'subtitle = "Export or restore balance & transactions for \'$accName\' only",'
new_subtitle = 'subtitle = "Export or restore all bank account profiles",'
content = content.replace(old_subtitle, new_subtitle)

with open(file_path, "w") as f:
    f.write(content)
