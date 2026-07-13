with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'sb.append("Date,Type,Total' in line:
        lines[i] = '                                        sb.append("Date,Type,Total\\\\n")\n'
    elif 'sb.append(t.totalAmount).append' in line:
        lines[i] = '                                            sb.append(t.totalAmount).append("\\\\n")\n'

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.writelines(lines)
print("Fixed final issues 5")
