import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

# Fix the caller
target_caller = r'HotelDetailsBottomSheet\(\s*hotel\s*=\s*showInfoDialog!!,\s*isBengali\s*=\s*isBengali,\s*onDismiss\s*=\s*\{\s*showInfoDialog\s*=\s*null\s*},\s*onEdit\s*=\s*\{\s*hotelToEdit\s*=\s*showInfoDialog\s*showInfoDialog\s*=\s*null\s*\}\s*\)'

# Let's just do a simpler replace using string methods instead of regex since it's easier to mess up regex
if "HotelDetailsBottomSheet(\n            hotel = showInfoDialog!!,\n            isBengali = isBengali,\n            onDismiss =" in content:
    content = content.replace(
        "HotelDetailsBottomSheet(\n            hotel = showInfoDialog!!,\n            isBengali = isBengali,\n            onDismiss =",
        "HotelDetailsBottomSheet(\n            hotel = showInfoDialog!!,\n            isBengali = isBengali,\n            viewModel = viewModel,\n            onDismiss ="
    )
    with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
        f.write(content)
    print("Fixed caller")
else:
    print("Not found")

