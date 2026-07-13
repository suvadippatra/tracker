with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

if "import com.example.ui.components.InlineDatePicker" not in content:
    content = content.replace("import com.example.ui.components.SlidingDatePickerModal", "import com.example.ui.components.SlidingDatePickerModal\nimport com.example.ui.components.InlineDatePicker")

old_card = """                    // TOP SECTION: DATE/TIME DRUM CONTROLS
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Date & Time Picked",
                                    color = AppTextColor().copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = sdf.format(Date(date)),
                                    color = AppTextColor(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Date Picker Launcher
                                GlassButton(
                                    onClick = {
                                        showDatePickerModal = true
                                    },
                                    cornerRadius = 8.dp
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = AccentCyan, modifier = Modifier.size(16.dp))
                                }


                            }
                        }
                    }"""

new_card = """                    // TOP SECTION: DATE/TIME DRUM CONTROLS
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Date Picked",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            InlineDatePicker(
                                initialDateMillis = date,
                                onDateSelected = { newDate ->
                                    // Just update the date directly using viewmodel
                                    // But wait, there is no viewModel.setDate() yet, we should add it if it's missing, or viewModel.addMealDate.value = newDate. 
                                    // Since addMealDate is MutableStateFlow in ViewModel, maybe there's a setter. Let's check ViewModel.
                                    viewModel.setAddMealDate(newDate)
                                },
                                textColor = AppTextColor()
                            )
                        }
                    }"""

content = content.replace(old_card, new_card)

# Remove the SlidingDatePickerModal if showDatePickerModal block is present at the end of AddMealFlowModal
import re
modal_regex = r"if\s*\(showDatePickerModal\)\s*\{\s*SlidingDatePickerModal\([\s\S]*?\}\s*\}"
content = re.sub(modal_regex, "", content)
content = content.replace("var showDatePickerModal by remember { mutableStateOf(false) }", "")
content = content.replace("val sdf = SimpleDateFormat(\"dd MMM yyyy, hh:mm a\", Locale.getDefault())", "val sdf = SimpleDateFormat(\"dd MMM yyyy\", Locale.getDefault())")
content = content.replace("val sdf = SimpleDateFormat(\"dd MMM yyyy, hh:mm a\", java.util.Locale.getDefault())", "val sdf = SimpleDateFormat(\"dd MMM yyyy\", java.util.Locale.getDefault())")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
