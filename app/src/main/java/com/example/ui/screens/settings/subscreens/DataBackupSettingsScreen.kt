package com.example.ui.screens.settings.subscreens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

private val AccentOrange = Color(0xFF64B5F6) // High contrast blue
private val AccentPurple = Color(0xFFBA68C8)
private val AccentAmber = com.example.ui.components.AccentCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupSettingsScreen(
    viewModel: FinancialViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Observe data counts
    val allHotels by viewModel.allHotels.collectAsStateWithLifecycle()
    val activeHotelId by viewModel.activeHotelId.collectAsStateWithLifecycle()
    val allBankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val activeBankAccountId by viewModel.activeBankAccountId.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExtraExpenses.collectAsStateWithLifecycle()
    val allReadings by viewModel.allMeterReadings.collectAsStateWithLifecycle()
    val allUtilityBills by viewModel.allUtilityBills.collectAsStateWithLifecycle()
    val allMenuItems by viewModel.allHotelMenus.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    // Selected Hotel for Hotel-specific exports/imports
    var selectedHotelId by remember(activeHotelId, allHotels) {
        mutableIntStateOf(activeHotelId ?: allHotels.firstOrNull()?.id ?: 1)
    }

    // Selected Account for Account-specific filtering (null = All Accounts)
    var selectedAccountScopeId by remember(activeBankAccountId, allBankAccounts) {
        mutableStateOf<Int?>(null)
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
    var activeQrItemName by remember { mutableStateOf("Full Application Backup") }
    var qrSessionId by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }
    var showKhataScanner by remember { mutableStateOf(false) }

    // Smart Import Flow State
    val importUiFlowState by viewModel.importUiFlowState.collectAsStateWithLifecycle()
    val parentDecisions by viewModel.parentDecisions.collectAsStateWithLifecycle()
    val acceptedMenuItemKeys by viewModel.acceptedMenuItemKeys.collectAsStateWithLifecycle()
    val conflictDecisions by viewModel.conflictDecisions.collectAsStateWithLifecycle()
    val allOwnerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle(initialValue = emptyList())

    // Pending file export payload & mime
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingZipCsvContent by remember { mutableStateOf<String?>(null) }
    var pendingImportDataType by remember { mutableStateOf<BackupDataType?>(null) }

    // File Save Launchers (SAF)
    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingExportContent != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(pendingExportContent!!.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "JSON file saved successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null && pendingExportContent != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(pendingExportContent!!.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "CSV file saved successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val saveZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null && (pendingExportContent != null || pendingZipCsvContent != null)) {
            val jsonPayload = pendingExportContent
            val csvPayload = pendingZipCsvContent
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    var mediaCount = 0
                    context.contentResolver.openOutputStream(uri)?.use { rawOs ->
                        java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(rawOs)).use { zos ->
                            if (!jsonPayload.isNullOrBlank()) {
                                val entry = java.util.zip.ZipEntry("backup_data.json")
                                zos.putNextEntry(entry)
                                zos.write(jsonPayload.toByteArray(Charsets.UTF_8))
                                zos.closeEntry()
                            }
                            if (!csvPayload.isNullOrBlank()) {
                                val entry = java.util.zip.ZipEntry("backup_data.csv")
                                zos.putNextEntry(entry)
                                zos.write(csvPayload.toByteArray(Charsets.UTF_8))
                                zos.closeEntry()
                            }

                            // Bundle referenced or stored local media files in filesDir/attachments
                            val combinedText = (jsonPayload ?: "") + "\n" + (csvPayload ?: "")
                            val attachmentsDir = java.io.File(context.filesDir, "attachments")
                            val addedNames = mutableSetOf<String>()

                            val mediaPrefixes = listOf("QR_", "IMG_", "PAY_", "STICKY_", "AVATAR_", "VOICE_", "UPI_SS_")
                            val mediaExts = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".m4a", ".mp3", ".aac", ".ogg", ".wav", ".pdf")

                            if (attachmentsDir.exists() && attachmentsDir.isDirectory) {
                                attachmentsDir.listFiles()?.forEach { file ->
                                    if (file.isFile && file.canRead()) {
                                        val entryName = "attachments/${file.name}"
                                        if (addedNames.add(entryName)) {
                                            zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                                            file.inputStream().buffered().use { fis -> fis.copyTo(zos) }
                                            zos.closeEntry()
                                            mediaCount++
                                        }
                                    }
                                }
                            }

                            // Also bundle reconstructed/stored payment QR codes in filesDir/qr_codes
                            val qrCodesDir = java.io.File(context.filesDir, "qr_codes")
                            if (qrCodesDir.exists() && qrCodesDir.isDirectory) {
                                qrCodesDir.listFiles()?.forEach { file ->
                                    if (file.isFile && file.canRead()) {
                                        val entryName = "qr_codes/${file.name}"
                                        if (addedNames.add(entryName)) {
                                            zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                                            file.inputStream().buffered().use { fis -> fis.copyTo(zos) }
                                            zos.closeEntry()
                                            mediaCount++
                                        }
                                    }
                                }
                            }

                            // Bundle user-attached QR photos, receipts, avatars, and sticky media stored directly in filesDir root
                            context.filesDir.listFiles()?.forEach { file ->
                                if (file.isFile && file.canRead()) {
                                    val name = file.name
                                    val isUserMedia = mediaPrefixes.any { name.startsWith(it, ignoreCase = true) } ||
                                        mediaExts.any { name.endsWith(it, ignoreCase = true) } ||
                                        (combinedText.isNotBlank() && combinedText.contains(name) && !name.endsWith(".db"))
                                    if (isUserMedia) {
                                        val entryName = "root_files/$name"
                                        if (addedNames.add(entryName)) {
                                            zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                                            file.inputStream().buffered().use { fis -> fis.copyTo(zos) }
                                            zos.closeEntry()
                                            mediaCount++
                                        }
                                    }
                                }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "ZIP archive saved ($mediaCount media attachment(s) included)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "ZIP save failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // File Import Launcher (OpenDocument - supports .zip, .json, .csv)
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val targetType = pendingImportDataType ?: BackupDataType.FULL_BACKUP
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes == null || bytes.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Check for ZIP magic header PK\x03\x04
                    val isZip = bytes.size >= 4 &&
                        bytes[0] == 0x50.toByte() &&
                        bytes[1] == 0x4B.toByte() &&
                        bytes[2] == 0x03.toByte() &&
                        bytes[3] == 0x04.toByte()

                    val extractedText: String?
                    var restoredMediaCount = 0

                    if (isZip) {
                        val attachmentsDir = java.io.File(context.filesDir, "attachments").apply {
                            if (!exists()) mkdirs()
                        }
                        val qrCodesDir = java.io.File(context.filesDir, "qr_codes").apply {
                            if (!exists()) mkdirs()
                        }
                        var jsonEntryText: String? = null
                        var csvEntryText: String? = null

                        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                val name = entry.name
                                if (!entry.isDirectory) {
                                    when {
                                        name.endsWith("backup_data.json", ignoreCase = true) || name.endsWith(".json", ignoreCase = true) -> {
                                            jsonEntryText = zis.readBytes().toString(Charsets.UTF_8)
                                        }
                                        name.endsWith("backup_data.csv", ignoreCase = true) || name.endsWith(".csv", ignoreCase = true) -> {
                                            csvEntryText = zis.readBytes().toString(Charsets.UTF_8)
                                        }
                                        name.startsWith("attachments/") -> {
                                            val cleanFileName = java.io.File(name).name
                                            if (cleanFileName.isNotBlank()) {
                                                val outFile = java.io.File(attachmentsDir, cleanFileName)
                                                java.io.FileOutputStream(outFile).use { fos ->
                                                    zis.copyTo(fos)
                                                }
                                                restoredMediaCount++
                                            }
                                        }
                                        name.startsWith("qr_codes/") -> {
                                            val cleanFileName = java.io.File(name).name
                                            if (cleanFileName.isNotBlank()) {
                                                val outFile = java.io.File(qrCodesDir, cleanFileName)
                                                java.io.FileOutputStream(outFile).use { fos ->
                                                    zis.copyTo(fos)
                                                }
                                                restoredMediaCount++
                                            }
                                        }
                                        name.startsWith("root_files/") -> {
                                            val cleanFileName = java.io.File(name).name
                                            if (cleanFileName.isNotBlank()) {
                                                val outFile = java.io.File(context.filesDir, cleanFileName)
                                                java.io.FileOutputStream(outFile).use { fos ->
                                                    zis.copyTo(fos)
                                                }
                                                restoredMediaCount++
                                            }
                                        }
                                    }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }

                        // Rewrite old device /data/.../files/attachments/<file>, /qr_codes/<file>, and root /files/<file> paths to this device
                        val rawPayload = jsonEntryText ?: csvEntryText
                        extractedText = rawPayload
                            ?.replace(
                                Regex("""/data/[^\s"\[\],;]+/files/attachments/([A-Za-z0-9_.\-]+)"""),
                                "${attachmentsDir.absolutePath}/$1"
                            )
                            ?.replace(
                                Regex("""/data/[^\s"\[\],;]+/files/qr_codes/([A-Za-z0-9_.\-]+)"""),
                                "${qrCodesDir.absolutePath}/$1"
                            )
                            ?.replace(
                                Regex("""/data/[^\s"\[\],;]+/files/(?!attachments/|qr_codes/)([A-Za-z0-9_.\-]+)"""),
                                "${context.filesDir.absolutePath}/$1"
                            )
                    } else {
                        extractedText = bytes.toString(Charsets.UTF_8)
                    }

                    if (!extractedText.isNullOrBlank()) {
                        val trimmed = extractedText.trimStart()
                        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                            withContext(Dispatchers.Main) {
                                viewModel.startSmartImportFlow(
                                    payloadText = extractedText,
                                    targetHotelId = selectedHotelId,
                                    targetBankAccountId = selectedAccountScopeId
                                ) { outcome ->
                                    when (outcome) {
                                        is ImportOutcome.Success -> {
                                            val suffix = if (restoredMediaCount > 0) " (+$restoredMediaCount media files restored)" else ""
                                            Toast.makeText(context, outcome.message + suffix, Toast.LENGTH_LONG).show()
                                        }
                                        is ImportOutcome.Error -> {
                                            Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        } else {
                            viewModel.importUniversal(extractedText, targetType, selectedHotelId) { outcome ->
                                when (outcome) {
                                    is ImportOutcome.Success -> {
                                        val suffix = if (restoredMediaCount > 0) " (+$restoredMediaCount media files restored)" else ""
                                        Toast.makeText(context, outcome.message + suffix, Toast.LENGTH_LONG).show()
                                    }
                                    is ImportOutcome.Error -> {
                                        Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "No valid backup data found in file", Toast.LENGTH_SHORT).show()
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
        if (type == BackupDataType.FULL_BACKUP || type == BackupDataType.SETTINGS || type == BackupDataType.HOTEL_INFO || type == BackupDataType.SINGLE_BANK_ACCOUNT || type == BackupDataType.HOTEL_WHOLE || type == BackupDataType.WALLET_WHOLE || type == BackupDataType.ELECTRICITY_WHOLE) {
            viewModel.exportDataJson(type, selectedHotelId, selectedAccountScopeId) { json ->
                clipboardManager.setText(AnnotatedString(json))
                Toast.makeText(context, "${type.displayName} JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        } else {
            viewModel.exportDataCsv(type, selectedHotelId, selectedAccountScopeId) { csv ->
                clipboardManager.setText(AnnotatedString(csv))
                Toast.makeText(context, "${type.displayName} CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper function to trigger QR send
    fun startQrSend(type: BackupDataType) {
        viewModel.exportViaQr(type, selectedHotelId, selectedAccountScopeId) { result ->
            when (result) {
                is ExportResult.Success -> {
                    qrSessionId = UUID.randomUUID().toString()
                    activeQrItemName = type.displayName
                    activeQrExportData = result
                }
                is ExportResult.Error -> {
                    Toast.makeText(context, "QR generation failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val allStickyNotes by viewModel.allStickyNotes.collectAsStateWithLifecycle()

    // Exact Transferable Data Size & Sync Number for Universal Transfer verification across 2 devices
    var exactTransferBytes by remember { mutableIntStateOf(0) }
    val totalRecordCount = remember(
        allHotels.size,
        allMenuItems.size,
        allTransactions.size,
        allBankAccounts.size,
        allExpenses.size,
        allReadings.size,
        allUtilityBills.size,
        allStickyNotes.size
    ) {
        allHotels.size + allMenuItems.size + allTransactions.size +
            allBankAccounts.size + allExpenses.size + allReadings.size +
            allUtilityBills.size + allStickyNotes.size
    }

    LaunchedEffect(totalRecordCount) {
        viewModel.exportDataJson(BackupDataType.FULL_BACKUP, selectedHotelId, null) { json ->
            exactTransferBytes = json.toByteArray(Charsets.UTF_8).size
        }
    }

    val formattedTransferSize = remember(exactTransferBytes, totalRecordCount) {
        val kb = exactTransferBytes / 1024.0
        val sizeLabel = when {
            exactTransferBytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.2f MB (%,d B)", exactTransferBytes / (1024.0 * 1024.0), exactTransferBytes)
            exactTransferBytes >= 1024 -> String.format(java.util.Locale.US, "%.2f KB (%,d B)", kb, exactTransferBytes)
            else -> "$exactTransferBytes B"
        }
        "$sizeLabel • $totalRecordCount records"
    }

    val backupListState = androidx.compose.runtime.saveable.rememberSaveable(
        saver = androidx.compose.foundation.lazy.LazyListState.Saver
    ) { androidx.compose.foundation.lazy.LazyListState() }

    LazyColumn(
        state = backupListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 120.dp + com.example.ui.components.rememberSplitScreenSafeBottomPadding()
        ),
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
                        Column(modifier = Modifier.weight(1f)) {
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
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AccentCyan.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, AccentCyan.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Sync Size: $formattedTransferSize",
                                        color = AccentCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCyan,
                                contentColor = AppOnAccentColor(AccentCyan)
                            )
                        ) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                tint = AppOnAccentColor(AccentCyan),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "QR Send",
                                color = AppOnAccentColor(AccentCyan),
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3: Export Full Backup File, Import File & Scan Khata to Import
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activeDataTypeForExport = BackupDataType.FULL_BACKUP },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTextColor()),
                            border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder())
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export All", color = AppTextColor(), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { activeDataTypeForImport = BackupDataType.FULL_BACKUP },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTextColor()),
                            border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder())
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Import All", color = AppTextColor(), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scan Khata to Import Featured Button
                    Button(
                        onClick = { showKhataScanner = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = AppOnAccentColor(AccentGold)
                        )
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = AppOnAccentColor(AccentGold),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Scan Khata to Import (Camera / Photo)",
                            color = AppOnAccentColor(AccentGold),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
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
                        items(allHotels, key = { it.id }) { hotel ->
                            val isSelected = hotel.id == selectedHotelId
                            val primaryCol = MaterialTheme.colorScheme.primary
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedHotelId = hotel.id },
                                label = { Text(hotel.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryCol.copy(alpha = 0.2f),
                                    selectedLabelColor = primaryCol,
                                    labelColor = AppTextColor()
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) primaryCol else getGlassBorder(),
                                    selectedBorderColor = primaryCol,
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

        // 3b. Hotel Profile Info Only
        item {
            val currentHotel = allHotels.find { it.id == selectedHotelId }
            DataBackupItemCard(
                title = "Hotel Profile Info (${currentHotel?.name ?: "Selected"})",
                subtitle = "Hotel name, contact numbers, location, advance & UPI details",
                countLabel = "1 Hotel Info",
                icon = Icons.Default.Hotel,
                accentColor = AccentPurple,
                onCopyClick = { copyDataToClipboard(BackupDataType.HOTEL_INFO) },
                onQrSendClick = { startQrSend(BackupDataType.HOTEL_INFO) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.HOTEL_INFO },
                onImportClick = { activeDataTypeForImport = BackupDataType.HOTEL_INFO },
                onPasteClick = {
                    pasteDataType = BackupDataType.HOTEL_INFO
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.HOTEL_INFO }
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

        // Account Scope Selector (All vs Specific Account)
        if (allBankAccounts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account Scope:",
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            val isAllSelected = selectedAccountScopeId == null
                            val secondaryCol = MaterialTheme.colorScheme.secondary
                            FilterChip(
                                selected = isAllSelected,
                                onClick = { selectedAccountScopeId = null },
                                label = { Text("All Accounts (${allBankAccounts.size})", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = secondaryCol.copy(alpha = 0.2f),
                                    selectedLabelColor = secondaryCol,
                                    labelColor = AppTextColor()
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isAllSelected) secondaryCol else getGlassBorder(),
                                    selectedBorderColor = secondaryCol,
                                    enabled = true,
                                    selected = isAllSelected
                                )
                            )
                        }
                        items(allBankAccounts, key = { it.id }) { account ->
                            val isSelected = account.id == selectedAccountScopeId
                            val secondaryCol = MaterialTheme.colorScheme.secondary
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedAccountScopeId = account.id },
                                label = { Text(account.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = secondaryCol.copy(alpha = 0.2f),
                                    selectedLabelColor = secondaryCol,
                                    labelColor = AppTextColor()
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) secondaryCol else getGlassBorder(),
                                    selectedBorderColor = secondaryCol,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Whole Wallet / Account Backup (Combined)
        item {
            val isSingleAcc = selectedAccountScopeId != null
            val currentAccount = allBankAccounts.find { it.id == selectedAccountScopeId }
            val accountExpenseCount = if (selectedAccountScopeId != null) {
                allExpenses.filter { it.bankAccountId == selectedAccountScopeId }.size
            } else {
                allExpenses.size
            }

            DataBackupItemCard(
                title = if (isSingleAcc) "Whole Account Data (${currentAccount?.name})" else "Whole Wallet Data (All Accounts)",
                subtitle = if (isSingleAcc) "Complete profile & transaction history for '${currentAccount?.name}'" else "Complete backup of all bank accounts & all transaction records",
                countLabel = if (isSingleAcc) "1 Account • $accountExpenseCount Txns" else "${allBankAccounts.size} Accounts • ${allExpenses.size} Txns",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = AccentGreen,
                onCopyClick = { copyDataToClipboard(BackupDataType.WALLET_WHOLE) },
                onQrSendClick = { startQrSend(BackupDataType.WALLET_WHOLE) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.WALLET_WHOLE },
                onImportClick = { activeDataTypeForImport = BackupDataType.WALLET_WHOLE },
                onPasteClick = {
                    pasteDataType = BackupDataType.WALLET_WHOLE
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.WALLET_WHOLE }
            )
        }

        // 5. Bank Account Profile(s) Only (Individual)
        item {
            val isSingleAcc = selectedAccountScopeId != null
            val currentAccount = allBankAccounts.find { it.id == selectedAccountScopeId }
            val targetDataType = BackupDataType.BANK_ACCOUNTS

            DataBackupItemCard(
                title = if (isSingleAcc) "Bank Account Profile (${currentAccount?.name})" else "Bank Account Profiles (All)",
                subtitle = if (isSingleAcc) "Account balance, UPI ID, IFSC & details without transaction logs" else "Account balances, UPI IDs, IFSC & details for all accounts",
                countLabel = if (isSingleAcc) "1 Account Profile" else "${allBankAccounts.size} Accounts",
                icon = Icons.Default.AccountBalance,
                accentColor = AccentAmber,
                onCopyClick = { copyDataToClipboard(targetDataType) },
                onQrSendClick = { startQrSend(targetDataType) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = targetDataType },
                onImportClick = { activeDataTypeForImport = targetDataType },
                onPasteClick = {
                    pasteDataType = targetDataType
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = targetDataType }
            )
        }

        // 6. Transaction History / Extra Expenses Only (Individual)
        item {
            val isSingleAcc = selectedAccountScopeId != null
            val currentAccount = allBankAccounts.find { it.id == selectedAccountScopeId }
            val filteredCount = if (isSingleAcc) {
                allExpenses.filter { it.bankAccountId == selectedAccountScopeId }.size
            } else {
                allExpenses.size
            }

            DataBackupItemCard(
                title = if (isSingleAcc) "Transaction History (${currentAccount?.name})" else "Transaction History (All Accounts)",
                subtitle = if (isSingleAcc) "Expense & income logs for '${currentAccount?.name}' only" else "Expense & income logs across all accounts",
                countLabel = "$filteredCount Transactions",
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

        // 7. Whole Electricity Data (Combined)
        item {
            DataBackupItemCard(
                title = "Whole Electricity Data",
                subtitle = "Complete meter readings, tariff rates & paid utility bills",
                countLabel = "${allReadings.size} Readings • ${allUtilityBills.size} Bills",
                icon = Icons.Default.ElectricBolt,
                accentColor = AccentAmber,
                onCopyClick = { copyDataToClipboard(BackupDataType.ELECTRICITY_WHOLE) },
                onQrSendClick = { startQrSend(BackupDataType.ELECTRICITY_WHOLE) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.ELECTRICITY_WHOLE },
                onImportClick = { activeDataTypeForImport = BackupDataType.ELECTRICITY_WHOLE },
                onPasteClick = {
                    pasteDataType = BackupDataType.ELECTRICITY_WHOLE
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.ELECTRICITY_WHOLE }
            )
        }

        // 8. Datewise Readings Only (Individual)
        item {
            DataBackupItemCard(
                title = "Datewise Meter Readings",
                subtitle = "Meter readings, date logs, consumed units & payment status",
                countLabel = "${allReadings.size} Readings",
                icon = Icons.Default.FlashOn,
                accentColor = AccentGold,
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

        // 9. Paid Bill List Only (Individual)
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

        // 9b. Electricity Rate Policies Only (Individual)
        item {
            DataBackupItemCard(
                title = "Electricity Rate Policies",
                subtitle = "Date-effective unit tariff rates & policy schedule",
                countLabel = "Tariff Policies",
                icon = Icons.Default.FlashOn,
                accentColor = AccentPurple,
                onCopyClick = { copyDataToClipboard(BackupDataType.ELECTRICITY_RATES) },
                onQrSendClick = { startQrSend(BackupDataType.ELECTRICITY_RATES) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.ELECTRICITY_RATES },
                onImportClick = { activeDataTypeForImport = BackupDataType.ELECTRICITY_RATES },
                onPasteClick = {
                    pasteDataType = BackupDataType.ELECTRICITY_RATES
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.ELECTRICITY_RATES }
            )
        }

        // ==========================================
        // SECTION 4: SETTINGS & PREFERENCES ONLY
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "App Settings & Preferences",
                icon = Icons.Default.Settings,
                color = AccentCyan,
                badge = "Settings Only"
            )
        }

        item {
            DataBackupItemCard(
                title = "Settings & Appearance Preferences",
                subtitle = "Export or restore theme, font scale, bold mode, sticky note layout & default selections separately",
                countLabel = "Preferences Snapshot",
                icon = Icons.Default.Settings,
                accentColor = AccentCyan,
                onCopyClick = { copyDataToClipboard(BackupDataType.SETTINGS) },
                onQrSendClick = { startQrSend(BackupDataType.SETTINGS) },
                onQrScanClick = { showQrScanner = true },
                onExportClick = { activeDataTypeForExport = BackupDataType.SETTINGS },
                onImportClick = { activeDataTypeForImport = BackupDataType.SETTINGS },
                onPasteClick = {
                    pasteDataType = BackupDataType.SETTINGS
                    pasteInputText = ""
                    showPasteDialog = true
                },
                onFormatInfoClick = { showCsvFormatDialogFor = BackupDataType.SETTINGS }
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
        Dialog(
            onDismissRequest = { activeDataTypeForExport = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            DialogEdgeToEdge()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { activeDataTypeForExport = null }
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .heightIn(max = 680.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    color = getDialogBackground(),
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AccentCyan)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Export ${type.displayName}",
                                color = AppTextColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { activeDataTypeForExport = null },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryTextColor())
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Choose your preferred export method:",
                            color = AppSecondaryTextColor(),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExportOptionRow(
                                icon = Icons.Default.Share,
                                title = "Save ZIP Archive (+ Full Media)",
                                subtitle = "Bundles all data + attached photos, audio, receipts & QRs",
                                accentColor = AccentGreen,
                                onClick = {
                                    val cur = type
                                    activeDataTypeForExport = null
                                    viewModel.exportDataJson(cur, selectedHotelId, selectedAccountScopeId) { json ->
                                        viewModel.exportDataCsv(cur, selectedHotelId, selectedAccountScopeId) { csv ->
                                            pendingExportContent = json
                                            pendingZipCsvContent = csv
                                            saveZipLauncher.launch("${cur.name.lowercase()}_with_media.zip")
                                        }
                                    }
                                }
                            )
                            ExportOptionRow(
                                icon = Icons.Default.Storage,
                                title = "Save JSON Backup (+ Exact QRs)",
                                subtitle = "Structured data & exact QR matrices (excludes raw photo/audio files)",
                                accentColor = AccentCyan,
                                onClick = {
                                    val cur = type
                                    activeDataTypeForExport = null
                                    viewModel.exportDataJson(cur, selectedHotelId, selectedAccountScopeId) { json ->
                                        pendingExportContent = json
                                        saveJsonLauncher.launch("${cur.name.lowercase()}_backup.json")
                                    }
                                }
                            )
                            ExportOptionRow(
                                icon = Icons.Default.ReceiptLong,
                                title = "Save CSV Spreadsheet (Data Only)",
                                subtitle = "Excel/Sheets table — excludes media files & QRs",
                                accentColor = AccentOrange,
                                onClick = {
                                    val cur = type
                                    activeDataTypeForExport = null
                                    viewModel.exportDataCsv(cur, selectedHotelId, selectedAccountScopeId) { csv ->
                                        pendingExportContent = csv
                                        saveCsvLauncher.launch("${cur.name.lowercase()}_export.csv")
                                    }
                                }
                            )
                            ExportOptionRow(
                                icon = Icons.Default.ContentCopy,
                                title = "Copy CSV to Clipboard (No Media)",
                                subtitle = "Copy plain table directly — excludes media & QRs",
                                accentColor = AccentGold,
                                onClick = {
                                    activeDataTypeForExport = null
                                    viewModel.exportDataCsv(type, selectedHotelId, selectedAccountScopeId) { csv ->
                                        clipboardManager.setText(AnnotatedString(csv))
                                        Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            ExportOptionRow(
                                icon = Icons.Default.QrCode,
                                title = "Export via QR Stream (+ Payment QRs)",
                                subtitle = "Encrypted QR stream + exact pixel-reconstructed payment QRs (no photos/audio)",
                                accentColor = AccentPurple,
                                onClick = {
                                    activeDataTypeForExport = null
                                    viewModel.exportViaQr(type, selectedHotelId, selectedAccountScopeId) { result ->
                                        when (result) {
                                            is ExportResult.Success -> {
                                                qrSessionId = UUID.randomUUID().toString()
                                                activeQrItemName = type.displayName
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
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { activeDataTypeForExport = null }) {
                                Text("Close", color = AppSecondaryTextColor(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // IMPORT METHOD DIALOG
    // ==========================================
    if (activeDataTypeForImport != null) {
        val type = activeDataTypeForImport!!
        Dialog(
            onDismissRequest = { activeDataTypeForImport = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            DialogEdgeToEdge()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { activeDataTypeForImport = null }
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .heightIn(max = 680.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    color = getDialogBackground(),
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = AccentCyan)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Import ${type.displayName}",
                                color = AppTextColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { activeDataTypeForImport = null },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryTextColor())
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Select how you want to import data:",
                            color = AppSecondaryTextColor(),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExportOptionRow(
                                icon = Icons.Default.Share,
                                title = "Import ZIP Archive (+ Full Media)",
                                subtitle = "Restores data along with all bundled QRs, photos, receipts & audio",
                                accentColor = AccentGreen,
                                onClick = {
                                    val cur = type
                                    pendingImportDataType = cur
                                    activeDataTypeForImport = null
                                    openFileLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                                }
                            )
                            ExportOptionRow(
                                icon = Icons.Default.Storage,
                                title = "Import File (.json / .csv / .zip)",
                                subtitle = "JSON carries exact QRs; ZIP adds all media files; CSV is data-only",
                                accentColor = AccentCyan,
                                onClick = {
                                    val cur = type
                                    pendingImportDataType = cur
                                    activeDataTypeForImport = null
                                    openFileLauncher.launch(arrayOf("*/*", "application/zip", "application/json", "text/csv", "text/comma-separated-values", "text/plain"))
                                }
                            )
                            ExportOptionRow(
                                icon = Icons.Default.ContentPaste,
                                title = "Paste CSV / JSON Text (No Raw Media)",
                                subtitle = "Paste raw CSV or JSON (+ exact QR matrices, no attached files)",
                                accentColor = AccentOrange,
                                onClick = {
                                    val cur = activeDataTypeForImport
                                    activeDataTypeForImport = null
                                    pasteDataType = cur ?: BackupDataType.FULL_BACKUP
                                    pasteInputText = ""
                                    showPasteDialog = true
                                }
                            )
                            ExportOptionRow(
                                icon = Icons.Default.QrCodeScanner,
                                title = "Scan QR Code (+ Payment QRs)",
                                subtitle = "Scans QR stream & reconstructs exact pixel-identical payment QRs",
                                accentColor = AccentPurple,
                                onClick = {
                                    activeDataTypeForImport = null
                                    showQrScanner = true
                                }
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { activeDataTypeForImport = null }) {
                                Text("Cancel", color = AppSecondaryTextColor(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // PASTE TEXT DIALOG (Available for ALL data types)
    // ==========================================
    if (showPasteDialog) {
        Dialog(
            onDismissRequest = { if (!isImporting) showPasteDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            DialogEdgeToEdge()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { if (!isImporting) showPasteDialog = false }
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    color = getDialogBackground(),
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = AccentCyan)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Paste ${pasteDataType.displayName}",
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Paste CSV or JSON formatted content below",
                                    color = AppSecondaryTextColor(),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
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
                        Spacer(modifier = Modifier.height(8.dp))
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { showPasteDialog = false },
                                enabled = !isImporting,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = AppSecondaryTextColor())
                            }
                            Button(
                                onClick = {
                                    if (pasteInputText.isBlank()) {
                                        Toast.makeText(context, "Please paste some content first", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val trimmed = pasteInputText.trimStart()
                                    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                                        val payload = pasteInputText
                                        showPasteDialog = false
                                        pasteInputText = ""
                                        isImporting = false
                                        viewModel.startSmartImportFlow(
                                            payloadText = payload,
                                            targetHotelId = selectedHotelId,
                                            targetBankAccountId = selectedAccountScopeId
                                        ) { outcome ->
                                            when (outcome) {
                                                is ImportOutcome.Success -> Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                                                is ImportOutcome.Error -> Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
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
                                    }
                                },
                                enabled = !isImporting && pasteInputText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor()),
                                modifier = Modifier.weight(1.4f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isImporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Importing...", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                } else {
                                    Text("Import & Save", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // CSV FORMAT INFO DIALOG
    // ==========================================
    if (showCsvFormatDialogFor != null) {
        val type = showCsvFormatDialogFor!!
        Dialog(
            onDismissRequest = { showCsvFormatDialogFor = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            DialogEdgeToEdge()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showCsvFormatDialogFor = null }
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .heightIn(max = 680.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    color = getDialogBackground(),
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AccentCyan)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${type.displayName} Format",
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showCsvFormatDialogFor = null },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = AppSecondaryTextColor())
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "You can prepare data in Microsoft Excel or Google Sheets and export as CSV:",
                                color = AppTextColor(),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Expected Columns:", color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppTextColor().copy(0.08f))
                                    .horizontalScroll(rememberScrollState())
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = getCsvHeaderFor(type),
                                    color = AccentCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Example Rows:", color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppTextColor().copy(0.08f))
                                    .horizontalScroll(rememberScrollState())
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = getSampleTemplateFor(type),
                                    color = AppSecondaryTextColor(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    softWrap = false
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { showCsvFormatDialogFor = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close", color = AppSecondaryTextColor())
                            }
                            Button(
                                onClick = {
                                    val sample = getSampleTemplateFor(type)
                                    clipboardManager.setText(AnnotatedString(sample))
                                    Toast.makeText(context, "Sample template copied", Toast.LENGTH_SHORT).show()
                                    showCsvFormatDialogFor = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor()),
                                modifier = Modifier.weight(1.4f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Template", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // SMART SYNC INTERMEDIATE FLOW DIALOGS
    // ==========================================
    when (val state = importUiFlowState) {
        is FinancialViewModel.ImportUiFlowState.ParentMappingRequired -> {
            ParentMappingDialog(
                unmappedParents = state.unmappedParents,
                availableHotels = allHotels,
                availableBankAccounts = allBankAccounts,
                availableOwnerAccounts = allOwnerAccounts,
                currentResolutions = parentDecisions,
                onResolutionChanged = { key, res ->
                    viewModel.setParentResolution(key, res)
                },
                onConfirm = {
                    viewModel.confirmParentMappings { outcome ->
                        when (outcome) {
                            is ImportOutcome.Success -> Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                            is ImportOutcome.Error -> Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismiss = {
                    viewModel.cancelImportFlow()
                }
            )
        }
        is FinancialViewModel.ImportUiFlowState.MenuConfirmationRequired -> {
            MenuConfirmationDialog(
                pendingMenuItems = state.pendingMenuItems,
                acceptedKeys = acceptedMenuItemKeys,
                onToggleItem = { key, isAccepted ->
                    viewModel.setMenuItemAccepted(key, isAccepted)
                },
                onToggleAll = { isAllAccepted ->
                    viewModel.setAllMenuItemsAccepted(isAllAccepted)
                },
                onConfirm = {
                    viewModel.confirmMenuItems { outcome ->
                        when (outcome) {
                            is ImportOutcome.Success -> Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                            is ImportOutcome.Error -> Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismiss = {
                    viewModel.cancelImportFlow()
                }
            )
        }
        is FinancialViewModel.ImportUiFlowState.ConflictsDetected -> {
            com.example.ui.screens.SyncResolutionDialog(
                conflicts = state.conflicts,
                conflictDecisions = conflictDecisions,
                isMerging = false,
                onResolutionChanged = { key, res ->
                    viewModel.setConflictResolution(key, res)
                },
                onResolveAllLatest = {
                    viewModel.resolveAllConflictsLatest()
                },
                onResolveAllLocal = {
                    viewModel.resolveAllConflictsLocal()
                },
                onConfirmMerge = {
                    viewModel.proceedToMergeSummaryConfirmation()
                },
                onDismiss = {
                    viewModel.cancelImportFlow()
                }
            )
        }
        is FinancialViewModel.ImportUiFlowState.MergeSummaryConfirmationRequired -> {
            com.example.ui.components.MergeSummaryWarningDialog(
                plan = state.plan,
                onConfirm = {
                    viewModel.executeMergePlan(state.plan) { outcome ->
                        when (outcome) {
                            is ImportOutcome.Success -> Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                            is ImportOutcome.Error -> Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismiss = {
                    viewModel.cancelImportFlow()
                }
            )
        }
        else -> {
            // Handled in other states
        }
    }

    // ==========================================
    // QR EXPORT STREAM DIALOG
    // ==========================================
    if (activeQrExportData != null) {
        QrExportStreamDialog(
            exportData = activeQrExportData!!,
            sessionId = qrSessionId,
            itemName = activeQrItemName,
            onDismiss = { activeQrExportData = null }
        )
    }

    // ==========================================
    // QR SCANNER DIALOG
    // ==========================================
    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onSuccess = { payload, key, iv, scannedQrUris ->
                showQrScanner = false
                viewModel.startQrSmartImportFlow(
                    payload = payload,
                    key = key,
                    iv = iv,
                    targetHotelId = selectedHotelId,
                    targetBankAccountId = selectedAccountScopeId,
                    scannedPaymentQrUris = scannedQrUris
                ) { outcome ->
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
        )
    }

    // ==========================================
    // KHATA SCANNER DIALOG
    // ==========================================
    if (showKhataScanner) {
        com.example.ui.screens.settings.components.KhataScannerDialog(
            hotels = allHotels,
            initialHotelId = selectedHotelId,
            allMenus = allMenuItems,
            allTransactions = allTransactions,
            onImportTransactions = { txs, onDone ->
                viewModel.importKhataTransactions(txs, onDone)
            },
            onDismiss = { showKhataScanner = false }
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
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAppLightMode()) 2.dp else 4.dp)
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
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AccentGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }

                // QR Send Button
                Button(
                    onClick = onQrSendClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.25f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("QR Send", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }

                // QR Receive / Scan Button
                Button(
                    onClick = onQrScanClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple.copy(alpha = 0.25f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("QR Scan", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
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
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }

                // Import Button
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
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
                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = AppSecondaryTextColor(), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
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
        shadowElevation = if (isAppLightMode()) 2.dp else 4.dp,
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
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = AppSecondaryTextColor(),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
        BackupDataType.HOTEL_INFO -> "Hotel Name,Advance Payment,Phone Number,Location,UPI ID,Notes"
        BackupDataType.HOTEL_HISTORY -> "Timestamp,Date,Is Night,Total Amount,Items"
        BackupDataType.SINGLE_BANK_ACCOUNT, BackupDataType.BANK_ACCOUNTS -> "Account Name,Balance,UPI ID,Account Number,IFSC Code,Branch Name"
        BackupDataType.WALLET_WHOLE -> "# BANK ACCOUNTS\nAccount Name,Balance,UPI ID...\n# TRANSACTION HISTORY\nTimestamp,Date,Type..."
        BackupDataType.TRANSACTION_HISTORY -> "Timestamp,Date,Description,Amount,Type,Category,Bank Account ID,Notes"
        BackupDataType.ELECTRICITY_WHOLE -> "# METER READINGS\nTimestamp,Date,Total Reading...\n# UTILITY BILLS\nBill ID,Start Date,End Date..."
        BackupDataType.METER_READINGS -> "Reading Timestamp,Date,Total Reading,Units Consumed,Is Paid"
        BackupDataType.UTILITY_BILLS -> "Start Date,End Date,Start Date Text,End Date Text,Total Units,Calculated Amount,Due Date,Is Paid,Transaction ID"
        BackupDataType.ELECTRICITY_RATES -> "Effective Timestamp,Effective Date,Rate Per Unit (₹/kWh)"
        BackupDataType.SETTINGS -> "Name,Contact,Currency,Language,Theme Mode,Text Size,Bold Text,Sticky Card Width %,Sticky Preview Lines,App Version"
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

        BackupDataType.HOTEL_INFO ->
            """# HOTEL DETAILS
Hotel Name,Advance Payment,Phone Number,Location,UPI ID,Notes
"Grand Bengal Hotel",500.0,"9876543210","College Street","hotel@upi","Daily Mess""""

        BackupDataType.HOTEL_HISTORY ->
            """Timestamp,Date,Is Night,Total Amount,Items
1723500000000,"2024-08-13 13:00",false,90.0,"Plain Rice:2:35.0;Chicken Piece:1:20.0"
1723536000000,"2024-08-13 20:30",true,35.0,"Roti:3:5.0;Egg Curry:1:20.0""""

        BackupDataType.SINGLE_BANK_ACCOUNT, BackupDataType.BANK_ACCOUNTS ->
            """Account Name,Balance,UPI ID,Account Number,IFSC Code,Branch Name
"Main Bank Account",15000.0,"myhotel@okaxis","1234567890","SBIN0001234","Kolkata Main"
"Cash Drawer",2500.0,"","","","Daily Cash""""

        BackupDataType.WALLET_WHOLE ->
            """# BANK ACCOUNTS
Account Name,Current Balance,UPI ID,Account Number,IFSC Code,Branch Name
"Main Bank Account",15000.0,"myhotel@okaxis","1234567890","SBIN0001234","Kolkata Main"

# TRANSACTION HISTORY
Timestamp,Date,Time,Transaction Type,Category,Description,Amount,Bank Account ID,Notes
1723500000000,"2024-08-13","10:30 AM","Expense","Grocery","Rice & Oil",1200.0,1,"Shop purchase""""

        BackupDataType.TRANSACTION_HISTORY ->
            """Timestamp,Date,Description,Amount,Type,Category,Bank Account ID,Notes
1723500000000,"2024-08-13 10:30","Grocery Supplies",1200.0,"EXPENSE","Grocery",1,"Rice and spices"
1723510000000,"2024-08-13 14:00","Daily Counter Sale",3500.0,"INCOME","Sales",1,"Lunch receipts""""

        BackupDataType.ELECTRICITY_WHOLE ->
            """# METER READINGS
Reading Timestamp,Date,Total Reading (kWh),Units Consumed,Payment Status
1723400000000,"2024-08-12",12450.0,14.5,"Paid"

# UTILITY BILLS
Bill ID,Billing Start Date,Billing End Date,Total Units,Effective Rate (₹/unit),Calculated Amount (₹),Due Date,Payment Status,Transaction Reference,Variable Rate Breakdown,Start Timestamp,End Timestamp,Due Timestamp
"bill-1","2024-07-13","2024-08-12",340.0,7.00,2380.0,"2024-08-25",Paid,"WBSEDCL-123","Flat",1720800000000,1723400000000,1724500000000"""

        BackupDataType.METER_READINGS ->
            """Reading Timestamp,Date,Total Reading,Units Consumed,Is Paid
1723400000000,"2024-08-12",12450.0,14.5,false
1723486400000,"2024-08-13",12465.2,15.2,false"""

        BackupDataType.UTILITY_BILLS ->
            """Start Date,End Date,Start Date Text,End Date Text,Total Units,Calculated Amount,Due Date,Is Paid,Transaction ID
1720800000000,1723400000000,"2024-07-13","2024-08-12",340.0,2380.0,"2024-08-25",true,"WBSEDCL-847291""""

        BackupDataType.ELECTRICITY_RATES ->
            """Effective Timestamp,Effective Date,Rate Per Unit (₹/kWh)
1749254400000,"2026-06-07",9.0
1725494400000,"2025-09-05",8.0"""

        BackupDataType.SETTINGS ->
            """{
  "dataType": "SETTINGS",
  "version": 3,
  "userProfile": {"language": "en", "themeMode": "Dark", "isBoldTextEnabled": true, "appVersion": "0.0.2"}
}"""

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
