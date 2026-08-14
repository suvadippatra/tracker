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
    var showAddMealFlow by remember { mutableStateOf(false) }
    var showCreateHotelModal by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    val quickActionTrigger by viewModel.quickActionTrigger.collectAsStateWithLifecycle()
    LaunchedEffect(quickActionTrigger) {
        if (quickActionTrigger == "ADD_MEAL_LOG") {
            showAddMealFlow = true
            viewModel.clearQuickActionTrigger()
        } else if (quickActionTrigger == "ADD_HOTEL") {
            showCreateHotelModal = true
            viewModel.clearQuickActionTrigger()
        }
    }

    // Dialog edit fields
    var hotelToEdit by remember { mutableStateOf<com.example.data.model.HotelEntity?>(null) }
    var showInfoDialog by remember { mutableStateOf<com.example.data.model.HotelEntity?>(null) }
    var manageMenuForHotel by remember { mutableStateOf<com.example.data.model.HotelEntity?>(null) }

    LaunchedEffect(showMealHistory) {
        viewModel.registerSubScreen("MealHistory", showMealHistory)
    }
    LaunchedEffect(showAddMealFlow) {
        viewModel.registerSubScreen("AddMeal", showAddMealFlow)
    }
    LaunchedEffect(hotelToEdit) {
        viewModel.registerSubScreen("EditHotel", hotelToEdit != null)
    }
    LaunchedEffect(showCreateHotelModal) {
        viewModel.registerSubScreen("CreateHotel", showCreateHotelModal)
    }
    LaunchedEffect(showInfoDialog) {
        viewModel.registerSubScreen("InfoDialog", showInfoDialog != null)
    }
    LaunchedEffect(manageMenuForHotel) {
        viewModel.registerSubScreen("ManageMenu", manageMenuForHotel != null)
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
                            .background(AccentCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddBusiness,
                            contentDescription = null,
                            tint = AccentCyan,
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
                        text = if (isBengali) "দয়া করে সেটিংস থেকে একটি নতুন হোটেল যোগ করুন যাতে খাবার লগ, মেনু এবং খরচ ট্র্যাক করা যায়।" else "Please add a new hotel from Settings to start tracking meal logs, menus, and expenses.",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

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
                    contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
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
                                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                val scale = 1f - (0.05f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                                val alpha = 1f - (0.3f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                                Box(modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }) {
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
                                                Text(
                                                    text = hotel.name,
                                                    color = AppTextColor(),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Phone,
                                                        contentDescription = "Call",
                                                        tint = AppTextColor(),
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .padding(end = 4.dp)
                                                            .clickable { if (hotel.phoneNumber.isNotBlank()) {
                                                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${hotel.phoneNumber}"))
                                                                    context.startActivity(intent)
                                                                } else {
                                                                    android.widget.Toast.makeText(context, "No phone number added", android.widget.Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
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
                                                            modifier = Modifier
                                                                .background(getDialogBackground())
                                                                .border(0.5.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                                                        ) {
                                                            DropdownMenuItem(
                                                                text = { Text(if (isBengali) "নতুন হোটেল যোগ করুন" else "+ Add New Hotel", color = AccentCyan) },
                                                                onClick = {
                                                                    showCreateHotelModal = true
                                                                    openDropdownHotelId = null
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text(if (isBengali) "কিউআর মাধ্যমে যোগ করুন" else "+ Import via QR", color = AccentCyan) },
                                                                onClick = {
                                                                    showQrScanner = true
                                                                    openDropdownHotelId = null
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text(if (isBengali) "হোটেল তথ্য সংশোধন" else "Edit this hotel", color = AccentCyan) },
                                                                onClick = {
                                                                    hotelToEdit = hotel
                                                                    openDropdownHotelId = null
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                                onClick = {
                                                                    pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                                    openDropdownHotelId = null
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text(if (isBengali) "কিউআর মাধ্যমে এক্সপোর্ট" else "Export via QR", color = AccentCyan) },
                                                                onClick = {
                                                                    openDropdownHotelId = null
                                                                    viewModel.exportHotelViaQr(hotel.id) { result ->
                                                                        if (result is com.example.data.ExportResult.Success) {
                                                                            viewModel.setQrExportPayload(result, java.util.UUID.randomUUID().toString())
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
                                                            fontSize = 24.sp,
                                                            fontWeight = FontWeight.Bold,
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
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
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
                                items(popularMeals, key = { it.first }) { (name, count) ->
                                    PresetMealCard(
                                        title = name,
                                        description = "Frequently ordered dish at this establishment",
                                        tag = "$count times",
                                        color = AccentCyan
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
                    contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
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
                                                        tint = if (tx.isNight) AccentCyan else AccentGold,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    val txSdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                                                    Text(
                                                        text = txSdf.format(Date(tx.timestamp)),
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
                                                            text = "Group ${idx + 1}: $grpSummary (₹${grp.subtotal.toInt()})",
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
                                        tint = AccentCyan
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
                    .padding(horizontal = 12.dp)
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
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val scale = 1f - (0.05f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                            val alpha = 1f - (0.3f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                            Box(modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }) {
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
                                            Text(
                                                text = hotel.name,
                                                color = AppTextColor(),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Phone,
                                                    contentDescription = "Call",
                                                    tint = AppTextColor(),
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .padding(end = 4.dp)
                                                        .clickable { if (hotel.phoneNumber.isNotBlank()) {
                                                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${hotel.phoneNumber}"))
                                                                context.startActivity(intent)
                                                            } else {
                                                                android.widget.Toast.makeText(context, "No phone number added", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
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
                                                        modifier = Modifier
                                                            .background(getDialogBackground())
                                                            .border(0.5.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text(if (isBengali) "নতুন হোটেল যোগ করুন" else "+ Add New Hotel", color = AccentCyan) },
                                                            onClick = {
                                                                showCreateHotelModal = true
                                                                openDropdownHotelId = null
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text(if (isBengali) "কিউআর মাধ্যমে যোগ করুন" else "+ Import via QR", color = AccentCyan) },
                                                            onClick = {
                                                                showQrScanner = true
                                                                openDropdownHotelId = null
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text(if (isBengali) "হোটেল তথ্য সংশোধন" else "Edit this hotel", color = AccentCyan) },
                                                            onClick = {
                                                                hotelToEdit = hotel
                                                                openDropdownHotelId = null
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                            onClick = {
                                                                pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                                openDropdownHotelId = null
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text(if (isBengali) "কিউআর মাধ্যমে এক্সপোর্ট" else "Export via QR", color = AccentCyan) },
                                                            onClick = {
                                                                openDropdownHotelId = null
                                                                viewModel.exportHotelViaQr(hotel.id) { result ->
                                                                        if (result is com.example.data.ExportResult.Success) {
                                                                            viewModel.setQrExportPayload(result, java.util.UUID.randomUUID().toString())
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
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
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
                            items(popularMeals, key = { it.first }) { (name, count) ->
                                PresetMealCard(
                                    title = name,
                                    description = "Frequently ordered dish at this establishment",
                                    tag = "$count times",
                                    color = AccentCyan
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))

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
                                                    tint = if (tx.isNight) AccentCyan else AccentGold,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                val txSdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                                                Text(
                                                    text = txSdf.format(Date(tx.timestamp)),
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
                                                        text = "Group ${idx + 1}: $grpSummary (₹${grp.subtotal.toInt()})",
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
                                    tint = AccentCyan
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
                    Spacer(modifier = Modifier.height(120.dp)) // Padding for bottom bar and FAB
                }
            }
        }

        // FAB FOR ADDING NEW MEAL
        if (activeHotel != null && !showAddMealFlow) {
            FloatingActionButton(
                onClick = {
                    viewModel.resetOrderFlow()
                    showAddMealFlow = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 120.dp, end = 16.dp)
                    .bounceClick()
                    .testTag("add_meal_fab"),
                shape = CircleShape,
                containerColor = Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isAppLightMode()) Color.White else Color(0xFF0F0B26), shape = CircleShape)
                        .border(2.dp, AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Meal",
                        tint = AccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        

    }

    // Modal Sheet / Full Dialog for Add Meal
    if (showAddMealFlow) {
        AddMealFlowModal(
            viewModel = viewModel,
            onDismiss = { showAddMealFlow = false }
        )
    }

    // Modal Dialog for Meal History
    
    if (showInfoDialog != null) {
        HotelDetailsBottomSheet(
            hotel = showInfoDialog!!,
            isBengali = isBengali,
            viewModel = viewModel,
            onDismiss = { showInfoDialog = null },
            onEdit = {
                hotelToEdit = showInfoDialog
                showInfoDialog = null
            }
        )
    }

    if (showMealHistory) {
        MealHistoryModal(
            viewModel = viewModel,
            onDismiss = { showMealHistory = false },
            onEditTransaction = { tx ->
                viewModel.loadTransactionForEditing(tx)
                showMealHistory = false
                showAddMealFlow = true
            }
        )
    }

    // Modal Sheet for Editing Hotel Parameters
    if (manageMenuForHotel != null) { MenuManagementScreen(hotel = manageMenuForHotel!!, viewModel = viewModel, onBack = { hotelToEdit = manageMenuForHotel; manageMenuForHotel = null }) }
    if (showCreateHotelModal || hotelToEdit != null) {
        EditHotelModal(
            hotel = hotelToEdit,
            viewModel = viewModel,
            onDismiss = { 
                showCreateHotelModal = false
                hotelToEdit = null 
            },
            onManageMenu = { 
                manageMenuForHotel = it
                showCreateHotelModal = false
                hotelToEdit = null
            }
        )
    }

    val exportPayload by viewModel.qrExportPayload.collectAsStateWithLifecycle()
    val exportSessionId by viewModel.qrExportSessionId.collectAsStateWithLifecycle()

    if (exportPayload != null) {
        com.example.ui.components.QrExportStreamDialog(
            exportData = exportPayload!!,
            sessionId = exportSessionId ?: "",
            onDismiss = { viewModel.setQrExportPayload(null, null) }
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
        GlassCard(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 16.dp
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


@Composable
fun AddMealFlowModal(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val date by viewModel.addMealDate.collectAsStateWithLifecycle()
    val isNight by viewModel.addMealIsNight.collectAsStateWithLifecycle()
    val quantities by viewModel.orderQuantities.collectAsStateWithLifecycle()
    val selectedMainId by viewModel.selectedMainDishId.collectAsStateWithLifecycle()
    val menuItems by viewModel.allMenuItems.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val committedGroups by viewModel.committedMealGroups.collectAsStateWithLifecycle()
    val editingTxId by viewModel.editingTransactionId.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    var showAllDishes by remember { mutableStateOf(false) }

    // Filter menu dynamically using remember to avoid redundant work
    val filteredDishes = remember(menuItems, isNight, showAllDishes) {
        menuItems.filter {
            if (showAllDishes) {
                true
            } else {
                if (isNight) it.isNightMenu else it.isDayMenu
            }
        }
    }
    val mainDishes = remember(filteredDishes) { filteredDishes.filter { !it.isAddOn } }
    val addOnDishes = remember(filteredDishes) { filteredDishes.filter { it.isAddOn } }

    BackHandler {
        onDismiss()
    }

    com.example.ui.components.AppThemeProvider(viewModel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(userProfile.themeMode))
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(getGlassBorder(), CircleShape).bounceClick()
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTextColor())
                    }

                    Text(
                        text = if (editingTxId != null) "Edit Meal Record" else "Add Meal Record",
                        color = AppTextColor(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Glowy Save Button
                    GlassButton(
                        onClick = {
                            viewModel.saveHotelTransaction()
                            onDismiss()
                        },
                        cornerRadius = 10.dp,
                        borderBrush = Brush.linearGradient(listOf(AccentCyan, AccentPink)),
                        modifier = Modifier.testTag("save_meal_button")
                    ) {
                        Text(
                            text = "Save",
                            color = AppTextColor(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // TOP SECTION: DATE CONTROLS
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            InlineDatePicker(
                                initialDateMillis = date,
                                onDateSelected = { newDate ->
                                    viewModel.setAddMealDate(newDate)
                                },
                                textColor = AppTextColor()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // DAY/NIGHT ICON-BASED TOGGLE SWITCH
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Menu Period Selection",
                            color = AppTextColor(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        val isAppLight = isAppLightMode()
                        Row(
                            modifier = Modifier
                                .background(
                                    if (isAppLight) Color(0x1A000000) else Color(0x1F000000),
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isAppLight) Color(0x33000000) else getGlassBorder(),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Day Icon Button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(
                                        if (!isNight) Brush.horizontalGradient(listOf(Color(0xFFFF9800), Color(0xFFFF5722)))
                                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable { if (isNight) viewModel.toggleDayNight() }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = "Day",
                                    tint = if (!isNight) Color.White else AppTextColor().copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Day",
                                    color = if (!isNight) Color.White else AppTextColor().copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Night Icon Button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(
                                        if (isNight) Brush.horizontalGradient(listOf(Color(0xFF3F51B5), Color(0xFF673AB7)))
                                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable { if (!isNight) viewModel.toggleDayNight() }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = "Night",
                                    tint = if (isNight) Color.White else AppTextColor().copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Night",
                                    color = if (isNight) Color.White else AppTextColor().copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MAIN DISHES LIST SECTION
                    Text(
                        text = "Main Dishes (Select One to View Add-ons)",
                        color = AppTextColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        mainDishes.forEach { dish ->
                            DishCard(
                                dish = dish,
                                qty = quantities[dish.id] ?: 0,
                                isSelected = selectedMainId == dish.id,
                                onSelect = { viewModel.selectMainDish(dish.id) },
                                onUpdateQty = { delta -> viewModel.updateItemQuantity(dish.id, delta) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // View More Dishes menu item / toggle button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(AccentCyan.copy(alpha = 0.15f), AccentPink.copy(alpha = 0.15f))
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .clickable { showAllDishes = !showAllDishes }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (showAllDishes) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showAllDishes) "View Less Dishes" else "View More Dishes (Show ${if (isNight) "Day" else "Night"} Dishes)",
                                color = AppTextColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // DYNAMICALLY RENDERED ADD-ON DISHES TRAY BELOW
                    AnimatedVisibility(
                        visible = selectedMainId != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            val activeMainName = menuItems.find { it.id == selectedMainId }?.englishName ?: ""
                            Text(
                                text = "Supplementary Add-ons for $activeMainName",
                                color = AccentCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    addOnDishes.forEach { addOn ->
                                        val addOnQty = quantities[addOn.id] ?: 0

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = addOn.banglaName,
                                                    color = AppTextColor(),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "${addOn.englishName} • ₹${addOn.price.toInt()}",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (addOnQty > 0) {
                                                    IconButton(
                                                        onClick = { viewModel.updateItemQuantity(addOn.id, -1) },
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .background(getGlassIconButtonBg(), CircleShape)
                                                    ) {
                                                        Icon(Icons.Default.Remove, contentDescription = "-", tint = AppTextColor(), modifier = Modifier.size(12.dp))
                                                    }
                                                    Text(
                                                        text = "$addOnQty",
                                                        color = AppTextColor(),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.updateItemQuantity(addOn.id, 1) },
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(if (addOnQty > 0) AccentCyan else getGlassIconButtonBg(), CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "+",
                                                        tint = if (addOnQty > 0) Color(0xFF0F0B26) else AppTextColor(),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (addOn != addOnDishes.last()) {
                                            HorizontalDivider(color = Color(0x0FFFFFFF), thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // COMMITTED MEAL GROUPS CARDS DISPLAY (IF SET TO PAGE BOTTOM)
                    if (userProfile.mealGroupPosition == "PageBottom" && committedGroups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Committed Meal Groups (${committedGroups.size})",
                            color = AppTextColor(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            itemsIndexed(committedGroups, key = { _, group -> group.id }) { index, group ->
                                GlassCard(
                                    modifier = Modifier.width(150.dp),
                                    cornerRadius = 16.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Group ${index + 1}",
                                                color = AccentCyan,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { viewModel.removeMealGroup(group.id) },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove Group",
                                                    tint = AccentPink,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        val itemsSummary = group.items.joinToString(", ") { "${it.name} x${it.quantity}" }
                                        Text(
                                            text = itemsSummary,
                                            color = AppTextColor().copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            maxLines = 2,
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
                                            color = AccentGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BOTTOM ACTION & TOTAL SUMMARY ROW (LOCKED AT BOTTOM)
                val hasDraftSelection = quantities.any { it.value > 0 }
                val draftSubtotal = quantities.mapNotNull { (id, qty) ->
                    val item = menuItems.find { it.id == id }
                    if (item != null && qty > 0) item.price * qty else null
                }.sum()
                val committedSubtotal = committedGroups.sumOf { it.subtotal }
                val overallTotal = committedSubtotal + draftSubtotal

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(getDialogBackground(), RoundedCornerShape(20.dp))
                        .border(
                            width = 1.dp,
                            color = getGlassBorder(),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // COMMITTED MEAL GROUPS CARDS DISPLAY (IN BOTTOM BAR)
                        if (userProfile.mealGroupPosition != "PageBottom" && committedGroups.isNotEmpty()) {
                            Text(
                                text = "Committed Meal Groups (${committedGroups.size})",
                                color = AppTextColor(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 10.dp)
                            ) {
                                itemsIndexed(committedGroups, key = { _, group -> group.id }) { index, group ->
                                    GlassCard(
                                        modifier = Modifier.width(140.dp),
                                        cornerRadius = 14.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Group ${index + 1}",
                                                    color = AccentCyan,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                IconButton(
                                                    onClick = { viewModel.removeMealGroup(group.id) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove Group",
                                                        tint = AccentPink,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            val itemsSummary = group.items.joinToString(", ") { "${it.name} x${it.quantity}" }
                                            Text(
                                                text = itemsSummary,
                                                color = AppTextColor().copy(alpha = 0.7f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            val mathBreakdown = if (group.items.size > 1) {
                                                val parts = group.items.joinToString(" + ") { "${(it.price * it.quantity).toInt()}" }
                                                "$parts = ₹${group.subtotal.toInt()}"
                                            } else {
                                                "₹${group.subtotal.toInt()}"
                                            }
                                            Text(
                                                text = mathBreakdown,
                                                color = AccentGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        Button(
                            onClick = { viewModel.commitCurrentSelectionGroup() },
                            enabled = hasDraftSelection,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCyan,
                                disabledContainerColor = AccentCyan.copy(alpha = 0.2f)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (hasDraftSelection) Color.Black else AppTextColor().copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Add Group",
                                color = if (hasDraftSelection) Color.Black else AppTextColor().copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL LEDGER",
                                color = AppTextColor().copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%,.2f", overallTotal)}",
                                color = AccentGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(24.dp))
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
    val transactions by viewModel.activeHotelTransactions.collectAsStateWithLifecycle()
    val earliestTs by viewModel.earliestTransactionTimestamp.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    var monthOffset by remember { mutableStateOf(0) }
    var isDayWiseView by remember { mutableStateOf(false) }
    
    val filteredTransactions = remember(transactions, monthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        val targetMonth = cal.get(Calendar.MONTH)
        val targetYear = cal.get(Calendar.YEAR)
        
        transactions.filter {
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = it.timestamp
            txCal.get(Calendar.MONTH) == targetMonth && txCal.get(Calendar.YEAR) == targetYear
        }
    }
    
    val currentDisplayMonth = remember(monthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        monthFormat.format(cal.time)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getAppBackgroundGradient(userProfile.themeMode))
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragAmount = 0f },
                    onDragEnd = {
                        val swipeThreshold = 40.dp.toPx()
                        if (totalDragAmount > swipeThreshold) {
                            // Swipe Right -> Previous Month (Go back in time, if not already at earliest month)
                            if (!currentIsEarliestMonth) {
                                monthOffset -= 1
                            }
                        } else if (totalDragAmount < -swipeThreshold) {
                            // Swipe Left -> Next Month (Go forward in time, restricted to not exceed current month)
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
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.background(getGlassBorder(), CircleShape).bounceClick()
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTextColor())
                    }

                    Text(
                        text = "Meal Record Ledger",
                        color = AppTextColor(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { isDayWiseView = !isDayWiseView }, modifier = Modifier.background(getGlassBorder(), CircleShape).bounceClick()) {
                        Icon(if (isDayWiseView) Icons.Default.List else Icons.Default.DateRange, contentDescription = "Toggle View", tint = AppTextColor())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { monthOffset -= 1 }, enabled = !isEarliestMonth, modifier = Modifier.bounceClick()) {
                        Icon(Icons.Default.ChevronLeft, "Previous Month", tint = if (!isEarliestMonth) AppTextColor() else AppTextColor().copy(alpha = 0.3f))
                    }
                    Text(currentDisplayMonth, color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { monthOffset += 1 }, enabled = monthOffset < 0) {
                        Icon(Icons.Default.ChevronRight, "Next Month", tint = if (monthOffset < 0) AppTextColor() else AppTextColor().copy(alpha = 0.3f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (filteredTransactions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        EmptyStateCard(message = "No past meal logs for $currentDisplayMonth.")
                    }
                } else {
                    if (isDayWiseView) {
                        val dayFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
                        val dayGroups = remember(filteredTransactions) {
                            filteredTransactions.groupBy { tx ->
                                dayFormat.format(Date(tx.timestamp))
                            }.toSortedMap(compareByDescending {
                                dayFormat.parse(it)?.time ?: 0L
                            })
                        }
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(dayGroups.keys.toList()) { dateStr ->
                                val dayTxs = dayGroups[dateStr] ?: emptyList()
                                val amountsStr = dayTxs.joinToString(" + ") { it.totalAmount.toInt().toString() }
                                val dayTotal = dayTxs.sumOf { it.totalAmount }
                                
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
                                                    tint = AccentCyan,
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
                                                text = "₹${dayTotal.toInt()}",
                                                color = AccentGreen,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "$dateStr → $amountsStr = ${dayTotal.toInt()}",
                                            color = AppTextColor().copy(alpha = 0.7f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredTransactions, key = { it.id }) { tx ->
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
                                                tint = if (tx.isNight) AccentCyan else AccentGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = sdf.format(Date(tx.timestamp)),
                                                color = AppTextColor(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        val groups = com.example.data.model.TransactionSerializer.deserializeGroups(tx.itemsSerialized)
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
                                                    text = "Group ${idx + 1}: $grpSummary (₹${grp.subtotal.toInt()})",
                                                    color = AppTextColor().copy(alpha = 0.65f),
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else {
                                            val items = TransactionSerializer.deserialize(tx.itemsSerialized)
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

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "₹${tx.totalAmount.toInt()}",
                                            color = AccentGreen,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        var showMenu by remember { mutableStateOf(false) }
                                        var showDeleteConfirm by remember { mutableStateOf(false) }

                                        if (showDeleteConfirm) {
                                            com.example.ui.components.Tier1DeleteDialog(
                                                itemTitle = "Transaction (₹${tx.totalAmount})",
                                                onConfirm = { viewModel.deleteTransaction(tx); showDeleteConfirm = false },
                                                onDismiss = { showDeleteConfirm = false }
                                            )
                                        }

                                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                            IconButton(
                                                onClick = { showMenu = true },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "More Options",
                                                    tint = AppTextColor().copy(alpha = 0.6f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false },
                                                modifier = Modifier.background(getDialogBackground())
                                            ) {
                                                if (onEditTransaction != null) {
                                                    DropdownMenuItem(
                                                        text = { Text("Edit Transaction", color = AccentCyan) },
                                                        onClick = {
                                                            showMenu = false
                                                            onEditTransaction(tx)
                                                        },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = null,
                                                                tint = AccentCyan,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    )
                                                }
                                                DropdownMenuItem(
                                                    text = { Text("Delete Transaction", color = AccentPink) },
                                                    onClick = {
                                                        showMenu = false
                                                        showDeleteConfirm = true
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = null,
                                                            tint = AccentPink,
                                                            modifier = Modifier.size(16.dp)
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
                }
                
                if (filteredTransactions.isNotEmpty()) {
                    val grandTotal = filteredTransactions.sumOf { it.totalAmount }
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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
                                    text = "Monthly Ledger Total",
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
    val borderColor = if (isLight) {
        if (isSelected) Color(0xFF0097A7) else Color(0x22000000)
    } else {
        if (isSelected) AccentCyan else Color(0x2BFFFFFF)
    }
    val addBtnBg = if (isLight) Color(0xFF0097A7).copy(alpha = 0.12f) else AccentCyan.copy(alpha = 0.15f)
    val addBtnBorder = if (isLight) Color(0xFF0097A7).copy(alpha = 0.35f) else AccentCyan.copy(alpha = 0.4f)
    val addBtnText = if (isLight) Color(0xFF00838F) else AccentCyan

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
                            if (isLight) Color(0xFF0097A7).copy(alpha = 0.18f) else AccentCyan.copy(alpha = 0.25f),
                            RoundedCornerShape(17.dp)
                        )
                        .border(
                            1.dp,
                            if (isLight) Color(0xFF0097A7).copy(alpha = 0.5f) else AccentCyan.copy(alpha = 0.6f),
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
    onDismiss: () -> Unit,
    onSave: (name: String, advanced: Double, phone: String, loc: String, upi: String, qr: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB3000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(24.dp),
            color = getDialogBackground()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 840.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                // Top header bar with a Back button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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
                        text = if (hotel == null) {
                            if (prefLanguage == "bn") "নতুন হোটেল যুক্ত করুন" else "Register Establishment"
                        } else {
                            if (prefLanguage == "bn") "হোটেল সংশোধন" else "Edit Establishment Details"
                        },
                        color = AppTextColor(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (hotel != null && hotelsCount > 1 && onDelete != null) {
                        var showHotelMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showHotelMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = AppTextColor())
                            }
                            DropdownMenu(
                                expanded = showHotelMenu,
                                onDismissRequest = { showHotelMenu = false },
                                modifier = Modifier.background(getDialogBackground())
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Move to Bin", color = AccentPink) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AccentPink, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showHotelMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier.weight(1f).testTag("save_hotel_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hotel == null) {
                            if (prefLanguage == "bn") "যোগ করুন" else "Register"
                        } else {
                            if (prefLanguage == "bn") "সংরক্ষণ" else "Save"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("cancel_hotel_btn")) {
                Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AppTextColor())
            }
            } // Close Column
        } // Close Box
        } // Close Surface
    } // Close Box
    } // Close Dialog
} // Close UnifiedHotelDialog

@Composable
fun HotelInfoDetailRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelDetailsBottomSheet(
    hotel: com.example.data.model.HotelEntity,
    isBengali: Boolean,
    viewModel: com.example.ui.viewmodel.FinancialViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var previewQrUri by remember { mutableStateOf<String?>(null) }
    var qrExportPayload by remember { mutableStateOf<com.example.data.ExportResult.Success?>(null) }
    var qrExportSessionId by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = getDialogBackground(),
            border = BorderStroke(1.dp, getGlassBorder())
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hotel.name,
                        color = AppTextColor(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBengali) "হোটেলের বিস্তারিত বিবরণ" else "Hotel Information & Details",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(38.dp)
                        .background(AccentCyan.copy(alpha = 0.15f), CircleShape)
                        .bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Info Card Group
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTextColor().copy(alpha = 0.04f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, getGlassBorder().copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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

                    HorizontalDivider(color = AppTextColor().copy(alpha = 0.06f))

                    // Advanced Payment
                    HotelInfoDetailRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = AccentCyan,
                        label = if (isBengali) "অগ্রিম জমা" else "Advanced Payment",
                        value = "₹${hotel.advancedPayment.toInt()}"
                    )

                    HorizontalDivider(color = AppTextColor().copy(alpha = 0.06f))

                    // Phone Number
                    HotelInfoDetailRow(
                        icon = Icons.Default.Phone,
                        iconTint = Color(0xFF81C784),
                        label = if (isBengali) "ফোন নম্বর" else "Phone Number",
                        value = hotel.phoneNumber.ifBlank { if (isBengali) "প্রদান করা হয়নি" else "Not provided" },
                        actionIcon = if (hotel.phoneNumber.isNotBlank()) Icons.Default.Call else null,
                        onAction = {
                            if (hotel.phoneNumber.isNotBlank()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${hotel.phoneNumber}"))
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            }
                        }
                    )

                    HorizontalDivider(color = AppTextColor().copy(alpha = 0.06f))

                    // UPI ID
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
                        }
                    )
                }
            }

            // QR Codes Section
            val qrUris = hotel.qrCodeUri.split(",").filter { it.isNotBlank() }
            if (qrUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (isBengali) "পেমেন্ট কিউআর কোড (${qrUris.size})" else "Payment QR Codes (${qrUris.size})",
                    color = AppTextColor(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    qrUris.forEachIndexed { idx, uri ->
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { previewQrUri = uri }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "QR Code ${idx + 1}",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))


            // Export via QR Button
            Button(
                onClick = { 
                    viewModel.exportHotelViaQr(hotel.id) { result ->
                        when(result) {
                            is com.example.data.ExportResult.Success -> {
                                // Start the QR stream
                                viewModel.setQrExportPayload(result, java.util.UUID.randomUUID().toString())
                            }
                            is com.example.data.ExportResult.Error -> {
                                android.widget.Toast.makeText(context, "Export failed: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).bounceClick(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA68C8)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isBengali) "কিউআর মাধ্যমে এক্সপোর্ট" else "Export via QR", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Close & Edit buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp).bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTextColor().copy(alpha = 0.2f))
                ) {
                    Text(if (isBengali) "বন্ধ করুন" else "Close", color = AppTextColor(), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1.2f).height(48.dp).bounceClick(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBengali) "সম্পাদনা করুন" else "Edit Hotel", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    }

    if (previewQrUri != null) {
        com.example.ui.components.FullScreenQRPreviewDialog(
            qrUri = previewQrUri!!,
            title = "Hotel Payment QR",
            onDismiss = { previewQrUri = null }
        )
    }
    

    

}
