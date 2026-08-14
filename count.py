with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    text = f.read()

count = 0
for i, c in enumerate(text):
    if c == '{': count += 1
    elif c == '}': count -= 1
    if count == 0 and i > 200:
        print(f"Braces hit 0 at index {i}, near: {text[i-20:i+20]}")
