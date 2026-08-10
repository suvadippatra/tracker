import os
import re

path = 'app/src/main/java/com/example/ui/screens'
for root, dirs, files in os.walk(path):
    for file in files:
        if file.endswith('.kt'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                content = f.read()
                
            original_content = content
            
            # Remove topBar background getDialogBackground()
            content = re.sub(
                r'topBar\s*=\s*\{\s*Row\(\s*modifier\s*=\s*Modifier\s*\.fillMaxWidth\(\)\s*\.background\(getDialogBackground\(\)\)',
                r'topBar = {\n                Row(\n                    modifier = Modifier\n                        .fillMaxWidth()',
                content
            )
            
            if original_content != content:
                with open(filepath, 'w') as f:
                    f.write(content)
                print(f"Fixed topBar in {file}")
