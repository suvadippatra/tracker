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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val isGlass = isLiquidGlassEnabled()
    val isLight = isAppLightMode()
    val cardBg = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark
    } else {
        if (isLight) Color.White.copy(alpha = 0.55f) else Color(0xFF1E2638).copy(alpha = 0.65f)
    }
    val borderCol = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3OutlineLight else com.example.ui.theme.M3OutlineDark
    } else {
        if (isLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.20f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Lightweight Odometer Digit Stepper Row (Zero LazyColumn / Zero 3D graphicsLayer lag)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                LightweightDigitStepper(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    value = digits.getOrElse(i) { 0 },
                    onValueChange = { newVal ->
                        digits[i] = newVal
                        onValueChange(assembleDouble(digits))
                    },
                    onTapDigit = { editingDigitIndex = i },
                    textColor = textColor,
                    accentColor = accentColor,
                    containerBg = cardBg,
                    containerBorder = borderCol,
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
                        .padding(horizontal = 2.dp),
                    value = digits.getOrElse(i) { 0 },
                    onValueChange = { newVal ->
                        digits[i] = newVal
                        onValueChange(assembleDouble(digits))
                    },
                    onTapDigit = { editingDigitIndex = i },
                    textColor = textColor,
                    accentColor = accentColor,
                    containerBg = cardBg,
                    containerBorder = borderCol,
                    isDecimal = true
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Direct Keypad Input Button for instant reading entry
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .clickable { showFullValueDialog = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
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
            .clip(RoundedCornerShape(10.dp))
            .background(containerBg)
            .border(
                width = 1.dp,
                color = if (isDecimal) accentColor.copy(alpha = 0.45f) else containerBorder,
                shape = RoundedCornerShape(10.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Increment (+) button
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

        // Current digit display (tap to edit directly)
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

        // Decrement (-) button
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

    val isGlass = isLiquidGlassEnabled()
    val isLight = isAppLightMode()
    val accent = AppAccentColor()
    val cardBg = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark
    } else {
        if (isLight) Color.White.copy(alpha = 0.55f) else Color(0xFF1E2638).copy(alpha = 0.65f)
    }
    val borderCol = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3OutlineLight else com.example.ui.theme.M3OutlineDark
    } else {
        if (isLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.20f)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Native Date Picker Button
            Row(
                modifier = Modifier
                    .weight(1.15f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(12.dp))
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

            // Native Time Picker Button
            Row(
                modifier = Modifier
                    .weight(0.85f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(12.dp))
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

        // Quick preset chips ("Now", "Yesterday", "-1 Hr")
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
