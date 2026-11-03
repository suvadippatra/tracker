package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MeterDialPicker(
    modifier: Modifier = Modifier,
    initialValue: Double,
    onValueChange: (Double) -> Unit,
    textColor: Color = AppTextColor(),
    accentColor: Color = AccentCyan
) {
    val formattedValue = remember(initialValue) { String.format(Locale.US, "%07.2f", initialValue.coerceIn(0.0, 9999.99)) }
    val initialDigits = remember(formattedValue) {
        formattedValue.replace(".", "").map { it.digitToIntOrNull() ?: 0 }
    }

    val digits = remember { mutableStateListOf<Int>().apply { addAll(initialDigits) } }

    LaunchedEffect(initialValue) {
        val currentFormatted = String.format(Locale.US, "%07.2f", assembleDouble(digits))
        if (currentFormatted != formattedValue && initialDigits.size == 6) {
            digits.clear()
            digits.addAll(initialDigits)
        }
    }

    var showFullValueDialog by remember { mutableStateOf(false) }
    var editingDigitIndex by remember { mutableStateOf<Int?>(null) }

    val isLight = isAppLightMode()
    val disableHeavy = isHeavyEffectsDisabled()
    val cardBg = if (isLight) Color(0xFFEFF4F9) else com.example.ui.theme.M3SurfaceHighDark
    val decimalBg = if (isLight) Color(0xFFD3E3FD) else Color(0xFF004A77)
    val digitRange = remember { (0..9).toList() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (disableHeavy) {
            // Fast Mode (Disable Heavy Effects = ON): Instant +/- Digital Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    LightweightDigitStepper(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp),
                        value = digits.getOrElse(i) { 0 },
                        onValueChange = { newVal ->
                            digits[i] = newVal
                            onValueChange(assembleDouble(digits))
                        },
                        onTapDigit = { editingDigitIndex = i },
                        textColor = textColor,
                        accentColor = accentColor,
                        containerBg = cardBg,
                        containerBorder = Color.Transparent,
                        isDecimal = false
                    )
                }

                Text(
                    text = ".",
                    color = accentColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                for (i in 4 until 6) {
                    LightweightDigitStepper(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp),
                        value = digits.getOrElse(i) { 0 },
                        onValueChange = { newVal ->
                            digits[i] = newVal
                            onValueChange(assembleDouble(digits))
                        },
                        onTapDigit = { editingDigitIndex = i },
                        textColor = textColor,
                        accentColor = accentColor,
                        containerBg = decimalBg,
                        containerBorder = Color.Transparent,
                        isDecimal = true
                    )
                }
            }
        } else {
            // Smooth Scrolling Mode (Disable Heavy Effects = OFF): Scrollable Cylindrical Wheel Dials
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val boxShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .UARTShadow(
                                elevation = if (isLight) 1.5.dp else 2.5.dp,
                                shape = boxShape,
                                ambientColor = if (isLight) Color(0x140F172A) else Color(0x44000000),
                                spotColor = if (isLight) Color(0x1E0F172A) else Color(0x66000000)
                            )
                            .clip(boxShape)
                            .background(cardBg)
                            .clickable { editingDigitIndex = i }
                    ) {
                        WheelPicker(
                            items = digitRange,
                            selectedItem = digits.getOrElse(i) { 0 },
                            onItemSelected = { newVal ->
                                if (digits.getOrElse(i) { 0 } != newVal) {
                                    digits[i] = newVal
                                    onValueChange(assembleDouble(digits))
                                }
                            },
                            itemHeight = 36.dp,
                            visibleItemsCount = 3
                        ) { item, isSelected ->
                            Text(
                                text = item.toString(),
                                color = if (isSelected) textColor else AppSecondaryTextColor(),
                                fontSize = if (isSelected) 20.sp else 15.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Text(
                    text = ".",
                    color = accentColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                for (i in 4 until 6) {
                    val boxShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .UARTShadow(
                                elevation = if (isLight) 1.5.dp else 2.5.dp,
                                shape = boxShape,
                                ambientColor = if (isLight) Color(0x140F172A) else Color(0x44000000),
                                spotColor = if (isLight) Color(0x1E0F172A) else Color(0x66000000)
                            )
                            .clip(boxShape)
                            .background(decimalBg)
                            .clickable { editingDigitIndex = i }
                    ) {
                        WheelPicker(
                            items = digitRange,
                            selectedItem = digits.getOrElse(i) { 0 },
                            onItemSelected = { newVal ->
                                if (digits.getOrElse(i) { 0 } != newVal) {
                                    digits[i] = newVal
                                    onValueChange(assembleDouble(digits))
                                }
                            },
                            itemHeight = 36.dp,
                            visibleItemsCount = 3
                        ) { item, isSelected ->
                            Text(
                                text = item.toString(),
                                color = if (isSelected) accentColor else accentColor.copy(alpha = 0.6f),
                                fontSize = if (isSelected) 20.sp else 15.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Direct Keypad Input Button for instant reading entry (Material 3 borderless tonal pill)
        val chipShape = RoundedCornerShape(14.dp)
        Row(
            modifier = Modifier
                .UARTShadow(
                    elevation = if (isLight) 2.dp else 3.dp,
                    shape = chipShape,
                    ambientColor = if (isLight) Color(0x160F172A) else Color(0x44000000),
                    spotColor = if (isLight) Color(0x200F172A) else Color(0x66000000)
                )
                .clip(chipShape)
                .background(decimalBg)
                .clickable { showFullValueDialog = true }
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Type Meter Reading",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Type Reading (${String.format(Locale.US, "%.2f", assembleDouble(digits))} kWh)",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (editingDigitIndex != null) {
        val idx = editingDigitIndex!!
        var textInput by remember(idx) { mutableStateOf(digits.getOrElse(idx) { 0 }.toString()) }
        AlertDialog(
            onDismissRequest = { editingDigitIndex = null },
            containerColor = getDialogBackground(),
            titleContentColor = AppTextColor(),
            textContentColor = AppTextColor(),
            shape = RoundedCornerShape(20.dp),
            title = { Text("Enter Digit (0-9)", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().imePadding()) {
                    GlassTextField(
                        value = textInput,
                        onValueChange = { if (it.length <= 1 && it.all { c -> c.isDigit() }) textInput = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newVal = textInput.toIntOrNull()?.coerceIn(0, 9) ?: digits[idx]
                        digits[idx] = newVal
                        onValueChange(assembleDouble(digits))
                        editingDigitIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor()),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingDigitIndex = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = AppSecondaryTextColor())
                }
            }
        )
    }

    if (showFullValueDialog) {
        var fullInput by remember {
            mutableStateOf(String.format(Locale.US, "%.2f", assembleDouble(digits)))
        }
        AlertDialog(
            onDismissRequest = { showFullValueDialog = false },
            containerColor = getDialogBackground(),
            titleContentColor = AppTextColor(),
            textContentColor = AppTextColor(),
            shape = RoundedCornerShape(20.dp),
            title = { Text("Enter Meter Reading", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().imePadding()) {
                    Text(
                        text = "Enter value between 0.00 and 9999.99",
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    GlassTextField(
                        value = fullInput,
                        onValueChange = { input ->
                            if (input.length <= 7 && input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) {
                                fullInput = input
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = fullInput.toDoubleOrNull()?.coerceIn(0.0, 9999.99)
                        if (parsed != null) {
                            val formatted = String.format(Locale.US, "%07.2f", parsed)
                            val newDigits = formatted.replace(".", "").map { it.digitToIntOrNull() ?: 0 }
                            if (newDigits.size == 6) {
                                digits.clear()
                                digits.addAll(newDigits)
                                onValueChange(assembleDouble(digits))
                            }
                        }
                        showFullValueDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor()),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFullValueDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = AppSecondaryTextColor())
                }
            }
        )
    }
}

@Composable
private fun LightweightDigitStepper(
    modifier: Modifier = Modifier,
    value: Int,
    onValueChange: (Int) -> Unit,
    onTapDigit: () -> Unit,
    textColor: Color,
    accentColor: Color,
    containerBg: Color,
    containerBorder: Color,
    isDecimal: Boolean
) {
    Column(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clickable { onValueChange((value + 1) % 10) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Increment digit",
                tint = AppSecondaryTextColor(),
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(if (isDecimal) accentColor.copy(alpha = 0.10f) else Color.Transparent)
                .clickable { onTapDigit() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                color = if (isDecimal) accentColor else textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clickable { onValueChange((value + 9) % 10) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrement digit",
                tint = AppSecondaryTextColor(),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun assembleDouble(digits: List<Int>): Double {
    if (digits.size < 6) return 0.0
    val integerPart = digits[0] * 1000 + digits[1] * 100 + digits[2] * 10 + digits[3]
    val decimalPart = digits[4] * 0.1 + digits[5] * 0.01
    return integerPart + decimalPart
}

@Composable
fun CylindricalDateTimePicker(
    modifier: Modifier = Modifier,
    initialDateTime: Long,
    onDateTimeChange: (Long) -> Unit,
    textColor: Color = AppTextColor()
) {
    val context = LocalContext.current
    val calendar = remember(initialDateTime) {
        Calendar.getInstance().apply { timeInMillis = initialDateTime }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val formattedDate = remember(initialDateTime) { dateFormatter.format(Date(initialDateTime)) }
    val formattedTime = remember(initialDateTime) { timeFormatter.format(Date(initialDateTime)) }

    val isLight = isAppLightMode()
    val disableHeavy = isHeavyEffectsDisabled()
    val accent = AppAccentColor()
    val cardBg = if (isLight) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark
    val borderCol = if (isLight) com.example.ui.theme.M3OutlineLight else com.example.ui.theme.M3OutlineDark

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (disableHeavy) {
            // Fast Native Mode: Native Date & Time Dialog Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1.15f)
                        .height(48.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = initialDateTime
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    onDateTimeChange(newCal.timeInMillis)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Select Date",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = initialDateTime
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                    }
                                    onDateTimeChange(newCal.timeInMillis)
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                false
                            ).show()
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Select Time",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedTime,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        } else {
            // Smooth Scrolling Cylindrical Wheel Date & Time Picker
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDays)
            val currentHour12 = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentAmPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

            val days = remember(maxDays) { (1..maxDays).toList() }
            val monthNames = remember { DateFormatSymbols.getInstance(Locale.US).shortMonths.take(12) }
            val years = remember { (2020..2035).toList() }
            val hours = remember { (1..12).toList() }
            val minutes = remember { (0..59).toList() }
            val amPmList = remember { listOf("AM", "PM") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day Wheel
                WheelPicker(
                    items = days,
                    selectedItem = currentDay,
                    onItemSelected = { d ->
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = initialDateTime
                            set(Calendar.DAY_OF_MONTH, d.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                        }
                        onDateTimeChange(newCal.timeInMillis)
                    },
                    modifier = Modifier.weight(0.8f),
                    itemHeight = 34.dp,
                    visibleItemsCount = 3
                ) { item, isSelected ->
                    Text(
                        text = String.format(Locale.US, "%02d", item),
                        color = if (isSelected) textColor else AppSecondaryTextColor(),
                        fontSize = if (isSelected) 14.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Month Wheel
                WheelPicker(
                    items = (0..11).toList(),
                    selectedItem = currentMonth,
                    onItemSelected = { m ->
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = initialDateTime
                            val prevDay = get(Calendar.DAY_OF_MONTH)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.MONTH, m)
                            set(Calendar.DAY_OF_MONTH, prevDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                        }
                        onDateTimeChange(newCal.timeInMillis)
                    },
                    modifier = Modifier.weight(1.1f),
                    itemHeight = 34.dp,
                    visibleItemsCount = 3
                ) { item, isSelected ->
                    Text(
                        text = monthNames.getOrElse(item) { "" },
                        color = if (isSelected) accent else AppSecondaryTextColor(),
                        fontSize = if (isSelected) 14.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Year Wheel
                WheelPicker(
                    items = years,
                    selectedItem = currentYear.coerceIn(2020, 2035),
                    onItemSelected = { y ->
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = initialDateTime
                            val prevDay = get(Calendar.DAY_OF_MONTH)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.YEAR, y)
                            set(Calendar.DAY_OF_MONTH, prevDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                        }
                        onDateTimeChange(newCal.timeInMillis)
                    },
                    modifier = Modifier.weight(1.1f),
                    itemHeight = 34.dp,
                    visibleItemsCount = 3
                ) { item, isSelected ->
                    Text(
                        text = item.toString(),
                        color = if (isSelected) textColor else AppSecondaryTextColor(),
                        fontSize = if (isSelected) 14.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Hour Wheel
                WheelPicker(
                    items = hours,
                    selectedItem = currentHour12,
                    onItemSelected = { h12 ->
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = initialDateTime
                            set(Calendar.HOUR, if (h12 == 12) 0 else h12)
                        }
                        onDateTimeChange(newCal.timeInMillis)
                    },
                    modifier = Modifier.weight(0.8f),
                    itemHeight = 34.dp,
                    visibleItemsCount = 3
                ) { item, isSelected ->
                    Text(
                        text = String.format(Locale.US, "%02d", item),
                        color = if (isSelected) textColor else AppSecondaryTextColor(),
                        fontSize = if (isSelected) 14.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Minute Wheel
                WheelPicker(
                    items = minutes,
                    selectedItem = currentMinute,
                    onItemSelected = { min ->
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = initialDateTime
                            set(Calendar.MINUTE, min)
                        }
                        onDateTimeChange(newCal.timeInMillis)
                    },
                    modifier = Modifier.weight(0.8f),
                    itemHeight = 34.dp,
                    visibleItemsCount = 3
                ) { item, isSelected ->
                    Text(
                        text = String.format(Locale.US, "%02d", item),
                        color = if (isSelected) textColor else AppSecondaryTextColor(),
                        fontSize = if (isSelected) 14.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // AM/PM Wheel
                WheelPicker(
                    items = amPmList,
                    selectedItem = currentAmPm,
                    onItemSelected = { ap ->
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = initialDateTime
                            set(Calendar.AM_PM, if (ap == "AM") Calendar.AM else Calendar.PM)
                        }
                        onDateTimeChange(newCal.timeInMillis)
                    },
                    modifier = Modifier.weight(0.85f),
                    itemHeight = 34.dp,
                    visibleItemsCount = 3
                ) { item, isSelected ->
                    Text(
                        text = item,
                        color = if (isSelected) accent else AppSecondaryTextColor(),
                        fontSize = if (isSelected) 13.sp else 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }
        }

        // Quick preset chips ("Now", "-1 Day", "+1 Day")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            QuickTimeChip(label = "Now", accent = accent) {
                onDateTimeChange(System.currentTimeMillis())
            }
            QuickTimeChip(label = "-1 Day", accent = accent) {
                onDateTimeChange(initialDateTime - 86_400_000L)
            }
            QuickTimeChip(label = "+1 Day", accent = accent) {
                onDateTimeChange(initialDateTime + 86_400_000L)
            }
        }
    }
}

@Composable
private fun QuickTimeChip(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    val isLight = isAppLightMode()
    val chipBg = if (isLight) Color(0xFFD3E3FD) else Color(0xFF004A77)
    val chipFg = if (isLight) Color(0xFF041E49) else Color(0xFFC2E7FF)
    Box(
        modifier = Modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(chipBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = chipFg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
