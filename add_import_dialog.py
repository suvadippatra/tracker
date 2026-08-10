import re
with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Add states for dialogs
dialog_states = """
    var showMenuImportDialog by remember { mutableStateOf(false) }
    var showHotelHistoryImportDialog by remember { mutableStateOf(false) }
    var showAccountHistoryImportDialog by remember { mutableStateOf(false) }
    var showMeterReadingImportDialog by remember { mutableStateOf(false) }
    
    var pastedCsvText by remember { mutableStateOf("") }
    var selectedHotelIdForImport by remember { mutableStateOf<Int?>(null) }
"""
content = re.sub(r'var settingsSearchQuery by remember \{ mutableStateOf\(""\) \}', r'var settingsSearchQuery by remember { mutableStateOf("") }\n' + dialog_states, content)

# Replace the Import button clicks
content = content.replace(
    'onClick = { menuImportLauncher.launch("text/*") }',
    'onClick = { pastedCsvText = ""; selectedHotelIdForImport = null; showMenuImportDialog = true }'
)
content = content.replace(
    'onClick = { hotelHistoryImportLauncher.launch("text/*") }',
    'onClick = { pastedCsvText = ""; selectedHotelIdForImport = null; showHotelHistoryImportDialog = true }'
)
content = content.replace(
    'onClick = { accountHistoryImportLauncher.launch("text/*") }',
    'onClick = { pastedCsvText = ""; showAccountHistoryImportDialog = true }'
)
content = content.replace(
    'onClick = { meterReadingImportLauncher.launch("text/*") }',
    'onClick = { pastedCsvText = ""; showMeterReadingImportDialog = true }'
)

# Insert the dialogs
dialog_ui = """
    if (showMenuImportDialog || showHotelHistoryImportDialog || showAccountHistoryImportDialog || showMeterReadingImportDialog) {
        val title = when {
            showMenuImportDialog -> "Import Menus"
            showHotelHistoryImportDialog -> "Import Hotel History"
            showAccountHistoryImportDialog -> "Import Account History"
            else -> "Import Meter Readings"
        }
        val showHotelSelector = showMenuImportDialog || showHotelHistoryImportDialog
        
        AlertDialog(
            onDismissRequest = {
                showMenuImportDialog = false
                showHotelHistoryImportDialog = false
                showAccountHistoryImportDialog = false
                showMeterReadingImportDialog = false
            },
            containerColor = getDialogBackground(),
            title = { Text(title, color = AppTextColor(), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (showHotelSelector) {
                        var hotelDropdownExpanded by remember { mutableStateOf(false) }
                        Text("Select Hotel (Optional)", color = AppTextColor(), fontSize = 14.sp)
                        Box {
                            OutlinedButton(
                                onClick = { hotelDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val selectedHotel = hotels.find { it.id == selectedHotelIdForImport }
                                Text(selectedHotel?.name ?: "Auto-detect from CSV or Select", color = AppTextColor())
                            }
                            DropdownMenu(
                                expanded = hotelDropdownExpanded,
                                onDismissRequest = { hotelDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Auto-detect from CSV", color = AppTextColor()) },
                                    onClick = { selectedHotelIdForImport = null; hotelDropdownExpanded = false }
                                )
                                hotels.forEach { hotel ->
                                    DropdownMenuItem(
                                        text = { Text(hotel.name, color = AppTextColor()) },
                                        onClick = { selectedHotelIdForImport = hotel.id; hotelDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                    
                    Text("Paste CSV Text:", color = AppTextColor(), fontSize = 14.sp)
                    OutlinedTextField(
                        value = pastedCsvText,
                        onValueChange = { pastedCsvText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor()
                        )
                    )
                    
                    Text("- OR -", color = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.align(Alignment.CenterHorizontally))
                    
                    Button(
                        onClick = {
                            when {
                                showMenuImportDialog -> menuImportLauncher.launch("text/*")
                                showHotelHistoryImportDialog -> hotelHistoryImportLauncher.launch("text/*")
                                showAccountHistoryImportDialog -> accountHistoryImportLauncher.launch("text/*")
                                showMeterReadingImportDialog -> meterReadingImportLauncher.launch("text/*")
                            }
                            showMenuImportDialog = false
                            showHotelHistoryImportDialog = false
                            showAccountHistoryImportDialog = false
                            showMeterReadingImportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select File", color = AccentCyan)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pastedCsvText.isNotBlank()) {
                            try {
                                when {
                                    showMenuImportDialog -> viewModel.importMenusFromCsv(pastedCsvText, selectedHotelIdForImport)
                                    showHotelHistoryImportDialog -> viewModel.importHotelTransactionsFromCsv(pastedCsvText, selectedHotelIdForImport)
                                    showAccountHistoryImportDialog -> viewModel.importExtraExpensesFromCsv(pastedCsvText)
                                    showMeterReadingImportDialog -> viewModel.importMeterReadingsFromCsv(pastedCsvText)
                                }
                                android.widget.Toast.makeText(context, "Imported successfully from text", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                        showMenuImportDialog = false
                        showHotelHistoryImportDialog = false
                        showAccountHistoryImportDialog = false
                        showMeterReadingImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text("Import from Text", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMenuImportDialog = false
                    showHotelHistoryImportDialog = false
                    showAccountHistoryImportDialog = false
                    showMeterReadingImportDialog = false
                }) {
                    Text("Cancel", color = AccentPink)
                }
            }
        )
    }
"""

content = content.replace("    val shareCsv = { filename: String, content: String ->", dialog_ui + "\n    val shareCsv = { filename: String, content: String ->")

# For file import, it still launches file picker and processes in launcher block, 
# but the launcher doesn't know about `selectedHotelIdForImport`. We need to update the launcher callbacks too.
content = content.replace('viewModel.importMenusFromCsv(content)', 'viewModel.importMenusFromCsv(content, selectedHotelIdForImport)')
content = content.replace('viewModel.importHotelTransactionsFromCsv(content)', 'viewModel.importHotelTransactionsFromCsv(content, selectedHotelIdForImport)')

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
print("Updated SettingsScreen.kt")
