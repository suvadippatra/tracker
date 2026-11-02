import re

def update_file(filename, new_sizes):
    with open(filename, 'r') as f:
        content = f.read()
    
    def replacer(match):
        return f'android:textSize="{new_sizes.pop(0)}"'
    
    content = re.sub(r'android:textSize="[^"]+"', replacer, content)
    
    with open(filename, 'w') as f:
        f.write(content)

update_file("app/src/main/res/layout/widget_expenses_compact.xml", ["8sp", "12sp", "7sp"])
update_file("app/src/main/res/layout/widget_hotel_compact.xml", ["7sp", "7sp", "7sp", "7sp", "10sp", "6sp", "7sp"])
update_file("app/src/main/res/layout/widget_spend_compact.xml", ["7sp", "6sp", "8sp", "6sp", "8sp", "6sp", "7sp"])
update_file("app/src/main/res/layout/widget_overall_compact.xml", ["8sp", "10sp", "7sp", "7sp"])

print("Sizes fixed!")
