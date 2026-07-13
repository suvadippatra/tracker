with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Change LightBackgroundGradient to something that contrasts with white stars
old_gradient = """val LightBackgroundGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFE0B2), // Warm Orange/Peach
        Color(0xFFE1BEE7), // Soft Purple
        Color(0xFFB2EBF2)  // Soft Cyan
    )
)"""

new_gradient = """val LightBackgroundGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF81D4FA), // Deep Sky Blue
        Color(0xFFCE93D8), // Vibrant Purple
        Color(0xFFFFCC80)  // Saturated Orange
    )
)"""

content = content.replace(old_gradient, new_gradient)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
