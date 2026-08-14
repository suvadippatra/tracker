package com.example.ui.screens.settings.subscreens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BackupDataType
import com.example.data.ExportResult
import com.example.data.ImportOutcome
import com.example.data.ImportResult
import com.example.data.model.BankAccountEntity
import com.example.data.model.ExtraExpenseEntity
import com.example.data.model.HotelEntity
import com.example.data.model.HotelTransactionEntity
import com.example.data.model.MenuEntity
import com.example.data.model.MeterReading
import com.example.data.model.UtilityBill
import com.example.ui.components.*
import com.example.ui.viewmodel.FinancialViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val AccentOrange = Color(0xFFFF9800)
private val AccentPurple = Color(0xFFBA68C8)
private val AccentAmber = Color(0xFFFFC107)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupSettingsScreen(viewModel: FinancialViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Observe data counts
    val allHotels by viewModel.allHotels.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeHotelId by viewModel.activeHotelId.collectAsStateWithLifecycle()
    val allBankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val allExpenses by viewModel.allExtraExpenses.collectAsStateWithLifecycle(initialValue = emptyList())
    val allReadings by viewModel.allMeterReadings.collectAsStateWithLifecycle(initialValue = emptyList())
    val allUtilityBills by viewModel.allUtilityBills.collectAsStateWithLifecycle(initialValue = emptyList())
    val allMenuItems by viewModel.allMenuItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())

    // Selected Hotel for Hotel-specific exports/imports
    var selectedHotelId by remember(activeHotelId, allHotels) {
        mutableIntStateOf(activeHotelId ?: allHotels.firstOrNull()?.id ?: 1)
    }

    // Active Action Dialog State
    var activeDataTypeForExport by remember { mutableStateOf<BackupDataType?>(null) }
    var activeDataTypeForImport by remember { mutableStateOf<BackupDataType?>(null) }
    var showCsvFormatDialogFor by remember { mutableStateOf<BackupDataType?>(null) }

    // Paste Dialog State
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteDataType by remember { mutableStateOf(BackupDataType.FULL_BACKUP) }
    var pasteInputText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }

    // QR Export & Scanner State
    var activeQrExportData by remember { mutableStateOf<ExportResult.Success?>(null) }
    var qrSessionId by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    // Pending file export payload & mime
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingExportFileName by remember { mutableStateOf("backup.json") }
    var pendingExportMimeType by remember { mutableStateOf("application/json") }

    // File Save Launcher (SAF)
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(pendingExportMimeType)
    ) { uri: Uri? ->
        if (uri != null && pendingExportContent != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(pendingExportContent!!.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "File saved successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // File Import Launcher
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val targetType = activeDataTypeForImport ?: BackupDataType.FULL_BACKUP
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!text.isNullOrBlank()) {
                        viewModel.importUniversal(text, targetType, selectedHotelId) { outcome ->
                            when (outcome) {
                                is ImportOutcome.Success -> {
                                    Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                                }
                                is ImportOutcome.Error -> {
                                    Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Helper function to copy data to clipboard
    fun copyDataToClipboard(type: BackupDataType) {
        if (type == BackupDataType.FULL_BACKUP) {
            viewModel.exportDataJson(BackupDataType.FULL_BACKUP, selectedHotelId) { json ->
                clipboardManager.setText(AnnotatedString(json))
                Toast.makeText(context, "Full Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        } else {
            viewModel.exportDataCsv(type, selectedHotelId) { csv ->
                clipboardManager.setText(AnnotatedString(csv))
                Toast.makeText(context, "${type.displayName} CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper function to trigger QR send
    fun startQrSend(type: BackupDataType) {
        viewModel.exportViaQr(type, selectedHotelId) { result ->
            when (result) {
                is ExportResult.Success -> {
                    qrSessionId = UUID.randomUUID().toString()
                    activeQrExportData = result
                }
                is ExportResult.Error -> {
                    Toast.makeText(context, "QR generation failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Global Action Card: Scanner & Universal Paste
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = getCardBackground()),
                border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder())
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Universal Transfer",
                                color = AppTextColor(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Send & Receive data via QR, Clipboard or Files",
                                color = AppSecondaryTextColor(),
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AccentCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 1: QR Send & QR Scan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // QR Send (Full Backup)
                        Button(
                            onClick = {
                                startQrSend(BackupDataType.FULL_BACKUP)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "QR Send",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Scan QR Button
                        Button(
                            onClick = {
                                showQrScanner = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "QR Scan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Copy Backup & Paste Data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                copyDataToClipboard(BackupDataType.FULL_BACKUP)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTextColor()),
                            border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder())
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Copy Data",
                                color = AppTextColor(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                pasteDataType = BackupDataType.FULL_BACKUP
                                pasteInputText = ""
                                showPasteDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTextColor()),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Paste Data",
                                color = AppTextColor(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 1: HOTEL DATA
        // ==========================================
        item {
            SectionHeader(
                title = "Hotel Data",
                icon = Icons.Default.Hotel,
                color = AccentCyan,
                badge = "${allHotels.size} Hotel(s)"
            )
        }

        // Hotel Selector if multiple hotels exist
        if (allHotels.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Target Hotel:",
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allHotels) { hotel ->
                            val isSelected = hotel.id == selectedHotelId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedHotelId = hotel.id },
                                label = { Text(hotel.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentCyan,
                                    labelColor = AppTextColor()
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) AccentCyan else getGlassBorder(),
                                    selectedBorderColor = AccentCyan,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }
            }
        }

        // 1. Whole Hotel Data
        item {
            val hotelMenus = allMenuItems.filter { it.hotelId == selectedHotelId }
            val hotelTxs = allTransactions.filter { it.hotelId == selectedHotelId }
            DataBackupItemCard(
                title = "Whole Hotel Data",
                subtitle = "Complete profile, menu list & order transactions",
                countLabel = "${hotelMenus.size} Menus • ${hotelTxs.size} Orders",
                icon = Icons.Default.Apartment,
                accentColor = AccentCyan,
                onCopyClick = { copyDataToClipboard(BackupDataType.HOTEL_WHOLE) },
                onQrSendClick = { startQrSend(BackupDataType.HOTEL_WHOLE) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.HOTEL_WHOLE },
                onImportClick = { activeDataTypeForImport = BackupDataType.HOTEL_WHOLE },
                onPasteClick = {
                    pasteDataType = BackupDataType.HOTEL_WHOLE
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.HOTEL_WHOLE }
            )
        }

        // 2. Hotel Menus Only
        item {
            val hotelMenus = allMenuItems.filter { it.hotelId == selectedHotelId }
            DataBackupItemCard(
                title = "Hotel Menus",
                subtitle = "Item names, prices, categories & day/night flags",
                countLabel = "${hotelMenus.size} Items",
                icon = Icons.Default.RestaurantMenu,
                accentColor = AccentOrange,
                onCopyClick = { copyDataToClipboard(BackupDataType.HOTEL_MENUS) },
                onQrSendClick = { startQrSend(BackupDataType.HOTEL_MENUS) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.HOTEL_MENUS },
                onImportClick = { activeDataTypeForImport = BackupDataType.HOTEL_MENUS },
                onPasteClick = {
                    pasteDataType = BackupDataType.HOTEL_MENUS
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.HOTEL_MENUS }
            )
        }

        // 3. Hotel History / Orders
        item {
            val hotelTxs = allTransactions.filter { it.hotelId == selectedHotelId }
            DataBackupItemCard(
                title = "Hotel Order History",
                subtitle = "Chronological meal orders, item logs & totals",
                countLabel = "${hotelTxs.size} Orders",
                icon = Icons.Default.ReceiptLong,
                accentColor = AccentGold,
                onCopyClick = { copyDataToClipboard(BackupDataType.HOTEL_HISTORY) },
                onQrSendClick = { startQrSend(BackupDataType.HOTEL_HISTORY) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.HOTEL_HISTORY },
                onImportClick = { activeDataTypeForImport = BackupDataType.HOTEL_HISTORY },
                onPasteClick = {
                    pasteDataType = BackupDataType.HOTEL_HISTORY
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.HOTEL_HISTORY }
            )
        }

        // ==========================================
        // SECTION 2: TRANSACTION DATA
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Transaction Data",
                icon = Icons.Default.AccountBalanceWallet,
                color = AccentGreen,
                badge = "${allBankAccounts.size} Accounts • ${allExpenses.size} Records"
            )
        }

        // 4. Account Details / Virtual Accounts
        item {
            DataBackupItemCard(
                title = "Account Details",
                subtitle = "Bank & virtual accounts, balances, UPI and IFSC",
                countLabel = "${allBankAccounts.size} Accounts",
                icon = Icons.Default.AccountBalance,
                accentColor = AccentGreen,
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
        }

        // 5. Transaction History / Extra Expenses
        item {
            DataBackupItemCard(
                title = "Transaction History",
                subtitle = "Expense & income logs, categories, amounts and notes",
                countLabel = "${allExpenses.size} Transactions",
                icon = Icons.Default.History,
                accentColor = AccentPurple,
                onCopyClick = { copyDataToClipboard(BackupDataType.TRANSACTION_HISTORY) },
                onQrSendClick = { startQrSend(BackupDataType.TRANSACTION_HISTORY) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.TRANSACTION_HISTORY },
                onImportClick = { activeDataTypeForImport = BackupDataType.TRANSACTION_HISTORY },
                onPasteClick = {
                    pasteDataType = BackupDataType.TRANSACTION_HISTORY
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.TRANSACTION_HISTORY }
            )
        }

        // ==========================================
        // SECTION 3: ELECTRICITY BILL DATA
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Electricity Bill Data",
                icon = Icons.Default.FlashOn,
                color = AccentAmber,
                badge = "${allReadings.size} Readings • ${allUtilityBills.size} Bills"
            )
        }

        // 6. Datewise Readings
        item {
            DataBackupItemCard(
                title = "Datewise Meter Readings",
                subtitle = "Meter readings, date logs, consumed units & status",
                countLabel = "${allReadings.size} Readings",
                icon = Icons.Default.FlashOn,
                accentColor = AccentAmber,
                onCopyClick = { copyDataToClipboard(BackupDataType.METER_READINGS) },
                onQrSendClick = { startQrSend(BackupDataType.METER_READINGS) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.METER_READINGS },
                onImportClick = { activeDataTypeForImport = BackupDataType.METER_READINGS },
                onPasteClick = {
                    pasteDataType = BackupDataType.METER_READINGS
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.METER_READINGS }
            )
        }

        // 7. Paid Bill List
        item {
            DataBackupItemCard(
                title = "Paid Bill List",
                subtitle = "Billing cycles, units, amounts, due dates & txn IDs",
                countLabel = "${allUtilityBills.size} Paid Bills",
                icon = Icons.Default.Receipt,
                accentColor = AccentGreen,
                onCopyClick = { copyDataToClipboard(BackupDataType.UTILITY_BILLS) },
                onQrSendClick = { startQrSend(BackupDataType.UTILITY_BILLS) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.UTILITY_BILLS },
                onImportClick = { activeDataTypeForImport = BackupDataType.UTILITY_BILLS },
                onPasteClick = {
                    pasteDataType = BackupDataType.UTILITY_BILLS
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.UTILITY_BILLS }
            )
        }

        // ==========================================
        // SECTION 4: FULL BACKUP
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Full Application Backup",
                icon = Icons.Default.Storage,
                color = AccentCyan,
                badge = "All Tables"
            )
        }

        item {
            DataBackupItemCard(
                title = "Full App Snapshot (All Data)",
                subtitle = "All hotels, menus, transactions, accounts, expenses, readings & bills",
                countLabel = "Complete System Backup",
                icon = Icons.Default.Storage,
                accentColor = AccentCyan,
                onCopyClick = { copyDataToClipboard(BackupDataType.FULL_BACKUP) },
                onQrSendClick = { startQrSend(BackupDataType.FULL_BACKUP) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.FULL_BACKUP },
                onImportClick = { activeDataTypeForImport = BackupDataType.FULL_BACKUP },
                onPasteClick = {
                    pasteDataType = BackupDataType.FULL_BACKUP
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.FULL_BACKUP }
            )
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // ==========================================
    // EXPORT METHOD DIALOG
    // ==========================================
    if (activeDataTypeForExport != null) {
        val type = activeDataTypeForExport!!
        AlertDialog(
            onDismissRequest = { activeDataTypeForExport = null },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AccentCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Export ${type.displayName}", color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Choose your preferred export method:",
                        color = AppSecondaryTextColor(),
                        fontSize = 13.sp
                    )

                    // 1. File Export (JSON)
                    ExportOptionRow(
                        icon = Icons.Default.Storage,
                        title = "Save as JSON File",
                        subtitle = "Structured backup file for devices",
                        accentColor = AccentCyan,
                        onClick = {
                            activeDataTypeForExport = null
                            viewModel.exportDataJson(type, selectedHotelId) { json ->
                                pendingExportContent = json
                                pendingExportFileName = "${type.name.lowercase()}_backup.json"
                                pendingExportMimeType = "application/json"
                                saveFileLauncher.launch(pendingExportFileName)
                            }
                        }
                    )

                    // 2. CSV File / Spreadsheet Export
                    ExportOptionRow(
                        icon = Icons.Default.ReceiptLong,
                        title = "Save as CSV (Spreadsheet)",
                        subtitle = "Open in MS Excel or Google Sheets",
                        accentColor = AccentOrange,
                        onClick = {
                            activeDataTypeForExport = null
                            viewModel.exportDataCsv(type, selectedHotelId) { csv ->
                                pendingExportContent = csv
                                pendingExportFileName = "${type.name.lowercase()}_export.csv"
                                pendingExportMimeType = "text/csv"
                                saveFileLauncher.launch(pendingExportFileName)
                            }
                        }
                    )

                    // 3. Copy to Clipboard
                    ExportOptionRow(
                        icon = Icons.Default.ContentCopy,
                        title = "Copy CSV to Clipboard",
                        subtitle = "Copy formatted table directly",
                        accentColor = AccentGold,
                        onClick = {
                            activeDataTypeForExport = null
                            viewModel.exportDataCsv(type, selectedHotelId) { csv ->
                                clipboardManager.setText(AnnotatedString(csv))
                                Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // 4. QR Method Export
                    ExportOptionRow(
                        icon = Icons.Default.QrCode,
                        title = "Export via QR Code Stream",
                        subtitle = "Stream encrypted QR code for instant camera scan",
                        accentColor = AccentPurple,
                        onClick = {
                            activeDataTypeForExport = null
                            viewModel.exportViaQr(type, selectedHotelId) { result ->
                                when (result) {
                                    is ExportResult.Success -> {
                                        qrSessionId = UUID.randomUUID().toString()
                                        activeQrExportData = result
                                    }
                                    is ExportResult.Error -> {
                                        Toast.makeText(context, "QR generation failed: ${result.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDataTypeForExport = null }) {
                    Text("Close", color = AppSecondaryTextColor())
                }
            }
        )
    }

    // ==========================================
    // IMPORT METHOD DIALOG
    // ==========================================
    if (activeDataTypeForImport != null) {
        val type = activeDataTypeForImport!!
        AlertDialog(
            onDismissRequest = { activeDataTypeForImport = null },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = AccentCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Import ${type.displayName}", color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Select how you want to import data:",
                        color = AppSecondaryTextColor(),
                        fontSize = 13.sp
                    )

                    // 1. File Import
                    ExportOptionRow(
                        icon = Icons.Default.Storage,
                        title = "Import from File",
                        subtitle = "Pick a .csv or .json file from storage",
                        accentColor = AccentCyan,
                        onClick = {
                            activeDataTypeForImport = null
                            fileImportLauncher.launch("*/*")
                        }
                    )

                    // 2. Paste Text Box
                    ExportOptionRow(
                        icon = Icons.Default.ContentPaste,
                        title = "Paste CSV / JSON Text",
                        subtitle = "Paste raw table text or JSON directly",
                        accentColor = AccentOrange,
                        onClick = {
                            val cur = activeDataTypeForImport
                            activeDataTypeForImport = null
                            pasteDataType = cur ?: BackupDataType.FULL_BACKUP
                            pasteInputText = ""
                            showPasteDialog = true
                        }
                    )

                    // 3. Scan QR Code
                    ExportOptionRow(
                        icon = Icons.Default.QrCodeScanner,
                        title = "Scan QR Code (Camera)",
                        subtitle = "Scan an animated QR stream from another device",
                        accentColor = AccentPurple,
                        onClick = {
                            activeDataTypeForImport = null
                            showQrScanner = true
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDataTypeForImport = null }) {
                    Text("Cancel", color = AppSecondaryTextColor())
                }
            }
        )
    }

    // ==========================================
    // PASTE TEXT DIALOG (Available for ALL data types)
    // ==========================================
    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isImporting) showPasteDialog = false },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(24.dp),
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = AccentCyan)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Paste ${pasteDataType.displayName}",
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Text(
                        text = "Paste CSV or JSON formatted content below",
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp
                    )
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = pasteInputText,
                        onValueChange = { pasteInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        placeholder = {
                            Text(
                                "Paste your CSV rows or JSON data here...",
                                color = AppSecondaryTextColor().copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = getGlassBorder(),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = AppTextColor()
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                pasteInputText = getSampleTemplateFor(pasteDataType)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Insert Sample", color = AccentGold, fontSize = 12.sp)
                        }

                        if (pasteInputText.isNotBlank()) {
                            TextButton(
                                onClick = { pasteInputText = "" },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Clear", color = AppSecondaryTextColor(), fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pasteInputText.isBlank()) {
                            Toast.makeText(context, "Please paste some content first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isImporting = true
                        viewModel.importUniversal(pasteInputText, pasteDataType, selectedHotelId) { outcome ->
                            isImporting = false
                            when (outcome) {
                                is ImportOutcome.Success -> {
                                    Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                                    showPasteDialog = false
                                    pasteInputText = ""
                                }
                                is ImportOutcome.Error -> {
                                    Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = !isImporting && pasteInputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Importing...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Import & Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPasteDialog = false },
                    enabled = !isImporting
                ) {
                    Text("Cancel", color = AppSecondaryTextColor())
                }
            }
        )
    }

    // ==========================================
    // CSV FORMAT INFO DIALOG
    // ==========================================
    if (showCsvFormatDialogFor != null) {
        val type = showCsvFormatDialogFor!!
        AlertDialog(
            onDismissRequest = { showCsvFormatDialogFor = null },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AccentCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("${type.displayName} Format", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "You can prepare data in Microsoft Excel or Google Sheets and export as CSV:",
                        color = AppTextColor(),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Expected Columns:", color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppTextColor().copy(0.08f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = getCsvHeaderFor(type),
                            color = AccentCyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Example Rows:", color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppTextColor().copy(0.08f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = getSampleTemplateFor(type),
                            color = AppSecondaryTextColor(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sample = getSampleTemplateFor(type)
                        clipboardManager.setText(AnnotatedString(sample))
                        Toast.makeText(context, "Sample template copied", Toast.LENGTH_SHORT).show()
                        showCsvFormatDialogFor = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Template", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvFormatDialogFor = null }) {
                    Text("Close", color = AppSecondaryTextColor())
                }
            }
        )
    }

    // ==========================================
    // QR EXPORT STREAM DIALOG
    // ==========================================
    if (activeQrExportData != null) {
        QrExportStreamDialog(
            exportData = activeQrExportData!!,
            sessionId = qrSessionId,
            onDismiss = { activeQrExportData = null }
        )
    }

    // ==========================================
    // QR SCANNER DIALOG
    // ==========================================
    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onSuccess = { payload, key, iv ->
                showQrScanner = false
                viewModel.importHotelViaQr(payload, key, iv) { result ->
                    when (result) {
                        is ImportResult.Success -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                        is ImportResult.Error -> {
                            Toast.makeText(context, "Import failed: ${result.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }
}

// ==========================================
// COMPOSABLE SUB-COMPONENTS
// ==========================================

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    color: Color,
    badge: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = AppTextColor(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = badge,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DataBackupItemCard(
    title: String,
    subtitle: String,
    countLabel: String,
    icon: ImageVector,
    accentColor: Color,
    onCopyClick: () -> Unit,
    onQrSendClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onPasteClick: () -> Unit,
    onFormatInfoClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = getCardBackground()),
        border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            color = AppTextColor(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = onFormatInfoClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Format Info",
                                tint = AppSecondaryTextColor().copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        color = AppSecondaryTextColor(),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = countLabel,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row 1: Direct Fast Actions (Copy, QR Send, QR Receive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy Button
                OutlinedButton(
                    onClick = onCopyClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTextColor()),
                    border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder()),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AccentGold, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // QR Send Button
                Button(
                    onClick = onQrSendClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.25f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("QR Send", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // QR Receive / Scan Button
                Button(
                    onClick = onQrScanClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple.copy(alpha = 0.25f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("QR Scan", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Row 2: File & Paste Actions (Export File, Import File, Paste Text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Export Button
                Button(
                    onClick = onExportClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Import Button
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Quick Paste Button
                OutlinedButton(
                    onClick = onPasteClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTextColor()),
                    border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder()),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = AppSecondaryTextColor(), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ExportOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = getCardBackground(),
        border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder()),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = AppSecondaryTextColor(),
                    fontSize = 11.sp
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = AppSecondaryTextColor().copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ==========================================
// CSV FORMAT & TEMPLATE HELPERS
// ==========================================

private fun getCsvHeaderFor(type: BackupDataType): String {
    return when (type) {
        BackupDataType.HOTEL_MENUS, BackupDataType.HOTEL_WHOLE -> "Bangla Name,English Name,Price,Day Menu,Night Menu,Add On,Category"
        BackupDataType.HOTEL_HISTORY -> "Timestamp,Date,Is Night,Total Amount,Items"
        BackupDataType.BANK_ACCOUNTS -> "Account Name,Balance,UPI ID,Account Number,IFSC Code,Branch Name"
        BackupDataType.TRANSACTION_HISTORY -> "Timestamp,Date,Description,Amount,Type,Category,Bank Account ID,Notes"
        BackupDataType.METER_READINGS -> "Reading Timestamp,Date,Total Reading,Units Consumed,Is Paid"
        BackupDataType.UTILITY_BILLS -> "Start Date,End Date,Start Date Text,End Date Text,Total Units,Calculated Amount,Due Date,Is Paid,Transaction ID"
        BackupDataType.FULL_BACKUP -> "JSON format containing userProfile, hotels, menus, transactions, bankAccounts, expenses, meterReadings, utilityBills"
    }
}

private fun getSampleTemplateFor(type: BackupDataType): String {
    return when (type) {
        BackupDataType.HOTEL_MENUS, BackupDataType.HOTEL_WHOLE ->
            """Bangla Name,English Name,Price,Day Menu,Night Menu,Add On,Category
"নর্মাল ভাত","Plain Rice",35.0,true,false,false,"Common"
"চিকেন পিস","Chicken Piece",20.0,true,true,true,"Non-Veg"
"মাছ পিস","Fish Piece",20.0,true,true,true,"Non-Veg"
"ডিম কারি","Egg Curry",15.0,true,true,true,"Non-Veg"
"রুটি","Roti",5.0,false,true,false,"Bread""""

        BackupDataType.HOTEL_HISTORY ->
            """Timestamp,Date,Is Night,Total Amount,Items
1723500000000,"2024-08-13 13:00",false,90.0,"Plain Rice:2:35.0;Chicken Piece:1:20.0"
1723536000000,"2024-08-13 20:30",true,35.0,"Roti:3:5.0;Egg Curry:1:20.0""""

        BackupDataType.BANK_ACCOUNTS ->
            """Account Name,Balance,UPI ID,Account Number,IFSC Code,Branch Name
"Main Bank Account",15000.0,"myhotel@okaxis","1234567890","SBIN0001234","Kolkata Main"
"Cash Drawer",2500.0,"","","","Daily Cash""""

        BackupDataType.TRANSACTION_HISTORY ->
            """Timestamp,Date,Description,Amount,Type,Category,Bank Account ID,Notes
1723500000000,"2024-08-13 10:30","Grocery Supplies",1200.0,"EXPENSE","Grocery",1,"Rice and spices"
1723510000000,"2024-08-13 14:00","Daily Counter Sale",3500.0,"INCOME","Sales",1,"Lunch receipts""""

        BackupDataType.METER_READINGS ->
            """Reading Timestamp,Date,Total Reading,Units Consumed,Is Paid
1723400000000,"2024-08-12",12450.0,14.5,false
1723486400000,"2024-08-13",12465.2,15.2,false"""

        BackupDataType.UTILITY_BILLS ->
            """Start Date,End Date,Start Date Text,End Date Text,Total Units,Calculated Amount,Due Date,Is Paid,Transaction ID
1720800000000,1723400000000,"2024-07-13","2024-08-12",340.0,2380.0,"2024-08-25",true,"WBSEDCL-847291""""

        BackupDataType.FULL_BACKUP ->
            """{
  "dataType": "FULL_BACKUP",
  "version": 1,
  "hotels": [{"name": "Grand Bengal Hotel", "advancedPayment": 500.0}],
  "menus": [{"banglaName": "নর্মাল ভাত", "englishName": "Plain Rice", "price": 35.0}],
  "bankAccounts": [{"name": "Main Bank", "balance": 10000.0}],
  "expenses": [{"name": "Grocery", "amount": 500.0, "type": "EXPENSE"}],
  "meterReadings": [{"readingDate": 1723500000000, "totalReading": 1200.0, "unitsConsumed": 10.0}],
  "utilityBills": [{"startDate": 1720800000000, "endDate": 1723400000000, "totalUnits": 200.0, "calculatedAmount": 1400.0, "isPaid": true}]
}"""
    }
}
