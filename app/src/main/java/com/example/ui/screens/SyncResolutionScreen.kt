package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.DialogEdgeToEdge
import com.example.utils.ConflictItem
import com.example.utils.ConflictResolution
import java.text.SimpleDateFormat
import java.util.*

private val ScreenBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val CardBorderColor = Color(0xFF334155)
private val LocalCardBackground = Color(0xFF1A2234)
private val ImportedCardBackground = Color(0xFF162A38)
private val AccentCyan = Color(0xFF38BDF8)
private val AccentGold = Color(0xFFFBBF24)
private val AccentGreen = Color(0xFF34D399)
private val AccentRed = Color(0xFFF87171)
private val AccentPurple = Color(0xFFA78BFA)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)
private val TextMuted = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncResolutionDialog(
    conflicts: List<ConflictItem>,
    conflictDecisions: Map<String, ConflictResolution>,
    isMerging: Boolean = false,
    onResolutionChanged: (String, ConflictResolution) -> Unit,
    onResolveAllLatest: () -> Unit,
    onResolveAllLocal: () -> Unit,
    onConfirmMerge: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isMerging) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        SyncResolutionScreen(
            conflicts = conflicts,
            conflictDecisions = conflictDecisions,
            isMerging = isMerging,
            onResolutionChanged = onResolutionChanged,
            onResolveAllLatest = onResolveAllLatest,
            onResolveAllLocal = onResolveAllLocal,
            onConfirmMerge = onConfirmMerge,
            onBack = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncResolutionScreen(
    conflicts: List<ConflictItem>,
    conflictDecisions: Map<String, ConflictResolution>,
    isMerging: Boolean = false,
    onResolutionChanged: (String, ConflictResolution) -> Unit,
    onResolveAllLatest: () -> Unit,
    onResolveAllLocal: () -> Unit,
    onConfirmMerge: () -> Unit,
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    // Summary calculations
    val takeImportedCount = remember(conflicts, conflictDecisions) {
        conflicts.count { conflictDecisions[it.conflictKey] == ConflictResolution.TAKE_IMPORTED }
    }
    val keepLocalCount = remember(conflicts, conflictDecisions) {
        conflicts.count { conflictDecisions[it.conflictKey] == ConflictResolution.KEEP_LOCAL }
    }
    val keepBothCount = remember(conflicts, conflictDecisions) {
        conflicts.count { conflictDecisions[it.conflictKey] == ConflictResolution.KEEP_BOTH }
    }
    var showOverwriteConfirmDialog by remember { mutableStateOf(false) }

    // Grouping by categories
    val hotelConflicts = remember(conflicts) {
        conflicts.filterIsInstance<ConflictItem.HotelConflict>()
    }
    val bankAccountConflicts = remember(conflicts) {
        conflicts.filterIsInstance<ConflictItem.BankAccountConflict>()
    }
    val ownerAccountConflicts = remember(conflicts) {
        conflicts.filterIsInstance<ConflictItem.OwnerAccountConflict>()
    }
    val transactionConflicts = remember(conflicts) {
        conflicts.filterIsInstance<ConflictItem.TransactionConflict>()
    }
    val mealConflicts = remember(conflicts) {
        conflicts.filterIsInstance<ConflictItem.MealConflict>()
    }
    val menuItemConflicts = remember(conflicts) {
        conflicts.filterIsInstance<ConflictItem.MenuItemConflict>()
    }
    val meterReadingConflicts = remember(conflicts) {
        conflicts.filterIsInstance<ConflictItem.MeterReadingConflict>()
    }
    val utilityBillConflicts = remember(conflicts) {
        conflicts.filterIsInstance<ConflictItem.UtilityBillConflict>()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = ScreenBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScreenBackground)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Conflict Resolution Hub",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                "${conflicts.size} conflict${if (conflicts.size > 1) "s" else ""} detected",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AccentGold
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            enabled = !isMerging,
                            modifier = Modifier
                                .size(40.dp)
                                .background(com.example.ui.components.getGlassIconButtonBg(), androidx.compose.foundation.shape.CircleShape)
                                .testTag("resolution_back_button")
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ScreenBackground
                    )
                )

                // Quick Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onResolveAllLatest,
                        enabled = !isMerging,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan.copy(alpha = 0.15f),
                            contentColor = AccentCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_take_all_latest"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Take All Latest",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onResolveAllLocal,
                        enabled = !isMerging,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardBackground,
                            contentColor = TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_keep_all_local"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Keep All Local",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }

                HorizontalDivider(
                    color = CardBorderColor.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBackground,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Summary pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(label = "$takeImportedCount Update", color = AccentCyan)
                            StatusBadge(label = "$keepLocalCount Keep", color = TextSecondary)
                            if (keepBothCount > 0) {
                                StatusBadge(label = "$keepBothCount New", color = AccentPurple)
                            }
                        }

                        Text(
                            "Total: ${conflicts.size}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Button(
                        onClick = {
                            if (takeImportedCount > 0) {
                                showOverwriteConfirmDialog = true
                            } else {
                                onConfirmMerge()
                            }
                        },
                        enabled = !isMerging,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_confirm_merge"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        if (isMerging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF0F172A),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Committing Changes...",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Apply & Commit Sync (${conflicts.size} items)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            // 0A. Hotel Conflicts
            if (hotelConflicts.isNotEmpty()) {
                item {
                    ConflictCategoryHeader(
                        title = "Hotels & Stays",
                        count = hotelConflicts.size,
                        icon = Icons.Default.Apartment,
                        color = AccentCyan
                    )
                }
                items(hotelConflicts, key = { it.conflictKey }) { conflict ->
                    val decision = conflictDecisions[conflict.conflictKey] ?: ConflictResolution.KEEP_LOCAL
                    ParentEntityConflictCard(
                        title = conflict.title,
                        subtitle = conflict.subtitle,
                        localHeader = "LOCAL HOTEL",
                        localPrimary = conflict.localHotel.name,
                        localSecondary = listOfNotNull(
                            conflict.localHotel.location.ifBlank { null },
                            conflict.localHotel.upiId.ifBlank { null },
                            "Adv: ₹${conflict.localHotel.advancedPayment}"
                        ).joinToString(" • "),
                        incomingHeader = "INCOMING HOTEL",
                        incomingPrimary = conflict.importedHotel.name,
                        incomingSecondary = listOfNotNull(
                            conflict.importedHotel.location.ifBlank { null },
                            conflict.importedHotel.upiId.ifBlank { null },
                            "Adv: ₹${conflict.importedHotel.advancedPayment}"
                        ).joinToString(" • "),
                        currentDecision = decision,
                        onDecisionChanged = { res -> onResolutionChanged(conflict.conflictKey, res) }
                    )
                }
            }

            // 0B. Bank Account Conflicts
            if (bankAccountConflicts.isNotEmpty()) {
                item {
                    ConflictCategoryHeader(
                        title = "Bank Accounts",
                        count = bankAccountConflicts.size,
                        icon = Icons.Default.AccountBalance,
                        color = AccentGreen
                    )
                }
                items(bankAccountConflicts, key = { it.conflictKey }) { conflict ->
                    val decision = conflictDecisions[conflict.conflictKey] ?: ConflictResolution.KEEP_LOCAL
                    ParentEntityConflictCard(
                        title = conflict.title,
                        subtitle = conflict.subtitle,
                        localHeader = "LOCAL ACCOUNT",
                        localPrimary = conflict.localBankAccount.name,
                        localSecondary = listOfNotNull(
                            "Bal: ₹${conflict.localBankAccount.balance}",
                            conflict.localBankAccount.upiId.ifBlank { null },
                            conflict.localBankAccount.ifsc.ifBlank { null }
                        ).joinToString(" • "),
                        incomingHeader = "INCOMING ACCOUNT",
                        incomingPrimary = conflict.importedBankAccount.name,
                        incomingSecondary = listOfNotNull(
                            "Bal: ₹${conflict.importedBankAccount.balance}",
                            conflict.importedBankAccount.upiId.ifBlank { null },
                            conflict.importedBankAccount.ifsc.ifBlank { null }
                        ).joinToString(" • "),
                        currentDecision = decision,
                        onDecisionChanged = { res -> onResolutionChanged(conflict.conflictKey, res) }
                    )
                }
            }

            // 0C. Owner Account Conflicts
            if (ownerAccountConflicts.isNotEmpty()) {
                item {
                    ConflictCategoryHeader(
                        title = "Owner Profile Accounts",
                        count = ownerAccountConflicts.size,
                        icon = Icons.Default.Person,
                        color = AccentPurple
                    )
                }
                items(ownerAccountConflicts, key = { it.conflictKey }) { conflict ->
                    val decision = conflictDecisions[conflict.conflictKey] ?: ConflictResolution.KEEP_LOCAL
                    ParentEntityConflictCard(
                        title = conflict.title,
                        subtitle = conflict.subtitle,
                        localHeader = "LOCAL OWNER",
                        localPrimary = conflict.localOwnerAccount.accountName.ifBlank { conflict.localOwnerAccount.ownerName },
                        localSecondary = listOfNotNull(
                            conflict.localOwnerAccount.ownerName.ifBlank { null },
                            conflict.localOwnerAccount.upiId.ifBlank { null },
                            conflict.localOwnerAccount.accountNumber.ifBlank { null }
                        ).joinToString(" • "),
                        incomingHeader = "INCOMING OWNER",
                        incomingPrimary = conflict.importedOwnerAccount.accountName.ifBlank { conflict.importedOwnerAccount.ownerName },
                        incomingSecondary = listOfNotNull(
                            conflict.importedOwnerAccount.ownerName.ifBlank { null },
                            conflict.importedOwnerAccount.upiId.ifBlank { null },
                            conflict.importedOwnerAccount.accountNumber.ifBlank { null }
                        ).joinToString(" • "),
                        currentDecision = decision,
                        onDecisionChanged = { res -> onResolutionChanged(conflict.conflictKey, res) }
                    )
                }
            }

            // 1. Transaction Conflicts
            if (transactionConflicts.isNotEmpty()) {
                item {
                    ConflictCategoryHeader(
                        title = "Wallet Transactions & Expenses",
                        count = transactionConflicts.size,
                        icon = Icons.Default.AccountBalanceWallet,
                        color = AccentCyan
                    )
                }
                items(transactionConflicts, key = { it.conflictKey }) { conflict ->
                    val decision = conflictDecisions[conflict.conflictKey] ?: ConflictResolution.KEEP_LOCAL
                    TransactionConflictCard(
                        conflict = conflict,
                        currentDecision = decision,
                        dateFormat = dateFormat,
                        onDecisionChanged = { res -> onResolutionChanged(conflict.conflictKey, res) }
                    )
                }
            }

            // 2. Meal Conflicts
            if (mealConflicts.isNotEmpty()) {
                item {
                    ConflictCategoryHeader(
                        title = "Hotel Meal Orders",
                        count = mealConflicts.size,
                        icon = Icons.Default.Restaurant,
                        color = AccentGold
                    )
                }
                items(mealConflicts, key = { it.conflictKey }) { conflict ->
                    val decision = conflictDecisions[conflict.conflictKey] ?: ConflictResolution.KEEP_LOCAL
                    MealConflictCard(
                        conflict = conflict,
                        currentDecision = decision,
                        dateFormat = dateFormat,
                        onDecisionChanged = { res -> onResolutionChanged(conflict.conflictKey, res) }
                    )
                }
            }

            // 3. Menu Item Conflicts
            if (menuItemConflicts.isNotEmpty()) {
                item {
                    ConflictCategoryHeader(
                        title = "Menu Items & Price Overrides",
                        count = menuItemConflicts.size,
                        icon = Icons.Default.RestaurantMenu,
                        color = AccentPurple
                    )
                }
                items(menuItemConflicts, key = { it.conflictKey }) { conflict ->
                    val decision = conflictDecisions[conflict.conflictKey] ?: ConflictResolution.KEEP_LOCAL
                    MenuItemConflictCard(
                        conflict = conflict,
                        currentDecision = decision,
                        onDecisionChanged = { res -> onResolutionChanged(conflict.conflictKey, res) }
                    )
                }
            }

            // 4. Meter Reading Conflicts
            if (meterReadingConflicts.isNotEmpty()) {
                item {
                    ConflictCategoryHeader(
                        title = "Meter Readings",
                        count = meterReadingConflicts.size,
                        icon = Icons.Default.ElectricMeter,
                        color = AccentGreen
                    )
                }
                items(meterReadingConflicts, key = { it.conflictKey }) { conflict ->
                    val decision = conflictDecisions[conflict.conflictKey] ?: ConflictResolution.KEEP_LOCAL
                    MeterReadingConflictCard(
                        conflict = conflict,
                        currentDecision = decision,
                        dateFormat = dateFormat,
                        onDecisionChanged = { res -> onResolutionChanged(conflict.conflictKey, res) }
                    )
                }
            }

            // 5. Utility Bill Conflicts
            if (utilityBillConflicts.isNotEmpty()) {
                item {
                    ConflictCategoryHeader(
                        title = "Utility Bills",
                        count = utilityBillConflicts.size,
                        icon = Icons.Default.ReceiptLong,
                        color = AccentRed
                    )
                }
                items(utilityBillConflicts, key = { it.conflictKey }) { conflict ->
                    val decision = conflictDecisions[conflict.conflictKey] ?: ConflictResolution.KEEP_LOCAL
                    UtilityBillConflictCard(
                        conflict = conflict,
                        currentDecision = decision,
                        dateFormat = dateFormat,
                        onDecisionChanged = { res -> onResolutionChanged(conflict.conflictKey, res) }
                    )
                }
            }
        }
    }

    if (showOverwriteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showOverwriteConfirmDialog = false },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AccentGold
                )
            },
            title = {
                Text(
                    text = "Confirm Overwrite ($takeImportedCount)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You selected 'Take Incoming' for $takeImportedCount conflicting record(s). " +
                        "Applying this sync will overwrite and replace your existing local records for those items.\n\n" +
                        "Keep Local ($keepLocalCount) and Keep Both ($keepBothCount) items will not remove existing data.",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverwriteConfirmDialog = false
                        onConfirmMerge()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_sync_overwrite_btn")
                ) {
                    Text("Overwrite & Sync", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showOverwriteConfirmDialog = false }
                ) {
                    Text("Review Again", color = AccentCyan)
                }
            }
        )
    }
}

