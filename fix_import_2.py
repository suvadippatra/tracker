import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.ui.components.SlidingDatePickerModal\\npackage com.example.ui.screens", "package com.example.ui.screens\\nimport com.example.ui.components.SlidingDatePickerModal")
content = content.replace("import com.example.ui.components.SlidingDatePickerModal\\n\\n@Composable\\nfun AddMealFlowModal(", "@Composable\\nfun AddMealFlowModal(")
content = content.replace("import com.example.ui.components.SlidingDatePickerModal\\n@Composable\\nfun AddMealFlowModal(", "@Composable\\nfun AddMealFlowModal(")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
