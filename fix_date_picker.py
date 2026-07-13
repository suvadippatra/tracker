import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Need to add state for showing date picker modal
# Let's see if there is a place we can add it in AddMealFlowModal
add_meal_modal_def = """@Composable
fun AddMealFlowModal(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {"""

add_meal_modal_new = """import com.example.ui.components.SlidingDatePickerModal

@Composable
fun AddMealFlowModal(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    var showDatePickerModal by remember { mutableStateOf(false) }"""

content = content.replace(add_meal_modal_def, add_meal_modal_new)

old_date_picker_btn = """                                // Date Picker Launcher
                                GlassButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = date }
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                cal.set(Calendar.YEAR, y)
                                                cal.set(Calendar.MONTH, m)
                                                cal.set(Calendar.DAY_OF_MONTH, d)
                                                viewModel.setOrderDate(cal.timeInMillis)
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    cornerRadius = 8.dp
                                ) {"""

new_date_picker_btn = """                                // Date Picker Launcher
                                GlassButton(
                                    onClick = {
                                        showDatePickerModal = true
                                    },
                                    cornerRadius = 8.dp
                                ) {"""

content = content.replace(old_date_picker_btn, new_date_picker_btn)

old_dialog_end = """        }
    }
}

// ==========================================
// QUICK SETTINGS MODAL
// =========================================="""

new_dialog_end = """        }
    }
    
    if (showDatePickerModal) {
        SlidingDatePickerModal(
            initialDateMillis = date,
            onDateSelected = { newMillis ->
                val cal = Calendar.getInstance().apply { timeInMillis = newMillis }
                val currentCal = Calendar.getInstance().apply { timeInMillis = date }
                cal.set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                cal.set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                viewModel.setOrderDate(cal.timeInMillis)
            },
            onDismiss = { showDatePickerModal = false },
            textColor = AppTextColor(),
            backgroundColor = if (isAppLightMode()) Color(0xFFF0F0F0) else Color(0xFF1E1E1E)
        )
    }
}

// ==========================================
// QUICK SETTINGS MODAL
// =========================================="""

content = content.replace(old_dialog_end, new_dialog_end)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
