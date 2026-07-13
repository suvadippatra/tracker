with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
imports = set()
for i, line in enumerate(lines):
    if line.startswith("import "):
        if line.strip() not in imports:
            imports.add(line.strip())
            new_lines.append(line)
    elif "import kotlin.random.Random" in line or "import androidx.compose.ui.geometry.Offset" in line or "import androidx.compose.ui.graphics.Color" in line or "import androidx.compose.foundation.Canvas" in line:
        continue # delete it from middle of file
    else:
        new_lines.append(line)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.writelines(new_lines)
print("Removed duplicate imports")
