with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "import com.example.ui.components.SlidingDatePickerModal" in line:
        continue
    new_lines.append(line)

new_lines.insert(1, "import com.example.ui.components.SlidingDatePickerModal\n")
    
# Wait, also fix the package line which might have literal '\n'
final_lines = []
for line in new_lines:
    if "\\n" in line:
        line = line.replace("\\n", "\n")
    final_lines.append(line)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.writelines(final_lines)
