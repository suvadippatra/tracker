import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.strip() in [
        "import kotlin.random.Random",
        "import androidx.compose.animation.core.*",
        "import androidx.compose.ui.geometry.Offset",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.foundation.Canvas"
    ]:
        if "kotlin.random.Random" in line and any("kotlin.random.Random" in l for l in new_lines): continue
        if "import androidx.compose.animation.core.*" in line and any("androidx.compose.animation.core.*" in l for l in new_lines): continue
        if "import androidx.compose.ui.geometry.Offset" in line and any("androidx.compose.ui.geometry.Offset" in l for l in new_lines): continue
        if "import androidx.compose.ui.graphics.Color" in line and any("androidx.compose.ui.graphics.Color" in l for l in new_lines): continue
        if "import androidx.compose.foundation.Canvas" in line and any("androidx.compose.foundation.Canvas" in l for l in new_lines): continue
        # Move these imports to top
        new_lines.insert(2, line) # insert after package declaration
    else:
        new_lines.append(line)

code = "".join(new_lines)

bg_code = """Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getAppBackgroundGradient(userProfile.themeMode))
    ) {
        MovingStarsBackground(isLight = isAppLightMode())
"""

if bg_code not in code:
    code = code.replace("""Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getAppBackgroundGradient(userProfile.themeMode))
    ) {""", bg_code)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)
print("Cleaned up imports and added stars background")

