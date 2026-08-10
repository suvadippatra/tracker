import re

with open('app/src/main/java/com/example/ui/screens/EditHotelModal.kt', 'r') as f:
    content = f.read()

# Remove the duplicated bottom bar (Save/Cancel) from the end of the view
# Wait, let me check where the duplicate button is in EditHotelModal
