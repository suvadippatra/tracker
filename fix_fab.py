import re

path = "app/src/main/java/com/example/ui/screens/HotelManagementTab.kt"
with open(path, "r") as f:
    content = f.read()

content = content.replace("if (activeHotel != null && !showAddMealFlow) {", "if (activeHotel != null) {")
content = re.sub(r'// Modal Sheet / Full Dialog for Add Meal.*?if \(showAddMealFlow\).*?onDismiss = \{ viewModel\.closeRootOverlay\(\) \}.*?\}', '', content, flags=re.DOTALL)

with open(path, "w") as f:
    f.write(content)