@Composable
private fun ConflictCategoryHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                "$count item${if (count > 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
    }
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        )
    }
}

// =================================================================================
// CONFLICT CARDS WITH SIDE-BY-SIDE COMPARISON & OVERRIDE RADIO BUTTONS
// =================================================================================

@Composable
private fun TransactionConflictCard(
    conflict: ConflictItem.TransactionConflict,
    currentDecision: ConflictResolution,
    dateFormat: SimpleDateFormat,
    onDecisionChanged: (ConflictResolution) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        conflict.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        conflict.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                if (conflict.isImportedNewer) {
                    Box(
                        modifier = Modifier
                            .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Incoming is Newer",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Side by Side Comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Local Item
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(LocalCardBackground, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (currentDecision == ConflictResolution.KEEP_LOCAL) AccentGold.copy(alpha = 0.6f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "CURRENT (LOCAL)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", conflict.localExpense.amount)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            conflict.localExpense.category,
                            style = MaterialTheme.typography.bodySmall.copy(color = AccentGold)
                        )
                        if (!conflict.localExpense.notes.isNullOrBlank()) {
                            Text(
                                conflict.localExpense.notes ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            dateFormat.format(Date(conflict.localExpense.timestamp)),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }

                // Imported Item
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(ImportedCardBackground, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (currentDecision == ConflictResolution.TAKE_IMPORTED) AccentCyan.copy(alpha = 0.6f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "INCOMING (IMPORT)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan
                                )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", conflict.importedExpense.amount)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            conflict.importedExpense.category,
                            style = MaterialTheme.typography.bodySmall.copy(color = AccentCyan)
                        )
                        if (!conflict.importedExpense.notes.isNullOrBlank()) {
                            Text(
                                conflict.importedExpense.notes ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            dateFormat.format(Date(conflict.importedExpense.timestamp)),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Resolution Overrides
            ResolutionChoiceSelector(
                currentDecision = currentDecision,
                allowKeepBoth = true,
                onSelect = onDecisionChanged
            )
        }
    }
}

@Composable
private fun MealConflictCard(
    conflict: ConflictItem.MealConflict,
    currentDecision: ConflictResolution,
    dateFormat: SimpleDateFormat,
    onDecisionChanged: (ConflictResolution) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        conflict.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        "${conflict.hotelName} • ${conflict.subtitle}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                if (conflict.isImportedNewer) {
                    Box(
                        modifier = Modifier
                            .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Incoming is Newer",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Local
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(LocalCardBackground, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (currentDecision == ConflictResolution.KEEP_LOCAL) AccentGold.copy(alpha = 0.6f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "CURRENT (LOCAL)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", conflict.localMeal.totalAmount)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            conflict.localItemsSummary.ifBlank { "No items detail" },
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            dateFormat.format(Date(conflict.localMeal.timestamp)),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }

                // Imported
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(ImportedCardBackground, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (currentDecision == ConflictResolution.TAKE_IMPORTED) AccentCyan.copy(alpha = 0.6f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "INCOMING (IMPORT)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", conflict.importedMeal.totalAmount)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            conflict.importedItemsSummary.ifBlank { "No items detail" },
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            dateFormat.format(Date(conflict.importedMeal.timestamp)),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            ResolutionChoiceSelector(
                currentDecision = currentDecision,
                allowKeepBoth = true,
                onSelect = onDecisionChanged
            )
        }
    }
}

@Composable
private fun MenuItemConflictCard(
    conflict: ConflictItem.MenuItemConflict,
    currentDecision: ConflictResolution,
    onDecisionChanged: (ConflictResolution) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                conflict.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                "${conflict.hotelName} • Price discrepancy detected",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Local
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(LocalCardBackground, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (currentDecision == ConflictResolution.KEEP_LOCAL) AccentGold.copy(alpha = 0.6f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "LOCAL MENU",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextMuted)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", conflict.localMenuItem.price)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            conflict.localMenuItem.englishName ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            maxLines = 1
                        )
                    }
                }

                // Imported
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(ImportedCardBackground, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (currentDecision == ConflictResolution.TAKE_IMPORTED) AccentCyan.copy(alpha = 0.6f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "INCOMING MENU",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AccentCyan)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", conflict.importedMenuItem.price)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            conflict.importedMenuItem.englishName ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            ResolutionChoiceSelector(
                currentDecision = currentDecision,
                allowKeepBoth = false,
                onSelect = onDecisionChanged
            )
        }
    }
}

