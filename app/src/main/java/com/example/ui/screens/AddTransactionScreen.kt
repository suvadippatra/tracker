@file:Suppress("DEPRECATION")
package com.example.ui.screens

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinancialViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

fun copyUriToLocalFile(context: Context, uri: Uri, prefix: String): Uri {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val extension = when (prefix) {
                "IMG" -> "jpg"
                "PDF" -> "pdf"
                else -> "bin"
            }
            val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$extension")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return Uri.fromFile(file)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return uri
}

@Composable
fun AddTransactionScreen(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    androidx.activity.compose.BackHandler { onDismiss() }

    val accounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val userCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBengali = userProfile?.language == "bn"

    val themeMode = userProfile?.themeMode ?: "Auto"
    val bgPreset = userProfile?.bgPreset ?: "Midnight Glow"
    val customBgUri = userProfile?.customBgUri ?: ""
    val isLiquidGlass = userProfile?.enableLiquidGlassTheme ?: false

    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0) }
    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    
    var type by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var category by remember { mutableStateOf("General") }
    var notes by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf(listOf<Pair<Uri, String>>()) } // uri to type

    var showAccountDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingTimer by remember { mutableStateOf(0) }
    val mediaRecorder = remember { mutableStateOf<MediaRecorder?>(null) }
    var currentVoiceUri by remember { mutableStateOf<Uri?>(null) }
    var playingVoiceUri by remember { mutableStateOf<Uri?>(null) }
    val mediaPlayerRef = remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayerRef.value?.apply { stop(); release() }
            } catch (_: Exception) {}
            mediaPlayerRef.value = null
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording(context, mediaRecorder, { isRecording = it }, { currentVoiceUri = it })
        } else {
            android.widget.Toast.makeText(context, "Microphone permission denied", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localUri = copyUriToLocalFile(context, it, "IMG")
            attachments = attachments + (localUri to "IMAGE")
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localUri = copyUriToLocalFile(context, it, "PDF")
            attachments = attachments + (localUri to "PDF")
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localUri = copyUriToLocalFile(context, it, "FILE")
            attachments = attachments + (localUri to "FILE")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        try {
            bitmap?.let {
                val file = File(context.cacheDir, "CAM_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                attachments = attachments + (Uri.fromFile(file) to "IMAGE")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            android.widget.Toast.makeText(context, "Camera permission denied", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording && recordingTimer < 120) {
                delay(1000)
                recordingTimer++
            }
            if (recordingTimer >= 120) {
                stopRecording(context, mediaRecorder, { isRecording = it }, { currentVoiceUri }, { attachments = attachments + (it to "VOICE") })
            }
        } else {
            recordingTimer = 0
        }
    }

    LaunchedEffect(accounts) {
        if ((selectedAccountId == 0 || accounts.none { it.id == selectedAccountId }) && accounts.isNotEmpty()) {
            selectedAccountId = accounts.first().id
        }
    }

    Dialog(
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
                    if (customBgUri.isNotBlank()) {
                        AsyncImage(
                            model = customBgUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isAppLightMode()) Color.White.copy(alpha = 0.40f)
                                    else Color.Black.copy(alpha = 0.50f)
                                )
                        )
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0.dp),
                        topBar = {
                            Surface(
                                color = if (!isLiquidGlass) {
                                    if (isAppLightMode()) com.example.ui.theme.M3SurfaceLight.copy(alpha = 0.95f) else com.example.ui.theme.M3SurfaceDark.copy(alpha = 0.95f)
                                } else {
                                    if (isAppLightMode()) Color.White.copy(alpha = 0.85f) else Color(0xFF121620).copy(alpha = 0.85f)
                                },
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .dialogSafeTopPadding(extraTop = 4.dp)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
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
                                            contentDescription = if (isBengali) "ফিরে যান" else "Back",
                                            tint = AppTextColor(),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isBengali) {
                                            if (type == "EXPENSE") "খরচ যোগ করুন" else "আয় যোগ করুন"
                                        } else {
                                            if (type == "EXPENSE") "Add Expense" else "Add Income"
                                        },
                                        color = AppTextColor(),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Save Button
                                    GlassButton(
                                        onClick = {
                                            if (name.isBlank()) {
                                                android.widget.Toast.makeText(context, if (isBengali) "দয়া করে শিরোনাম লিখুন" else "Please enter a title", android.widget.Toast.LENGTH_SHORT).show()
                                                return@GlassButton
                                            }
                                            val amt = amountStr.toDoubleOrNull()
                                            if (amt == null || amt <= 0) {
                                                android.widget.Toast.makeText(context, if (isBengali) "দয়া করে সঠিক পরিমাণ লিখুন" else "Please enter a valid amount", android.widget.Toast.LENGTH_SHORT).show()
                                                return@GlassButton
                                            }
                                            var accId = selectedAccountId
                                            if (accId == 0) {
                                                if (accounts.isNotEmpty()) {
                                                    accId = accounts.first().id
                                                } else {
                                                    android.widget.Toast.makeText(context, if (isBengali) "দয়া করে অ্যাকাউন্ট সিলেক্ট করুন" else "Please select an account", android.widget.Toast.LENGTH_SHORT).show()
                                                    return@GlassButton
                                                }
                                            }
                                            viewModel.saveExtraExpense(
                                                name = name,
                                                amount = amt,
                                                bankAccountId = accId,
                                                timestamp = timestamp,
                                                receiptUri = null,
                                                type = type,
                                                category = category,
                                                notes = notes,
                                                attachments = attachments.map { it.first.toString() to it.second }
                                            )
                                            onDismiss()
                                        },
                                        cornerRadius = 10.dp,
                                        borderBrush = Brush.linearGradient(listOf(AppAccentColor(), AccentPink)),
                                        modifier = Modifier.testTag("save_transaction_button")
                                    ) {
                                        Text(
                                            text = if (isBengali) "সংরক্ষণ" else "Save",
                                            color = AppTextColor(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Cylindrical Dial Inline Date Picker (exactly like Add Meal Record page)
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    InlineDatePicker(
                                        initialDateMillis = timestamp,
                                        onDateSelected = { newDate ->
                                            val currentCal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                            val newCal = Calendar.getInstance().apply {
                                                timeInMillis = newDate
                                                set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                                set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                                                set(Calendar.SECOND, currentCal.get(Calendar.SECOND))
                                            }
                                            timestamp = newCal.timeInMillis
                                        },
                                        textColor = AppTextColor()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Type Switcher
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(4.dp)
                                ) {
                                    val isExpense = type == "EXPENSE"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isExpense) AccentPink.copy(alpha = 0.25f) else Color.Transparent)
                                            .clickable { type = "EXPENSE"; if (category == "Salary") category = "General" }
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (isBengali) "খরচ" else "Expense",
                                            color = if (isExpense) AccentPink else AppTextColor().copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (!isExpense) AccentGreen.copy(alpha = 0.25f) else Color.Transparent)
                                            .clickable { type = "INCOME"; if (category == "General") category = "Salary" }
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (isBengali) "আয়" else "Income",
                                            color = if (!isExpense) AccentGreen else AppTextColor().copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Amount Input
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        if (isBengali) "পরিমাণ (₹)" else "Amount (₹)",
                                        color = AppTextColor().copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    BasicTextField(
                                        value = amountStr,
                                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountStr = it },
                                        textStyle = TextStyle(
                                            color = AppTextColor(),
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center
                                        ),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Next
                                        ),
                                        cursorBrush = SolidColor(AppAccentColor()),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("transaction_amount_input"),
                                        decorationBox = { inner ->
                                            if (amountStr.isEmpty()) {
                                                Text(
                                                    "0.00",
                                                    color = AppTextColor().copy(alpha = 0.15f),
                                                    fontSize = 44.sp,
                                                    fontWeight = FontWeight.Black,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            inner()
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Details Card
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Title
                                    GlassTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text(if (isBengali) "শিরোনাম" else "Title") },
                                        placeholder = { Text(if (isBengali) "যেমন: জ্বালানি, দুপুরের খাবার" else "e.g. Fuel, Lunch") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                    )

                                    // Account
                                    Column {
                                        Text(
                                            if (isBengali) "অ্যাকাউন্ট / ওয়ালেট" else "Wallet / Account",
                                            color = AppTextColor().copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box {
                                            val activeAcc = accounts.find { it.id == selectedAccountId }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp)
                                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                    .border(1.dp, AppTextColor().copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                                    .clickable { showAccountDropdown = true }
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(activeAcc?.name ?: if (isBengali) "অ্যাকাউন্ট নির্বাচন করুন" else "Select Account", color = AppTextColor())
                                                Icon(Icons.Default.ArrowDropDown, null, tint = AppTextColor().copy(alpha = 0.6f))
                                            }
                                            DropdownMenu(
                                                expanded = showAccountDropdown,
                                                onDismissRequest = { showAccountDropdown = false },
                                                modifier = Modifier.background(getDialogBackground())
                                            ) {
                                                accounts.forEach { acc ->
                                                    DropdownMenuItem(
                                                        text = { Text(acc.name, color = AppTextColor()) },
                                                        onClick = { selectedAccountId = acc.id; showAccountDropdown = false }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Category
                                    Column {
                                        Text(
                                            if (isBengali) "বিভাগ" else "Category",
                                            color = AppTextColor().copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp)
                                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                    .border(1.dp, AppTextColor().copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                                    .clickable { showCategoryDropdown = true }
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(category, color = AppTextColor())
                                                Icon(Icons.Default.ArrowDropDown, null, tint = AppTextColor().copy(alpha = 0.6f))
                                            }
                                            DropdownMenu(
                                                expanded = showCategoryDropdown,
                                                onDismissRequest = { showCategoryDropdown = false },
                                                modifier = Modifier.background(getDialogBackground())
                                            ) {
                                                val cats = if (type == "EXPENSE") ExpenseCategories else IncomeCategories
                                                val allCats = (cats + userCategories.filter { it.type == type }.map { it.name }).distinct()
                                                allCats.forEach { cat ->
                                                    DropdownMenuItem(
                                                        text = { Text(cat, color = AppTextColor()) },
                                                        onClick = { category = cat; showCategoryDropdown = false }
                                                    )
                                                }
                                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                                DropdownMenuItem(
                                                    text = { Text(if (isBengali) "+ নতুন বিভাগ" else "+ Custom Category", color = AppAccentColor()) },
                                                    onClick = { showCategoryDropdown = false; showAddCategoryDialog = true }
                                                )
                                            }
                                        }
                                    }

                                    // Time of Day
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                if (isBengali) "সময়" else "Time",
                                                color = AppTextColor().copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                sdf.format(Date(timestamp)),
                                                color = AppTextColor(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                                android.app.TimePickerDialog(context, { _, h, m ->
                                                    cal.set(Calendar.HOUR_OF_DAY, h)
                                                    cal.set(Calendar.MINUTE, m)
                                                    timestamp = cal.timeInMillis
                                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                                            },
                                            modifier = Modifier.background(getGlassIconButtonBg(), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Schedule, contentDescription = "Time", tint = AppAccentColor(), modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    // Notes
                                    GlassTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text(if (isBengali) "নোট (ঐচ্ছিক)" else "Notes (Optional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = false,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Attachments Card
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        if (isBengali) "সংযুক্তি" else "Attachments",
                                        color = AppTextColor().copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AttachmentActionChip(icon = Icons.Default.CameraAlt, label = if (isBengali) "ক্যামেরা" else "Camera", color = AppAccentColor()) {
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                        AttachmentActionChip(icon = Icons.Default.Image, label = if (isBengali) "গ্যালারি (≤2MB)" else "Image (≤2MB)", color = AppAccentColor()) {
                                            imagePicker.launch("image/*")
                                        }
                                        AttachmentActionChip(icon = Icons.Default.PictureAsPdf, label = if (isBengali) "পিডিএফ" else "PDF (Unlimited)", color = AccentGold) {
                                            pdfPicker.launch("application/pdf")
                                        }
                                        AttachmentActionChip(
                                            icon = if (isRecording) Icons.Default.StopCircle else Icons.Default.Mic,
                                            label = if (isRecording) {
                                                val mm = recordingTimer / 60
                                                val ss = recordingTimer % 60
                                                String.format(Locale.US, "Stop (%02d:%02d/02:00)", mm, ss)
                                            } else {
                                                if (isBengali) "ভয়েস (২ মি.)" else "Voice (Max 2m)"
                                            },
                                            color = if (isRecording) AccentPink else AccentGreen
                                        ) {
                                            if (isRecording) {
                                                stopRecording(
                                                    context = context,
                                                    recorder = mediaRecorder,
                                                    setRecording = { isRecording = it },
                                                    currentUri = { currentVoiceUri },
                                                    onComplete = { recordedUri ->
                                                        attachments = attachments + (recordedUri to "VOICE")
                                                    }
                                                )
                                            } else {
                                                audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                        AttachmentActionChip(icon = Icons.Default.AttachFile, label = if (isBengali) "ফাইল" else "File", color = AccentGold) {
                                            filePicker.launch("*/*")
                                        }
                                    }

                                    if (isRecording) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = AccentPink.copy(alpha = 0.14f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = AccentPink, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isBengali) "ভয়েস মেসেজ রেকর্ড হচ্ছে... (${recordingTimer}s / 120s)" else "Recording compressed voice note... (${recordingTimer}s / 120s)",
                                                        color = AppTextColor(),
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                TextButton(
                                                    onClick = {
                                                        stopRecording(
                                                            context = context,
                                                            recorder = mediaRecorder,
                                                            setRecording = { isRecording = it },
                                                            currentUri = { currentVoiceUri },
                                                            onComplete = { recordedUri ->
                                                                attachments = attachments + (recordedUri to "VOICE")
                                                            }
                                                        )
                                                    }
                                                ) {
                                                    Text(if (isBengali) "থামান" else "Stop", color = AccentPink, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    if (attachments.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            attachments.forEach { (uri, fileType) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = when(fileType) {
                                                                "IMAGE" -> Icons.Default.Image
                                                                "PDF" -> Icons.Default.PictureAsPdf
                                                                "VOICE" -> Icons.Default.Mic
                                                                else -> Icons.Default.AttachFile
                                                            },
                                                            contentDescription = null,
                                                            tint = AppTextColor().copy(alpha = 0.6f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            uri.lastPathSegment ?: "Attachment",
                                                            color = AppTextColor(),
                                                            fontSize = 12.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (fileType == "VOICE") {
                                                            val isPlayingThis = playingVoiceUri == uri
                                                            IconButton(
                                                                onClick = {
                                                                    try {
                                                                        if (isPlayingThis) {
                                                                            mediaPlayerRef.value?.apply { stop(); release() }
                                                                            mediaPlayerRef.value = null
                                                                            playingVoiceUri = null
                                                                        } else {
                                                                            mediaPlayerRef.value?.apply { stop(); release() }
                                                                            val mp = android.media.MediaPlayer().apply {
                                                                                setDataSource(context, uri)
                                                                                prepare()
                                                                                setOnCompletionListener {
                                                                                    playingVoiceUri = null
                                                                                    it.release()
                                                                                    mediaPlayerRef.value = null
                                                                                }
                                                                                start()
                                                                            }
                                                                            mediaPlayerRef.value = mp
                                                                            playingVoiceUri = uri
                                                                        }
                                                                    } catch (e: Exception) {
                                                                        e.printStackTrace()
                                                                        playingVoiceUri = null
                                                                    }
                                                                },
                                                                modifier = Modifier.size(28.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                                    contentDescription = "Play Voice",
                                                                    tint = AppAccentColor(),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                if (playingVoiceUri == uri) {
                                                                    try { mediaPlayerRef.value?.apply { stop(); release() } } catch (_: Exception) {}
                                                                    mediaPlayerRef.value = null
                                                                    playingVoiceUri = null
                                                                }
                                                                attachments = attachments.filter { it.first != uri }
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
    }

    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(if (isBengali) "নতুন বিভাগ" else "New Category") },
            text = {
                Box(modifier = Modifier.imePadding()) {
                    GlassTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text(if (isBengali) "নাম" else "Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCatName.isNotBlank()) {
                        viewModel.addCategory(newCatName, type)
                        category = newCatName
                        showAddCategoryDialog = false
                    }
                }) { Text(if (isBengali) "যোগ করুন" else "Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text(if (isBengali) "বাতিল" else "Cancel") }
            }
        )
    }
}

private fun startRecording(context: Context, recorder: MutableState<MediaRecorder?>, setRecording: (Boolean) -> Unit, setUri: (Uri?) -> Unit) {
    try {
        val fileName = "VOICE_${System.currentTimeMillis()}.m4a"
        val file = File(context.cacheDir, fileName)
        val uri = Uri.fromFile(file)
        setUri(uri)
        
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        r.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64000)
            setAudioSamplingRate(22050)
            setMaxDuration(120_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder.value = r
        setRecording(true)
    } catch (e: Exception) {
        Log.e("AddTransaction", "Record start failed", e)
    }
}

private fun stopRecording(context: Context, recorder: MutableState<MediaRecorder?>, setRecording: (Boolean) -> Unit, currentUri: () -> Uri?, onComplete: (Uri) -> Unit) {
    try {
        recorder.value?.apply {
            stop()
            release()
        }
        recorder.value = null
        setRecording(false)
        currentUri()?.let { onComplete(it) }
    } catch (e: Exception) {
        Log.e("AddTransaction", "Record stop failed", e)
        recorder.value = null
        setRecording(false)
    }
}
