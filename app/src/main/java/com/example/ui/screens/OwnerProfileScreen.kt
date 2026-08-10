package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.data.model.OwnerAccountEntity
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.bounceClick
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun OwnerProfileScreen(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val ownerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle()
    
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<OwnerAccountEntity?>(null) }

    androidx.activity.compose.BackHandler {
        onDismiss()
    }
    
    com.example.ui.components.AppThemeProvider(viewModel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(userProfile.themeMode))
                .pointerInput(Unit) { detectTapGestures { } }
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Scaffold(
                modifier = Modifier.fillMaxHeight().widthIn(max = 840.dp),
                containerColor = Color.Transparent,
                contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(getGlassBorder().copy(alpha = 0.1f), CircleShape).bounceClick()
                        ) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = AppTextColor())
                        }
                        Text("Owner Profile", color = AppTextColor(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.background(AccentCyan.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, "Edit Profile", tint = AccentCyan)
                        }
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        // Profile Info Section
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (userProfile.avatar.isNotBlank()) {
                                    AsyncImage(
                                        model = userProfile.avatar,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.size(100.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Text(userProfile.name.ifBlank { "Owner Name" }, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppTextColor())
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(userProfile.contact.ifBlank { "Contact Not Set" }, fontSize = 16.sp, color = AppTextColor().copy(alpha = 0.6f))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Bank Accounts & UPI Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bank & UPI Details", color = AppTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (ownerAccounts.isEmpty()) {
                        item { EmptyStateCard(message = "No bank details added.") }
                    } else {
                        items(ownerAccounts) { acc ->
                            OwnerAccountCard(
                                account = acc,
                                onEdit = { accountToEdit = acc; showAddAccountDialog = true },
                                onDelete = { viewModel.deleteOwnerAccount(acc) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = { showAddAccountDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AccentCyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add New Bank/UPI", fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            userProfile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, contact, avatar ->
                viewModel.updateProfile(name, contact, avatar)
                showEditProfileDialog = false
            }
        )
    }

    if (showAddAccountDialog) {
        AddOwnerAccountDialog(
            account = accountToEdit,
            onDismiss = { showAddAccountDialog = false; accountToEdit = null },
            onSave = { newAcc ->
                if (accountToEdit == null) {
                    viewModel.addOwnerAccount(newAcc)
                } else {
                    viewModel.updateOwnerAccount(newAcc)
                }
                showAddAccountDialog = false
                accountToEdit = null
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    userProfile: com.example.data.model.UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(userProfile.name) }
    var contact by remember { mutableStateOf(userProfile.contact) }
    var avatar by remember { mutableStateOf(userProfile.avatar) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { avatar = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Owner Profile") },
        text = {
            Column(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.2f)).clickable { photoPicker.launch("image/*") }.align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar.isNotBlank()) {
                        AsyncImage(model = avatar, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.Gray)
                    }
                }
                GlassTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                GlassTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, contact, avatar) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyStateCard(message: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Inbox, null, tint = AppTextColor().copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(message, color = AppTextColor().copy(alpha = 0.3f), fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
fun OwnerAccountCard(
    account: com.example.data.model.OwnerAccountEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showQR by remember { mutableStateOf(false) }
    var isSensitiveVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Delete", color = AppTextColor(), fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this bank details?", color = AppTextColor()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = AccentPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = AppTextColor().copy(alpha = 0.6f))
                }
            },
            containerColor = getDialogBackground()
        )
    }

    GlassCard(cornerRadius = 20.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(account.accountName.ifBlank { "Bank Account" }, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppTextColor())
                    Text(account.ownerName, fontSize = 12.sp, color = AppTextColor().copy(alpha = 0.6f))
                }
                Row {
                    IconButton(onClick = { isSensitiveVisible = !isSensitiveVisible }) {
                        Icon(if (isSensitiveVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = AppTextColor().copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    }
                    var showOverflowMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = AppTextColor().copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier.background(getDialogBackground())
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Delete Account", color = AccentPink) },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteConfirmation = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = AccentPink, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AppTextColor().copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            
            val mask = { text: String ->
                if (isSensitiveVisible || text.isBlank()) {
                    text
                } else {
                    if (text.length > 4) {
                        "•••• •••• " + text.takeLast(4)
                    } else {
                        "•••• " + text
                    }
                }
            }
            
            DetailRowItem(label = "A/C Number", value = mask(account.accountNumber))
            DetailRowItem(label = "IFSC Code", value = mask(account.ifscCode))
            DetailRowItem(label = "Branch", value = account.branchName)
            DetailRowItem(label = "UPI ID", value = mask(account.upiId))
            
            if (account.qrCodeUri.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                GlassButton(
                    onClick = { showQR = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View QR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showQR) {
        Dialog(onDismissRequest = { showQR = false }) {
            Box(modifier = Modifier.background(getDialogBackground(), RoundedCornerShape(24.dp))) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("UPI QR Code", fontWeight = FontWeight.Bold, color = AppTextColor())
                    Spacer(modifier = Modifier.height(16.dp))
                    AsyncImage(
                        model = account.qrCodeUri,
                        contentDescription = "QR",
                        modifier = Modifier.size(250.dp).clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(onClick = { showQR = false }) { Text("Close", color = AccentCyan) }
                }
            }
        }
    }
}

@Composable
fun DetailRowItem(label: String, value: String) {
    if (value.isBlank()) return
    var showLargeView by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { showLargeView = true },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = AppTextColor().copy(alpha = 0.5f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTextColor())
    }

    if (showLargeView) {
        Dialog(onDismissRequest = { showLargeView = false }) {
            GlassCard(cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(label, color = AppTextColor().copy(alpha = 0.5f), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(value, color = AppTextColor(), fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(32.dp))
                    TextButton(onClick = { showLargeView = false }) { Text("Close") }
                }
            }
        }
    }
}

@Composable
fun AddOwnerAccountDialog(
    account: com.example.data.model.OwnerAccountEntity?,
    onDismiss: () -> Unit,
    onSave: (com.example.data.model.OwnerAccountEntity) -> Unit
) {
    var isUpiMode by remember { mutableStateOf(account?.upiId?.isNotBlank() == true || account?.qrCodeUri?.isNotBlank() == true) }
    
    var ownerName by remember { mutableStateOf(account?.ownerName ?: "") }
    var accountName by remember { mutableStateOf(account?.accountName ?: "") }
    var accountNumber by remember { mutableStateOf(account?.accountNumber ?: "") }
    var ifscCode by remember { mutableStateOf(account?.ifscCode ?: "") }
    var branchName by remember { mutableStateOf(account?.branchName ?: "") }
    var upiId by remember { mutableStateOf(account?.upiId ?: "") }
    var qrCodeUri by remember { mutableStateOf(account?.qrCodeUri ?: "") }

    val qrPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { qrCodeUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "Add Account Details" else "Edit Account Details") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Account Holder Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                GlassTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Bank Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                GlassTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                GlassTextField(
                    value = ifscCode,
                    onValueChange = { ifscCode = it },
                    label = { Text("IFSC Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                GlassTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text("Branch Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("UPI Details (Optional)", fontWeight = FontWeight.Bold, color = AppTextColor())
                GlassTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { qrPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.1f), contentColor = AccentCyan)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (qrCodeUri.isEmpty()) "Upload UPI QR" else "QR Uploaded")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(OwnerAccountEntity(
                    id = account?.id ?: 0,
                    ownerName = ownerName,
                    accountName = accountName,
                    accountNumber = accountNumber,
                    ifscCode = ifscCode,
                    branchName = branchName,
                    upiId = upiId,
                    qrCodeUri = qrCodeUri
                ))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OwnerQRViewerDialog(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val ownerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle()
    val accountsWithQR = ownerAccounts.filter { it.qrCodeUri.isNotBlank() }

    androidx.activity.compose.BackHandler {
        onDismiss()
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            if (accountsWithQR.isEmpty()) {
                Text(
                    text = "No QR codes found in Owner Profile.",
                    color = Color.White,
                    fontSize = 16.sp
                )
            } else {
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                    pageCount = { accountsWithQR.size }
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).clickable(enabled = false) {} // block dismiss on content click
                ) {
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        userScrollEnabled = accountsWithQR.size > 1
                    ) { page ->
                        val account = accountsWithQR[page]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = account.accountName.ifBlank { "Bank QR" },
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (account.upiId.isNotBlank()) {
                                Text(
                                    text = account.upiId,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            AsyncImage(
                                model = account.qrCodeUri,
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .size(300.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    
                    if (accountsWithQR.size > 1) {
                        Row(
                            Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                                .padding(bottom = 32.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(accountsWithQR.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.3f)
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

