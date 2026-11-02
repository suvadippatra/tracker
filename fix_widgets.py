import re
import glob

files = glob.glob("app/src/main/res/layout/*compact*.xml")

# Because I messed them up with sed, let's just reset them based on some rules or rewrite them explicitly.
# Wait, I don't have the original, so let's just parse the XML and adjust the textSize values.
for file in files:
    with open(file, "r") as f:
        content = f.read()
    
    # Increase everything by 1 to revert the double-down effect from sed?
    # Not exactly, let's just set them manually or use regex carefully.
    
    # For now, let's see what sizes are in widget_spend_compact.xml
    print(f"{file} textSizes:", re.findall(r'android:textSize="([^"]+)"', content))

