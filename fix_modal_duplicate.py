import re
with open('app/src/main/java/com/example/ui/screens/EditHotelModal.kt', 'r') as f:
    content = f.read()

# Fix topBar background
content = re.sub(
    r'\.fillMaxWidth\(\)\s*\.background\(getDialogBackground\(\)\)',
    r'.fillMaxWidth()',
    content
)

# Remove bottomBar
bottom_bar_regex = r',\s*bottomBar\s*=\s*\{\s*Surface\(\s*color\s*=\s*getDialogBackground\(\)[\s\S]*?Spacer\(modifier\s*=\s*Modifier\.width\(16\.dp\)\)\s*Button\([^\{]*\{[\s\S]*?\}\s*\}\s*\}\s*\}'
content = re.sub(bottom_bar_regex, '', content)

with open('app/src/main/java/com/example/ui/screens/EditHotelModal.kt', 'w') as f:
    f.write(content)
print("done")
