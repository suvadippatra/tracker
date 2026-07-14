import sys

def check_braces(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
        
    start_line = 3170 # 0-indexed
    end_line = 4024
    
    count = 0
    for i in range(start_line, end_line + 1):
        line = lines[i]
        count += line.count('{')
        count -= line.count('}')
        if count < 0:
            print(f"Mismatched brace at line {i+1}, count: {count}")
    
    print(f"Final count: {count}")

check_braces('app/src/main/java/com/example/ui/screens/TrakerScreen.kt')
