import re

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# Make sure that if no Day/Night explicitly specified, isDayMenu and isNightMenu evaluate to true.
# In FinancialViewModel, we did:
# isDayMenu = if (!isDayRaw && !isNightRaw) true else isDayRaw,
# But what if parts[pOffset + 3] doesn't exist?
# we had: `if (isFullExport && parts.size >= 8)`
# so it exists.

