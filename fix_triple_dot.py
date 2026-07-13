import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_code = """                                        Spacer(modifier = Modifier.width(12.dp))
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More",
                                            tint = AppTextColor(),
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable { showHotelDropdown = true }
                                        )
                                    }
                                }"""

new_code = """                                        Spacer(modifier = Modifier.width(12.dp))
                                        Box {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "More",
                                                tint = AppTextColor(),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable { showHotelDropdown = true }
                                            )
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
                                                        text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                        onClick = {
                                                            val txs = viewModel.activeHotelTransactions.value
                                                            val cal = java.util.Calendar.getInstance()
                                                            val currentMonth = cal.get(java.util.Calendar.MONTH)
                                                            val currentYear = cal.get(java.util.Calendar.YEAR)
                                                            
                                                            val monthTxs = txs.filter { 
                                                                val tCal = java.util.Calendar.getInstance()
                                                                tCal.timeInMillis = it.timestamp
                                                                tCal.get(java.util.Calendar.MONTH) == currentMonth && tCal.get(java.util.Calendar.YEAR) == currentYear
                                                            }
                                                            
                                                            // We create a PDF document
                                                            val pdfDocument = android.graphics.pdf.PdfDocument()
                                                            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                                                            val page = pdfDocument.startPage(pageInfo)
                                                            val canvas = page.canvas
                                                            val paint = android.graphics.Paint().apply { textSize = 16f; color = android.graphics.Color.BLACK }
                                                            val boldPaint = android.graphics.Paint().apply { textSize = 20f; color = android.graphics.Color.BLACK; isFakeBoldText = true }
                                                            
                                                            canvas.drawText("Transactions - ${hotel.name}", 20f, 40f, boldPaint)
                                                            var y = 80f
                                                            
                                                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                                                            for (t in monthTxs) {
                                                                val dateStr = sdf.format(java.util.Date(t.timestamp))
                                                                val typeStr = if (t.isNight) "Night" else "Day"
                                                                val amt = "Rs.${t.totalAmount}"
                                                                canvas.drawText("$dateStr | $typeStr | $amt", 20f, y, paint)
                                                                y += 25f
                                                                if (y > 800f) break // simple limit for 1 page
                                                            }
                                                            pdfDocument.finishPage(page)
                                                            
                                                            try {
                                                                val file = java.io.File(context.cacheDir, "transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                                val fos = java.io.FileOutputStream(file)
                                                                pdfDocument.writeTo(fos)
                                                                pdfDocument.close()
                                                                fos.close()
                                                                
                                                                // Provide path via strict content uri if file provider existed, or just open for now via generic intent. Since file provider isn't guaranteed, we will rely on UI feedback.
                                                                android.widget.Toast.makeText(context, "Saved PDF to Cache: ${file.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
                                                            } catch(e: Exception) {
                                                                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                            showHotelDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
