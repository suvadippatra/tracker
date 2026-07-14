import sys

def check_braces(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
        
    start_line = 815 # 0-indexed
    
    count = 0
    for i in range(start_line, len(lines)):
        line = lines[i]
        count += line.count('{')
        count -= line.count('}')
        if count == 0 and i > 815:
            print(f"Closes at line {i+1}")
            break

check_braces('app/src/main/java/com/example/ui/screens/TrakerScreen.kt')
