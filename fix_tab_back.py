import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'if (manageMenuForHotel != null) { MenuManagementScreen(hotel = manageMenuForHotel!!, viewModel = viewModel, onBack = { hotelToEdit = manageMenuForHotel; manageMenuForHotel = null }) }',
    'if (manageMenuForHotel != null) { MenuManagementScreen(hotel = manageMenuForHotel!!, viewModel = viewModel, onBack = { hotelToEdit = manageMenuForHotel; manageMenuForHotel = null }) }'
)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
