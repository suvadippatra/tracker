import re
with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

code = code.replace("Color(0x2BFFFFFF), RoundedCornerShape(8.dp)", "getGlassBorder(), RoundedCornerShape(8.dp)")
code = code.replace("Color(0x3DFFFFFF), RoundedCornerShape(8.dp)", "getGlassBorder(), RoundedCornerShape(8.dp)")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)
print("Fixed Dropdown borders")
