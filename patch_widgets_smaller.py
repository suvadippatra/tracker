import os
import re

widgets = ["widget_overall", "widget_hotel", "widget_spend", "widget_expenses"]
base_dir = "app/src/main/res/layout"

replacements = {
    'paddingStart="12dp"': 'paddingStart="6dp"',
    'paddingEnd="12dp"': 'paddingEnd="6dp"',
    'paddingStart="8dp"': 'paddingStart="4dp"',
    'paddingEnd="8dp"': 'paddingEnd="4dp"',
    'paddingTop="4dp"': 'paddingTop="1dp"',
    'paddingBottom="4dp"': 'paddingBottom="1dp"',
    'paddingTop="2dp"': 'paddingTop="0dp"',
    'paddingBottom="2dp"': 'paddingBottom="0dp"',
    
    'layout_height="22dp"': 'layout_height="18dp"',
    'layout_height="20dp"': 'layout_height="18dp"',
    'layout_height="16dp"': 'layout_height="14dp"',
    
    'textSize="14sp"': 'textSize="11sp"',
    'textSize="11sp"': 'textSize="9sp"',
    'textSize="10sp"': 'textSize="8sp"',
    'textSize="9sp"': 'textSize="8sp"',
    'textSize="8sp"': 'textSize="7sp"',
}

for widget in widgets:
    comp_path = os.path.join(base_dir, f"{widget}_compact.xml")
    
    with open(comp_path, "r") as f:
        content = f.read()
        
    def replacer(match):
        val = match.group(0)
        return replacements.get(val, val)
        
    pattern = re.compile("|".join(re.escape(k) for k in replacements.keys()))
    
    shrunk = pattern.sub(replacer, content)
    
    with open(comp_path, "w") as f:
        f.write(shrunk)

print("Widgets patched to be even smaller!")
