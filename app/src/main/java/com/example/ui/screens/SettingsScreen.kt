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
                .background(getAppBackgroundGradient())
                .pointerInput(Unit) { detectTapGestures { } }
                
                ,
            contentAlignment = Alignment.TopCenter
        ) {
            Scaffold(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
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
                            if (currentScreen != "main") {
                                IconButton(
                                    onClick = { currentScreen = "main" },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(getGlassIconButtonBg(), CircleShape)
                                        .border(1.dp, getGlassBorder(), CircleShape)
                                        .bounceClick()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppTextColor())
                                }
                            } else {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(getGlassIconButtonBg(), CircleShape)
                                        .border(1.dp, getGlassBorder(), CircleShape)
                                        .bounceClick()
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTextColor())
                                }
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
                                    "about" -> if (isBn) "অ্যাপ সম্পর্কিত" else "About Traker"
                                    else -> "Settings"
                                },
                                fontSize = 22.sp,
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
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
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

@Composable
fun SettingsDialog(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit,
    onOpenBin: () -> Unit,
    onOpenOwnerProfile: () -> Unit,
    onOpenUtilityConfig: () -> Unit
) {
    SettingsScreen(
        viewModel = viewModel,
        onDismiss = onDismiss,
        onOpenBin = onOpenBin,
        onOpenOwnerProfile = onOpenOwnerProfile,
        onOpenUtilityConfig = onOpenUtilityConfig
    )
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
            var showHelpGuide by remember { mutableStateOf(false) }
            if (showHelpGuide) {
                LegalInfoDialog(
                    type = "info",
                    prefLanguage = prefLanguage,
                    onDismiss = { showHelpGuide = false }
                )
            }
            PixelSettingsGroup(title = "Help & About App") {
                PixelSettingsItem(
                    icon = Icons.Default.HelpOutline,
                    title = if (prefLanguage == "bn") "ব্যবহারবিধি ও গাইড (Help)" else "Help & User Guide",
                    subtitle = if (prefLanguage == "bn") "সকল ২৫টি পেজ, Android 8 ফাস্ট মোড ও GitHub APK গাইড" else "Complete guide to all 25 pages, Android 8 fast mode & GitHub APK build",
                    onClick = { showHelpGuide = true }
                )
                PixelSettingsItem(
                    icon = Icons.Default.Info,
                    title = "About & Legal",
                    subtitle = "App version, licenses & developer info",
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
    onSave: (name: String, balance: Double, details: String, upiId: String, qrCodeUri: String, accountNumber: String, ifsc: String, branchName: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    androidx.activity.compose.BackHandler { onDismiss() }

    val context = androidx.compose.ui.platform.LocalContext.current
    var accName by remember(account) { mutableStateOf(account?.name ?: "") }
    var accBal by remember(account) { mutableStateOf(account?.let { if (it.initialBalance != 0.0) it.initialBalance.toString() else it.balance.toString() } ?: "") }
    var details by remember(account) { mutableStateOf(account?.details ?: "") }
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
                            .statusBarsPadding()
                            .navigationBarsPadding()
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
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = AppTextColor()
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
                        IconButton(
                            onClick = {
                                if (accName.isNotBlank()) {
                                    val bal = accBal.toDoubleOrNull() ?: 0.0
                                    onSave(accName, bal, details, upiId, qrCodeUri, accountNumber, ifsc, branchName)
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
                                .background(AccentCyan, CircleShape)
                                .testTag("save_account_btn")
                                .bounceClick()
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save",
                                tint = Color.Black
                            )
                        }

                        if (account != null && accountsCount > 1 && onDelete != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            var showTopMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showTopMenu = true },
                                    modifier = Modifier.size(38.dp).background(getGlassIconButtonBg(), CircleShape).bounceClick()
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
                                    modifier = Modifier.background(getDialogBackground())
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (prefLanguage == "bn") "মুছে ফেলুন" else "Delete Account", color = AccentPink) },
                                        onClick = {
                                            showTopMenu = false
                                            showDeleteConfirm = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = AccentPink,
                                                modifier = Modifier.size(18.dp)
                                            )
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
                        .padding(horizontal = 20.dp, vertical = 16.dp),
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = getSettingsItemBg()),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, getGlassBorder()),
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
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .border(1.dp, AppAccentColor().copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = qrCodeUri,
                                            contentDescription = "Account Image",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { imagePickerLauncher.launch("image/*") },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppAccentColor())
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
                                OutlinedButton(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, AppAccentColor().copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppAccentColor())
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
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
    onDismiss: () -> Unit
) {
    androidx.activity.compose.BackHandler { onDismiss() }

    val title = when (type) {
        "info" -> if (prefLanguage == "bn") "অ্যাপ তথ্য" else "App Information"
        "license" -> if (prefLanguage == "bn") "লাইসেন্স ও শর্তাবলী" else "Licenses & Terms"
        "privacy" -> if (prefLanguage == "bn") "প্রাইভেসি পলিসি" else "Privacy Policy"
        else -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = com.example.R.drawable.app_logo,
                    contentDescription = "App Logo",
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Text(title, color = AppTextColor(), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val content = when (type) {
                    "info" -> if (prefLanguage == "bn") """
                        Traker v1.2.0 (Argon Android 8 High-Speed Release)
                        ডেভেলপার: @suvadippatra (GitHub)
                        © ২০২৬ @suvadippatra. সর্বস্বত্ব সংরক্ষিত।

                        ───────────────────────────
                        📖 সম্পূর্ণ ব্যবহারবিধি ও গাইড (User Guide)
                        ───────────────────────────

                        ১. ⚡ Argon Modification — Android 8 ফাস্ট মোড ও ফ্ল্যাট মেটেরিয়াল ৩ ইউআই
                        • সমস্ত ২৫টি পেজ ও পপআপে দ্রুত লোডিং: ওপেনিং সিন (হোটেল ট্যাব, খরচ ট্যাব, বিল ট্যাব, অ্যাপ লক) এবং সমস্ত সাব-স্ক্রিনে ভারী গ্র্যাডিয়েন্ট ও ট্রান্সলুসেন্ট ওভারড্র সরিয়ে ফ্ল্যাট মেটেরিয়াল ৩ (Material 3) সারফেস যুক্ত করা হয়েছে, যাতে Android 8 ডিভাইসেও কোনো ল্যাগ ছাড়াই মুহূর্তের মধ্যে পেজ লোড হয়।
                        • পরিচিত লেআউট ও ফিচার অক্ষুণ্ণ: প্রতিটি কার্ডের গঠন, ৩-ডট মেনু (⋮), অ্যাকশন বাটন এবং প্রধান ফিচার একই জায়গায় রাখা হয়েছে।
                        • ভারী ইফেক্ট বন্ধ ও নেটিভ কন্ট্রোল টগল: Settings → Appearance & Visuals থেকে "Disable Heavy Effects & Use Native Controls" অপশনটি চালু/বন্ধ করতে পারবেন।

                        ২. 🗓️ নেটিভ অ্যান্ড্রয়েড ডেট/টাইম পিকার ও হালকা মিটার স্টেপার
                        • নেটিভ ক্যালেন্ডার ও ঘড়ি: ভারী ৩ডি হুইল পিকারের বদলে অ্যান্ড্রয়েডের নেটিভ DatePickerDialog এবং TimePickerDialog যুক্ত করা হয়েছে, সাথে "Today", "Yesterday", "Now", "+1 Day", "-1 Day" কুইক চিপস।
                        • ডিজিটাল ওডোমিটার স্টেপার (Bill ট্যাব): ৬-চাকার ভারী মিটার ডায়ালের পরিবর্তে জিরো-ল্যাগ ফ্ল্যাট মেটেরিয়াল ৩ ডিজিটাল স্টেপার ('+' / '-' বাটন) এবং সরাসরি কিবোর্ডে রিডিং লেখার জন্য "Type Reading" বাটন যুক্ত করা হয়েছে।

                        ৩. 💰 ড্যাশবোর্ড, খাবার ও হোটেল ব্যয় ট্র্যাকিং (Hotels & Extras)
                        • শিফট ও মেনু ট্র্যাকিং: ডে (দুপুর) এবং নাইট (রাত) শিফটে আলাদাভাবে খাবারের হিসাব রাখুন এবং কাস্টম মেনু পরিচালনা করুন।
                        • মাল্টি-অ্যাকাউন্ট ও ব্যালেন্স: ক্যাশ, ব্যাঙ্ক ও ডিজিটাল ওয়ালেটের ব্যালেন্স, আয়/ব্যয় ও ট্রান্সফার এক নজরে পর্যবেক্ষণ করুন।

                        ৪. 🔒 গোপনীয়তা, অফলাইন ব্যাকআপ ও কিউআর ট্রান্সফার
                        • ১০০% অফলাইন ও নিরাপদ: আপনার সমস্ত তথ্য ডিভাইসেই সংরক্ষিত থাকে।
                        • অফলাইন QR ট্রান্সফার ও JSON/CSV ব্যাকআপ: ইন্টারনেট ছাড়াই দ্রুত QR কোড বা ফাইল ব্যাকআপের মাধ্যমে ডাটা স্থানান্তর করুন।

                        ৫. 🛠️ GitHub Actions থেকে ম্যানুয়ালি APK ও AAB তৈরি করার নিয়ম
                        • আপনার GitHub রিপোজিটরির "Actions" ট্যাবে যান।
                        • বাম পাশ থেকে "Build & Release Android App" ওয়ার্কফ্লো নির্বাচন করুন।
                        • "Run workflow" বাটনে ক্লিক করে আপনার পছন্দের আউটপুট বেছে নিন:
                          - all (AAB + Universal APK + arm64-v8a APK)
                          - aab (Play Store রিলিজ বান্ডেল)
                          - universal-apk (সব ডিভাইসের জন্য APK)
                          - arm64-v8a-apk (আধুনিক 64-bit ফোনের জন্য হালকা ও দ্রুত APK)
                        • বিল্ড শেষ হলে ওয়ার্কফ্লো সামারি পেজের নিচে থাকা "Artifacts" সেকশন থেকে ফাইলগুলো ডাউনলোড করুন।

                        👨‍💻 ডেভেলপার ও ওপেন সোর্স:
                        GitHub: @suvadippatra (https://github.com/suvadippatra)
                        লাইসেন্স: Apache License 2.0
                    """.trimIndent() else """
                        Traker v1.2.0 (Argon Android 8 High-Speed Release)
                        Developed by @suvadippatra (GitHub)
                        © 2026 @suvadippatra. All rights reserved.

                        ───────────────────────────
                        📖 COMPLETE USER GUIDE & HOW TO USE
                        ───────────────────────────

                        1. ⚡ ARGON MODIFICATION — ANDROID 8 FAST MODE & FLAT M3 UI
                        • Fast Flat Material 3 Surfaces Across All 25 Pages: Stripped heavy multi-stop gradients, continuous alpha animations, and translucent overdraw layers across all 4 opening scenes (Hotels Tab, Extras Tab, Bill Tab, App Lock) and all 21 secondary screens/modals while keeping the familiar layout, triple-dot (⋮) menus, and features 100% intact.
                        • Settings Toggle: Go to Settings → Appearance & Visuals → "Disable Heavy Effects & Use Native Controls" (enabled by default) to use native Android pickers and disable heavy visual effects/animations on Android 8+.

                        2. 🗓️ NATIVE ANDROID PICKERS & LIGHTWEIGHT METER STEPPER
                        • Native Date & Time Dialogs: Replaced heavy 3D wheel pickers with instant Android OS DatePickerDialog and TimePickerDialog plus quick "Today", "Yesterday", "Now", "-1 Day", and "+1 Day" chips.
                        • Digital Odometer Stepper (Bill Tab): Replaced the 6-wheel 3D cylinder dial with a zero-lag flat Material 3 digital stepper ('+' / '-' buttons per digit) plus a "Type Reading" button for direct decimal keypad entry.

                        3. 💰 HOTEL MEALS, EXTRA EXPENSES & ACCOUNTS
                        • Meal & Hotel Tracker: Log meals across Day/Night shifts, manage hotel menus, and track monthly food balances.
                        • Multi-Account & Cashflow: Manage cash, bank, and wallet accounts with instant balance updates and category filtering.

                        4. 🔒 PRIVACY, OFFLINE BACKUP & QR STREAMING
                        • 100% Offline & Private: All financial data lives securely on your device.
                        • Offline QR Stream & File Backups: Transfer databases via animated fountain QR codes or export/import JSON & CSV backups.

                        5. 🛠️ HOW TO BUILD APK & AAB MANUALLY ON GITHUB
                        • Open your repository on GitHub and click the "Actions" tab.
                        • Select "Build & Release Android App" in the left sidebar and click "Run workflow".
                        • Choose your desired Build Target:
                          - all (Builds Play Store .aab + Universal .apk + arm64-v8a .apk)
                          - aab (Play Store Android App Bundle .aab)
                          - universal-apk (Universal APK supporting all CPU architectures)
                          - arm64-v8a-apk (Lightweight, high-speed APK for 64-bit ARM devices)
                        • Once the workflow finishes, scroll down to the "Artifacts" section on the workflow run page to download your compiled APKs/AAB, or enable "create_release" to attach them to a GitHub Release tag.

                        👨‍💻 Developer & Open Source:
                        GitHub: @suvadippatra (https://github.com/suvadippatra)
                        License: Apache License 2.0
                    """.trimIndent()
                    "license" -> if (prefLanguage == "bn") """
                        এই অ্যাপ্লিকেশনটি Apache License 2.0 এর অধীনে লাইসেন্সপ্রাপ্ত।
                        
                        আপনি লাইসেন্সের শর্তাবলী অনুযায়ী এই সফটওয়্যারটি ব্যবহার, বিতরণ এবং পরিবর্তন করতে পারেন।
                        
                        ব্যবহৃত থার্ড-পার্টি লাইব্রেরি:
                        - Jetpack Compose
                        - Room Database
                        - Kotlin Coroutines
                        - Material Design 3
                    """.trimIndent() else """
                        This application is licensed under the Apache License 2.0.
                        
                        You may use, distribute, and modify this software in accordance with the terms of the license.
                        
                        Third-party libraries used:
                        - Jetpack Compose
                        - Room Database
                        - Kotlin Coroutines
                        - Material Design 3
                    """.trimIndent()
                    "privacy" -> if (prefLanguage == "bn") """
                        আপনার প্রাইভেসি আমাদের কাছে সবচেয়ে গুরুত্বপূর্ণ।
                        
                        Traker একটি অফলাইন-ফার্স্ট অ্যাপ্লিকেশন। এর অর্থ:
                        ১. আপনার সমস্ত তথ্য শুধুমাত্র আপনার ফোনেই সংরক্ষিত থাকে।
                        ২. কোনো আর্থিক তথ্য কোনো সার্ভারে পাঠানো হয় না।
                        ৩. আমরা আপনার কোনো ব্যক্তিগত তথ্য সংগ্রহ বা বিক্রি করি না।
                        
                        আপনার তথ্য সম্পূর্ণ আপনার নিয়ন্ত্রণে।
                    """.trimIndent() else """
                        Your privacy is our priority.
                        
                        Traker is an OFFLINE-FIRST application. This means:
                        1. All your data is stored locally on your device.
                        2. No financial data is sent to any external server.
                        3. We do not collect or sell your personal information.
                        
                        You are in full control of your data.
                    """.trimIndent()
                    else -> ""
                }
                Text(
                    content,
                    color = AppTextColor().copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (prefLanguage == "bn") "বন্ধ করুন" else "Close",
                    color = AppAccentColor(),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
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
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(getGlassIconButtonBg())
                                            .border(0.5.dp, getGlassBorder(), CircleShape)
                                            .clickable {
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
                                            .bounceClick(),
                                        contentAlignment = Alignment.Center
                                    ) {
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
