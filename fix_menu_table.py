import re
with open('app/src/main/java/com/example/ui/screens/MenuManagementScreen.kt', 'r') as f:
    content = f.read()

# Remove the GlassCard wrapper around the table
table_regex = r'GlassCard\(\s*modifier = Modifier\.fillMaxSize\(\),\s*cornerRadius = 16\.dp,\s*containerAlpha = 3f\s*\)\s*\{\s*(Column\(modifier = Modifier\.fillMaxSize\(\)\) \{[\s\S]*?\}\s*)\}'
content = re.sub(table_regex, r'\1', content)

with open('app/src/main/java/com/example/ui/screens/MenuManagementScreen.kt', 'w') as f:
    f.write(content)
print("done")
