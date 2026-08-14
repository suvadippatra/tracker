import re
with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("        return uriString\n        // Import/Export QR methods", "        return uriString\n    }\n\n    // Import/Export QR methods")

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