@Composable
private fun MeterReadingConflictCard(
    conflict: ConflictItem.MeterReadingConflict,
    currentDecision: ConflictResolution,
    dateFormat: SimpleDateFormat,
    onDecisionChanged: (ConflictResolution) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                conflict.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                conflict.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Local
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(LocalCardBackground, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "LOCAL READING",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextMuted)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${conflict.localReading.totalReading} Units",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            "Consumed: ${conflict.localReading.unitsConsumed}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            dateFormat.format(Date(conflict.localReading.readingDate)),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = (if (conflict.localReading.isPaid) AccentGreen else AccentRed).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (conflict.localReading.isPaid) "PAID" else "UNPAID",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (conflict.localReading.isPaid) AccentGreen else AccentRed,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Imported
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(ImportedCardBackground, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "INCOMING READING",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AccentCyan)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${conflict.importedReading.totalReading} Units",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            "Consumed: ${conflict.importedReading.unitsConsumed}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            dateFormat.format(Date(conflict.importedReading.readingDate)),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = (if (conflict.importedReading.isPaid) AccentGreen else AccentRed).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (conflict.importedReading.isPaid) "PAID" else "UNPAID",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (conflict.importedReading.isPaid) AccentGreen else AccentRed,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            ResolutionChoiceSelector(
                currentDecision = currentDecision,
                allowKeepBoth = true,
                onSelect = onDecisionChanged
            )
        }
    }
}

