import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'Text("Select Hotel (Optional)", color = AppTextColor(), fontSize = 14.sp)',
    'Text("Select Hotel (Required if no hotel ID in CSV)", color = AppTextColor(), fontSize = 14.sp)'
)

# And let's prevent import if hotel is null and we need one.
# Actually, the user says: "If in the csv file there is no hotel name specified and if that is successfully imported after import this will ask user for the hotel name or choose the hotel."

