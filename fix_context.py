import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Add context back to EditHotelModal
old_str = """fun EditHotelModal(
    hotel: com.example.data.model.HotelEntity,
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    var editHotelName by remember { mutableStateOf(hotel.name) }"""

new_str = """fun EditHotelModal(
    hotel: com.example.data.model.HotelEntity,
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var editHotelName by remember { mutableStateOf(hotel.name) }"""

content = content.replace(old_str, new_str)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
