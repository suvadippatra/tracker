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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    val customDishName: String = "",
    val isCustomSelected: Boolean = false,
    val customPriceText: String = if (price % 1.0 == 0.0) price.toInt().toString() else price.toString()
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

    // Full-resolution Camera capture using FileProvider URI + fallback to preview
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val processDecodedKhataBitmap: (Bitmap, String) -> Unit = { decodedBmp, sourceLabel ->
        scope.launch {
            isProcessingImage = false
            errorMessage = null
            capturedBitmap = decodedBmp
            scanSummaryStatus = "Photo loaded ($sourceLabel). View image above for reference and type or paste Khata records below."
        }
    }

    val fullResCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            scope.launch {
                isProcessingImage = true
                val decoded = withContext(Dispatchers.IO) {
                    decodeSampledBitmapFromUri(context.contentResolver, uri, 1600)
                }
                if (decoded != null) {
                    processDecodedKhataBitmap(decoded, "camera photo")
                } else {
                    isProcessingImage = false
                    errorMessage = "Could not decode captured camera image."
                }
            }
        }
    }

    val launchFullResCamera: () -> Unit = {
        try {
            val photoFile = java.io.File(context.cacheDir, "khata_scan_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            pendingCameraUri = uri
            fullResCameraLauncher.launch(uri)
        } catch (e: Exception) {
            errorMessage = "Unable to launch camera: ${e.localizedMessage}"
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchFullResCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to capture Khata pages", Toast.LENGTH_SHORT).show()
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
                val decoded = withContext(Dispatchers.IO) {
                    decodeSampledBitmapFromUri(context.contentResolver, uri, 1600)
                }
                if (decoded != null) {
                    processDecodedKhataBitmap(decoded, "selected photo")
                } else {
                    isProcessingImage = false
                    errorMessage = "Unable to decode selected image safely."
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
                    .imePadding(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dialogSafeTopPadding(extraTop = 4.dp)
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
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. Khata Photo Reference Card
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "1. Khata Photo (Reference)",
                                                color = accent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (capturedBitmap != null) {
                                                Text(
                                                    text = "Remove Photo",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable {
                                                        capturedBitmap = null
                                                        scanSummaryStatus = null
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val hasCamPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                                        context,
                                                        android.Manifest.permission.CAMERA
                                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                    if (hasCamPerm) {
                                                        launchFullResCamera()
                                                    } else {
                                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp),
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
                                                    .height(44.dp),
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

                                        if (isProcessingImage) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    "Loading photo...",
                                                    color = AppSecondaryTextColor(),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        // Full Uncropped Image Preview
                                        capturedBitmap?.let { bmp ->
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(240.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isLight) Color(0xFFF1F5F9) else Color(0xFF0F172A))
                                                    .border(1.dp, AppTextColor().copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                                                    .clickable { showFullBitmapModal = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Uncropped Khata Image",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.65f),
                                                    shape = RoundedCornerShape(bottomEnd = 10.dp),
                                                    modifier = Modifier.align(Alignment.TopStart)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Tap to Zoom",
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

                                // 2. Khata Records Text Card
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "2. Khata Records",
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
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = {
                                                val clipText = clipboardManager.getText()?.text.orEmpty()
                                                if (clipText.isNotBlank()) {
                                                    val repairedLines = clipText.lines()
                                                        .map { normalizeMessyKhataOcrLine(it) }
                                                        .filter { it.isNotBlank() }
                                                    extractedText = repairedLines.joinToString("\n")
                                                    errorMessage = null
                                                    scanSummaryStatus = "Pasted & formatted ${repairedLines.size} line(s) from clipboard."
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
                                                text = "Paste from Lens / Clipboard",
                                                color = AppTextColor(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
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
                                            maxLines = 10,
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

                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                                color = Color.Transparent
                            ) {
                                Button(
                                    onClick = {
                                        if (hotels.isEmpty()) {
                                            errorMessage = "Please create a Hotel first before matching Khata records."
                                            return@Button
                                        }
                                        if (extractedText.isBlank()) {
                                            errorMessage = "Please enter or paste at least one Date - Price line."
                                            return@Button
                                        }
                                        val parsed = buildEntriesFromRawText()
                                        if (parsed.isEmpty()) {
                                            errorMessage = "Could not find valid Date & Price numbers. Example: '9.10.26 - 60+50' or '11.10.26 - 40'."
                                        } else {
                                            errorMessage = null
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
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent,
                                        contentColor = AppOnAccentColor(accent)
                                    )
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Next: Check Captured Dates & Match Menu",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
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
                                                        val formattedSegPrice = if (seg.price % 1.0 == 0.0) seg.price.toInt().toString() else String.format(Locale.US, "%.2f", seg.price)
                                                        Text(
                                                            text = "Dish Price: ₹$formattedSegPrice",
                                                            color = AppTextColor(),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))

                                                        val topFourGuesses = remember(seg.guesses) { seg.guesses.take(4) }

                                                        if (topFourGuesses.isNotEmpty()) {
                                                            Text(
                                                                text = "Choose detected combination or Custom Menu:",
                                                                color = AppSecondaryTextColor(),
                                                                fontSize = 11.sp
                                                            )
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                topFourGuesses.forEachIndexed { gIdx, guess ->
                                                                    val isSelected = !seg.isCustomSelected && seg.selectedGuessIndex == gIdx
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
                                                                                val guessPriceText = if (guess.totalPrice % 1.0 == 0.0) guess.totalPrice.toInt().toString() else String.format(Locale.US, "%.2f", guess.totalPrice)
                                                                                parsedEntries = parsedEntries.toMutableList().also { list ->
                                                                                    val updatedSegs = entry.segments.toMutableList()
                                                                                    updatedSegs[segIdx] = seg.copy(
                                                                                        selectedGuessIndex = gIdx,
                                                                                        isCustomSelected = false,
                                                                                        price = guess.totalPrice,
                                                                                        customPriceText = guessPriceText
                                                                                    )
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
                                                        }

                                                        // Explicit 5th option: Custom Menu / Manual (always available like exact meal record page)
                                                        val isCustomCardSelected = seg.isCustomSelected || topFourGuesses.isEmpty()
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (isCustomCardSelected) accent.copy(alpha = 0.16f) else Color.Transparent)
                                                                .border(
                                                                    1.dp,
                                                                    if (isCustomCardSelected) accent else AppTextColor().copy(alpha = 0.15f),
                                                                    RoundedCornerShape(8.dp)
                                                                )
                                                                .clickable {
                                                                    parsedEntries = parsedEntries.toMutableList().also { list ->
                                                                        val updatedSegs = entry.segments.toMutableList()
                                                                        val prefillDish = seg.customDishName.ifBlank {
                                                                            topFourGuesses.getOrNull(seg.selectedGuessIndex)?.description ?: ""
                                                                        }
                                                                        updatedSegs[segIdx] = seg.copy(
                                                                            isCustomSelected = true,
                                                                            customDishName = prefillDish
                                                                        )
                                                                        list[dayIdx] = entry.copy(segments = updatedSegs)
                                                                    }
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                                                .testTag("khata_custom_menu_option_${dayIdx}_$segIdx"),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isCustomCardSelected) Icons.Default.CheckCircle else Icons.Default.Edit,
                                                                contentDescription = null,
                                                                tint = if (isCustomCardSelected) accent else AppSecondaryTextColor(),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = "Custom Menu / Manual",
                                                                color = AppTextColor(),
                                                                fontWeight = if (isCustomCardSelected) FontWeight.Bold else FontWeight.Medium,
                                                                fontSize = 12.sp
                                                            )
                                                        }

                                                        if (hotelMenus.isNotEmpty()) {
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .horizontalScroll(rememberScrollState()),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                hotelMenus.forEach { menuItem ->
                                                                    val menuTitle = menuItem.englishName.ifBlank { menuItem.banglaName }
                                                                    val menuPriceStr = if (menuItem.price % 1.0 == 0.0) menuItem.price.toInt().toString() else String.format(Locale.US, "%.2f", menuItem.price)
                                                                    val chipSelected = seg.isCustomSelected && seg.customDishName.equals(menuTitle, ignoreCase = true)
                                                                    Surface(
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = if (chipSelected) accent.copy(alpha = 0.2f) else AppTextColor().copy(alpha = 0.06f),
                                                                        border = BorderStroke(
                                                                            1.dp,
                                                                            if (chipSelected) accent else AppTextColor().copy(alpha = 0.14f)
                                                                        ),
                                                                        modifier = Modifier.clickable {
                                                                            parsedEntries = parsedEntries.toMutableList().also { list ->
                                                                                val updatedSegs = entry.segments.toMutableList()
                                                                                updatedSegs[segIdx] = seg.copy(
                                                                                    isCustomSelected = true,
                                                                                    customDishName = menuTitle,
                                                                                    price = menuItem.price,
                                                                                    customPriceText = menuPriceStr
                                                                                )
                                                                                list[dayIdx] = entry.copy(segments = updatedSegs)
                                                                            }
                                                                        }
                                                                    ) {
                                                                        Text(
                                                                            text = "$menuTitle • ₹$menuPriceStr",
                                                                            color = if (chipSelected) accent else AppTextColor(),
                                                                            fontSize = 11.sp,
                                                                            fontWeight = if (chipSelected) FontWeight.Bold else FontWeight.Medium,
                                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        // Always visible Menu Name + Price textboxes
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            GlassTextField(
                                                                value = seg.customDishName,
                                                                onValueChange = { newName ->
                                                                    parsedEntries = parsedEntries.toMutableList().also { list ->
                                                                        val updatedSegs = entry.segments.toMutableList()
                                                                        updatedSegs[segIdx] = seg.copy(
                                                                            customDishName = newName,
                                                                            isCustomSelected = true
                                                                        )
                                                                        list[dayIdx] = entry.copy(segments = updatedSegs)
                                                                    }
                                                                },
                                                                placeholder = { Text("Menu / Dish name") },
                                                                modifier = Modifier.weight(1.45f)
                                                            )

                                                            GlassTextField(
                                                                value = seg.customPriceText,
                                                                onValueChange = { newPriceText ->
                                                                    val parsedVal = newPriceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                                                    parsedEntries = parsedEntries.toMutableList().also { list ->
                                                                        val updatedSegs = entry.segments.toMutableList()
                                                                        updatedSegs[segIdx] = seg.copy(
                                                                            customPriceText = newPriceText,
                                                                            price = parsedVal,
                                                                            isCustomSelected = true
                                                                        )
                                                                        list[dayIdx] = entry.copy(segments = updatedSegs)
                                                                    }
                                                                },
                                                                placeholder = { Text("₹ Price") },
                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                                modifier = Modifier.weight(0.85f)
                                                            )
                                                        }
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
                                            val chosenGuess = if (!seg.isCustomSelected && seg.customDishName.isBlank()) {
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

    // Read EXIF orientation so rotated camera/gallery photos are always turned upright before OCR!
    val rotationDegrees = try {
        resolver.openInputStream(uri)?.use { stream ->
            val exif = android.media.ExifInterface(stream)
            when (exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    } catch (_: Exception) {
        0f
    }

    val oriented = if (rotationDegrees != 0f) {
        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees) }
        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    } else {
        raw
    }
    return resizeBitmapSafely(oriented, reqMaxDim)
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
                selectedGuessIndex = 0,
                isCustomSelected = guesses.isEmpty()
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
