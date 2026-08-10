import re

# 1. Fix HotelManagementTab: Make back from MenuEditor return to EditHotelModal
with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    hm_content = f.read()

hm_content = hm_content.replace(
    'if (manageMenuForHotel != null) { MenuManagementScreen(hotel = manageMenuForHotel!!, viewModel = viewModel, onBack = { hotelToEdit = manageMenuForHotel; manageMenuForHotel = null }) }',
    'if (manageMenuForHotel != null) { MenuManagementScreen(hotel = manageMenuForHotel!!, viewModel = viewModel, onBack = { hotelToEdit = manageMenuForHotel; manageMenuForHotel = null }) }'
)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(hm_content)


# 2. Fix MenuManagementScreen: remove card, spread table over screen
with open('app/src/main/java/com/example/ui/screens/MenuManagementScreen.kt', 'r') as f:
    menu_content = f.read()

old_card = """            // Table Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {"""

new_card = """            // Table Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {"""

menu_content = menu_content.replace(old_card, new_card)

with open('app/src/main/java/com/example/ui/screens/MenuManagementScreen.kt', 'w') as f:
    f.write(menu_content)


