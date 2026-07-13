with open("app/src/main/java/com/example/ui/components/SlidingDatePickerModal.kt", "r") as f:
    content = f.read()

inline_picker = """@Composable
fun InlineDatePicker(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    textColor: Color
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    val monthsStr = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val years = (1900..2100).toList()

    val maxDaysInMonth = remember(selectedMonth, selectedYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonth)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    LaunchedEffect(maxDaysInMonth) {
        if (selectedDay > maxDaysInMonth) {
            selectedDay = maxDaysInMonth
        }
    }

    LaunchedEffect(selectedDay, selectedMonth, selectedYear) {
        val newCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, selectedDay)
        }
        onDateSelected(newCal.timeInMillis)
    }

    val dynamicDays = (1..maxDaysInMonth).toList()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day
            WheelPicker(
                items = dynamicDays,
                selectedItem = selectedDay,
                onItemSelected = { selectedDay = it },
                modifier = Modifier.weight(1f),
                itemHeight = 40.dp,
                visibleItemsCount = 3
            ) { item, isSelected ->
                Text(
                    text = String.format(Locale.getDefault(), "%02d", item),
                    color = if (isSelected) textColor else textColor.copy(alpha = 0.5f),
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Month
            WheelPicker(
                items = monthsStr,
                selectedItem = monthsStr[selectedMonth],
                onItemSelected = { selectedMonth = monthsStr.indexOf(it) },
                modifier = Modifier.weight(1f),
                itemHeight = 40.dp,
                visibleItemsCount = 3
            ) { item, isSelected ->
                Text(
                    text = item,
                    color = if (isSelected) textColor else textColor.copy(alpha = 0.5f),
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Year
            WheelPicker(
                items = years,
                selectedItem = selectedYear,
                onItemSelected = { selectedYear = it },
                modifier = Modifier.weight(1f),
                itemHeight = 40.dp,
                visibleItemsCount = 3
            ) { item, isSelected ->
                Text(
                    text = item.toString(),
                    color = if (isSelected) textColor else textColor.copy(alpha = 0.5f),
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.Center)
                .background(textColor.copy(alpha = 0.05f))
        )
    }
}
"""

content = content + "\n\n" + inline_picker
with open("app/src/main/java/com/example/ui/components/SlidingDatePickerModal.kt", "w") as f:
    f.write(content)
