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
import java.text.SimpleDateFormat
import java.util.*

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
        uri?.let { attachments = attachments + (it to "IMAGE") }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { attachments = attachments + (it to "PDF") }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { attachments = attachments + (it to "FILE") }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            // Save bitmap to file and add to attachments
            val file = File(context.cacheDir, "CAM_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            attachments = attachments + (Uri.fromFile(file) to "IMAGE")
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

    val isFormValid = remember(name, amountStr, category) {
        name.isNotBlank() && amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && category.isNotBlank()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        com.example.ui.components.AppThemeProvider(viewModel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.example.ui.components.getAppBackgroundGradient(userProfile.themeMode))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(getGlassBorder().copy(alpha = 0.1f), CircleShape)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTextColor()
                        )
                    }
                    Text(
                        text = if (type == "EXPENSE") "Add Expense" else "Add Income",
                        color = AppTextColor(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    // Modern Save Button
                    GlassButton(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull()
                            if (isFormValid && amt != null && selectedAccountId != 0) {
                                viewModel.saveExtraExpense(
                                    name = name,
                                    amount = amt,
                                    bankAccountId = selectedAccountId,
                                    timestamp = timestamp,
                                    receiptUri = null,
                                    type = type,
                                    category = category,
                                    notes = notes,
                                    attachments = attachments.map { it.first.toString() to it.second }
                                )
                                onDismiss()
                            }
                        },
                        cornerRadius = 12.dp,
                        borderBrush = Brush.linearGradient(listOf(AccentCyan, AccentPink)),
                        modifier = Modifier.testTag("save_transaction_button").height(40.dp).width(80.dp)
                    ) {
                        Text(
                            text = "Save",
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
                        .padding(horizontal = 16.dp)
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
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Title") },
                                placeholder = { Text("e.g. Fuel, Lunch") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = AppTextColor().copy(alpha = 0.1f)
                                )
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
                                        modifier = Modifier.background(Color(0xFF1E293B))
                                    ) {
                                        accounts.forEach { acc ->
                                            DropdownMenuItem(
                                                text = { Text(acc.name, color = Color.White) },
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
                                        modifier = Modifier.background(Color(0xFF1E293B))
                                    ) {
                                        val cats = if (type == "EXPENSE") ExpenseCategories else IncomeCategories
                                        val allCats = (cats + userCategories.filter { it.type == type }.map { it.name }).distinct()
                                        allCats.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat, color = Color.White) },
                                                onClick = { category = cat; showCategoryDropdown = false }
                                            )
                                        }
                                        Divider(color = Color.White.copy(alpha = 0.1f))
                                        DropdownMenuItem(
                                            text = { Text("+ Custom", color = AccentCyan) },
                                            onClick = { showCategoryDropdown = false; showAddCategoryDialog = true }
                                        )
                                    }
                                }
                            }

                            // Notes
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Notes (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = AppTextColor().copy(alpha = 0.1f)
                                )
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
                                    cameraLauncher.launch(null)
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
                                AttachmentActionChip(
                                    icon = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    label = if (isRecording) "${120 - recordingTimer}s" else "Voice",
                                    color = if (isRecording) Color.Red else AccentPink
                                ) {
                                    if (isRecording) {
                                        stopRecording(context, mediaRecorder, { isRecording = it }, { currentVoiceUri }, { attachments = attachments + (it to "VOICE") })
                                    } else {
                                        startRecording(context, mediaRecorder, { isRecording = it }, { currentVoiceUri = it })
                                    }
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

                    Spacer(modifier = Modifier.height(80.dp))
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
                OutlinedTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
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
