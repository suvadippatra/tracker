import re

with open("app/src/main/java/com/example/ui/screens/settings/subscreens/DataBackupSettingsScreen.kt", "r") as f:
    content = f.read()

# Remove the "Account Details (All Accounts)" block.
# Lines 530 to 550 approx.
pattern = re.compile(r'\s*// 4\. Account Details / Virtual Accounts.*?item \{.*?DataBackupItemCard\([\s\S]*?showCsvFormatDialogFor = BackupDataType\.BANK_ACCOUNTS \}.*?\).*?\}', re.DOTALL)
content = pattern.sub('', content)

with open("app/src/main/java/com/example/ui/screens/settings/subscreens/DataBackupSettingsScreen.kt", "w") as f:
    f.write(content)
