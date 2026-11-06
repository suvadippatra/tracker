@file:Suppress("DEPRECATION")
package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.screens.settings.components.*
import com.example.ui.screens.settings.subscreens.ProfileSettingsScreen
import com.example.ui.screens.settings.subscreens.AppearanceSettingsScreen
import com.example.ui.screens.settings.subscreens.SecuritySettingsScreen
import com.example.ui.screens.settings.subscreens.DataBackupSettingsScreen
import com.example.ui.screens.settings.subscreens.AboutSettingsScreen
import kotlinx.coroutines.delay
import com.example.data.model.UserProfileEntity
import com.example.data.model.BankAccountEntity
import com.example.ui.components.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit,
    onOpenBin: () -> Unit,
    onOpenOwnerProfile: () -> Unit,
    onOpenUtilityConfig: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("main") }
    var contentVisible by remember { mutableStateOf(true) }
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBn = userProfile?.language == "bn"

    // Intercept back button when inside a subscreen so it navigates to "main" rather than closing the whole app
    androidx.activity.compose.BackHandler(enabled = true) {
        if (currentScreen != "main") {
            currentScreen = "main"
        } else {
            onDismiss()
        }
    }

    AppThemeProvider(viewModel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient()),
            contentAlignment = Alignment.TopCenter
        ) {
            Scaffold(
                    modifier = Modifier.fillMaxSize().dialogSystemBarsPadding(extraTop = 8.dp),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentScreen != "main") {
                                        currentScreen = "main"
                                    } else {
                                        onDismiss()
                                    }
                                },
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
                                text = when(currentScreen) {
                                    "main" -> if (isBn) "সেটিংস" else "Settings"
                                    "profile" -> if (isBn) "অ্যাকাউন্ট ও প্রোফাইল" else "Account & Profile"
                                    "appearance" -> if (isBn) "থিম ও কাস্টমাইজেশন" else "Appearance & Visuals"
                                    "security" -> if (isBn) "নিরাপত্তা ও লক" else "Security & Privacy"
                                    "data" -> if (isBn) "ডাটা ও ব্যাকআপ" else "Data & Backups"
                                    "bin" -> if (isBn) "রিসাইকেল বিন" else "Recycle Bin"
                                    "about" -> if (isBn) "অ্যাপ সম্পর্কিত ও সহায়িকা" else "About & Help"
                                    else -> "Settings"
                                },
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTextColor()
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 840.dp)
                            .fillMaxWidth()
                    ) {
                        val fastMode = isHeavyEffectsDisabled() || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                val dur = if (fastMode) 90 else 150
                                if (targetState != "main" && initialState == "main") {
                                    (slideInHorizontally(tween(dur)) { it / 8 } + fadeIn(tween(dur))) togetherWith
                                        fadeOut(tween(dur / 2))
                                } else {
                                    (slideInHorizontally(tween(dur)) { -it / 10 } + fadeIn(tween(dur))) togetherWith
                                        fadeOut(tween(dur / 2))
                                }
                            },
                            label = "settings_subscreen_transition"
                        ) { screen ->
                            when (screen) {
                                "main" -> SettingsMainList(
                                    prefLanguage = userProfile?.language ?: "en",
                                    onNavigate = { currentScreen = it },
                                    onOpenBin = { currentScreen = "bin" },
                                    onOpenOwnerProfile = onOpenOwnerProfile
                                )
                                "profile" -> ProfileSettingsScreen(viewModel, onOpenOwnerProfile, onBack = { currentScreen = "main" })
                                "appearance" -> AppearanceSettingsScreen(viewModel, onOpenUtilityConfig, onBack = { currentScreen = "main" })
                                "security" -> SecuritySettingsScreen(viewModel, onBack = { currentScreen = "main" })
                                "data" -> DataBackupSettingsScreen(viewModel, onBack = { currentScreen = "main" })
                                "bin" -> BinScreen(viewModel = viewModel, onDismiss = { currentScreen = "main" }, showHeader = false)
                                "about" -> AboutSettingsScreen(viewModel, onBack = { currentScreen = "main" })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit,
    onOpenBin: () -> Unit,
    onOpenOwnerProfile: () -> Unit,
    onOpenUtilityConfig: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        SettingsScreen(
            viewModel = viewModel,
            onDismiss = onDismiss,
            onOpenBin = onOpenBin,
            onOpenOwnerProfile = onOpenOwnerProfile,
            onOpenUtilityConfig = onOpenUtilityConfig
        )
    }
}

