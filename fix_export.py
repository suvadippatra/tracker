import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_export = """                                    onClick = {
                                        val txs = viewModel.activeHotelTransactions.value
                                        val sb = java.lang.StringBuilder()
                                        sb.append("Date,Type,Total\\n")
                                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy hh:mm a", java.util.Locale.getDefault())
                                        for (t in txs) {
                                            sb.append(sdf.format(java.util.Date(t.timestamp))).append(",")
                                            sb.append(if (t.isNight) "Night" else "Day").append(",")
                                            sb.append(t.totalAmount).append("\\n")
                                        }
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "${hotel.name} Transactions")
                                            putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Export Transactions"))
                                        showHotelDropdown = false
                                    }"""

new_export = """                                    onClick = {
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
                                    }"""

content = content.replace(old_export, new_export)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
