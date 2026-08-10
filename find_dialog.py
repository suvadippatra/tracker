import os
for f in os.listdir('app/src/main/java/com/example/ui/screens'):
    if f.endswith('.kt'):
        with open('app/src/main/java/com/example/ui/screens/' + f) as file:
            c = file.read()
            if 'getDialogBackground()' in c:
                print(f)
