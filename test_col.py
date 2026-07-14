import sys

with open('app/src/main/java/com/example/ui/screens/TrakerScreen.kt', 'r') as f:
    lines = f.readlines()
    
count = 0
for i in range(3247, 3879):
    count += lines[i].count('{')
    count -= lines[i].count('}')
print(f"Column braces: {count}")