@Composable
fun SettingsMainList(
    prefLanguage: String = "en",
    onNavigate: (String) -> Unit,
    onOpenBin: () -> Unit,
    onOpenOwnerProfile: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PixelSettingsGroup(title = "Account & Profile") {
                PixelSettingsItem(
                    icon = Icons.Default.Person,
                    title = "Account & Owner Details",
                    subtitle = "Manage personal profile and utility config",
                    onClick = { onNavigate("profile") }
                )
            }
        }

        item {
            PixelSettingsGroup(title = "Display & Personalization") {
                PixelSettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Appearance & Visual Style",
                    subtitle = "Themes, Android 8 fast mode, accent colors",
                    onClick = { onNavigate("appearance") }
                )
            }
        }

        item {
            PixelSettingsGroup(title = "Privacy & Security") {
                PixelSettingsItem(
                    icon = Icons.Default.Security,
                    title = "Security & App Lock",
                    subtitle = "PIN lock, biometrics, auto-lock timeout",
                    onClick = { onNavigate("security") }
                )
            }
        }

        item {
            PixelSettingsGroup(title = "Data Management") {
                PixelSettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Data & Backups",
                    subtitle = "Export, Import, CSV & database management",
                    onClick = { onNavigate("data") }
                )
                PixelSettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Recycle Bin",
                    subtitle = "Restore deleted records & items",
                    onClick = { onNavigate("bin") }
                )
            }
        }

        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "অ্যাপ সম্পর্কিত ও সহায়িকা" else "About & Help") {
                PixelSettingsItem(
                    icon = Icons.Default.Info,
                    title = if (prefLanguage == "bn") "অ্যাপ পরিচিতি, ব্যবহারবিধি (Help) ও আইনি তথ্য" else "About Traker, Help Guide & Legal",
                    subtitle = if (prefLanguage == "bn") "সম্পূর্ণ ব্যবহারবিধি (Help), সংস্করণ, লাইসেন্স ও ডেভেলপার তথ্য" else "Built-in Help & User Guide, version info, licenses & privacy",
                    onClick = { onNavigate("about") }
                )
            }
        }
    }
}

