with open("app/src/main/java/com/example/ui/screens/AddTransactionScreen.kt", "r") as f:
    text = f.read()

# find first unmatched brace from the end
lines = text.split('\n')
for i, line in enumerate(lines):
    print(f"{i+1}: {line}")
