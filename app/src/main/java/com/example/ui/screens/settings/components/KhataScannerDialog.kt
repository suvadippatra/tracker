package com.example.ui.screens.settings.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class MenuCombinationGuess(
    val description: String,
    val itemsWithQty: List<Pair<MenuEntity, Int>>,
    val totalPrice: Double
)

data class KhataPriceSegment(
    val price: Double,
    val guesses: List<MenuCombinationGuess>,
    val selectedGuessIndex: Int = 0,
    val customDishName: String = ""
)

data class KhataDayEntry(
    val dayOfMonth: Int,
    val month: Int,
    val year: Int,
    val hasExplicitMonthYear: Boolean,
    val timestamp: Long,
    val hasExistingData: Boolean,
    val existingSummary: String,
    val existingTotalAmount: Double,
    val segments: List<KhataPriceSegment>,
    val includeInImport: Boolean = true,
    val useNewWhenExisting: Boolean = false
)

@Composable
fun KhataScannerDialog(
    hotels: List<HotelEntity>,
    initialHotelId: Int,
    allMenus: List<MenuEntity>,
    allTransactions: List<HotelTransactionEntity>,
    onImportTransactions: (List<HotelTransactionEntity>, (Int) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val isLight = isAppLightMode()
    val isBn = false
    val accent = AppAccentColor()

    var selectedHotelId by remember(hotels, initialHotelId) {
        mutableIntStateOf(
            hotels.find { it.id == initialHotelId }?.id
                ?: hotels.firstOrNull()?.id
                ?: 1
        )
    }

    val currentCal = remember { Calendar.getInstance() }
    var targetYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var targetMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showFullBitmapModal by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scanSummaryStatus by remember { mutableStateOf<String?>(null) }

    // Raw text starts empty
    var extractedText by remember { mutableStateOf("") }

    var parsedEntries by remember { mutableStateOf<List<KhataDayEntry>>(emptyList()) }
    var step by remember { mutableIntStateOf(1) } // 1 = Raw Capture & Text Scan, 2 = Date Collision & Menu Guess Selection

    val hotelMenus = remember(allMenus, selectedHotelId) {
        allMenus.filter { it.hotelId == selectedHotelId && !it.isDeleted }
    }

    fun buildEntriesFromRawText(
        hotelId: Int = selectedHotelId,
        month: Int = targetMonth,
        year: Int = targetYear,
        menus: List<MenuEntity> = hotelMenus
    ): List<KhataDayEntry> {
        return parseKhataRawText(
            rawText = extractedText,
            fallbackYear = year,
            fallbackMonth = month,
            hotelId = hotelId,
            hotelMenus = menus,
            existingTransactions = allTransactions
        )
    }

    BackHandler {
        if (showFullBitmapModal) {
            showFullBitmapModal = false
        } else if (step == 2) {
            step = 1
        } else {
            onDismiss()
        }
    }

    // Full-screen uncropped bitmap viewer when user taps the Khata image
    if (showFullBitmapModal && capturedBitmap != null) {
        Dialog(
            onDismissRequest = { showFullBitmapModal = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            DialogEdgeToEdge()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { showFullBitmapModal = false }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Full Uncropped Khata Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Tap anywhere to close",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Camera capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            scope.launch {
                isProcessingImage = true
                errorMessage = null
                scanSummaryStatus = null
                try {
                    val safeBmp = withContext(Dispatchers.Default) {
                        resizeBitmapSafely(bmp, 1440)
                    }
                    capturedBitmap = safeBmp
                    val scannedLines = withContext(Dispatchers.Default) {
                        runHybridKhataOcr(safeBmp)
                    }
                    if (scannedLines.isNotEmpty()) {
                        extractedText = scannedLines.joinToString("\n")
                        scanSummaryStatus = "ML Kit + Handwriting OCR detected ${scannedLines.size} Khata line(s). Review or edit below."
                    } else {
                        errorMessage = "Could not automatically isolate clear Date - Price rows from this image. You can inspect the full uncropped image above and type/adjust the lines below."
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to process camera image: ${e.localizedMessage}"
                } finally {
                    isProcessingImage = false
                }
            }
        }
    }

    // Photo picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessingImage = true
                errorMessage = null
                scanSummaryStatus = null
                try {
                    val decoded = withContext(Dispatchers.IO) {
                        decodeSampledBitmapFromUri(context.contentResolver, uri, 1440)
                    }
                    if (decoded != null) {
                        capturedBitmap = decoded
                        val scannedLines = withContext(Dispatchers.Default) {
                            runHybridKhataOcr(decoded)
                        }
                        if (scannedLines.isNotEmpty()) {
                            extractedText = scannedLines.joinToString("\n")
                            scanSummaryStatus = "ML Kit + Handwriting OCR detected ${scannedLines.size} Khata line(s). Review or edit below."
                        } else {
                            errorMessage = "Could not automatically isolate clear Date - Price rows from this photo. You can view the full uncropped photo above and type/edit lines below."
                        }
                    } else {
                        errorMessage = "Unable to decode selected image safely."
                    }
                } catch (e: Exception) {
                    errorMessage = "Image loading failed: ${e.localizedMessage}"
                } finally {
                    isProcessingImage = false
                }
            }
        }
    }

    val monthNames = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isLight) Color(0xFFF8FAFC) else Color(0xFF0B0F19)
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
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = {
                                    if (step == 2) step = 1 else onDismiss()
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(getGlassIconButtonBg())
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppTextColor()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (step == 1) {
                                        "Scan Khata (Step 1: Raw Data)"
                                    } else {
                                        "Scan Khata (Step 2: Date & Menu Match)"
                                    },
                                    color = AppTextColor(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (step == 1) {
                                        "Offline English & Handwriting OCR • Full uncropped image preview"
                                    } else {
                                        "Compare Existing vs New dates & pick up to 4 menu combinations"
                                    },
                                    color = AppSecondaryTextColor(),
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (step == 2) {
                            TextButton(onClick = { step = 1 }) {
                                Text("Edit Raw Lines", color = accent, fontWeight = FontWeight.Bold)
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
                            .widthIn(max = 840.dp)
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        if (errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        color = Color(0xFFEF4444),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        if (scanSummaryStatus != null && step == 1) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = scanSummaryStatus ?: "",
                                        color = Color(0xFF10B981),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (step == 1) {
                            // STEP 1: Stable Scrollable Layout (prevents any tablet recomposition/height flickering!)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "1. Capture or Import Khata Photo (Offline English OCR)",
                                            color = accent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Capture or pick your handwritten English Khata page. The full image is shown below without 16:9 cropping so you can clearly read every row while editing.",
                                            color = AppSecondaryTextColor(),
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Button(
                                                onClick = { cameraLauncher.launch(null) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = accent,
                                                    contentColor = AppOnAccentColor(accent)
                                                )
                                            ) {
                                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Camera Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    galleryLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF10B981),
                                                    contentColor = AppOnAccentColor(Color(0xFF10B981))
                                                )
                                            ) {
                                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Pick Image", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                val clipText = clipboardManager.getText()?.text.orEmpty()
                                                if (clipText.isNotBlank()) {
                                                    val repairedLines = clipText.lines()
                                                        .map { normalizeMessyKhataOcrLine(it) }
                                                        .filter { it.isNotBlank() }
                                                    extractedText = repairedLines.joinToString("\n")
                                                    errorMessage = null
                                                    scanSummaryStatus = "Pasted & auto-repaired ${repairedLines.size} line(s) from Google Lens / Clipboard."
                                                } else {
                                                    Toast.makeText(context, "Clipboard is empty. Copy text from Google Lens first.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(42.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Paste / Google Lens Text (Auto-Repair Handwritten OCR)",
                                                color = AppTextColor(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        if (isProcessingImage) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    "Running offline English & handwriting OCR...",
                                                    color = AppSecondaryTextColor(),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        // Full Uncropped Image Preview (ContentScale.Fit, expandable to full screen)
                                        capturedBitmap?.let { bmp ->
                                            Spacer(modifier = Modifier.height(12.dp))
                                            val aspect = remember(bmp) {
                                                (bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)).coerceIn(0.45f, 2.2f)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 240.dp, max = 460.dp)
                                                    .aspectRatio(aspect, matchHeightConstraintsFirst = false)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isLight) Color(0xFFF1F5F9) else Color(0xFF0F172A))
                                                    .border(1.dp, AppTextColor().copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                                                    .clickable { showFullBitmapModal = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Uncropped Khata Image",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                // Top-left Full Screen hint
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.62f),
                                                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                                                    modifier = Modifier.align(Alignment.TopStart)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Full Image (Tap to Zoom)",
                                                            color = Color.White,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                // Top-right Re-scan button
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.65f),
                                                    shape = RoundedCornerShape(bottomStart = 12.dp),
                                                    modifier = Modifier.align(Alignment.TopEnd)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .clickable {
                                                                scope.launch {
                                                                    isProcessingImage = true
                                                                    val rescanned = withContext(Dispatchers.Default) {
                                                                        runHybridKhataOcr(bmp)
                                                                    }
                                                                    if (rescanned.isNotEmpty()) {
                                                                        extractedText = rescanned.joinToString("\n")
                                                                        scanSummaryStatus = "Re-scanned ${rescanned.size} line(s)."
                                                                    }
                                                                    isProcessingImage = false
                                                                }
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Re-scan Image",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Raw Data Editor Card
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "2. Extracted Khata Lines (Flexible Date & Price Format)",
                                                color = accent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (extractedText.isNotBlank()) {
                                                Text(
                                                    text = "Clear",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable { extractedText = "" }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Supports full dates or day numbers with or without spaces, e.g.:\n9.10.26 - 60+50  •  11.10.26 - 40  •  13.10.26-20  •  31/10/26 40",
                                            color = AppSecondaryTextColor(),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = extractedText,
                                            onValueChange = { extractedText = it },
                                            placeholder = {
                                                Text(
                                                    "9.10.26 - 60+50\n11.10.26 - 40\n13.10.26 - 20\n31.10.26 - 40",
                                                    color = AppSecondaryTextColor().copy(alpha = 0.6f),
                                                    fontSize = 13.sp
                                                )
                                            },
                                            minLines = 6,
                                            maxLines = 12,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = accent,
                                                unfocusedBorderColor = AppTextColor().copy(alpha = 0.18f),
                                                focusedTextColor = AppTextColor(),
                                                unfocusedTextColor = AppTextColor()
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (hotels.isEmpty()) {
                                        errorMessage = "Please create a Hotel first before matching Khata records."
                                        return@Button
                                    }
                                    if (extractedText.isBlank()) {
                                        errorMessage = "Please scan a Khata image or enter at least one Date - Price line."
                                        return@Button
                                    }
                                    val parsed = buildEntriesFromRawText()
                                    if (parsed.isEmpty()) {
                                        errorMessage = "Could not find valid Date & Price numbers. Example: '9.10.26 - 60+50' or '11.10.26 - 40'."
                                    } else {
                                        errorMessage = null
                                        // Sync targetMonth and targetYear to the first explicit date if present
                                        parsed.firstOrNull { it.hasExplicitMonthYear }?.let { firstExplicit ->
                                            targetMonth = firstExplicit.month
                                            targetYear = firstExplicit.year
                                        }
                                        parsedEntries = parsed
                                        step = 2
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = AppOnAccentColor(accent)
                                )
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Next: Check Captured Dates & Match Menu",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }
                        } else {
                            // STEP 2: Target Hotel, Month Selection, Existing vs New Date Resolution, and Menu Combination Matching
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Target Hotel & Default Month (for lines without explicit month)",
                                        color = accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        hotels.forEach { hotel ->
                                            val selected = hotel.id == selectedHotelId
                                            FilterChip(
                                                selected = selected,
                                                onClick = {
                                                    selectedHotelId = hotel.id
                                                    val newMenus = allMenus.filter { it.hotelId == hotel.id && !it.isDeleted }
                                                    parsedEntries = buildEntriesFromRawText(
                                                        hotelId = hotel.id,
                                                        month = targetMonth,
                                                        year = targetYear,
                                                        menus = newMenus
                                                    )
                                                },
                                                label = { Text(hotel.name, fontWeight = FontWeight.Bold) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = accent,
                                                    selectedLabelColor = AppOnAccentColor(accent)
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        monthNames.forEachIndexed { index, mName ->
                                            val isSel = targetMonth == index
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) accent else getGlassIconButtonBg(),
                                                modifier = Modifier.clickable {
                                                    targetMonth = index
                                                    parsedEntries = buildEntriesFromRawText(
                                                        hotelId = selectedHotelId,
                                                        month = index,
                                                        year = targetYear,
                                                        menus = hotelMenus
                                                    )
                                                }
                                            ) {
                                                Text(
                                                    text = mName,
                                                    color = if (isSel) AppOnAccentColor(accent) else AppTextColor(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val capturedCount = parsedEntries.count { it.hasExistingData }
                            val newCount = parsedEntries.size - capturedCount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "New Dates: $newCount",
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Existing in DB: $capturedCount",
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = parsedEntries,
                                    key = { idx, item -> "${item.year}_${item.month}_${item.dayOfMonth}_$idx" }
                                ) { dayIdx, entry ->
                                    val dateStr = remember(entry.timestamp) {
                                        dateFormat.format(Date(entry.timestamp))
                                    }
                                    val scannedTotal = entry.segments.sumOf { it.price }

                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Checkbox(
                                                        checked = entry.includeInImport,
                                                        onCheckedChange = { checked ->
                                                            parsedEntries = parsedEntries.toMutableList().also {
                                                                it[dayIdx] = entry.copy(
                                                                    includeInImport = checked,
                                                                    useNewWhenExisting = if (entry.hasExistingData) checked else entry.useNewWhenExisting
                                                                )
                                                            }
                                                        }
                                                    )
                                                    Column {
                                                        Text(
                                                            text = dateStr,
                                                            color = AppTextColor(),
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 15.sp
                                                        )
                                                        if (entry.hasExistingData) {
                                                            Text(
                                                                text = "⚠️ Date already exists in database — choose Existing or New below",
                                                                color = Color(0xFFF59E0B),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        } else {
                                                            Text(
                                                                text = "✓ New Date — select dishes below",
                                                                color = Color(0xFF10B981),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = "₹${scannedTotal.toInt()}",
                                                    color = accent,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp
                                                )
                                            }

                                            // If Date Exists in Database: Explicitly show Existing vs New choice!
                                            if (entry.hasExistingData) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val keepExistingSelected = !entry.useNewWhenExisting
                                                    // Option A: Keep Existing
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (keepExistingSelected) Color(0xFFF59E0B).copy(alpha = 0.16f) else getGlassIconButtonBg().copy(alpha = 0.5f),
                                                        border = BorderStroke(
                                                            if (keepExistingSelected) 1.5.dp else 1.dp,
                                                            if (keepExistingSelected) Color(0xFFF59E0B) else AppTextColor().copy(alpha = 0.12f)
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                parsedEntries = parsedEntries.toMutableList().also {
                                                                    it[dayIdx] = entry.copy(
                                                                        useNewWhenExisting = false,
                                                                        includeInImport = false
                                                                    )
                                                                }
                                                            }
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = if (keepExistingSelected) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                                                                    contentDescription = null,
                                                                    tint = if (keepExistingSelected) Color(0xFFF59E0B) else AppSecondaryTextColor(),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = "Keep Existing",
                                                                    color = AppTextColor(),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 12.sp
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = entry.existingSummary.ifBlank { "Recorded: ₹${entry.existingTotalAmount.toInt()}" },
                                                                color = AppSecondaryTextColor(),
                                                                fontSize = 11.sp,
                                                                lineHeight = 15.sp
                                                            )
                                                        }
                                                    }

                                                    // Option B: Use New Scanned Data
                                                    val useNewSelected = entry.useNewWhenExisting
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (useNewSelected) accent.copy(alpha = 0.16f) else getGlassIconButtonBg().copy(alpha = 0.5f),
                                                        border = BorderStroke(
                                                            if (useNewSelected) 1.5.dp else 1.dp,
                                                            if (useNewSelected) accent else AppTextColor().copy(alpha = 0.12f)
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                parsedEntries = parsedEntries.toMutableList().also {
                                                                    it[dayIdx] = entry.copy(
                                                                        useNewWhenExisting = true,
                                                                        includeInImport = true
                                                                    )
                                                                }
                                                            }
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = if (useNewSelected) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                                                                    contentDescription = null,
                                                                    tint = if (useNewSelected) accent else AppSecondaryTextColor(),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = "Use New (₹${scannedTotal.toInt()})",
                                                                    color = AppTextColor(),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 12.sp
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = "Scanned: ${entry.segments.joinToString(" + ") { "₹${it.price.toInt()}" }}",
                                                                color = AppSecondaryTextColor(),
                                                                fontSize = 11.sp,
                                                                lineHeight = 15.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Show dish / 4-option combination selectors when New Date OR when user chose "Use New"
                                            if (!entry.hasExistingData || entry.useNewWhenExisting) {
                                                Spacer(modifier = Modifier.height(8.dp))

                                                entry.segments.forEachIndexed { segIdx, seg ->
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 5.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(getGlassIconButtonBg().copy(alpha = 0.65f))
                                                            .padding(10.dp)
                                                    ) {
                                                        Text(
                                                            text = "Dish Price: ₹${seg.price.toInt()}",
                                                            color = AppTextColor(),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))

                                                        val topFourGuesses = remember(seg.guesses) { seg.guesses.take(4) }

                                                        if (topFourGuesses.isNotEmpty()) {
                                                            Text(
                                                                text = if (topFourGuesses.size == 1) {
                                                                    "Matched Menu Dish:"
                                                                } else {
                                                                    "Choose 1 of ${topFourGuesses.size} possible menu combinations for ₹${seg.price.toInt()}:"
                                                                },
                                                                color = AppSecondaryTextColor(),
                                                                fontSize = 11.sp
                                                            )
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                topFourGuesses.forEachIndexed { gIdx, guess ->
                                                                    val isSelected = seg.customDishName.isBlank() && seg.selectedGuessIndex == gIdx
                                                                    Row(
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(if (isSelected) accent.copy(alpha = 0.16f) else Color.Transparent)
                                                                            .border(
                                                                                1.dp,
                                                                                if (isSelected) accent else AppTextColor().copy(alpha = 0.15f),
                                                                                RoundedCornerShape(8.dp)
                                                                            )
                                                                            .clickable {
                                                                                parsedEntries = parsedEntries.toMutableList().also { list ->
                                                                                    val updatedSegs = entry.segments.toMutableList()
                                                                                    updatedSegs[segIdx] = seg.copy(selectedGuessIndex = gIdx, customDishName = "")
                                                                                    list[dayIdx] = entry.copy(segments = updatedSegs)
                                                                                }
                                                                            }
                                                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Add,
                                                                            contentDescription = null,
                                                                            tint = if (isSelected) accent else AppSecondaryTextColor(),
                                                                            modifier = Modifier.size(18.dp)
                                                                        )
                                                                        Spacer(modifier = Modifier.width(8.dp))
                                                                        Text(
                                                                            text = guess.description,
                                                                            color = AppTextColor(),
                                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                            fontSize = 12.sp
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                        } else {
                                                            Text(
                                                                text = "No exact menu item matches ₹${seg.price.toInt()}. Enter custom dish name below:",
                                                                color = AppSecondaryTextColor(),
                                                                fontSize = 11.sp
                                                            )
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                        }

                                                        GlassTextField(
                                                            value = seg.customDishName,
                                                            onValueChange = { newName ->
                                                                parsedEntries = parsedEntries.toMutableList().also { list ->
                                                                    val updatedSegs = entry.segments.toMutableList()
                                                                    updatedSegs[segIdx] = seg.copy(customDishName = newName)
                                                                    list[dayIdx] = entry.copy(segments = updatedSegs)
                                                                }
                                                            },
                                                            placeholder = {
                                                                Text(
                                                                    if (topFourGuesses.isNotEmpty()) "Or type custom dish name..."
                                                                    else "Custom dish name (e.g. Special Meal)"
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            val selectedToImportCount = parsedEntries.count {
                                it.includeInImport && (!it.hasExistingData || it.useNewWhenExisting)
                            }
                            Button(
                                onClick = {
                                    val toImport = parsedEntries.filter {
                                        it.includeInImport && (!it.hasExistingData || it.useNewWhenExisting)
                                    }
                                    val txsToInsert = mutableListOf<HotelTransactionEntity>()
                                    toImport.forEach { dayEntry ->
                                        val groups = mutableListOf<MealGroup>()
                                        var totalAmount = 0.0
                                        dayEntry.segments.forEach { seg ->
                                            totalAmount += seg.price
                                            val chosenGuess = if (seg.customDishName.isBlank()) {
                                                seg.guesses.getOrNull(seg.selectedGuessIndex)
                                            } else null

                                            val orderedItems = if (chosenGuess != null && chosenGuess.itemsWithQty.isNotEmpty()) {
                                                chosenGuess.itemsWithQty.map { (m, qty) ->
                                                    OrderedItem(
                                                        name = m.englishName.ifBlank { m.banglaName },
                                                        quantity = qty,
                                                        price = m.price
                                                    )
                                                }
                                            } else {
                                                val dishLabel = seg.customDishName.trim().ifBlank { "Khata Meal" }
                                                listOf(
                                                    OrderedItem(
                                                        name = dishLabel,
                                                        quantity = 1,
                                                        price = seg.price
                                                    )
                                                )
                                            }
                                            groups.add(MealGroup(items = orderedItems))
                                        }
                                        if (totalAmount > 0 && groups.isNotEmpty()) {
                                            txsToInsert.add(
                                                HotelTransactionEntity(
                                                    hotelId = selectedHotelId,
                                                    timestamp = dayEntry.timestamp,
                                                    isNight = false,
                                                    itemsSerialized = TransactionSerializer.serializeGroups(groups),
                                                    totalAmount = totalAmount
                                                )
                                            )
                                        }
                                    }
                                    if (txsToInsert.isNotEmpty()) {
                                        onImportTransactions(txsToInsert) { count ->
                                            Toast.makeText(
                                                context,
                                                "Imported $count Khata day record(s)!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onDismiss()
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "No records selected for import.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = AppOnAccentColor(Color(0xFF10B981))
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Import Selected Khata Records ($selectedToImportCount Days)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resizeBitmapSafely(source: Bitmap, maxDim: Int): Bitmap {
    val w = source.width
    val h = source.height
    if (w <= maxDim && h <= maxDim) return source
    val ratio = w.toFloat() / h.toFloat()
    val newW = if (w > h) maxDim else (maxDim * ratio).toInt().coerceAtLeast(1)
    val newH = if (h >= w) maxDim else (maxDim / ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, newW, newH, true)
}

private fun decodeSampledBitmapFromUri(
    resolver: android.content.ContentResolver,
    uri: Uri,
    reqMaxDim: Int
): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    var inSampleSize = 1
    val (height, width) = options.outHeight to options.outWidth
    if (height > reqMaxDim || width > reqMaxDim) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqMaxDim && (halfWidth / inSampleSize) >= reqMaxDim) {
            inSampleSize *= 2
        }
    }
    val decodeOpts = BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
        this.inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) } ?: return null
    return resizeBitmapSafely(raw, reqMaxDim)
}

/**
 * Offline English + Handwriting Optical Row & Digit/Operator Recognizer:
 * - Adaptive integral-image local binarization with gentle notebook rule suppression (preserves vertical digit strokes).
 * - Segments handwritten or printed rows and extracts connected glyph boxes across full dates (`9.10.26 - 60+50`)
 *   as well as day-only rows (`17 - 60+40`).
 */
private fun analyzeHandwrittenKhataBitmap(bitmap: Bitmap): List<String> {
    val procBmp = resizeBitmapSafely(bitmap, 900)
    val w = procBmp.width
    val h = procBmp.height
    if (w < 60 || h < 60) return emptyList()

    val pixels = IntArray(w * h)
    procBmp.getPixels(pixels, 0, w, 0, 0, w, h)

    val lum = IntArray(w * h)
    for (i in pixels.indices) {
        val c = pixels[i]
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        lum[i] = (r * 299 + g * 587 + b * 114) / 1000
    }

    val integral = LongArray((w + 1) * (h + 1))
    for (y in 1..h) {
        var rowSum = 0L
        val srcOffset = (y - 1) * w
        val intOffset = y * (w + 1)
        val prevIntOffset = (y - 1) * (w + 1)
        for (x in 1..w) {
            rowSum += lum[srcOffset + x - 1]
            integral[intOffset + x] = integral[prevIntOffset + x] + rowSum
        }
    }

    val winR = max(14, min(w, h) / 22)
    val ink = BooleanArray(w * h)
    for (y in 0 until h) {
        val y0 = max(0, y - winR)
        val y1 = min(h - 1, y + winR)
        for (x in 0 until w) {
            val x0 = max(0, x - winR)
            val x1 = min(w - 1, x + winR)
            val area = (x1 - x0 + 1) * (y1 - y0 + 1)
            val sum = integral[(y1 + 1) * (w + 1) + (x1 + 1)] -
                integral[y0 * (w + 1) + (x1 + 1)] -
                integral[(y1 + 1) * (w + 1) + x0] +
                integral[y0 * (w + 1) + x0]
            val mean = (sum / area).toInt()
            ink[y * w + x] = lum[y * w + x] < (mean - 18)
        }
    }

    // Suppress thin horizontal notebook lines ONLY where vertical stroke thickness is tiny, preserving digit strokes
    val marginX = (w * 0.03f).toInt()
    for (y in 2 until (h - 2)) {
        var rowCount = 0
        for (x in marginX until (w - marginX)) {
            if (ink[y * w + x]) rowCount++
        }
        if (rowCount > (w - 2 * marginX) * 0.55f) {
            for (x in 0 until w) {
                val hasAbove = ink[(y - 2) * w + x]
                val hasBelow = ink[(y + 2) * w + x]
                if (!hasAbove && !hasBelow) {
                    ink[y * w + x] = false
                }
            }
        }
    }

    val rowInk = IntArray(h)
    for (y in 0 until h) {
        var cnt = 0
        for (x in marginX until (w - marginX)) {
            if (ink[y * w + x]) cnt++
        }
        rowInk[y] = cnt
    }

    val minRowInk = max(3, (w * 0.008f).toInt())
    val minBandHeight = max(9, (h * 0.012f).toInt())
    val maxBandHeight = max(32, (h * 0.16f).toInt())

    val bands = mutableListOf<IntRange>()
    var bandStart = -1
    var gapCount = 0
    for (y in 0 until h) {
        if (rowInk[y] >= minRowInk) {
            if (bandStart == -1) bandStart = y
            gapCount = 0
        } else if (bandStart != -1) {
            gapCount++
            if (gapCount >= 3) {
                val bandEnd = y - gapCount
                val bh = bandEnd - bandStart + 1
                if (bh in minBandHeight..maxBandHeight) {
                    bands.add(bandStart..bandEnd)
                }
                bandStart = -1
                gapCount = 0
            }
        }
    }
    if (bandStart != -1) {
        val bh = (h - 1) - bandStart + 1
        if (bh in minBandHeight..maxBandHeight) {
            bands.add(bandStart until h)
        }
    }

    val results = mutableListOf<String>()

    for (band in bands) {
        val bh = band.last - band.first + 1
        val colInk = IntArray(w)
        for (x in marginX until (w - marginX)) {
            var c = 0
            for (y in band) {
                if (ink[y * w + x]) c++
            }
            colInk[x] = c
        }

        val charSpans = mutableListOf<IntRange>()
        var cStart = -1
        var cGap = 0
        val maxInternalGap = max(2, bh / 10)
        for (x in marginX until (w - marginX)) {
            if (colInk[x] >= 1) {
                if (cStart == -1) cStart = x
                cGap = 0
            } else if (cStart != -1) {
                cGap++
                if (cGap > maxInternalGap) {
                    val cEnd = x - cGap
                    val cw = cEnd - cStart + 1
                    if (cw >= 2) charSpans.add(cStart..cEnd)
                    cStart = -1
                    cGap = 0
                }
            }
        }
        if (cStart != -1) {
            val cEnd = (w - marginX - 1)
            if (cEnd - cStart + 1 >= 2) charSpans.add(cStart..cEnd)
        }

        if (charSpans.size < 2) continue

        data class GlyphBox(val x0: Int, val y0: Int, val x1: Int, val y1: Int, val pixelCount: Int)
        val boxes = mutableListOf<GlyphBox>()
        for (span in charSpans) {
            var yMin = band.last
            var yMax = band.first
            var pixelCount = 0
            for (y in band) {
                for (x in span) {
                    if (ink[y * w + x]) {
                        if (y < yMin) yMin = y
                        if (y > yMax) yMax = y
                        pixelCount++
                    }
                }
            }
            val gw = span.last - span.first + 1
            val gh = yMax - yMin + 1
            if (pixelCount >= 3 && gh >= 3 && gw <= bh * 2.4f) {
                if (gw > gh * 1.45f && gw >= 16 && gh >= bh * 0.45f) {
                    var minColX = (span.first + span.last) / 2
                    var minColVal = Int.MAX_VALUE
                    for (sx in (span.first + gw / 3)..(span.last - gw / 3)) {
                        if (colInk[sx] < minColVal) {
                            minColVal = colInk[sx]
                            minColX = sx
                        }
                    }
                    boxes.add(GlyphBox(span.first, yMin, minColX, yMax, pixelCount / 2))
                    boxes.add(GlyphBox(minColX + 1, yMin, span.last, yMax, pixelCount / 2))
                } else {
                    boxes.add(GlyphBox(span.first, yMin, span.last, yMax, pixelCount))
                }
            }
        }

        if (boxes.size < 2) continue

        // Classify all glyphs in the row first so we can locate '-' separator or widest gap
        val chars = CharArray(boxes.size)
        for (i in boxes.indices) {
            val b = boxes[i]
            chars[i] = classifyGlyph(ink, w, b.x0, b.y0, b.x1, b.y1, bh, band.first, band.last, allowOperators = true)
        }

        // Find explicit '-' separator or widest horizontal gap separating Date (left) and Prices (right)
        var dashIdx = -1
        for (i in 1 until min(boxes.size - 1, 10)) {
            if (chars[i] == '-') {
                dashIdx = i
                break
            }
        }

        val leftBoxes: List<GlyphBox>
        val leftChars: List<Char>
        val rightBoxes: List<GlyphBox>
        val rightChars: List<Char>

        if (dashIdx != -1) {
            leftBoxes = boxes.subList(0, dashIdx)
            leftChars = chars.take(dashIdx)
            rightBoxes = boxes.subList(dashIdx + 1, boxes.size)
            rightChars = chars.drop(dashIdx + 1)
        } else {
            var splitIndex = -1
            var maxGapWidth = -1
            val searchLimit = min(boxes.size - 1, 9)
            for (i in 0 until searchLimit) {
                val gap = boxes[i + 1].x0 - boxes[i].x1
                if (gap > maxGapWidth) {
                    maxGapWidth = gap
                    splitIndex = i
                }
            }
            if (splitIndex == -1) continue
            leftBoxes = boxes.subList(0, splitIndex + 1)
            leftChars = chars.take(splitIndex + 1)
            rightBoxes = boxes.subList(splitIndex + 1, boxes.size)
            rightChars = chars.drop(splitIndex + 1)
        }

        if (leftBoxes.isEmpty() || rightBoxes.isEmpty()) continue

        // Reconstruct left date string (supports both "9.10.26" and "9")
        val leftBuilder = StringBuilder()
        for (i in leftBoxes.indices) {
            val ch = leftChars[i]
            if (ch in '0'..'9') {
                leftBuilder.append(ch)
            } else if (ch == '.' || ch == '-') {
                if (leftBuilder.isNotEmpty() && leftBuilder.last() != '.') {
                    leftBuilder.append('.')
                }
            }
        }
        val dateToken = leftBuilder.toString().trim('.')
        if (dateToken.isEmpty()) continue

        // Validate day part of dateToken
        val dateParts = dateToken.split('.').filter { it.isNotEmpty() }
        val dayNum = dateParts.firstOrNull()?.toIntOrNull()?.let { d ->
            if (d > 31 && d >= 10) (d / 10).coerceIn(1, 31) else d
        } ?: continue
        if (dayNum !in 1..31) continue

        val formattedDatePart = if (dateParts.size >= 3) {
            val mNum = dateParts[1].toIntOrNull()?.coerceIn(1, 12) ?: 10
            val yNum = dateParts[2].takeLast(2)
            "$dayNum.$mNum.$yNum"
        } else if (dateParts.size == 2) {
            val mNum = dateParts[1].toIntOrNull()?.coerceIn(1, 12) ?: 10
            "$dayNum.$mNum"
        } else {
            "$dayNum"
        }

        // Reconstruct right prices string (supports "60+50", "40", etc.)
        val rightTokens = StringBuilder()
        var prevX1 = rightBoxes.first().x0
        for (idx in rightBoxes.indices) {
            val b = rightBoxes[idx]
            val gap = b.x0 - prevX1
            prevX1 = b.x1
            val ch = rightChars[idx]
            if (ch == '-' || ch == '.') continue
            if (ch == '+') {
                if (rightTokens.isNotEmpty() && rightTokens.last() != '+') {
                    rightTokens.append('+')
                }
            } else if (ch in '0'..'9') {
                if (idx > 0 && gap > bh * 0.75f && rightTokens.isNotEmpty() && rightTokens.last() != '+') {
                    rightTokens.append('+')
                }
                rightTokens.append(ch)
            }
        }

        val cleanedPrices = rightTokens.toString()
            .split('+')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 5..2500 }

        if (cleanedPrices.isNotEmpty()) {
            results.add("$formattedDatePart - ${cleanedPrices.joinToString("+")}")
        }
    }

    return results
}

private fun classifyGlyph(
    ink: BooleanArray,
    stride: Int,
    x0: Int,
    y0: Int,
    x1: Int,
    y1: Int,
    bandHeight: Int,
    bandY0: Int,
    bandY1: Int,
    allowOperators: Boolean
): Char {
    val gw = (x1 - x0 + 1).coerceAtLeast(1)
    val gh = (y1 - y0 + 1).coerceAtLeast(1)
    val aspect = gw.toFloat() / gh.toFloat()

    // Small low dot '.' in dates like 9.10.26
    if (allowOperators && gh <= bandHeight * 0.36f && gw <= bandHeight * 0.36f && y0 >= bandY0 + bandHeight * 0.45f) {
        return '.'
    }

    // Horizontal dash '-'
    if (allowOperators && gh <= bandHeight * 0.36f && aspect >= 1.35f) {
        return '-'
    }

    val z = FloatArray(9)
    var totalInk = 0
    for (y in y0..y1) {
        val zy = (((y - y0) * 3) / gh).coerceIn(0, 2)
        for (x in x0..x1) {
            if (ink[y * stride + x]) {
                val zx = (((x - x0) * 3) / gw).coerceIn(0, 2)
                z[zy * 3 + zx] += 1f
                totalInk++
            }
        }
    }
    val cellArea = (gw * gh / 9f).coerceAtLeast(1f)
    for (i in 0..8) z[i] = (z[i] / cellArea).coerceIn(0f, 1f)
    val fillRatio = totalInk.toFloat() / (gw * gh).toFloat()

    if (allowOperators && gh <= bandHeight * 0.82f && aspect in 0.60f..1.50f) {
        val crossSum = z[1] + z[3] + z[4] + z[5] + z[7]
        val cornerSum = z[0] + z[2] + z[6] + z[8]
        if (z[4] > 0.32f && crossSum > cornerSum * 1.95f) {
            return '+'
        }
    }

    if (aspect < 0.45f || (gw <= 6 && gh >= 10)) {
        return '1'
    }

    val holes = countEnclosedHoles(ink, stride, x0, y0, x1, y1)

    if (holes >= 2) return '8'

    if (holes == 1) {
        val topInk = z[0] + z[1] + z[2]
        val botInk = z[6] + z[7] + z[8]
        val midCenter = z[4]
        return when {
            midCenter < 0.24f && abs(topInk - botInk) < 0.48f -> '0'
            topInk > botInk + 0.22f -> '9'
            botInk > topInk + 0.22f -> '6'
            else -> '0'
        }
    }

    val topBar = (z[0] + z[1] + z[2]) / 3f
    val botBar = (z[6] + z[7] + z[8]) / 3f
    val leftTop = z[0] + z[3]
    val rightTop = z[2] + z[5]
    val leftBot = z[3] + z[6]
    val rightBot = z[5] + z[8]

    return when {
        topBar > 0.34f && botBar < 0.28f && z[3] < 0.24f -> '7'
        z[3] + z[4] + z[5] > 1.02f && botBar < 0.34f && z[0] > 0.22f -> '4'
        leftTop > rightTop + 0.16f && rightBot > leftBot -> '5'
        rightTop > leftTop && leftBot > rightBot * 0.82f && botBar > 0.32f -> '2'
        (rightTop + rightBot) > (leftTop + leftBot) * 1.22f -> '3'
        fillRatio < 0.32f -> '0'
        else -> '5'
    }
}

private fun countEnclosedHoles(
    ink: BooleanArray,
    stride: Int,
    x0: Int,
    y0: Int,
    x1: Int,
    y1: Int
): Int {
    val gw = x1 - x0 + 1
    val gh = y1 - y0 + 1
    if (gw < 5 || gh < 7) return 0

    val pw = gw + 2
    val ph = gh + 2
    val visited = BooleanArray(pw * ph)
    for (py in 1..gh) {
        val sy = y0 + py - 1
        for (px in 1..gw) {
            val sx = x0 + px - 1
            if (ink[sy * stride + sx]) {
                visited[py * pw + px] = true
            }
        }
    }

    val queue = IntArray(pw * ph)
    var head = 0
    var tail = 0
    visited[0] = true
    queue[tail++] = 0

    val dx = intArrayOf(1, -1, 0, 0)
    val dy = intArrayOf(0, 0, 1, -1)

    while (head < tail) {
        val curr = queue[head++]
        val cx = curr % pw
        val cy = curr / pw
        for (d in 0..3) {
            val nx = cx + dx[d]
            val ny = cy + dy[d]
            if (nx in 0 until pw && ny in 0 until ph) {
                val nIdx = ny * pw + nx
                if (!visited[nIdx]) {
                    visited[nIdx] = true
                    queue[tail++] = nIdx
                }
            }
        }
    }

    var holes = 0
    val minHolePixels = max(2, (gw * gh) / 36)
    for (py in 1..gh) {
        for (px in 1..gw) {
            val startIdx = py * pw + px
            if (!visited[startIdx]) {
                var compSize = 0
                head = 0
                tail = 0
                visited[startIdx] = true
                queue[tail++] = startIdx
                while (head < tail) {
                    val curr = queue[head++]
                    compSize++
                    val cx = curr % pw
                    val cy = curr / pw
                    for (d in 0..3) {
                        val nx = cx + dx[d]
                        val ny = cy + dy[d]
                        if (nx in 1..gw && ny in 1..gh) {
                            val nIdx = ny * pw + nx
                            if (!visited[nIdx]) {
                                visited[nIdx] = true
                                queue[tail++] = nIdx
                            }
                        }
                    }
                }
                if (compSize >= minHolePixels) {
                    holes++
                }
            }
        }
    }
    return holes
}

/**
 * Super-flexible parser for Khata lines:
 * Understands all variations with or without spaces, full dates, or day-only lines:
 * - `9.10.26 - 60+50`
 * - `11.10.26 - 40`
 * - `13.10.26 - 20`
 * - `31.10.26 - 40`
 * - `9.10.26-60+50` (no spaces)
 * - `9/10/2026 60+50` (slash date, space separator)
 * - `9-10-26 : 60 + 50` (hyphen date)
 * - `17 - 60+40` or `17 60+40` or `17-60+40` (day only)
 */
/**
 * Runs Google ML Kit on-device Text Recognition first, repairs handwritten OCR errors via
 * [normalizeMessyKhataOcrLine], and falls back to [analyzeHandwrittenKhataBitmap] if needed.
 */
private suspend fun runHybridKhataOcr(bitmap: Bitmap): List<String> {
    val mlKitLines = runCatching {
        suspendCancellableCoroutine<List<String>> { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val rawVisualLines = visionText.textBlocks
                        .flatMap { it.lines }
                        .sortedBy { it.boundingBox?.top ?: 0 }
                        .map { it.text.trim() }
                        .filter { it.isNotEmpty() }

                    // Merge horizontally adjacent fragments on the same vertical row if ML Kit split a date and price into 2 blocks
                    val visualElements = visionText.textBlocks
                        .flatMap { it.lines }
                        .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))

                    val mergedRows = mutableListOf<String>()
                    var currentTop = -1000
                    var currentHeight = 40
                    val currentRowParts = mutableListOf<Pair<Int, String>>()

                    for (line in visualElements) {
                        val box = line.boundingBox
                        val top = box?.top ?: 0
                        val left = box?.left ?: 0
                        val height = (box?.height() ?: 40).coerceAtLeast(20)
                        if (currentRowParts.isEmpty() || abs(top - currentTop) <= (max(currentHeight, height) * 0.55f)) {
                            if (currentRowParts.isEmpty()) {
                                currentTop = top
                                currentHeight = height
                            }
                            currentRowParts.add(left to line.text.trim())
                        } else {
                            mergedRows.add(currentRowParts.sortedBy { it.first }.joinToString(" ") { it.second })
                            currentRowParts.clear()
                            currentTop = top
                            currentHeight = height
                            currentRowParts.add(left to line.text.trim())
                        }
                    }
                    if (currentRowParts.isNotEmpty()) {
                        mergedRows.add(currentRowParts.sortedBy { it.first }.joinToString(" ") { it.second })
                    }

                    val candidateLines = (if (mergedRows.isNotEmpty()) mergedRows else rawVisualLines)
                        .map { normalizeMessyKhataOcrLine(it) }
                        .filter { it.isNotBlank() }

                    if (cont.isActive) cont.resume(candidateLines)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(emptyList())
                }
        }
    }.getOrDefault(emptyList())

    if (mlKitLines.isNotEmpty()) {
        return mlKitLines
    }
    return analyzeHandwrittenKhataBitmap(bitmap).map { normalizeMessyKhataOcrLine(it) }
}

/**
 * Repairs common handwritten Khata & Google Lens OCR errors such as:
 * - `01/10/26-40` -> `01/10/26 - 40`
 * - `2/10/2660+20+` -> `2/10/26 - 60+20` (merged 2-digit year and price, trailing `+`)
 * - `4/10/26→50` -> `4/10/26 - 50` (arrow symbol)
 * - `6p 10 ( 26 -> 70 + 20` -> `6/10/26 - 70+20` (`p` and `(` misread from handwritten `/`)
 * - `H1of2645` -> `7/10/26 - 45` (`7/1` misread as `H1`, `0/` misread as `of`, merged `26` and `45`)
 * - `(9/16) / 26 -> 60 + 50` -> `9/10/26 - 60+50` (`(9/16)` misread from looped `9/10/`)
 * - `11/10/2640/` -> `11/10/26 - 40` (merged year+price and trailing `/`)
 * - `13/10/26201-` -> `13/10/26 - 20` (merged year+price and `/-` misread as `1-`)
 * - `31/10/26-401-` or `31/10/26 -> 40ff-` -> `31/10/26 - 40` (`/-` misread as `1-` or `ff-`)
 */
fun normalizeMessyKhataOcrLine(rawLine: String): String {
    var s = rawLine.trim()
    if (s.isEmpty()) return ""

    // 1. Normalize arrows and dashes
    s = s.replace("→", " - ")
        .replace("->", " - ")
        .replace("=>", " - ")
        .replace("—", "-")
        .replace("–", "-")
        .replace("~", "-")

    // 2. Repair handwritten "7/10/26" misread as "H1of26..." or "H10/26..." or "71of26..."
    s = s.replace(Regex("""^[HhTt]1\s*[oO0][fF/]\s*(\d{2})\s*(\d+)$"""), "7/10/$1 - $2")
    s = s.replace(Regex("""^[HhTt]1\s*[oO0][fF/]\s*(\d{2})\s*[-:]?\s*(.+)$"""), "7/10/$1 - $2")
    s = s.replace(Regex("""^7\s*1\s*[oO0][fF/]\s*(\d{2})\s*[-:]?\s*(.+)$"""), "7/10/$1 - $2")
    s = s.replace(Regex("""^(\d{1,2})\s*[oO0][fF]\s*(\d{2})\s*(\d+)$"""), "$1/10/$2 - $3")

    // 3. Remove parentheses around date segments like "(9/16) / 26" and repair looped "/10/" misread as "p 10 (" or "/16)"
    s = s.replace("(", "/").replace(")", "/")
    s = s.replace(Regex("""^(\d{1,2})\s*[pP/|lI]\s*(\d{1,2})\s*[/|lI(\s]+\s*(\d{2,4})"""), "$1/$2/$3")
    s = s.replace(Regex("""/+"""), "/")
    s = s.replace(Regex("""^/"""), "")

    // If month was misread as 16 or 18 (looped 0 in 10), repair to 10
    s = s.replace(Regex("""^(\d{1,2})/(16|18|19)/(\d{2,4})"""), "$1/10/$3")

    // 4. Split merged 2-digit year + price when there is no separator (e.g., "2/10/2660+20+" -> "2/10/26 - 60+20+")
    s = s.replace(
        Regex("""^(\d{1,2}[./-]\d{1,2}[./-](?:2\d|3[0-5]))(\d{2,}.*)$"""),
        "$1 - $2"
    )

    // 5. Ensure a clean " - " separator between the date prefix and price expression
    val sepMatch = Regex("""^(\d{1,2}(?:[./-]\d{1,2}(?:[./-]\d{2,4})?)?)\s*(?:[-:=>|]+\s*|\s+)(.+)$""").find(s)
    if (sepMatch != null) {
        val datePart = sepMatch.groupValues[1].trim()
        var pricePart = sepMatch.groupValues[2].trim()

        // Clean each '+' separated price token for Indian Rupee trailing "/-" misreads ("1-", "ff-", "/", "|-")
        val cleanedTokens = pricePart.split(Regex("""[+,&]"""))
            .map { tok ->
                var t = tok.trim()
                // Strip trailing "/-", "ff-", "f-", "|-", "/"
                t = t.replace(Regex("""(?:/-|ff-|f-|\|-|/|-)+$""", RegexOption.IGNORE_CASE), "").trim()
                // If a 3+ digit number ends with "1" because "/-" was misread as "1-" (e.g. "201-" -> "201", "401-" -> "401")
                if (tok.trim().endsWith("1-") || tok.trim().endsWith("1/")) {
                    val withoutOne = t.removeSuffix("1")
                    if (withoutOne.isNotEmpty() && withoutOne.all { it.isDigit() }) {
                        t = withoutOne
                    }
                } else if (t.length == 3 && t.endsWith("1") && (t.startsWith("20") || t.startsWith("30") || t.startsWith("40") || t.startsWith("50") || t.startsWith("60") || t.startsWith("70") || t.startsWith("80") || t.startsWith("90"))) {
                    t = t.substring(0, 2)
                }
                t
            }
            .filter { it.isNotBlank() && it.any { ch -> ch.isDigit() } }

        if (cleanedTokens.isNotEmpty()) {
            pricePart = cleanedTokens.joinToString("+")
            return "$datePart - $pricePart"
        }
    }

    return s
}

fun parseKhataRawText(
    rawText: String,
    fallbackYear: Int,
    fallbackMonth: Int,
    hotelId: Int,
    hotelMenus: List<MenuEntity>,
    existingTransactions: List<HotelTransactionEntity>
): List<KhataDayEntry> {
    val lines = rawText.lines().map { normalizeMessyKhataOcrLine(it) }.filter { it.isNotEmpty() }
    val entries = mutableListOf<KhataDayEntry>()

    val hotelTx = existingTransactions.filter { it.hotelId == hotelId && !it.isDeleted }

    val flexibleLineRegex = Regex(
        """^\s*(\d{1,2})(?:[./-](\d{1,2})(?:[./-](\d{2,4}))?)?\s*(?:[-–—:=>|]+\s*|\s+)(.+)$"""
    )

    for (rawLine in lines) {
        val normalizedLine = rawLine
            .replace('—', '-')
            .replace('–', '-')
            .replace('~', '-')
            .trim()

        val match = flexibleLineRegex.find(normalizedLine) ?: continue
        val day = match.groupValues[1].toIntOrNull() ?: continue
        if (day !in 1..31) continue

        val monthGroup = match.groupValues[2]
        val yearGroup = match.groupValues[3]
        val pricesExpr = match.groupValues[4].trim()

        val hasExplicitMonth = monthGroup.isNotBlank()
        val parsedMonthZeroBased = if (hasExplicitMonth) {
            val m = monthGroup.toIntOrNull() ?: (fallbackMonth + 1)
            (m - 1).coerceIn(0, 11)
        } else {
            fallbackMonth
        }

        val parsedYear = if (yearGroup.isNotBlank()) {
            val y = yearGroup.toIntOrNull() ?: fallbackYear
            if (y < 100) 2000 + y else y
        } else {
            fallbackYear
        }

        // Extract all '+' or ',' separated price tokens from the right side
        val priceTokens = pricesExpr.split(Regex("""[+,&]"""))
            .map { it.trim() }
            .mapNotNull { token ->
                Regex("""(\d+(\.\d+)?)""").find(token)?.groupValues?.get(1)?.toDoubleOrNull()
            }
            .filter { it > 0.0 }

        if (priceTokens.isEmpty()) continue

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, parsedYear)
            set(Calendar.MONTH, parsedMonthZeroBased)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, day.coerceAtMost(maxDay))
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val ts = cal.timeInMillis

        val matchingExistingTxs = hotelTx.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.YEAR) == parsedYear &&
                txCal.get(Calendar.MONTH) == parsedMonthZeroBased &&
                txCal.get(Calendar.DAY_OF_MONTH) == day
        }
        val hasExisting = matchingExistingTxs.isNotEmpty()
        val existingTotal = matchingExistingTxs.sumOf { it.totalAmount }
        val existingSummary = if (hasExisting) {
            val dishStrings = matchingExistingTxs.flatMap { tx ->
                TransactionSerializer.deserializeGroups(tx.itemsSerialized).flatMap { g ->
                    g.items.map { item ->
                        if (item.quantity > 1) "${item.name} ×${item.quantity} (₹${(item.price * item.quantity).toInt()})"
                        else "${item.name} (₹${item.price.toInt()})"
                    }
                }
            }
            if (dishStrings.isNotEmpty()) {
                "${dishStrings.joinToString(", ")} • Total: ₹${existingTotal.toInt()}"
            } else {
                "Existing Total: ₹${existingTotal.toInt()}"
            }
        } else ""

        val segments = priceTokens.map { priceVal ->
            val guesses = computeMenuCombinationsForPrice(priceVal, hotelMenus)
            KhataPriceSegment(
                price = priceVal,
                guesses = guesses,
                selectedGuessIndex = 0
            )
        }

        entries.add(
            KhataDayEntry(
                dayOfMonth = day,
                month = parsedMonthZeroBased,
                year = parsedYear,
                hasExplicitMonthYear = hasExplicitMonth,
                timestamp = ts,
                hasExistingData = hasExisting,
                existingSummary = existingSummary,
                existingTotalAmount = existingTotal,
                segments = segments,
                includeInImport = !hasExisting,
                useNewWhenExisting = false
            )
        )
    }

    return entries.sortedBy { it.timestamp }
}

/**
 * Computes up to 4 most relevant menu combinations for a single price token (e.g. ₹60 -> 1x ₹60 item, or ₹40 + ₹20, or 3x ₹20).
 */
fun computeMenuCombinationsForPrice(
    targetPrice: Double,
    menus: List<MenuEntity>
): List<MenuCombinationGuess> {
    if (menus.isEmpty() || targetPrice <= 0.0) return emptyList()
    val validMenus = menus.filter { it.price > 0.0 }.sortedByDescending { it.price }
    val results = mutableListOf<MenuCombinationGuess>()

    // 1. Exact single item match (1x Dish)
    validMenus.filter { abs(it.price - targetPrice) < 0.01 }.forEach { item ->
        val dishName = item.englishName.ifBlank { item.banglaName }
        results.add(
            MenuCombinationGuess(
                description = "$dishName (₹${item.price.toInt()})",
                itemsWithQty = listOf(item to 1),
                totalPrice = item.price
            )
        )
    }

    // 2. Exact multiple of a single item (e.g., 20 x 3 = 60)
    validMenus.forEach { item ->
        val dishName = item.englishName.ifBlank { item.banglaName }
        val ratio = (targetPrice / item.price).roundToInt()
        if (ratio in 2..10 && abs(item.price * ratio - targetPrice) < 0.01) {
            val desc = "$dishName × $ratio (₹${item.price.toInt()}×$ratio)"
            if (results.none { it.description == desc }) {
                results.add(
                    MenuCombinationGuess(
                        description = desc,
                        itemsWithQty = listOf(item to ratio),
                        totalPrice = targetPrice
                    )
                )
            }
        }
    }

    // 3. Two-item combinations (e.g. 40 + 20 = 60)
    for (i in validMenus.indices) {
        for (j in i + 1 until validMenus.size) {
            val m1 = validMenus[i]
            val m2 = validMenus[j]
            if (abs((m1.price + m2.price) - targetPrice) < 0.01) {
                val n1 = m1.englishName.ifBlank { m1.banglaName }
                val n2 = m2.englishName.ifBlank { m2.banglaName }
                val desc = "$n1 (₹${m1.price.toInt()}) + $n2 (₹${m2.price.toInt()})"
                results.add(
                    MenuCombinationGuess(
                        description = desc,
                        itemsWithQty = listOf(m1 to 1, m2 to 1),
                        totalPrice = targetPrice
                    )
                )
            }
        }
    }

    // 4. Quantity + second item combinations (e.g. 2x ₹20 + 1x ₹10 = ₹50)
    if (results.size < 4) {
        for (i in validMenus.indices) {
            for (j in validMenus.indices) {
                if (i == j) continue
                val m1 = validMenus[i]
                val m2 = validMenus[j]
                for (qty1 in 2..4) {
                    if (abs((m1.price * qty1 + m2.price) - targetPrice) < 0.01) {
                        val n1 = m1.englishName.ifBlank { m1.banglaName }
                        val n2 = m2.englishName.ifBlank { m2.banglaName }
                        val desc = "$n1 ×$qty1 (₹${(m1.price * qty1).toInt()}) + $n2 (₹${m2.price.toInt()})"
                        if (results.none { it.description == desc }) {
                            results.add(
                                MenuCombinationGuess(
                                    description = desc,
                                    itemsWithQty = listOf(m1 to qty1, m2 to 1),
                                    totalPrice = targetPrice
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    return results.distinctBy { it.description }.take(4)
}
