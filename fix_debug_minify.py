import re
with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

debug_block = r'''    debug \{
      isMinifyEnabled = true
      isShrinkResources = true'''

new_debug = '''    debug {
      isMinifyEnabled = false
      isShrinkResources = false'''

content = re.sub(debug_block, new_debug, content)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
print("Updated debug config")
