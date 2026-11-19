@file:Suppress("DEPRECATION")
package com.example.ui.screens.accounts

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.BankAccountEntity
import com.example.data.model.OwnerAccountEntity
import com.example.ui.components.*
import com.example.ui.viewmodel.FinancialViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagerScreen(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val ownerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle()

    var editingBankAccount by remember { mutableStateOf<BankAccountEntity?>(null) }
    var isAddingBankAccount by remember { mutableStateOf(false) }
    var editingOwnerAccount by remember { mutableStateOf<OwnerAccountEntity?>(null) }
    var isAddingOwnerAccount by remember { mutableStateOf(false) }
    var selectedQrPreviewUri by remember { mutableStateOf<String?>(null) }

    val isBengali = userProfile.language == "bn"
    val themeMode = userProfile.themeMode
    val bgPreset = userProfile.bgPreset

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalAppTheme provides themeMode,
                LocalAppBgPreset provides bgPreset,
                LocalAppCustomBgUri provides userProfile.customBgUri,
                LocalAppLiquidGlassTheme provides userProfile.enableLiquidGlassTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isAppLightMode()) com.example.ui.theme.M3BackgroundLight else com.example.ui.theme.M3BackgroundDark
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(getAppBackgroundGradient(themeMode, bgPreset))
                            .pointerInput(Unit) { detectTapGestures { } },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding(),
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets(0.dp),
                            topBar = {
                                Box(modifier = Modifier.fillMaxWidth().dialogSafeTopPadding(extraTop = 4.dp)) {
                                    com.example.ui.screens.settings.components.SettingsPageHeader(
                                        title = if (isBengali) "অ্যাকাউন্ট ব্যবস্থাপনা" else "Account Manager",
                                        onBack = onDismiss,
                                        trailingContent = {
                                            IconButton(
                                                onClick = { isAddingBankAccount = true },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(AppAccentColor(), CircleShape)
                                                    .bounceClick()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add Account",
                                                    tint = AppOnAccentColor()
                                                )
                                            }
                                        }
                                    )
                                }
                            }
            ) { innerPadding ->
                val accountManagerListState = rememberPersistedLazyListState("account_manager")
                LazyColumn(
                    state = accountManagerListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp + rememberSplitScreenSafeBottomPadding())
                ) {
                // Summary Card
                item {
                    val totalBalance = bankAccounts.filter { !it.isDeleted }.sumOf { it.balance }
                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isBengali) "মোট ব্যালেন্স" else "Total Accounts Balance",
                                color = AppTextColor().copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%,.2f", totalBalance)}",
                                color = AccentGreen,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${bankAccounts.filter { !it.isDeleted }.size} ${if (isBengali) "টি সক্রিয় ব্যাংক অ্যাকাউন্ট" else "Active Bank Accounts"} • ${ownerAccounts.size} ${if (isBengali) "টি পেমেন্ট প্রোফাইল" else "Payment Profiles"}",
                                color = AppTextColor().copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Bank Accounts Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBengali) "ব্যাংক অ্যাকাউন্টসমূহ" else "Bank Accounts",
                            color = AppTextColor(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val activeBankAccounts = bankAccounts.filter { !it.isDeleted }
                if (activeBankAccounts.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBengali) "কোন অ্যাকাউন্ট নেই। যোগ করতে + চাপুন।" else "No accounts found. Tap + to add.",
                                    color = AppTextColor().copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    items(activeBankAccounts, key = { it.id }) { account ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingBankAccount = account }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(AppAccentColor().copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (account.qrCodeUri.isNotBlank()) {
                                            AsyncImage(
                                                model = account.qrCodeUri,
                                                contentDescription = account.name,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = AppAccentColor(),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = account.name,
                                            color = AppTextColor(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (account.accountNumber.isNotBlank()) {
                                            Text(
                                                text = "A/C: ${account.accountNumber}",
                                                color = AppTextColor().copy(alpha = 0.6f),
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (account.upiId.isNotBlank()) {
                                            Text(
                                                text = "UPI: ${account.upiId}",
                                                color = AppAccentColor().copy(alpha = 0.8f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.2f", account.balance)}",
                                        color = if (account.balance >= 0) AccentGreen else AccentPink,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { editingBankAccount = account },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = AppTextColor().copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Owner Accounts Section
                if (ownerAccounts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBengali) "মালিক পেমেন্ট প্রোফাইল" else "Owner Payment Profiles",
                            color = AppTextColor(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(ownerAccounts, key = { it.id }) { ownerAcc ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingOwnerAccount = ownerAcc }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(AccentPink.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = AccentPink,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = ownerAcc.accountName.ifBlank { ownerAcc.ownerName },
                                            color = AppTextColor(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (ownerAcc.upiId.isNotBlank()) {
                                            Text(
                                                text = "UPI: ${ownerAcc.upiId}",
                                                color = AppAccentColor().copy(alpha = 0.8f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                if (ownerAcc.upiId.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(ownerAcc.upiId))
                                            Toast.makeText(context, "UPI Copied: ${ownerAcc.upiId}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy UPI",
                                            tint = AppAccentColor(),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add / Edit Bank Account Subscreen
        if (isAddingBankAccount || editingBankAccount != null) {
            AccountEditorScreen(
                account = editingBankAccount,
                prefLanguage = userProfile.language,
                onDismiss = {
                    isAddingBankAccount = false
                    editingBankAccount = null
                },
                onSave = { name, balance, details, upi, qr, accNum, ifsc, branch, notes, phone, whatsapp, email ->
                    if (editingBankAccount != null) {
                        viewModel.editVirtualAccount(
                            editingBankAccount!!.copy(
                                name = name,
                                initialBalance = balance,
                                balance = balance,
                                details = details,
                                notes = notes,
                                phoneNumber = phone,
                                whatsappNumber = whatsapp,
                                email = email,
                                upiId = upi,
                                qrCodeUri = qr,
                                accountNumber = accNum,
                                ifsc = ifsc,
                                branchName = branch
                            )
                        )
                    } else {
                        viewModel.addVirtualAccount(
                            name = name,
                            balance = balance,
                            details = details,
                            notes = notes,
                            phoneNumber = phone,
                            whatsappNumber = whatsapp,
                            email = email,
                            qrCodeUri = qr,
                            upiId = upi,
                            accountNumber = accNum,
                            ifsc = ifsc,
                            branchName = branch
                        )
                    }
                    isAddingBankAccount = false
                    editingBankAccount = null
                },
                onDelete = editingBankAccount?.let { acc ->
                    {
                        viewModel.deleteVirtualAccount(acc)
                        editingBankAccount = null
                    }
                }
            )
        }
    }
}
}
}
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditorScreen(
    account: BankAccountEntity?,
    prefLanguage: String,
    onDismiss: () -> Unit,
    onSave: (name: String, balance: Double, details: String, upiId: String, qrCodeUri: String, accountNumber: String, ifsc: String, branchName: String, notes: String, phoneNumber: String, whatsappNumber: String, email: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    var accName by remember(account) { mutableStateOf(account?.name ?: "") }
    var accBal by remember(account) { mutableStateOf(account?.initialBalance?.toString() ?: "") }
    var details by remember(account) { mutableStateOf(account?.details ?: "") }
    var notes by remember(account) { mutableStateOf(account?.notes ?: "") }
    var phoneNumber by remember(account) { mutableStateOf(account?.phoneNumber ?: "") }
    var whatsappNumber by remember(account) { mutableStateOf(account?.whatsappNumber ?: "") }
    var email by remember(account) { mutableStateOf(account?.email ?: "") }
    var qrCodeUri by remember(account) { mutableStateOf(account?.qrCodeUri ?: "") }
    var upiId by remember(account) { mutableStateOf(account?.upiId ?: "") }
    var accountNumber by remember(account) { mutableStateOf(account?.accountNumber ?: "") }
    var ifsc by remember(account) { mutableStateOf(account?.ifsc ?: "") }
    var branchName by remember(account) { mutableStateOf(account?.branchName ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            qrCodeUri = copyUriToPersistentFile(context, uri, "QR")
        }
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            itemName = accName,
            requireTypedConfirmation = true,
            onConfirm = {
                onDelete?.invoke()
                showDeleteConfirm = false
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    val themeMode = LocalAppTheme.current
    val bgPreset = LocalAppBgPreset.current

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isAppLightMode()) com.example.ui.theme.M3BackgroundLight else com.example.ui.theme.M3BackgroundDark
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(getAppBackgroundGradient(themeMode, bgPreset))
                        .pointerInput(Unit) { detectTapGestures { } },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0.dp),
                        topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dialogSafeTopPadding(extraTop = 4.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .background(getGlassIconButtonBg(), CircleShape)
                                .bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Back",
                                tint = AppTextColor(),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (account == null) {
                                if (prefLanguage == "bn") "নতুন অ্যাকাউন্ট যুক্ত করুন" else "Add Account"
                            } else {
                                if (prefLanguage == "bn") "অ্যাকাউন্ট সংশোধন" else "Edit Account Details"
                            },
                            color = AppTextColor(),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = {
                                if (accName.isNotBlank()) {
                                    val bal = accBal.toDoubleOrNull() ?: 0.0
                                    onSave(accName, bal, details, upiId, qrCodeUri, accountNumber, ifsc, branchName, notes, phoneNumber, whatsappNumber, email)
                                } else {
                                    Toast.makeText(
                                        context,
                                        if (prefLanguage == "bn") "অ্যাকাউন্টের নাম দিন" else "Enter Account Name",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(AppAccentColor(), CircleShape)
                                .bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = AppOnAccentColor()
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                GlassTextField(
                    value = accName,
                    onValueChange = { accName = it },
                    label = { Text(if (prefLanguage == "bn") "অ্যাকাউন্টের নাম *" else "Account Name *") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                GlassTextField(
                    value = accBal,
                    onValueChange = { accBal = it },
                    label = { Text(if (prefLanguage == "bn") "প্রারম্ভিক ব্যালেন্স (₹)" else "Starting Balance (₹)") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                GlassTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text(if (prefLanguage == "bn") "বিবরণ (ঐচ্ছিক)" else "Details (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                // QR Code / Account Image Section
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (prefLanguage == "bn") "অ্যাকাউন্ট ছবি / QR কোড" else "Account Image / QR Code",
                            color = AppTextColor(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (qrCodeUri.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(70.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    shadowElevation = 2.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        AsyncImage(
                                            model = qrCodeUri,
                                            contentDescription = "Account Image",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalButton(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = AppAccentColor().copy(alpha = 0.14f),
                                            contentColor = AppAccentColor()
                                        )
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (prefLanguage == "bn") "ছবি পরিবর্তন" else "Change Image", fontSize = 12.sp)
                                    }

                                    TextButton(
                                        onClick = { qrCodeUri = "" },
                                        colors = ButtonDefaults.textButtonColors(contentColor = AccentPink)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (prefLanguage == "bn") "ছবি মুছুন" else "Remove Image", fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AppAccentColor().copy(alpha = 0.14f),
                                    contentColor = AppAccentColor()
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (prefLanguage == "bn") "ছবি বা QR কোড যোগ করুন" else "Add Image / QR Code", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Bank & UPI Details
                GlassTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number") },
                    leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassTextField(
                            value = ifsc,
                            onValueChange = { ifsc = it },
                            label = { Text("IFSC Code") },
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GlassTextField(
                            value = branchName,
                            onValueChange = { branchName = it },
                            label = { Text("Branch") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                GlassTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID (e.g. user@okhdfc)") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone / SMS Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GlassTextField(
                            value = whatsappNumber,
                            onValueChange = { whatsappNumber = it },
                            label = { Text("WhatsApp Number") },
                            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                GlassTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                GlassTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Short Notes") },
                    leadingIcon = { Icon(Icons.Default.StickyNote2, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                )

                if (account != null && onDelete != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPink.copy(alpha = 0.2f), contentColor = AccentPink)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (prefLanguage == "bn") "অ্যাকাউন্ট মুছুন" else "Delete Account", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(120.dp + rememberSplitScreenSafeBottomPadding()))
            }
        }
    }
}
}
}
}
