package com.example.ui.screens

import androidx.compose.foundation.shape.CircleShape


import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.animation.*

import androidx.compose.animation.core.tween

import androidx.compose.ui.window.Dialog

import androidx.compose.ui.window.DialogProperties

import androidx.compose.foundation.*

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.data.model.MeterReading

import com.example.data.model.UtilityBill

import com.example.ui.components.*

import com.example.ui.components.SlidingDatePickerModal

import com.example.ui.viewmodel.MeterReadingUiState

import com.example.ui.viewmodel.MeterReadingViewModel

import androidx.compose.ui.text.input.ImeAction

import java.text.SimpleDateFormat

import java.util.*

import com.example.ui.components.MeterDialPicker

import com.example.ui.components.CylindricalDateTimePicker

import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun BillTrackingTab(
    viewModel: MeterReadingViewModel,
    userProfile: com.example.data.model.UserProfileEntity,
    financialViewModel: FinancialViewModel? = null,
    onOpenUtilityConfig: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLight = isAppLightMode()
    val language = userProfile.language
    val currency = userProfile.currencySymbol

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        when (val state = uiState) {
            is MeterReadingUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            }
            is MeterReadingUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: ${state.message}",
                        color = AccentPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            is MeterReadingUiState.Success -> {
                if (isWide) {
                    // Responsive side-by-side layout for tablets / foldables
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            DailyReadingInputScreen(
                                viewModel = viewModel,
                                successState = state,
                                language = language,
                                currency = currency,
                                isLight = isLight,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(modifier = Modifier.weight(1.2f)) {
                            BillSummaryScreen(
                                viewModel = viewModel,
                                successState = state,
                                language = language,
                                currency = currency,
                                isLight = isLight,
                                modifier = Modifier.fillMaxSize(),
                                financialViewModel = financialViewModel
                            )
                        }
                    }
                } else {
                    // Mobile portrait view with a fast toggle at the top
                    var subTab by remember { mutableStateOf("Log") } // "Log" or "Summary"

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val tabs = listOf("Log", "Summary")
                            tabs.forEach { tab ->
                                val isSelected = subTab == tab
                                val label = if (language == "bn") {
                                    if (tab == "Log") "রিডিং এন্ট্রি" else "বিল সারসংক্ষেপ"
                                } else {
                                    if (tab == "Log") "Log Reading" else "Bill Summary"
                                }

                                val tabBg = if (isSelected) AccentCyan.copy(alpha = 0.2f) else Color.Transparent
                                val borderBrush = Brush.verticalGradient(
                                    if (isSelected) listOf(AccentCyan, AccentCyan.copy(alpha = 0.3f))
                                    else listOf(AppTextColor().copy(alpha = 0.1f), AppTextColor().copy(alpha = 0.1f))
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .bounceClick()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tabBg)
                                        .border(1.dp, borderBrush, RoundedCornerShape(12.dp))
                                        .clickable { subTab = tab }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            AnimatedContent(
                                targetState = subTab,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                                },
                                label = "sub_tab_transition"
                            ) { activeSubTab ->
                                when (activeSubTab) {
                                    "Log" -> DailyReadingInputScreen(
                                        viewModel = viewModel,
                                        successState = state,
                                        language = language,
                                        currency = currency,
                                        isLight = isLight,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    "Summary" -> BillSummaryScreen(
                                        viewModel = viewModel,
                                        successState = state,
                                        language = language,
                                        currency = currency,
                                        isLight = isLight,
                                        modifier = Modifier.fillMaxSize(),
                                        financialViewModel = financialViewModel,
                                        onOpenUtilityConfig = onOpenUtilityConfig
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

@Composable
fun DailyReadingInputScreen(
    viewModel: MeterReadingViewModel,
    successState: MeterReadingUiState.Success,
    language: String,
    currency: String,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val latestMeterReading by viewModel.latestMeterReading.collectAsStateWithLifecycle()
    val initialReading = latestMeterReading?.totalReading ?: 0.0
    
    var totalReadingValue by remember(initialReading) { mutableStateOf(initialReading) }
    var readingDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    val formatter = remember { SimpleDateFormat("dd MMM, yyyy  HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (language == "bn") "মিটার রিডিং নথিবদ্ধ করুন" else "Log Daily Meter Reading",
                        color = AppTextColor(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Date selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTextColor().copy(alpha = 0.05f))
                            .clickable { showDateTimePicker = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Select Date Time",
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "রিডিংয়ের সময়:" else "Reading Timestamp:",
                                color = AppTextColor().copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatter.format(Date(readingDateMillis)),
                            color = AccentCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (language == "bn") "মিটারের রিডিং সেট করুন" else "Set Meter Total Reading",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    MeterDialPicker(
                        initialValue = totalReadingValue,
                        onValueChange = { totalReadingValue = it },
                        textColor = AppTextColor(),
                        accentColor = AccentCyan
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.insertReading(totalReadingValue, readingDateMillis)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .bounceClick()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "রিডিং সংরক্ষণ করুন" else "Save Daily Reading",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (language == "bn") "সাম্প্রতিক এন্ট্রি" else "Recent Meter Log",
                color = AppTextColor(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (successState.readings.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Empty",
                                tint = AppTextColor().copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == "bn") "কোনো রিডিং পাওয়া যায়নি" else "No readings recorded yet.",
                                color = AppTextColor().copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            val initialReadingId = successState.readings.minByOrNull { it.readingDate }?.id
            
            items(successState.readings.take(20), key = { it.id }) { reading ->
                val isInitialReading = reading.id == initialReadingId
                val readingColor = if (isInitialReading || reading.isPaid) AppTextColor() else Color.Red
                val displayFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                
                // Calculate per day usage
                val previousReading = successState.readings.filter { it.readingDate < reading.readingDate }
                    .maxByOrNull { it.readingDate }
                
                val perDayUsage = if (previousReading != null) {
                    val units = reading.totalReading - previousReading.totalReading
                    val timeDiff = reading.readingDate - previousReading.readingDate
                    val days = timeDiff.toDouble() / (24 * 60 * 60 * 1000)
                    if (days > 0) units / days else 0.0
                } else null

                var showMenu by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }

                if (showDeleteDialog) {
                    com.example.ui.components.Tier1DeleteDialog(
                        itemTitle = "Meter Reading (${reading.totalReading})",
                        onConfirm = { viewModel.deleteReading(reading) },
                        onDismiss = { showDeleteDialog = false }
                    )
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflineBolt,
                                    contentDescription = "Bolt",
                                    tint = if (isInitialReading || reading.isPaid) AccentCyan else Color.Red,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${reading.totalReading} Total",
                                        color = readingColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!isInitialReading && !reading.isPaid) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (language == "bn") "(বাকি)" else "(Unpaid)",
                                            color = Color.Red,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = displayFormatter.format(Date(reading.readingDate)),
                                    color = AppTextColor().copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (perDayUsage != null) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "Usage: %.2f units/day", perDayUsage),
                                        color = AccentCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = AppTextColor().copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(getDialogBackground())
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit", color = AppTextColor()) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        showEditDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Remove to bin", color = AccentPink) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AccentPink, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                if (showEditDialog) {
                    var editTotalValue by remember { mutableDoubleStateOf(reading.totalReading) }
                    var editDateMillis by remember { mutableLongStateOf(reading.readingDate) }
                    var showDatePickerInside by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { showEditDialog = false },
                        containerColor = getDialogBackground(),
                        shape = RoundedCornerShape(24.dp),
                        title = { Text("Edit Meter Reading", color = AppTextColor()) },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppTextColor().copy(alpha = 0.05f))
                                        .clickable { showDatePickerInside = true }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Timestamp:", color = AppTextColor().copy(alpha = 0.6f), fontSize = 13.sp)
                                    Text(displayFormatter.format(Date(editDateMillis)), color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                MeterDialPicker(
                                    initialValue = editTotalValue,
                                    onValueChange = { editTotalValue = it },
                                    textColor = AppTextColor(),
                                    accentColor = AccentCyan
                                )
                                
                                if (showDatePickerInside) {
                                    AlertDialog(
                                        onDismissRequest = { showDatePickerInside = false },
                                        containerColor = getDialogBackground(),
                                        title = { Text("Set Date & Time", color = AppTextColor(), fontSize = 16.sp) },
                                        text = {
                                            CylindricalDateTimePicker(
                                                initialDateTime = editDateMillis,
                                                onDateTimeChange = { editDateMillis = it },
                                                textColor = AppTextColor()
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showDatePickerInside = false }) {
                                                Text("OK", color = AccentCyan)
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.updateReading(reading.copy(totalReading = editTotalValue, readingDate = editDateMillis))
                                    showEditDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                            ) {
                                Text("Update", color = Color.Black)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditDialog = false }) {
                                Text("Cancel", color = AppTextColor().copy(alpha = 0.6f))
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDateTimePicker) {
        AlertDialog(
            onDismissRequest = { showDateTimePicker = false },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (language == "bn") "তারিখ ও সময় সেট করুন" else "Set Date & Time",
                    color = AppTextColor(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                CylindricalDateTimePicker(
                    initialDateTime = readingDateMillis,
                    onDateTimeChange = { readingDateMillis = it },
                    textColor = AppTextColor()
                )
            },
            confirmButton = {
                TextButton(onClick = { showDateTimePicker = false }) {
                    Text("OK", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun BillSummaryScreen(
    viewModel: MeterReadingViewModel,
    successState: MeterReadingUiState.Success,
    language: String,
    currency: String,
    isLight: Boolean,
    modifier: Modifier = Modifier,
    financialViewModel: FinancialViewModel? = null,
    onOpenUtilityConfig: () -> Unit = {}
) {
    val formatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    
    // Logic: Default start date to the oldest unpaid reading
    val defaultStart = remember(successState.readings, successState.bills) {
        val lastPaidBill = successState.bills.filter { it.isPaid }.maxByOrNull { it.endDate }
        lastPaidBill?.endDate ?: successState.readings.minByOrNull { it.readingDate }?.readingDate ?: (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
    }
    val defaultEnd = remember { System.currentTimeMillis() }

    var startMillis by remember(successState.readings.size) { mutableStateOf(defaultStart) }
    var endMillis by remember { mutableStateOf(defaultEnd) }
    
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val fixedCharge by viewModel.fixedServiceCharge.collectAsStateWithLifecycle()

    var showGenerateBillDialog by remember { mutableStateOf(false) }
    var deletingBill by remember { mutableStateOf<UtilityBill?>(null) }

    if (deletingBill != null) {
        com.example.ui.components.Tier1DeleteDialog(
            itemTitle = "Utility Bill (${deletingBill?.totalUnits} units)",
            onConfirm = {
                deletingBill?.let { viewModel.deleteUtilityBill(it) }
                deletingBill = null
            },
            onDismiss = { deletingBill = null }
        )
    }

    if (successState.readings.isEmpty() && successState.rates.isEmpty() && fixedCharge == 0.0) {
        EmptyStateCard(message = if (language == "bn") "কোনো রিডিং যোগ করা হয়নি এবং রেট সেট করা হয়নি।" else "No readings added and rates are not set. Start logging to generate bills.")
        return
    }

    // Check if range includes paid session
    val paidSessionsInRange = successState.readings.filter { it.readingDate in startMillis..endMillis && it.isPaid }
    val includesPaid = paidSessionsInRange.isNotEmpty()

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Billing Period Selector Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "bn") "বিল হিসাব করার সময়সীমা" else "Billing Period Selection",
                            color = AppTextColor(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onOpenUtilityConfig,
                            modifier = Modifier
                                .size(32.dp)
                                .background(AppTextColor().copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Utility Configuration",
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (includesPaid) {
                        val firstPaid = paidSessionsInRange.minByOrNull { it.readingDate }?.readingDate ?: 0L
                        val lastPaid = paidSessionsInRange.maxByOrNull { it.readingDate }?.readingDate ?: 0L
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentPink.copy(alpha = 0.12f))
                                .border(1.dp, AccentPink.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AccentPink, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == "bn") 
                                        "সতর্কতা: এই সময়সীমার মধ্যে পরিশোধিত সেশন রয়েছে (${formatter.format(Date(firstPaid))} থেকে ${formatter.format(Date(lastPaid))})" 
                                        else "Strong Warning: Selected range includes paid sessions from ${formatter.format(Date(firstPaid))} to ${formatter.format(Date(lastPaid))}.",
                                    color = AccentPink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == "bn") "শুরুর তারিখ" else "Start Date",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppTextColor().copy(alpha = 0.05f))
                                    .clickable { showStartPicker = true }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatter.format(Date(startMillis)),
                                    color = AccentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.CalendarToday, contentDescription = "Start", tint = AccentCyan, modifier = Modifier.size(14.dp))
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == "bn") "শেষের তারিখ" else "End Date",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppTextColor().copy(alpha = 0.05f))
                                    .clickable { showEndPicker = true }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatter.format(Date(endMillis)),
                                    color = AccentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.CalendarToday, contentDescription = "End", tint = AccentCyan, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // Bill calculation breakdown card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "bn") "প্রাক্কলিত বিদ্যুৎ বিল" else "Projected Utility Bill",
                            color = AppTextColor(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = String.format(Locale.getDefault(), "%s %.2f", currency, successState.currentProjectedBill),
                        color = AccentCyan,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTextColor().copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (language == "bn") "মোট ব্যবহৃত ইউনিট" else "Aggregated Units",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${successState.totalUnitsConsumedInRange} kWh",
                                color = AppTextColor(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (language == "bn") "ইউনিট প্রতি রেট" else "Rate/Unit",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            val activeRate = successState.rates.maxByOrNull { it.effectiveDate }?.ratePerUnit ?: 0.0
                            Text(
                                text = String.format(Locale.getDefault(), "%s %.2f", currency, activeRate),
                                color = AppTextColor(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (language == "bn") "সার্ভিস চার্জ" else "Service Charge",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%s %.0f", currency, fixedCharge),
                                color = AppTextColor(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { showGenerateBillDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, AccentCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Generate", tint = AccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "বিল চূড়ান্ত করুন" else "Finalize & Record Bill",
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (language == "bn") "পরিশোধিত বিলের ইতিহাস" else "Paid Bill History",
                color = AppTextColor(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
            )
        }

        val paidBills = successState.bills.filter { it.isPaid }
        if (paidBills.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = AppTextColor().copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == "bn") "কোনো পরিশোধিত বিল নেই" else "No paid bill records found.",
                                color = AppTextColor().copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            items(paidBills, key = { it.id }) { bill ->
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
                            Column {
                                Text(
                                    text = String.format(Locale.getDefault(), "%s %.2f", currency, bill.calculatedAmount),
                                    color = AccentCyan,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${bill.totalUnits} Units (${formatter.format(Date(bill.startDate))} - ${formatter.format(Date(bill.endDate))})",
                                    color = AppTextColor().copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }

                            val statusBg = if (bill.isPaid) Color(0x3300FF87) else AccentPink.copy(alpha = 0.2f)
                            val statusBorder = if (bill.isPaid) Color(0xFF00FF87) else AccentPink
                            val statusText = if (bill.isPaid) {
                                if (language == "bn") "পরিশোধিত" else "PAID"
                            } else {
                                if (language == "bn") "বকেয়া" else "UNPAID"
                            }

                            Box(
                                modifier = Modifier
                                    .bounceClick()
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, statusBorder, RoundedCornerShape(6.dp))
                                    .background(statusBg, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.toggleBillPayment(bill) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    color = if (bill.isPaid) Color(0xFF00FF87) else AccentPink,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == "bn") "শেষ তারিখ: ${formatter.format(Date(bill.dueDate))}" else "Due Date: ${formatter.format(Date(bill.dueDate))}",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        SlidingDatePickerModal(
            initialDateMillis = startMillis,
            onDateSelected = { 
                startMillis = it
                viewModel.setCalculationDateRange(it, endMillis) 
            },
            onDismiss = { showStartPicker = false },
            textColor = AppTextColor(),
            backgroundColor = getDialogBackground()
        )
    }

    if (showEndPicker) {
        SlidingDatePickerModal(
            initialDateMillis = endMillis,
            onDateSelected = { 
                endMillis = it
                viewModel.setCalculationDateRange(startMillis, it) 
            },
            onDismiss = { showEndPicker = false },
            textColor = AppTextColor(),
            backgroundColor = getDialogBackground()
        )
    }

    if (showGenerateBillDialog) {
        var isPaidSelected by remember { mutableStateOf(false) }
        var dueDateMillis by remember { mutableStateOf(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L) }
        var showDueDatePicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showGenerateBillDialog = false },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (language == "bn") "ইউটিলিটি বিল তৈরি করুন" else "Record Utility Bill",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (language == "bn") {
                            "আপনি কি নিচের প্রাক্কলন অনুযায়ী একটি বিল নথিবদ্ধ করতে চান?"
                        } else {
                            "Would you like to finalize and record this billing calculation?"
                        },
                        color = AppTextColor().copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${if (language == "bn") "মোট ইউনিট:" else "Total Units:"} ${successState.totalUnitsConsumedInRange} kWh",
                            color = AppTextColor(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (language == "bn") "মোট বিল:" else "Bill Amount:"} $currency ${successState.currentProjectedBill}",
                            color = AccentCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (language == "bn") "সময়সীমা:" else "Period:"} ${formatter.format(Date(startMillis))} - ${formatter.format(Date(endMillis))}",
                            color = AppTextColor().copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppTextColor().copy(alpha = 0.05f))
                            .clickable { showDueDatePicker = true }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (language == "bn") "বিল পরিশোধের শেষ সময়:" else "Due Date:", color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
                        Text(formatter.format(Date(dueDateMillis)), color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "bn") "পরিশোধ করা হয়েছে?" else "Mark as Paid?",
                            color = AppTextColor(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        GlassSwitch(
                            checked = isPaidSelected,
                            onCheckedChange = { isPaidSelected = it },
                            activeText = "PAID",
                            inactiveText = "UNPAID"
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.insertUtilityBill(
                            startDate = startMillis,
                            endDate = endMillis,
                            totalUnits = successState.totalUnitsConsumedInRange,
                            amount = successState.currentProjectedBill,
                            dueDate = dueDateMillis,
                            isPaid = isPaidSelected
                        )
                        showGenerateBillDialog = false
                    }
                ) {
                    Text(if (language == "bn") "নিশ্চিত" else "Confirm", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateBillDialog = false }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel", color = AppTextColor().copy(alpha = 0.5f))
                }
            }
        )

        if (showDueDatePicker) {
            SlidingDatePickerModal(
                initialDateMillis = dueDateMillis,
                onDateSelected = { dueDateMillis = it },
                onDismiss = { showDueDatePicker = false },
                textColor = AppTextColor(),
                backgroundColor = getDialogBackground()
            )
        }
    }
}
