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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val accounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val userCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

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
    
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localUri = com.example.ui.screens.copyUriToLocalFile(context, it, "IMG")
            attachments = attachments + (localUri to "IMAGE")
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localUri = com.example.ui.screens.copyUriToLocalFile(context, it, "PDF")
            attachments = attachments + (localUri to "PDF")
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localUri = com.example.ui.screens.copyUriToLocalFile(context, it, "FILE")
            attachments = attachments + (localUri to "FILE")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        try {
            bitmap?.let {
                // Save bitmap to file and add to attachments
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

    val isFormValid = remember(name, amountStr, category) {
        name.isNotBlank() && amountStr.toDoubleOrNull() != null && (amountStr.toDoubleOrNull() ?: 0.0) > 0 && category.isNotBlank()
    }

    androidx.activity.compose.BackHandler { onDismiss() }

    if (true) {
        com.example.ui.components.AppThemeProvider(viewModel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.example.ui.components.getAppBackgroundGradient(userProfile.themeMode))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .background(getGlassBorder().copy(alpha = 0.1f), CircleShape)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTextColor()
                        )
                    }
                    Text(
                        text = if (type == "EXPENSE") "Add Expense" else "Add Income",
                        color = AppTextColor(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    // High-contrast Save Button
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                android.widget.Toast.makeText(context, if (userProfile.language == "bn") "দয়া করে শিরোনাম লিখুন" else "Please enter a title", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val amt = amountStr.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                android.widget.Toast.makeText(context, if (userProfile.language == "bn") "দয়া করে সঠিক পরিমাণ লিখুন" else "Please enter a valid amount", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            var accId = selectedAccountId
                            if (accId == 0) {
                                if (accounts.isNotEmpty()) {
                                    accId = accounts.first().id
                                } else {
                                    android.widget.Toast.makeText(context, if (userProfile.language == "bn") "দয়া করে অ্যাকাউন্ট সিলেক্ট করুন" else "Please select an account", android.widget.Toast.LENGTH_SHORT).show()
                                    return@Button
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
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .testTag("save_transaction_button")
                            .bounceClick()
                    ) {
                        Text(
                            text = if (userProfile.language == "bn") "সংরক্ষণ" else "Save",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 4.dp)
                ) {
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
                                    .background(if (isExpense) AccentPink.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { type = "EXPENSE"; if (category == "Salary") category = "General" }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Expense", color = if (isExpense) AccentPink else AppTextColor().copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isExpense) AccentGreen.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { type = "INCOME"; if (category == "General") category = "Salary" }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Income", color = if (!isExpense) AccentGreen else AppTextColor().copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Amount Input
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Amount (₹)", color = AppTextColor().copy(alpha = 0.5f), fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicTextField(
                                    value = amountStr,
                                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountStr = it },
                                    textStyle = TextStyle(
                                        color = AppTextColor(),
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    cursorBrush = SolidColor(AccentCyan),
                                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input"),
                                    decorationBox = { inner ->
                                        if (amountStr.isEmpty()) Text("0.00", color = AppTextColor().copy(alpha = 0.1f), fontSize = 48.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                        inner()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Details
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Title
                            GlassTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Title") },
                                placeholder = { Text("e.g. Fuel, Lunch") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )

                            // Account
                            Column {
                                Text("Wallet / Account", color = AppTextColor().copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box {
                                    val activeAcc = accounts.find { it.id == selectedAccountId }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            .border(1.dp, AppTextColor().copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .clickable { showAccountDropdown = true }
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(activeAcc?.name ?: "Select Account", color = AppTextColor())
                                        Icon(Icons.Default.ArrowDropDown, null, tint = AppTextColor().copy(alpha = 0.5f))
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
                                Text("Category", color = AppTextColor().copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            .border(1.dp, AppTextColor().copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .clickable { showCategoryDropdown = true }
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(category, color = AppTextColor())
                                        Icon(Icons.Default.ArrowDropDown, null, tint = AppTextColor().copy(alpha = 0.5f))
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
                                            text = { Text("+ Custom", color = AccentCyan) },
                                            onClick = { showCategoryDropdown = false; showAddCategoryDialog = true }
                                        )
                                    }
                                }
                            }

                            // Notes
                            GlassTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Notes (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date/Time
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Date & Time", color = AppTextColor().copy(alpha = 0.5f), fontSize = 11.sp)
                                Text(sdf.format(Date(timestamp)), color = AppTextColor(), fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                    android.app.DatePickerDialog(context, { _, y, m, d ->
                                        cal.set(y, m, d)
                                        timestamp = cal.timeInMillis
                                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                }, modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)) {
                                    Icon(Icons.Default.CalendarToday, null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                    android.app.TimePickerDialog(context, { _, h, m ->
                                        cal.set(Calendar.HOUR_OF_DAY, h)
                                        cal.set(Calendar.MINUTE, m)
                                        timestamp = cal.timeInMillis
                                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                                }, modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)) {
                                    Icon(Icons.Default.Schedule, null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Attachments
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Attachments", color = AppTextColor().copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Horizontal flow of action chips
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AttachmentActionChip(icon = Icons.Default.CameraAlt, label = "Camera", color = AccentCyan) {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                                AttachmentActionChip(icon = Icons.Default.Image, label = "Gallery", color = AccentCyan) {
                                    imagePicker.launch("image/*")
                                }
                                AttachmentActionChip(icon = Icons.Default.PictureAsPdf, label = "PDF", color = AccentGold) {
                                    pdfPicker.launch("application/pdf")
                                }
                                AttachmentActionChip(icon = Icons.Default.AttachFile, label = "File", color = AccentGold) {
                                    filePicker.launch("*/*")
                                }
                            }

                            if (attachments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    attachments.forEach { (uri, type) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when(type) {
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
                                                Text(uri.lastPathSegment ?: "Attachment", color = AppTextColor(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
                                            }
                                            IconButton(onClick = { attachments = attachments.filter { it.first != uri } }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        }
    }

    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                Box(modifier = Modifier.imePadding()) {
                    GlassTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Name") },
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
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun startRecording(context: Context, recorder: MutableState<MediaRecorder?>, setRecording: (Boolean) -> Unit, setUri: (Uri?) -> Unit) {
    try {
        val fileName = "VOICE_${System.currentTimeMillis()}.amr"
        val file = File(context.cacheDir, fileName)
        val uri = Uri.fromFile(file)
        setUri(uri)
        
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        r.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
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
    }
}
