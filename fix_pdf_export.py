import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Add launcher at the beginning of HotelManagementTab
launcher_code = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelManagementTab(viewModel: FinancialViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val pdfExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val hotel = viewModel.activeHotel.value
            if (hotel != null) {
                try {
                    val txs = viewModel.activeHotelTransactions.value
                    val cal = java.util.Calendar.getInstance()
                    val currentMonth = cal.get(java.util.Calendar.MONTH)
                    val currentYear = cal.get(java.util.Calendar.YEAR)
                    
                    val monthTxs = txs.filter { 
                        val tCal = java.util.Calendar.getInstance()
                        tCal.timeInMillis = it.timestamp
                        tCal.get(java.util.Calendar.MONTH) == currentMonth && tCal.get(java.util.Calendar.YEAR) == currentYear
                    }
                    
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
                        if (y > 800f) break
                    }
                    pdfDocument.finishPage(page)
                    
                    context.contentResolver.openOutputStream(uri)?.use { fos ->
                        pdfDocument.writeTo(fos)
                    }
                    pdfDocument.close()
                    android.widget.Toast.makeText(context, "PDF Exported Successfully", android.widget.Toast.LENGTH_SHORT).show()
                } catch(e: Exception) {
                    android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
"""
content = content.replace("@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun HotelManagementTab(viewModel: FinancialViewModel) {", launcher_code)

# Replace the onClick logic in DropdownMenu to just use the launcher
old_dropdown_click = """                                                    DropdownMenuItem(
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
                                                    )"""

new_dropdown_click = """                                                    DropdownMenuItem(
                                                        text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                        onClick = {
                                                            pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                            showHotelDropdown = false
                                                        }
                                                    )"""

content = content.replace(old_dropdown_click, new_dropdown_click)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
