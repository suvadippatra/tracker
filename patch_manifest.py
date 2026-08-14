import re

with open('/app/applet/app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

if 'android.permission.CAMERA' not in content:
    target = '<application'
    repl = '<uses-permission android:name="android.permission.CAMERA" />\n    <application'
    content = content.replace(target, repl)
    with open('/app/applet/app/src/main/AndroidManifest.xml', 'w') as f:
        f.write(content)
    print("Added CAMERA permission")
else:
    print("CAMERA permission already exists")
