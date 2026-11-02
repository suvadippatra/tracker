package com.example.ui.screens
import com.example.utils.Formatters
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraExpensesTab(viewModel: FinancialViewModel) {
    val allBankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val ownerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle()
    val expenses by viewModel.allExtraExpenses.collectAsStateWithLifecycle()
    val userCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val activeBankAccountId by viewModel.activeBankAccountId.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile?.language ?: "en"
    val context = LocalContext.current

    val accounts = remember(allBankAccounts) {
        allBankAccounts.filter { !it.isDeleted }
    }

    var showAddExpense by remember { mutableStateOf(viewModel.quickActionTrigger.value == "ADD_EXPENSE") }
    var showAddAccountDialog by remember { mutableStateOf(viewModel.quickActionTrigger.value == "ADD_ACCOUNT") }
    var showSelectDefaultAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<com.example.data.model.BankAccountEntity?>(null) }
    var activeQrExportData by remember { mutableStateOf<com.example.data.ExportResult.Success?>(null) }
    var qrSessionId by remember { mutableStateOf("") }
    var qrExportItemName by remember { mutableStateOf("") }
    var showQrImportScanner by remember { mutableStateOf(false) }

    if (showSelectDefaultAccountDialog) {
        AlertDialog(
            onDismissRequest = { showSelectDefaultAccountDialog = false },
            containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color(0xFFFFFFFF),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (prefLanguage == "bn") "ডিফল্ট এক্সট্রা অ্যাকাউন্ট নির্বাচন করুন" else "Select Default Extras Account",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val nonDeletedAccounts = accounts.filter { !it.isDeleted }
                    if (nonDeletedAccounts.isEmpty()) {
                        Text(
                            text = if (prefLanguage == "bn") "কোনো অ্যাকাউন্ট পাওয়া যায়নি" else "No ledger accounts found",
                            color = AppSecondaryTextColor(),
                            fontSize = 14.sp
                        )
                    } else {
                        nonDeletedAccounts.forEach { acc ->
                            val isSelected = userProfile?.defaultBankAccountId == acc.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AppAccentColor().copy(alpha = 0.2f) else getCardBackground())
                                    .border(1.dp, if (isSelected) AppAccentColor() else getGlassBorder(), RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.setDefaultBankAccount(acc.id)
                                        showSelectDefaultAccountDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setDefaultBankAccount(acc.id)
                                        showSelectDefaultAccountDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AppAccentColor())
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = acc.name, color = AppTextColor(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Balance: ₹${acc.balance}", color = AccentGreen, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelectDefaultAccountDialog = false }) {
                    Text(if (prefLanguage == "bn") "বন্ধ করুন" else "Close", color = AppAccentColor())
                }
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

    if (accounts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTextColor().copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, getGlassBorder().copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AppAccentColor().copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = AppAccentColor(),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (prefLanguage == "bn") "কোনো অ্যাকাউন্ট যোগ করা হয়নি" else "No Bank Accounts Configured",
                        color = AppTextColor(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (prefLanguage == "bn") "এক্সট্রা খরচের জন্য একটি অ্যাকাউন্ট যোগ করুন" else "Create a ledger account to track extra expenses and income.",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showAddAccountDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor(), contentColor = AppOnAccentColor()),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AppOnAccentColor(), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (prefLanguage == "bn") "+ এক্সট্রা অ্যাকাউন্ট যোগ করুন" else "+ Add Extras Account",
                            color = AppOnAccentColor(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showQrImportScanner = true },
                        border = BorderStroke(1.dp, AppAccentColor().copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = AppAccentColor())
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (prefLanguage == "bn") "কিউআর (QR) এর মাধ্যমে ইমপোর্ট করুন" else "Import via QR",
                            color = AppAccentColor(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    } else {
        val initialPage = remember(accounts, activeBankAccountId) {
            val idx = accounts.indexOfFirst { it.id == activeBankAccountId }
            if (idx >= 0) idx else 0
        }
        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
            initialPage = initialPage,
            pageCount = { accounts.size.coerceAtLeast(1) }
        )

        val selectedAccount = accounts.getOrNull(pagerState.currentPage.coerceAtMost((accounts.size - 1).coerceAtLeast(0)))

        // Asynchronous Calculations using remember & derivedStateOf
        val totalInitialBalance by remember(accounts) {
            derivedStateOf {
                accounts.filter { !it.isDeleted }.sumOf {
                    if (it.initialBalance != 0.0 || it.balance == 0.0) it.initialBalance else it.balance
                }
            }
        }
        val totalIncome by remember(expenses, accounts) {
            derivedStateOf {
                val accountIds = accounts.filter { !it.isDeleted }.map { it.id }.toSet()
                expenses.filter { !it.isDeleted && it.type.equals("INCOME", ignoreCase = true) && it.bankAccountId in accountIds }.sumOf { it.amount }
            }
        }
        val totalExpense by remember(expenses, accounts) {
            derivedStateOf {
                val accountIds = accounts.filter { !it.isDeleted }.map { it.id }.toSet()
                expenses.filter { !it.isDeleted && it.type.equals("EXPENSE", ignoreCase = true) && it.bankAccountId in accountIds }.sumOf { it.amount }
            }
        }
        val totalBalance by remember(totalInitialBalance, totalIncome, totalExpense) {
            derivedStateOf {
                totalInitialBalance + totalIncome - totalExpense
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (prefLanguage == "bn") "এক্সট্রা অ্যাকাউন্টস" else "Extras Accounts",
                    color = AppTextColor().copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showQrImportScanner = true },
                        modifier = Modifier.size(32.dp).testTag("import_qr_account_header_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Import via QR",
                            tint = AppAccentColor(),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showAddAccountDialog = true },
                        modifier = Modifier.size(32.dp).testTag("add_account_header_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Account",
                            tint = AppAccentColor(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (accounts.isEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "No accounts set.",
                            color = AppTextColor().copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        GlassButton(
                            onClick = { showAddAccountDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = AppAccentColor())
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add Account", color = AppAccentColor(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
                    userScrollEnabled = true
                ) { page ->
                        val account = accounts[page]
                        val accIncome = expenses.filter { it.bankAccountId == account.id && !it.isDeleted && it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
                        val accExpense = expenses.filter { it.bankAccountId == account.id && !it.isDeleted && it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
                        val accInitBal = if (account.initialBalance != 0.0 || account.balance == 0.0) account.initialBalance else account.balance
                        val computedAccBalance = accInitBal + accIncome - accExpense

                        var showAccountMenu by remember { mutableStateOf(false) }
                        var showQRDialog by remember { mutableStateOf(false) }
                        var showFullScreenQR by remember { mutableStateOf(false) }
                        var showDeleteDialog by remember { mutableStateOf(false) }

                        if (showDeleteDialog) {
                            com.example.ui.components.Tier1DeleteDialog(
                                itemTitle = account.name,
                                onConfirm = {
                                    showDeleteDialog = false
                                    viewModel.deleteVirtualAccount(account)
                                },
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
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = account.name,
                                                    color = AppTextColor(),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                val isDefaultAcc = userProfile?.defaultBankAccountId == account.id
                                                if (isDefaultAcc) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(AppAccentColor().copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (prefLanguage == "bn") "ডিফল্ট" else "Default",
                                                            color = AppAccentColor(),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
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
                                                Icon(Icons.Default.Info, contentDescription = "Account Info", tint = AppAccentColor())
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
                                                        text = { Text("Edit this account", color = AppAccentColor()) },
                                                        onClick = {
                                                            showAccountMenu = false
                                                            editingAccount = account
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = AppAccentColor()) }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Share Account via QR", color = AccentGreen) },
                                                        onClick = {
                                                            showAccountMenu = false
                                                            viewModel.exportViaQr(com.example.data.BackupDataType.SINGLE_BANK_ACCOUNT, bankAccountId = account.id) { result ->
                                                                if (result is com.example.data.ExportResult.Success) {
                                                                    qrSessionId = java.util.UUID.randomUUID().toString()
                                                                    qrExportItemName = "${account.name} account's data"
                                                                    activeQrExportData = result
                                                                } else if (result is com.example.data.ExportResult.Error) {
                                                                    android.widget.Toast.makeText(context, "Export failed: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.QrCode, null, tint = AccentGreen) }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Import Ledger via QR", color = AppAccentColor()) },
                                                        onClick = {
                                                            showAccountMenu = false
                                                            showQrImportScanner = true
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, null, tint = AppAccentColor()) }
                                                    )
                                                    val isDefaultAcc = userProfile?.defaultBankAccountId == account.id
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                if (isDefaultAcc) "★ Default Account" else "☆ Set as Default Account",
                                                                color = if (isDefaultAcc) AppAccentColor() else AppTextColor()
                                                            )
                                                        },
                                                        onClick = {
                                                            viewModel.setDefaultBankAccount(account.id)
                                                            showAccountMenu = false
                                                        },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Default.Star,
                                                                null,
                                                                tint = AppAccentColor()
                                                            )
                                                        }
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

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (prefLanguage == "bn") "চলতি ব্যালেন্স" else "Current Balance",
                                            color = AppTextColor().copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                        RollingNumberText(
                                            value = computedAccBalance,
                                            prefix = "₹",
                                            style = TextStyle(
                                                color = AppTextColor(),
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )

                                        // Metric breakdown row (Initial, Income, Expense)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AppTextColor().copy(alpha = 0.05f))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = if (prefLanguage == "bn") "প্রারম্ভিক" else "Initial",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 9.sp
                                                )
                                                Text(
                                                    text = "₹${"%,.0f".format(accInitBal)}",
                                                    color = AppTextColor().copy(alpha = 0.85f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "আয়" else "Income",
                                                    color = AccentGreen.copy(alpha = 0.7f),
                                                    fontSize = 9.sp
                                                )
                                                Text(
                                                    text = "+₹${"%,.0f".format(accIncome)}",
                                                    color = AccentGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "ব্যয়" else "Expense",
                                                    color = AccentPink.copy(alpha = 0.7f),
                                                    fontSize = 9.sp
                                                )
                                                Text(
                                                    text = "-₹${"%,.0f".format(accExpense)}",
                                                    color = AccentPink,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                }
                            }
                        if (showQRDialog) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            val isBengali = prefLanguage == "bn"

                            androidx.activity.compose.BackHandler(onBack = { showQRDialog = false })

                            androidx.compose.ui.window.Dialog(
                                onDismissRequest = { showQRDialog = false },
                                properties = androidx.compose.ui.window.DialogProperties(
                                    usePlatformDefaultWidth = false,
                                    decorFitsSystemWindows = false,
                                    dismissOnClickOutside = true,
                                    dismissOnBackPress = true
                                )
                            ) {
                                com.example.ui.components.DialogEdgeToEdge()
                                AppThemeProvider(viewModel) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .clickable(
                                                onClick = { showQRDialog = false },
                                                indication = null,
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth(0.92f)
                                                .widthIn(max = 520.dp)
                                                .padding(vertical = 24.dp)
                                                .clickable(
                                                    onClick = {},
                                                    indication = null,
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                                ),
                                            shape = RoundedCornerShape(24.dp),
                                            color = getDialogBackground(),
                                            border = BorderStroke(1.dp, getGlassBorder())
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(20.dp)
                                                    .verticalScroll(rememberScrollState())
                                            ) {
                                                // Header Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .clip(CircleShape)
                                                                .background(AppAccentColor().copy(alpha = 0.18f))
                                                                .border(1.dp, AppAccentColor().copy(alpha = 0.4f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.AccountBalance,
                                                                contentDescription = null,
                                                                tint = AppAccentColor(),
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            text = if (isBengali) "অ্যাকাউন্টের বিবরণ" else "Account Details",
                                                            color = AppTextColor(),
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { showQRDialog = false },
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .background(getGlassIconButtonBg(), CircleShape)
                                                            .bounceClick()
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTextColor())
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Header Banner Card
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = getSettingsItemBg()),
                                                    shape = RoundedCornerShape(16.dp),
                                                    border = BorderStroke(1.dp, getGlassBorder()),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(14.dp)
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = account.name,
                                                                color = AppTextColor(),
                                                                fontSize = 18.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = if (isBengali) "অ্যাকাউন্টের বিস্তারিত বিবরণ" else "Account Information & Details",
                                                                color = AppSecondaryTextColor(),
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Info Card Group
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = getSettingsItemBg()),
                                                    shape = RoundedCornerShape(16.dp),
                                                    border = BorderStroke(1.dp, getGlassBorder()),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                                        // Account Number
                                                        if (account.accountNumber.isNotBlank()) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(36.dp)
                                                                            .clip(RoundedCornerShape(10.dp))
                                                                            .background(AppAccentColor().copy(alpha = 0.15f)),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(Icons.Default.Numbers, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(18.dp))
                                                                    }
                                                                    Spacer(modifier = Modifier.width(12.dp))
                                                                    Column {
                                                                        Text(if (isBengali) "অ্যাকাউন্ট নম্বর" else "Account Number", color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
                                                                        Text(account.accountNumber, color = AppTextColor(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                                                    }
                                                                }
                                                                IconButton(
                                                                    onClick = {
                                                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(account.accountNumber))
                                                                        android.widget.Toast.makeText(context, if (isBengali) "অ্যাকাউন্ট নম্বর কপি করা হয়েছে!" else "Account # copied!", android.widget.Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    modifier = Modifier
                                                                        .size(34.dp)
                                                                        .background(AppAccentColor().copy(alpha = 0.1f), CircleShape)
                                                                        .bounceClick()
                                                                ) {
                                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AppAccentColor(), modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                            HorizontalDivider(color = getGlassBorder())
                                                        }

                                                        // IFSC Code
                                                        if (account.ifsc.isNotBlank()) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(36.dp)
                                                                            .clip(RoundedCornerShape(10.dp))
                                                                            .background(Color(0xFFFFB74D).copy(alpha = 0.15f)),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                                                                    }
                                                                    Spacer(modifier = Modifier.width(12.dp))
                                                                    Column {
                                                                        Text(if (isBengali) "আইএফএসসি কোড (IFSC)" else "IFSC Code", color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
                                                                        Text(account.ifsc, color = AppTextColor(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                                                    }
                                                                }
                                                                IconButton(
                                                                    onClick = {
                                                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(account.ifsc))
                                                                        android.widget.Toast.makeText(context, if (isBengali) "IFSC কপি করা হয়েছে!" else "IFSC copied!", android.widget.Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    modifier = Modifier
                                                                        .size(34.dp)
                                                                        .background(Color(0xFFFFB74D).copy(alpha = 0.1f), CircleShape)
                                                                        .bounceClick()
                                                                ) {
                                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                            HorizontalDivider(color = getGlassBorder())
                                                        }

                                                        // UPI ID
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(36.dp)
                                                                        .clip(RoundedCornerShape(10.dp))
                                                                        .background(Color(0xFFBA68C8).copy(alpha = 0.15f)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFFBA68C8), modifier = Modifier.size(18.dp))
                                                                }
                                                                Spacer(modifier = Modifier.width(12.dp))
                                                                Column {
                                                                    Text(if (isBengali) "ইউপিআই আইডি (UPI ID)" else "UPI ID", color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
                                                                    Text(account.upiId.ifBlank { if (isBengali) "প্রদান করা হয়নি" else "Not provided" }, color = AppTextColor(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                                                }
                                                            }
                                                            if (account.qrCodeUri.isNotBlank()) {
                                                                IconButton(
                                                                    onClick = { showFullScreenQR = true },
                                                                    modifier = Modifier
                                                                        .size(34.dp)
                                                                        .background(Color(0xFFBA68C8).copy(alpha = 0.1f), CircleShape)
                                                                        .bounceClick()
                                                                ) {
                                                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Preview QR", tint = Color(0xFFBA68C8), modifier = Modifier.size(16.dp))
                                                                }
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                            }
                                                            if (account.upiId.isNotBlank()) {
                                                                IconButton(
                                                                    onClick = {
                                                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(account.upiId))
                                                                        android.widget.Toast.makeText(context, if (isBengali) "UPI ID কপি করা হয়েছে!" else "UPI ID copied!", android.widget.Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    modifier = Modifier
                                                                        .size(34.dp)
                                                                        .background(Color(0xFFBA68C8).copy(alpha = 0.1f), CircleShape)
                                                                        .bounceClick()
                                                                ) {
                                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFFBA68C8), modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                        }

                                                        if (!account.importedFrom.isNullOrBlank()) {
                                                            HorizontalDivider(color = getGlassBorder())
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(36.dp)
                                                                        .clip(RoundedCornerShape(10.dp))
                                                                        .background(Color(0xFF64B5F6).copy(alpha = 0.15f)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(18.dp))
                                                                }
                                                                Spacer(modifier = Modifier.width(12.dp))
                                                                Column {
                                                                    Text(if (isBengali) "আমদানি করা হয়েছে" else "Import Source", color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
                                                                    Text(if (isBengali) "আমদানি করা হয়েছে: ${account.importedFrom}" else "Imported from: ${account.importedFrom}", color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Financial Summary Card
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = getSettingsItemBg()),
                                                    shape = RoundedCornerShape(16.dp),
                                                    border = BorderStroke(1.dp, getGlassBorder()),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                                        Text(if (isBengali) "আর্থিক সারসংক্ষেপ" else "Financial Summary", color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                        
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text(if (isBengali) "প্রারম্ভিক স্থিতি" else "Opening Balance", color = AppSecondaryTextColor(), fontSize = 12.sp)
                                                            Text("₹${"%,.0f".format(accInitBal)}", color = AppTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                        }
                                                        
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text(if (isBengali) "মোট আয়" else "Total Income", color = AppSecondaryTextColor(), fontSize = 12.sp)
                                                            Text("+₹${"%,.0f".format(accIncome)}", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                        }

                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text(if (isBengali) "মোট ব্যয়" else "Total Expense", color = AppSecondaryTextColor(), fontSize = 12.sp)
                                                            Text("-₹${"%,.0f".format(accExpense)}", color = AccentPink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                        }
                                                        
                                                        HorizontalDivider(color = getGlassBorder())

                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text(if (isBengali) "নেট ব্যালেন্স" else "Net Balance", color = AppTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                            Text("₹${"%,.0f".format(computedAccBalance)}", color = AppAccentColor(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }

                                                // Payment QR Code Card
                                                if (account.qrCodeUri.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        text = if (isBengali) "পেমেন্ট কিউআর কোড" else "Payment QR Code",
                                                        color = AppTextColor(),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(120.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(Color.White)
                                                            .border(1.dp, AppAccentColor().copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                                            .clickable { showFullScreenQR = true }
                                                            .padding(6.dp)
                                                            .align(Alignment.CenterHorizontally),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        AsyncImage(
                                                            model = account.qrCodeUri,
                                                            contentDescription = "QR Code",
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Close Button
                                                Button(
                                                    onClick = { showQRDialog = false },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .bounceClick(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor(), contentColor = AppOnAccentColor())
                                                ) {
                                                    Text(if (isBengali) "বন্ধ করুন" else "Close", color = AppOnAccentColor(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                            }
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
                items(allCategoryNames, key = { it }, contentType = { "category_filter" }) { cat ->
                    val isSelected = activeCategoryFilter == cat
                    val chipBg = if (isSelected) AppAccentColor().copy(alpha = 0.15f) else Color.Transparent
                    val chipBorder = if (isSelected) AppAccentColor() else AppTextColor().copy(alpha = 0.2f)
                    val chipText = if (isSelected) AppAccentColor() else AppTextColor().copy(alpha = 0.6f)

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
            items(itemsToDisplay, key = { it.id }, contentType = { "expense_item" }) { exp ->
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
                                        color = AppAccentColor(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = " • ${Formatters.formatShortDate(exp.timestamp)}",
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
                            tint = AppAccentColor()
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
            Spacer(modifier = Modifier.height(156.dp))
        }
    }

    // Bottom-end floating button
    if (accounts.isNotEmpty()) {
        FloatingActionButton(
            onClick = { showAddExpense = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 148.dp, end = 16.dp)
                .bounceClick()
                .testTag("add_expense_fab"),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Transaction",
                modifier = Modifier.size(28.dp)
            )
        }
    }
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

    // QR Export Stream Dialog
    if (activeQrExportData != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = activeQrExportData!!,
            sessionId = qrSessionId,
            itemName = qrExportItemName,
            onDismiss = { activeQrExportData = null }
        )
    }

    // QR Scanner Dialog for Import
    if (showQrImportScanner) {
        com.example.ui.components.QrScannerDialog(
            onDismiss = { showQrImportScanner = false },
            onSuccess = { payload, key, iv ->
                showQrImportScanner = false
                viewModel.importAccountViaQr(payload, key, iv) { result ->
                    when (result) {
                        is com.example.data.ImportResult.Success -> {
                            android.widget.Toast.makeText(context, result.message, android.widget.Toast.LENGTH_LONG).show()
                        }
                        is com.example.data.ImportResult.Error -> {
                            android.widget.Toast.makeText(context, "Import failed: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    if (showAddAccountDialog) {
        com.example.ui.screens.UnifiedAccountDialog(viewModel = viewModel,
            account = null,
            prefLanguage = prefLanguage,
            accountsCount = accounts.size,
            onDismiss = { showAddAccountDialog = false },
            onSave = { name, initialBal, details, upiId, qrCodeUri, accountNumber, ifsc, branchName ->
                viewModel.addVirtualAccount(
                    name = name,
                    balance = initialBal,
                    details = details,
                    qrCodeUri = qrCodeUri,
                    upiId = upiId,
                    accountNumber = accountNumber,
                    ifsc = ifsc,
                    branchName = branchName
                )
                showAddAccountDialog = false
            },
            onDelete = null
        )
    }

    if (editingAccount != null) {
        com.example.ui.screens.UnifiedAccountDialog(viewModel = viewModel,
            account = editingAccount,
            prefLanguage = prefLanguage,
            accountsCount = accounts.size,
            onDismiss = { editingAccount = null },
            onSave = { name, initialBal, details, upiId, qrCodeUri, accountNumber, ifsc, branchName ->
                val acc = editingAccount!!
                val accInc = expenses.filter { it.bankAccountId == acc.id && !it.isDeleted && it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
                val accExp = expenses.filter { it.bankAccountId == acc.id && !it.isDeleted && it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
                val newComputedBalance = initialBal + accInc - accExp
                viewModel.editVirtualAccount(
                    acc.copy(
                        name = name,
                        balance = newComputedBalance,
                        initialBalance = initialBal,
                        details = details,
                        upiId = upiId,
                        qrCodeUri = qrCodeUri,
                        accountNumber = accountNumber,
                        ifsc = ifsc,
                        branchName = branchName
                    )
                )
                editingAccount = null
            },
            onDelete = {
                viewModel.deleteVirtualAccount(editingAccount!!)
                editingAccount = null
            }
        )
    }
}


@Composable
fun AttachmentViewerDialog(
    expenseId: Int,
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

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
                    items(attachments, key = { it.uri }, contentType = { "attachment_item" }) { att ->
                        AttachmentItem(att, context)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AppAccentColor())
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
    
    var monthOffset by remember { mutableStateOf(0) }
    val monthFormat = remember { Formatters.getSimpleDateFormat("MMMM yyyy") }

    val earliestTs = remember(expenses) {
        expenses.filter { !it.isDeleted }.minOfOrNull { it.timestamp } ?: System.currentTimeMillis()
    }

    val isEarliestMonth = remember(monthOffset, earliestTs) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        
        val eCal = Calendar.getInstance().apply { timeInMillis = earliestTs }
        val earliestYear = eCal.get(Calendar.YEAR)
        val earliestMonth = eCal.get(Calendar.MONTH)
        
        (currentYear < earliestYear) || (currentYear == earliestYear && currentMonth <= earliestMonth)
    }

    val (startOfMonthMillis, endOfMonthMillis) = remember(monthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        Pair(start, end)
    }

    val filteredExpenses by remember(expenses, searchQuery, selectedAccountId, startOfMonthMillis, endOfMonthMillis) {
        derivedStateOf {
            expenses.filter { exp ->
                val matchesMonth = exp.timestamp in startOfMonthMillis..endOfMonthMillis
                val matchesSearch = searchQuery.isBlank() || exp.name.contains(searchQuery, ignoreCase = true) || exp.notes.contains(searchQuery, ignoreCase = true)
                val matchesAccount = selectedAccountId == null || exp.bankAccountId == selectedAccountId
                matchesMonth && matchesSearch && matchesAccount
            }
        }
    }

    val currentMonthOffset by rememberUpdatedState(monthOffset)
    val currentIsEarliestMonth by rememberUpdatedState(isEarliestMonth)

    var totalDragAmount by remember { mutableStateOf(0f) }
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getAppBackgroundGradient(com.example.ui.components.LocalAppTheme.current))
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragAmount = 0f },
                    onDragEnd = {
                        val swipeThreshold = 40.dp.toPx()
                        if (totalDragAmount > swipeThreshold) {
                            // Swipe Right -> Previous Month (Go back in time, if not already at earliest month)
                            if (!currentIsEarliestMonth) {
                                monthOffset -= 1
                            }
                        } else if (totalDragAmount < -swipeThreshold) {
                            // Swipe Left -> Next Month (Go forward in time, restricted to not exceed current month)
                            if (currentMonthOffset < 0) {
                                monthOffset += 1
                            }
                        }
                        totalDragAmount = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragAmount += dragAmount
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
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
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = AppTextColor(),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedContainerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    items(accounts, key = { it.id }, contentType = { "account_filter_chip" }) { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = AppTextColor(),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedContainerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentDisplayMonth = remember(monthOffset) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.MONTH, monthOffset)
                        monthFormat.format(cal.time)
                    }
                    IconButton(onClick = { monthOffset -= 1 }, enabled = !isEarliestMonth, modifier = Modifier.bounceClick()) {
                        Icon(Icons.Default.ChevronLeft, "Previous Month", tint = if (!isEarliestMonth) AppTextColor() else AppTextColor().copy(alpha = 0.3f))
                    }
                    Text(currentDisplayMonth, color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { monthOffset += 1 }, enabled = monthOffset < 0, modifier = Modifier.bounceClick()) {
                        Icon(Icons.Default.ChevronRight, "Next Month", tint = if (monthOffset < 0) AppTextColor() else AppTextColor().copy(alpha = 0.3f))
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (filteredExpenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                val currentDisplayMonth = remember(monthOffset) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MONTH, monthOffset)
                    monthFormat.format(cal.time)
                }
                EmptyStateCard(message = "No past transactions for $currentDisplayMonth.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
            ) {
                items(filteredExpenses, key = { it.id }, contentType = { "past_expense_item" }) { expense ->
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
}

@Composable
fun TransactionItem(
    expense: ExtraExpenseEntity,
    accountName: String,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        com.example.ui.components.Tier1DeleteDialog(
            itemTitle = expense.name,
            onConfirm = { onDelete(); showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    val isIncome = expense.type == "INCOME" 
    
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
                Text(Formatters.formatDateTime(expense.timestamp), fontSize = 10.sp, color = AppTextColor().copy(alpha = 0.4f))
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
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(getDialogBackground())) {
                        DropdownMenuItem(
                            text = { Text("Move to Bin", color = AccentPink) },
                            onClick = { showMenu = false; showDeleteConfirm = true },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = AccentPink) }
                        )
                    }
                }
            }
        }
    }
}
