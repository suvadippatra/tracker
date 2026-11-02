package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun MeterDialPicker(
    modifier: Modifier = Modifier,
    initialValue: Double,
    onValueChange: (Double) -> Unit,
    textColor: Color = AppTextColor(),
    accentColor: Color = AccentCyan
) {
    val formattedValue = String.format(Locale.US, "%07.2f", initialValue)
    val initialDigits = formattedValue.replace(".", "").map { it.toString().toInt() }
    
    // Local state for digits to ensure independence during interaction
    val digits = remember { mutableStateListOf<Int>().apply { addAll(initialDigits) } }
    
    // Sync with external value changes (e.g. if parent resets)
    LaunchedEffect(initialValue) {
        val currentFormatted = String.format(Locale.US, "%07.2f", assembleDouble(digits))
        if (currentFormatted != formattedValue) {
            digits.clear()
            digits.addAll(initialDigits)
        }
    }

    var showEditDialog by remember { mutableStateOf<Int?>(null) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thousands, Hundreds, Tens, Units
        for (i in 0 until 4) {
            DialDigit(
                modifier = Modifier.weight(1f).padding(horizontal = 1.5.dp),
                value = digits[i],
                onValueChange = { newValue ->
                    digits[i] = newValue
                    onValueChange(assembleDouble(digits))
                },
                onDoubleTap = { showEditDialog = i },
                textColor = textColor,
                accentColor = accentColor
            )
        }
        
        Box(
            modifier = Modifier
                .height(120.dp)
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ".",
                color = accentColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        
        // Tenths, Hundredths
        for (i in 4 until 6) {
            DialDigit(
                modifier = Modifier.weight(1f).padding(horizontal = 1.5.dp),
                value = digits[i],
                onValueChange = { newValue ->
                    digits[i] = newValue
                    onValueChange(assembleDouble(digits))
                },
                onDoubleTap = { showEditDialog = i },
                textColor = textColor,
                accentColor = accentColor
            )
        }
    }

    if (showEditDialog != null) {
        var textInput by remember { mutableStateOf(digits[showEditDialog!!].toString()) }
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            containerColor = getDialogBackground(),
            titleContentColor = AppTextColor(),
            textContentColor = AppTextColor(),
            shape = RoundedCornerShape(20.dp),
            title = { Text("Enter Digit", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().imePadding()) {
                    GlassTextField(
                        value = textInput,
                        onValueChange = { if (it.length <= 1 && it.all { c -> c.isDigit() }) textInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newVal = textInput.toIntOrNull() ?: digits[showEditDialog!!]
                        digits[showEditDialog!!] = newVal
                        onValueChange(assembleDouble(digits))
                        showEditDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor()),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = AppSecondaryTextColor())
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialDigit(
    modifier: Modifier = Modifier,
    value: Int,
    onValueChange: (Int) -> Unit,
    onDoubleTap: () -> Unit,
    textColor: Color,
    accentColor: Color = AccentCyan
) {
    val isGlass = isLiquidGlassEnabled()
    val isLight = isAppLightMode()
    
    val containerBg = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark
    } else {
        if (isLight) Color.White.copy(alpha = 0.45f) else Color(0xFF1E2638).copy(alpha = 0.50f)
    }
    val containerBorder = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3OutlineLight else com.example.ui.theme.M3OutlineDark
    } else {
        if (isLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.20f)
    }

    BoxWithConstraints(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerBg)
            .border(1.dp, containerBorder, RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = {},
                onDoubleClick = onDoubleTap
            ),
        contentAlignment = Alignment.Center
    ) {
        val calculatedItemHeight = (maxHeight / 3).coerceIn(34.dp, 42.dp)
        val calculatedFontSize = (maxWidth.value * 0.48f).coerceIn(16f, 22f).sp

        WheelPicker(
            items = (0..9).toList(),
            selectedItem = value,
            onItemSelected = { onValueChange(it) },
            modifier = Modifier.fillMaxSize(),
            itemHeight = calculatedItemHeight,
            visibleItemsCount = 3
        ) { digit, isSelected ->
            val digitColor = if (isSelected) {
                if (!isGlass) accentColor else textColor
            } else {
                textColor.copy(alpha = 0.35f)
            }
            Text(
                text = digit.toString(),
                color = digitColor,
                fontSize = if (isSelected) calculatedFontSize else (calculatedFontSize.value * 0.88f).sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun assembleDouble(digits: List<Int>): Double {
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
    val calendar = remember(initialDateTime) {
        Calendar.getInstance().apply { timeInMillis = initialDateTime }
    }
    
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
    val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    val isPm = hour24 >= 12
    val minute = calendar.get(Calendar.MINUTE)
    
    val years = (2024..2030).map { it.toString() }
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val months = (0..11).map { i -> "${monthNames[i]}(${String.format(Locale.US, "%02d", i + 1)})" }
    val days = (1..31).map { String.format(Locale.US, "%02d", it) }
    val hours12List = (1..12).map { String.format(Locale.US, "%02d", it) }
    val minutes = (0..59).map { String.format(Locale.US, "%02d", it) }
    val amPmList = listOf("AM", "PM")

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Date Part
            DateTimePickerWheel(
                startIndex = days.indexOf(String.format(Locale.US, "%02d", day)),
                texts = days,
                onValueChange = { d ->
                    calendar.set(Calendar.DAY_OF_MONTH, d + 1)
                    onDateTimeChange(calendar.timeInMillis)
                },
                textColor = textColor,
                label = "DD",
                width = 50.dp
            )
            DateTimePickerWheel(
                startIndex = month,
                texts = months,
                onValueChange = { m ->
                    calendar.set(Calendar.MONTH, m)
                    onDateTimeChange(calendar.timeInMillis)
                },
                textColor = textColor,
                label = "MM",
                width = 95.dp
            )
            DateTimePickerWheel(
                startIndex = years.indexOf(year.toString()).coerceAtLeast(0),
                texts = years,
                onValueChange = { y ->
                    calendar.set(Calendar.YEAR, years[y].toInt())
                    onDateTimeChange(calendar.timeInMillis)
                },
                textColor = textColor,
                label = "YYYY",
                width = 65.dp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Time Part
            DateTimePickerWheel(
                startIndex = hour12 - 1,
                texts = hours12List,
                onValueChange = { hIdx ->
                    val h12 = hIdx + 1
                    val newHour24 = when {
                        isPm && h12 < 12 -> h12 + 12
                        isPm && h12 == 12 -> 12
                        !isPm && h12 == 12 -> 0
                        else -> h12
                    }
                    calendar.set(Calendar.HOUR_OF_DAY, newHour24)
                    onDateTimeChange(calendar.timeInMillis)
                },
                textColor = textColor,
                label = "HH",
                width = 50.dp
            )
            Text(":", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            DateTimePickerWheel(
                startIndex = minute,
                texts = minutes,
                onValueChange = { m ->
                    calendar.set(Calendar.MINUTE, m)
                    onDateTimeChange(calendar.timeInMillis)
                },
                textColor = textColor,
                label = "MIN",
                width = 50.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            DateTimePickerWheel(
                startIndex = if (isPm) 1 else 0,
                texts = amPmList,
                onValueChange = { amPmIdx ->
                    val pm = amPmIdx == 1
                    val h12_current = if (calendar.get(Calendar.HOUR_OF_DAY) % 12 == 0) 12 else calendar.get(Calendar.HOUR_OF_DAY) % 12
                    val newHour24 = when {
                        pm && h12_current < 12 -> h12_current + 12
                        pm && h12_current == 12 -> 12
                        !pm && h12_current == 12 -> 0
                        else -> h12_current
                    }
                    calendar.set(Calendar.HOUR_OF_DAY, newHour24)
                    onDateTimeChange(calendar.timeInMillis)
                },
                textColor = textColor,
                label = "AM/PM",
                width = 65.dp
            )
        }
    }
}

@Composable
private fun DateTimePickerWheel(
    startIndex: Int,
    texts: List<String>,
    onValueChange: (Int) -> Unit,
    textColor: Color,
    label: String,
    width: Dp = 50.dp
) {
    val isGlass = isLiquidGlassEnabled()
    val isLight = isAppLightMode()

    val containerBg = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark
    } else {
        if (isLight) Color.White.copy(alpha = 0.45f) else Color(0xFF1E2638).copy(alpha = 0.50f)
    }
    val containerBorder = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3OutlineLight else com.example.ui.theme.M3OutlineDark
    } else {
        if (isLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.20f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = AppSecondaryTextColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        BoxWithConstraints(
            modifier = Modifier
                .size(width = width, height = 100.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(containerBg)
                .border(1.dp, containerBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            val calculatedItemHeight = (maxHeight / 3).coerceIn(28.dp, 34.dp)
            val calculatedFontSize = (maxWidth.value * 0.28f).coerceIn(12f, 15f).sp

            WheelPicker(
                items = texts,
                selectedItem = if (startIndex in texts.indices) texts[startIndex] else texts.first(),
                onItemSelected = { selected ->
                    val index = texts.indexOf(selected)
                    if (index != -1) onValueChange(index)
                },
                modifier = Modifier.fillMaxSize(),
                itemHeight = calculatedItemHeight,
                visibleItemsCount = 3
            ) { text, isSelected ->
                val activeTextColor = if (isSelected) {
                    if (!isGlass) AccentCyan else textColor
                } else {
                    textColor.copy(alpha = 0.35f)
                }
                Text(
                    text = text,
                    color = activeTextColor,
                    fontSize = if (isSelected) calculatedFontSize else (calculatedFontSize.value * 0.88f).sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
