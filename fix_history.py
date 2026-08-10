with open('app/src/main/java/com/example/ui/screens/ExtraExpensesTab.kt', 'r') as f:
    content = f.read()

# Let's count the braces in ExtraExpenseDetailedHistoryScreen
screen_code = content.split("fun ExtraExpenseDetailedHistoryScreen(")[1].split("@Composable\nfun TransactionItem(")[0]

open_braces = screen_code.count('{')
close_braces = screen_code.count('}')

print(f"Open: {open_braces}, Close: {close_braces}")

if open_braces > close_braces:
    # We are missing closing braces!
    diff = open_braces - close_braces
    new_screen_code = screen_code + ("\n}" * diff)
    content = content.replace(screen_code, new_screen_code)
    
    with open('app/src/main/java/com/example/ui/screens/ExtraExpensesTab.kt', 'w') as f:
        f.write(content)
        
