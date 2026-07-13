with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

import re

# Add state variable
if "var showDatePickerModal by remember { mutableStateOf(false) }" not in content[content.find("fun AddExpenseDialog"):content.find("fun SettingsDialog")]:
    content = content.replace("var showAccountDropdown by remember { mutableStateOf(false) }", "var showAccountDropdown by remember { mutableStateOf(false) }\n    var showDatePickerModal by remember { mutableStateOf(false) }")

# Replace TimePickerDialog
time_picker_regex = r"IconButton\(\s*onClick = \{\s*val cal = Calendar\.getInstance\(\)\.apply \{ timeInMillis = timestamp \}\s*TimePickerDialog\([\s\S]*?\}\s*\)\s*\{\s*Icon\(Icons\.Default\.Schedule, contentDescription = null, tint = AccentCyan, modifier = Modifier\.size\(\d+\.dp\)\)\s*\}"

content = re.sub(r"IconButton\(\s*onClick = \{\s*val cal = Calendar\.getInstance\(\)\.apply \{ timeInMillis = timestamp \}\s*TimePickerDialog\([\s\S]*?\}\s*\)\s*\{\s*Icon\(Icons\.Default\.Schedule, contentDescription = null, tint = AccentCyan, modifier = Modifier\.size\(\d+\.dp\)\)\s*\}", "", content)

# Replace DatePickerDialog
date_picker_regex = r"val cal = Calendar\.getInstance\(\)\.apply \{ timeInMillis = timestamp \}\s*DatePickerDialog\([\s\S]*?\.show\(\)"
content = re.sub(date_picker_regex, "showDatePickerModal = true", content)

# Inject SlidingDatePickerModal at the end of the Dialog Box
modal_injection = """    if (showDatePickerModal) {
        SlidingDatePickerModal(
            initialDateMillis = timestamp,
            onDateSelected = { newMillis ->
                val cal = Calendar.getInstance().apply { timeInMillis = newMillis }
                val currentCal = Calendar.getInstance().apply { timeInMillis = timestamp }
                cal.set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                cal.set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                timestamp = cal.timeInMillis
            },
            onDismiss = { showDatePickerModal = false },
            textColor = AppTextColor(),
            backgroundColor = if (isAppLightMode()) androidx.compose.ui.graphics.Color(0xFFF0F0F0) else androidx.compose.ui.graphics.Color(0xFF1E1E1E)
        )
    }
}

@Composable
fun SettingsDialog"""

content = content.replace("}\n\n@Composable\nfun SettingsDialog", modal_injection)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
