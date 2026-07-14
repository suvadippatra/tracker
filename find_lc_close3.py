import sys

def check_braces(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
        
    start_line = 813 # "        ) {"
    
    count = 1
    for i in range(start_line + 1, len(lines)):
        line = lines[i]
        count += line.count('{')
        count -= line.count('}')
        if count == 0:
            print(f"LazyColumn closes at line {i+1}")
            break

check_braces('app/src/main/java/com/example/ui/screens/TrakerScreen.kt')
