import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Replace Accents
content = content.replace("val AccentGold = Color(0xFFFFD700)", "val AccentGold = Color(0xFFD84315)") # Deep Orange, better contrast
content = content.replace("val AccentCyan = Color(0xFF00E5FF)", "val AccentCyan = Color(0xFF0091EA)") # Deep Cyan/Blue
content = content.replace("val AccentPink = Color(0xFFFF4081)", "val AccentPink = Color(0xFFC2185B)") # Deep Pink
content = content.replace("val AccentGreen = Color(0xFF00E676)", "val AccentGreen = Color(0xFF00B0FF)") # Replaced Green with Blue for better contrast, or 0xFF00A000

# Fix LightBackgroundGradient
old_light_grad = """val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFAFAFA), // Explicitly high-contrast white background
        Color(0xFFF0F0F0), 
        Color(0xFFE8E8E8)
    )
)"""

new_light_grad = """val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFEBF3), // Soft pink
        Color(0xFFE3F2FD), // Soft blue
        Color(0xFFF3E5F5)  // Soft purple
    )
)"""
content = content.replace(old_light_grad, new_light_grad)

old_glass_card = """    val bgColors = if (isLight) {
        listOf(Color(0xFFFFFFFF), Color(0xFFF8F8F8)) // Solid white for maximum contrast
    } else {"""

new_glass_card = """    val bgColors = if (isLight) {
        listOf(Color(0x99FFFFFF), Color(0x66FFFFFF)) // Semi-transparent for liquid glass
    } else {"""
content = content.replace(old_glass_card, new_glass_card)

# Update AppTextColor to be very dark in light mode
content = content.replace("return if (isAppLightMode()) Color(0xFF111111) else Color(0xFFFFFFFF)", "return if (isAppLightMode()) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
