import re

with open("app/src/main/java/com/example/ui/screens/AddTransactionScreen.kt", "r") as f:
    content = f.read()

# find the box
box_pattern = r"Box\(\s*modifier = Modifier\s*\.fillMaxSize\(\)\s*\.background\(com\.example\.ui\.components\.getAppBackgroundGradient\(.*?\)\)\s*\.imePadding\(\),\s*contentAlignment = Alignment\.TopCenter\s*\)\s*\{"

replacement = """Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.ui.components.getDialogBackground())
                    .imePadding(),
                contentAlignment = Alignment.TopCenter
            ) {"""

content = re.sub(box_pattern, replacement, content)

# Now find the end of the AppThemeProvider block.
# Actually, the AppThemeProvider block ends right before the AlertDialog block (if there is one) or before the end of the function.
# Let's just run sed to add the missing brace.
with open("app/src/main/java/com/example/ui/screens/AddTransactionScreen.kt", "w") as f:
    f.write(content)

print("Replaced Box in AddTransactionScreen")
