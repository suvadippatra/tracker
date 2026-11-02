def find_unmatched_braces(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    stack = []
    for i, line in enumerate(lines):
        for j, char in enumerate(line):
            if char == '{':
                stack.append((i+1, j+1))
            elif char == '}':
                if stack:
                    stack.pop()
                else:
                    print(f"Unmatched }} at {i+1}:{j+1}")
                    return
    if stack:
        print(f"Unmatched {{ at {stack}")

find_unmatched_braces('app/src/main/java/com/example/ui/screens/ExtraExpensesTab.kt')
