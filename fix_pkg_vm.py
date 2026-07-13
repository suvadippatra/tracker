with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("import kotlinx.coroutines.flow.first\npackage com.example.ui.viewmodel", "package com.example.ui.viewmodel\nimport kotlinx.coroutines.flow.first")

with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "w") as f:
    f.write(content)
