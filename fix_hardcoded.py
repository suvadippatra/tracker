import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

helper2 = """@Composable
fun getDialogBackground(): Color {
    return if (isAppLightMode()) Color(0xFFFFFFFF) else Color(0xFF140D2D)
}

@Composable
fun getGlassBorder(): Color {
    return if (isAppLightMode()) Color(0x33000000) else Color(0x33FFFFFF)
}

@Composable
fun getGlassIconButtonBg(): Color {
    return if (isAppLightMode()) Color(0x1A000000) else Color(0x33FFFFFF)
}

"""
code = code.replace("@Composable\nfun isAppLightMode()", helper2 + "@Composable\nfun isAppLightMode()")

code = code.replace("Color(0xFF140D2D)", "getDialogBackground()")
code = code.replace("Color(0x33FFFFFF), CircleShape", "getGlassIconButtonBg(), CircleShape")
code = code.replace("Color(0x33FFFFFF), RoundedCornerShape", "getGlassBorder(), RoundedCornerShape")
code = code.replace("Color(0x1AFFFFFF)", "getGlassIconButtonBg()")
code = code.replace("Color(0x1FFFFFFF), CircleShape", "getGlassBorder(), CircleShape")

# Also for TopBar avatar placeholder
# .background(Color(0x1FFFFFFF), CircleShape) -> handled above
# .background(Color(0x15FFFFFF), CircleShape)
code = code.replace("Color(0x15FFFFFF), CircleShape", "getGlassIconButtonBg(), CircleShape")

# HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp) -> wait I replaced 0x1AFFFFFF with getGlassIconButtonBg()
# Let's check how many 0x1AFFFFFF there were
with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)

print("Updated hardcoded colors")
