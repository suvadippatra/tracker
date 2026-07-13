with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

import re

# Remove Time Picker Launcher from AddMealFlowModal
time_picker_regex = r"// Time Picker Launcher\s*GlassButton\(\s*onClick = \{\s*val cal = Calendar\.getInstance\(\)\.apply \{ timeInMillis = date \}\s*TimePickerDialog\(\s*context,\s*\{ _, hr, min ->\s*cal\.set\(Calendar\.HOUR_OF_DAY, hr\)\s*cal\.set\(Calendar\.MINUTE, min\)\s*viewModel\.setOrderDate\(cal\.timeInMillis\)\s*\},\s*cal\.get\(Calendar\.HOUR_OF_DAY\),\s*cal\.get\(Calendar\.MINUTE\),\s*false\s*\)\.show\(\)\s*\}\s*\) \{\s*Icon\(\s*imageVector = Icons\.Default\.Schedule,\s*contentDescription = \"Time\",\s*tint = AccentCyan\s*\)\s*Spacer\(modifier = Modifier\.width\(8\.dp\)\)\s*Text\(\s*text = sdfTime\.format\(Date\(date\)\),\s*color = AppTextColor\(\),\s*fontSize = 15\.sp,\s*fontWeight = FontWeight\.Bold\s*\)\s*\}"

content = re.sub(time_picker_regex, "", content)

# Remove the Time Picker from the editable history item
edit_time_picker_regex = r"IconButton\(\s*onClick = \{\s*val cal = Calendar\.getInstance\(\)\.apply \{ timeInMillis = timestamp \}\s*TimePickerDialog\(\s*context,\s*\{ _, hr, min ->\s*cal\.set\(Calendar\.HOUR_OF_DAY, hr\)\s*cal\.set\(Calendar\.MINUTE, min\)\s*timestamp = cal\.timeInMillis\s*\},\s*cal\.get\(Calendar\.HOUR_OF_DAY\),\s*cal\.get\(Calendar\.MINUTE\),\s*false\s*\)\.show\(\)\s*\}\s*\) \{\s*Icon\(Icons\.Default\.Schedule, contentDescription = \"Time\", tint = AccentCyan, modifier = Modifier\.size\(20\.dp\)\)\s*\}"

content = re.sub(edit_time_picker_regex, "", content)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
