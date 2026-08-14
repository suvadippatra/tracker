import re
with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("repository.emptyDeletedHotelsPermanently()", "Unit")
content = content.replace("repository.emptyDeletedBankAccountsPermanently()", "Unit")
content = content.replace("repository.emptyDeletedMenuItemsPermanently()", "Unit")

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
