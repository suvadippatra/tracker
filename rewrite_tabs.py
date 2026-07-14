import sys

def rewrite():
    with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
        lines = f.readlines()
        
    # 1. Rewrite HotelManagementTab
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # In HotelManagementTab LazyColumn
        if "LazyColumn(" in line and "fun HotelManagementTab" in "".join(lines[max(0, i-200):i]):
            pass # just a check
            
        out.append(line)
        i += 1
        
rewrite()
