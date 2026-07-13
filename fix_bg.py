import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

old_bg = """val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFE2E8F0), // Cool slate gray
        Color(0xFFF1F5F9), // Arctic white
        Color(0xFFE2E8F0), // Cool slate gray
        Color(0xFFCBD5E1)  // Warm mist
    )
)"""

new_bg = """val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFD1FF), // Soft pastel pink
        Color(0xFFC2E9FB), // Sky blue
        Color(0xFFA1C4FD)  // Deeper pastel blue
    )
)"""

if old_bg in code:
    code = code.replace(old_bg, new_bg)
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(code)
    print("Replaced LightBackgroundGradient")
else:
    print("Could not find LightBackgroundGradient")
