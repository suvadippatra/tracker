package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.example.ui.components.SlidingDatePickerModal
import com.example.ui.viewmodel.MeterReadingUiState
import com.example.ui.viewmodel.MeterReadingViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BillTrackingTab(
    viewModel: MeterReadingViewModel,
    userProfile: com.example.data.model.UserProfileEntity
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
                                modifier = Modifier.fillMaxSize()
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
                                .padding(horizontal = 20.dp, vertical = 8.dp),
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
                                .padding(horizontal = 16.dp)
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
                                        modifier = Modifier.fillMaxSize()
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
    val lastReadingValue = successState.readings.firstOrNull()?.unitsConsumed ?: 15.0
    var unitsStr by remember(lastReadingValue) { mutableStateOf(String.format(Locale.US, "%.1f", lastReadingValue)) }
    var readingDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val formatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            .clickable { showDatePicker = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Date",
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "রিডিংয়ের তারিখ:" else "Reading Date:",
                                color = AppTextColor().copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = formatter.format(Date(readingDateMillis)),
                            color = AccentCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (language == "bn") "ব্যবহৃত বিদ্যুতের ইউনিট" else "Units Consumed",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = unitsStr,
                            color = AccentCyan,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "kWh",
                            color = AppTextColor().copy(alpha = 0.4f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Bottom)
                        )
                    }

                    // Frictionless Stepper & Slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val current = unitsStr.toDoubleOrNull() ?: lastReadingValue
                                if (current > 0.1) {
                                    unitsStr = String.format(Locale.US, "%.1f", current - 0.5)
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .bounceClick()
                                .background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = AppTextColor())
                        }

                        val currentUnits = unitsStr.toDoubleOrNull() ?: lastReadingValue
                        Slider(
                            value = currentUnits.toFloat(),
                            onValueChange = {
                                unitsStr = String.format(Locale.US, "%.1f", it.toDouble())
                            },
                            valueRange = 0f..(lastReadingValue.toFloat() + 100f).coerceAtLeast(150f),
                            colors = SliderDefaults.colors(
                                thumbColor = AccentCyan,
                                activeTrackColor = AccentCyan,
                                inactiveTrackColor = AppTextColor().copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        )

                        IconButton(
                            onClick = {
                                val current = unitsStr.toDoubleOrNull() ?: lastReadingValue
                                unitsStr = String.format(Locale.US, "%.1f", current + 0.5)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .bounceClick()
                                .background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = AppTextColor())
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = unitsStr,
                        onValueChange = {
                            if (it.isEmpty() || it.toDoubleOrNull() != null || it == ".") {
                                unitsStr = it
                            }
                        },
                        label = { Text(if (language == "bn") "সরাসরি ইনপুট দিন" else "Direct Unit Entry", color = AppTextColor().copy(alpha = 0.4f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = AppTextColor().copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val finalUnits = unitsStr.toDoubleOrNull() ?: lastReadingValue
                            viewModel.insertReading(finalUnits, readingDateMillis)
                            unitsStr = String.format(Locale.US, "%.1f", lastReadingValue)
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
                            .padding(24.dp),
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
            items(successState.readings.take(8)) { reading ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflineBolt,
                                    contentDescription = "Bolt",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${reading.unitsConsumed} Units",
                                    color = AppTextColor(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatter.format(Date(reading.readingDate)),
                                    color = AppTextColor().copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteReading(reading) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = AccentPink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        SlidingDatePickerModal(
            initialDateMillis = readingDateMillis,
            onDateSelected = { readingDateMillis = it },
            onDismiss = { showDatePicker = false },
            textColor = AppTextColor(),
            backgroundColor = getDialogBackground()
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
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val startMillis by viewModel.calculationStartDate.collectAsStateWithLifecycle()
    val endMillis by viewModel.calculationEndDate.collectAsStateWithLifecycle()
    val flatRate by viewModel.flatRatePerUnit.collectAsStateWithLifecycle()
    val fixedCharge by viewModel.fixedServiceCharge.collectAsStateWithLifecycle()

    var showParamEdit by remember { mutableStateOf(false) }
    var showGenerateBillDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Billing Period Selector Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (language == "bn") "বিল হিসাব করার সময়সীমা" else "Billing Period Selection",
                        color = AppTextColor(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

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
                        .padding(20.dp),
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
                        IconButton(
                            onClick = { showParamEdit = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AccentCyan, modifier = Modifier.size(16.dp))
                        }
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
                            Text(
                                text = String.format(Locale.getDefault(), "%s %.2f", currency, flatRate),
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
                text = if (language == "bn") "ঐতিহাসিক বিলসমূহ" else "Generated Bill History",
                color = AppTextColor(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (successState.bills.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
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
                                text = if (language == "bn") "কোনো বিল জেনারেট করা হয়নি" else "No bills generated yet.",
                                color = AppTextColor().copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            items(successState.bills) { bill ->
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

                            IconButton(
                                onClick = { viewModel.deleteUtilityBill(bill) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Bill", tint = AccentPink, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        SlidingDatePickerModal(
            initialDateMillis = startMillis,
            onDateSelected = { viewModel.setCalculationDateRange(it, endMillis) },
            onDismiss = { showStartPicker = false },
            textColor = AppTextColor(),
            backgroundColor = getDialogBackground()
        )
    }

    if (showEndPicker) {
        SlidingDatePickerModal(
            initialDateMillis = endMillis,
            onDateSelected = { viewModel.setCalculationDateRange(startMillis, it) },
            onDismiss = { showEndPicker = false },
            textColor = AppTextColor(),
            backgroundColor = getDialogBackground()
        )
    }

    if (showParamEdit) {
        var rateInput by remember { mutableStateOf("") }
        var rateDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
        var showRateDatePicker by remember { mutableStateOf(false) }
        var chargeInput by remember { mutableStateOf(fixedCharge.toString()) }

        AlertDialog(
            onDismissRequest = { showParamEdit = false },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (language == "bn") "ইউটিলিটি প্যারামিটার সেটিংস" else "Utility Configuration",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = chargeInput,
                            onValueChange = { chargeInput = it },
                            label = { Text(if (language == "bn") "সার্ভিস চার্জ (₹)" else "Fixed Service Charge (₹)", color = AppTextColor().copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AppTextColor(),
                                unfocusedTextColor = AppTextColor(),
                                focusedBorderColor = AccentCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text(
                            text = if (language == "bn") "তারিখ ভিত্তিক রেট পরিবর্তন" else "Historical Rates over Time",
                            color = AppTextColor(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = if (language == "bn") "নির্দিষ্ট তারিখ থেকে নতুন রেট স্বয়ংক্রিয়ভাবে কার্যকর হবে।" else "New rates apply automatically starting from their effective dates.",
                            color = AppTextColor().copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Add new rate change row
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (language == "bn") "নতুন রেট যুক্ত করুন" else "Add New Rate Change",
                                    color = AccentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = rateInput,
                                        onValueChange = { rateInput = it },
                                        placeholder = { Text("Rate e.g. 8.5", fontSize = 12.sp, color = AppTextColor().copy(alpha = 0.4f)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = AppTextColor(),
                                            unfocusedTextColor = AppTextColor(),
                                            focusedBorderColor = AccentCyan
                                        ),
                                        modifier = Modifier.weight(1.2f)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AppTextColor().copy(alpha = 0.05f))
                                            .clickable { showRateDatePicker = true }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = formatter.format(Date(rateDateMillis)),
                                            color = AccentCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val r = rateInput.toDoubleOrNull()
                                            if (r != null) {
                                                viewModel.insertElectricityRate(r, rateDateMillis)
                                                rateInput = ""
                                            }
                                        },
                                        modifier = Modifier.background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = AccentCyan)
                                    }
                                }
                            }
                        }
                    }

                    // Configured historical rates list
                    if (successState.rates.isEmpty()) {
                        item {
                            Text(
                                text = if (language == "bn") "কোনো কাস্টম রেট সেট করা নেই (ডিফল্ট: ₹৭.৫০)" else "No custom rates set (Default: ₹7.50/unit)",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else {
                        items(successState.rates) { rate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppTextColor().copy(alpha = 0.03f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${currency} ${rate.ratePerUnit}/unit",
                                        color = AppTextColor(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${if (language == "bn") "কার্যকর:" else "Effective:"} ${formatter.format(Date(rate.effectiveDate))}",
                                        color = AppTextColor().copy(alpha = 0.5f),
                                        fontSize = 10.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteElectricityRate(rate) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentPink, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val charge = chargeInput.toDoubleOrNull() ?: fixedCharge
                        viewModel.updateCalculationParameters(flatRate, charge)
                        showParamEdit = false
                    }
                ) {
                    Text(if (language == "bn") "সম্পন্ন" else "Done", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            }
        )

        if (showRateDatePicker) {
            SlidingDatePickerModal(
                initialDateMillis = rateDateMillis,
                onDateSelected = { rateDateMillis = it },
                onDismiss = { showRateDatePicker = false },
                textColor = AppTextColor(),
                backgroundColor = getDialogBackground()
            )
        }
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
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
