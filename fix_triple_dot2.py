import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Delete the old dropdown menu code which comes after Spacer(modifier = Modifier.height(24.dp))
old_stray_dropdown = """                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "monthly expenditure",
                                        color = AppTextColor().copy(alpha = 0.9f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text("₹ ", color = AppTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                                        RollingNumberText(
                                            value = if (hotel.id == activeHotelId) monthlyTotal else 0.0,
                                            style = TextStyle(
                                                color = AppTextColor(),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (hotel.id == activeHotelId) {
                            DropdownMenu(
                                expanded = showHotelDropdown,
                                onDismissRequest = { showHotelDropdown = false },
                                modifier = Modifier
                                    .background(getDialogBackground())
                                    .border(0.5.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit this hotel", color = AccentCyan) },
                                    
                                    onClick = {
                                        showEditHotelDialog = true
                                        showHotelDropdown = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export transactions", color = AppTextColor()) },
                                    
                                    onClick = {
                                        val txs = viewModel.activeHotelTransactions.value
                                        val sb = java.lang.StringBuilder()
                                        sb.append("Date,Type,Total\\n")
                                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy hh:mm a", java.util.Locale.getDefault())
                                        
                                        // Export only the current month's transactions (Month-wise export)
                                        val cal = java.util.Calendar.getInstance()
                                        val currentMonth = cal.get(java.util.Calendar.MONTH)
                                        val currentYear = cal.get(java.util.Calendar.YEAR)
                                        
                                        val monthTxs = txs.filter { 
                                            val tCal = java.util.Calendar.getInstance()
                                            tCal.timeInMillis = it.timestamp
                                            tCal.get(java.util.Calendar.MONTH) == currentMonth && tCal.get(java.util.Calendar.YEAR) == currentYear
                                        }
                                        
                                        for (t in monthTxs) {
                                            sb.append(sdf.format(java.util.Date(t.timestamp))).append(",")
                                            sb.append(if (t.isNight) "Night" else "Day").append(",")
                                            sb.append(t.totalAmount).append("\\n")
                                        }
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "${hotel.name} Monthly Transactions")
                                            putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Export Transactions"))
                                        showHotelDropdown = false
                                    }
                                )
                            }
                        }"""

new_stray_dropdown = """                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "monthly expenditure",
                                        color = AppTextColor().copy(alpha = 0.9f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text("₹ ", color = AppTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                                        RollingNumberText(
                                            value = if (hotel.id == activeHotelId) monthlyTotal else 0.0,
                                            style = TextStyle(
                                                color = AppTextColor(),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        )
                                    }
                                }
                            }
                        }"""

content = content.replace(old_stray_dropdown, new_stray_dropdown)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
