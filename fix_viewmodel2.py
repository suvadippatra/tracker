with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("if (current != null) {", "")
content = content.replace("                    )\n                )\n            }", "                    )\n                )")

with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "w") as f:
    f.write(content)
