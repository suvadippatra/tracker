import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_end = """}

// ==========================================
// CHRONOLOGICAL VIEW MEAL HISTORY MODAL
// =========================================="""

new_end = """    if (showDatePickerModal) {
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
            backgroundColor = if (isAppLightMode()) androidx.compose.ui.graphics.Color(0xFFF0F0F0) else androidx.compose.ui.graphics.Color(0xFF1E1E1E)
        )
    }
}

// ==========================================
// CHRONOLOGICAL VIEW MEAL HISTORY MODAL
// =========================================="""

content = content.replace(old_end, new_end)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
