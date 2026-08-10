import re
with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# Replace any sequence of } followed by whitespace then fun importHotelTransactionsFromCsv
# with one less }
match = re.search(r'\}\s*\}\s*\}\s*\}\s*\}\s*fun importHotelTransactionsFromCsv', content)
if match:
    content = content.replace(match.group(0), '} } } } \n    fun importHotelTransactionsFromCsv')
else:
    print("Not found 5")

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
print("done")
