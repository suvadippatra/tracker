import re

file_path = "app/src/main/java/com/example/ui/screens/ExtraExpensesTab.kt"
with open(file_path, "r") as f:
    content = f.read()

# Fix the pager count
old_pager = "pageCount = { (accounts.size + 1).coerceAtLeast(1) }"
new_pager = "pageCount = { accounts.size.coerceAtLeast(1) }"
content = content.replace(old_pager, new_pager)

with open(file_path, "w") as f:
    f.write(content)
