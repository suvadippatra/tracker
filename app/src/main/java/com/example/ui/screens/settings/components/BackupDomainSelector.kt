package com.example.ui.screens.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExportSelection
import com.example.data.model.BankAccountEntity
import com.example.data.model.HotelEntity
import com.example.data.model.StickyNoteEntity
import com.example.ui.components.*

@Composable
fun UniversalImportCard(
    isBn: Boolean,
    isBusy: Boolean,
    onScanQr: () -> Unit,
    onImportFile: () -> Unit,
    onPasteData: () -> Unit
) {
    val accent = AppAccentColor()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = getSettingsItemBg(),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        shadowElevation = if (isAppLightMode()) 2.dp else 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = if (isBn) "ইমপোর্ট করুন" else "Import Data",
                    color = AppTextColor(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onScanQr,
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("universal_import_qr_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = AppOnAccentColor(accent)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isBn) "QR স্ক্যান" else "Scan QR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onImportFile,
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("universal_import_file_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentCyan.copy(alpha = 0.16f),
                        contentColor = AppTextColor()
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isBn) "ফাইল" else "File",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onPasteData,
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("universal_import_paste_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, getGlassBorder()),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = AppSecondaryTextColor(), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isBn) "পেস্ট" else "Paste",
                        color = AppTextColor(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModularExportBuilderCard(
    isBn: Boolean,
    selection: ExportSelection,
    onSelectionChange: (ExportSelection) -> Unit,
    hotels: List<HotelEntity>,
    menuCount: Int,
    orderCount: Int,
    bankAccounts: List<BankAccountEntity>,
    expenseCount: Int,
    readingCount: Int,
    billCount: Int,
    stickyNotes: List<StickyNoteEntity>,
    stickyTags: List<String>,
    ownerAccountCount: Int,
    isBusy: Boolean,
    onExportZip: () -> Unit,
    onExportQr: () -> Unit,
    onCopyJson: () -> Unit,
    onExportCsv: () -> Unit
) {
    val accent = AppAccentColor()
    val hasSelection = selection.hasAnySelection()
    var expandedDomain by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = getSettingsItemBg(),
        border = BorderStroke(1.dp, getGlassBorder()),
        shadowElevation = if (isAppLightMode()) 2.dp else 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Mode Toggle (Full / Custom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentGreen.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = if (isBn) "এক্সপোর্ট করুন" else "Export Data",
                        color = AppTextColor(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Segmented Full vs Custom Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTextColor().copy(alpha = 0.07f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ModePill(
                        label = if (isBn) "সব" else "Full",
                        selected = selection.isFullBackup,
                        accent = accent,
                        tag = "export_mode_full",
                        onClick = { onSelectionChange(selection.copy(isFullBackup = true)) }
                    )
                    ModePill(
                        label = if (isBn) "কাস্টম" else "Custom",
                        selected = !selection.isFullBackup,
                        accent = accent,
                        tag = "export_mode_custom",
                        onClick = { onSelectionChange(selection.copy(isFullBackup = false)) }
                    )
                }
            }

            // Compact live summary pill row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (selection.isFullBackup || selection.includeHotelsDomain) {
                    CountChip("${hotels.size} ${if (isBn) "হোটেল" else "Hotels"} • $orderCount ${if (isBn) "অর্ডার" else "Orders"}")
                }
                if (selection.isFullBackup || selection.includeExtrasDomain) {
                    CountChip("${bankAccounts.size} ${if (isBn) "অ্যাকাউন্ট" else "Accounts"} • $expenseCount ${if (isBn) "লেনদেন" else "Tx"}")
                }
                if (selection.isFullBackup || selection.includeElectricityDomain) {
                    CountChip("$readingCount ${if (isBn) "রিডিং" else "Readings"} • $billCount ${if (isBn) "বিল" else "Bills"}")
                }
                if (selection.isFullBackup || selection.includeStickyDomain) {
                    CountChip("${stickyNotes.size} ${if (isBn) "নোট" else "Notes"}")
                }
            }

            // Custom Domain Selector
            AnimatedVisibility(visible = !selection.isFullBackup) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. Hotels
                    DomainToggleRow(
                        icon = Icons.Default.Restaurant,
                        title = if (isBn) "হোটেল ও মেনু" else "Hotels",
                        countText = "${hotels.size} / $menuCount / $orderCount",
                        checked = selection.includeHotelsDomain,
                        expanded = expandedDomain == "hotels",
                        onCheckedChange = { onSelectionChange(selection.copy(includeHotelsDomain = it)) },
                        onToggleExpand = { expandedDomain = if (expandedDomain == "hotels") null else "hotels" }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ScopeChip(
                                    label = if (isBn) "সব হোটেল" else "All Hotels",
                                    selected = selection.selectedHotelIds.isEmpty(),
                                    onClick = { onSelectionChange(selection.copy(selectedHotelIds = emptySet())) }
                                )
                                hotels.forEach { h ->
                                    val isSel = h.id in selection.selectedHotelIds
                                    ScopeChip(
                                        label = h.name,
                                        selected = isSel,
                                        onClick = {
                                            val next = selection.selectedHotelIds.toMutableSet()
                                            if (isSel) next.remove(h.id) else next.add(h.id)
                                            onSelectionChange(selection.copy(selectedHotelIds = next))
                                        }
                                    )
                                }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SubToggleChip(if (isBn) "তথ্য ও QR" else "Profile & QR", selection.includeHotelInfo) {
                                    onSelectionChange(selection.copy(includeHotelInfo = it))
                                }
                                SubToggleChip(if (isBn) "মেনু" else "Menus", selection.includeHotelMenus) {
                                    onSelectionChange(selection.copy(includeHotelMenus = it))
                                }
                                SubToggleChip(if (isBn) "অর্ডার ও পেমেন্ট" else "Orders & Paid", selection.includeHotelOrders) {
                                    onSelectionChange(selection.copy(includeHotelOrders = it))
                                }
                            }
                        }
                    }

                    // 2. Extras & Accounts
                    DomainToggleRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = if (isBn) "অ্যাকাউন্ট ও খরচ" else "Extras & Accounts",
                        countText = "${bankAccounts.size} / $expenseCount",
                        checked = selection.includeExtrasDomain,
                        expanded = expandedDomain == "extras",
                        onCheckedChange = { onSelectionChange(selection.copy(includeExtrasDomain = it)) },
                        onToggleExpand = { expandedDomain = if (expandedDomain == "extras") null else "extras" }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ScopeChip(
                                    label = if (isBn) "সব অ্যাকাউন্ট" else "All Accounts",
                                    selected = selection.selectedAccountIds.isEmpty(),
                                    onClick = { onSelectionChange(selection.copy(selectedAccountIds = emptySet())) }
                                )
                                bankAccounts.forEach { b ->
                                    val isSel = b.id in selection.selectedAccountIds
                                    ScopeChip(
                                        label = b.name,
                                        selected = isSel,
                                        onClick = {
                                            val next = selection.selectedAccountIds.toMutableSet()
                                            if (isSel) next.remove(b.id) else next.add(b.id)
                                            onSelectionChange(selection.copy(selectedAccountIds = next))
                                        }
                                    )
                                }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SubToggleChip(if (isBn) "অ্যাকাউন্ট ও QR" else "Accounts & QRs", selection.includeBankAccounts) {
                                    onSelectionChange(selection.copy(includeBankAccounts = it))
                                }
                                SubToggleChip(if (isBn) "লেনদেন" else "Transactions", selection.includeTransactions) {
                                    onSelectionChange(selection.copy(includeTransactions = it))
                                }
                            }
                        }
                    }

                    // 3. Electricity
                    DomainToggleRow(
                        icon = Icons.Default.Bolt,
                        title = if (isBn) "বিদ্যুৎ বিল" else "Electricity",
                        countText = "$readingCount / $billCount",
                        checked = selection.includeElectricityDomain,
                        expanded = expandedDomain == "electricity",
                        onCheckedChange = { onSelectionChange(selection.copy(includeElectricityDomain = it)) },
                        onToggleExpand = { expandedDomain = if (expandedDomain == "electricity") null else "electricity" }
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SubToggleChip(if (isBn) "রিডিং" else "Readings", selection.includeMeterReadings) {
                                onSelectionChange(selection.copy(includeMeterReadings = it))
                            }
                            SubToggleChip(if (isBn) "বিল" else "Bills", selection.includeUtilityBills) {
                                onSelectionChange(selection.copy(includeUtilityBills = it))
                            }
                            SubToggleChip(if (isBn) "রেট" else "Rates", selection.includeElectricityRates) {
                                onSelectionChange(selection.copy(includeElectricityRates = it))
                            }
                        }
                    }

                    // 4. Sticky Notes
                    DomainToggleRow(
                        icon = Icons.Default.StickyNote2,
                        title = if (isBn) "স্টিকি নোটস" else "Sticky Notes",
                        countText = "${stickyNotes.size}",
                        checked = selection.includeStickyDomain,
                        expanded = expandedDomain == "sticky",
                        onCheckedChange = { onSelectionChange(selection.copy(includeStickyDomain = it)) },
                        onToggleExpand = { expandedDomain = if (expandedDomain == "sticky") null else "sticky" }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ScopeChip(
                                    label = if (isBn) "সব নোট" else "All Notes",
                                    selected = selection.selectedStickyTags.isEmpty() && selection.selectedStickyNoteIds.isEmpty(),
                                    onClick = {
                                        onSelectionChange(selection.copy(selectedStickyTags = emptySet(), selectedStickyNoteIds = emptySet()))
                                    }
                                )
                                stickyTags.forEach { tag ->
                                    val isSel = tag in selection.selectedStickyTags
                                    ScopeChip(
                                        label = "#$tag",
                                        selected = isSel,
                                        onClick = {
                                            val next = selection.selectedStickyTags.toMutableSet()
                                            if (isSel) next.remove(tag) else next.add(tag)
                                            onSelectionChange(selection.copy(selectedStickyTags = next, selectedStickyNoteIds = emptySet()))
                                        }
                                    )
                                }
                            }
                            if (stickyNotes.isNotEmpty() && selection.selectedStickyTags.isEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    stickyNotes.take(12).forEach { note ->
                                        val isSel = note.id in selection.selectedStickyNoteIds
                                        val titleLabel = note.title.ifBlank { note.bodyText.take(18).ifBlank { "Note #${note.id}" } }
                                        ScopeChip(
                                            label = titleLabel,
                                            selected = isSel,
                                            onClick = {
                                                val next = selection.selectedStickyNoteIds.toMutableSet()
                                                if (isSel) next.remove(note.id) else next.add(note.id)
                                                onSelectionChange(selection.copy(selectedStickyNoteIds = next))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Profile & Owner Accounts
                    DomainToggleRow(
                        icon = Icons.Default.Person,
                        title = if (isBn) "প্রোফাইল ও ওনার অ্যাকাউন্ট" else "Profile & Owner",
                        countText = "$ownerAccountCount",
                        checked = selection.includeProfileDomain,
                        expanded = false,
                        expandable = false,
                        onCheckedChange = {
                            onSelectionChange(selection.copy(includeProfileDomain = it, includeOwnerAccounts = it))
                        },
                        onToggleExpand = {}
                    ) {}

                    // 6. App Settings
                    DomainToggleRow(
                        icon = Icons.Default.Settings,
                        title = if (isBn) "অ্যাপ সেটিংস" else "App Settings",
                        countText = "",
                        checked = selection.includeSettingsDomain,
                        expanded = false,
                        expandable = false,
                        onCheckedChange = { onSelectionChange(selection.copy(includeSettingsDomain = it)) },
                        onToggleExpand = {}
                    ) {}
                }
            }

            // Primary Export Action Bar (3 buttons: ZIP, QR Send, Copy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportZip,
                    enabled = hasSelection && !isBusy,
                    modifier = Modifier
                        .weight(1.1f)
                        .heightIn(min = 48.dp)
                        .testTag("export_zip_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = AppOnAccentColor(accent)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("ZIP", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                FilledTonalButton(
                    onClick = onExportQr,
                    enabled = hasSelection && !isBusy,
                    modifier = Modifier
                        .weight(1.1f)
                        .heightIn(min = 48.dp)
                        .testTag("export_qr_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentGreen.copy(alpha = 0.16f),
                        contentColor = AppTextColor()
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isBn) "QR পাঠান" else "QR Send",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onCopyJson,
                    enabled = hasSelection && !isBusy,
                    modifier = Modifier
                        .weight(0.95f)
                        .heightIn(min = 48.dp)
                        .testTag("export_copy_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, getGlassBorder()),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AppSecondaryTextColor(), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isBn) "কপি" else "Copy",
                        color = AppTextColor(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            // Discreet Export-Only CSV for Excel viewing
            TextButton(
                onClick = onExportCsv,
                enabled = hasSelection && !isBusy,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 40.dp)
                    .testTag("export_csv_excel_btn"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null, tint = AppSecondaryTextColor(), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isBn) "CSV এক্সপোর্ট (Excel)" else "Export CSV (Excel)",
                    color = AppSecondaryTextColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CompactKhataImportCard(
    isBn: Boolean,
    onOpenKhata: () -> Unit
) {
    Surface(
        onClick = onOpenKhata,
        shape = RoundedCornerShape(18.dp),
        color = getSettingsItemBg(),
        border = BorderStroke(1.dp, getGlassBorder()),
        shadowElevation = if (isAppLightMode()) 2.dp else 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backup_khata_entry_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentGold.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = if (isBn) "খাতা টেক্সট এন্ট্রি" else "Khata Text Entry",
                    color = AppTextColor(),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppSecondaryTextColor(),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun UniversalPasteImportDialog(
    isBn: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(22.dp),
            color = getDialogBackground(),
            border = BorderStroke(1.dp, getGlassBorder()),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "ডেটা পেস্ট করুন" else "Paste Backup JSON",
                        color = AppTextColor(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onPasteFromClipboard,
                        modifier = Modifier.testTag("paste_from_clipboard_btn")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isBn) "ক্লিপবোর্ড" else "Clipboard", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("{ ... }", color = AppSecondaryTextColor().copy(alpha = 0.6f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 260.dp)
                        .testTag("paste_backup_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text(if (isBn) "বাতিল" else "Cancel", color = AppSecondaryTextColor())
                    }
                    Button(
                        onClick = onImport,
                        enabled = text.isNotBlank(),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp)
                            .testTag("confirm_paste_import_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppAccentColor(),
                            contentColor = AppOnAccentColor()
                        )
                    ) {
                        Text(if (isBn) "ইমপোর্ট" else "Import", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModePill(
    label: String,
    selected: Boolean,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) AppOnAccentColor(accent) else AppSecondaryTextColor(),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun CountChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AppTextColor().copy(alpha = 0.06f)
    ) {
        Text(
            text = text,
            color = AppSecondaryTextColor(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DomainToggleRow(
    icon: ImageVector,
    title: String,
    countText: String,
    checked: Boolean,
    expanded: Boolean,
    expandable: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    onToggleExpand: () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    val accent = AppAccentColor()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppTextColor().copy(alpha = if (checked) 0.05f else 0.025f),
        border = BorderStroke(1.dp, if (checked) accent.copy(alpha = 0.25f) else getGlassBorder().copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (expandable) onToggleExpand() else onCheckedChange(!checked)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(checkedColor = accent)
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) accent else AppSecondaryTextColor(),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    color = AppTextColor(),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (countText.isNotBlank()) {
                    Text(
                        text = countText,
                        color = AppSecondaryTextColor(),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                if (expandable) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AppSecondaryTextColor(),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expandable && expanded && checked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                ) {
                    expandedContent()
                }
            }
        }
    }
}

@Composable
private fun ScopeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = AppAccentColor()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else AppTextColor().copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (selected) accent else getGlassBorder())
    ) {
        Text(
            text = label,
            color = if (selected) accent else AppTextColor(),
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SubToggleChip(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val accent = AccentGreen
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(10.dp),
        color = if (checked) accent.copy(alpha = 0.14f) else Color.Transparent,
        border = BorderStroke(1.dp, if (checked) accent.copy(alpha = 0.6f) else getGlassBorder())
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (checked) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = if (checked) accent else AppSecondaryTextColor(),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = if (checked) AppTextColor() else AppSecondaryTextColor(),
                fontSize = 11.sp,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
