@file:Suppress("DEPRECATION")
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.data.model.BankAccountEntity
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
    
    val visibleOwnerAccountIds = remember(userProfile.visibleBankAccountsOnExtra) {
        val raw = userProfile.visibleBankAccountsOnExtra
        if (raw.isBlank()) emptySet<Int>()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showChooseVisibleAccountsDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<OwnerAccountEntity?>(null) }
    var showOwnerAvatarPreview by remember { mutableStateOf(false) }

    if (showOwnerAvatarPreview && userProfile.avatar.isNotBlank()) {
        FullScreenImagePreviewDialog(
            imageUri = userProfile.avatar,
            title = userProfile.name.ifBlank { "Profile Photo" },
            onDismiss = { showOwnerAvatarPreview = false }
        )
    }

    androidx.activity.compose.BackHandler {
        when {
            showOwnerAvatarPreview -> showOwnerAvatarPreview = false
            showEditProfileDialog -> showEditProfileDialog = false
            showAddAccountDialog -> showAddAccountDialog = false
            showChooseVisibleAccountsDialog -> showChooseVisibleAccountsDialog = false
            accountToEdit != null -> accountToEdit = null
            else -> onDismiss()
        }
    }
    
    val themeMode = userProfile.themeMode
    val bgPreset = userProfile.bgPreset
    val customBgUri = userProfile.customBgUri
    val isLiquidGlass = userProfile.enableLiquidGlassTheme

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()
        Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalAppTheme provides themeMode,
            LocalAppBgPreset provides bgPreset,
            LocalAppCustomBgUri provides customBgUri,
            LocalAppLiquidGlassTheme provides isLiquidGlass
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
                                    title = "Owner Profile",
                                    onBack = onDismiss,
                                    trailingContent = {
                                        FilledTonalIconButton(
                                            onClick = { showEditProfileDialog = true },
                                            modifier = Modifier.size(40.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = AppAccentColor().copy(alpha = 0.15f),
                                                contentColor = AppAccentColor()
                                            )
                                        ) {
                                            Icon(Icons.Default.Edit, "Edit Profile", tint = AppAccentColor())
                                        }
                                    }
                                )
                            }
                        }
                    ) { padding ->
                val editProfileListState = rememberPersistedLazyListState("edit_profile")
                LazyColumn(
                    state = editProfileListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp + rememberSplitScreenSafeBottomPadding())
                ) {
                    item {
                        // Profile Info Section
                        val defaultOwnerAccount = ownerAccounts.find { it.id == userProfile.defaultOwnerAccountId } ?: ownerAccounts.firstOrNull()

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (userProfile.avatar.isNotBlank()) {
                                    AsyncImage(
                                        model = userProfile.avatar,
                                        contentDescription = "Profile Picture (Tap to preview)",
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, AppAccentColor().copy(alpha = 0.6f), CircleShape)
                                            .clickable { showOwnerAvatarPreview = true },
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                Text(userProfile.name.ifBlank { "Owner Name" }, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AppTextColor())
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(userProfile.contact.ifBlank { "Contact Not Set" }, fontSize = 14.sp, color = AppTextColor().copy(alpha = 0.6f))

                                if (defaultOwnerAccount != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = getGlassBorder().copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (defaultOwnerAccount.upiId.isNotBlank()) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("PROFILE UPI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppPrimaryBlueColor())
                                                Text(defaultOwnerAccount.upiId, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppTextColor())
                                            }
                                        }
                                        if (defaultOwnerAccount.accountName.isNotBlank() || defaultOwnerAccount.accountNumber.isNotBlank()) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("LINKED BANK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isAppLightMode()) Color(0xFF1B5E20) else AccentGreen)
                                                Text(
                                                    text = "${defaultOwnerAccount.accountName.ifBlank { "Bank" }} (${if (defaultOwnerAccount.accountNumber.length > 4) "****" + defaultOwnerAccount.accountNumber.takeLast(4) else defaultOwnerAccount.accountNumber})",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AppTextColor()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Daily Transaction Accounts Section
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Daily Transaction Accounts", color = AppHeadingColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (ownerAccounts.isNotEmpty()) {
                        items(ownerAccounts, key = { it.id }) { acc ->
                            OwnerAccountCard(
                                viewModel = viewModel,
                                account = acc,
                                isDefault = (acc.id == userProfile.defaultOwnerAccountId),
                                isVisibleOnExtra = (acc.linkedBankAccountId != null || acc.id in visibleOwnerAccountIds),
                                onSetDefault = {
                                    viewModel.saveUserProfile(userProfile.copy(defaultOwnerAccountId = acc.id))
                                },
                                onEdit = { accountToEdit = acc; showAddAccountDialog = true },
                                onDelete = { viewModel.deleteOwnerAccount(acc) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { showAddAccountDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 50.dp)
                                .wrapContentHeight(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppAccentColor(),
                                contentColor = AppOnAccentColor()
                            )
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Profile Account", fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
}

    if (showEditProfileDialog) {
        val defaultOwnerAccount = ownerAccounts.find { it.id == userProfile.defaultOwnerAccountId } ?: ownerAccounts.firstOrNull()
        EditProfileDialog(viewModel = viewModel,
            userProfile = userProfile,
            defaultOwnerAccount = defaultOwnerAccount,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, contact, avatar, bankName, accNum, ifsc, branch, upi, qrUri ->
                viewModel.updateProfile(name, contact, avatar)
                if (defaultOwnerAccount != null) {
                    val updated = defaultOwnerAccount.copy(
                        ownerName = name.ifBlank { defaultOwnerAccount.ownerName },
                        ownerMobile = contact.ifBlank { defaultOwnerAccount.ownerMobile },
                        accountName = bankName,
                        accountNumber = accNum,
                        ifscCode = ifsc,
                        branchName = branch,
                        upiId = upi,
                        qrCodeUri = qrUri
                    )
                    viewModel.updateOwnerAccount(updated)
                } else if (bankName.isNotBlank() || upi.isNotBlank() || accNum.isNotBlank() || qrUri.isNotBlank()) {
                    val newAcc = com.example.data.model.OwnerAccountEntity(
                        ownerName = name,
                        ownerMobile = contact,
                        accountName = bankName,
                        accountNumber = accNum,
                        ifscCode = ifsc,
                        branchName = branch,
                        upiId = upi,
                        qrCodeUri = qrUri
                    )
                    viewModel.addOwnerAccount(newAcc)
                }
                showEditProfileDialog = false
            }
        )
    }

    if (showAddAccountDialog || accountToEdit != null) {
        val allBankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
        val linkedBank = accountToEdit?.linkedBankAccountId?.let { linkedId ->
            allBankAccounts.find { it.id == linkedId && !it.isDeleted }
        }
        val targetBank = accountToEdit?.let { ownerAcc ->
            BankAccountEntity(
                id = linkedBank?.id ?: 0,
                name = ownerAcc.accountName,
                balance = linkedBank?.balance ?: 0.0,
                initialBalance = linkedBank?.initialBalance ?: 0.0,
                details = linkedBank?.details ?: ownerAcc.notes,
                notes = ownerAcc.notes,
                phoneNumber = ownerAcc.ownerMobile,
                whatsappNumber = ownerAcc.whatsappNumber,
                email = ownerAcc.email,
                accountNumber = ownerAcc.accountNumber,
                ifsc = ownerAcc.ifscCode,
                branchName = ownerAcc.branchName,
                upiId = ownerAcc.upiId,
                qrCodeUri = ownerAcc.qrCodeUri
            )
        }

        UnifiedAccountDialog(
            viewModel = viewModel,
            account = targetBank,
            prefLanguage = userProfile.language,
            accountsCount = ownerAccounts.size,
            onDismiss = {
                showAddAccountDialog = false
                accountToEdit = null
            },
            onSaveExtended = { accName, accBal, details, upi, qrUri, accNum, ifscCode, branch, notes, phone, whatsapp, email ->
                if (accountToEdit != null) {
                    val updated = accountToEdit!!.copy(
                        accountName = accName,
                        ownerMobile = phone.ifBlank { accountToEdit!!.ownerMobile },
                        whatsappNumber = whatsapp,
                        email = email,
                        notes = notes.ifBlank { details },
                        accountNumber = accNum,
                        ifscCode = ifscCode,
                        branchName = branch,
                        upiId = upi,
                        qrCodeUri = qrUri
                    )
                    viewModel.updateOwnerAccount(updated)
                    // If linked to an Extras BankAccount, also sync balance/details
                    if (linkedBank != null) {
                        viewModel.editVirtualAccount(
                            linkedBank.copy(
                                name = accName,
                                initialBalance = accBal,
                                details = details,
                                notes = notes,
                                phoneNumber = phone,
                                whatsappNumber = whatsapp,
                                email = email,
                                qrCodeUri = qrUri,
                                upiId = upi,
                                accountNumber = accNum,
                                ifsc = ifscCode,
                                branchName = branch
                            )
                        )
                    }
                } else {
                    // Sandboxed by default: creates OwnerAccountEntity in Profile ONLY unless user links it
                    val ownerNameVal = userProfile.name.ifBlank { accName }
                    val newAcc = OwnerAccountEntity(
                        ownerName = ownerNameVal,
                        ownerMobile = phone.ifBlank { userProfile.contact },
                        whatsappNumber = whatsapp,
                        email = email,
                        notes = notes.ifBlank { details },
                        accountName = accName,
                        accountNumber = accNum,
                        ifscCode = ifscCode,
                        branchName = branch,
                        upiId = upi,
                        qrCodeUri = qrUri,
                        linkedBankAccountId = null
                    )
                    viewModel.addOwnerAccount(newAcc)
                }
                showAddAccountDialog = false
                accountToEdit = null
            },
            onDelete = if (accountToEdit != null && ownerAccounts.size > 1) {
                {
                    accountToEdit?.let { viewModel.deleteOwnerAccount(it) }
                    showAddAccountDialog = false
                    accountToEdit = null
                }
            } else null
        )
    }
}
}

@Composable
fun EditProfileDialog(
    viewModel: com.example.ui.viewmodel.FinancialViewModel,
    userProfile: com.example.data.model.UserProfileEntity,
    defaultOwnerAccount: com.example.data.model.OwnerAccountEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, contact: String, avatar: String, bankName: String, accNum: String, ifsc: String, branch: String, upi: String, qrUri: String) -> Unit
) {
    var name by remember { mutableStateOf(userProfile.name) }
    var contact by remember { mutableStateOf(userProfile.contact) }
    var avatar by remember { mutableStateOf(userProfile.avatar) }

    var bankName by remember { mutableStateOf(defaultOwnerAccount?.accountName ?: "") }
    var accNum by remember { mutableStateOf(defaultOwnerAccount?.accountNumber ?: "") }
    var ifsc by remember { mutableStateOf(defaultOwnerAccount?.ifscCode ?: "") }
    var branch by remember { mutableStateOf(defaultOwnerAccount?.branchName ?: "") }
    var upi by remember { mutableStateOf(defaultOwnerAccount?.upiId ?: "") }
    var qrUri by remember { mutableStateOf(defaultOwnerAccount?.qrCodeUri ?: "") }
    var showEditAvatarPreview by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val hasUnsavedChanges = name != userProfile.name ||
        contact != userProfile.contact ||
        avatar != userProfile.avatar ||
        bankName != (defaultOwnerAccount?.accountName ?: "") ||
        accNum != (defaultOwnerAccount?.accountNumber ?: "") ||
        ifsc != (defaultOwnerAccount?.ifscCode ?: "") ||
        branch != (defaultOwnerAccount?.branchName ?: "") ||
        upi != (defaultOwnerAccount?.upiId ?: "") ||
        qrUri != (defaultOwnerAccount?.qrCodeUri ?: "")

    val handleBack = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onDismiss()
        }
    }
    androidx.activity.compose.BackHandler { handleBack() }

    if (showDiscardDialog) {
        UnsavedChangesWarningDialog(
            isBengali = userProfile.language == "bn",
            onKeepEditing = { showDiscardDialog = false },
            onDiscard = {
                showDiscardDialog = false
                onDismiss()
            }
        )
    }

    if (showEditAvatarPreview && avatar.isNotBlank()) {
        FullScreenImagePreviewDialog(
            imageUri = avatar,
            title = name.ifBlank { "Profile Photo" },
            onDismiss = { showEditAvatarPreview = false }
        )
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { avatar = com.example.ui.components.copyUriToPersistentFile(context, it, "AVATAR") ?: avatar }
    }

    val qrPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { qrUri = com.example.ui.components.copyUriToPersistentFile(context, it, "QR") ?: qrUri }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { handleBack() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalAppTheme provides userProfile.themeMode,
                LocalAppBgPreset provides userProfile.bgPreset,
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
                            .background(getAppBackgroundGradient(userProfile.themeMode, userProfile.bgPreset))
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
                                        title = "Edit Owner Profile",
                                        onBack = { handleBack() },
                                        trailingContent = {
                                            FilledIconButton(
                                                onClick = { onSave(name, contact, avatar, bankName, accNum, ifsc, branch, upi, qrUri) },
                                                modifier = Modifier.size(40.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = AppAccentColor(),
                                                    contentColor = AppOnAccentColor()
                                                )
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Save", tint = AppOnAccentColor())
                                            }
                                        }
                                    )
                                }
                            }
                        ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 680.dp)
                                .verticalScroll(rememberScrollState())
                                .imePadding()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(if (isAppLightMode()) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.1f))
                                        .clickable {
                                            if (avatar.isNotBlank()) {
                                                showEditAvatarPreview = true
                                            } else {
                                                photoPicker.launch("image/*")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatar.isNotBlank()) {
                                        AsyncImage(model = avatar, contentDescription = "Tap to preview", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Default.CameraAlt, null, tint = AppTextColor().copy(alpha = 0.6f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(onClick = { photoPicker.launch("image/*") }) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (avatar.isNotBlank()) "Change Profile Photo (≤2MB)" else "Upload Profile Photo (≤2MB)",
                                        color = AppAccentColor(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
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
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )

                            HorizontalDivider(color = getGlassBorder().copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Profile Linked Bank & UPI Details",
                                color = AppHeadingColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            GlassTextField(
                                value = bankName,
                                onValueChange = { bankName = it },
                                label = { Text("Bank Name / Account Label") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            GlassTextField(
                                value = accNum,
                                onValueChange = { accNum = it },
                                label = { Text("Account Number") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    GlassTextField(
                                        value = ifsc,
                                        onValueChange = { ifsc = it },
                                        label = { Text("IFSC Code") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    GlassTextField(
                                        value = branch,
                                        onValueChange = { branch = it },
                                        label = { Text("Branch") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }

                            GlassTextField(
                                value = upi,
                                onValueChange = { upi = it },
                                label = { Text("UPI ID (e.g. name@upi)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 80.dp)
                                    .clickable { qrPicker.launch("image/*") },
                                shape = RoundedCornerShape(16.dp),
                                color = getCardBackground(),
                                shadowElevation = if (isAppLightMode()) 2.dp else 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (qrUri.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(8.dp)) {
                                            AsyncImage(model = qrUri, contentDescription = "QR Code", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
                                            Text("UPI QR Code Attached (Tap to change)", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                                            Icon(Icons.Default.QrCode, contentDescription = null, tint = AppAccentColor())
                                            Text("+ Upload UPI QR Code Image", color = AppAccentColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }
                }
            }
        }
    }
}
}
}

@Composable
fun EmptyStateCard(message: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Inbox, null, tint = AppAccentColor().copy(alpha = 0.7f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = message,
                    color = AppSecondaryTextColor(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun OwnerAccountCard(
    viewModel: com.example.ui.viewmodel.FinancialViewModel,
    account: com.example.data.model.OwnerAccountEntity,
    isDefault: Boolean = false,
    isVisibleOnExtra: Boolean = false,
    onSetDefault: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showQRDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showViewDetailsModal by remember { mutableStateOf(false) }
    var showDelinkDialog by remember { mutableStateOf(false) }
    val isLinkedToExtras = account.linkedBankAccountId != null

    val exportAccountCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && account.linkedBankAccountId != null) {
            viewModel.exportDataCsv(
                type = com.example.data.BackupDataType.SINGLE_BANK_ACCOUNT,
                bankAccountId = account.linkedBankAccountId
            ) { csv ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    }
                    android.widget.Toast.makeText(context, "Transaction history downloaded! Account de-linked.", android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.delinkOwnerAccountFromExtras(account, moveExtrasAccountToBin = true)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showDelinkDialog) {
        DelinkAccountFromExtrasDialog(
            accountName = account.accountName.ifBlank { account.ownerName },
            onMoveToBin = {
                showDelinkDialog = false
                viewModel.delinkOwnerAccountFromExtras(account, moveExtrasAccountToBin = true)
                android.widget.Toast.makeText(context, "Extras account & history moved to Recycle Bin", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDownloadHistory = {
                showDelinkDialog = false
                val safeName = account.accountName.ifBlank { "account" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                exportAccountCsvLauncher.launch("${safeName}_history.csv")
            },
            onOpenExportTool = {
                showDelinkDialog = false
                viewModel.delinkOwnerAccountFromExtras(account, moveExtrasAccountToBin = false)
                android.widget.Toast.makeText(
                    context,
                    "Account de-linked. Open Settings > Data & Backups to export (Note: only QR, JSON, CSV, or Copy-Paste exports exclude media attachments).",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            },
            onDismiss = { showDelinkDialog = false }
        )
    }

    if (showViewDetailsModal) {
        ViewAccountDialog(viewModel = viewModel,
            account = account,
            onDismiss = { showViewDetailsModal = false },
            onEdit = onEdit,
            onShowQR = if (account.qrCodeUri.isNotBlank()) { { showQRDialog = true } } else null
        )
    }

    if (showDeleteConfirmation) {
        if (com.example.ui.components.SecretDeleteModeManager.isActive()) {
            LaunchedEffect(Unit) {
                showDeleteConfirmation = false
                onDelete()
            }
        } else {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Move to Recycle Bin?", color = AppTextColor(), fontWeight = FontWeight.Bold) },
                text = { Text("Move account to Recycle Bin? You can restore it anytime from the Bin.", color = AppTextColor()) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            onDelete()
                        }
                    ) {
                        Text("Move to Bin", color = AccentPink, fontWeight = FontWeight.Bold)
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
    }

    if (showQRDialog && account.qrCodeUri.isNotBlank()) {
        FullScreenQRPreviewDialog(
            qrUri = account.qrCodeUri,
            title = account.accountName.ifBlank { "Account QR" },
            upiId = account.upiId,
            onDismiss = { showQRDialog = false }
        )
    }

    GlassCard(cornerRadius = 14.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { showViewDetailsModal = true }
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppAccentColor().copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = AppAccentColor(),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.accountName.ifBlank { "Profile Account" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AppTextColor(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isDefault) {
                            Surface(
                                color = if (isAppLightMode()) Color(0xFFFEF3C7) else Color(0xFF451A03),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Default", color = AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                        Surface(
                            color = if (isLinkedToExtras) AccentGreen.copy(alpha = 0.15f) else AppTextColor().copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLinkedToExtras) Icons.Default.Link else Icons.Default.LinkOff,
                                    contentDescription = null,
                                    tint = if (isLinkedToExtras) AccentGreen else AppSecondaryTextColor(),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isLinkedToExtras) "Linked to Extras" else "Profile Only",
                                    color = if (isLinkedToExtras) AccentGreen else AppSecondaryTextColor(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (account.upiId.isNotBlank()) account.upiId
                               else if (account.accountNumber.isNotBlank()) "A/C: •••• ${account.accountNumber.takeLast(4)}"
                               else "Profile Payment Account",
                        fontSize = 12.sp,
                        color = AppSecondaryTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = AppSecondaryTextColor(),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    offset = androidx.compose.ui.unit.DpOffset(x = (-12).dp, y = 4.dp),
                    modifier = Modifier
                        .background(getDialogBackground())
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Account", color = AppTextColor(), fontWeight = FontWeight.Medium) },
                        onClick = {
                            showOverflowMenu = false
                            onEdit()
                        }
                    )
                    if (!isDefault) {
                        DropdownMenuItem(
                            text = { Text("Set as Default Profile Account", color = AccentGold, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                showOverflowMenu = false
                                onSetDefault()
                            }
                        )
                    }
                    if (isLinkedToExtras) {
                        DropdownMenuItem(
                            text = { Text("De-link from Extras Page", color = AccentPink, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                showOverflowMenu = false
                                showDelinkDialog = true
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Link to Extras Page (2-Way Sync)", color = AccentGreen, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                showOverflowMenu = false
                                viewModel.linkOwnerAccountToExtras(account)
                                android.widget.Toast.makeText(context, "Linked to Extras page with 2-way sync!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    if (account.qrCodeUri.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text("Show QR Code", color = AppTextColor(), fontWeight = FontWeight.Medium) },
                            onClick = {
                                showOverflowMenu = false
                                showQRDialog = true
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("View Account Details", color = AppTextColor(), fontWeight = FontWeight.Medium) },
                        onClick = {
                            showOverflowMenu = false
                            showViewDetailsModal = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Account", color = AccentPink, fontWeight = FontWeight.Medium) },
                        onClick = {
                            showOverflowMenu = false
                            showDeleteConfirmation = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DelinkAccountFromExtrasDialog(
    accountName: String,
    onMoveToBin: () -> Unit,
    onDownloadHistory: () -> Unit,
    onOpenExportTool: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LinkOff, contentDescription = null, tint = AccentPink, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "De-link '$accountName' from Extras?",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "This account remains in your Owner Profile, but what should happen to its Extras ledger and transaction history data?",
                    color = AppSecondaryTextColor(),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppAccentColor().copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "ℹ️ Media Restriction Note: Only QR, JSON, CSV, or Copy-Paste exports exclude media attachments for compact size. Full ZIP backups include all media.",
                        color = AppTextColor().copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Button(
                    onClick = onMoveToBin,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPink.copy(alpha = 0.16f),
                        contentColor = AccentPink
                    )
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Move Extras Data & History to Bin", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onDownloadHistory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen.copy(alpha = 0.16f),
                        contentColor = AccentGreen
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Transaction History (CSV)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onOpenExportTool,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppAccentColor().copy(alpha = 0.16f),
                        contentColor = AppAccentColor()
                    )
                ) {
                    Icon(Icons.Default.ImportExport, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("De-link & Use App Export Tool", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppSecondaryTextColor())
            }
        }
    )
}

@Composable
fun DetailRowItem(
    label: String,
    rawValue: String,
    displayValue: String = rawValue
) {
    if (rawValue.isBlank() && displayValue.isBlank()) return
    val context = LocalContext.current
    var showLargeView by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .pointerInput(rawValue) {
                detectTapGestures(
                    onTap = { showLargeView = true },
                    onDoubleTap = {
                        val clip = android.content.ClipData.newPlainText(label, rawValue)
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Copied $label to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .padding(vertical = 2.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = AppSecondaryTextColor(),
            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
        )
        Text(
            displayValue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextColor(),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }

    if (showLargeView) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showLargeView = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = getDialogBackground(),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(label, color = AppSecondaryTextColor(), fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(rawValue, color = AppTextColor(), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Double tap row to copy", color = AppAccentColor(), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = { showLargeView = false }) { Text("Close", color = AppAccentColor(), fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun AddOwnerAccountDialog(
    viewModel: com.example.ui.viewmodel.FinancialViewModel,
    account: com.example.data.model.OwnerAccountEntity?,
    onDismiss: () -> Unit,
    onSave: (com.example.data.model.OwnerAccountEntity, Double, String) -> Unit
) {
    androidx.activity.compose.BackHandler { onDismiss() }

    var ownerName by remember { mutableStateOf(account?.ownerName ?: "") }
    var accountName by remember { mutableStateOf(account?.accountName ?: "") }
    var accountNumber by remember { mutableStateOf(account?.accountNumber ?: "") }
    var ifscCode by remember { mutableStateOf(account?.ifscCode ?: "") }
    var branchName by remember { mutableStateOf(account?.branchName ?: "") }
    var upiId by remember { mutableStateOf(account?.upiId ?: "") }
    var qrCodeUri by remember { mutableStateOf(account?.qrCodeUri ?: "") }
    var initialBalanceStr by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    val qrPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { qrCodeUri = com.example.ui.components.copyUriToPersistentFile(context, it, "QR") }
    }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val themeMode = userProfile.themeMode
    val bgPreset = userProfile.bgPreset
    val customBgUri = userProfile.customBgUri
    val isLiquidGlass = userProfile.enableLiquidGlassTheme

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()
        Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalAppTheme provides themeMode,
            LocalAppBgPreset provides bgPreset,
            LocalAppCustomBgUri provides customBgUri,
            LocalAppLiquidGlassTheme provides isLiquidGlass
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
                                    title = if (account == null) "Add Account" else "Edit Account Details",
                                    onBack = onDismiss,
                                    trailingContent = {
                                        FilledIconButton(
                                            onClick = {
                                                val bal = initialBalanceStr.toDoubleOrNull() ?: 0.0
                                                if (accountName.isNotBlank()) {
                                                    onSave(
                                                        OwnerAccountEntity(
                                                            id = account?.id ?: 0,
                                                            ownerName = ownerName,
                                                            accountName = accountName,
                                                            accountNumber = accountNumber,
                                                            ifscCode = ifscCode,
                                                            branchName = branchName,
                                                            upiId = upiId,
                                                            qrCodeUri = qrCodeUri
                                                        ),
                                                        bal,
                                                        details
                                                    )
                                                } else {
                                                    android.widget.Toast.makeText(context, "Enter Account Name", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(40.dp),
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = AppAccentColor(),
                                                contentColor = AppOnAccentColor()
                                            )
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Save", tint = AppOnAccentColor())
                                        }
                                    }
                                )
                            }
                        }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 680.dp)
                                .verticalScroll(rememberScrollState())
                                .imePadding()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            GlassTextField(
                                value = accountName,
                                onValueChange = { accountName = it },
                                label = { Text("Account Name (e.g. Cash, HDFC)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )

                            GlassTextField(
                                value = initialBalanceStr,
                                onValueChange = { initialBalanceStr = it },
                                label = { Text("Account Balance (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                            )

                            GlassTextField(
                                value = details,
                                onValueChange = { details = it },
                                label = { Text("Account Details (Notes / Purpose)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Account Image / QR Code", fontWeight = FontWeight.Bold, color = AppTextColor(), fontSize = 13.sp)

                            if (qrCodeUri.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(64.dp),
                                        shape = RoundedCornerShape(10.dp),
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

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilledTonalButton(
                                            onClick = { qrPicker.launch("image/*") },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = AppAccentColor().copy(alpha = 0.14f),
                                                contentColor = AppAccentColor()
                                            )
                                        ) {
                                            Text("Change Image", fontSize = 11.sp)
                                        }

                                        TextButton(
                                            onClick = { qrCodeUri = "" },
                                            colors = ButtonDefaults.textButtonColors(contentColor = AccentPink)
                                        ) {
                                            Text("Remove", fontSize = 11.sp)
                                        }
                                    }
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { qrPicker.launch("image/*") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 46.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = AppAccentColor().copy(alpha = 0.14f),
                                        contentColor = AppAccentColor()
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Image / QR Code", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Optional Bank & UPI Details", fontWeight = FontWeight.Bold, color = AppTextColor().copy(alpha = 0.7f), fontSize = 12.sp)

                            GlassTextField(
                                value = ownerName,
                                onValueChange = { ownerName = it },
                                label = { Text("Account Holder Name") },
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
                            GlassTextField(
                                value = upiId,
                                onValueChange = { upiId = it },
                                label = { Text("UPI ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                            Spacer(modifier = Modifier.navigationBarsPadding())
                        }
                    }
                }
            }
        }
    }
}
}
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OwnerQRViewerDialog(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit,
    onOpenOwnerProfile: (() -> Unit)? = null
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val ownerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle()

    val ownerAccount = ownerAccounts.find { it.id == userProfile.defaultOwnerAccountId && !it.isDeleted }
        ?: ownerAccounts.firstOrNull { !it.isDeleted && it.qrCodeUri.isNotBlank() }
        ?: ownerAccounts.firstOrNull { !it.isDeleted }

    val qrUri = ownerAccount?.qrCodeUri.orEmpty()
    val upiId = ownerAccount?.upiId.orEmpty()
    val bankLabel = ownerAccount?.accountName.orEmpty()
    val accNumber = ownerAccount?.accountNumber.orEmpty()
    val ifscCode = ownerAccount?.ifscCode.orEmpty()
    val ownerName = userProfile.name.ifBlank { ownerAccount?.ownerName.orEmpty().ifBlank { "Owner Payment Details" } }
    val hasAnyPaymentDetails = qrUri.isNotBlank() || upiId.isNotBlank() || accNumber.isNotBlank() || bankLabel.isNotBlank()

    if (!hasAnyPaymentDetails) {
        androidx.activity.compose.BackHandler { onDismiss() }
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable { onDismiss() }
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .clickable(enabled = false) {},
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = AppAccentColor(),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Default Profile Payment Account",
                            color = AppTextColor(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add a payment account or UPI QR code in your Owner Profile and set it as Default to display it here when double-tapping the greeting.",
                            color = AppSecondaryTextColor(),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenOwnerProfile?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor(), contentColor = AppOnAccentColor()),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open Owner Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        MinimalistPaymentInfoDialog(
            title = ownerName,
            subtitle = bankLabel.takeIf { it.isNotBlank() },
            badgeLabel = "Default Profile Account",
            qrUriString = qrUri,
            upiId = upiId,
            detailRows = listOfNotNull(
                accNumber.takeIf { it.isNotBlank() }?.let { PaymentInfoRowItem("Account No", it) },
                ifscCode.takeIf { it.isNotBlank() }?.let { PaymentInfoRowItem("IFSC Code", it) },
                ownerAccount?.branchName?.takeIf { it.isNotBlank() }?.let { PaymentInfoRowItem("Branch", it) }
            ),
            contactChips = listOfNotNull(
                ownerAccount?.ownerMobile?.takeIf { it.isNotBlank() }?.let { PaymentContactChip("Ph", it) },
                ownerAccount?.whatsappNumber?.takeIf { it.isNotBlank() }?.let { PaymentContactChip("WA", it) },
                ownerAccount?.email?.takeIf { it.isNotBlank() }?.let { PaymentContactChip("Email", it, isEmail = true) }
            ),
            onDismiss = onDismiss,
            onEdit = onOpenOwnerProfile?.let { openProfile ->
                {
                    onDismiss()
                    openProfile()
                }
            }
        )
    }
}

@Composable
fun SelectVisibleAccountsOnExtraDialog(
    accounts: List<com.example.data.model.OwnerAccountEntity>,
    visibleIds: Set<Int>,
    prefLanguage: String = "en",
    onDismiss: () -> Unit,
    onSave: (Set<Int>) -> Unit
) {
    androidx.activity.compose.BackHandler { onDismiss() }

    var selectedIds by remember { mutableStateOf(visibleIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (prefLanguage == "bn") "এক্সট্রা পেজে অ্যাকাউন্ট নির্বাচন করুন" else "Choose Accounts on Extra Page",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (prefLanguage == "bn")
                        "যেসব অ্যাকাউন্ট নির্বাচন করবেন কেবল সেগুলোই এক্সট্রা পেজে প্রদর্শিত হবে। ডিফল্টভাবে নতুন অ্যাকাউন্ট হাইড থাকে।"
                    else
                        "Selected accounts will be displayed on the Extra page. Unchecked accounts will be hidden from the Extra page carousel.",
                    color = AppSecondaryTextColor(),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (accounts.isEmpty()) {
                    Text(
                        text = if (prefLanguage == "bn") "কোনো ব্যাংক অ্যাকাউন্ট পাওয়া যায়নি" else "No bank accounts found",
                        color = AppSecondaryTextColor(),
                        fontSize = 14.sp
                    )
                } else {
                    accounts.forEach { acc ->
                        val isChecked = acc.id in selectedIds
                        val displayName = acc.accountName.ifBlank { acc.ownerName.ifBlank { "Account #${acc.id}" } }
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (isChecked) selectedIds - acc.id else selectedIds + acc.id
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isChecked) {
                                if (isAppLightMode()) Color(0xFFD3E3FD) else Color(0xFF004A77)
                            } else getCardBackground(),
                            shadowElevation = if (isChecked) 3.dp else 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) selectedIds + acc.id else selectedIds - acc.id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = AppAccentColor())
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        color = AppTextColor(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val subDetail = when {
                                        acc.upiId.isNotBlank() -> "UPI: ${acc.upiId}"
                                        acc.accountNumber.isNotBlank() -> "A/C: ${acc.accountNumber}"
                                        acc.branchName.isNotBlank() -> "Branch: ${acc.branchName}"
                                        else -> "Owner: ${acc.ownerName}"
                                    }
                                    Text(
                                        text = subDetail,
                                        color = AppSecondaryTextColor(),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedIds) },
                colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor(), contentColor = AppOnAccentColor()),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (prefLanguage == "bn") "সংরক্ষণ করুন" else "Save Selection", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AppSecondaryTextColor())
            }
        }
    )
}

@Composable
fun ViewAccountDialog(
    viewModel: com.example.ui.viewmodel.FinancialViewModel,
    account: com.example.data.model.OwnerAccountEntity,
    prefLanguage: String = "en",
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onShowQR: (() -> Unit)? = null
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    MinimalistPaymentInfoDialog(
        title = account.accountName.ifBlank { account.ownerName.ifBlank { "Profile Account" } },
        subtitle = account.ownerName.takeIf { it.isNotBlank() && it != account.accountName },
        badgeLabel = if (account.linkedBankAccountId != null) "Linked to Extras" else "Profile Only",
        qrUriString = account.qrCodeUri,
        upiId = account.upiId,
        detailRows = listOfNotNull(
            account.accountNumber.takeIf { it.isNotBlank() }?.let { PaymentInfoRowItem(if (prefLanguage == "bn") "অ্যাকাউন্ট নং" else "Account No", it) },
            account.ifscCode.takeIf { it.isNotBlank() }?.let { PaymentInfoRowItem("IFSC Code", it) },
            account.branchName.takeIf { it.isNotBlank() }?.let { PaymentInfoRowItem(if (prefLanguage == "bn") "শাখা" else "Branch", it) },
            account.importedFrom?.takeIf { it.isNotBlank() }?.let { PaymentInfoRowItem(if (prefLanguage == "bn") "ইম্পোর্ট উৎস" else "Imported From", it, copyable = false) }
        ),
        contactChips = listOfNotNull(
            account.ownerMobile.takeIf { it.isNotBlank() }?.let { PaymentContactChip("Ph", it) },
            account.whatsappNumber.takeIf { it.isNotBlank() }?.let { PaymentContactChip("WA", it) },
            account.email.takeIf { it.isNotBlank() }?.let { PaymentContactChip("Email", it, isEmail = true) }
        ),
        notesText = account.notes.takeIf { it.isNotBlank() },
        onDismiss = onDismiss,
        onEdit = onEdit?.let { editFn ->
            {
                onDismiss()
                editFn()
            }
        }
    )
}

@Composable
fun AccountInfoDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    actionIcon2: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onAction2: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
            Text(value, color = AppTextColor(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        if (actionIcon2 != null && onAction2 != null) {
            IconButton(
                onClick = onAction2,
                modifier = Modifier
                    .size(34.dp)
                    .background(iconTint.copy(alpha = 0.1f), CircleShape)
                    .bounceClick()
            ) {
                Icon(actionIcon2, contentDescription = "Action", tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (onCopy != null) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .size(34.dp)
                    .background(iconTint.copy(alpha = 0.1f), CircleShape)
                    .bounceClick()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = iconTint, modifier = Modifier.size(16.dp))
            }
        }
    }
}

