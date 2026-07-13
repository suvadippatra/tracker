import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Fix newline in writer.write
content = content.replace('writer.write("Name,Price,Type,Emoji\\n")', 'writer.write("Name,Price,Type,Emoji\\n")')
# Since it might be literally broken into lines, let's just use re.sub
content = re.sub(r'writer\.write\("Name,Price,Type,Emoji\n"\)', r'writer.write("Name,Price,Type,Emoji\\n")', content)
content = re.sub(r'writer\.write\("\$\{m\.englishName\},\$\{m\.price\},\$\{m\.type\},\$\{m\.emoji\}\n"\)', r'writer.write("${m.englishName},${m.price},${m.type},${m.emoji}\\n")', content)

# Fix updateHotel -> editHotel
content = content.replace("viewModel.updateHotel(hotel.copy(", "viewModel.editHotel(hotel.copy(")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
