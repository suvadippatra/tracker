import re
with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# Fix copyToInternalStorage issue
bad_pattern = r'updateWidgets\(\)\s*\}\s*private fun copyToInternalStorage\('
good_str = 'updateWidgets()\n        }\n    }\n\n    private fun copyToInternalStorage('
content = re.sub(bad_pattern, good_str, content)

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
