with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_time_picker1 = """                                // Time Picker Launcher
                                GlassButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = date }
                                        TimePickerDialog(
                                            context,
                                            { _, hr, min ->
                                                cal.set(Calendar.HOUR_OF_DAY, hr)
                                                cal.set(Calendar.MINUTE, min)
                                                viewModel.setOrderDate(cal.timeInMillis)
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            false
                                        ).show()
                                    },
                                    cornerRadius = 8.dp
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Time", tint = AccentCyan, modifier = Modifier.size(16.dp))
                                }"""

content = content.replace(old_time_picker1, "")

old_time_picker2 = """                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                TimePickerDialog(
                                    context,
                                    { _, hr, min ->
                                        cal.set(Calendar.HOUR_OF_DAY, hr)
                                        cal.set(Calendar.MINUTE, min)
                                        timestamp = cal.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            }
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = "Time", tint = AccentCyan, modifier = Modifier.size(20.dp))
                        }"""

content = content.replace(old_time_picker2, "")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
