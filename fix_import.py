import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.ui.components.SlidingDatePickerModal\\n\\n@Composable\\nfun AddMealFlowModal(", "@Composable\\nfun AddMealFlowModal(")

# add to top
content = "import com.example.ui.components.SlidingDatePickerModal\\n" + content

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
