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
// SETTINGS / CONFIGURATION FULL DIALOG
// ==========================================
@Composable
fun SettingsDialog(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit,
    onOpenBin: () -> Unit,
    onOpenOwnerProfile: () -> Unit,
    onOpenUtilityConfig: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val hotels by viewModel.allHotels.collectAsStateWithLifecycle()
    val accounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()

    // Input fields for Profile & Preferences
    var profileName by remember(userProfile) { mutableStateOf(userProfile?.name ?: "") }
    var profileContact by remember(userProfile) { mutableStateOf(userProfile?.contact ?: "") }

    var prefLanguage by remember(userProfile) { mutableStateOf(userProfile?.language ?: "en") }
    var prefThemeMode by remember(userProfile) { mutableStateOf(userProfile?.themeMode ?: "Auto") }
    var prefShowAdvancedPayment by remember(userProfile) { mutableStateOf(userProfile?.showAdvancedPayment ?: false) }
    var prefAppLockEnabled by remember(userProfile) { mutableStateOf(userProfile?.appLockEnabled ?: false) }
    var prefAppLockPin by remember(userProfile) { mutableStateOf(userProfile?.appLockPin ?: "") }
    var prefBiometricEnabled by remember(userProfile) { mutableStateOf(userProfile?.biometricEnabled ?: false) }
    var prefSecurityQuestion by remember(userProfile) { mutableStateOf(userProfile?.securityQuestion ?: "") }
    var prefSecurityAnswer by remember(userProfile) { mutableStateOf(userProfile?.securityAnswer ?: "") }
    var pinVisible by remember { mutableStateOf(false) }
    var isPinVerifiedForSettings by remember(userProfile) { mutableStateOf(userProfile?.appLockEnabled != true) }
    var showSecurityUnlockDialog by remember { mutableStateOf(false) }
    var showSecurityPinDialog by remember { mutableStateOf(false) }
    var showRecoverySetupDialog by remember { mutableStateOf(false) }
    var securityDialogMode by remember { mutableStateOf(SecurityPinMode.SET_NEW) }
    var prefDefaultHotelId by remember(userProfile) { mutableStateOf(userProfile?.defaultHotelId ?: 1) }
    var prefEnableStarAnimation by remember(userProfile) { mutableStateOf(userProfile?.enableStarAnimation ?: true) }
    var prefTextSize by remember(userProfile) { mutableStateOf(userProfile?.textSize ?: "Medium") }
    var customisationMealGroupPos by remember(userProfile) { mutableStateOf(userProfile?.mealGroupPosition ?: "BottomBar") }
    var customisationBgPreset by remember(userProfile) { mutableStateOf(userProfile?.bgPreset ?: "Default") }
    var customisationBgUri by remember(userProfile) { mutableStateOf(userProfile?.customBgUri ?: "") }

    val bgImagePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            customisationBgUri = it.toString()
            viewModel.updateCustomisation(customisationMealGroupPos, customisationBgPreset, customisationBgUri)
        }
    }

    var settingsSearchQuery by remember { mutableStateOf("") }

    var showMenuImportDialog by remember { mutableStateOf(false) }
    var showHotelHistoryImportDialog by remember { mutableStateOf(false) }
    var showAccountHistoryImportDialog by remember { mutableStateOf(false) }
    var showMeterReadingImportDialog by remember { mutableStateOf(false) }
    
    var pastedCsvText by remember { mutableStateOf("") }
    var selectedHotelIdForImport by remember { mutableStateOf<Int?>(null) }


    val totalImportLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                if (content != null) {
                    viewModel.importAllDataFromJson(content)
                    android.widget.Toast.makeText(context, "Full Backup Restored Successfully", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Restore Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val menuImportLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                if (content != null) {
                    viewModel.importMenusFromCsv(content, selectedHotelIdForImport)
                    android.widget.Toast.makeText(context, "Menus Imported Successfully", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val hotelHistoryImportLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                if (content != null) {
                    viewModel.importHotelTransactionsFromCsv(content, selectedHotelIdForImport)
                    android.widget.Toast.makeText(context, "Hotel History Imported Successfully", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val accountHistoryImportLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                if (content != null) {
                    viewModel.importExtraExpensesFromCsv(content)
                    android.widget.Toast.makeText(context, "Account History Imported Successfully", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val meterReadingImportLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                if (content != null) {
                    viewModel.importMeterReadingsFromCsv(content)
                    android.widget.Toast.makeText(context, "Meter Readings Imported Successfully", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }


    if (showMenuImportDialog || showHotelHistoryImportDialog || showAccountHistoryImportDialog || showMeterReadingImportDialog) {
        val title = when {
            showMenuImportDialog -> if (prefLanguage == "bn") "মেনু রিস্টোর করুন" else "Restore Menus from Device File"
            showHotelHistoryImportDialog -> if (prefLanguage == "bn") "হোটেল ইতিহাস রিস্টোর করুন" else "Restore Hotel History from Device File"
            showAccountHistoryImportDialog -> if (prefLanguage == "bn") "অ্যাকাউন্ট ইতিহাস রিস্টোর করুন" else "Restore Account History from Device File"
            else -> if (prefLanguage == "bn") "মিটার রিডিং রিস্টোর করুন" else "Restore Meter Readings from Device File"
        }
        val showHotelSelector = showMenuImportDialog || showHotelHistoryImportDialog
        
        AlertDialog(
            onDismissRequest = {
                showMenuImportDialog = false
                showHotelHistoryImportDialog = false
                showAccountHistoryImportDialog = false
                showMeterReadingImportDialog = false
            },
            containerColor = getDialogBackground(),
            title = { Text(title, color = AppTextColor(), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (showHotelSelector) {
                        var hotelDropdownExpanded by remember { mutableStateOf(false) }
                        Text(if (prefLanguage == "bn") "হোটেল নির্বাচন করুন (ঐচ্ছিক)" else "Select Hotel (Optional)", color = AppTextColor(), fontSize = 14.sp)
                        Box {
                            OutlinedButton(
                                onClick = { hotelDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val selectedHotel = hotels.find { it.id == selectedHotelIdForImport }
                                Text(selectedHotel?.name ?: if (prefLanguage == "bn") "স্বয়ংক্রিয় সনাক্তকরণ বা নির্বাচন" else "Auto-detect from file or Select", color = AppTextColor())
                            }
                            DropdownMenu(
                                expanded = hotelDropdownExpanded,
                                onDismissRequest = { hotelDropdownExpanded = false },
                                modifier = Modifier.background(getDialogBackground())
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (prefLanguage == "bn") "স্বয়ংক্রিয় সনাক্তকরণ" else "Auto-detect from file", color = AppTextColor()) },
                                    onClick = { selectedHotelIdForImport = null; hotelDropdownExpanded = false }
                                )
                                hotels.forEach { hotel ->
                                    DropdownMenuItem(
                                        text = { Text(hotel.name, color = AppTextColor()) },
                                        onClick = { selectedHotelIdForImport = hotel.id; hotelDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = if (prefLanguage == "bn") "ডিভাইস স্টোরেজ থেকে ব্যাকআপ ফাইল নির্বাচন করে রিস্টোর সম্পূর্ণ করুন।" else "Select a backup file saved in your device storage to restore.",
                        color = AppTextColor().copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            showMenuImportDialog -> menuImportLauncher.launch("*/*")
                            showHotelHistoryImportDialog -> hotelHistoryImportLauncher.launch("*/*")
                            showAccountHistoryImportDialog -> accountHistoryImportLauncher.launch("*/*")
                            showMeterReadingImportDialog -> meterReadingImportLauncher.launch("*/*")
                        }
                        showMenuImportDialog = false
                        showHotelHistoryImportDialog = false
                        showAccountHistoryImportDialog = false
                        showMeterReadingImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (prefLanguage == "bn") "ফাইল নির্বাচন করুন" else "Select File from Device", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMenuImportDialog = false
                    showHotelHistoryImportDialog = false
                    showAccountHistoryImportDialog = false
                    showMeterReadingImportDialog = false
                }) {
                    Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AccentPink)
                }
            }
        )
    }

    var pendingFileContent by remember { mutableStateOf("") }
    var pendingFileName by remember { mutableStateOf("") }

    val saveFileLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(pendingFileContent.toByteArray(Charsets.UTF_8))
                }
                android.widget.Toast.makeText(context, if (prefLanguage == "bn") "ফাইল ডিভাইসে সেভ হয়েছে" else "Backup saved to device storage: $pendingFileName", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Save Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        pendingFileContent = ""
        pendingFileName = ""
    }

    val saveBackupFile = { filename: String, content: String ->
        pendingFileName = filename
        pendingFileContent = content
        saveFileLauncher.launch(filename)
    }

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val customFontScale = when (prefTextSize) {
        "Small" -> 0.85f
        "Large" -> 1.15f
        else -> 1.0f
    }

    // Dialog trigger states
    var editingHotel by remember { mutableStateOf<HotelEntity?>(null) }
    var manageMenuForHotel by remember { mutableStateOf<HotelEntity?>(null) }
    var showAddHotelDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<BankAccountEntity?>(null) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showLegalInfoType by remember { mutableStateOf<String?>(null) } // "info", "license", "privacy"

    LaunchedEffect(prefLanguage, prefThemeMode, prefShowAdvancedPayment, prefAppLockEnabled, prefAppLockPin, prefBiometricEnabled, prefSecurityQuestion, prefSecurityAnswer, prefDefaultHotelId, prefEnableStarAnimation, prefTextSize, userProfile) {
        val up = userProfile
        if (up != null && (
            prefLanguage != up.language ||
            prefThemeMode != up.themeMode ||
            prefShowAdvancedPayment != up.showAdvancedPayment ||
            prefAppLockEnabled != up.appLockEnabled ||
            prefAppLockPin != up.appLockPin ||
            prefBiometricEnabled != up.biometricEnabled ||
            prefSecurityQuestion != up.securityQuestion ||
            prefSecurityAnswer != up.securityAnswer ||
            prefDefaultHotelId != up.defaultHotelId ||
            prefEnableStarAnimation != up.enableStarAnimation ||
            prefTextSize != up.textSize
        )) {
            viewModel.updatePreferences(
                language = prefLanguage,
                themeMode = prefThemeMode,
                showAdvancedPayment = prefShowAdvancedPayment,
                appLockEnabled = prefAppLockEnabled,
                appLockPin = prefAppLockPin,
                biometricEnabled = prefBiometricEnabled,
                securityQuestion = prefSecurityQuestion,
                securityAnswer = prefSecurityAnswer,
                defaultHotelId = prefDefaultHotelId,
                enableStarAnimation = prefEnableStarAnimation,
                textSize = prefTextSize
            )
        }
    }

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger = true
    }

    BackHandler {
        onDismiss()
    }

    CompositionLocalProvider(
        LocalAppTheme provides prefThemeMode,
        LocalAppBgPreset provides customisationBgPreset,
        LocalAppCustomBgUri provides customisationBgUri,
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
            density = currentDensity.density,
            fontScale = customFontScale
        )
    ) {
        AnimatedVisibility(
            visible = animateTrigger,
            enter = slideInVertically(initialOffsetY = { it / 8 }) + fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(targetOffsetY = { it / 8 }) + fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getAppBackgroundGradient())
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center
            ) {
                if (customisationBgUri.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = customisationBgUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 840.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Top App Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
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
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (prefLanguage == "bn") "সেটিংস ও নিয়ন্ত্রণ" else "Settings & Controls",
                            color = AppTextColor(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val q = settingsSearchQuery.trim()
                    val matches = { text: String ->
                        q.isEmpty() || text.contains(q, ignoreCase = true)
                    }

                    val showOwnerProfile = matches("Owner Profile") || matches("মালিক প্রোফাইল") || matches(profileName) || matches(profileContact) || matches("Owner Profile & Management") || matches("মালিকের প্রোফাইল ও ব্যবস্থাপনা")
                    val showBin = matches("Recycle Bin") || matches("রিসাইকেল বিন") || matches("restore deleted items") || matches("মুছে ফেলা আইটেম")
                    val showUtility = matches("Utility Configuration") || matches("ইউটিলিটি কনফিগারেশন") || matches("Electricity") || matches("বিদ্যুৎ")
                    val showProfileGroup = showOwnerProfile || showBin || showUtility

                    val showAppTheme = matches("App Theme") || matches("অ্যাপ থিম") || matches("Auto") || matches("Light") || matches("Dark") || matches("লাইট") || matches("ডার্ক")
                    val showLanguage = matches("App Language") || matches("অ্যাপের ভাষা") || matches("English") || matches("বাংলা")
                    val showTextSize = matches("Text Size") || matches("অক্ষরের আকার") || matches("Small") || matches("Medium") || matches("Large")
                    val showStartupHotel = matches("Default Startup Hotel") || matches("ডিফল্ট স্টার্টআপ হোটেল")
                    val showAdvancedPaymentOpt = matches("Show Advanced Payment") || matches("অগ্রিম পেমেন্ট কার্ড ড্যাশবোর্ড") || matches("advanced balance")
                    val showStarAnimation = matches("Celebration Star Animations") || matches("স্টার অ্যানিমেশন প্রভাব") || matches("Animate flying colorful stars")
                    val showAppearanceGroup = showAppTheme || showLanguage || showTextSize || showStartupHotel || showAdvancedPaymentOpt || showStarAnimation || matches("Appearance & Design") || matches("ডিসপ্লে ও অবয়ব")

                    val matchingHotels = hotels.filter { matches(it.name) || matches(it.location) || matches(it.phoneNumber) }
                    val showHotelsGroup = matchingHotels.isNotEmpty() || matches("Hotels & Establishments") || matches("হোটেল ও রেস্তোরাঁসমূহ") || matches("Add New Hotel") || matches("নতুন হোটেল যোগ করুন")

                    val matchingAccounts = accounts.filter { matches(it.name) }
                    val showAccountsGroup = matchingAccounts.isNotEmpty() || matches("Virtual Bank Accounts") || matches("ভার্চুয়াল ব্যাংক অ্যাকাউন্ট") || matches("Add New Account") || matches("নতুন অ্যাকাউন্ট যোগ করুন")

                    val showAppLock = matches("Enable App Passcode") || matches("পাসকোড স্ক্রিন লক") || matches("Security PIN") || matches("৪-ডিজিট সিকিউরিটি পিন") || matches("Security & Protection") || matches("নিরাপত্তা ও প্রাইভেসি")
                    val showSecurityGroup = showAppLock

                    val showInfo = matches("App Information") || matches("অ্যাপ তথ্য") || matches("Version 1.0.0")
                    val showLicense = matches("Licenses & Terms") || matches("লাইসেন্স ও শর্তাবলী")
                    val showPrivacy = matches("Privacy Policy") || matches("প্রাইভেসি পলিসি")
                    val showAboutGroup = showInfo || showLicense || showPrivacy || matches("About & Legal") || matches("সম্পর্কে ও আইনি তথ্য")
                    val showBackupRestoreGroup = settingsSearchQuery.isEmpty() || matches("Backup & Restore") || matches("ব্যাকআপ এবং রিস্টোর") || matches("csv") || matches("export") || matches("import")

                    val hasAnyMatch = showProfileGroup || showAppearanceGroup || showHotelsGroup || showAccountsGroup || showSecurityGroup || showBackupRestoreGroup || showAboutGroup

                    Spacer(modifier = Modifier.height(4.dp))

                    // Main scrollable settings content (Lazy-loaded for speed)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (showProfileGroup) {
                            item {
                                // Category: Profile
                                PixelSettingsGroup(
                                    title = if (prefLanguage == "bn") "মালিকের প্রোফাইল ও ব্যবস্থাপনা" else "Owner Profile & Management"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (showOwnerProfile) {
                                            PixelSettingsItem(
                                                icon = Icons.Default.AccountCircle,
                                                iconBgColor = Color(0xFFE3F2FD),
                                                iconTint = Color(0xFF1976D2),
                                                title = profileName.ifBlank { if (prefLanguage == "bn") "মালিক প্রোফাইল" else "Owner Profile" },
                                                subtitle = if (profileContact.isNotBlank()) profileContact else (if (prefLanguage == "bn") "মালিক প্রোফাইল পরিচালনা" else "Manage Owner Details & UPI"),
                                                onClick = { onDismiss(); onOpenOwnerProfile() }
                                            )
                                        }
                                        if (showBin) {
                                            PixelSettingsItem(
                                                icon = Icons.Default.DeleteOutline,
                                                iconBgColor = Color(0xFFECEFF1),
                                                iconTint = Color(0xFF455A64),
                                                title = if (prefLanguage == "bn") "রিসাইকেল বিন" else "Recycle Bin",
                                                subtitle = if (prefLanguage == "bn") "মুছে ফেলা আইটেমগুলো দেখুন" else "View and restore deleted items",
                                                onClick = { onDismiss(); onOpenBin() }
                                            )
                                        }
                                        if (showUtility) {
                                            PixelSettingsItem(
                                                icon = Icons.Default.Settings,
                                                iconBgColor = Color(0xFFE0F7FA),
                                                iconTint = Color(0xFF00ACC1),
                                                title = if (prefLanguage == "bn") "ইউটিলিটি কনফিগারেশন" else "Utility Configuration",
                                                subtitle = if (prefLanguage == "bn") "বিদ্যুৎ চার্জ ও রেট পরিচালনা" else "Manage electricity rates & charges",
                                                onClick = { onDismiss(); onOpenUtilityConfig() }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showAppearanceGroup) {
                            item {
                                // Category: Customisation
                                PixelSettingsGroup(
                                    title = if (prefLanguage == "bn") "কাস্টমাইজেশন" else "Customisation"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // 1. Meal Groups Display Position
                                        Column {
                                            Text(
                                                text = if (prefLanguage == "bn") "মেইল গ্রুপ পজিশন" else "Meal Groups Position",
                                                color = AppTextColor(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val groupOptions = listOf(
                                                    "BottomBar" to if (prefLanguage == "bn") "বটম বার" else "Bottom Action Bar",
                                                    "PageBottom" to if (prefLanguage == "bn") "পেজ শেষ" else "Page Bottom"
                                                )
                                                groupOptions.forEach { (posKey, label) ->
                                                    val isSel = customisationMealGroupPos == posKey
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(if (isSel) AccentCyan.copy(alpha = 0.2f) else AppTextColor().copy(alpha = 0.05f))
                                                            .border(1.dp, if (isSel) AccentCyan else getGlassBorder(), RoundedCornerShape(12.dp))
                                                            .clickable {
                                                                customisationMealGroupPos = posKey
                                                                viewModel.updateCustomisation(posKey, customisationBgPreset, customisationBgUri)
                                                            }
                                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            color = if (isSel) AccentCyan else AppTextColor(),
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp)

                                        // 2. Change Background
                                        Column {
                                            Text(
                                                text = if (prefLanguage == "bn") "ব্যাকগ্রাউন্ড পরিবর্তন করুন" else "Change Background",
                                                color = AppTextColor(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )

                                            // Preset Chips
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                            ) {
                                                val presets = listOf(
                                                    "Default" to "Default",
                                                    "Midnight Blue" to "Midnight",
                                                    "Emerald Dark" to "Emerald",
                                                    "Sunset Warm" to "Sunset",
                                                    "Purple Twilight" to "Purple",
                                                    "Soft Minimal Light" to "Minimal Light"
                                                )
                                                items(presets) { (presetKey, label) ->
                                                    val isSel = customisationBgPreset == presetKey && customisationBgUri.isBlank()
                                                    FilterChip(
                                                        selected = isSel,
                                                        onClick = {
                                                            customisationBgPreset = presetKey
                                                            customisationBgUri = ""
                                                            viewModel.updateCustomisation(customisationMealGroupPos, presetKey, "")
                                                        },
                                                        label = { Text(label, fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = AccentCyan,
                                                            selectedLabelColor = Color.White
                                                        )
                                                    )
                                                }
                                            }

                                            // Custom Image Upload Option
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                OutlinedButton(
                                                    onClick = { bgImagePickerLauncher.launch("image/*") },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        if (customisationBgUri.isNotBlank()) "Change Custom Image" else "Upload Custom Image",
                                                        fontSize = 12.sp
                                                    )
                                                }

                                                if (customisationBgUri.isNotBlank()) {
                                                    TextButton(
                                                        onClick = {
                                                            customisationBgUri = ""
                                                            viewModel.updateCustomisation(customisationMealGroupPos, customisationBgPreset, "")
                                                        }
                                                    ) {
                                                        Text("Reset Image", color = AccentPink, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                // Category: App Appearance Controls
                                PixelSettingsGroup(
                                    title = if (prefLanguage == "bn") "ডিসপ্লে ও অবয়ব" else "Appearance & Design"
                                ) {
                                    var hasPrevious = false

                                    // Sub-section: Theme Select
                                    if (showAppTheme) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (prefLanguage == "bn") "অ্যাপ থিম" else "App Theme",
                                                color = AppTextColor(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val themeOptions = listOf(
                                                    Triple("Auto", Icons.Default.Settings, if (prefLanguage == "bn") "অটো (Auto)" else "Auto"),
                                                    Triple("Light", Icons.Default.WbSunny, if (prefLanguage == "bn") "লাইট" else "Light"),
                                                    Triple("Dark", Icons.Default.NightsStay, if (prefLanguage == "bn") "ডার্ক" else "Dark")
                                                )

                                                themeOptions.forEach { (mode, icon, label) ->
                                                    val isSelected = prefThemeMode == mode
                                                    val cardBg = if (isSelected) AccentCyan.copy(alpha = 0.15f) else AppTextColor().copy(alpha = 0.04f)
                                                    val borderTint = if (isSelected) AccentCyan else Color.Transparent

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(cardBg)
                                                            .border(1.5.dp, borderTint, RoundedCornerShape(16.dp))
                                                            .clickable { prefThemeMode = mode }
                                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(
                                                                imageVector = icon,
                                                                contentDescription = label,
                                                                tint = if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.7f),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = label,
                                                                color = if (isSelected) AccentCyan else AppTextColor(),
                                                                fontSize = 11.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        hasPrevious = true
                                    }

                                    // Language Selector
                                    if (showLanguage) {
                                        if (hasPrevious) {
                                            HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "অ্যাপের ভাষা" else "App Language",
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (prefLanguage == "bn") "ইংরেজি ও বাংলা সমর্থন করে" else "Supports English & Bengali",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf("en" to "English", "bn" to "বাংলা").forEach { (langCode, langName) ->
                                                    val isSel = prefLanguage == langCode
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(if (isSel) AccentCyan else AppTextColor().copy(alpha = 0.05f))
                                                            .clickable { prefLanguage = langCode }
                                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = langName,
                                                            color = if (isSel) Color.Black else AppTextColor(),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        hasPrevious = true
                                    }

                                    // Text Size Slider
                                    if (showTextSize) {
                                        if (hasPrevious) {
                                            HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "অক্ষরের আকার" else "Text Size",
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = prefTextSize,
                                                    color = AccentCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            
                                            var localSliderValue by remember(prefTextSize) { 
                                                mutableFloatStateOf(
                                                    when (prefTextSize) {
                                                        "Small" -> 0f
                                                        "Large" -> 2f
                                                        else -> 1f
                                                    }
                                                )
                                            }
                                            androidx.compose.material3.Slider(
                                                value = localSliderValue,
                                                onValueChange = { newVal ->
                                                    localSliderValue = newVal
                                                },
                                                onValueChangeFinished = {
                                                    prefTextSize = when (Math.round(localSliderValue)) {
                                                        0 -> "Small"
                                                        2 -> "Large"
                                                        else -> "Medium"
                                                    }
                                                },
                                                valueRange = 0f..2f,
                                                steps = 1,
                                                colors = androidx.compose.material3.SliderDefaults.colors(
                                                    thumbColor = AccentCyan,
                                                    activeTrackColor = AccentCyan,
                                                    inactiveTrackColor = AppTextColor().copy(alpha = 0.2f)
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                        }
                                        hasPrevious = true
                                    }

                                    // Default Startup Hotel
                                    if (showStartupHotel) {
                                        if (hasPrevious) {
                                            HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (prefLanguage == "bn") "ডিফল্ট স্টার্টআপ হোটেল" else "Default Startup Hotel",
                                                color = AppTextColor(),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (prefLanguage == "bn") "অ্যাপটি খুললে প্রথমে এই হোটেলটি নির্বাচিত থাকবে" else "This hotel is automatically active on startup",
                                                color = AppTextColor().copy(alpha = 0.5f),
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(hotels, key = { it.id }) { hotel ->
                                                    val isSel = prefDefaultHotelId == hotel.id
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(if (isSel) AccentCyan.copy(alpha = 0.2f) else AppTextColor().copy(alpha = 0.05f))
                                                            .border(1.dp, if (isSel) AccentCyan else Color.Transparent, RoundedCornerShape(12.dp))
                                                            .clickable { prefDefaultHotelId = hotel.id }
                                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = hotel.name,
                                                            color = if (isSel) AccentCyan else AppTextColor(),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        hasPrevious = true
                                    }

                                    // Show Advanced Payment Card Switch
                                    if (showAdvancedPaymentOpt) {
                                        if (hasPrevious) {
                                            HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "অগ্রিম পেমেন্ট কার্ড ড্যাশবোর্ড" else "Show Advanced Payment",
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (prefLanguage == "bn") "প্রধান স্ক্রিনে অ্যাডভান্সড ডিপোজিট ব্যালেন্স প্রদর্শন" else "Display advanced balance in the main dashboard",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            GlassSwitch(
                                                checked = prefShowAdvancedPayment,
                                                onCheckedChange = { prefShowAdvancedPayment = it },
                                                activeText = "ON",
                                                inactiveText = "OFF"
                                            )
                                        }
                                        hasPrevious = true
                                    }

                                    // Enable Star Celebration Animation
                                    if (showStarAnimation) {
                                        if (hasPrevious) {
                                            HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "স্টার অ্যানিমেশন প্রভাব" else "Celebration Star Animations",
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (prefLanguage == "bn") "লেনদেন বা মিল এন্ট্রি করলে উড়ন্ত স্টারের চমৎকার দৃশ্য" else "Animate flying colorful stars on successful entries",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            GlassSwitch(
                                                checked = prefEnableStarAnimation,
                                                onCheckedChange = { prefEnableStarAnimation = it },
                                                activeText = "ON",
                                                inactiveText = "OFF"
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showHotelsGroup) {
                            item {
                                // Category: Establishments (Hotels)
                                PixelSettingsGroup(
                                    title = if (prefLanguage == "bn") "হোটেল ও রেস্তোরাঁসমূহ" else "Hotels & Establishments"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val hotelsToDisplay = if (q.isEmpty() || matches("Hotels & Establishments") || matches("হোটেল ও রেস্তোরাঁসমূহ")) hotels else matchingHotels
                                        hotelsToDisplay.forEach { hotel ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(AppTextColor().copy(alpha = 0.04f))
                                                    .clickable { editingHotel = hotel }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFFF3E0)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Restaurant,
                                                        contentDescription = null,
                                                        tint = Color(0xFFE65100),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = hotel.name,
                                                        color = AppTextColor(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = "Advance: ₹${hotel.advancedPayment.toInt()} • " + 
                                                               (if (hotel.phoneNumber.isNotBlank()) hotel.phoneNumber else "No Phone") +
                                                               (if (hotel.location.isNotBlank()) " • ${hotel.location}" else ""),
                                                        color = AppTextColor().copy(alpha = 0.5f),
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit details",
                                                    tint = AccentCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Sibling Adder Button
                                        Button(
                                            onClick = { showAddHotelDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = AccentCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (prefLanguage == "bn") "নতুন হোটেল যোগ করুন" else "Add New Hotel",
                                                color = AccentCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showAccountsGroup) {
                            item {
                                // Category: Financial Bank Accounts
                                PixelSettingsGroup(
                                    title = if (prefLanguage == "bn") "ভার্চুয়াল ব্যাংক অ্যাকাউন্ট" else "Virtual Bank Accounts"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val accountsToDisplay = if (q.isEmpty() || matches("Virtual Bank Accounts") || matches("ভার্চুয়াল ব্যাংক অ্যাকাউন্ট")) accounts else matchingAccounts
                                        accountsToDisplay.forEach { acc ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(AppTextColor().copy(alpha = 0.04f))
                                                    .clickable { editingAccount = acc }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFE8F5E9)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AccountBalance,
                                                        contentDescription = null,
                                                        tint = Color(0xFF2E7D32),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = acc.name,
                                                        color = AppTextColor(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = "Liquid Balance: ₹${acc.balance.toInt()}",
                                                        color = AccentGreen,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit details",
                                                    tint = AccentCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Sibling Adder Button
                                        Button(
                                            onClick = { showAddAccountDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = AccentGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (prefLanguage == "bn") "নতুন অ্যাকাউন্ট যোগ করুন" else "Add New Account",
                                                color = AccentGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showSecurityGroup) {
                            item {
                                // Category: Security & Pin Lock
                                PixelSettingsGroup(
                                    title = if (prefLanguage == "bn") "নিরাপত্তা ও প্রাইভেসি" else "Security & Protection"
                                ) {
                                    Column {
                                        if (userProfile?.appLockEnabled == true && !isPinVerifiedForSettings) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (prefLanguage == "bn") "সিকিউরিটি সেটিংস লক করা" else "Security Settings Locked",
                                                        color = AppTextColor(),
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = if (prefLanguage == "bn") "পরিবর্তন করতে বর্তমান পিন যাচাই করুন" else "Verify current PIN to make modifications",
                                                        color = AppTextColor().copy(alpha = 0.5f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                GlassButton(
                                                    onClick = { showSecurityUnlockDialog = true },
                                                    cornerRadius = 12.dp
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = "Unlock Security",
                                                        tint = AccentCyan,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (prefLanguage == "bn") "আনলক" else "Unlock",
                                                        color = AppTextColor(),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (prefLanguage == "bn") "পাসকোড স্ক্রিন লক" else "Enable App Passcode",
                                                        color = AppTextColor(),
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = if (prefLanguage == "bn") "অ্যাপ অ্যাক্সেস করতে ৪-সংখ্যার পিন লক সক্রিয় করুন" else "Requires a 4-digit PIN lock on opening",
                                                        color = AppTextColor().copy(alpha = 0.5f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                GlassSwitch(
                                                    checked = prefAppLockEnabled,
                                                    onCheckedChange = { isCheckingOn ->
                                                        if (isCheckingOn) {
                                                            if (prefSecurityQuestion.trim().isBlank() || prefSecurityAnswer.trim().isBlank()) {
                                                                showRecoverySetupDialog = true
                                                            } else {
                                                                securityDialogMode = SecurityPinMode.SET_NEW
                                                                showSecurityPinDialog = true
                                                            }
                                                        } else {
                                                            securityDialogMode = SecurityPinMode.DISABLE
                                                            showSecurityPinDialog = true
                                                        }
                                                    },
                                                    activeText = "ON",
                                                    inactiveText = "OFF"
                                                )
                                            }

                                            if (prefAppLockEnabled) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = if (prefLanguage == "bn") "বায়োমেট্রিক আনলক" else "Use Biometric Unlock",
                                                            color = AppTextColor(),
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 14.sp
                                                        )
                                                        Text(
                                                            text = if (prefLanguage == "bn") "আঙুলের ছাপ বা ফেস আইডি ব্যবহার করুন" else "Use fingerprint or face ID to unlock",
                                                            color = AppTextColor().copy(alpha = 0.5f),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                    GlassSwitch(
                                                        checked = prefBiometricEnabled,
                                                        onCheckedChange = { prefBiometricEnabled = it },
                                                        activeText = "ON",
                                                        inactiveText = "OFF"
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Text(
                                                        text = if (prefLanguage == "bn") "রিকভারি সিকিউরিটি প্রশ্ন" else "Recovery Security Question",
                                                        color = AppTextColor(),
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = if (prefSecurityQuestion.isNotBlank()) "Q: $prefSecurityQuestion" else (if (prefLanguage == "bn") "কোন প্রশ্ন সেট করা নেই" else "No security question set"),
                                                        color = AppTextColor().copy(alpha = 0.7f),
                                                        fontSize = 12.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    OutlinedButton(
                                                        onClick = { showRecoverySetupDialog = true },
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Icon(Icons.Default.Build, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = if (prefLanguage == "bn") "রিকভারি প্রশ্ন পরিবর্তন করুন" else "Edit Recovery Method",
                                                            color = AccentCyan,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            securityDialogMode = SecurityPinMode.CHANGE
                                                            showSecurityPinDialog = true
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                                                        shape = RoundedCornerShape(16.dp),
                                                        modifier = Modifier.weight(1f).testTag("change_pin_button").bounceClick()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Lock,
                                                            contentDescription = null,
                                                            tint = AccentCyan,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (prefLanguage == "bn") "পিন পরিবর্তন" else "Change PIN",
                                                            color = AccentCyan,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    Button(
                                                        onClick = { viewModel.lockApp() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPink.copy(alpha = 0.15f)),
                                                        shape = RoundedCornerShape(16.dp),
                                                        modifier = Modifier.weight(1f).bounceClick()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Lock,
                                                            contentDescription = null,
                                                            tint = AccentPink,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (prefLanguage == "bn") "অ্যাপ লক করুন" else "Lock Now",
                                                            color = AccentPink,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showBackupRestoreGroup) {
                            item {
                                // Category: Backup & Restore (CSV)
                                PixelSettingsGroup(
                                    title = if (prefLanguage == "bn") "সম্পূর্ণ অ্যাপ ব্যাকআপ" else "Full App Backup (.trakerbackup)"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = if (prefLanguage == "bn") "অ্যাপের সমস্ত সেটিংস ও ডেটা ব্যাকআপ নিন বা রিস্টোর করুন।" else "Backup or restore all app data and settings in a single file.",
                                            color = AppTextColor().copy(alpha = 0.5f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val context = androidx.compose.ui.platform.LocalContext.current
                                            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                                            
                                            val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                                androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")
                                            ) { uri ->
                                                uri?.let {
                                                    coroutineScope.launch {
                                                        val success = com.example.data.BackupRestoreManager(context).createBackup(it)
                                                        val msg = if (success) "Backup Successful" else "Backup Failed"
                                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }

                                            val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                                androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                                            ) { uri ->
                                                uri?.let {
                                                    coroutineScope.launch {
                                                        val success = com.example.data.BackupRestoreManager(context).restoreBackup(it)
                                                        if (!success) {
                                                            android.widget.Toast.makeText(context, "Restore Failed", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        
                                            Button(
                                                onClick = { exportLauncher.launch("TrakerBackup_${System.currentTimeMillis()}.trakerbackup") },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Download, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (prefLanguage == "bn") "ব্যাকআপ তৈরি" else "Create Backup", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = { importLauncher.launch(arrayOf("*/*")) },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Upload, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (prefLanguage == "bn") "রিস্টোর করুন" else "Restore Backup", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                
                                PixelSettingsGroup(
                                    title = if (prefLanguage == "bn") "ব্যাকআপ এবং রিস্টোর (CSV)" else "Backup & Restore (CSV)"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = if (prefLanguage == "bn") "আপনার সমস্ত তথ্য সুরক্ষিত রাখতে এবং ডিভাইস পরিবর্তনের জন্য CSV ফরম্যাটে ব্যাকআপ নিন।" else "Keep your records safe or migrate devices by backing up and restoring via CSV files.",
                                            color = AppTextColor().copy(alpha = 0.5f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )

                                        var showInstructions by remember { mutableStateOf(false) }
                                        TextButton(
                                            onClick = { showInstructions = true },
                                            modifier = Modifier.align(Alignment.Start).bounceClick(),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Info, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (prefLanguage == "bn") "ইমপোর্ট করার নিয়মাবলী দেখুন" else "View Import Instructions",
                                                color = AccentCyan,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        PixelSettingsItem(
                                            icon = Icons.Default.CloudDownload,
                                            iconBgColor = Color(0xFFE3F2FD),
                                            iconTint = Color(0xFF1976D2),
                                            title = if (prefLanguage == "bn") "সম্পূর্ণ ডেটা ব্যাকআপ (JSON)" else "Backup Data (JSON)",
                                            subtitle = if (prefLanguage == "bn") "সমস্ত ডেটার পূর্ণাঙ্গ ব্যাকআপ সংরক্ষণ করুন" else "Export a full JSON backup of all data",
                                            onClick = { saveBackupFile("app_full_backup.json", viewModel.exportAllDataToJson()) }
                                        )

                                        PixelSettingsItem(
                                            icon = Icons.Default.Restore,
                                            iconBgColor = Color(0xFFE8F5E9),
                                            iconTint = Color(0xFF2E7D32),
                                            title = if (prefLanguage == "bn") "সম্পূর্ণ ডেটা রিস্টোর (JSON)" else "Restore All Data (JSON)",
                                            subtitle = if (prefLanguage == "bn") "ব্যাকআপ ফাইল থেকে সমস্ত ডেটা রিস্টোর করুন" else "Restore everything from a total backup file",
                                            onClick = { totalImportLauncher.launch("*/*") }
                                        )

                                        // Item 1: Menus Backup
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "হোটেলের মেনু তালিকা" else "Hotel Menus",
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (prefLanguage == "bn") "হোটেল মেনুর খাবার ও দামের তালিকা" else "List of food items and pricing",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { saveBackupFile("menus_backup.csv", viewModel.exportMenusToCsv()) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Download, null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Export", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = { pastedCsvText = ""; selectedHotelIdForImport = null; showMenuImportDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Upload, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Import", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                                        // Item 2: Hotel history Backup
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "হোটেল লেনদেনের ইতিহাস" else "Hotel History",
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (prefLanguage == "bn") "হোটেলের খাবার হিসাব ও ড্যাশবোর্ড ডেটা" else "Meal transactions and log history",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { saveBackupFile("hotel_history_backup.csv", viewModel.exportHotelTransactionsToCsv()) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Download, null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Export", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = { pastedCsvText = ""; selectedHotelIdForImport = null; showHotelHistoryImportDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Upload, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Import", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                                        // Item 3: Account history Backup
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "অ্যাকাউন্টের লেনদেন" else "Account History",
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (prefLanguage == "bn") "অন্যান্য খরচ ও ব্যালেন্স লেনদেনের তথ্য" else "Virtual bank transactions and extra expenses",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { saveBackupFile("account_history_backup.csv", viewModel.exportExtraExpensesToCsv()) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Download, null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Export", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = { accountHistoryImportLauncher.launch("*/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Upload, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Import", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                                        // Item 4: Electric Metre readings Backup
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "বিদ্যুৎ মিটার রিডিং" else "Meter Readings",
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (prefLanguage == "bn") "বিদ্যুৎ মিটার রিডিং ও ইউনিটের হিসাব" else "Electric meter readings history data",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { saveBackupFile("meter_readings_backup.csv", viewModel.exportMeterReadingsToCsv()) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Download, null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Export", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = { meterReadingImportLauncher.launch("*/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Upload, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Import", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                                        // Item 5: All-in-One JSON Sync (Unified File)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "অল-ইন-ওয়ান সিঙ্ক ফাইল (JSON)" else "Full Unified Sync (JSON)",
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (prefLanguage == "bn") "সব ডেটা একটি ফাইলে (সিঙ্ক এবং মাইগ্রেশন)" else "Single file for multi-device sync & migration",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Button(
                                                onClick = { saveBackupFile("all_data_sync.json", viewModel.exportAllDataToJson()) },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.CloudSync, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Export All", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showAboutGroup) {
                            item {
                                // Category: About & Legal
                                PixelSettingsGroup(
                                    title = if (prefLanguage == "bn") "সম্পর্কে ও আইনি তথ্য" else "About & Legal"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (showInfo) {
                                            PixelSettingsItem(
                                                icon = Icons.Default.Info,
                                                iconBgColor = Color(0xFFF3E5F5),
                                                iconTint = Color(0xFF7B1FA2),
                                                title = if (prefLanguage == "bn") "অ্যাপ তথ্য" else "App Information",
                                                subtitle = "Version 1.0.0 (Stable)",
                                                onClick = { showLegalInfoType = "info" }
                                            )
                                        }
                                        if (showLicense) {
                                            PixelSettingsItem(
                                                icon = Icons.Default.Gavel,
                                                iconBgColor = Color(0xFFEFEBE9),
                                                iconTint = Color(0xFF5D4037),
                                                title = if (prefLanguage == "bn") "লাইসেন্স ও শর্তাবলী" else "Licenses & Terms",
                                                subtitle = "Usage terms & credits",
                                                onClick = { showLegalInfoType = "license" }
                                            )
                                        }
                                        if (showPrivacy) {
                                            PixelSettingsItem(
                                                icon = Icons.Default.PrivacyTip,
                                                iconBgColor = Color(0xFFE1F5FE),
                                                iconTint = Color(0xFF0288D1),
                                                title = if (prefLanguage == "bn") "প্রাইভেসি পলিসি" else "Privacy Policy",
                                                subtitle = "Your data safety",
                                                onClick = { showLegalInfoType = "privacy" }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!hasAnyMatch) {
                            item {
                                EmptyStateCard(message = if (prefLanguage == "bn") "\"$settingsSearchQuery\" এর জন্য কোনো সেটিংস পাওয়া যায়নি" else "No settings found for \"$settingsSearchQuery\"")
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                // =======================================================
                // ALL DIALOGS (Styled like beautiful Pixel dynamic alerts)
                // =======================================================

                // 2. EditHotelModal (The best editing screen, unified for both Add and Edit with superior features!)
                if (manageMenuForHotel != null) {
                    MenuManagementScreen(
                        hotel = manageMenuForHotel!!,
                        viewModel = viewModel,
                        onBack = { 
                            editingHotel = manageMenuForHotel
                            manageMenuForHotel = null 
                        }
                    )
                }
                if (showAddHotelDialog || editingHotel != null) {
                    EditHotelModal(
                        hotel = editingHotel,
                        viewModel = viewModel,
                        onDismiss = {
                            showAddHotelDialog = false
                            editingHotel = null
                        },
                        onManageMenu = {
                            manageMenuForHotel = it
                            showAddHotelDialog = false
                            editingHotel = null
                        }
                    )
                }

                // 3. Unified Account Dialog (For both Add/Edit with superior features)
                if (showAddAccountDialog || editingAccount != null) {
                    UnifiedAccountDialog(
                        account = editingAccount,
                        prefLanguage = prefLanguage,
                        accountsCount = accounts.size,
                        onDismiss = {
                            showAddAccountDialog = false
                            editingAccount = null
                        },
                        onSave = { name, balance, upiId, qrCodeUri, accountNumber, ifsc, branchName ->
                            if (editingAccount != null) {
                                viewModel.editVirtualAccount(editingAccount!!.copy(name = name, balance = balance, upiId = upiId, qrCodeUri = qrCodeUri, accountNumber = accountNumber, ifsc = ifsc, branchName = branchName))
                            } else {
                                viewModel.addVirtualAccount(name, balance, upiId, qrCodeUri, accountNumber, ifsc, branchName)
                            }
                            showAddAccountDialog = false
                            editingAccount = null
                        },
                        onDelete = if (editingAccount != null) {
                            {
                                viewModel.deleteVirtualAccount(editingAccount!!)
                                editingAccount = null
                            }
                        } else null
                    )
                }

                // 4. Security Verification Dialog (To unlock settings modifications)
                if (showSecurityUnlockDialog) {
                    var currentPinInput by remember { mutableStateOf("") }
                    var currentPinError by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { showSecurityUnlockDialog = false },
                        containerColor = getDialogBackground(),
                        shape = RoundedCornerShape(28.dp),
                        title = {
                            Text(
                                text = if (prefLanguage == "bn") "নিরাপত্তা নিশ্চিতকরণ" else "Security Verification",
                                color = AppTextColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth().imePadding()) {
                                Text(
                                    text = if (prefLanguage == "bn") "সিকিউরিটি সেটিংস পরিবর্তন করতে আপনার বর্তমান ৪-সংখ্যার পিনটি লিখুন:" else "Please enter your current 4-digit PIN to modify security settings:",
                                    color = AppTextColor().copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                GlassTextField(
                                    value = currentPinInput,
                                    onValueChange = {
                                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                            currentPinInput = it
                                            currentPinError = false
                                        }
                                    },
                                    label = { Text(if (prefLanguage == "bn") "বর্তমান পিন" else "Current PIN") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    visualTransformation = PasswordVisualTransformation(),
                                    isError = currentPinError,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (currentPinError) {
                                    Text(
                                        text = if (prefLanguage == "bn") "ভুল পিন! অনুগ্রহ করে আবার চেষ্টা করুন।" else "Incorrect PIN! Please try again.",
                                        color = AccentPink,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (currentPinInput == userProfile?.appLockPin) {
                                        isPinVerifiedForSettings = true
                                        showSecurityUnlockDialog = false
                                    } else {
                                        currentPinError = true
                                        currentPinInput = ""
                                    }
                                }
                            ) {
                                Text(
                                    text = if (prefLanguage == "bn") "যাচাই করুন" else "Verify",
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSecurityUnlockDialog = false }) {
                                Text(
                                    text = if (prefLanguage == "bn") "বাতিল" else "Cancel",
                                    color = AppTextColor().copy(alpha = 0.5f)
                                )
                            }
                        }
                    )
                }

                if (showSecurityPinDialog) {
                    SecurityPinDialog(
                        mode = securityDialogMode,
                        currentSavedPin = prefAppLockPin,
                        prefLanguage = prefLanguage,
                        onDismiss = { showSecurityPinDialog = false },
                        onSuccess = { newPin, enabled ->
                            if (enabled) {
                                if (newPin != null) {
                                    prefAppLockPin = newPin
                                }
                                prefAppLockEnabled = true
                            } else {
                                prefAppLockEnabled = false
                            }
                            showSecurityPinDialog = false
                        }
                    )
                }

                if (showRecoverySetupDialog) {
                    RecoverySetupDialog(
                        currentQuestion = prefSecurityQuestion,
                        currentAnswer = prefSecurityAnswer,
                        prefLanguage = prefLanguage,
                        onDismiss = { showRecoverySetupDialog = false },
                        onSave = { q, a ->
                            prefSecurityQuestion = q
                            prefSecurityAnswer = a
                            showRecoverySetupDialog = false
                            viewModel.updatePreferences(
                                language = prefLanguage,
                                themeMode = prefThemeMode,
                                showAdvancedPayment = prefShowAdvancedPayment,
                                appLockEnabled = prefAppLockEnabled,
                                appLockPin = prefAppLockPin,
                                defaultHotelId = prefDefaultHotelId,
                                enableStarAnimation = prefEnableStarAnimation,
                                textSize = prefTextSize,
                                biometricEnabled = prefBiometricEnabled,
                                securityQuestion = q,
                                securityAnswer = a
                            )
                            if (!prefAppLockEnabled) {
                                securityDialogMode = SecurityPinMode.SET_NEW
                                showSecurityPinDialog = true
                            }
                        }
                    )
                }

                if (showLegalInfoType != null) {
                    LegalInfoDialog(
                        type = showLegalInfoType!!,
                        prefLanguage = prefLanguage,
                        onDismiss = { showLegalInfoType = null }
                    )
                }

        }
    }
}
}


@Composable
fun PixelSettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = AccentCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}


@Composable
fun PixelSettingsItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AppTextColor(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = AppTextColor().copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = AppTextColor().copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAccountDialog(
    account: BankAccountEntity?,
    prefLanguage: String,
    accountsCount: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, balance: Double, upiId: String, qrCodeUri: String, accountNumber: String, ifsc: String, branchName: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var accName by remember(account) { mutableStateOf(account?.name ?: "") }
    var accBal by remember(account) { mutableStateOf(account?.balance?.toString() ?: "") }
    var upiId by remember(account) { mutableStateOf(account?.upiId ?: "") }
    var accountNumber by remember(account) { mutableStateOf(account?.accountNumber ?: "") }
    var ifsc by remember(account) { mutableStateOf(account?.ifsc ?: "") }
    var branchName by remember(account) { mutableStateOf(account?.branchName ?: "") }
    var qrCodeUri by remember(account) { mutableStateOf(account?.qrCodeUri ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    if (showDeleteConfirm) {
        com.example.ui.components.ConfirmDeleteDialog(
            itemName = accName,
            requireTypedConfirmation = true,
            onConfirm = { onDelete?.invoke(); showDeleteConfirm = false; onDismiss() },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    androidx.activity.compose.BackHandler { onDismiss() }
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
                .background(getAppBackgroundGradient())
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
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
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (account == null) {
                                if (prefLanguage == "bn") "নতুন অ্যাকাউন্ট যুক্ত করুন" else "Add Virtual Account"
                            } else {
                                if (prefLanguage == "bn") "অ্যাকাউন্ট সংশোধন" else "Edit Account Details"
                            },
                            color = AppTextColor(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (accName.isNotBlank()) {
                                    val bal = accBal.toDoubleOrNull() ?: 0.0
                                    onSave(accName, bal, upiId, qrCodeUri, accountNumber, ifsc, branchName)
                                } else {
                                    android.widget.Toast.makeText(
                                        onDismiss.javaClass.enclosingClass?.run { null } ?: run { null },
                                        if (prefLanguage == "bn") "অ্যাকাউন্টের নাম দিন" else "Enter Account Name",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp).testTag("save_account_btn").bounceClick()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (account == null) {
                                    if (prefLanguage == "bn") "যুক্ত করুন" else "Register"
                                } else {
                                    if (prefLanguage == "bn") "সংরক্ষণ" else "Save"
                                },
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                bottomBar = {
                    if (account != null && accountsCount > 1 && onDelete != null) {
                        Surface(
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var showMenu by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.wrapContentSize(Alignment.BottomStart)) {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.background(AppTextColor().copy(alpha = 0.1f), CircleShape).bounceClick()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More Options",
                                            tint = AppTextColor()
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        modifier = Modifier.background(getDialogBackground())
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(if (prefLanguage == "bn") "মুছে ফেলুন" else "Delete", color = AccentPink) },
                                            onClick = {
                                                showMenu = false
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
                                TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_account_btn").bounceClick()) {
                                    Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AccentPink, fontWeight = FontWeight.Bold)
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
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text(if (prefLanguage == "bn") "অ্যাকাউন্টের নাম" else "Account Name") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().testTag("account_name_input")
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        GlassTextField(
                            value = ifsc,
                            onValueChange = { ifsc = it },
                            label = { Text("IFSC Code") },
                            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                        GlassTextField(
                            value = branchName,
                            onValueChange = { branchName = it },
                            label = { Text("Branch") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
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
                    GlassTextField(
                        value = accBal,
                        onValueChange = { accBal = it },
                        label = {
                            Text(
                                if (account == null) {
                                    if (prefLanguage == "bn") "প্রারম্ভিক ব্যালেন্স (₹)" else "Starting Balance (₹)"
                                } else {
                                    if (prefLanguage == "bn") "তরল ব্যালেন্স (₹)" else "Liquid Balance (₹)"
                                }
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("account_balance_input")
                    )
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
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                Text(title, color = AppTextColor(), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val content = when (type) {
                    "info" -> if (prefLanguage == "bn") """
                        FinNexus v1.0.0 (Nexus Finance)
                        
                        Nexus Finance একটি আধুনিক, অফলাইন-ফার্স্ট ব্যক্তিগত আর্থিক ব্যবস্থাপনা অ্যাপ। এটি আপনার খরচ ট্র্যাক করা, একাধিক অ্যাকাউন্ট পরিচালনা এবং মিটারের রিডিং নিখুঁতভাবে পর্যবেক্ষণ করতে সাহায্য করে।
                        
                        Jetpack Compose ও Kotlin দিয়ে তৈরি।
                        © ২০২৬ Nexus দেব টিম।
                    """.trimIndent() else """
                        FinNexus v1.0.0 (Nexus Finance)
                        
                        Nexus Finance is a modern, offline-first personal financial management tool designed for individuals and business owners to track expenses, manage multiple accounts, and monitor meter readings with precision.
                        
                        Developed with Jetpack Compose & Kotlin.
                        © 2026 Nexus Dev Team.
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
                        
                        FinNexus একটি অফলাইন-ফার্স্ট অ্যাপ্লিকেশন। এর অর্থ:
                        ১. আপনার সমস্ত তথ্য শুধুমাত্র আপনার ফোনেই সংরক্ষিত থাকে।
                        ২. কোনো আর্থিক তথ্য কোনো সার্ভারে পাঠানো হয় না।
                        ৩. আমরা আপনার কোনো ব্যক্তিগত তথ্য সংগ্রহ বা বিক্রি করি না।
                        
                        আপনার তথ্য সম্পূর্ণ আপনার নিয়ন্ত্রণে।
                    """.trimIndent() else """
                        Your privacy is our priority.
                        
                        FinNexus is an OFFLINE-FIRST application. This means:
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
                    color = AccentCyan,
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
    prefLanguage: String,
    onDismiss: () -> Unit,
    onSuccess: (newPin: String?, enabled: Boolean) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var enteredPin1 by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val title = when (mode) {
        SecurityPinMode.SET_NEW -> if (prefLanguage == "bn") "নতুন পিন সেট করুন" else "Set New PIN"
        SecurityPinMode.DISABLE -> if (prefLanguage == "bn") "পিন লক নিষ্ক্রিয় করুন" else "Disable PIN Lock"
        SecurityPinMode.CHANGE -> if (prefLanguage == "bn") "পিন পরিবর্তন করুন" else "Change Security PIN"
    }

    val instructions = when (mode) {
        SecurityPinMode.SET_NEW -> {
            if (step == 1) {
                if (prefLanguage == "bn") "একটি ৪-সংখ্যার সিকিউরিটি পিন লিখুন" else "Enter a new 4-digit security PIN"
            } else {
                if (prefLanguage == "bn") "পিনটি নিশ্চিত করতে আবার লিখুন" else "Enter PIN again to confirm"
            }
        }
        SecurityPinMode.DISABLE -> {
            if (prefLanguage == "bn") "পাসকোড নিষ্ক্রিয় করতে আপনার বর্তমান পিনটি লিখুন" else "Enter your current PIN to disable app lock"
        }
        SecurityPinMode.CHANGE -> {
            when (step) {
                1 -> if (prefLanguage == "bn") "আপনার বর্তমান ৪-সংখ্যার পিনটি লিখুন" else "Enter your current 4-digit security PIN"
                2 -> if (prefLanguage == "bn") "একটি নতুন ৪-সংখ্যার পিন লিখুন" else "Enter a new 4-digit security PIN"
                else -> if (prefLanguage == "bn") "নতুন পিনটি নিশ্চিত করতে আবার লিখুন" else "Enter new PIN again to confirm"
            }
        }
    }

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
                        val color = if (errorMessage != null) AccentPink else if (active) AccentCyan else AppTextColor().copy(alpha = 0.2f)
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
                                                            // Trigger action based on mode & step
                                                            val input = pinInput
                                                            when (mode) {
                                                                SecurityPinMode.SET_NEW -> {
                                                                    if (step == 1) {
                                                                        enteredPin1 = input
                                                                        pinInput = ""
                                                                        step = 2
                                                                    } else {
                                                                        if (input == enteredPin1) {
                                                                            onSuccess(input, true)
                                                                        } else {
                                                                            errorMessage = if (prefLanguage == "bn") "পিন দুটি মেলেনি! অনুগ্রহ করে নতুন পিন দিয়ে আবার চেষ্টা করুন।" else "PINs do not match! Please enter a new PIN again."
                                                                            enteredPin1 = ""
                                                                            pinInput = ""
                                                                            step = 1
                                                                        }
                                                                    }
                                                                }
                                                                SecurityPinMode.DISABLE -> {
                                                                    if (input == currentSavedPin) {
                                                                        onSuccess(null, false)
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
                                                                                onSuccess(input, true)
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
        },
        confirmButton = {},
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            colors = RadioButtonDefaults.colors(selectedColor = AccentCyan)
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (prefLanguage == "bn") "সংরক্ষণ করুন" else "Save & Continue",
                    color = Color.Black,
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
