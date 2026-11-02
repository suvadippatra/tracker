import re

path = "app/src/main/java/com/example/ui/screens/HotelManagementTab.kt"
with open(path, "r") as f:
    content = f.read()

# Replace local states with ViewModel calls
content = re.sub(r'var showAddMealFlow by remember.*?$', '', content, flags=re.MULTILINE)
content = re.sub(r'var showCreateHotelModal by remember.*?$', '', content, flags=re.MULTILINE)
content = re.sub(r'var manageMenuForHotel by remember.*?$', '', content, flags=re.MULTILINE)
content = re.sub(r'var hotelToEdit by remember.*?$', '', content, flags=re.MULTILINE)

# Replace assignments
content = content.replace("showAddMealFlow = true", "viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.AddMeal)")
content = content.replace("showAddMealFlow = false", "viewModel.closeRootOverlay()")
content = content.replace("showCreateHotelModal = true", "viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.EditHotel(null))")
content = content.replace("showCreateHotelModal = false", "viewModel.closeRootOverlay()")
content = content.replace("hotelToEdit = hotel", "viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.EditHotel(hotel))")
content = content.replace("manageMenuForHotel = hotelToEdit", "viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.ManageMenu(hotelToEdit!!))")
content = content.replace("manageMenuForHotel = hotel", "viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.ManageMenu(hotel))")

# Remove the actual component calls in HotelManagementTab since they are in TrakerApp now
content = re.sub(r'if \(manageMenuForHotel != null\) \{ MenuManagementScreen.*?\}', '', content, flags=re.DOTALL)
content = re.sub(r'if \(showCreateHotelModal \|\| hotelToEdit != null\).*?EditHotelModal.*?\}', '', content, flags=re.DOTALL)

with open(path, "w") as f:
    f.write(content)
print("Patched HotelManagementTab.kt")