@Composable
private fun UtilityBillConflictCard(
    conflict: ConflictItem.UtilityBillConflict,
    currentDecision: ConflictResolution,
    dateFormat: SimpleDateFormat,
    onDecisionChanged: (ConflictResolution) -> Unit
) {
    fun billStatusLabel(bill: com.example.data.model.UtilityBill): Pair<String, Color> {
        val paidAmt = bill.transactionId?.removePrefix("PAID_AMT:")?.toDoubleOrNull()
        return when {
            bill.isPaid -> "PAID" to AccentGreen
            paidAmt != null && paidAmt > 0.0 -> {
                val due = (bill.calculatedAmount - paidAmt).coerceAtLeast(0.0)
                "PARTIAL (Paid ₹${paidAmt.toInt()} • Due ₹${due.toInt()})" to AccentGold
            }
            else -> "UNPAID" to AccentRed
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                conflict.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                conflict.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            Spacer(Modifier.height(12.dp))

            val (localStatusText, localStatusColor) = billStatusLabel(conflict.localBill)
            val (impStatusText, impStatusColor) = billStatusLabel(conflict.importedBill)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Local
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(LocalCardBackground, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "LOCAL BILL",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextMuted)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", conflict.localBill.calculatedAmount)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = localStatusColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = localStatusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = localStatusColor,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (conflict.localBill.note.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = conflict.localBill.note,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Imported
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(ImportedCardBackground, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "INCOMING BILL",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AccentCyan)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", conflict.importedBill.calculatedAmount)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = impStatusColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = impStatusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = impStatusColor,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (conflict.importedBill.note.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = conflict.importedBill.note,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            ResolutionChoiceSelector(
                currentDecision = currentDecision,
                allowKeepBoth = true,
                onSelect = onDecisionChanged
            )
        }
    }
}

