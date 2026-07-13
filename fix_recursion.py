import re
with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

code = code.replace("return if (isAppLightMode()) Color(0xFFFFFFFF) else getDialogBackground()",
                    "return if (isAppLightMode()) Color(0xFFFFFFFF) else Color(0xFF140D2D)")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(code)
print("Fixed recursion")
