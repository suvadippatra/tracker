package com.example.ui.screens
import com.example.ui.components.*
import com.example.ui.components.SlidingDatePickerModal
import com.example.ui.components.InlineDatePicker
import androidx.compose.foundation.Canvas
import kotlin.random.Random
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.viewmodel.MeterReadingViewModel
import com.example.ui.viewmodel.MeterReadingUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


// ==========================================
// MODULE B: EXTRA EXPENSES TAB
// ==========================================
@Composable
fun ExtraExpensesTab(viewModel: FinancialViewModel) {
    val accounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val expenses by viewModel.allExtraExpenses.collectAsStateWithLifecycle()
    val userCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val activeBankAccountId by viewModel.activeBankAccountId.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile?.language ?: "en"

    var showAddExpense by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<com.example.data.model.BankAccountEntity?>(null) }

    if (showAddAccountDialog) {
        com.example.ui.screens.UnifiedAccountDialog(
            account = null,
            prefLanguage = prefLanguage,
            accountsCount = accounts.size,
            onDismiss = { showAddAccountDialog = false },
            onSave = { name, balance, upiId, qrCodeUri, accountNumber, ifsc, branchName ->
                viewModel.addVirtualAccount(name, balance, upiId, qrCodeUri, accountNumber, ifsc, branchName)
                showAddAccountDialog = false
            },
            onDelete = null
        )
    }

    if (editingAccount != null) {
        com.example.ui.screens.UnifiedAccountDialog(
            account = editingAccount,
            prefLanguage = prefLanguage,
            accountsCount = accounts.size,
            onDismiss = { editingAccount = null },
            onSave = { name, balance, upiId, qrCodeUri, accountNumber, ifsc, branchName ->
                viewModel.editVirtualAccount(editingAccount!!.copy(name = name, balance = balance, upiId = upiId, qrCodeUri = qrCodeUri, accountNumber = accountNumber, ifsc = ifsc, branchName = branchName))
                editingAccount = null
            },
            onDelete = {
                viewModel.deleteVirtualAccount(editingAccount!!)
                editingAccount = null
            }
        )
    }
    var selectedExpenseIdForAttachments by remember { mutableStateOf<Int?>(null) }
    var showDetailedHistory by remember { mutableStateOf(false) }

    val quickActionTrigger by viewModel.quickActionTrigger.collectAsStateWithLifecycle()
    LaunchedEffect(quickActionTrigger) {
        if (quickActionTrigger == "ADD_EXPENSE") {
            showAddExpense = true
            viewModel.clearQuickActionTrigger()
        } else if (quickActionTrigger == "ADD_ACCOUNT") {
            showAddAccountDialog = true
            viewModel.clearQuickActionTrigger()
        }
    }
    
    // Interactive Category Filter & Search
    var searchQuery by remember { mutableStateOf("") }
    val systemCategories = listOf("All", "Food", "Academics", "Travel", "Salary", "Shopping", "Entertainment", "General")
    val allCategoryNames = remember(userCategories) {
        (systemCategories + userCategories.map { it.name }).distinct()
    }
    var activeCategoryFilter by remember { mutableStateOf("All") }

    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    if (accounts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EmptyStateCard(message = "No Bank Accounts configured. Create one here or in Settings!")
                GlassButton(onClick = { showAddAccountDialog = true }) {
                    Text("Add Account")
                }
            }
        }
        return
    }

    val initialPage = remember(accounts, activeBankAccountId) {
        accounts.indexOfFirst { it.id == activeBankAccountId }.coerceAtLeast(0)
    }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { accounts.size }
    )

    val selectedAccount = accounts.getOrNull(pagerState.currentPage)

    // Asynchronous Calculations using remember & derivedStateOf
    val totalBalance by remember(accounts) {
        derivedStateOf { accounts.sumOf { it.balance } }
    }
    val totalIncome by remember(expenses, selectedAccount) {
        derivedStateOf {
            expenses.filter { it.bankAccountId == selectedAccount?.id && it.type == "INCOME" }.sumOf { it.amount }
        }
    }
    val totalExpense by remember(expenses, selectedAccount) {
        derivedStateOf {
            expenses.filter { it.bankAccountId == selectedAccount?.id && it.type == "EXPENSE" }.sumOf { it.amount }
        }
    }

    val filteredExpenses by remember(expenses, activeCategoryFilter, searchQuery, selectedAccount) {
        derivedStateOf {
            expenses.filter { exp ->
                val matchesAccount = exp.bankAccountId == selectedAccount?.id
                val matchesCategory = if (activeCategoryFilter == "All") true 
                                     else exp.category.lowercase() == activeCategoryFilter.lowercase()
                val matchesSearch = if (searchQuery.isBlank()) true 
                                   else exp.name.contains(searchQuery, ignoreCase = true) || 
                                        exp.notes.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch && matchesAccount
            }
        }
    }

    LaunchedEffect(showDetailedHistory) {
        viewModel.registerSubScreen("DetailedHistory", showDetailedHistory)
    }
    LaunchedEffect(showAddExpense) {
        viewModel.registerSubScreen("AddExpense", showAddExpense)
    }

    if (showDetailedHistory) {
        ExtraExpenseDetailedHistoryScreen(
            viewModel = viewModel,
            onDismiss = { showDetailedHistory = false }
        )
    } else if (showAddExpense) {
        AddTransactionScreen(
            viewModel = viewModel,
            onDismiss = { showAddExpense = false }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
                    .padding(horizontal = 12.dp)
            ) {
                item {
                    // Hero Balance Card with split Income and Expense indications
                    // Move this to top as requested
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 12.dp)
                            .clickable { showDetailedHistory = true } // Open detailed history on click
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp
                        ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Available Balance",
                            color = AppTextColor().copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RollingNumberText(
                            value = totalBalance,
                            prefix = "₹",
                            style = TextStyle(
                                color = AppTextColor(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = AppTextColor().copy(alpha = 0.1f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Total Income Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0x1F00E676), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Income",
                                        tint = AccentGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Income", color = AppTextColor().copy(alpha = 0.5f), fontSize = 10.sp)
                                    RollingNumberText(
                                        value = totalIncome,
                                        prefix = "₹",
                                        style = TextStyle(
                                            color = AccentGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Divider Line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(AppTextColor().copy(alpha = 0.1f))
                            )

                            // Total Expense Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0x1FFF4081), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Expense",
                                        tint = AccentPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Expense", color = AppTextColor().copy(alpha = 0.5f), fontSize = 10.sp)
                                    RollingNumberText(
                                        value = totalExpense,
                                        prefix = "₹",
                                        style = TextStyle(
                                            color = AccentPink,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // ACCOUNT BANKING METALLIC CAROUSEL
            Text(
                text = "Accounts",
                color = AppTextColor().copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (accounts.isEmpty()) {
                EmptyStateCard(message = "No accounts set. Add one in Settings!")
            } else {
                val view = androidx.compose.ui.platform.LocalView.current
                LaunchedEffect(pagerState, accounts) {
                    androidx.compose.runtime.snapshotFlow { pagerState.currentPage }.collect { page ->
                        if (page in accounts.indices) {
                            if (viewModel.activeBankAccountId.value != accounts[page].id) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                viewModel.activeBankAccountId.value = accounts[page].id
                            }
                        }
                    }
                }
                
                LaunchedEffect(activeBankAccountId, accounts) {
                    val index = accounts.indexOfFirst { it.id == activeBankAccountId }
                    if (index >= 0 && index != pagerState.currentPage) {
                        pagerState.animateScrollToPage(index)
                    }
                }
                
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    userScrollEnabled = accounts.size > 1
                ) { page ->
                    val account = accounts[page]
                    var showAccountMenu by remember { mutableStateOf(false) }
                    var showQRDialog by remember { mutableStateOf(false) }
                    var showFullScreenQR by remember { mutableStateOf(false) }
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    if (showDeleteDialog) {
                        com.example.ui.components.Tier2DeleteDialog(
                            itemTitle = account.name,
                            onConfirm = { viewModel.deleteVirtualAccount(account) },
                            onDismiss = { showDeleteDialog = false }
                        )
                    }

                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val scale = 1f - (0.05f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                    val alpha = 1f - (0.3f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            cornerRadius = 22.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = account.name,
                                            color = AppTextColor(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        if (account.upiId.isNotBlank()) {
                                            Text(
                                                text = account.upiId,
                                                color = AppTextColor().copy(alpha = 0.5f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { showQRDialog = true }) {
                                            Icon(Icons.Default.Info, contentDescription = "Account Info", tint = AccentCyan)
                                        }
                                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                            IconButton(onClick = { showAccountMenu = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = AppTextColor())
                                            }
                                            DropdownMenu(
                                                expanded = showAccountMenu,
                                                onDismissRequest = { showAccountMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Edit this account", color = AccentCyan) },
                                                    onClick = {
                                                        showAccountMenu = false
                                                        editingAccount = account
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = AccentCyan) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete", color = Color.Red) },
                                                    onClick = {
                                                        showAccountMenu = false
                                                        showDeleteDialog = true
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                                                )
                                            }
                                        }
                                    }
                                }

                                Column {
                                    Text(
                                        text = "Current Balance",
                                        color = AppTextColor().copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                    RollingNumberText(
                                        value = account.balance,
                                        prefix = "₹",
                                        style = TextStyle(
                                            color = AppTextColor(),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                            }
                        }
                        if (showQRDialog) {
                            Dialog(onDismissRequest = { showQRDialog = false }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .padding(16.dp)
                                        .background(getDialogBackground(), RoundedCornerShape(24.dp))
                                        .border(BorderStroke(1.dp, getGlassBorder()), RoundedCornerShape(24.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Account Details", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppTextColor())
                                        Spacer(modifier = Modifier.height(16.dp))
                                        if (account.accountNumber.isNotBlank()) {
                                            Text("A/C No: ${account.accountNumber}", color = AppTextColor(), fontSize = 14.sp)
                                        }
                                        if (account.ifsc.isNotBlank()) {
                                            Text("IFSC: ${account.ifsc}", color = AppTextColor(), fontSize = 14.sp)
                                        }
                                        if (account.branchName.isNotBlank()) {
                                            Text("Branch: ${account.branchName}", color = AppTextColor(), fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                        if (account.qrCodeUri.isNotBlank()) {
                                            AsyncImage(
                                                model = account.qrCodeUri,
                                                contentDescription = "QR Code",
                                                modifier = Modifier
                                                    .size(200.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { showFullScreenQR = true }
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                        if (account.upiId.isNotBlank()) {
                                            Text("UPI ID: ${account.upiId}", color = AppTextColor(), fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(20.dp))
                                        }
                                        GlassButton(onClick = { showQRDialog = false }) {
                                            Text("Close")
                                        }
                                    }
                                }
                            }
                        }

                        if (showFullScreenQR) {
                            com.example.ui.components.FullScreenQRPreviewDialog(
                                qrUri = account.qrCodeUri,
                                title = account.name,
                                upiId = account.upiId,
                                onDismiss = { showFullScreenQR = false }
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            // Horizontal Scrollable Category Chips for Quick Filter
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategoryNames, key = { it }) { cat ->
                    val isSelected = activeCategoryFilter == cat
                    val chipBg = if (isSelected) AccentCyan.copy(alpha = 0.15f) else Color.Transparent
                    val chipBorder = if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.2f)
                    val chipText = if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.6f)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(50))
                            .clickable { activeCategoryFilter = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            color = chipText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        val displayCount = if (filteredExpenses.size >= 5) 4 else filteredExpenses.size
        val itemsToDisplay = filteredExpenses.take(displayCount)

        if (filteredExpenses.isEmpty()) {
            item {
                EmptyStateCard(message = if (activeCategoryFilter == "All") "No transactions registered yet." else "No transactions found in category '$activeCategoryFilter'")
            }
        } else {
            items(itemsToDisplay, key = { it.id }) { exp ->
                val sourceAccountName = accounts.find { it.id == exp.bankAccountId }?.name ?: "Unknown Account"
                val (categoryIcon, categoryColor) = getCategoryIconAndColor(exp.category, exp.type)

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    cornerRadius = 18.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Category Icon wrapped in a circular tonal container
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(categoryColor.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, categoryColor.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIcon,
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = exp.name,
                                    color = AppTextColor(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    val tagBg = if (exp.type == "INCOME") Color(0x1F00E676) else Color(0x1FFF4081)
                                    val tagText = if (exp.type == "INCOME") AccentGreen else AccentPink
                                    Box(
                                        modifier = Modifier
                                            .background(tagBg, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = exp.category,
                                            color = tagText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sourceAccountName,
                                        color = AccentCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = " • ${sdf.format(Date(exp.timestamp))}",
                                        color = AppTextColor().copy(alpha = 0.4f),
                                        fontSize = 10.sp
                                    )
                                }

                                // Display Custom Notes if present
                                if (exp.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = exp.notes,
                                        color = AppTextColor().copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                                
                                // Always show attachment icon if there are attachments
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        selectedExpenseIdForAttachments = exp.id
                                    }
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = AccentGold, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "View Attachments",
                                        color = AccentGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            val amountColor = if (exp.type == "INCOME") AccentGreen else AccentPink
                            val amountPrefix = if (exp.type == "INCOME") "+₹" else "-₹"

                            RollingNumberText(
                                value = exp.amount,
                                prefix = amountPrefix,
                                style = TextStyle(
                                    color = amountColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            var showExpMenu by remember { mutableStateOf(false) }
                            var showDeleteDialog by remember { mutableStateOf(false) }

                            if (showDeleteDialog) {
                                com.example.ui.components.Tier1DeleteDialog(
                                    itemTitle = exp.name,
                                    onConfirm = { viewModel.deleteExtraExpense(exp) },
                                    onDismiss = { showDeleteDialog = false }
                                )
                            }
                            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                IconButton(
                                    onClick = { showExpMenu = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = AppTextColor().copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showExpMenu,
                                    onDismissRequest = { showExpMenu = false },
                                    modifier = Modifier.background(getDialogBackground())
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = AccentPink) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = AccentPink, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showExpMenu = false
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (expenses.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassButton(
                        onClick = { showDetailedHistory = true },
                        modifier = Modifier.fillMaxWidth().testTag("view_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = AccentCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (userProfile.language == "bn") "ইতিহাস দেখুন" else "View History",
                            color = AppTextColor(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    // Bottom-end floating button
    if (accounts.isNotEmpty()) {
        FloatingActionButton(
            onClick = { showAddExpense = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 16.dp)
                .bounceClick()
                .testTag("add_expense_fab"),
            shape = CircleShape,
            containerColor = Color.Transparent,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(if (isAppLightMode()) Color.White else Color(0xFF0F0B26), shape = CircleShape)
                    .border(2.dp, AccentCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    tint = AccentCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // Attachment Viewer modal
    if (selectedExpenseIdForAttachments != null) {
        AttachmentViewerDialog(
            expenseId = selectedExpenseIdForAttachments!!,
            viewModel = viewModel,
            onDismiss = { selectedExpenseIdForAttachments = null }
        )
    }
}
}
}


@Composable
fun AttachmentViewerDialog(
    expenseId: Int,
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val attachments by viewModel.getAttachmentsForExpense(expenseId).collectAsState(initial = emptyList())
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier
            .padding(24.dp)
            .background(getDialogBackground(), RoundedCornerShape(24.dp))
            .border(0.5.dp, getGlassBorder(), RoundedCornerShape(24.dp)),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Attachments", color = AppTextColor(), fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = AppTextColor())
                }
            }
        },
        text = {
            if (attachments.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No attachments found", color = AppTextColor().copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(attachments, key = { it.uri }) { att ->
                        AttachmentItem(att, context)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentCyan)
            }
        }
    )
}


@Composable
fun AttachmentItem(attachment: AttachmentEntity, context: android.content.Context) {
    var showImageViewer by remember { mutableStateOf(false) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    val mediaPlayer = remember { android.media.MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    val (icon, color) = when (attachment.type) {
        "IMAGE" -> Icons.Default.Image to Color(0xFF64B5F6)
        "PDF" -> Icons.Default.PictureAsPdf to Color(0xFFF06292)
        "VOICE" -> if (isPlayingAudio) Icons.Default.Stop to Color(0xFFE57373) else Icons.Default.PlayArrow to Color(0xFF4DB6AC)
        else -> Icons.Default.AttachFile to Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(getDialogBackground().copy(alpha = 0.5f))
            .border(0.5.dp, getGlassBorder(), RoundedCornerShape(12.dp))
            .clickable {
                try {
                    val uri = Uri.parse(attachment.uri)
                    if (attachment.type == "IMAGE") {
                        showImageViewer = true
                    } else if (attachment.type == "VOICE") {
                        if (isPlayingAudio) {
                            mediaPlayer.stop()
                            isPlayingAudio = false
                        } else {
                            mediaPlayer.reset()
                            mediaPlayer.setDataSource(context, uri)
                            mediaPlayer.prepare()
                            mediaPlayer.start()
                            isPlayingAudio = true
                            mediaPlayer.setOnCompletionListener {
                                isPlayingAudio = false
                            }
                        }
                    } else {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, when(attachment.type) {
                                "PDF" -> "application/pdf"
                                else -> "*/*"
                            })
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Could not open attachment", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(attachment.uri.substringAfterLast("/"), color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (attachment.type == "VOICE" && isPlayingAudio) "PLAYING..." else attachment.type, color = color.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }

    if (showImageViewer) {
        Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { showImageViewer = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = "Image Viewer",
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }
    }
}


@Composable
fun ExtraExpenseDetailedHistoryScreen(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val expenses by viewModel.allExtraExpenses.collectAsStateWithLifecycle()
    val accounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Int?>(null) }

    val filteredExpenses by remember(expenses, searchQuery, selectedAccountId) {
        derivedStateOf {
            expenses.filter { exp ->
                val matchesSearch = searchQuery.isBlank() || exp.name.contains(searchQuery, ignoreCase = true) || exp.notes.contains(searchQuery, ignoreCase = true)
                val matchesAccount = selectedAccountId == null || exp.bankAccountId == selectedAccountId
                matchesSearch && matchesAccount
            }
        }
    }

    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Box(modifier = Modifier.fillMaxSize().background(getAppBackgroundGradient(com.example.ui.components.LocalAppTheme.current)).statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Scaffold(
            modifier = Modifier.fillMaxHeight().widthIn(max = 840.dp),
            topBar = {
            Column(modifier = Modifier.background(Color.Transparent).padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.background(getGlassIconButtonBg(), CircleShape).bounceClick()) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTextColor())
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Detailed History",
                        color = AppTextColor(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedAccountId == null,
                            onClick = { selectedAccountId = null },
                            label = { Text("All Accounts") },
                            colors = FilterChipDefaults.filterChipColors(labelColor = AppTextColor(), selectedLabelColor = Color.Black, selectedContainerColor = AccentCyan)
                        )
                    }
                    items(accounts, key = { it.id }) { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) },
                            colors = FilterChipDefaults.filterChipColors(labelColor = AppTextColor(), selectedLabelColor = Color.Black, selectedContainerColor = AccentCyan)
                        )
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            items(filteredExpenses, key = { it.id }) { expense ->
                val account = accounts.find { it.id == expense.bankAccountId }
                TransactionItem(
                    expense = expense,
                    accountName = account?.name ?: "Unknown",
                    onDelete = { viewModel.deleteExtraExpense(expense) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }
}
}

@Composable
fun TransactionItem(
    expense: ExtraExpenseEntity,
    accountName: String,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        com.example.ui.components.ConfirmDeleteDialog(
            itemName = expense.name,
            requireTypedConfirmation = false,
            onConfirm = { onDelete(); showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    val isIncome = expense.type == "INCOME" 
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    
    GlassCard(cornerRadius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isIncome) Color(0x1F00E676) else Color(0x1FFF4081), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (isIncome) AccentGreen else AccentPink,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.name, fontWeight = FontWeight.Bold, color = AppTextColor())
                Text("$accountName • ${expense.category}", fontSize = 10.sp, color = AppTextColor().copy(alpha = 0.5f))
                Text(sdf.format(Date(expense.timestamp)), fontSize = 10.sp, color = AppTextColor().copy(alpha = 0.4f))
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isIncome) "+" else "-"}₹${expense.amount}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (isIncome) AccentGreen else AccentPink
                )
                
                var showMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, null, tint = AppTextColor().copy(alpha = 0.5f))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        )
                    }
                }
            }
        }
    }
}
