import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

helper = """
import androidx.compose.animation.animateContentSize

fun Modifier.hapticClickable(onClick: () -> Unit) = composed {
    val view = androidx.compose.ui.platform.LocalView.current
    this.clickable {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        onClick()
    }
}
"""

if "hapticClickable" not in content:
    content = content.replace("import androidx.compose.animation.animateContentSize", helper)
    
    # replace simple clickables
    content = re.sub(r'\.clickable\s*\{\s*', '.hapticClickable { ', content)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
