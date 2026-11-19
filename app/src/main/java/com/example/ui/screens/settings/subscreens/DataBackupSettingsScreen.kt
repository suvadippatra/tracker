package com.example.ui.screens.settings.subscreens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BackupDataType
import com.example.data.ExportResult
import com.example.data.ExportSelection
import com.example.data.ImportOutcome
import com.example.ui.components.*
import com.example.ui.screens.settings.components.*
import com.example.ui.viewmodel.FinancialViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Composable
fun DataBackupSettingsScreen(
    viewModel: FinancialViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBn = userProfile.language == "bn"

    val allHotels by viewModel.allHotels.collectAsStateWithLifecycle()
    val activeHotelId by viewModel.activeHotelId.collectAsStateWithLifecycle()
    val allBankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExtraExpenses.collectAsStateWithLifecycle()
    val allReadings by viewModel.allMeterReadings.collectAsStateWithLifecycle()
    val allUtilityBills by viewModel.allUtilityBills.collectAsStateWithLifecycle()
    val allMenuItems by viewModel.allHotelMenus.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allStickyNotes by viewModel.allStickyNotes.collectAsStateWithLifecycle()
    val allStickyTags by viewModel.allStickyTags.collectAsStateWithLifecycle()
    val allOwnerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle()

    val importUiFlowState by viewModel.importUiFlowState.collectAsStateWithLifecycle()
    val parentDecisions by viewModel.parentDecisions.collectAsStateWithLifecycle()
    val acceptedMenuItemKeys by viewModel.acceptedMenuItemKeys.collectAsStateWithLifecycle()
    val conflictDecisions by viewModel.conflictDecisions.collectAsStateWithLifecycle()

    var selection by remember { mutableStateOf(ExportSelection()) }
    var isBusy by remember { mutableStateOf(false) }

    // Dialog / Modal state
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteInputText by remember { mutableStateOf("") }
    var activeQrExportData by remember { mutableStateOf<ExportResult.Success?>(null) }
    var activeQrItemName by remember { mutableStateOf("Full Backup") }
    var qrSessionId by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }
    var showKhataScanner by remember { mutableStateOf(false) }

    // Pending export payloads for SAF CreateDocument callbacks
    var pendingZipSelection by remember { mutableStateOf<ExportSelection?>(null) }
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }

    val hasLocalModalOpen = showPasteDialog || activeQrExportData != null || showQrScanner || showKhataScanner
    BackHandler(enabled = !hasLocalModalOpen && importUiFlowState is FinancialViewModel.ImportUiFlowState.Idle) {
        onBack()
    }

    // SAF Launcher: Save ZIP Archive (JSON + Scoped Media ONLY for current selection)
    val saveZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        val sel = pendingZipSelection
        if (uri != null && sel != null) {
            isBusy = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val db = com.example.data.AppDatabase.getDatabase(context)
                    val engine = com.example.data.UnifiedExportEngine(db)
                    val jsonPayload = engine.exportJson(sel, includeQrMedia = true)
                    val scopedMedia = engine.collectScopedMediaEntries(context, sel)

                    var mediaCount = 0
                    context.contentResolver.openOutputStream(uri)?.use { rawOs ->
                        ZipOutputStream(BufferedOutputStream(rawOs)).use { zos ->
                            zos.putNextEntry(ZipEntry("backup_data.json"))
                            zos.write(jsonPayload.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()

                            scopedMedia.forEach { (entryName, file) ->
                                if (file.exists() && file.canRead()) {
                                    zos.putNextEntry(ZipEntry(entryName))
                                    file.inputStream().buffered().use { fis -> fis.copyTo(zos) }
                                    zos.closeEntry()
                                    mediaCount++
                                }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        isBusy = false
                        val msg = if (isBn) {
                            "ZIP সংরক্ষিত হয়েছে ($mediaCount টি মিডিয়া ফাইলসহ)"
                        } else {
                            "ZIP saved ($mediaCount media file(s) included)"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isBusy = false
                        Toast.makeText(context, "ZIP save failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // SAF Launcher: Export-Only CSV for Excel viewing
    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val csv = pendingCsvContent
        if (uri != null && !csv.isNullOrBlank()) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(csv.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            if (isBn) "CSV সংরক্ষিত হয়েছে" else "CSV saved for Excel",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "CSV save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // SAF Launcher: Universal File Import (.zip / .json)
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isBusy = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes == null || bytes.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            isBusy = false
                            Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val isZip = bytes.size >= 4 &&
                        bytes[0] == 0x50.toByte() &&
                        bytes[1] == 0x4B.toByte() &&
                        bytes[2] == 0x03.toByte() &&
                        bytes[3] == 0x04.toByte()

                    val extractedText: String?
                    var restoredMediaCount = 0

                    if (isZip) {
                        val attachmentsDir = File(context.filesDir, "attachments").apply { if (!exists()) mkdirs() }
                        val qrCodesDir = File(context.filesDir, "qr_codes").apply { if (!exists()) mkdirs() }
                        var jsonEntryText: String? = null
                        var csvEntryText: String? = null

                        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
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
                                            val cleanName = File(name).name
                                            if (cleanName.isNotBlank()) {
                                                FileOutputStream(File(attachmentsDir, cleanName)).use { fos -> zis.copyTo(fos) }
                                                restoredMediaCount++
                                            }
                                        }
                                        name.startsWith("qr_codes/") -> {
                                            val cleanName = File(name).name
                                            if (cleanName.isNotBlank()) {
                                                FileOutputStream(File(qrCodesDir, cleanName)).use { fos -> zis.copyTo(fos) }
                                                restoredMediaCount++
                                            }
                                        }
                                        name.startsWith("root_files/") -> {
                                            val cleanName = File(name).name
                                            if (cleanName.isNotBlank()) {
                                                FileOutputStream(File(context.filesDir, cleanName)).use { fos -> zis.copyTo(fos) }
                                                restoredMediaCount++
                                            }
                                        }
                                    }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }

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

                    withContext(Dispatchers.Main) {
                        isBusy = false
                        if (extractedText.isNullOrBlank()) {
                            Toast.makeText(context, "No valid backup data found in file", Toast.LENGTH_SHORT).show()
                        } else {
                            val trimmed = extractedText.trimStart()
                            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                                viewModel.startSmartImportFlow(
                                    payloadText = extractedText,
                                    targetHotelId = activeHotelId,
                                    targetBankAccountId = null
                                ) { outcome ->
                                    when (outcome) {
                                        is ImportOutcome.Success -> {
                                            val suffix = if (restoredMediaCount > 0) " (+$restoredMediaCount media)" else ""
                                            Toast.makeText(context, outcome.message + suffix, Toast.LENGTH_LONG).show()
                                        }
                                        is ImportOutcome.Error -> {
                                            Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    if (isBn) "শুধুমাত্র .zip বা .json ব্যাকআপ ইমপোর্ট করা যাবে" else "Please import a .zip or .json backup file",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isBusy = false
                        Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val backupListState = rememberPersistedLazyListState("settings_data_backup")

    LazyColumn(
        state = backupListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 120.dp + rememberSplitScreenSafeBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isBusy || importUiFlowState is FinancialViewModel.ImportUiFlowState.Analyzing) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppAccentColor()
                )
            }
        }

        // 1. Universal Import Card (3 buttons: Scan QR, File, Paste)
        item {
            UniversalImportCard(
                isBn = isBn,
                isBusy = isBusy,
                onScanQr = { showQrScanner = true },
                onImportFile = {
                    openFileLauncher.launch(arrayOf("application/zip", "application/json", "text/plain", "*/*"))
                },
                onPasteData = {
                    pasteInputText = ""
                    showPasteDialog = true
                }
            )
        }

        // 2. Modular Export Builder Card (Full / Custom -> ZIP, QR Send, Copy + Export CSV)
        item {
            ModularExportBuilderCard(
                isBn = isBn,
                selection = selection,
                onSelectionChange = { selection = it },
                hotels = allHotels,
                menuCount = allMenuItems.size,
                orderCount = allTransactions.size,
                bankAccounts = allBankAccounts,
                expenseCount = allExpenses.size,
                readingCount = allReadings.size,
                billCount = allUtilityBills.size,
                stickyNotes = allStickyNotes,
                stickyTags = allStickyTags,
                ownerAccountCount = allOwnerAccounts.size,
                isBusy = isBusy,
                onExportZip = {
                    pendingZipSelection = selection
                    val timestamp = System.currentTimeMillis() / 1000
                    saveZipLauncher.launch("${selection.toFilePrefix()}_$timestamp.zip")
                },
                onExportQr = {
                    isBusy = true
                    val title = selection.toDisplayTitle(allHotels, allBankAccounts)
                    viewModel.exportSelectionViaQr(selection) { result ->
                        isBusy = false
                        when (result) {
                            is ExportResult.Success -> {
                                qrSessionId = UUID.randomUUID().toString()
                                activeQrItemName = title
                                activeQrExportData = result
                            }
                            is ExportResult.Error -> {
                                Toast.makeText(context, "QR export failed: ${result.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                onCopyJson = {
                    isBusy = true
                    viewModel.exportSelectionJson(selection, includeQrMedia = false) { json ->
                        isBusy = false
                        clipboardManager.setText(AnnotatedString(json))
                        Toast.makeText(
                            context,
                            if (isBn) "JSON ক্লিপবোর্ডে কপি হয়েছে" else "Backup JSON copied",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onExportCsv = {
                    isBusy = true
                    viewModel.exportSelectionCsv(selection) { csv ->
                        isBusy = false
                        pendingCsvContent = csv
                        val timestamp = System.currentTimeMillis() / 1000
                        saveCsvLauncher.launch("${selection.toFilePrefix()}_$timestamp.csv")
                    }
                }
            )
        }

        // 3. Compact Khata Text Entry Card
        item {
            CompactKhataImportCard(
                isBn = isBn,
                onOpenKhata = { showKhataScanner = true }
            )
        }
    }

    // Paste JSON Import Dialog
    if (showPasteDialog) {
        UniversalPasteImportDialog(
            isBn = isBn,
            text = pasteInputText,
            onTextChange = { pasteInputText = it },
            onPasteFromClipboard = {
                val clip = clipboardManager.getText()?.text
                if (!clip.isNullOrBlank()) {
                    pasteInputText = clip
                } else {
                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            },
            onImport = {
                val content = pasteInputText.trim()
                showPasteDialog = false
                if (content.startsWith("{") || content.startsWith("[")) {
                    viewModel.startSmartImportFlow(
                        payloadText = content,
                        targetHotelId = activeHotelId,
                        targetBankAccountId = null
                    ) { outcome ->
                        when (outcome) {
                            is ImportOutcome.Success -> Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                            is ImportOutcome.Error -> Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    viewModel.importUniversal(content, BackupDataType.FULL_BACKUP, activeHotelId) { outcome ->
                        when (outcome) {
                            is ImportOutcome.Success -> Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                            is ImportOutcome.Error -> Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDismiss = { showPasteDialog = false }
        )
    }

    // Smart Sync Interactive Review Dialogs
    when (val state = importUiFlowState) {
        is FinancialViewModel.ImportUiFlowState.ParentMappingRequired -> {
            ParentMappingDialog(
                unmappedParents = state.unmappedParents,
                availableHotels = allHotels,
                availableBankAccounts = allBankAccounts,
                availableOwnerAccounts = allOwnerAccounts,
                currentResolutions = parentDecisions,
                onResolutionChanged = { key, res -> viewModel.setParentResolution(key, res) },
                onConfirm = {
                    viewModel.confirmParentMappings { outcome ->
                        when (outcome) {
                            is ImportOutcome.Success -> Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                            is ImportOutcome.Error -> Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismiss = { viewModel.cancelImportFlow() }
            )
        }
        is FinancialViewModel.ImportUiFlowState.MenuConfirmationRequired -> {
            MenuConfirmationDialog(
                pendingMenuItems = state.pendingMenuItems,
                acceptedKeys = acceptedMenuItemKeys,
                onToggleItem = { key, isAccepted -> viewModel.setMenuItemAccepted(key, isAccepted) },
                onToggleAll = { isAllAccepted -> viewModel.setAllMenuItemsAccepted(isAllAccepted) },
                onConfirm = {
                    viewModel.confirmMenuItems { outcome ->
                        when (outcome) {
                            is ImportOutcome.Success -> Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                            is ImportOutcome.Error -> Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismiss = { viewModel.cancelImportFlow() }
            )
        }
        is FinancialViewModel.ImportUiFlowState.ConflictsDetected -> {
            com.example.ui.screens.SyncResolutionDialog(
                conflicts = state.conflicts,
                conflictDecisions = conflictDecisions,
                isMerging = false,
                onResolutionChanged = { key, res -> viewModel.setConflictResolution(key, res) },
                onResolveAllLatest = { viewModel.resolveAllConflictsLatest() },
                onResolveAllLocal = { viewModel.resolveAllConflictsLocal() },
                onConfirmMerge = { viewModel.proceedToMergeSummaryConfirmation() },
                onDismiss = { viewModel.cancelImportFlow() }
            )
        }
        is FinancialViewModel.ImportUiFlowState.MergeSummaryConfirmationRequired -> {
            MergeSummaryWarningDialog(
                plan = state.plan,
                onConfirm = {
                    viewModel.executeMergePlan(state.plan) { outcome ->
                        when (outcome) {
                            is ImportOutcome.Success -> Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                            is ImportOutcome.Error -> Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismiss = { viewModel.cancelImportFlow() }
            )
        }
        else -> Unit
    }

    // QR Export Stream Dialog
    if (activeQrExportData != null) {
        QrExportStreamDialog(
            exportData = activeQrExportData!!,
            sessionId = qrSessionId,
            itemName = activeQrItemName,
            onDismiss = { activeQrExportData = null }
        )
    }

    // QR Scanner Dialog
    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onSuccess = { payload, key, iv, scannedQrUris ->
                showQrScanner = false
                viewModel.startQrSmartImportFlow(
                    payload = payload,
                    key = key,
                    iv = iv,
                    targetHotelId = activeHotelId,
                    targetBankAccountId = null,
                    scannedPaymentQrUris = scannedQrUris
                ) { outcome ->
                    when (outcome) {
                        is ImportOutcome.Success -> Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                        is ImportOutcome.Error -> Toast.makeText(context, "Import failed: ${outcome.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Khata Text Entry Dialog
    if (showKhataScanner) {
        KhataScannerDialog(
            hotels = allHotels,
            initialHotelId = activeHotelId,
            allMenus = allMenuItems,
            allTransactions = allTransactions,
            onImportTransactions = { txs, onDone -> viewModel.importKhataTransactions(txs, onDone) },
            onDismiss = { showKhataScanner = false }
        )
    }
}
