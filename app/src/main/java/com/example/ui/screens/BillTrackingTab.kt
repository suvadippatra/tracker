package com.example.ui.screens
import androidx.compose.ui.draw.rotate

import androidx.activity.compose.BackHandler
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
import com.example.utils.Formatters

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
                    CircularProgressIndicator(color = AppAccentColor())
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

                                val primaryCol = MaterialTheme.colorScheme.primary
                                val tabBg = if (isSelected) primaryCol.copy(alpha = 0.16f) else Color.Transparent
                                val borderCol = if (isSelected) primaryCol.copy(alpha = 0.55f) else AppTextColor().copy(alpha = 0.12f)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .bounceClick()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tabBg)
                                        .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                                        .clickable { subTab = tab }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) primaryCol else AppTextColor(),
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
                            when (subTab) {
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

// --- Pre-computed UI Display Models for Zero-Allocation List Scrolling ---
data class MeterReadingDisplayItem(
    val id: String,
    val reading: MeterReading,
    val isInitial: Boolean,
    val isPaid: Boolean,
    val formattedTotal: String,
    val formattedDate: String,
    val perDayUsageText: String?
)

data class UtilityBillDisplayItem(
    val id: String,
    val bill: UtilityBill,
    val formattedAmount: String,
    val formattedSubtitle: String,
    val formattedDueDate: String,
    val statusText: String,
    val isPaid: Boolean
)

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

    val formatter = remember { Formatters.getSimpleDateFormat("dd MMM, yyyy  HH:mm") }

    val displayItems = remember(successState.readings, language) {
        val sortedAsc = successState.readings.sortedBy { it.readingDate }
        val initialId = sortedAsc.firstOrNull()?.id
        
        val usageMap = mutableMapOf<String, Double>()
        for (i in 1 until sortedAsc.size) {
            val curr = sortedAsc[i]
            val prev = sortedAsc[i - 1]
            val units = curr.totalReading - prev.totalReading
            val timeDiff = curr.readingDate - prev.readingDate
            val days = timeDiff.toDouble() / (24 * 60 * 60 * 1000)
            if (days > 0) {
                usageMap[curr.id] = units / days
            }
        }

        val sortedDesc = successState.readings.sortedByDescending { it.readingDate }
        sortedDesc.map { reading ->
            val isInitial = reading.id == initialId
            val usage = usageMap[reading.id]
            val usageText = if (usage != null) {
                String.format(Locale.getDefault(), "Usage: %.2f units/day", usage)
            } else null
            
            MeterReadingDisplayItem(
                id = reading.id,
                reading = reading,
                isInitial = isInitial,
                isPaid = reading.isPaid,
                formattedTotal = "${reading.totalReading} Total",
                formattedDate = Formatters.formatShortDate(reading.readingDate),
                perDayUsageText = usageText
            )
        }
    }

    var showAllLogReadings by remember { mutableStateOf(false) }

    val visibleLogItems = remember(displayItems, showAllLogReadings) {
        derivedStateOf {
            if (showAllLogReadings || displayItems.size <= 4) displayItems else displayItems.take(4)
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 156.dp)
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
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Select Date Time",
                                tint = AppAccentColor(),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "রিডিং সময়:" else "Reading Timestamp:",
                                color = AppTextColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatter.format(Date(readingDateMillis)),
                            color = AppAccentColor(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            maxLines = 2,
                            softWrap = true
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (language == "bn") "মিটারের রিডিং সেট করুন" else "Set Meter Total Reading",
                        color = AppTextColor(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    MeterDialPicker(
                        initialValue = totalReadingValue,
                        onValueChange = { totalReadingValue = it },
                        textColor = AppTextColor(),
                        accentColor = AppAccentColor()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.insertReading(totalReadingValue, readingDateMillis)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor()),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .bounceClick()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "রিডিং সংরক্ষণ করুন" else "Save Daily Reading",
                            color = Color.White,
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
                color = AppHeadingColor(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (displayItems.isEmpty()) {
            item(key = "empty_log_readings") {
                EmptyStateCard(message = if (language == "bn") "কোনো রিডিং পাওয়া যায়নি।" else "No meter readings recorded yet.")
            }
        } else {
            items(visibleLogItems.value, key = { it.id }, contentType = { "reading_log_item" }) { item ->
                val reading = item.reading
                val unpaidTextColor = AppUnpaidTextColor()
                val readingColor = if (item.isInitial || item.isPaid) AppTextColor() else unpaidTextColor

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
                                    .background(if (item.isInitial || item.isPaid) AppAccentColor().copy(alpha = 0.15f) else AppUnpaidBgColor(), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflineBolt,
                                    contentDescription = "Bolt",
                                    tint = if (item.isInitial || item.isPaid) AppAccentColor() else unpaidTextColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.formattedTotal,
                                        color = readingColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!item.isInitial && !item.isPaid) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (language == "bn") "(বাকি)" else "(Unpaid)",
                                            color = unpaidTextColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = item.formattedDate,
                                    color = AppSecondaryTextColor(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (item.perDayUsageText != null) {
                                    Text(
                                        text = item.perDayUsageText,
                                        color = AppAccentColor(),
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
                                    tint = AppTextColor(),
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
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(18.dp)) },
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
                                    Text(
                                        text = "Timestamp:",
                                        color = AppTextColor().copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Formatters.formatShortDate(editDateMillis),
                                        color = AppAccentColor(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                MeterDialPicker(
                                    initialValue = editTotalValue,
                                    onValueChange = { editTotalValue = it },
                                    textColor = AppTextColor(),
                                    accentColor = AppAccentColor()
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
                                                Text("OK", color = AppAccentColor())
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
                                colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor())
                            ) {
                                Text("Update", color = Color.White, fontWeight = FontWeight.Bold)
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

        if (displayItems.size > 4) {
            item(key = "toggle_view_all_readings") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { showAllLogReadings = !showAllLogReadings },
                        colors = ButtonDefaults.textButtonColors(contentColor = AppAccentColor()),
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = if (showAllLogReadings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showAllLogReadings) {
                                if (language == "bn") "কম দেখান" else "Show Less"
                            } else {
                                if (language == "bn") "সব দেখুন (${displayItems.size} টি এন্ট্রি)" else "View All (${displayItems.size} items)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showDateTimePicker) {
        BackHandler { showDateTimePicker = false }
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
                    Text("OK", color = AppAccentColor(), fontWeight = FontWeight.Bold)
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
    val formatter = remember { Formatters.getSimpleDateFormat("dd MMM, yyyy") }
    
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

    val currentCalculation = remember(startMillis, endMillis, successState.readings, successState.rates, fixedCharge) {
        viewModel.calculateDetailedBill(
            start = startMillis,
            end = endMillis,
            sortedReadings = successState.readings.sortedBy { it.readingDate },
            rates = successState.rates,
            fixedCharge = fixedCharge
        )
    }

    var showGenerateBillDialog by remember { mutableStateOf(false) }
    var deletingBill by remember { mutableStateOf<UtilityBill?>(null) }
    var billToEditStatus by remember { mutableStateOf<UtilityBill?>(null) }
    var editingStatusIsPaid by remember { mutableStateOf(false) }
    var editingStatusNote by remember { mutableStateOf("") }

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

    if (billToEditStatus != null) {
        val targetBill = billToEditStatus!!
        AlertDialog(
            onDismissRequest = { billToEditStatus = null },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (language == "bn") "পেমেন্ট স্ট্যাটাস ও নোট আপডেট" else "Update Payment Status & Notes",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${if (language == "bn") "বিল পরিমাণ:" else "Bill Amount:"} ${String.format(Locale.getDefault(), "%s %.2f", currency, targetBill.calculatedAmount)}",
                            color = AppAccentColor(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (language == "bn") "মোট ইউনিট:" else "Total Units:"} ${String.format(Locale.getDefault(), "%.2f", targetBill.totalUnits)} kWh",
                            color = AppTextColor(),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${if (language == "bn") "সময়সীমা:" else "Period:"} ${formatter.format(Date(targetBill.startDate))} - ${formatter.format(Date(targetBill.endDate))}",
                            color = AppSecondaryTextColor(),
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "bn") "পরিশোধের স্থিতি:" else "Payment Status:",
                            color = AppTextColor(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        GlassSwitch(
                            checked = editingStatusIsPaid,
                            onCheckedChange = { editingStatusIsPaid = it },
                            activeText = "PAID",
                            inactiveText = "UNPAID"
                        )
                    }

                    OutlinedTextField(
                        value = editingStatusNote,
                        onValueChange = { editingStatusNote = it },
                        label = { Text(if (language == "bn") "অতিরিক্ত নোট (ঐচ্ছিক)" else "Extra Notes (Optional)", color = AppSecondaryTextColor()) },
                        placeholder = { Text(if (language == "bn") "যেমন: ট্রানজাকশন আইডি / ক্যাশ পেমেন্ট..." else "e.g. Paid via UPI, Ref #1234...", color = AppSecondaryTextColor().copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AppAccentColor(),
                            unfocusedBorderColor = AppTextColor().copy(alpha = 0.2f),
                            cursorColor = AppAccentColor()
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateBillPaymentStatus(
                            bill = targetBill,
                            newIsPaid = editingStatusIsPaid,
                            note = editingStatusNote
                        )
                        billToEditStatus = null
                    }
                ) {
                    Text(if (language == "bn") "সংরক্ষণ" else "Save", color = AppAccentColor(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { billToEditStatus = null }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel", color = AppTextColor())
                }
            }
        )
    }

    if (successState.readings.isEmpty() && successState.rates.isEmpty() && fixedCharge == 0.0) {
        EmptyStateCard(message = if (language == "bn") "কোনো রিডিং যোগ করা হয়নি এবং রেট সেট করা হয়নি।" else "No readings added and rates are not set. Start logging to generate bills.")
        return
    }

    // Check if range includes paid session
    val paidSessionsInRange = remember(successState.readings, startMillis, endMillis) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val calS = cal.timeInMillis

        cal.timeInMillis = endMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val calE = cal.timeInMillis

        successState.readings.filter { reading ->
            reading.isPaid && reading.readingDate in (calS + 1)..calE
        }
    }
    val includesPaid = paidSessionsInRange.isNotEmpty()

    val paidBillDisplayItems = remember(successState.bills, currency, language) {
        successState.bills.filter { it.isPaid }.sortedByDescending { it.endDate }.map { bill ->
            UtilityBillDisplayItem(
                id = bill.id,
                bill = bill,
                formattedAmount = String.format(Locale.getDefault(), "%s %.2f", currency, bill.calculatedAmount),
                formattedSubtitle = "${String.format(Locale.getDefault(), "%.2f", bill.totalUnits)} Units (${Formatters.formatDayMonthYear(bill.startDate)} - ${Formatters.formatDayMonthYear(bill.endDate)})",
                formattedDueDate = if (bill.dueDate > 0) "Due: ${Formatters.formatDayMonthYear(bill.dueDate)}" else "",
                statusText = if (language == "bn") "পরিশোধিত" else "PAID",
                isPaid = true
            )
        }
    }

    var showAllPaidBills by remember { mutableStateOf(false) }

    val visiblePaidBillItems = remember(paidBillDisplayItems, showAllPaidBills) {
        derivedStateOf {
            if (showAllPaidBills || paidBillDisplayItems.size <= 4) paidBillDisplayItems else paidBillDisplayItems.take(4)
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 156.dp)
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
                            color = AppHeadingColor(),
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
                                tint = AppAccentColor(),
                                modifier = Modifier.size(16.dp).rotate(90f)
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
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AccentPink, modifier = Modifier.size(16.dp).rotate(90f))
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
                                color = AppTextColor(),
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
                                    color = AppAccentColor(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.CalendarToday, contentDescription = "Start", tint = AppAccentColor(), modifier = Modifier.size(14.dp))
                            }
                        }

                        IconButton(
                            onClick = {
                                val temp = startMillis
                                startMillis = endMillis
                                endMillis = temp
                                viewModel.setCalculationDateRange(startMillis, endMillis)
                            },
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppAccentColor().copy(alpha = 0.12f))
                                .border(1.dp, AppAccentColor().copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap Dates",
                                tint = AppAccentColor(),
                                modifier = Modifier.size(20.dp).rotate(90f)
                            )
                        }


                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == "bn") "শেষের তারিখ" else "End Date",
                                color = AppTextColor(),
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
                                    color = AppAccentColor(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.CalendarToday, contentDescription = "End", tint = AppAccentColor(), modifier = Modifier.size(14.dp))
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
                        text = String.format(Locale.getDefault(), "%s %.2f", currency, currentCalculation.totalCost),
                        color = AppAccentColor(),
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
                                color = AppSecondaryTextColor(),
                                fontSize = 11.sp
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f kWh", currentCalculation.totalUnits),
                                color = AppTextColor(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (language == "bn") "ইউনিট প্রতি রেট" else "Rate/Unit",
                                color = AppSecondaryTextColor(),
                                fontSize = 11.sp
                            )
                            val activeRates = currentCalculation.activeRates
                            val rateDisplay = if (activeRates.isEmpty()) {
                                val latest = successState.rates.maxByOrNull { it.effectiveDate }?.ratePerUnit ?: 0.0
                                String.format(Locale.getDefault(), "%s %.2f", currency, latest)
                            } else if (activeRates.size == 1) {
                                String.format(Locale.getDefault(), "%s %.2f", currency, activeRates.first())
                            } else {
                                activeRates.joinToString(", ") { String.format(Locale.getDefault(), "%s %.2f", currency, it) }
                            }
                            Text(
                                text = rateDisplay,
                                color = AppTextColor(),
                                fontSize = if (activeRates.size > 1) 12.sp else 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (language == "bn") "সার্ভিস চার্জ" else "Service Charge",
                                color = AppSecondaryTextColor(),
                                fontSize = 11.sp
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%s %.2f", currency, fixedCharge),
                                color = AppTextColor(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (currentCalculation.breakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppTextColor().copy(alpha = 0.04f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (language == "bn") "রেট অনুযায়ী বিভাজন:" else "Rate-wise Consumption Breakdown:",
                                color = AppSecondaryTextColor(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            currentCalculation.breakdown.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatter.format(Date(item.periodStart))} - ${formatter.format(Date(item.periodEnd))}: ${String.format(Locale.getDefault(), "%.2f", item.units)} kWh @ ${String.format(Locale.getDefault(), "%s %.2f", currency, item.ratePerUnit)}",
                                        color = AppTextColor(),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%s %.2f", currency, item.cost),
                                        color = AppAccentColor(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { showGenerateBillDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor().copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, AppAccentColor()),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Generate", tint = AppAccentColor())
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "বিল চূড়ান্ত করুন" else "Finalize & Record Bill",
                            color = AppAccentColor(),
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
                color = AppHeadingColor(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
            )
        }

        if (paidBillDisplayItems.isEmpty()) {
            item(key = "empty_paid_bills") {
                EmptyStateCard(message = if (language == "bn") "কোনো পরিশোধিত বিল নেই।" else "No paid bill records found.")
            }
        } else {
            items(visiblePaidBillItems.value, key = { it.id }, contentType = { "paid_bill_item" }) { item ->
                val bill = item.bill
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
                                    text = item.formattedAmount,
                                    color = AppPrimaryBlueColor(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.formattedSubtitle,
                                    color = AppSecondaryTextColor(),
                                    fontSize = 11.sp
                                )
                            }

                            val statusBg = AppPaidBgColor()
                            val statusBorder = AppPaidBorderColor()
                            val statusTextColor = AppPaidTextColor()

                            Box(
                                modifier = Modifier
                                    .bounceClick()
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, statusBorder, RoundedCornerShape(6.dp))
                                    .background(statusBg, RoundedCornerShape(6.dp))
                                    .clickable {
                                        billToEditStatus = bill
                                        editingStatusIsPaid = !bill.isPaid
                                        editingStatusNote = bill.note
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = item.statusText,
                                    color = statusTextColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (bill.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppTextColor().copy(alpha = 0.04f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AppAccentColor().copy(alpha = 0.8f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = bill.note,
                                    color = AppTextColor(),
                                    fontSize = 11.sp
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
                                color = AppSecondaryTextColor(),
                                fontSize = 11.sp
                            )
                            
                            var showBillMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showBillMenu = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = AppTextColor(),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showBillMenu,
                                    onDismissRequest = { showBillMenu = false },
                                    modifier = Modifier.background(getDialogBackground())
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (bill.isPaid) "Mark as Unpaid & Note" else "Mark as Paid & Note", color = AppTextColor()) },
                                        leadingIcon = { Icon(if (bill.isPaid) Icons.Default.Close else Icons.Default.Check, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showBillMenu = false
                                            billToEditStatus = bill
                                            editingStatusIsPaid = !bill.isPaid
                                            editingStatusNote = bill.note
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (language == "bn") "নোট সম্পাদনা" else "Edit Note / Status", color = AppTextColor()) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showBillMenu = false
                                            billToEditStatus = bill
                                            editingStatusIsPaid = bill.isPaid
                                            editingStatusNote = bill.note
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Remove to bin", color = AccentPink) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AccentPink, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showBillMenu = false
                                            deletingBill = bill
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (paidBillDisplayItems.size > 4) {
                item(key = "toggle_view_all_paid_bills") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { showAllPaidBills = !showAllPaidBills },
                            colors = ButtonDefaults.textButtonColors(contentColor = AppAccentColor()),
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(
                                imageVector = if (showAllPaidBills) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showAllPaidBills) {
                                    if (language == "bn") "কম দেখান" else "Show Less"
                                } else {
                                    if (language == "bn") "সব দেখুন (${paidBillDisplayItems.size} টি বিল)" else "View All (${paidBillDisplayItems.size} items)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
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
        var billNote by remember { mutableStateOf("") }

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
                        color = AppTextColor(),
                        fontSize = 13.sp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${if (language == "bn") "মোট ইউনিট:" else "Total Units:"} ${String.format(Locale.getDefault(), "%.2f", currentCalculation.totalUnits)} kWh",
                            color = AppTextColor(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (language == "bn") "মোট বিল:" else "Bill Amount:"} ${String.format(Locale.getDefault(), "%s %.2f", currency, currentCalculation.totalCost)}",
                            color = AppAccentColor(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (language == "bn") "সময়সীমা:" else "Period:"} ${formatter.format(Date(startMillis))} - ${formatter.format(Date(endMillis))}",
                            color = AppSecondaryTextColor(),
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
                        Text(if (language == "bn") "বিল পরিশোধের শেষ সময়:" else "Due Date:", color = AppSecondaryTextColor(), fontSize = 12.sp)
                        Text(formatter.format(Date(dueDateMillis)), color = AppAccentColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

                    OutlinedTextField(
                        value = billNote,
                        onValueChange = { billNote = it },
                        label = { Text(if (language == "bn") "অতিরিক্ত নোট (ঐচ্ছিক)" else "Extra Notes (Optional)", color = AppSecondaryTextColor()) },
                        placeholder = { Text(if (language == "bn") "যেমন: মে মাসের বিল পরিশোধ..." else "e.g. Paid via UPI, Ref #1234...", color = AppSecondaryTextColor().copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AppAccentColor(),
                            unfocusedBorderColor = AppTextColor().copy(alpha = 0.2f),
                            cursorColor = AppAccentColor()
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.insertUtilityBill(
                            startDate = startMillis,
                            endDate = endMillis,
                            totalUnits = currentCalculation.totalUnits,
                            amount = currentCalculation.totalCost,
                            dueDate = dueDateMillis,
                            isPaid = isPaidSelected,
                            note = billNote
                        )
                        showGenerateBillDialog = false
                    }
                ) {
                    Text(if (language == "bn") "নিশ্চিত" else "Confirm", color = AppAccentColor(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateBillDialog = false }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel", color = AppTextColor())
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