@Composable
fun UnifiedAccountDialog(
    account: BankAccountEntity?,
    prefLanguage: String,
    accountsCount: Int,
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit,
    onSave: (name: String, balance: Double, details: String, upiId: String, qrCodeUri: String, accountNumber: String, ifsc: String, branchName: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSaveExtended: ((name: String, balance: Double, details: String, upiId: String, qrCodeUri: String, accountNumber: String, ifsc: String, branchName: String, notes: String, phoneNumber: String, whatsappNumber: String, email: String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    androidx.activity.compose.BackHandler { onDismiss() }

    val context = androidx.compose.ui.platform.LocalContext.current
    var accName by remember(account) { mutableStateOf(account?.name ?: "") }
    var accBal by remember(account) { mutableStateOf(account?.let { if (it.initialBalance != 0.0) it.initialBalance.toString() else it.balance.toString() } ?: "") }
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
            qrCodeUri = com.example.ui.components.copyUriToPersistentFile(context, uri, "QR")
        }
    }

    if (showDeleteConfirm) {
        com.example.ui.components.ConfirmDeleteDialog(
            itemName = accName,
            requireTypedConfirmation = true,
            onConfirm = { onDelete?.invoke(); showDeleteConfirm = false; onDismiss() },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    androidx.activity.compose.BackHandler { onDismiss() }

    val themeMode = com.example.ui.components.LocalAppTheme.current
    val bgPreset = com.example.ui.components.LocalAppBgPreset.current
    val customBgUri = com.example.ui.components.LocalAppCustomBgUri.current
    val isLiquidGlass = com.example.ui.components.LocalAppLiquidGlassTheme.current

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()
        CompositionLocalProvider(
            com.example.ui.components.LocalAppTheme provides themeMode,
            com.example.ui.components.LocalAppBgPreset provides bgPreset,
            com.example.ui.components.LocalAppCustomBgUri provides customBgUri,
            com.example.ui.components.LocalAppLiquidGlassTheme provides isLiquidGlass
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(com.example.ui.components.getAppBackgroundGradient(themeMode, bgPreset)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .dialogSystemBarsPadding(extraTop = 8.dp)
                            .imePadding(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                Icons.Default.ChevronLeft,
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
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        FilledIconButton(
                            onClick = {
                                if (accName.isNotBlank()) {
                                    val bal = accBal.toDoubleOrNull() ?: 0.0
                                    if (onSaveExtended != null) {
                                        onSaveExtended(accName, bal, details, upiId, qrCodeUri, accountNumber, ifsc, branchName, notes, phoneNumber, whatsappNumber, email)
                                    } else {
                                        onSave(accName, bal, details, upiId, qrCodeUri, accountNumber, ifsc, branchName)
                                    }
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        if (prefLanguage == "bn") "অ্যাকাউন্টের নাম দিন" else "Enter Account Name",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("save_account_btn"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = AppAccentColor(),
                                contentColor = AppOnAccentColor()
                            )
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save",
                                tint = AppOnAccentColor()
                            )
                        }

                        if (account != null && accountsCount > 1 && onDelete != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            var showTopMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                IconButton(
                                    onClick = { showTopMenu = true },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = AppTextColor()
                                    )
                                }
                                DropdownMenu(
                                    expanded = showTopMenu,
                                    onDismissRequest = { showTopMenu = false },
                                    offset = androidx.compose.ui.unit.DpOffset(x = (-12).dp, y = 4.dp),
                                    modifier = Modifier.background(getDialogBackground())
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (prefLanguage == "bn") "মুছে ফেলুন" else "Delete Account", color = AccentPink, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            showTopMenu = false
                                            showDeleteConfirm = true
                                        }
                                    )
                                }
                            }
                        }
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
                        .widthIn(max = 640.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Account Name
                    GlassTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text(if (prefLanguage == "bn") "অ্যাকাউন্টের নাম" else "Account Name") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                    )

                    // Account Initial Balance
                    Column(modifier = Modifier.fillMaxWidth()) {
                        GlassTextField(
                            value = accBal,
                            onValueChange = { accBal = it },
                            label = {
                                Text(if (prefLanguage == "bn") "প্রারম্ভিক ব্যালেন্স (₹)" else "Initial Balance (₹)")
                            },
                            placeholder = { Text("0.00") },
                            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("account_balance_input")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (prefLanguage == "bn") "আয়-ব্যয়ের হিসাবের জন্য মূল প্রারম্ভিক ব্যালেন্স" else "Initial opening balance for tracking income & expense status",
                            color = AppTextColor().copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    // Account Details (Notes / Description)
                    GlassTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text(if (prefLanguage == "bn") "অ্যাকাউন্ট বিবরণ / নোট" else "Account Details (Notes / Purpose)") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                    )

                    // Add Image / QR Code Section
                    Surface(
                        color = getSettingsItemBg(),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = AppAccentColor().copy(alpha = 0.14f),
                                        contentColor = AppAccentColor()
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (prefLanguage == "bn") "ছবি বা QR কোড যোগ করুন" else "Add Image / QR Code", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // Optional Bank / Payment Info
                    Text(
                        text = if (prefLanguage == "bn") "ঐচ্ছিক ব্যাংক / UPI বিবরণ (যদি থাকে):" else "Optional Bank & UPI Details (If applicable):",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

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
                                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
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
                        label = { Text("UPI ID") },
                        leadingIcon = { Icon(Icons.Default.Payment, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (prefLanguage == "bn") "যোগাযোগ ও সংক্ষিপ্ত নোট (ঐচ্ছিক):" else "Contact Options & Short Notes (Optional):",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            GlassTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text(if (prefLanguage == "bn") "ফোন / SMS নম্বর" else "Phone / SMS Number") },
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
                                label = { Text(if (prefLanguage == "bn") "হোয়াটসঅ্যাপ নম্বর" else "WhatsApp Number") },
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
                        label = { Text(if (prefLanguage == "bn") "ইমেইল ঠিকানা" else "Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    GlassTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (prefLanguage == "bn") "সংক্ষিপ্ত নোট (অ্যাকাউন্ট ইনফোতে দেখাবে)" else "Short Notes (Displayed on Account Info)") },
                        leadingIcon = { Icon(Icons.Default.StickyNote2, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}
}
}
}

@Composable
fun LegalInfoDialog(
    type: String,
    prefLanguage: String,
    appVersion: String = "0.0.1",
    onDismiss: () -> Unit
) {
    androidx.activity.compose.BackHandler { onDismiss() }

    val title = when (type) {
        "info" -> if (prefLanguage == "bn") "ব্যবহারবিধি ও অ্যাপ তথ্য (Help Guide)" else "Help & User Guide"
        "license" -> if (prefLanguage == "bn") "লাইসেন্স ও শর্তাবলী" else "Licenses & Terms"
        "privacy" -> if (prefLanguage == "bn") "প্রাইভেসি পলিসি" else "Privacy Policy"
        else -> ""
    }

    val content = when (type) {
        "info" -> if (prefLanguage == "bn") """
            Traker v$appVersion (Khata Optical Scanner, Word-Processor Sticky Notes, Inline Menu Editor & Android 8 Speed Boost)
            ডেভেলপার: @suvadippatra (GitHub)
            © ২০২৬ @suvadippatra. সর্বস্বত্ব সংরক্ষিত।

            ───────────────────────────
            📖 সম্পূর্ণ ব্যবহারবিধি ও গাইড (User Guide)
            ───────────────────────────

            ১. 📌 "Sticky" ট্যাব: গুগল কিপ ধাঁচের নোট, লাইন-ভিত্তিক অ্যালাইনমেন্ট ও ভিজ্যুয়াল গ্রিড টেবিল
            • স্ক্রিন সীমার মধ্যে ব্যাকগ্রাউন্ড ওবলিং: মোবাইল ও ট্যাবলেট উভয় মোডেই সমস্ত স্টিকি কার্ড সম্পূর্ণ স্ক্রিনের ভেতরে থাকে। কার্ডের ওপরের লেখা স্থির রেখে শুধুমাত্র পেছনের কার্ড ব্যাকগ্রাউন্ড দুলতে থাকে (Illusion Wobble), ফলে লেখা পড়তে কোনো অসুবিধা হয় না।
            • শুধুমাত্র এডিট মোডে কন্ট্রোল ও ফিক্সড অ্যাটাচমেন্ট: প্রিভিউ মোডে কোনো এডিটিং টুলবার বা বাটন দেখায় না—এডিট আইকনে ট্যাপ করলেই নিচে ৮-আইকন টুলবার ও ট্যাগ কন্ট্রোল আসে এবং এডিটরের নিচে কোনো ডুপ্লিকেট প্রিভিউ দেখায় না। ফাইল বা ছবি অ্যাটাচ করলে তা সরাসরি ডাটাবেসে সেভ হয়।
            • লাইন-ভিত্তিক অ্যালাইনমেন্ট ও ভিজ্যুয়াল টেবিল গ্রিড: ওয়ার্ড প্রসেসর অ্যাপের মতো কার্সার যে লাইনে আছে শুধুমাত্র সেই লাইন বা প্যারাগ্রাফে Left, Center, Right বা Justify অ্যালাইনমেন্ট প্রযোজ্য হয়। টেবিলগুলো ইনলাইন মার্কডাউনের বদলে সরাসরি ইন্টারঅ্যাক্টিভ গ্রিড হিসেবে এডিট করা যায়।

            ২. 📷 "Scan Khata to Import" (২-ধাপে র অপটিক্যাল স্ক্যান ও মেনু মিলকরণ)
            • ধাপ ১ (র স্ক্যানিং পেজ): কোনো প্রি-রিটেন টেক্সট বা মাসের টগল ছাড়াই সরাসরি ক্যামেরা বা গ্যালারি থেকে খাতার ছবি স্ক্যান করে `তারিখ - দাম১+দাম২` (যেমন `17 - 40+60`) লাইন শনাক্ত করা হয়।
            • ধাপ ২ (তারিখ যাচাই ও মেনু অনুমান): ২য় ধাপে হোটেল ও মাস নির্বাচন করলে কোন কোন তারিখ ইতিমধ্যে মিল লেজারে আছে তা দেখায় এবং `+` দিয়ে লেখা প্রতিটি দামকে হোটেলের মেনুর সাথে মিলিয়ে ১–৪টি অনুমান বা কাস্টম নাম দেওয়ার সুবিধা দেয়।

            ৩. 🍽️ হোটেল এডিট পেজে ইনলাইন মেনু এডিটর ও দাম পরিবর্তনের কার্যকর তারিখ (Effective Date)
            • হোটেল এডিট করার সময় একই পেজ থেকে সরাসরি মেনু আইটেম যোগ বা এডিট করা যায়, এবং ফুল মেনু এডিটর থেকে Back চাপলে হোটেল এডিট পেজটি বন্ধ না হয়ে সেখানেই ফিরে আসে।
            • যেকোনো মেনু আইটেমের দাম পরিবর্তনের সময় কোন তারিখ (Effective Date) থেকে নতুন দাম কার্যকর হবে তা নির্ধারণ করা যায় এবং তা মেনু ইতিহাসে সংরক্ষিত থাকে।

            ৪. ⚡ বিদ্যুৎ বিল ক্যালকুলেটর ও রেট পলিসি (ডিফল্ট রেট ছাড়া)
            • নির্বাচিত Start Date এবং End Date-এর মধ্যকার রিডিং অনুযায়ী নিখুঁতভাবে ইউনিট হিসাব করা হয় এবং রেট পলিসি পরিবর্তনের তারিখ অনুযায়ী গ্রুপ ভাগ ও লিনিয়ার ইন্টারপোলেশন করা হয়।

            ৫. 🚀 Android 8 ল্যাগ ফিক্স, তাৎক্ষণিক ডাটা লোডিং ও অ্যাপ সংস্করণ লক
            • Android 8 ও পুরনো ডিভাইসে টাচ রেসপন্স ও স্ক্রলিং মাখনের মতো মসৃণ করতে শ্যাডো লেয়ার ও ৬০ FPS রিকম্পোজিশন অপটিমাইজ করা হয়েছে এবং পেজ ট্রানজিশন ও ডাটা লোডিং তাৎক্ষণিক করা হয়েছে।
            • অ্যাপের সংস্করণ (App Version) এখন সম্পূর্ণ রিড-ওনলি; অ্যাপের ভেতর থেকে এটি পরিবর্তন করা যায় না।

            👨‍💻 ডেভেলপার ও ওপেন সোর্স:
            GitHub: @suvadippatra (https://github.com/suvadippatra)
            লাইসেন্স: Apache License 2.0
        """.trimIndent() else """
            Traker v$appVersion (Khata Optical Scanner, Word-Processor Sticky Notes, Inline Menu Editor & Android 8 Speed Boost)
            Developed by @suvadippatra (GitHub)
            © 2026 @suvadippatra. All rights reserved.

            ───────────────────────────
            📖 COMPLETE USER GUIDE & LATEST FEATURES
            ───────────────────────────

            1. 📌 "STICKY" TAB: BACKGROUND-ONLY WOBBLE, PER-LINE ALIGNMENT & VISUAL GRID TABLES
            • In-Bounds Tablet Layout & Background-Only Wobble: All sticky note columns fit strictly within the screen bounds on both phones and tablets. Only the background card surface wobbles while the foreground text and tables stay rock-steady for effortless reading.
            • Edit-Mode-Only Controls & Reliable Attachments: Formatting controls, tag editors, and table controls appear strictly in Editing Mode (never in Preview Mode), and the redundant bottom preview box has been removed. Attached files/images persist immediately to the database.
            • Word-Processor Line Alignment & Interactive Table Grid: Alignment buttons (Left, Center, Right, Justify) align only the specific line/sentence at your cursor rather than the entire note. Tables render as interactive visual grids directly inside the editor instead of raw pipe markdown.

            2. 📷 "SCAN KHATA TO IMPORT" (2-STEP RAW OPTICAL SCAN & MENU MATCHING)
            • Step 1 (Raw Scanning Page): Starts with a clean, empty state and no month toggle. Capture or pick a handwritten Khata image to segment rows and recognize `Date - Price1+Price2` lines (e.g., `17 - 40+60`).
            • Step 2 (Captured Date Check & Menu Solver): Select target Hotel and Month on Step 2 to see which dates are already recorded in your Meal Ledger and match each `+`-separated price token to menu combinations (1–4 guesses or custom dish input).

            3. 🍽️ INLINE MENU EDITING IN HOTEL EDIT, BACK NAVIGATION & EFFECTIVE DATE
            • Edit menu items directly inside the Edit Hotel dialog or open the full Menu Editor—pressing Back from the Menu Editor now returns cleanly to Edit Hotel instead of closing the screen.
            • When editing a menu item's price, select the Effective From Date for the price change so historical records and modification history reflect the exact date.

            4. ⚡ STRICT DATE-RANGE BILL CALCULATOR & RATE POLICY GROUPING
            • Strictly calculates kWh consumption between your selected Start Date and End Date, splitting pro-rata across rate policy changes and linearly interpolating partial end ranges.

            5. 🚀 ANDROID 8 PERFORMANCE ENGINE, INSTANT DATA LOADING & LOCKED VERSION
            • Eliminated per-frame card recompositions and heavy offscreen shadow rasterization on Android 8 for instant touch response, zero-delay data loading (`SharingStarted.Eagerly`), and smooth page transitions.
            • App Version is strictly read-only and bound to the official build configuration.

            👨‍💻 Developer & Open Source:
            GitHub: @suvadippatra (https://github.com/suvadippatra)
            License: Apache License 2.0
        """.trimIndent()
        "license" -> if (prefLanguage == "bn") """
            এই অ্যাপ্লিকেশনটি Apache License 2.0 এর অধীনে লাইসেন্সপ্রাপ্ত।
            
            ব্যবহৃত ওপেন-সোর্স লাইব্রেরি ও ডাটাবেস:
            - Android Jetpack Compose & Material Design 3
            - AndroidX Room SQLite Database (Schema v36)
            - Google Fonts: Archivo Black & Archivo (SIL OFL 1.1)
            - Kotlin Coroutines & Flow
            - ZXing QR Engine & CameraX
            - Coil Image Loader
        """.trimIndent() else """
            This application is licensed under the Apache License 2.0.
            
            Open-source repositories, databases & assets used:
            - Android Jetpack Compose & Material Design 3
            - AndroidX Room SQLite Database (Schema v36)
            - Google Fonts: Archivo Black & Archivo (SIL OFL 1.1)
            - Kotlin Coroutines & Flow
            - ZXing QR Engine & CameraX
            - Coil Image Loader
        """.trimIndent()
        "privacy" -> if (prefLanguage == "bn") """
            আপনার প্রাইভেসি আমাদের কাছে সবচেয়ে গুরুত্বপূর্ণ।
            
            Traker একটি ১০০% অফলাইন-ফার্স্ট অ্যাপ্লিকেশন:
            ১. আপনার সমস্ত তথ্য শুধুমাত্র আপনার ডিভাইসের রুম ডাটাবেসে সংরক্ষিত থাকে।
            ২. কোনো আর্থিক তথ্য বা ব্যক্তিগত ডেটা কোনো সার্ভারে পাঠানো হয় না।
            ৩. গিটহাব বা পাবলিক সোর্স কোডে কোনো ব্যক্তিগত তথ্য বা API কী রাখা হয়নি।
        """.trimIndent() else """
            Your privacy is our priority.
            
            Traker is a 100% OFFLINE-FIRST application:
            1. All your data is stored locally on your device in Room SQLite.
            2. No financial or personal data is transmitted to any external server.
            3. Zero personal information, secrets, or API keys are exposed in the repository source code.
        """.trimIndent()
        else -> ""
    }

    val isLight = isAppLightMode()
    val bgColor = if (isLight) com.example.ui.theme.M3BackgroundLight else com.example.ui.theme.M3BackgroundDark

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bgColor
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .dialogSystemBarsPadding(extraTop = 8.dp),
                containerColor = bgColor,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                        AsyncImage(
                            model = com.example.R.drawable.app_logo,
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = title,
                            color = AppTextColor(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = AppTextColor()
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = getCardBackground(),
                        shadowElevation = if (isLight) 4.dp else 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = content,
                            color = AppTextColor(),
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}


@Composable
fun SecurityPinDialog(
    mode: SecurityPinMode,
    currentSavedPin: String,
    initialQuestion: String = "",
    initialAnswer: String = "",
    prefLanguage: String,
    onDismiss: () -> Unit,
    onSuccess: (newPin: String?, enabled: Boolean, question: String, answer: String) -> Unit
) {
    androidx.activity.compose.BackHandler { onDismiss() }

    var step by remember { mutableIntStateOf(1) }
    var enteredPin1 by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Recovery Question State (for Step 3)
    val presetQuestionsEn = listOf(
        "What is your pet's name?",
        "What was your first school name?",
        "What is your favorite color?",
        "In what city were you born?",
        "Custom Question..."
    )
    val presetQuestionsBn = listOf(
        "আপনার পোষা প্রাণীর নাম কি?",
        "আপনার প্রথম স্কুলের নাম কি?",
        "আপনার প্রিয় রং কি?",
        "আপনি কোন শহরে জন্মগ্রহণ করেছেন?",
        "নিজের প্রশ্ন লিখুন..."
    )
    val presets = if (prefLanguage == "bn") presetQuestionsBn else presetQuestionsEn
    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    var customQuestionText by remember { mutableStateOf(if (initialQuestion.isNotBlank() && !presets.dropLast(1).contains(initialQuestion)) initialQuestion else "") }
    var answerText by remember { mutableStateOf(initialAnswer) }

    val activeQuestion = if (selectedPresetIndex == presets.size - 1) customQuestionText else presets[selectedPresetIndex]

    val title = when (mode) {
        SecurityPinMode.SET_NEW -> {
            if (step == 3) if (prefLanguage == "bn") "রিকভারি মেথড যোগ করুন" else "Set Recovery Method"
            else if (prefLanguage == "bn") "নতুন পিন সেট করুন" else "Set New PIN"
        }
        SecurityPinMode.DISABLE -> if (prefLanguage == "bn") "পিন লক নিষ্ক্রিয় করুন" else "Disable PIN Lock"
        SecurityPinMode.CHANGE -> {
            if (step == 4) if (prefLanguage == "bn") "রিকভারি মেথড আপডেট করুন" else "Update Recovery Method"
            else if (prefLanguage == "bn") "পিন পরিবর্তন করুন" else "Change Security PIN"
        }
    }

    val instructions = when (mode) {
        SecurityPinMode.SET_NEW -> {
            when (step) {
                1 -> if (prefLanguage == "bn") "একটি ৪-সংখ্যার সিকিউরিটি পিন লিখুন" else "Enter a new 4-digit security PIN"
                2 -> if (prefLanguage == "bn") "পিনটি নিশ্চিত করতে আবার লিখুন" else "Enter PIN again to confirm"
                else -> if (prefLanguage == "bn") "পিন ভুলে গেলে সুরক্ষার জন্য একটি রিকভারি প্রশ্ন ও উত্তর সেট করুন" else "Set a recovery question to regain access if you forget your PIN"
            }
        }
        SecurityPinMode.DISABLE -> {
            if (prefLanguage == "bn") "পাসকোড নিষ্ক্রিয় করতে আপনার বর্তমান পিনটি লিখুন" else "Enter your current PIN to disable app lock"
        }
        SecurityPinMode.CHANGE -> {
            when (step) {
                1 -> if (prefLanguage == "bn") "আপনার বর্তমান ৪-সংখ্যার পিনটি লিখুন" else "Enter your current 4-digit security PIN"
                2 -> if (prefLanguage == "bn") "একটি নতুন ৪-সংখ্যার পিন লিখুন" else "Enter a new 4-digit security PIN"
                3 -> if (prefLanguage == "bn") "নতুন পিনটি নিশ্চিত করতে আবার লিখুন" else "Enter new PIN again to confirm"
                else -> if (prefLanguage == "bn") "রিকভারি প্রশ্ন ও উত্তর আপডেট করুন" else "Update your recovery question and answer"
            }
        }
    }

    val isRecoveryStep = (mode == SecurityPinMode.SET_NEW && step == 3) || (mode == SecurityPinMode.CHANGE && step == 4)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = instructions,
                    color = AppTextColor().copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        text = {
            if (isRecoveryStep) {
                // Recovery Step (Step 3)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    Text(
                        text = if (prefLanguage == "bn") "প্রশ্ন বেছে নিন:" else "Select Question:",
                        color = AppTextColor(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    presets.forEachIndexed { index, q ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedPresetIndex = index }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = (selectedPresetIndex == index),
                                onClick = { selectedPresetIndex = index },
                                colors = RadioButtonDefaults.colors(selectedColor = AppAccentColor())
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(text = q, color = AppTextColor(), fontSize = 12.sp)
                        }
                    }

                    if (selectedPresetIndex == presets.size - 1) {
                        GlassTextField(
                            value = customQuestionText,
                            onValueChange = { customQuestionText = it; errorMessage = null },
                            label = { Text(if (prefLanguage == "bn") "আপনার প্রশ্ন লিখুন" else "Custom Question") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    GlassTextField(
                        value = answerText,
                        onValueChange = { answerText = it; errorMessage = null },
                        label = { Text(if (prefLanguage == "bn") "আপনার উত্তর লিখুন *" else "Your Answer *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = AccentPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Passcode Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        for (i in 0 until 4) {
                            val active = i < pinInput.length
                            val color = if (errorMessage != null) AccentPink else if (active) AppAccentColor() else AppTextColor().copy(alpha = 0.2f)
                            val size by animateDpAsState(if (active) 16.dp else 12.dp)

                            Box(
                                modifier = Modifier
                                    .size(size)
                                    .background(color, CircleShape)
                                    .border(0.5.dp, AppTextColor().copy(alpha = 0.4f), CircleShape)
                            )
                        }
                    }

                    // Error Message block
                    Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = AccentPink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Custom tactile numeric keypad
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "⌫")
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        keys.forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                row.forEach { digit ->
                                    Surface(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .bounceClick(),
                                        shape = CircleShape,
                                        color = getGlassIconButtonBg(),
                                        shadowElevation = 2.dp,
                                        onClick = {
                                            errorMessage = null
                                            when (digit) {
                                                "C" -> pinInput = ""
                                                "⌫" -> {
                                                    if (pinInput.isNotEmpty()) {
                                                        pinInput = pinInput.dropLast(1)
                                                    }
                                                }
                                                else -> {
                                                    if (pinInput.length < 4) {
                                                        pinInput += digit
                                                        if (pinInput.length == 4) {
                                                            val input = pinInput
                                                            when (mode) {
                                                                SecurityPinMode.SET_NEW -> {
                                                                    if (step == 1) {
                                                                        enteredPin1 = input
                                                                        pinInput = ""
                                                                        step = 2
                                                                    } else {
                                                                        if (input == enteredPin1) {
                                                                            confirmedPin = input
                                                                            pinInput = ""
                                                                            step = 3 // Move to recovery question step
                                                                        } else {
                                                                            errorMessage = if (prefLanguage == "bn") "পিন দুটি মেলেনি! অনুগ্রহ করে আবার চেষ্টা করুন।" else "PINs do not match! Please try again."
                                                                            enteredPin1 = ""
                                                                            pinInput = ""
                                                                            step = 1
                                                                        }
                                                                    }
                                                                }
                                                                SecurityPinMode.DISABLE -> {
                                                                    if (input == currentSavedPin) {
                                                                        onSuccess(null, false, "", "")
                                                                    } else {
                                                                        errorMessage = if (prefLanguage == "bn") "ভুল পিন! অনুগ্রহ করে আবার চেষ্টা করুন।" else "Incorrect PIN! Please try again."
                                                                        pinInput = ""
                                                                    }
                                                                }
                                                                SecurityPinMode.CHANGE -> {
                                                                    when (step) {
                                                                        1 -> {
                                                                            if (input == currentSavedPin) {
                                                                                pinInput = ""
                                                                                step = 2
                                                                            } else {
                                                                                errorMessage = if (prefLanguage == "bn") "ভুল পিন! অনুগ্রহ করে আবার চেষ্টা করুন।" else "Incorrect PIN! Please try again."
                                                                                pinInput = ""
                                                                            }
                                                                        }
                                                                        2 -> {
                                                                            enteredPin1 = input
                                                                            pinInput = ""
                                                                            step = 3
                                                                        }
                                                                        else -> {
                                                                            if (input == enteredPin1) {
                                                                                confirmedPin = input
                                                                                pinInput = ""
                                                                                step = 4 // Move to recovery update step
                                                                            } else {
                                                                                errorMessage = if (prefLanguage == "bn") "পিন দুটি মেলেনি! অনুগ্রহ করে আবার চেষ্টা করুন।" else "PINs do not match! Please try again."
                                                                                enteredPin1 = ""
                                                                                pinInput = ""
                                                                                step = 2
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = digit,
                                                color = AppTextColor(),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isRecoveryStep) {
                Button(
                    onClick = {
                        if (activeQuestion.isBlank()) {
                            errorMessage = if (prefLanguage == "bn") "অনুগ্রহ করে একটি প্রশ্ন লিখুন" else "Please enter a question"
                            return@Button
                        }
                        if (answerText.trim().isBlank()) {
                            errorMessage = if (prefLanguage == "bn") "অনুগ্রহ করে উত্তর লিখুন" else "Please provide an answer"
                            return@Button
                        }
                        onSuccess(confirmedPin, true, activeQuestion.trim(), answerText.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor(), contentColor = AppOnAccentColor()),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (prefLanguage == "bn") "সম্পূর্ণ করুন ও লক সক্রিয় করুন" else "Complete & Enable Lock",
                        color = AppOnAccentColor(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (prefLanguage == "bn") "বাতিল" else "Cancel",
                    color = AppTextColor().copy(alpha = 0.5f)
                )
            }
        }
    )
}

@Composable
fun RecoverySetupDialog(
    currentQuestion: String = "",
    currentAnswer: String = "",
    prefLanguage: String,
    onDismiss: () -> Unit,
    onSave: (question: String, answer: String) -> Unit
) {
    androidx.activity.compose.BackHandler { onDismiss() }

    val presetQuestionsEn = listOf(
        "What is your pet's name?",
        "What was your first school name?",
        "What is your favorite color?",
        "In what city were you born?",
        "Custom Question..."
    )
    val presetQuestionsBn = listOf(
        "আপনার পোষা প্রাণীর নাম কি?",
        "আপনার প্রথম স্কুলের নাম কি?",
        "আপনার প্রিয় রং কি?",
        "আপনি কোন শহরে জন্মগ্রহণ করেছেন?",
        "নিজের প্রশ্ন লিখুন..."
    )
    val presets = if (prefLanguage == "bn") presetQuestionsBn else presetQuestionsEn

    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    var customQuestionText by remember { mutableStateOf(if (currentQuestion.isNotBlank() && !presets.dropLast(1).contains(currentQuestion)) currentQuestion else "") }
    var answerText by remember { mutableStateOf(currentAnswer) }
    var isError by remember { mutableStateOf(false) }

    val activeQuestion = if (selectedPresetIndex == presets.size - 1) customQuestionText else presets[selectedPresetIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    text = if (prefLanguage == "bn") "রিকভারি সিকিউরিটি প্রশ্ন সেট করুন" else "Set Recovery Method",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (prefLanguage == "bn") "পিন ভুলে গেলে এই প্রশ্ন ও উত্তর দিয়ে অ্যাপ আনলক করা যাবে।" else "Set a security question & answer to recover access if you forget your PIN.",
                    color = AppTextColor().copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                Text(
                    text = if (prefLanguage == "bn") "সুরক্ষা প্রশ্ন বেছে নিন:" else "Select Security Question:",
                    color = AppTextColor(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                presets.forEachIndexed { index, questionStr ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedPresetIndex = index }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        RadioButton(
                            selected = (selectedPresetIndex == index),
                            onClick = { selectedPresetIndex = index },
                            colors = RadioButtonDefaults.colors(selectedColor = AppAccentColor())
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = questionStr,
                            color = AppTextColor(),
                            fontSize = 13.sp
                        )
                    }
                }

                if (selectedPresetIndex == presets.size - 1) {
                    GlassTextField(
                        value = customQuestionText,
                        onValueChange = { customQuestionText = it; isError = false },
                        label = { Text(if (prefLanguage == "bn") "আপনার প্রশ্ন লিখুন" else "Type Custom Question") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                GlassTextField(
                    value = answerText,
                    onValueChange = { answerText = it; isError = false },
                    label = { Text(if (prefLanguage == "bn") "উত্তর লিখুন" else "Enter Security Answer") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isError) {
                    Text(
                        text = if (prefLanguage == "bn") "প্রশ্ন এবং উত্তর উভয়ই পূরণ করতে হবে!" else "Both question and answer are required!",
                        color = AccentPink,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (activeQuestion.trim().isNotBlank() && answerText.trim().isNotBlank()) {
                        onSave(activeQuestion.trim(), answerText.trim())
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor(), contentColor = AppOnAccentColor()),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (prefLanguage == "bn") "সংরক্ষণ করুন" else "Save & Continue",
                    color = AppOnAccentColor(),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (prefLanguage == "bn") "বাতিল" else "Cancel",
                    color = AppTextColor().copy(alpha = 0.5f)
                )
            }
        }
    )
}
