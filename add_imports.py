with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

imports = """import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot"""

if "import androidx.compose.ui.layout.positionInRoot" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", f"import androidx.compose.ui.Modifier\\n{imports}")
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(content)
