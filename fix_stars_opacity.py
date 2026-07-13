with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_color = "val starColor = if (isLight) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.4f)"
new_color = "val starColor = if (isLight) Color.White.copy(alpha = 1.0f) else Color.White.copy(alpha = 0.4f)\n    val shadowColor = Color.Black.copy(alpha = 0.15f)"

old_draw = """            drawCircle(
                color = starColor.copy(alpha = starColor.alpha * twinkle),
                radius = star.size,
                center = Offset(x, y)
            )"""

new_draw = """            if (isLight) {
                drawCircle(
                    color = shadowColor.copy(alpha = shadowColor.alpha * twinkle),
                    radius = star.size + 1f,
                    center = Offset(x, y + 1f)
                )
            }
            drawCircle(
                color = starColor.copy(alpha = starColor.alpha * twinkle),
                radius = star.size,
                center = Offset(x, y)
            )"""

content = content.replace(old_color, new_color).replace(old_draw, new_draw)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
