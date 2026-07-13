import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Replace LightBackgroundGradient
old_light_grad = """val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF0F4FC), // High-contrast light
        Color(0xFFE2EBF8), 
        Color(0xFFD3E0F5)
    )
)"""

new_light_grad = """val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFAFAFA), // Explicitly high-contrast white background
        Color(0xFFF0F0F0), 
        Color(0xFFE8E8E8)
    )
)"""

content = content.replace(old_light_grad, new_light_grad)

# Replace GlassCard bgColors
old_glass_card = """    val bgColors = if (isLight) {
        listOf(Color(0xFAFFFFFF), Color(0xF0FFFFFF)) // Higher contrast light mode cards
    } else {"""

new_glass_card = """    val bgColors = if (isLight) {
        listOf(Color(0xFFFFFFFF), Color(0xFFF8F8F8)) // Solid white for maximum contrast
    } else {"""

content = content.replace(old_glass_card, new_glass_card)

# Replace GlassBorder
old_glass_border = """@Composable
fun getGlassBorder(): Color {
    return if (isAppLightMode()) Color(0x33000000) else Color(0x33FFFFFF)
}"""

new_glass_border = """@Composable
fun getGlassBorder(): Color {
    return if (isAppLightMode()) Color(0x40000000) else Color(0x33FFFFFF)
}"""

content = content.replace(old_glass_border, new_glass_border)

# Replace GlassButton light colors
old_button_colors = """                brush = Brush.verticalGradient(
                    colors = if (isAppLightMode()) listOf(Color(0xE6FFFFFF), Color(0xCCFFFFFF))
                             else listOf(Color(0x3DFFFFFF), Color(0x18FFFFFF))
                ),"""

new_button_colors = """                brush = Brush.verticalGradient(
                    colors = if (isAppLightMode()) listOf(Color(0xFFFFFFFF), Color(0xFFF0F0F0))
                             else listOf(Color(0x3DFFFFFF), Color(0x18FFFFFF))
                ),"""

content = content.replace(old_button_colors, new_button_colors)

# Replace GlassButton borders
old_button_borders = """                brush = borderBrush ?: Brush.verticalGradient(
                    colors = if (isAppLightMode()) listOf(Color(0xFFFFFFFF), Color(0x99FFFFFF))
                             else listOf(GlassBorderTop, GlassBorderBottom)
                ),"""

new_button_borders = """                brush = borderBrush ?: Brush.verticalGradient(
                    colors = if (isAppLightMode()) listOf(Color(0x66000000), Color(0x33000000))
                             else listOf(GlassBorderTop, GlassBorderBottom)
                ),"""

content = content.replace(old_button_borders, new_button_borders)

# Increase Stars
content = content.replace("val starCount = if (isLight) 20 else 50", "val starCount = if (isLight) 100 else 200")

# Update Smriti pocket text in Bengali
content = content.replace('"init_app" to "স্মৃতি পকেট চালু হচ্ছে...",', '"init_app" to "পকেট চালু হচ্ছে...",')

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
