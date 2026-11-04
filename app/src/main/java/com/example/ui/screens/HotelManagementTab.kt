@file:Suppress("DEPRECATION")
package com.example.ui.screens
import com.example.utils.Formatters
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
// MODULE A: HOTEL MANAGEMENT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelManagementTab(viewModel: FinancialViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val pdfExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val hotel = viewModel.activeHotel.value
            if (hotel != null) {
                try {
                    val txs = viewModel.activeHotelTransactions.value
                    val cal = java.util.Calendar.getInstance()
                    val currentMonth = cal.get(java.util.Calendar.MONTH)
                    val currentYear = cal.get(java.util.Calendar.YEAR)
                    
                    val monthTxs = txs.filter { 
                        val tCal = java.util.Calendar.getInstance()
                        tCal.timeInMillis = it.timestamp
                        tCal.get(java.util.Calendar.MONTH) == currentMonth && tCal.get(java.util.Calendar.YEAR) == currentYear
                    }
                    
                    val pdfDocument = android.graphics.pdf.PdfDocument()
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    val paint = android.graphics.Paint().apply { textSize = 16f; color = android.graphics.Color.BLACK }
                    val boldPaint = android.graphics.Paint().apply { textSize = 20f; color = android.graphics.Color.BLACK; isFakeBoldText = true }
                    
                    canvas.drawText("Transactions - ${hotel.name}", 20f, 40f, boldPaint)
                    var y = 80f
                    
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                    for (t in monthTxs) {
                        val dateStr = sdf.format(java.util.Date(t.timestamp))
                        val typeStr = if (t.isNight) "Night" else "Day"
                        val amt = "Rs.${t.totalAmount}"
                        canvas.drawText("$dateStr | $typeStr | $amt", 20f, y, paint)
                        y += 25f
                        if (y > 800f) break
                    }
                    pdfDocument.finishPage(page)
                    
                    context.contentResolver.openOutputStream(uri)?.use { fos ->
                        pdfDocument.writeTo(fos)
                    }
                    pdfDocument.close()
                    android.widget.Toast.makeText(context, "PDF Exported Successfully", android.widget.Toast.LENGTH_SHORT).show()
                } catch(e: Exception) {
                    android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val hotels by viewModel.allHotels.collectAsStateWithLifecycle()
    val activeHotelId by viewModel.activeHotelId.collectAsStateWithLifecycle()
    val activeHotel by viewModel.activeHotel.collectAsStateWithLifecycle()
    val monthlyTotal by viewModel.activeMonthTotal.collectAsStateWithLifecycle()
    val popularMeals by viewModel.popularMeals.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val transactions by viewModel.activeHotelTransactions.collectAsStateWithLifecycle()

    val currentMonthTransactions = remember(transactions) {
        val cal = java.util.Calendar.getInstance()
        val currentMonth = cal.get(java.util.Calendar.MONTH)
        val currentYear = cal.get(java.util.Calendar.YEAR)
        transactions.filter {
            val txCal = java.util.Calendar.getInstance()
            txCal.timeInMillis = it.timestamp
            txCal.get(java.util.Calendar.MONTH) == currentMonth && txCal.get(java.util.Calendar.YEAR) == currentYear
        }
    }

    var openDropdownHotelId by remember { mutableStateOf<Int?>(null) }
    var hotelMenuOffsets by remember { mutableStateOf(mapOf<Int, androidx.compose.ui.geometry.Offset>()) }
    var showMealHistory by remember { mutableStateOf(false) }
    
    
    var showQrScanner by remember { mutableStateOf(false) }

    val quickActionTrigger by viewModel.quickActionTrigger.collectAsStateWithLifecycle()
    LaunchedEffect(quickActionTrigger) {
        if (quickActionTrigger == "ADD_MEAL_LOG") {
            viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.AddMeal)
            viewModel.clearQuickActionTrigger()
        } else if (quickActionTrigger == "ADD_HOTEL") {
            viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.EditHotel(null))
            viewModel.clearQuickActionTrigger()
        }
    }

    // Dialog edit fields
    
    var showInfoDialog by remember { mutableStateOf<com.example.data.model.HotelEntity?>(null) }
    var showContactPopupHotel by remember { mutableStateOf<com.example.data.model.HotelEntity?>(null) }
    

    LaunchedEffect(showMealHistory) {
        viewModel.registerSubScreen("MealHistory", showMealHistory)
    }
    var editHotelName by remember { mutableStateOf("") }
    var editHotelAdvanced by remember { mutableStateOf("") }

    val isBengali = userProfile.language == "bn"

    if (hotels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTextColor().copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, getGlassBorder().copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AppAccentColor().copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddBusiness,
                            contentDescription = null,
                            tint = AppAccentColor(),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isBengali) "কোনো হোটেল যুক্ত করা হয়নি" else "No Hotels Added Yet",
                        color = AppTextColor(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isBengali) "খাবার লগ, মেনু এবং খরচ ট্র্যাক করা শুরু করতে একটি নতুন হোটেল যোগ করুন।" else "Add a new hotel to start tracking meal logs, menus, and expenses.",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.EditHotel(null)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppAccentColor(),
                            contentColor = AppOnAccentColor()
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick()
                            .testTag("add_first_hotel_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AppOnAccentColor())
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBengali) "+ নতুন হোটেল যোগ করুন" else "+ Add New Hotel",
                            color = AppOnAccentColor(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showQrScanner = true },
                        border = BorderStroke(1.dp, AppAccentColor().copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .bounceClick()
                            .testTag("import_qr_hotel_btn")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = AppAccentColor())
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBengali) "কিউআর মাধ্যমে ইমপোর্ট করুন" else "Import via QR",
                            color = AppAccentColor(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    } else {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            // WIDESCREEN TABLET LAYOUT: Two columns side-by-side
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Ledger Card & Popular Meals Carousel
                LazyColumn(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 156.dp)
                ) {
                    item {
                        // PRIMARY GLASS LEDGER CARD
                        if (hotels.isNotEmpty()) {
                            val initialPage = hotels.indexOfFirst { it.id == activeHotelId }.coerceAtLeast(0)
                            val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                                initialPage = initialPage,
                                pageCount = { hotels.size }
                            )
                            
                            val view = androidx.compose.ui.platform.LocalView.current
                            LaunchedEffect(pagerState, hotels) {
                                androidx.compose.runtime.snapshotFlow { pagerState.currentPage }.collect { page ->
                                    if (page in hotels.indices) {
                                        if (viewModel.activeHotelId.value != hotels[page].id) {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                            viewModel.activeHotelId.value = hotels[page].id
                                        }
                                    }
                                }
                            }
                              
                            LaunchedEffect(activeHotelId, hotels) {
                                val index = hotels.indexOfFirst { it.id == activeHotelId }
                                if (index >= 0 && index != pagerState.currentPage) {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                            
                            androidx.compose.foundation.pager.HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth(),
                                pageSpacing = 16.dp,
                                contentPadding = PaddingValues(horizontal = 0.dp),
                                userScrollEnabled = hotels.size > 1
                            ) { page ->
                                val hotel = hotels[page]
                                val disableHeavy = isHeavyEffectsDisabled()
                                val pageModifier = if (disableHeavy) {
                                    Modifier
                                } else {
                                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                    val scale = 1f - (0.05f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                                    val alpha = 1f - (0.3f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                                    Modifier.graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        this.alpha = alpha
                                    }
                                }
                                Box(modifier = pageModifier) {
                                    GlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                                                    Text(
                                                        text = hotel.name,
                                                        color = AppTextColor(),
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Phone,
                                                        contentDescription = "Call or WhatsApp",
                                                        tint = AppTextColor(),
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .padding(end = 4.dp)
                                                            .clickable { showContactPopupHotel = hotel }
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = "Info",
                                                        tint = AppTextColor(),
                                                        modifier = Modifier.size(24.dp).padding(end = 4.dp).clickable { showInfoDialog = hotel }
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                                        Icon(
                                                            imageVector = Icons.Default.MoreVert,
                                                            contentDescription = "More",
                                                            tint = AppTextColor(),
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clickable { openDropdownHotelId = hotel.id }
                                                        )
                                                        DropdownMenu(
                                                            expanded = openDropdownHotelId == hotel.id,
                                                            onDismissRequest = { openDropdownHotelId = null },
                                                            offset = androidx.compose.ui.unit.DpOffset(x = (-12).dp, y = 4.dp),
                                                            modifier = Modifier.background(getDialogBackground())
                                                        ) {
                                                            DropdownMenuItem(
                                                                text = { Text(if (isBengali) "হোটেল তথ্য সংশোধন" else "Edit Hotel Details", color = AppTextColor(), fontWeight = FontWeight.Medium) },
                                                                onClick = {
                                                                    viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.EditHotel(hotel))
                                                                    openDropdownHotelId = null
                                                                }
                                                            )
                                                            val isDefaultHotel = userProfile.defaultHotelId == hotel.id
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(
                                                                        if (isDefaultHotel) (if (isBengali) "ডিফল্ট হোটেল (নির্বাচিত)" else "Default Hotel (Active)")
                                                                        else (if (isBengali) "ডিফল্ট হোটেল হিসেবে সেট করুন" else "Set as Default Hotel"),
                                                                        color = if (isDefaultHotel) AppAccentColor() else AppTextColor(),
                                                                        fontWeight = if (isDefaultHotel) FontWeight.Bold else FontWeight.Medium
                                                                    )
                                                                },
                                                                onClick = {
                                                                    viewModel.setDefaultHotel(hotel.id)
                                                                    openDropdownHotelId = null
                                                                    android.widget.Toast.makeText(context, if (isBengali) "ডিফল্ট হোটেল সেট করা হয়েছে" else "Default Hotel Updated", android.widget.Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text(if (isBengali) "মিলের ইতিহাস দেখুন" else "View Meal History", color = AppTextColor(), fontWeight = FontWeight.Medium) },
                                                                onClick = {
                                                                    openDropdownHotelId = null
                                                                    showMealHistory = true
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("Export Transactions (PDF)", color = AppTextColor(), fontWeight = FontWeight.Medium) },
                                                                onClick = {
                                                                    pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                                    openDropdownHotelId = null
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text(if (isBengali) "কিউআর মাধ্যমে এক্সপোর্ট" else "Export via QR", color = AppAccentColor(), fontWeight = FontWeight.Medium) },
                                                                onClick = {
                                                                    openDropdownHotelId = null
                                                                    viewModel.exportHotelViaQr(hotel.id) { result ->
                                                                        if (result is com.example.data.ExportResult.Success) {
                                                                            viewModel.setQrExportPayload(result, java.util.UUID.randomUUID().toString(), "${hotel.name} hotel's menu")
                                                                        } else {
                                                                            android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "monthly expenditure",
                                                    color = AppTextColor().copy(alpha = 0.9f),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    RollingNumberText(
                                                        value = if (hotel.id == activeHotelId) monthlyTotal else 0.0,
                                                        style = TextStyle(
                                                            color = AppTextColor(),
                                                            fontSize = 30.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontFamily = FontFamily.SansSerif
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            EmptyStateCard(message = "No Hotels configured. Create one in Settings!")
                        }
                    }

                    // TABLET LEFT COLUMN: HOTEL INFORMATION PANEL (Instead of Blank Space)
                    activeHotel?.let { currentHotel ->
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Apartment,
                                                contentDescription = null,
                                                tint = AppAccentColor(),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isBengali) "হোটেলের তথ্য ও বিবরণ" else "Hotel Information",
                                                color = AppHeadingColor(),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        TextButton(
                                            onClick = { viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.EditHotel(currentHotel)) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isBengali) "সম্পাদনা" else "Edit", color = AppAccentColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    HorizontalDivider(color = getGlassBorder().copy(alpha = 0.4f))

                                    HotelInfoDetailRow(
                                        icon = Icons.Default.AccountBalanceWallet,
                                        iconTint = AppAccentColor(),
                                        label = if (isBengali) "অগ্রিম জমা" else "Advanced Payment",
                                        value = "₹${currentHotel.advancedPayment.toInt()}"
                                    )

                                    if (currentHotel.phoneNumber.isNotBlank() || currentHotel.whatsappNumber.isNotBlank()) {
                                        HotelInfoDetailRow(
                                            icon = Icons.Default.Phone,
                                            iconTint = Color(0xFF4CAF50),
                                            label = if (isBengali) "যোগাযোগ / হোয়াটসঅ্যাপ" else "Phone / WhatsApp",
                                            value = listOfNotNull(
                                                currentHotel.phoneNumber.takeIf { it.isNotBlank() },
                                                currentHotel.whatsappNumber.takeIf { it.isNotBlank() && it != currentHotel.phoneNumber }?.let { "WA: $it" }
                                            ).joinToString(" • "),
                                            actionIcon = Icons.Default.Call,
                                            onAction = { showContactPopupHotel = currentHotel }
                                        )
                                    }

                                    if (currentHotel.upiId.isNotBlank()) {
                                        HotelInfoDetailRow(
                                            icon = Icons.Default.QrCode,
                                            iconTint = Color(0xFFBA68C8),
                                            label = "UPI ID",
                                            value = currentHotel.upiId,
                                            actionIcon = Icons.Default.Info,
                                            onAction = { showInfoDialog = currentHotel }
                                        )
                                    }

                                    if (currentHotel.location.isNotBlank()) {
                                        HotelInfoDetailRow(
                                            icon = Icons.Default.LocationOn,
                                            iconTint = Color(0xFFFFB74D),
                                            label = if (isBengali) "অবস্থান" else "Location",
                                            value = currentHotel.location
                                        )
                                    }

                                    if (currentHotel.notes.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = AppAccentColor().copy(alpha = 0.08f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = if (isBengali) "সংক্ষিপ্ত নোট" else "Short Notes",
                                                    color = AppAccentColor(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = currentHotel.notes,
                                                    color = AppTextColor(),
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Frequently Ordered Meals",
                            color = AppTextColor(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        if (popularMeals.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text("No frequently ordered meals yet. Add some meals to see insights!", color = AppTextColor().copy(alpha = 0.5f), fontSize = 14.sp)
                            }
                        } else {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(popularMeals, key = { it.first }, contentType = { "popular_meal" }) { (name, count) ->
                                    PresetMealCard(
                                        title = name,
                                        description = "Frequently ordered dish at this establishment",
                                        tag = "$count times",
                                        color = AppAccentColor()
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Right Column: Recent Meal Logs & view history button
                LazyColumn(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 156.dp)
                ) {
                    item {
                        Text(
                            text = "Recent Meal Logs",
                            color = AppTextColor(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        if (transactions.isEmpty()) {
                            EmptyStateCard(message = "No meal logs registered yet.")
                        } else {
                            val displayCount = if (transactions.size >= 7) 6 else transactions.size
                            val itemsToDisplay = transactions.take(displayCount)

                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsToDisplay.forEach { tx ->
                                    val groups = com.example.data.model.TransactionSerializer.deserializeGroups(tx.itemsSerialized)
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (tx.isNight) Icons.Default.NightsStay else Icons.Default.WbSunny,
                                                        contentDescription = null,
                                                        tint = if (tx.isNight) AppAccentColor() else AccentGold,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = com.example.utils.Formatters.formatShortDate(tx.timestamp),
                                                        color = AppTextColor(),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                
                                                if (groups.size > 1) {
                                                    val mathFormula = groups.joinToString(" + ") { "₹${it.subtotal.toInt()}" } + " = ₹${tx.totalAmount.toInt()}"
                                                    Text(
                                                        text = mathFormula,
                                                        color = AccentGreen,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    groups.forEachIndexed { idx, grp ->
                                                        val grpSummary = grp.items.joinToString(", ") { "${it.name} x${it.quantity}" }
                                                        Text(
                                                            text = "${idx + 1}. $grpSummary (₹${grp.subtotal.toInt()})",
                                                            color = AppTextColor().copy(alpha = 0.65f),
                                                            fontSize = 11.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                } else {
                                                    val items = com.example.data.model.TransactionSerializer.deserialize(tx.itemsSerialized)
                                                    val summary = items.joinToString(", ") { "${it.name} x${it.quantity}" }
                                                    Text(
                                                        text = summary,
                                                        color = AppTextColor().copy(alpha = 0.65f),
                                                        fontSize = 11.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "₹${tx.totalAmount.toInt()}",
                                                color = AccentGreen,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (transactions.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            activeHotel?.let {
                                GlassButton(
                                    onClick = { showMealHistory = true },
                                    modifier = Modifier.fillMaxWidth().testTag("view_meal_history_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "History",
                                        tint = AppAccentColor()
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (userProfile.language == "bn") "মিলের ইতিহাস দেখুন" else "View Meal History",
                                        color = AppTextColor(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // REGULAR MOBILE VERTICAL SCREEN LAYOUT
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    // PRIMARY GLASS LEDGER CARD
                    if (hotels.isNotEmpty()) {
                        val initialPage = hotels.indexOfFirst { it.id == activeHotelId }.coerceAtLeast(0)
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                            initialPage = initialPage,
                            pageCount = { hotels.size }
                        )
                        
                        val view = androidx.compose.ui.platform.LocalView.current
                        LaunchedEffect(pagerState, hotels) {
                            androidx.compose.runtime.snapshotFlow { pagerState.currentPage }.collect { page ->
                                if (page in hotels.indices) {
                                    if (viewModel.activeHotelId.value != hotels[page].id) {
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                        viewModel.activeHotelId.value = hotels[page].id
                                    }
                                }
                            }
                        }
                          
                        LaunchedEffect(activeHotelId, hotels) {
                            val index = hotels.indexOfFirst { it.id == activeHotelId }
                            if (index >= 0 && index != pagerState.currentPage) {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                        
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth(),
                            pageSpacing = 16.dp,
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            userScrollEnabled = hotels.size > 1
                        ) { page ->
                            val hotel = hotels[page]
                            val disableHeavy = isHeavyEffectsDisabled()
                            val pageModifier = if (disableHeavy) {
                                Modifier
                            } else {
                                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                val scale = 1f - (0.05f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                                val alpha = 1f - (0.3f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                                Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                            }
                            Box(modifier = pageModifier) {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                                                Text(
                                                    text = hotel.name,
                                                    color = AppTextColor(),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Phone,
                                                    contentDescription = "Call or WhatsApp",
                                                    tint = AppTextColor(),
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .padding(end = 4.dp)
                                                        .clickable { showContactPopupHotel = hotel }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "Info",
                                                    tint = AppTextColor(),
                                                    modifier = Modifier.size(24.dp).padding(end = 4.dp).clickable { showInfoDialog = hotel }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                                    Icon(
                                                        imageVector = Icons.Default.MoreVert,
                                                        contentDescription = "More",
                                                        tint = AppTextColor(),
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clickable { openDropdownHotelId = hotel.id }
                                                    )
                                                    DropdownMenu(
                                                        expanded = openDropdownHotelId == hotel.id,
                                                        onDismissRequest = { openDropdownHotelId = null },
                                                        offset = androidx.compose.ui.unit.DpOffset(x = (-12).dp, y = 4.dp),
                                                        modifier = Modifier.background(getDialogBackground())
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text(if (isBengali) "হোটেল তথ্য সংশোধন" else "Edit Hotel Details", color = AppTextColor(), fontWeight = FontWeight.Medium) },
                                                            onClick = {
                                                                viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.EditHotel(hotel))
                                                                openDropdownHotelId = null
                                                            }
                                                        )
                                                        val isDefaultHotel = userProfile.defaultHotelId == hotel.id
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    if (isDefaultHotel) (if (isBengali) "ডিফল্ট হোটেল (নির্বাচিত)" else "Default Hotel (Active)")
                                                                    else (if (isBengali) "ডিফল্ট হোটেল হিসেবে সেট করুন" else "Set as Default Hotel"),
                                                                    color = if (isDefaultHotel) AppAccentColor() else AppTextColor(),
                                                                    fontWeight = if (isDefaultHotel) FontWeight.Bold else FontWeight.Medium
                                                                )
                                                            },
                                                            onClick = {
                                                                viewModel.setDefaultHotel(hotel.id)
                                                                openDropdownHotelId = null
                                                                android.widget.Toast.makeText(context, if (isBengali) "ডিফল্ট হোটেল সেট করা হয়েছে" else "Default Hotel Updated", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text(if (isBengali) "মিলের ইতিহাস দেখুন" else "View Meal History", color = AppTextColor(), fontWeight = FontWeight.Medium) },
                                                            onClick = {
                                                                openDropdownHotelId = null
                                                                showMealHistory = true
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Export Transactions (PDF)", color = AppTextColor(), fontWeight = FontWeight.Medium) },
                                                            onClick = {
                                                                pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                                openDropdownHotelId = null
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text(if (isBengali) "কিউআর মাধ্যমে এক্সপোর্ট" else "Export via QR", color = AppAccentColor(), fontWeight = FontWeight.Medium) },
                                                            onClick = {
                                                                openDropdownHotelId = null
                                                                viewModel.exportHotelViaQr(hotel.id) { result ->
                                                                    if (result is com.example.data.ExportResult.Success) {
                                                                        viewModel.setQrExportPayload(result, java.util.UUID.randomUUID().toString(), "${hotel.name} hotel's menu")
                                                                    } else {
                                                                        android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "monthly expenditure",
                                                color = AppTextColor().copy(alpha = 0.9f),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Row(verticalAlignment = Alignment.Bottom) {
                                                RollingNumberText(
                                                    value = if (hotel.id == activeHotelId) monthlyTotal else 0.0,
                                                    style = TextStyle(
                                                        color = AppTextColor(),
                                                        fontSize = 30.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontFamily = FontFamily.SansSerif
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty state if no hotel selected/available
                        EmptyStateCard(message = "No Hotels configured. Create one in Settings!")
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // BEHAVIORAL ANALYTICS CAROUSEL HEADER
                    Text(
                        text = "Frequently Ordered Meals",
                        color = AppHeadingColor(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    if (popularMeals.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("No frequently ordered meals yet. Add some meals to see insights!", color = AppTextColor().copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(popularMeals, key = { it.first }, contentType = { "popular_meal" }) { (name, count) ->
                                PresetMealCard(
                                    title = name,
                                    description = "Frequently ordered dish at this establishment",
                                    tag = "$count times",
                                    color = AppAccentColor()
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Recent Meal Logs",
                        color = AppHeadingColor(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    if (transactions.isEmpty()) {
                        EmptyStateCard(message = "No meal logs registered yet.")
                    } else {
                        val displayCount = if (transactions.size >= 5) 4 else transactions.size
                        val itemsToDisplay = transactions.take(displayCount)

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsToDisplay.forEach { tx ->
                                val groups = com.example.data.model.TransactionSerializer.deserializeGroups(tx.itemsSerialized)
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (tx.isNight) Icons.Default.NightsStay else Icons.Default.WbSunny,
                                                    contentDescription = null,
                                                    tint = if (tx.isNight) AppAccentColor() else AccentGold,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = com.example.utils.Formatters.formatShortDate(tx.timestamp),
                                                    color = AppTextColor(),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            if (groups.size > 1) {
                                                val mathFormula = groups.joinToString(" + ") { "₹${it.subtotal.toInt()}" } + " = ₹${tx.totalAmount.toInt()}"
                                                Text(
                                                    text = mathFormula,
                                                    color = AccentGreen,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                groups.forEachIndexed { idx, grp ->
                                                    val grpSummary = grp.items.joinToString(", ") { "${it.name} x${it.quantity}" }
                                                    Text(
                                                        text = "${idx + 1}. $grpSummary (₹${grp.subtotal.toInt()})",
                                                        color = AppTextColor().copy(alpha = 0.65f),
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            } else {
                                                val items = com.example.data.model.TransactionSerializer.deserialize(tx.itemsSerialized)
                                                val summary = items.joinToString(", ") { "${it.name} x${it.quantity}" }
                                                Text(
                                                    text = summary,
                                                    color = AppTextColor().copy(alpha = 0.65f),
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Text(
                                            text = "₹${tx.totalAmount.toInt()}",
                                            color = AccentGreen,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (transactions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))

                        // VIEW MEAL HISTORY TRIGGER BUTTON
                        activeHotel?.let {
                            GlassButton(
                                onClick = { showMealHistory = true },
                                modifier = Modifier.fillMaxWidth().testTag("view_meal_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    tint = AppAccentColor()
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (userProfile.language == "bn") "মিলের ইতিহাস দেখুন" else "View Meal History",
                                    color = AppTextColor(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(156.dp)) // Padding for bottom bar and FAB
                }
            }
        }

        // FAB FOR ADDING NEW MEAL
        if (activeHotel != null) {
            FloatingActionButton(
                onClick = {
                    viewModel.resetOrderFlow()
                    viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.AddMeal)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 148.dp, end = 16.dp)
                    .bounceClick()
                    .testTag("add_meal_fab"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Meal",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

    

    // Modal Dialog for Meal History
    
    if (showContactPopupHotel != null) {
        HotelContactOptionsDialog(
            hotel = showContactPopupHotel!!,
            isBengali = isBengali,
            onEditHotel = {
                val target = showContactPopupHotel
                showContactPopupHotel = null
                viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.EditHotel(target))
            },
            onDismiss = { showContactPopupHotel = null }
        )
    }

    if (showInfoDialog != null) {
        HotelDetailsBottomSheet(
            hotel = showInfoDialog!!,
            isBengali = isBengali,
            viewModel = viewModel,
            onDismiss = { showInfoDialog = null }
        )
    }

    if (showMealHistory) {
        MealHistoryModal(
            viewModel = viewModel,
            onDismiss = { showMealHistory = false },
            onEditTransaction = { tx ->
                viewModel.loadTransactionForEditing(tx)
                showMealHistory = false
                viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.AddMeal)
            }
        )
    }


    val exportPayload by viewModel.qrExportPayload.collectAsStateWithLifecycle()
    val exportSessionId by viewModel.qrExportSessionId.collectAsStateWithLifecycle()
    val exportItemName by viewModel.qrExportItemName.collectAsStateWithLifecycle()

    if (exportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = exportPayload!!,
            sessionId = exportSessionId ?: "",
            itemName = exportItemName ?: "Hotel Data",
            onDismiss = { viewModel.setQrExportPayload(null, null, null) }
        )
    }

    if (showQrScanner) {
        val context = androidx.compose.ui.platform.LocalContext.current
        com.example.ui.components.QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onSuccess = { payload: ByteArray, key: ByteArray, iv: ByteArray ->
                showQrScanner = false
                viewModel.importHotelViaQr(payload, key, iv) { result ->
                    when (result) {
                        is com.example.data.ImportResult.Success -> {
                            android.widget.Toast.makeText(context, "Hotel imported successfully!", android.widget.Toast.LENGTH_LONG).show()
                        }
                        is com.example.data.ImportResult.Error -> {
                            android.widget.Toast.makeText(context, "Import failed: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }
}


@Composable
fun PresetMealCard(
    title: String,
    description: String,
    tag: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(110.dp)
    ) {
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            color = color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        color = AppTextColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = description,
                    color = AppTextColor().copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}




// ==========================================
// CHRONOLOGICAL VIEW MEAL HISTORY MODAL
// ==========================================
@Composable
fun MealHistoryModal(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit,
    onEditTransaction: ((com.example.data.model.HotelTransactionEntity) -> Unit)? = null
) {
    BackHandler { onDismiss() }

    val transactions by viewModel.activeHotelTransactions.collectAsStateWithLifecycle()
    val earliestTs by viewModel.earliestTransactionTimestamp.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val allHotelPayments by viewModel.allHotelPayments.collectAsStateWithLifecycle()
    val activeHotelId by viewModel.activeHotelId.collectAsStateWithLifecycle()
    val activeHotel by viewModel.activeHotel.collectAsStateWithLifecycle()
    val sdf = remember { Formatters.getSimpleDateFormat("dd MMM, hh:mm a") }
    val monthFormat = remember { Formatters.getSimpleDateFormat("MMMM yyyy") }
    val paymentDateFormat = remember { Formatters.getSimpleDateFormat("dd MMM yyyy") }
    
    var monthOffset by remember { mutableStateOf(0) }
    var isDayWiseView by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    
    val (startOfMonthMillis, endOfMonthMillis) = remember(monthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        Pair(start, end)
    }

    val filteredTransactions = remember(transactions, startOfMonthMillis, endOfMonthMillis) {
        transactions.filter { it.timestamp in startOfMonthMillis..endOfMonthMillis }
    }
    
    val currentDisplayMonth = remember(monthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        monthFormat.format(cal.time)
    }

    val targetMonthCal = remember(monthOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }
    }
    val targetMonth = targetMonthCal.get(Calendar.MONTH)
    val targetYear = targetMonthCal.get(Calendar.YEAR)

    val currentMonthPayment = remember(allHotelPayments, activeHotelId, targetYear, targetMonth) {
        allHotelPayments.find {
            it.hotelId == activeHotelId && it.year == targetYear && it.month == targetMonth && !it.isDeleted
        }
    }

    val isEarliestMonth = remember(monthOffset, earliestTs) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        
        val eCal = Calendar.getInstance().apply { timeInMillis = earliestTs }
        val earliestYear = eCal.get(Calendar.YEAR)
        val earliestMonth = eCal.get(Calendar.MONTH)
        
        (currentYear < earliestYear) || (currentYear == earliestYear && currentMonth <= earliestMonth)
    }

    val currentMonthOffset by rememberUpdatedState(monthOffset)
    val currentIsEarliestMonth by rememberUpdatedState(isEarliestMonth)

    var totalDragAmount by remember { mutableStateOf(0f) }

    androidx.activity.compose.BackHandler(onBack = onDismiss)

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
            color = if (isAppLightMode()) com.example.ui.theme.M3BackgroundLight else com.example.ui.theme.M3BackgroundDark
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getAppBackgroundGradient(userProfile?.themeMode ?: "Auto"))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragAmount = 0f },
                            onDragEnd = {
                                val swipeThreshold = 40.dp.toPx()
                                if (totalDragAmount > swipeThreshold) {
                                    if (!currentIsEarliestMonth) {
                                        monthOffset -= 1
                                    }
                                } else if (totalDragAmount < -swipeThreshold) {
                                    if (currentMonthOffset < 0) {
                                        monthOffset += 1
                                    }
                                }
                                totalDragAmount = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDragAmount += dragAmount
                            }
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp)
                        .dialogSystemBarsPadding(extraTop = 8.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                // Header Bar (Separated from Month Selector to prevent accidental touches)
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = AppTextColor(), modifier = Modifier.size(24.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (userProfile?.language == "bn") "মিলের ইতিহাস লেজার" else "Meal Record Ledger",
                            color = AppTextColor(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeHotel != null) {
                            Text(
                                text = activeHotel!!.name,
                                color = AppAccentColor(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Month Navigation Card (Clearly separated with generous padding away from Top-Left Back Button)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = getCardBackground(),
                    shadowElevation = if (isAppLightMode()) 2.dp else 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = { monthOffset -= 1 },
                            enabled = !isEarliestMonth,
                            modifier = Modifier.size(38.dp).bounceClick(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = AppAccentColor().copy(alpha = 0.12f),
                                contentColor = AppAccentColor()
                            )
                        ) {
                            Icon(Icons.Default.ChevronLeft, "Previous Month", tint = if (!isEarliestMonth) AppAccentColor() else AppTextColor().copy(alpha = 0.3f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(currentDisplayMonth, color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = if (userProfile?.language == "bn") "মাস পরিবর্তনের জন্য বামে/ডানে সোয়াইপ করুন" else "Swipe left or right to switch months",
                                color = AppSecondaryTextColor(),
                                fontSize = 10.sp
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { monthOffset += 1 },
                            enabled = monthOffset < 0,
                            modifier = Modifier.size(38.dp).bounceClick(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = AppAccentColor().copy(alpha = 0.12f),
                                contentColor = AppAccentColor()
                            )
                        ) {
                            Icon(Icons.Default.ChevronRight, "Next Month", tint = if (monthOffset < 0) AppAccentColor() else AppTextColor().copy(alpha = 0.3f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                
                if (filteredTransactions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        EmptyStateCard(message = "No past meal logs for $currentDisplayMonth.")
                    }
                } else {
                    // Single Unified Mode: "Adding One by One" Expression Ledger with expandable record actions
                    val dayGroupList = remember(filteredTransactions) {
                        filteredTransactions.groupBy { tx ->
                            Formatters.formatDayDotMonthYear(tx.timestamp)
                        }.map { (dateStr, dayTxs) ->
                            val orderedDayTxs = dayTxs.sortedBy { it.timestamp }
                            var dayTotal = 0.0
                            val amountsSb = StringBuilder()
                            val sessionGroupBreakdowns = mutableListOf<Pair<com.example.data.model.HotelTransactionEntity, List<String>>>()
                            for (i in orderedDayTxs.indices) {
                                val tx = orderedDayTxs[i]
                                dayTotal += tx.totalAmount
                                if (i > 0) amountsSb.append(" + ")
                                val groups = com.example.data.model.TransactionSerializer.deserializeGroups(tx.itemsSerialized)
                                val linesForTx = mutableListOf<String>()
                                if (groups.size > 1) {
                                    val grpExpr = groups.joinToString(" + ") { it.subtotal.toInt().toString() }
                                    amountsSb.append("($grpExpr)")
                                    groups.forEachIndexed { gIdx, grp ->
                                        val itemAddition = grp.items.joinToString(" + ") { "${(it.quantity * it.price).toInt()}" }
                                        val itemNames = grp.items.joinToString(", ") { "${it.name} x${it.quantity}" }
                                        linesForTx.add(
                                            "${if (tx.isNight) "Night" else "Day"} • ${gIdx + 1}. $itemAddition = ₹${grp.subtotal.toInt()} ($itemNames)"
                                        )
                                    }
                                } else {
                                    amountsSb.append(tx.totalAmount.toInt())
                                    val items = com.example.data.model.TransactionSerializer.deserialize(tx.itemsSerialized)
                                    val itemAddition = items.joinToString(" + ") { "${(it.quantity * it.price).toInt()}" }
                                    val itemNames = items.joinToString(", ") { "${it.name} x${it.quantity}" }
                                    if (items.size > 1) {
                                        linesForTx.add(
                                            "${if (tx.isNight) "Night" else "Day"}: $itemAddition = ₹${tx.totalAmount.toInt()} ($itemNames)"
                                        )
                                    } else {
                                        linesForTx.add(
                                            "${if (tx.isNight) "Night" else "Day"}: ₹${tx.totalAmount.toInt()} ($itemNames)"
                                        )
                                    }
                                }
                                sessionGroupBreakdowns.add(tx to linesForTx)
                            }
                            Triple(dateStr, dayTotal.toInt(), Pair(amountsSb.toString(), sessionGroupBreakdowns))
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(dayGroupList, key = { it.first }, contentType = { "day_group" }) { (dateStr, dayTotalInt, exprPair) ->
                            val (amountsStr, txBreakdowns) = exprPair
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = AppAccentColor(),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = dateStr,
                                                color = AppTextColor(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "₹$dayTotalInt",
                                            color = AccentGreen,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$dateStr → $amountsStr = ₹$dayTotalInt",
                                        color = AppAccentColor(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (txBreakdowns.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        txBreakdowns.forEach { (tx, lines) ->
                                            var showMenu by remember { mutableStateOf(false) }
                                            var showDeleteConfirm by remember { mutableStateOf(false) }
                                            if (showDeleteConfirm) {
                                                com.example.ui.components.Tier1DeleteDialog(
                                                    itemTitle = "Transaction (₹${tx.totalAmount.toInt()})",
                                                    onConfirm = { viewModel.deleteTransaction(tx); showDeleteConfirm = false },
                                                    onDismiss = { showDeleteConfirm = false }
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    lines.forEach { line ->
                                                        Text(
                                                            text = line,
                                                            color = AppSecondaryTextColor(),
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.padding(top = 1.dp)
                                                        )
                                                    }
                                                }
                                                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                                    IconButton(
                                                        onClick = { showMenu = true },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.MoreVert,
                                                            contentDescription = "Options",
                                                            tint = AppTextColor().copy(alpha = 0.55f),
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                    DropdownMenu(
                                                        expanded = showMenu,
                                                        onDismissRequest = { showMenu = false },
                                                        offset = androidx.compose.ui.unit.DpOffset(x = (-12).dp, y = 4.dp),
                                                        modifier = Modifier.background(getDialogBackground())
                                                    ) {
                                                        if (onEditTransaction != null) {
                                                            DropdownMenuItem(
                                                                text = { Text("Edit Meal Record", color = AppAccentColor(), fontWeight = FontWeight.Medium) },
                                                                onClick = {
                                                                    showMenu = false
                                                                    onEditTransaction(tx)
                                                                }
                                                            )
                                                        }
                                                        DropdownMenuItem(
                                                            text = { Text("Delete Record", color = AccentPink, fontWeight = FontWeight.Medium) },
                                                            onClick = {
                                                                showMenu = false
                                                                showDeleteConfirm = true
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
                }
                if (filteredTransactions.isNotEmpty()) {
                    val grandTotal = filteredTransactions.sumOf { it.totalAmount }
                    val isPaid = currentMonthPayment != null && currentMonthPayment.amountPaid > 0
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Total Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (userProfile.language == "bn") "মাসিক লেজার সর্বমোট" else "Monthly Ledger Total",
                                        color = AppTextColor(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "₹${grandTotal.toInt()}",
                                    color = AccentGreen,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            HorizontalDivider(color = getGlassBorder().copy(alpha = 0.3f), thickness = 0.5.dp)

                            // Payment Status & Date Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isPaid) AccentGreen.copy(alpha = 0.15f) else Color(0xFFFFB74D).copy(alpha = 0.15f),
                                            border = BorderStroke(
                                                0.5.dp,
                                                if (isPaid) AccentGreen.copy(alpha = 0.5f) else Color(0xFFFFB74D).copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = if (isPaid) AccentGreen else Color(0xFFFFB74D),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = if (isPaid) {
                                                        if (userProfile.language == "bn") "পরিশোধিত" else "Paid"
                                                    } else {
                                                        if (userProfile.language == "bn") "বকেয়া" else "Unpaid"
                                                    },
                                                    color = if (isPaid) AccentGreen else Color(0xFFFFB74D),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        if (isPaid) {
                                            val dateStr = paymentDateFormat.format(Date(currentMonthPayment.paymentDate))
                                            Text(
                                                text = dateStr,
                                                color = AppSecondaryTextColor(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    if (isPaid && currentMonthPayment.note.isNotBlank()) {
                                        Text(
                                            text = currentMonthPayment.note,
                                            color = AppSecondaryTextColor(),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 3.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isPaid) getGlassIconButtonBg() else AppAccentColor().copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (isPaid) getGlassBorder() else AppAccentColor().copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showPaymentDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPaid) Icons.Default.Edit else Icons.Default.Payment,
                                            contentDescription = null,
                                            tint = if (isPaid) AppTextColor() else AppAccentColor(),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isPaid) {
                                                if (userProfile.language == "bn") "স্ট্যাটাস পরিবর্তন" else "Edit Status"
                                            } else {
                                                if (userProfile.language == "bn") "+ পেমেন্ট লিখুন" else "+ Set Paid Status"
                                            },
                                            color = if (isPaid) AppTextColor() else AppAccentColor(),
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
        }
    }
    }

    if (showPaymentDialog) {
        val grandTotal = filteredTransactions.sumOf { it.totalAmount }
        HotelMonthlyPaymentDialog(
            hotelName = activeHotel?.name ?: "Hotel",
            monthName = currentDisplayMonth,
            grandTotal = grandTotal,
            existingPayment = currentMonthPayment,
            language = userProfile.language,
            onSave = { amount, date, note, isPaid ->
                viewModel.setHotelMonthlyPayment(
                    hotelId = activeHotelId,
                    year = targetYear,
                    month = targetMonth,
                    amountPaid = amount,
                    paymentDate = date,
                    note = note,
                    isPaid = isPaid
                )
                showPaymentDialog = false
            },
            viewModel = viewModel, onDismiss = { showPaymentDialog = false }
        )
    }
}

@Composable
fun HotelMonthlyPaymentDialog(
    hotelName: String,
    monthName: String,
    grandTotal: Double,
    existingPayment: HotelPaymentEntity?,
    language: String,
    viewModel: com.example.ui.viewmodel.FinancialViewModel,
    onSave: (amount: Double, paymentDate: Long, note: String, isPaid: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    var isPaidSelected by remember { mutableStateOf(existingPayment != null && existingPayment.amountPaid > 0) }
    var amountText by remember {
        mutableStateOf(
            if (existingPayment != null && existingPayment.amountPaid > 0) {
                existingPayment.amountPaid.toInt().toString()
            } else {
                grandTotal.toInt().toString()
            }
        )
    }
    var paymentDateMillis by remember {
        mutableLongStateOf(existingPayment?.paymentDate ?: System.currentTimeMillis())
    }
    var noteText by remember { mutableStateOf(existingPayment?.note ?: "") }
    
    val dateDisplayFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }

    val datePickerDialog = remember(paymentDateMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = paymentDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                paymentDateMillis = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        
    ) {
        
        val isLight = isAppLightMode()
        val solidDialogBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF1E222D)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .heightIn(max = 620.dp),
                shape = RoundedCornerShape(24.dp),
                color = solidDialogBg,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, getGlassBorder())
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = if (language == "bn") "পেমেন্ট স্ট্যাটাস ও তারিখ" else "Payment Status & Date",
                                color = AppTextColor(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$monthName • $hotelName",
                                color = AppAccentColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .background(AppTextColor().copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = AppTextColor(),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = getGlassBorder().copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Scrollable Body
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Bill Summary Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppTextColor().copy(alpha = 0.05f),
                            border = BorderStroke(0.5.dp, getGlassBorder()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == "bn") "মোট বিল:" else "Total Bill Amount:",
                                    color = AppSecondaryTextColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "₹${grandTotal.toInt()}",
                                    color = if (isLight) Color(0xFF00796B) else AccentGreen,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Status Segmented Toggle: Paid vs Unpaid
                        Text(
                            text = if (language == "bn") "স্ট্যাটাস নির্বাচন করুন:" else "Select Status:",
                            color = AppTextColor(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Paid Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPaidSelected) (if (isLight) Color(0xFFE0F2F1) else AccentGreen.copy(alpha = 0.2f)) else AppTextColor().copy(alpha = 0.04f),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isPaidSelected) (if (isLight) Color(0xFF00796B) else AccentGreen) else getGlassBorder()
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isPaidSelected = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isPaidSelected) (if (isLight) Color(0xFF00796B) else AccentGreen) else AppSecondaryTextColor(),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == "bn") "পরিশোধিত (Paid)" else "Paid",
                                        color = if (isPaidSelected) (if (isLight) Color(0xFF00796B) else AccentGreen) else AppTextColor(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Unpaid Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isPaidSelected) (if (isLight) Color(0xFFFFF3E0) else Color(0xFFFFB74D).copy(alpha = 0.2f)) else AppTextColor().copy(alpha = 0.04f),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (!isPaidSelected) (if (isLight) Color(0xFFE65100) else Color(0xFFFFB74D)) else getGlassBorder()
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isPaidSelected = false }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (!isPaidSelected) (if (isLight) Color(0xFFE65100) else Color(0xFFFFB74D)) else AppSecondaryTextColor(),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == "bn") "বকেয়া (Unpaid)" else "Unpaid",
                                        color = if (!isPaidSelected) (if (isLight) Color(0xFFE65100) else Color(0xFFFFB74D)) else AppTextColor(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (isPaidSelected) {
                            // Payment Date Section
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (language == "bn") "পেমেন্টের তারিখ:" else "Payment Date:",
                                    color = AppTextColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AppTextColor().copy(alpha = 0.04f),
                                    border = BorderStroke(1.dp, getGlassBorder()),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { datePickerDialog.show() }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = AppAccentColor(),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = dateDisplayFormat.format(Date(paymentDateMillis)),
                                                color = AppTextColor(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Text(
                                            text = if (language == "bn") "পরিবর্তন" else "Change",
                                            color = AppAccentColor(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Quick Presets
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AppTextColor().copy(alpha = 0.06f),
                                        border = BorderStroke(0.5.dp, getGlassBorder()),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                paymentDateMillis = System.currentTimeMillis()
                                            }
                                    ) {
                                        Text(
                                            text = if (language == "bn") "আজ (Today)" else "Today",
                                            color = AppTextColor(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AppTextColor().copy(alpha = 0.06f),
                                        border = BorderStroke(0.5.dp, getGlassBorder()),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
                                                paymentDateMillis = cal.timeInMillis
                                            }
                                    ) {
                                        Text(
                                            text = if (language == "bn") "গতকাল (Yesterday)" else "Yesterday",
                                            color = AppTextColor(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // Amount Paid Field
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (language == "bn") "পরিশোধিত টাকার পরিমাণ (₹):" else "Amount Paid (₹):",
                                    color = AppTextColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { amountText = it },
                                    placeholder = { Text("₹ 0", color = AppSecondaryTextColor()) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isLight) Color(0xFF00796B) else AccentGreen,
                                        unfocusedBorderColor = getGlassBorder(),
                                        focusedTextColor = AppTextColor(),
                                        unfocusedTextColor = AppTextColor()
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Payment Note / Mode
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (language == "bn") "পেমেন্ট মাধ্যম / নোট (ঐচ্ছিক):" else "Payment Note / Mode (Optional):",
                                    color = AppTextColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                OutlinedTextField(
                                    value = noteText,
                                    onValueChange = { noteText = it },
                                    placeholder = { Text("e.g. Paid via UPI / GPay / Cash", color = AppSecondaryTextColor()) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isLight) Color(0xFF0284C7) else AppAccentColor(),
                                        unfocusedBorderColor = getGlassBorder(),
                                        focusedTextColor = AppTextColor(),
                                        unfocusedTextColor = AppTextColor()
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = getGlassBorder().copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, getGlassBorder()),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(
                                text = if (language == "bn") "বাতিল" else "Cancel",
                                color = AppTextColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = {
                                val amt = if (isPaidSelected) (amountText.toDoubleOrNull() ?: grandTotal) else 0.0
                                onSave(amt, paymentDateMillis, noteText.trim(), isPaidSelected)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaidSelected) (if (isLight) Color(0xFF00897B) else AccentGreen) else (if (isLight) Color(0xFFE65100) else Color(0xFFFFB74D))
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1.25f)
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(
                                text = if (language == "bn") "সংরক্ষণ করুন" else "Save Status",
                                color = if (isPaidSelected) Color.White else (if (isLight) Color.White else Color(0xFF0F0B26)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DishImageAndEmoji(
    emoji: String,
    imageUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(0x11FFFFFF), RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(emoji, fontSize = 10.sp)
            }
        } else {
            Text(emoji, fontSize = (size.value * 0.5f).sp)
        }
    }
}


@Composable
fun DishCard(
    dish: com.example.data.model.MenuEntity,
    qty: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdateQty: (Int) -> Unit
) {
    val isLight = isAppLightMode()
    val cardBg = if (isLight) {
        if (isSelected) Color(0xFFE0F7FA) else Color(0xF5FFFFFF)
    } else {
        if (isSelected) Color(0x3300E5FF) else Color(0x1FFFFFFF)
    }
    val borderColor = if (isSelected) AppAccentColor() else (if (isLight) Color(0x22000000) else Color(0x2BFFFFFF))
    val addBtnBg = AppAccentColor().copy(alpha = 0.15f)
    val addBtnBorder = AppAccentColor().copy(alpha = 0.4f)
    val addBtnText = AppAccentColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, shape = RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left part: Dish Image/Emoji + Name Details (Clickable to select with premium bounce & haptic feedback)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .bounceClick()
                    .clickable { onSelect() },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DishImageAndEmoji(
                    emoji = dish.emoji,
                    imageUri = dish.imageUri,
                    size = 54.dp
                )
                
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = dish.banglaName,
                        color = AppTextColor(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dish.englishName,
                        color = AppTextColor().copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${dish.price.toInt()}",
                        color = if (isLight) Color(0xFFD84315) else AccentGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right part: Quantity Controller (+ - button)
            if (qty == 0) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(34.dp)
                        .background(addBtnBg, RoundedCornerShape(17.dp))
                        .border(0.5.dp, addBtnBorder, RoundedCornerShape(17.dp))
                        .clickable { 
                            onSelect()
                            onUpdateQty(1) 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add +",
                        color = addBtnText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .width(88.dp)
                        .height(34.dp)
                        .background(
                            if (isLight) Color(0xFF0097A7).copy(alpha = 0.18f) else AppAccentColor().copy(alpha = 0.25f),
                            RoundedCornerShape(17.dp)
                        )
                        .border(
                            1.dp,
                            if (isLight) Color(0xFF0097A7).copy(alpha = 0.5f) else AppAccentColor().copy(alpha = 0.6f),
                            RoundedCornerShape(17.dp)
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onUpdateQty(-1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "-",
                            tint = AppTextColor(),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "$qty",
                        color = AppTextColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onUpdateQty(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "+",
                            tint = AppTextColor(),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun UnifiedHotelDialog(
    hotel: HotelEntity?, // null for register, non-null for edit
    prefLanguage: String,
    hotelsCount: Int,
    viewModel: com.example.ui.viewmodel.FinancialViewModel,
    onDismiss: () -> Unit,
    onSave: (name: String, advanced: Double, phone: String, loc: String, upi: String, qr: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    BackHandler { onDismiss() }

    var hotelName by remember(hotel) { mutableStateOf(hotel?.name ?: "") }
    var hotelAdv by remember(hotel) { mutableStateOf(hotel?.advancedPayment?.toString() ?: "") }
    var hotelPhone by remember(hotel) { mutableStateOf(hotel?.phoneNumber ?: "") }
    var hotelLoc by remember(hotel) { mutableStateOf(hotel?.location ?: "") }
    var hotelUpi by remember(hotel) { mutableStateOf(hotel?.upiId ?: "") }
    var hotelQr by remember(hotel) { mutableStateOf(hotel?.qrCodeUri ?: "") }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        com.example.ui.components.Tier2DeleteDialog(
            itemTitle = hotelName,
            onConfirm = { onDelete?.invoke(); showDeleteConfirm = false; onDismiss() },
            onDismiss = { showDeleteConfirm = false }
        )
    }

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
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getAppBackgroundGradient()),
                contentAlignment = Alignment.TopCenter
            ) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .dialogSystemBarsPadding(extraTop = 8.dp),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0.dp),
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
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
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = if (hotel == null) {
                                    if (prefLanguage == "bn") "নতুন হোটেল যুক্ত করুন" else "Register Establishment"
                                } else {
                                    if (prefLanguage == "bn") "হোটেল সংশোধন" else "Edit Establishment Details"
                                },
                                color = AppTextColor(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            
                            if (hotel != null && hotelsCount > 1 && onDelete != null) {
                                var showHotelMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { showHotelMenu = true },
                                        modifier = Modifier.size(38.dp).background(getGlassIconButtonBg(), CircleShape).bounceClick()
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = AppTextColor())
                                    }
                                    DropdownMenu(
                                        expanded = showHotelMenu,
                                        onDismissRequest = { showHotelMenu = false },
                                        modifier = Modifier.background(getDialogBackground())
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Move to Bin", color = AccentPink, fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                showHotelMenu = false
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
                                .fillMaxSize()
                                .widthIn(max = 600.dp)
                                .verticalScroll(rememberScrollState())
                                .imePadding()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GlassTextField(
                                    value = hotelName,
                                    onValueChange = { hotelName = it },
                                    label = { Text(if (prefLanguage == "bn") "হোটেলের নাম" else "Hotel Name") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth().testTag("hotel_name_input")
                                )
                                GlassTextField(
                                    value = hotelAdv,
                                    onValueChange = { hotelAdv = it },
                                    label = { Text(if (prefLanguage == "bn") "অগ্রিম জমা (₹)" else "Advanced Deposit (₹)") },
                                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("hotel_adv_input")
                                )
                                GlassTextField(
                                    value = hotelPhone,
                                    onValueChange = { hotelPhone = it },
                                    label = { Text(if (prefLanguage == "bn") "ফোন নম্বর" else "Phone Number") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("hotel_phone_input")
                                )
                                GlassTextField(
                                    value = hotelLoc,
                                    onValueChange = { hotelLoc = it },
                                    label = { Text(if (prefLanguage == "bn") "অবস্থান / ঠিকানা" else "Location / Address") },
                                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth().testTag("hotel_loc_input")
                                )
                                GlassTextField(
                                    value = hotelUpi,
                                    onValueChange = { hotelUpi = it },
                                    label = { Text(if (prefLanguage == "bn") "UPI আইডি (যেমন- upi@upi)" else "UPI ID (e.g. name@bank)") },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth().testTag("hotel_upi_input")
                                )
                                GlassTextField(
                                    value = hotelQr,
                                    onValueChange = { hotelQr = it },
                                    label = { Text(if (prefLanguage == "bn") "QR কোড ফাইল বা লিঙ্ক" else "QR Code File Path / Link") },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier.fillMaxWidth().testTag("hotel_qr_input")
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        if (hotelName.isNotBlank()) {
                                            val adv = hotelAdv.toDoubleOrNull() ?: 0.0
                                            onSave(hotelName, adv, hotelPhone, hotelLoc, hotelUpi, hotelQr)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppAccentColor(),
                                        contentColor = AppOnAccentColor()
                                    ),
                                    modifier = Modifier.weight(1f).testTag("save_hotel_btn")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = AppOnAccentColor(), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (hotel == null) {
                                            if (prefLanguage == "bn") "যোগ করুন" else "Register"
                                        } else {
                                            if (prefLanguage == "bn") "সংরক্ষণ" else "Save"
                                        },
                                        color = AppOnAccentColor(),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("cancel_hotel_btn")) {
                                Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AppTextColor())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HotelInfoDetailRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    actionIcon2: ImageVector? = null,
    onAction2: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
            Text(value, color = AppTextColor(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        if (actionIcon2 != null && onAction2 != null) {
            IconButton(
                onClick = onAction2,
                modifier = Modifier
                    .size(34.dp)
                    .background(iconTint.copy(alpha = 0.1f), CircleShape)
                    .bounceClick()
            ) {
                Icon(actionIcon2, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (actionIcon != null && onAction != null) {
            IconButton(
                onClick = onAction,
                modifier = Modifier
                    .size(34.dp)
                    .background(iconTint.copy(alpha = 0.1f), CircleShape)
                    .bounceClick()
            ) {
                Icon(actionIcon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun HotelContactOptionsDialog(
    hotel: com.example.data.model.HotelEntity,
    isBengali: Boolean,
    onEditHotel: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasTelephony = remember {
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEPHONY)
    }
    val phoneNum = hotel.phoneNumber.trim()
    val whatsappNum = hotel.whatsappNumber.trim().ifBlank { phoneNum }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = getDialogBackground(),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppAccentColor().copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBengali) "যোগাযোগ করুন" else "Contact ${hotel.name}",
                                color = AppTextColor(),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!hasTelephony) {
                                Text(
                                    text = "Wi-Fi Tablet Mode • WhatsApp Chat",
                                    color = AppSecondaryTextColor(),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTextColor())
                    }
                }

                // 1. Phone Call Option (Hidden on Wi-Fi-only tablets without telephony)
                if (hasTelephony) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = getSettingsItemBg(),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (phoneNum.isNotBlank()) {
                                    onDismiss()
                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phoneNum"))
                                    try { context.startActivity(intent) } catch (e: Exception) {}
                                } else {
                                    android.widget.Toast.makeText(context, "No phone number added. Tap Edit to add.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2196F3).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBengali) "ফোন কল করুন" else "Phone Call",
                                    color = AppTextColor(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = phoneNum.ifBlank { if (isBengali) "নম্বর যোগ করা হয়নি" else "No phone number set" },
                                    color = AppSecondaryTextColor(),
                                    fontSize = 12.sp
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppSecondaryTextColor())
                        }
                    }
                }

                // 2. WhatsApp Option (Always shown)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = getSettingsItemBg(),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (whatsappNum.isNotBlank()) {
                                onDismiss()
                                val cleanNum = whatsappNum.replace(Regex("[^0-9+]"), "").removePrefix("+")
                                val finalNum = if (cleanNum.length == 10) "91$cleanNum" else cleanNum
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://wa.me/$finalNum")
                                )
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Could not open WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "No WhatsApp number added. Tap Edit to add.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF25D366).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBengali) "হোয়াটসঅ্যাপ চ্যাট" else "WhatsApp Chat",
                                color = AppTextColor(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = whatsappNum.ifBlank { if (isBengali) "নম্বর যোগ করা হয়নি" else "No WhatsApp number set" },
                                color = AppSecondaryTextColor(),
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppSecondaryTextColor())
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onEditHotel) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBengali) "নম্বর সম্পাদনা" else "Edit Contact Numbers", color = AppAccentColor(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(if (isBengali) "বন্ধ করুন" else "Close", color = AppSecondaryTextColor(), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelDetailsBottomSheet(
    hotel: com.example.data.model.HotelEntity,
    isBengali: Boolean,
    viewModel: com.example.ui.viewmodel.FinancialViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var previewQrUri by remember { mutableStateOf<String?>(null) }
    val qrUris = hotel.qrCodeUri.split(",").filter { it.isNotBlank() }

    androidx.activity.compose.BackHandler(onBack = onDismiss)

    // Unified Popup Layout matching Account Info Popup
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()
        AppThemeProvider(viewModel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        onClick = onDismiss,
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 520.dp)
                        .padding(vertical = 24.dp)
                        .clickable(
                            onClick = {},
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ),
                    shape = RoundedCornerShape(24.dp),
                    color = getDialogBackground(),
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(AppAccentColor().copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apartment,
                                        contentDescription = null,
                                        tint = AppAccentColor(),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isBengali) "হোটেলের বিবরণ" else "Hotel Details",
                                    color = AppTextColor(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(getGlassIconButtonBg(), CircleShape)
                                    .bounceClick()
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTextColor())
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Header Banner Card (Exact match with Account Info Popup)
                        Surface(
                            color = getSettingsItemBg(),
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = hotel.name,
                                        color = AppTextColor(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isBengali) "হোটেলের বিস্তারিত বিবরণ ও পেমেন্ট তথ্য" else "Hotel Information & Payment Details",
                                        color = AppSecondaryTextColor(),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Info Card Group (Exact match with Account Info Popup)
                        Surface(
                            color = getSettingsItemBg(),
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Advanced Payment
                                HotelInfoDetailRow(
                                    icon = Icons.Default.AccountBalanceWallet,
                                    iconTint = AppAccentColor(),
                                    label = if (isBengali) "অগ্রিম জমা" else "Advanced Payment",
                                    value = "₹${hotel.advancedPayment.toInt()}"
                                )

                                HorizontalDivider(color = getGlassBorder())

                                // Phone Number
                                HotelInfoDetailRow(
                                    icon = Icons.Default.Phone,
                                    iconTint = Color(0xFF81C784),
                                    label = if (isBengali) "ফোন নম্বর" else "Phone Number",
                                    value = hotel.phoneNumber.ifBlank { if (isBengali) "প্রদান করা হয়নি" else "Not provided" },
                                    actionIcon = if (hotel.phoneNumber.isNotBlank()) Icons.Default.ContentCopy else null,
                                    onAction = if (hotel.phoneNumber.isNotBlank()) {
                                        {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(hotel.phoneNumber))
                                            android.widget.Toast.makeText(context, "Phone number copied!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } else null
                                )

                                if (hotel.whatsappNumber.isNotBlank()) {
                                    HorizontalDivider(color = getGlassBorder())
                                    HotelInfoDetailRow(
                                        icon = Icons.Default.Chat,
                                        iconTint = Color(0xFF25D366),
                                        label = if (isBengali) "হোয়াটসঅ্যাপ নম্বর" else "WhatsApp Number",
                                        value = hotel.whatsappNumber,
                                        actionIcon = Icons.Default.ContentCopy,
                                        onAction = {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(hotel.whatsappNumber))
                                            android.widget.Toast.makeText(context, "WhatsApp number copied!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }

                                HorizontalDivider(color = getGlassBorder())

                                // Location
                                HotelInfoDetailRow(
                                    icon = Icons.Default.LocationOn,
                                    iconTint = Color(0xFFFFB74D),
                                    label = if (isBengali) "অবস্থান / ঠিকানা" else "Location / Address",
                                    value = hotel.location.ifBlank { if (isBengali) "প্রদান করা হয়নি" else "Not provided" },
                                    actionIcon = if (hotel.location.isNotBlank()) Icons.Default.Map else null,
                                    onAction = {
                                        if (hotel.location.isNotBlank()) {
                                            val intent = if (hotel.location.startsWith("http")) {
                                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(hotel.location))
                                            } else {
                                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(hotel.location)}"))
                                            }
                                            try { context.startActivity(intent) } catch (e: Exception) {}
                                        }
                                    }
                                )

                                HorizontalDivider(color = getGlassBorder())

                                // UPI ID & QR Code button
                                HotelInfoDetailRow(
                                    icon = Icons.Default.QrCode,
                                    iconTint = Color(0xFFBA68C8),
                                    label = if (isBengali) "ইউপিআই আইডি (UPI ID)" else "UPI ID",
                                    value = hotel.upiId.ifBlank { if (isBengali) "প্রদান করা হয়নি" else "Not provided" },
                                    actionIcon = if (hotel.upiId.isNotBlank()) Icons.Default.ContentCopy else null,
                                    onAction = {
                                        if (hotel.upiId.isNotBlank()) {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(hotel.upiId))
                                            android.widget.Toast.makeText(context, if (isBengali) "UPI ID কপি করা হয়েছে!" else "UPI ID copied!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    actionIcon2 = if (qrUris.isNotEmpty()) Icons.Default.QrCodeScanner else null,
                                    onAction2 = if (qrUris.isNotEmpty()) {
                                        { previewQrUri = qrUris.first() }
                                    } else null
                                )

                                if (!hotel.importedFrom.isNullOrBlank()) {
                                    HorizontalDivider(color = getGlassBorder())
                                    HotelInfoDetailRow(
                                        icon = Icons.Default.Download,
                                        iconTint = Color(0xFF64B5F6),
                                        label = if (isBengali) "আমদানি করা হয়েছে" else "Import Source",
                                        value = if (isBengali) "আমদানি করা হয়েছে: ${hotel.importedFrom}" else "Imported from: ${hotel.importedFrom}"
                                    )
                                }
                            }
                        }

                        // Short Notes Card
                        if (hotel.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = getSettingsItemBg(),
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Notes, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBengali) "সংক্ষিপ্ত নোট" else "Short Notes",
                                            color = AppTextColor(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = hotel.notes,
                                        color = AppSecondaryTextColor(),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        // QR Codes Section
                        if (qrUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = getSettingsItemBg(),
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isBengali) "পেমেন্ট কিউআর কোড (${qrUris.size})" else "Payment QR Codes (${qrUris.size})",
                                        color = AppTextColor(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        qrUris.forEachIndexed { idx, uri ->
                                            Surface(
                                                modifier = Modifier
                                                    .size(120.dp)
                                                    .clickable { previewQrUri = uri },
                                                shape = RoundedCornerShape(14.dp),
                                                color = Color.White,
                                                shadowElevation = 4.dp
                                            ) {
                                                AsyncImage(
                                                    model = uri,
                                                    contentDescription = "QR Code ${idx + 1}",
                                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons (Matching Account Info Popup)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    onDismiss()
                                    viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.EditHotel(hotel))
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = getSettingsItemBg(),
                                    contentColor = AppTextColor()
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBengali) "সম্পাদনা" else "Edit", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(44.dp).bounceClick(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor(), contentColor = AppOnAccentColor())
                            ) {
                                Text(if (isBengali) "বন্ধ করুন" else "Close", color = AppOnAccentColor(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (previewQrUri != null) {
        com.example.ui.components.FullScreenQRPreviewDialog(
            qrUri = previewQrUri!!,
            title = if (isBengali) "হোটেল পেমেন্ট কিউআর" else "${hotel.name} Payment QR",
            upiId = hotel.upiId,
            onDismiss = { previewQrUri = null }
        )
    }
}

@Composable
fun MealGroupCard(
    group: com.example.data.model.MealGroup,
    index: Int,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val isLight = com.example.ui.components.isAppLightMode()
    val cardBg = if (isLight) Color(0xFFEDF4FA) else Color(0xFF1E283C)
    val cardBorder = if (isLight) Color(0xFFBAE6FD) else Color(0xFF2E4060)
    val badgeBg = if (isLight) AppAccentColor().copy(alpha = 0.2f) else AppAccentColor().copy(alpha = 0.25f)

    Card(
        modifier = modifier.width(if (isCompact) 144.dp else 160.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.5.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 10.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = "${index + 1}.",
                        color = AppAccentColor(),
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(if (isCompact) 20.dp else 22.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Group",
                        tint = AccentPink,
                        modifier = Modifier.size(if (isCompact) 13.dp else 14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val itemsSummary = group.items.joinToString(", ") { "${it.name} x${it.quantity}" }
            Text(
                text = itemsSummary,
                color = AppTextColor().copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = if (isCompact) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            val mathBreakdown = if (group.items.size > 1) {
                val parts = group.items.joinToString(" + ") { "${(it.price * it.quantity).toInt()}" }
                "$parts = ₹${group.subtotal.toInt()}"
            } else {
                "₹${group.subtotal.toInt()}"
            }
            Text(
                text = mathBreakdown,
                color = if (isLight) Color(0xFF15803D) else AccentGreen,
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
