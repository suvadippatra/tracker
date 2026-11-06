package com.example.ui.screens.settings.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import java.text.SimpleDateFormat
import java.util.*
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
    val timestamp: Long,
    val hasExistingData: Boolean,
    val segments: List<KhataPriceSegment>,
    val includeInImport: Boolean = true
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
    var isProcessingImage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scanSummaryStatus by remember { mutableStateOf<String?>(null) }

    // Raw text starts completely empty (never pre-written)
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
            year = year,
            month = month,
            hotelId = hotelId,
            hotelMenus = menus,
            existingTransactions = allTransactions
        )
    }

    BackHandler {
        if (step == 2) step = 1 else onDismiss()
    }

    // Camera capture (thumbnail/safe bitmap)
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
                        resizeBitmapSafely(bmp, 1280)
                    }
                    capturedBitmap = safeBmp
                    val scannedLines = withContext(Dispatchers.Default) {
                        analyzeHandwrittenKhataBitmap(safeBmp)
                    }
                    if (scannedLines.isNotEmpty()) {
                        extractedText = scannedLines.joinToString("\n")
                        scanSummaryStatus = if (isBn) "${scannedLines.size}টি খাতা লাইন শনাক্ত হয়েছে। প্রয়োজনে নিচে সংশোধন করুন।"
                        else "Detected ${scannedLines.size} khata row(s) from image. Verify or refine raw lines below."
                    } else {
                        errorMessage = if (isBn) "ছবিতে স্পষ্ট তারিখ ও দাম শনাক্ত করা যায়নি। নিচে সরাসরি টাইপ করুন।"
                        else "Could not automatically isolate clear Date - Price rows from this image. Please type or adjust the raw lines below."
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to process camera image safely: ${e.localizedMessage}"
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
                        decodeSampledBitmapFromUri(context.contentResolver, uri, 1280)
                    }
                    if (decoded != null) {
                        capturedBitmap = decoded
                        val scannedLines = withContext(Dispatchers.Default) {
                            analyzeHandwrittenKhataBitmap(decoded)
                        }
                        if (scannedLines.isNotEmpty()) {
                            extractedText = scannedLines.joinToString("\n")
                            scanSummaryStatus = if (isBn) "${scannedLines.size}টি খাতা লাইন শনাক্ত হয়েছে। প্রয়োজনে নিচে সংশোধন করুন।"
                            else "Detected ${scannedLines.size} khata row(s) from image. Verify or refine raw lines below."
                        } else {
                            errorMessage = if (isBn) "ছবিতে স্পষ্ট তারিখ ও দাম শনাক্ত করা যায়নি। নিচে সরাসরি টাইপ করুন।"
                            else "Could not automatically isolate clear Date - Price rows from this photo. Please type or adjust the raw lines below."
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isLight) Color(0xFFF8FAFC) else Color(0xFF0B0F19))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                    if (isBn) "খাতা স্ক্যান (ধাপ ১: র ডেটা)" else "Scan Khata (Step 1: Raw Data)"
                                } else {
                                    if (isBn) "খাতা স্ক্যান (ধাপ ২: তারিখ ও মেনু মিলকরণ)" else "Scan Khata (Step 2: Date & Menu Match)"
                                },
                                color = AppTextColor(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (step == 1) {
                                    if (isBn) "খাতার ছবি তুলুন বা আপলোড করে তারিখ ও দাম স্ক্যান করুন" else "Capture or pick Khata photo to extract raw Date - Price lines"
                                } else {
                                    if (isBn) "মাস ও হোটেল অনুযায়ী ইতিমধ্যে থাকা তারিখ এবং খাবার যাচাই করুন" else "Verify captured dates in ledger and match prices to dishes"
                                },
                                color = AppSecondaryTextColor(),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    if (step == 2) {
                        TextButton(onClick = { step = 1 }) {
                            Text(if (isBn) "র টেক্সট এডিট" else "Edit Raw Lines", color = accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
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
                    // STEP 1: Pure Raw Scanning Page (No Month Toggle or Collision Badges Here)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = if (isBn) "১. খাতার ছবি স্ক্যান করুন" else "1. Capture or Import Khata Photo",
                                        color = accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isBn) "খাতার বাম দিকে তারিখ এবং ডান দিকে খাবারের দাম (যেমন: 17 - 40+60) থাকা ছবি নির্বাচন করুন।"
                                        else "Point camera at your handwritten Khata page or import an image. Our on-device optical line scanner segments rows into 'Date - Price1+Price2'.",
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
                                            modifier = Modifier.weight(1f).height(46.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accent,
                                                contentColor = AppOnAccentColor(accent)
                                            )
                                        ) {
                                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isBn) "ক্যামেরা" else "Camera Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Button(
                                            onClick = {
                                                galleryLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier.weight(1f).height(46.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF10B981),
                                                contentColor = AppOnAccentColor(Color(0xFF10B981))
                                            )
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isBn) "গ্যালারি ছবি" else "Pick Image", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }

                                    if (isProcessingImage) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                if (isBn) "খাতার লাইন ও সংখ্যা বিশ্লেষণ করা হচ্ছে..."
                                                else "Segmenting handwritten rows & recognizing digits...",
                                                color = AppSecondaryTextColor(),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    capturedBitmap?.let { bmp ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, AppTextColor().copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                        ) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Captured Khata Image",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
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
                                                                    analyzeHandwrittenKhataBitmap(bmp)
                                                                }
                                                                if (rescanned.isNotEmpty()) {
                                                                    extractedText = rescanned.joinToString("\n")
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
                                                        text = if (isBn) "পুনরায় স্ক্যান" else "Re-scan Image",
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
                        }

                        // Raw Data Editor Card
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isBn) "২. স্ক্যান করা র খাতা লাইন (Date - Prices)" else "2. Raw Extracted Khata Lines (Date - Prices)",
                                            color = accent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (extractedText.isNotBlank()) {
                                            Text(
                                                text = if (isBn) "মুছুন" else "Clear",
                                                color = Color(0xFFEF4444),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { extractedText = "" }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBn) "প্রতি লাইনে প্রথমে তারিখ এবং পরে দাম লিখুন (যেমন: 17 - 40+60 অথবা 18 - 50)। পরের ধাপে কোন তারিখ ইতিমধ্যে সেভ করা আছে এবং কোন দামে কোন খাবার পড়বে তা দেখা যাবে।"
                                        else "Format each line as 'Day - Price1+Price2' (e.g. '17 - 40+60' or '18 - 30'). In Step 2, you will see which dates are already recorded and match prices to menu items.",
                                        color = AppSecondaryTextColor(),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = extractedText,
                                        onValueChange = { extractedText = it },
                                        placeholder = {
                                            Text(
                                                if (isBn) "এখানে স্ক্যান করা তারিখ ও দাম আসবে (যেমন:\n17 - 40+60\n18 - 35)"
                                                else "Scanned raw lines appear here, or type directly:\n17 - 40+60\n18 - 35+50",
                                                color = AppSecondaryTextColor().copy(alpha = 0.6f),
                                                fontSize = 13.sp
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 180.dp, max = 320.dp),
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
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (hotels.isEmpty()) {
                                errorMessage = if (isBn) "প্রথমে একটি হোটেল যোগ করুন!" else "Please create a Hotel first before matching Khata records."
                                return@Button
                            }
                            if (extractedText.isBlank()) {
                                errorMessage = if (isBn) "অনুগ্রহ করে প্রথমে খাতার ছবি স্ক্যান করুন অথবা লাইন লিখুন।" else "Please scan a Khata image or enter at least one Date - Price line."
                                return@Button
                            }
                            val parsed = buildEntriesFromRawText()
                            if (parsed.isEmpty()) {
                                errorMessage = if (isBn) "কোনো বৈধ 'তারিখ - দাম' লাইন পাওয়া যায়নি (যেমন: 17 - 40+60)।" else "No valid 'Day - Price' lines found. Ensure lines follow '17 - 40+60' format."
                            } else {
                                errorMessage = null
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
                            text = if (isBn) "পরবর্তী ধাপ: তারিখ যাচাই ও মেনু মিলকরণ" else "Next: Check Captured Dates & Match Menu",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    // STEP 2: Target Hotel, Month Selection, Existing Date Collision Status, and Menu Combination Matching
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isBn) "টার্গেট হোটেল ও মাস নির্বাচন করুন" else "Target Hotel & Month for Matching",
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
                                text = if (isBn) "নতুন তারিখ: $newCount টি" else "New Dates: $newCount",
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
                                text = if (isBn) "ইতিমধ্যে খাতায় আছে: $capturedCount টি" else "Already in Ledger: $capturedCount",
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(parsedEntries) { dayIdx, entry ->
                            val dateStr = remember(entry.timestamp) {
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entry.timestamp))
                            }
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = entry.includeInImport,
                                                onCheckedChange = { checked ->
                                                    parsedEntries = parsedEntries.toMutableList().also {
                                                        it[dayIdx] = entry.copy(includeInImport = checked)
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
                                                        text = if (isBn) "⚠️ এই তারিখের ডেটা ইতিমধ্যে লেজারে রয়েছে" else "⚠️ Date already captured in Meal Ledger",
                                                        color = Color(0xFFF59E0B),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                } else {
                                                    Text(
                                                        text = if (isBn) "✓ নতুন তারিখ (লেজারে নেই)" else "✓ Ready to import (not yet in ledger)",
                                                        color = Color(0xFF10B981),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "₹${entry.segments.sumOf { it.price }.toInt()}",
                                            color = accent,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    entry.segments.forEachIndexed { segIdx, seg ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(getGlassIconButtonBg().copy(alpha = 0.65f))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = if (isBn) "দাম: ₹${seg.price.toInt()} (পৃথক খাবার)" else "Price Token: ₹${seg.price.toInt()} (Separate Dish)",
                                                color = AppTextColor(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))

                                            when {
                                                seg.guesses.isEmpty() -> {
                                                    Text(
                                                        text = if (isBn) "মেনুতে ₹${seg.price.toInt()}-এর কোনো মিল পাওয়া যায়নি। খাবারের নাম লিখুন:"
                                                        else "No menu combination matches ₹${seg.price.toInt()}. Enter dish name:",
                                                        color = AppSecondaryTextColor(),
                                                        fontSize = 11.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
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
                                                            Text(if (isBn) "খাবারের নাম (যেমন: Special Thali)" else "Custom dish name (e.g. Special Meal)")
                                                        }
                                                    )
                                                }
                                                seg.guesses.size > 4 -> {
                                                    Text(
                                                        text = if (isBn) "৪টির বেশি সম্ভাব্য কম্বিনেশন পাওয়া গেছে (${seg.guesses.size}টি)। সঠিক খাবারটি লিখুন বা নিচের থেকে বাছুন:"
                                                        else "More than 4 possible combinations (${seg.guesses.size}). Please specify dish name or pick top guess:",
                                                        color = Color(0xFFF59E0B),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
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
                                                            Text(if (isBn) "খাবারের নাম লিখুন..." else "Type exact dish name...")
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        seg.guesses.take(4).forEachIndexed { gIdx, guess ->
                                                            FilterChip(
                                                                selected = seg.customDishName.isBlank() && seg.selectedGuessIndex == gIdx,
                                                                onClick = {
                                                                    parsedEntries = parsedEntries.toMutableList().also { list ->
                                                                        val updatedSegs = entry.segments.toMutableList()
                                                                        updatedSegs[segIdx] = seg.copy(selectedGuessIndex = gIdx, customDishName = "")
                                                                        list[dayIdx] = entry.copy(segments = updatedSegs)
                                                                    }
                                                                },
                                                                label = { Text(guess.description, fontSize = 11.sp) }
                                                            )
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    Text(
                                                        text = if (isBn) "সম্ভাব্য মেনু নির্বাচন করুন (১-৪টি অনুমান):" else "Select matching menu combination (up to 4 guesses):",
                                                        color = AppSecondaryTextColor(),
                                                        fontSize = 11.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        seg.guesses.forEachIndexed { gIdx, guess ->
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
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val toImport = parsedEntries.filter { it.includeInImport }
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
                                        if (isBn) "$count দিনের খাতা রেকর্ড ইমপোর্ট হয়েছে!" else "Imported $count Khata day record(s)!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                }
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
                            text = if (isBn) "খাতা রেকর্ড ইমপোর্ট নিশ্চিত করুন (${parsedEntries.count { it.includeInImport }} দিন)"
                            else "Import Selected Khata Records (${parsedEntries.count { it.includeInImport }} Days)",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
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
 * Pure Kotlin/Android optical row and digit/operator segmentation for handwritten or printed Khata pages:
 * 1. Downscales to processing width (max 720px) and computes local integral-image adaptive binarization.
 * 2. Suppresses full-width horizontal notebook rules so ruled lines do not fuse digits.
 * 3. Segments horizontal text rows via ink density projection.
 * 4. Within each row, extracts connected components, merges horizontally adjacent stroke fragments,
 *    classifies each glyph into ('0'..'9', '+', '-') using 3x3 zoning + Euler hole topology + aspect geometry,
 *    and formats valid rows as `Day - Price1+Price2`.
 */
private fun analyzeHandwrittenKhataBitmap(bitmap: Bitmap): List<String> {
    val procBmp = resizeBitmapSafely(bitmap, 720)
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

    val winR = max(12, min(w, h) / 24)
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
            ink[y * w + x] = lum[y * w + x] < (mean - 22)
        }
    }

    // Remove notebook horizontal ruled lines
    val marginX = (w * 0.04f).toInt()
    for (y in 0 until h) {
        var rowCount = 0
        for (x in marginX until (w - marginX)) {
            if (ink[y * w + x]) rowCount++
        }
        if (rowCount > (w - 2 * marginX) * 0.52f) {
            for (x in 0 until w) ink[y * w + x] = false
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

    val minRowInk = max(4, (w * 0.012f).toInt())
    val minBandHeight = max(10, (h * 0.016f).toInt())
    val maxBandHeight = max(28, (h * 0.14f).toInt())

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
    val usedDays = mutableSetOf<Int>()

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
        val maxInternalGap = max(2, bh / 9)
        for (x in marginX until (w - marginX)) {
            if (colInk[x] >= 1) {
                if (cStart == -1) cStart = x
                cGap = 0
            } else if (cStart != -1) {
                cGap++
                if (cGap > maxInternalGap) {
                    val cEnd = x - cGap
                    val cw = cEnd - cStart + 1
                    if (cw >= 3) charSpans.add(cStart..cEnd)
                    cStart = -1
                    cGap = 0
                }
            }
        }
        if (cStart != -1) {
            val cEnd = (w - marginX - 1)
            if (cEnd - cStart + 1 >= 3) charSpans.add(cStart..cEnd)
        }

        if (charSpans.size < 2) continue

        data class GlyphBox(val x0: Int, val y0: Int, val x1: Int, val y1: Int)
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
            if (pixelCount >= 5 && gh >= 4 && gw <= bh * 2.2f) {
                if (gw > gh * 1.35f && gw >= 14) {
                    var minColX = (span.first + span.last) / 2
                    var minColVal = Int.MAX_VALUE
                    for (sx in (span.first + gw / 3)..(span.last - gw / 3)) {
                        if (colInk[sx] < minColVal) {
                            minColVal = colInk[sx]
                            minColX = sx
                        }
                    }
                    boxes.add(GlyphBox(span.first, yMin, minColX, yMax))
                    boxes.add(GlyphBox(minColX + 1, yMin, span.last, yMax))
                } else {
                    boxes.add(GlyphBox(span.first, yMin, span.last, yMax))
                }
            }
        }

        if (boxes.size < 2) continue

        var splitIndex = -1
        var maxGapWidth = -1
        for (i in 0 until min(3, boxes.size - 1)) {
            val gap = boxes[i + 1].x0 - boxes[i].x1
            if (gap > maxGapWidth) {
                maxGapWidth = gap
                splitIndex = i
            }
        }
        if (splitIndex == -1) continue

        val leftBoxes = boxes.subList(0, splitIndex + 1)
        val rightBoxes = boxes.subList(splitIndex + 1, boxes.size)

        val dayDigits = StringBuilder()
        for (b in leftBoxes) {
            val ch = classifyGlyph(ink, w, b.x0, b.y0, b.x1, b.y1, bh, allowOperators = false)
            if (ch in '0'..'9') dayDigits.append(ch)
        }
        var dayVal = dayDigits.toString().toIntOrNull()
        if (dayVal != null && dayVal > 31 && dayDigits.length >= 2) {
            dayVal = dayDigits.substring(0, 2).toIntOrNull()
            if (dayVal != null && dayVal > 31) {
                dayVal = dayDigits.substring(0, 1).toIntOrNull()
            }
        }
        if (dayVal == null || dayVal !in 1..31 || dayVal in usedDays) continue

        val rightTokens = StringBuilder()
        var prevX1 = rightBoxes.first().x0
        for ((idx, b) in rightBoxes.withIndex()) {
            val gap = b.x0 - prevX1
            prevX1 = b.x1
            val ch = classifyGlyph(ink, w, b.x0, b.y0, b.x1, b.y1, bh, allowOperators = true)
            if (ch == '-') continue
            if (ch == '+') {
                if (rightTokens.isNotEmpty() && rightTokens.last() != '+') {
                    rightTokens.append('+')
                }
            } else if (ch in '0'..'9') {
                if (idx > 0 && gap > bh * 0.85f && rightTokens.isNotEmpty() && rightTokens.last() != '+') {
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
            usedDays.add(dayVal)
            results.add("$dayVal - ${cleanedPrices.joinToString("+")}")
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
    allowOperators: Boolean
): Char {
    val gw = (x1 - x0 + 1).coerceAtLeast(1)
    val gh = (y1 - y0 + 1).coerceAtLeast(1)
    val aspect = gw.toFloat() / gh.toFloat()

    if (allowOperators && gh <= bandHeight * 0.35f && aspect >= 1.4f) {
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

    if (allowOperators && gh <= bandHeight * 0.78f && aspect in 0.65f..1.45f) {
        val crossSum = z[1] + z[3] + z[4] + z[5] + z[7]
        val cornerSum = z[0] + z[2] + z[6] + z[8]
        if (z[4] > 0.35f && crossSum > cornerSum * 2.1f) {
            return '+'
        }
    }

    if (aspect < 0.42f || (gw <= 5 && gh >= 10)) {
        return '1'
    }

    val holes = countEnclosedHoles(ink, stride, x0, y0, x1, y1)

    if (holes >= 2) return '8'

    if (holes == 1) {
        val topInk = z[0] + z[1] + z[2]
        val botInk = z[6] + z[7] + z[8]
        val midCenter = z[4]
        return when {
            midCenter < 0.22f && abs(topInk - botInk) < 0.45f -> '0'
            topInk > botInk + 0.25f -> '9'
            botInk > topInk + 0.25f -> '6'
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
        topBar > 0.35f && botBar < 0.28f && z[3] < 0.22f -> '7'
        z[3] + z[4] + z[5] > 1.05f && botBar < 0.32f && z[0] > 0.25f -> '4'
        leftTop > rightTop + 0.18f && rightBot > leftBot -> '5'
        rightTop > leftTop && leftBot > rightBot * 0.85f && botBar > 0.35f -> '2'
        (rightTop + rightBot) > (leftTop + leftBot) * 1.25f -> '3'
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
 * Parses lines like:
 * 17 - 60+40
 * 18: 50
 * 19 -> 30 + 20
 * Each '+' separated price is treated as a separate dish token and matched against the hotel's menu.
 */
fun parseKhataRawText(
    rawText: String,
    year: Int,
    month: Int,
    hotelId: Int,
    hotelMenus: List<MenuEntity>,
    existingTransactions: List<HotelTransactionEntity>
): List<KhataDayEntry> {
    val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val entries = mutableListOf<KhataDayEntry>()

    val hotelTx = existingTransactions.filter { it.hotelId == hotelId && !it.isDeleted }

    val lineRegex = Regex("""^(\d{1,2})\s*[-:=–>|\s]\s*(.+)$""")

    for (line in lines) {
        val match = lineRegex.find(line) ?: continue
        val day = match.groupValues[1].toIntOrNull() ?: continue
        if (day !in 1..31) continue

        val pricesExpr = match.groupValues[2]
        val priceTokens = pricesExpr.split("+")
            .map { it.trim() }
            .mapNotNull { token ->
                Regex("""(\d+(\.\d+)?)""").find(token)?.groupValues?.get(1)?.toDoubleOrNull()
            }
            .filter { it > 0.0 }

        if (priceTokens.isEmpty()) continue

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, day.coerceAtMost(maxDay))
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val ts = cal.timeInMillis

        val hasExisting = hotelTx.any { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.YEAR) == year &&
                txCal.get(Calendar.MONTH) == month &&
                txCal.get(Calendar.DAY_OF_MONTH) == day
        }

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
                timestamp = ts,
                hasExistingData = hasExisting,
                segments = segments,
                includeInImport = !hasExisting
            )
        )
    }

    return entries.sortedBy { it.dayOfMonth }
}

/**
 * Finds possible menu combinations for a single price token (e.g. ₹60 -> 1x ₹60 item, or ₹40 + ₹20, or 5x ₹12).
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

    // 2. Exact multiple of a single item (e.g., 12 x 5 = 60)
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
        for (j in i until validMenus.size) {
            val m1 = validMenus[i]
            val m2 = validMenus[j]
            if (i != j && abs((m1.price + m2.price) - targetPrice) < 0.01) {
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

    return results.distinctBy { it.description }.take(8)
}
