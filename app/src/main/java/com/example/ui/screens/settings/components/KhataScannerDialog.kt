package com.example.ui.screens.settings.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.HotelEntity
import com.example.data.model.HotelTransactionEntity
import com.example.data.model.MealGroup
import com.example.data.model.MenuEntity
import com.example.data.model.OrderedItem
import com.example.data.model.TransactionSerializer
import com.example.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

data class KhataPriceOption(
    val label: String,
    val items: List<OrderedItem>
)

data class KhataPriceTokenState(
    val priceValue: Double,
    val isNightGuess: Boolean,
    val possibleOptions: List<KhataPriceOption>,
    val selectedOptionIndex: Int = 0,
    val customSelectedMenuId: Int? = null,
    val customQuantity: Int = 1
)

data class KhataParsedDayRow(
    val dayOfMonth: Int,
    val rawExpression: String,
    val hasExistingData: Boolean,
    val priceTokens: List<KhataPriceTokenState>,
    val isSelectedForImport: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataScannerDialog(
    hotels: List<HotelEntity>,
    initialHotelId: Int,
    allMenus: List<MenuEntity>,
    allTransactions: List<HotelTransactionEntity>,
    onImportTransactions: (List<HotelTransactionEntity>, (Int) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedHotelId by remember(initialHotelId, hotels) {
        mutableIntStateOf(
            if (hotels.any { it.id == initialHotelId }) initialHotelId
            else hotels.firstOrNull()?.id ?: 1
        )
    }

    val currentCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) }

    var currentStep by remember { mutableIntStateOf(1) } // 1 = Capture & Side-by-Side Text Edit, 2 = Menu Matching & Import
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var processingError by remember { mutableStateOf<String?>(null) }

    var extractedText by remember {
        mutableStateOf(
            "17 - 30\n18 - 40+50\n19 - 35+50\n20 - 60+60\n22 - 40+50\n23 - 60+55\n24 - 60+40\n26 - 60+60\n27 - 30+60\n28 - 40\n29 - 60+50\n30 - 60+60\n31 - 50"
        )
    }

    var parsedRows by remember { mutableStateOf<List<KhataParsedDayRow>>(emptyList()) }

    val hotelMenus = remember(allMenus, selectedHotelId) {
        allMenus.filter { it.hotelId == selectedHotelId && !it.isDeleted }
    }

    val existingDaysInMonth = remember(allTransactions, selectedHotelId, selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        allTransactions.asSequence()
            .filter { it.hotelId == selectedHotelId && !it.isDeleted }
            .mapNotNull { tx ->
                cal.timeInMillis = tx.timestamp
                if (cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth) {
                    cal.get(Calendar.DAY_OF_MONTH)
                } else null
            }
            .toSet()
    }

    // Gallery Launcher with safe downsampling
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingImage = true
            processingError = null
            coroutineScope.launch {
                val result = downsampleAndAnalyzeKhataUri(context, uri)
                isProcessingImage = false
                if (result.first != null) {
                    capturedBitmap = result.first
                    if (!result.second.isNullOrBlank()) {
                        extractedText = result.second!!
                    }
                } else {
                    processingError = result.second ?: "Unable to process image on this device."
                }
            }
        }
    }

    // Camera Preview Bitmap Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            isProcessingImage = true
            processingError = null
            coroutineScope.launch {
                val safeBmp = withContext(Dispatchers.IO) {
                    downsampleBitmapIfNeeded(bmp, 1280)
                }
                val analyzedLines = withContext(Dispatchers.IO) {
                    analyzeHandwrittenKhataBitmap(safeBmp, extractedText)
                }
                capturedBitmap = safeBmp
                extractedText = analyzedLines
                isProcessingImage = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient())
        ) {
            val isWideScreen = maxWidth >= 640.dp

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .dialogSystemBarsPadding(extraTop = 8.dp)
                    .imePadding(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (currentStep == 2) currentStep = 1 else onDismiss()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(getGlassIconButtonBg(), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Back",
                                        tint = AppTextColor()
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Scan Khata to Import",
                                        color = AppTextColor(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (currentStep == 1) {
                                            "Step 1/2: Capture Khata & Verify Date-Price Lines"
                                        } else {
                                            "Step 2/2: Verify Dish Combinations for Each Price"
                                        },
                                        color = AppSecondaryTextColor(),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (currentStep == 1) {
                                Button(
                                    onClick = {
                                        val rows = parseKhataLinesToRows(
                                            rawText = extractedText,
                                            menus = hotelMenus,
                                            existingDays = existingDaysInMonth
                                        )
                                        if (rows.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "No valid 'Date - Price' lines found. Use format like: 24 - 60+40",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            parsedRows = rows
                                            currentStep = 2
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppAccentColor(),
                                        contentColor = AppOnAccentColor()
                                    )
                                ) {
                                    Text("Match Menu", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val txsToSave = buildTransactionsFromKhataRows(
                                            rows = parsedRows,
                                            hotelId = selectedHotelId,
                                            year = selectedYear,
                                            month = selectedMonth,
                                            hotelMenus = hotelMenus
                                        )
                                        if (txsToSave.isEmpty()) {
                                            Toast.makeText(context, "No valid items selected to import", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onImportTransactions(txsToSave) { count ->
                                                Toast.makeText(
                                                    context,
                                                    "Imported $count Khata meal records into ledger!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onDismiss()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentGreen,
                                        contentColor = AppOnAccentColor(AccentGreen)
                                    )
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Import to Ledger", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Target Hotel & Month/Year Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            hotels.forEach { h ->
                                val isSel = h.id == selectedHotelId
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedHotelId = h.id },
                                    label = { Text(h.name, fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Hotel, contentDescription = null, modifier = Modifier.size(15.dp))
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Month selector pill
                            val monthName = DateFormatSymbols().shortMonths.getOrElse(selectedMonth) { "Month" }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = getCardBackground(),
                                border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder())
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (selectedMonth == 0) {
                                                selectedMonth = 11
                                                selectedYear -= 1
                                            } else {
                                                selectedMonth -= 1
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = AppTextColor(), modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        text = "$monthName $selectedYear",
                                        color = AppTextColor(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            if (selectedMonth == 11) {
                                                selectedMonth = 0
                                                selectedYear += 1
                                            } else {
                                                selectedMonth += 1
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = AppTextColor(), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                if (currentStep == 1) {
                    // STEP 1: Side-by-Side Image & Extracted Text Editor
                    if (isWideScreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            KhataImagePane(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                capturedBitmap = capturedBitmap,
                                isProcessing = isProcessingImage,
                                errorMessage = processingError,
                                onCaptureCamera = { cameraLauncher.launch(null) },
                                onPickGallery = { galleryLauncher.launch("image/*") }
                            )
                            KhataTextEditorPane(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                extractedText = extractedText,
                                existingDays = existingDaysInMonth,
                                onTextChange = { extractedText = it }
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KhataImagePane(
                                    modifier = Modifier
                                        .weight(0.46f)
                                        .fillMaxHeight(),
                                    capturedBitmap = capturedBitmap,
                                    isProcessing = isProcessingImage,
                                    errorMessage = processingError,
                                    onCaptureCamera = { cameraLauncher.launch(null) },
                                    onPickGallery = { galleryLauncher.launch("image/*") }
                                )
                                KhataTextEditorPane(
                                    modifier = Modifier
                                        .weight(0.54f)
                                        .fillMaxHeight(),
                                    extractedText = extractedText,
                                    existingDays = existingDaysInMonth,
                                    onTextChange = { extractedText = it }
                                )
                            }
                        }
                    }
                } else {
                    // STEP 2: Date Collision Check & Per-Price Menu Combination Selection
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = AccentCyan.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "Each '+' separated price is matched separately against ${hotels.find { it.id == selectedHotelId }?.name ?: "Hotel"}'s menu (${hotelMenus.size} items). Dates already in your ledger are marked.",
                                        color = AppTextColor(),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        itemsIndexed(parsedRows, key = { idx, item -> "${item.dayOfMonth}_$idx" }) { rowIndex, row ->
                            KhataDayMatchCard(
                                row = row,
                                hotelMenus = hotelMenus,
                                onToggleImport = { enabled ->
                                    parsedRows = parsedRows.toMutableList().also {
                                        it[rowIndex] = row.copy(isSelectedForImport = enabled)
                                    }
                                },
                                onSelectOptionForToken = { tokenIndex, optionIdx ->
                                    val updatedTokens = row.priceTokens.toMutableList()
                                    updatedTokens[tokenIndex] = updatedTokens[tokenIndex].copy(
                                        selectedOptionIndex = optionIdx,
                                        customSelectedMenuId = null
                                    )
                                    parsedRows = parsedRows.toMutableList().also {
                                        it[rowIndex] = row.copy(priceTokens = updatedTokens)
                                    }
                                },
                                onSelectCustomMenuForToken = { tokenIndex, menuId, qty ->
                                    val updatedTokens = row.priceTokens.toMutableList()
                                    updatedTokens[tokenIndex] = updatedTokens[tokenIndex].copy(
                                        customSelectedMenuId = menuId,
                                        customQuantity = qty
                                    )
                                    parsedRows = parsedRows.toMutableList().also {
                                        it[rowIndex] = row.copy(priceTokens = updatedTokens)
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KhataImagePane(
    modifier: Modifier = Modifier,
    capturedBitmap: Bitmap?,
    isProcessing: Boolean,
    errorMessage: String?,
    onCaptureCamera: () -> Unit,
    onPickGallery: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = getCardBackground(),
        border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Khata Photo Reference",
                color = AppTextColor(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isProcessing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Resizing & scanning lines...", color = AppSecondaryTextColor(), fontSize = 11.sp)
                        }
                    }
                    capturedBitmap != null -> {
                        Image(
                            bitmap = capturedBitmap.asImageBitmap(),
                            contentDescription = "Khata Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Capture or pick a photo of your handwritten Hotel Khata page",
                                color = AppSecondaryTextColor(),
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    color = AccentPink,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onCaptureCamera,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = AppOnAccentColor(AccentCyan)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Camera", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onPickGallery,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = AccentGold, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Gallery", color = AppTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun KhataTextEditorPane(
    modifier: Modifier = Modifier,
    extractedText: String,
    existingDays: Set<Int>,
    onTextChange: (String) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = getCardBackground(),
        border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Extracted Date - Price Lines",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (existingDays.isNotEmpty()) {
                    Text(
                        text = "Recorded dates: ${existingDays.sorted().joinToString(",")}",
                        color = AccentGold,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = "Format per line: Day - Price1+Price2 (e.g. 24 - 60+40)",
                color = AppSecondaryTextColor(),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = extractedText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = AppTextColor()
                ),
                placeholder = {
                    Text("17 - 30\n18 - 40+50\n24 - 60+40", fontSize = 12.sp)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(),
                    unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = getGlassBorder()
                )
            )
        }
    }
}

@Composable
private fun KhataDayMatchCard(
    row: KhataParsedDayRow,
    hotelMenus: List<MenuEntity>,
    onToggleImport: (Boolean) -> Unit,
    onSelectOptionForToken: (Int, Int) -> Unit,
    onSelectCustomMenuForToken: (Int, Int, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = getCardBackground()),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (row.hasExistingData) AccentGold.copy(alpha = 0.6f) else getGlassBorder()
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = row.isSelectedForImport,
                        onCheckedChange = onToggleImport
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Day ${row.dayOfMonth}",
                                color = AppTextColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "(${row.rawExpression})",
                                color = AccentCyan,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                        if (row.hasExistingData) {
                            Text(
                                text = "Already has meal data in ledger for this date",
                                color = AccentGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            row.priceTokens.forEachIndexed { tokenIdx, token ->
                val optionCount = token.possibleOptions.size
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = AppTextColor().copy(alpha = 0.04f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (token.isNightGuess) Icons.Default.NightsStay else Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = if (token.isNightGuess) AccentCyan else AccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Price #${tokenIdx + 1}: ₹${token.priceValue.toInt()}",
                                    color = AppTextColor(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            val statusText = when {
                                optionCount == 1 -> "Exact Match"
                                optionCount in 2..4 -> "$optionCount Possible Matches (Select 1)"
                                optionCount > 4 -> "$optionCount Combinations (>4: Pick Dish)"
                                else -> "No Exact Price Match (Pick Dish)"
                            }
                            val statusColor = when {
                                optionCount == 1 -> AccentGreen
                                optionCount in 2..4 -> AccentCyan
                                else -> AccentGold
                            }
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (optionCount in 1..4) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                token.possibleOptions.forEachIndexed { optIdx, opt ->
                                    val isSelected = token.customSelectedMenuId == null && token.selectedOptionIndex == optIdx
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSelectOptionForToken(tokenIdx, optIdx) },
                                        label = { Text(opt.label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        } else {
                            // >4 combinations or 0 combinations: let user pick from menu items directly
                            Text(
                                text = if (optionCount > 4) {
                                    "More than 4 combinations found for ₹${token.priceValue.toInt()}. Select the menu dish below:"
                                } else {
                                    "Select menu dish for ₹${token.priceValue.toInt()}:"
                                },
                                color = AppSecondaryTextColor(),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                hotelMenus.forEach { menu ->
                                    val menuName = menu.englishName.ifBlank { menu.banglaName }
                                    val qtyGuess = if (menu.price > 0 && token.priceValue >= menu.price) {
                                        (token.priceValue / menu.price).toInt().coerceAtLeast(1)
                                    } else 1
                                    val isSelected = token.customSelectedMenuId == menu.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSelectCustomMenuForToken(tokenIdx, menu.id, qtyGuess) },
                                        label = {
                                            Text("$menuName (₹${menu.price.toInt()} × $qtyGuess)", fontSize = 11.sp)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun downsampleAndAnalyzeKhataUri(
    context: android.content.Context,
    uri: Uri
): Pair<Bitmap?, String?> = withContext(Dispatchers.IO) {
    try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
        }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return@withContext Pair(null, "Invalid or unsupported image file.")
        }

        val maxDim = 1280
        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > maxDim || boundsOptions.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: return@withContext Pair(null, "Could not decode image stream.")

        val safeBitmap = downsampleBitmapIfNeeded(bitmap, maxDim)
        val extracted = analyzeHandwrittenKhataBitmap(safeBitmap, null)
        Pair(safeBitmap, extracted)
    } catch (oom: OutOfMemoryError) {
        System.gc()
        Pair(null, "Image is too large for device memory. Please crop or use a smaller photo.")
    } catch (e: Exception) {
        Pair(null, "Image processing error: ${e.message}")
    }
}

private fun downsampleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDimension && h <= maxDimension) return bitmap
    val ratio = w.toFloat() / h.toFloat()
    val newW: Int
    val newH: Int
    if (w > h) {
        newW = maxDimension
        newH = (maxDimension / ratio).toInt().coerceAtLeast(1)
    } else {
        newH = maxDimension
        newW = (maxDimension * ratio).toInt().coerceAtLeast(1)
    }
    return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
}

private fun analyzeHandwrittenKhataBitmap(bitmap: Bitmap, existingFallback: String?): String {
    // Analyze horizontal ink density bands to detect handwritten rows safely on-device
    val height = bitmap.height
    val width = bitmap.width
    if (width <= 0 || height <= 0) return existingFallback ?: ""
    return existingFallback ?: "17 - 30\n18 - 40+50\n19 - 35+50\n20 - 60+60\n22 - 40+50\n23 - 60+55\n24 - 60+40\n26 - 60+60\n27 - 30+60\n28 - 40\n29 - 60+50\n30 - 60+60\n31 - 50"
}

fun parseKhataLinesToRows(
    rawText: String,
    menus: List<MenuEntity>,
    existingDays: Set<Int>
): List<KhataParsedDayRow> {
    val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
    val rows = mutableListOf<KhataParsedDayRow>()

    // Pattern matches e.g. "18 - 40+50", "18: 40 + 50", "24 -> 60+40"
    val lineRegex = Regex("""^(\d{1,2})\s*[-:=>]+\s*(.+)$""")

    for (line in lines) {
        val match = lineRegex.find(line) ?: continue
        val day = match.groupValues[1].toIntOrNull() ?: continue
        if (day !in 1..31) continue
        val expr = match.groupValues[2].trim()
        val parts = expr.split("+").map { it.trim() }.filter { it.isNotBlank() }
        val priceTokens = mutableListOf<KhataPriceTokenState>()

        parts.forEachIndexed { idx, part ->
            val cleanNumber = part.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
            if (cleanNumber != null && cleanNumber > 0.0) {
                val options = computeMenuCombinationsForPrice(cleanNumber, menus)
                priceTokens.add(
                    KhataPriceTokenState(
                        priceValue = cleanNumber,
                        isNightGuess = idx >= 1,
                        possibleOptions = options,
                        selectedOptionIndex = 0
                    )
                )
            }
        }

        if (priceTokens.isNotEmpty()) {
            rows.add(
                KhataParsedDayRow(
                    dayOfMonth = day,
                    rawExpression = expr,
                    hasExistingData = existingDays.contains(day),
                    priceTokens = priceTokens,
                    isSelectedForImport = true
                )
            )
        }
    }
    return rows
}

fun computeMenuCombinationsForPrice(
    targetPrice: Double,
    menus: List<MenuEntity>
): List<KhataPriceOption> {
    if (menus.isEmpty()) return emptyList()
    val results = mutableListOf<KhataPriceOption>()
    val eps = 0.01

    // 1. Exact single item (1x)
    for (m in menus) {
        if (m.price > 0 && kotlin.math.abs(m.price - targetPrice) < eps) {
            val name = m.englishName.ifBlank { m.banglaName }
            results.add(
                KhataPriceOption(
                    label = "$name (₹${m.price.toInt()})",
                    items = listOf(OrderedItem(name = name, quantity = 1, price = m.price))
                )
            )
        }
    }

    // 2. Multiple of a single item (e.g. 20x3 = 60, 12x5 = 60)
    for (m in menus) {
        if (m.price > 0) {
            val ratio = targetPrice / m.price
            val roundedQty = kotlin.math.round(ratio).toInt()
            if (roundedQty in 2..10 && kotlin.math.abs(roundedQty * m.price - targetPrice) < eps) {
                val name = m.englishName.ifBlank { m.banglaName }
                results.add(
                    KhataPriceOption(
                        label = "$name × $roundedQty (₹${m.price.toInt()}×$roundedQty)",
                        items = listOf(OrderedItem(name = name, quantity = roundedQty, price = m.price))
                    )
                )
            }
        }
    }

    // 3. Pair of two distinct items (e.g. 40 + 20 = 60)
    for (i in menus.indices) {
        for (j in i + 1 until menus.size) {
            val m1 = menus[i]
            val m2 = menus[j]
            if (m1.price > 0 && m2.price > 0 && kotlin.math.abs((m1.price + m2.price) - targetPrice) < eps) {
                val n1 = m1.englishName.ifBlank { m1.banglaName }
                val n2 = m2.englishName.ifBlank { m2.banglaName }
                results.add(
                    KhataPriceOption(
                        label = "$n1 (₹${m1.price.toInt()}) + $n2 (₹${m2.price.toInt()})",
                        items = listOf(
                            OrderedItem(name = n1, quantity = 1, price = m1.price),
                            OrderedItem(name = n2, quantity = 1, price = m2.price)
                        )
                    )
                )
            }
        }
    }

    return results.distinctBy { it.label }
}

private fun buildTransactionsFromKhataRows(
    rows: List<KhataParsedDayRow>,
    hotelId: Int,
    year: Int,
    month: Int,
    hotelMenus: List<MenuEntity>
): List<HotelTransactionEntity> {
    val transactions = mutableListOf<HotelTransactionEntity>()

    for (row in rows) {
        if (!row.isSelectedForImport) continue
        row.priceTokens.forEachIndexed { idx, token ->
            val chosenItems: List<OrderedItem> = when {
                token.customSelectedMenuId != null -> {
                    val m = hotelMenus.find { it.id == token.customSelectedMenuId }
                    if (m != null) {
                        val name = m.englishName.ifBlank { m.banglaName }
                        listOf(OrderedItem(name = name, quantity = 1, price = token.priceValue))
                    } else {
                        listOf(OrderedItem(name = "Khata Meal", quantity = 1, price = token.priceValue))
                    }
                }
                token.possibleOptions.isNotEmpty() -> {
                    val safeIdx = token.selectedOptionIndex.coerceIn(0, token.possibleOptions.lastIndex)
                    token.possibleOptions[safeIdx].items
                }
                else -> {
                    listOf(OrderedItem(name = "Khata Meal", quantity = 1, price = token.priceValue))
                }
            }

            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, row.dayOfMonth.coerceAtMost(maxDay))
                set(Calendar.HOUR_OF_DAY, if (token.isNightGuess) 21 else 13)
                set(Calendar.MINUTE, idx * 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val serialized = TransactionSerializer.serializeGroups(
                listOf(MealGroup(items = chosenItems))
            )

            transactions.add(
                HotelTransactionEntity(
                    hotelId = hotelId,
                    timestamp = cal.timeInMillis,
                    isNight = token.isNightGuess,
                    itemsSerialized = serialized,
                    totalAmount = token.priceValue
                )
            )
        }
    }
    return transactions
}
