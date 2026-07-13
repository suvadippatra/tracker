import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

old_light_bg = """val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFD1FF), // Soft pastel pink
        Color(0xFFC2E9FB), // Sky blue
        Color(0xFFA1C4FD)  // Deeper pastel blue
    )
)"""

new_light_bg = """val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF0F4FC), // High-contrast light
        Color(0xFFE2EBF8), 
        Color(0xFFD3E0F5)
    )
)"""

code = code.replace(old_light_bg, new_light_bg)

old_text = """fun AppTextColor(): Color {
    return if (isAppLightMode()) Color(0xFF1F1F1F) else Color.White
}"""

new_text = """fun AppTextColor(): Color {
    return if (isAppLightMode()) Color(0xFF111111) else Color(0xFFFFFFFF)
}"""

code = code.replace(old_text, new_text)

# Also fix the glass cards for higher contrast and speed
old_glass_card = """    val bgColors = if (isLight) {
        listOf(Color(0xE6FFFFFF), Color(0xCCFFFFFF)) // High contrast white glass
    } else {
        listOf(Color(0x2BFFFFFF), Color(0x12FFFFFF))
    }
    val borderColors = if (isLight) {
        listOf(Color(0xFFFFFFFF), Color(0x66FFFFFF))
    } else {
        listOf(GlassBorderTop, GlassBorderBottom)
    }"""

new_glass_card = """    val bgColors = if (isLight) {
        listOf(Color(0xFAFFFFFF), Color(0xF0FFFFFF)) // Higher contrast light mode cards
    } else {
        listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))
    }
    val borderColors = if (isLight) {
        listOf(Color(0xFFFFFFFF), Color(0x88FFFFFF))
    } else {
        listOf(GlassBorderTop, GlassBorderBottom)
    }"""

code = code.replace(old_glass_card, new_glass_card)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)

print("Colors updated for high contrast")
