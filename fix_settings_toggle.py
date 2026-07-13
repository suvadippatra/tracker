import re
with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

code = code.replace(".background(Color(0x1F000000))\n                                            .border(0.5.dp, getGlassBorder(), RoundedCornerShape(17.dp))",
                    ".background(getGlassIconButtonBg())\n                                            .border(1.dp, getGlassBorder(), RoundedCornerShape(17.dp))")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)
print("Fixed Settings Toggle")
