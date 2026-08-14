import re
with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# I will find the first occurrence of "var searchQuery = androidx.compose.runtime.mutableStateOf("")"
# and cut everything from there to the end.
idx = content.find('var searchQuery = androidx.compose.runtime.mutableStateOf("")')
if idx != -1:
    content = content[:idx] + "}\n"

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
