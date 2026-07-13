import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Fix showEditHotel -> showEditHotelDialog = true
content = content.replace("showEditHotel = hotel\n                                        showHotelDropdown = false", "showEditHotelDialog = true\n                                        showHotelDropdown = false")

# Add showInfoDialog if not present
if "var showInfoDialog" not in content:
    content = content.replace("var showEditHotelDialog by remember { mutableStateOf(false) }", "var showEditHotelDialog by remember { mutableStateOf(false) }\n    var showInfoDialog by remember { mutableStateOf<HotelEntity?>(null) }")

# Fix string syntax for CSV newline
content = content.replace('sb.append("Date,Type,Total\\n")', 'sb.append("Date,Type,Total\\\\n")')
content = content.replace('sb.append(t.totalAmount).append("\\n")', 'sb.append(t.totalAmount).append("\\\\n")')

# Wait, previously the re.sub resolved \\n into \n (actual newline).
# So the code has actual newlines inside the quotes.
# We need to find `sb.append("Date,Type,Total\n")` and replace with `sb.append("Date,Type,Total\\n")`
content = content.replace('sb.append("Date,Type,Total\n")', 'sb.append("Date,Type,Total\\n")')
content = content.replace('sb.append(t.totalAmount).append("\n")', 'sb.append(t.totalAmount).append("\\n")')

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
print("Fixed final issues 2")
