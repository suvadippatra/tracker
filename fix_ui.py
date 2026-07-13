with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

import re

# We need to import InlineDatePicker
if "import com.example.ui.components.InlineDatePicker" not in content:
    content = content.replace("import com.example.ui.components.SlidingDatePickerModal", "import com.example.ui.components.SlidingDatePickerModal\nimport com.example.ui.components.InlineDatePicker")

# Change the "Date & Time Picked" Card to use InlineDatePicker
old_card = r"""                    // TOP SECTION: DATE/TIME DRUM CONTROLS[\s\S]*?Spacer\(modifier = Modifier\.height\(14\.dp\)\)"""
# Wait, let's just do a string replacement on the exact block.
