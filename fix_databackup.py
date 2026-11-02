import re

with open("app/src/main/java/com/example/ui/screens/settings/subscreens/DataBackupSettingsScreen.kt", "r") as f:
    content = f.read()

target = """        // Account Selector if multiple accounts exist
        if (allBankAccounts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Target Account:",
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allBankAccounts) { acc ->
                            val isSelected = acc.id == selectedAccountId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text(acc.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentGreen,
                                    labelColor = AppTextColor()
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) AccentGreen else getGlassBorder(),
                                    selectedBorderColor = AccentGreen,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4b. Single Selected Account Data Backup
        item {
            val selAcc = allBankAccounts.find { it.id == selectedAccountId }
            val accName = selAcc?.name ?: "Selected Account"
            val txCountForAcc = allExpenses.count { it.bankAccountId == selectedAccountId }
            DataBackupItemCard(
                title = "Bank Accounts",
                subtitle = "Export or restore all bank account profiles",
                countLabel = "$txCountForAcc Transactions",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = AccentAmber,
                onCopyClick = { copyDataToClipboard(BackupDataType.SINGLE_BANK_ACCOUNT) },
                onQrSendClick = { startQrSend(BackupDataType.SINGLE_BANK_ACCOUNT) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.SINGLE_BANK_ACCOUNT },
                onImportClick = { activeDataTypeForImport = BackupDataType.SINGLE_BANK_ACCOUNT },
                onPasteClick = {
                    pasteDataType = BackupDataType.SINGLE_BANK_ACCOUNT
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.SINGLE_BANK_ACCOUNT }
            )
        }"""

replacement = """        // 4. All Bank Accounts Backup
        item {
            DataBackupItemCard(
                title = "Bank Accounts",
                subtitle = "Export or restore all bank account profiles",
                countLabel = "${allBankAccounts.size} Accounts",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = AccentAmber,
                onCopyClick = { copyDataToClipboard(BackupDataType.BANK_ACCOUNTS) },
                onQrSendClick = { startQrSend(BackupDataType.BANK_ACCOUNTS) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.BANK_ACCOUNTS },
                onImportClick = { activeDataTypeForImport = BackupDataType.BANK_ACCOUNTS },
                onPasteClick = {
                    pasteDataType = BackupDataType.BANK_ACCOUNTS
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.BANK_ACCOUNTS }
            )
        }"""

if target in content:
    with open("app/src/main/java/com/example/ui/screens/settings/subscreens/DataBackupSettingsScreen.kt", "w") as f:
        f.write(content.replace(target, replacement))
    print("Success")
else:
    print("Failed")
