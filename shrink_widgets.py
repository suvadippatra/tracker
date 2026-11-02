import os
import glob
import re

for xml_file in glob.glob("app/src/main/res/layout/*compact.xml"):
    with open(xml_file, "r") as f:
        content = f.read()

    # Reduce padding in widget_root
    content = re.sub(r'android:paddingStart="[^"]+"', 'android:paddingStart="2dp"', content)
    content = re.sub(r'android:paddingEnd="[^"]+"', 'android:paddingEnd="2dp"', content)
    content = re.sub(r'android:paddingTop="[^"]+"', 'android:paddingTop="0dp"', content)
    content = re.sub(r'android:paddingBottom="[^"]+"', 'android:paddingBottom="0dp"', content)

    # Margins
    content = re.sub(r'android:layout_marginBottom="1dp"', 'android:layout_marginBottom="0dp"', content)
    content = re.sub(r'android:layout_marginTop="1dp"', 'android:layout_marginTop="0dp"', content)
    content = re.sub(r'android:layout_marginEnd="4dp"', 'android:layout_marginEnd="2dp"', content)
    content = re.sub(r'android:layout_marginStart="4dp"', 'android:layout_marginStart="2dp"', content)
    content = re.sub(r'android:layout_margin="[^"]+"', 'android:layout_margin="0dp"', content)

    # Text Sizes
    content = re.sub(r'android:textSize="12sp"', 'android:textSize="9sp"', content)
    content = re.sub(r'android:textSize="10sp"', 'android:textSize="8sp"', content)
    content = re.sub(r'android:textSize="8sp"', 'android:textSize="7sp"', content)
    content = re.sub(r'android:textSize="7sp"', 'android:textSize="6sp"', content)
    content = re.sub(r'android:textSize="6sp"', 'android:textSize="5sp"', content)

    # Heights
    content = re.sub(r'android:layout_height="16dp"', 'android:layout_height="12dp"', content)
    content = re.sub(r'android:layout_height="14dp"', 'android:layout_height="10dp"', content)
    
    with open(xml_file, "w") as f:
        f.write(content)
