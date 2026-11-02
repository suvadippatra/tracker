import re

with open("app/src/main/java/com/example/ui/screens/meals/AddMealScreen.kt", "r") as f:
    content = f.read()

# Find the bottom action section
bottom_action_start = content.find("// BOTTOM ACTION & TOTAL SUMMARY ROW (LOCKED AT BOTTOM)")
bottom_action_end = content.find("            }\n\n                    }\n                }")
if bottom_action_start != -1 and bottom_action_end != -1:
    bottom_action_code = content[bottom_action_start:bottom_action_end].strip()
    
    # split it to vars and box
    box_start = bottom_action_code.find("Box(\n")
    if box_start == -1:
        box_start = bottom_action_code.find("Box (")
    
    vars_code = bottom_action_code[:box_start].strip()
    box_code = bottom_action_code[box_start:].strip()
    
    # insert vars before Scaffold
    scaffold_start = content.find("Scaffold(\n")
    if scaffold_start != -1:
        new_content = content[:scaffold_start] + vars_code + "\n                    " + content[scaffold_start:bottom_action_start]
        
        # remove the old bottom_action_code and the extra spaces
        # wait, let's just replace bottom_action_code with empty
        new_content = new_content.replace(bottom_action_code, "")
        
        # now insert bottomBar = { box_code } into Scaffold
        top_bar_end = new_content.find("                            }\n                        }\n                    ) { innerPadding ->")
        if top_bar_end != -1:
            top_bar_end_index = top_bar_end + len("                            }\n                        }")
            
            bottom_bar_insertion = "\n                        ,\n                        bottomBar = {\n                            " + box_code.replace("\n", "\n                            ") + "\n                        }"
            
            new_content = new_content[:top_bar_end_index] + bottom_bar_insertion + new_content[top_bar_end_index:]
            
            with open("app/src/main/java/com/example/ui/screens/meals/AddMealScreen.kt", "w") as f:
                f.write(new_content)
            print("Successfully updated AddMealScreen.kt")
        else:
            print("Could not find topBar end")
    else:
        print("Could not find Scaffold")
else:
    print("Could not find bottom action section")