@Composable
private fun ResolutionChoiceSelector(
    currentDecision: ConflictResolution,
    allowKeepBoth: Boolean,
    onSelect: (ConflictResolution) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
            .border(1.dp, CardBorderColor, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Keep Local Option
        ChoiceButton(
            title = "Keep Local",
            isSelected = currentDecision == ConflictResolution.KEEP_LOCAL,
            selectedColor = AccentGold,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(ConflictResolution.KEEP_LOCAL) }
        )

        // Take Imported Option
        ChoiceButton(
            title = "Take Incoming",
            isSelected = currentDecision == ConflictResolution.TAKE_IMPORTED,
            selectedColor = AccentCyan,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(ConflictResolution.TAKE_IMPORTED) }
        )

        // Keep Both Option
        if (allowKeepBoth) {
            ChoiceButton(
                title = "Keep Both",
                isSelected = currentDecision == ConflictResolution.KEEP_BOTH,
                selectedColor = AccentPurple,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(ConflictResolution.KEEP_BOTH) }
            )
        }
    }
}

@Composable
private fun ParentEntityConflictCard(
    title: String,
    subtitle: String,
    localHeader: String,
    localPrimary: String,
    localSecondary: String,
    incomingHeader: String,
    incomingPrimary: String,
    incomingSecondary: String,
    currentDecision: ConflictResolution,
    onDecisionChanged: (ConflictResolution) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(LocalCardBackground, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (currentDecision == ConflictResolution.KEEP_LOCAL) AccentGold.copy(alpha = 0.6f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            localHeader,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextMuted)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            localPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (localSecondary.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                localSecondary,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(ImportedCardBackground, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (currentDecision == ConflictResolution.TAKE_IMPORTED) AccentCyan.copy(alpha = 0.6f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            incomingHeader,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AccentCyan)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            incomingPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (incomingSecondary.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                incomingSecondary,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            ResolutionChoiceSelector(
                currentDecision = currentDecision,
                allowKeepBoth = true,
                onSelect = onDecisionChanged
            )
        }
    }
}

@Composable
private fun ChoiceButton(
    title: String,
    isSelected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) selectedColor.copy(alpha = 0.2f) else Color.Transparent
            )
            .border(
                1.dp,
                if (isSelected) selectedColor.copy(alpha = 0.7f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) selectedColor else TextSecondary
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
