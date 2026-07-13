with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    lines = f.readlines()

inside_hotel_tab = False
context_found = False
new_lines = []

for line in lines:
    if "fun HotelManagementTab" in line:
        inside_hotel_tab = True
        context_found = False
    
    if inside_hotel_tab and "val context = androidx.compose.ui.platform.LocalContext.current" in line:
        if context_found:
            continue # skip duplicate
        else:
            context_found = True
            
    if "fun AddMealFlowModal" in line:
        inside_hotel_tab = False
        
    new_lines.append(line)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.writelines(new_lines)
