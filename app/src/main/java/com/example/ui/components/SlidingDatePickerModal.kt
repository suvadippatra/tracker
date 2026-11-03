package com.example.ui.components

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

private fun findActivity(context: android.content.Context): Activity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun SlidingDatePickerModal(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    textColor: Color = AppTextColor(),
    backgroundColor: Color = Color.Unspecified
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val activity = remember(context) { findActivity(context) }
    val useNativeDialog = isHeavyEffectsDisabled() && activity != null && !activity.isFinishing

    if (useNativeDialog) {
        val currentOnDateSelected by rememberUpdatedState(onDateSelected)
        val currentOnDismiss by rememberUpdatedState(onDismiss)

        DisposableEffect(Unit) {
            val cal = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
            val dialog = DatePickerDialog(
                activity!!,
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        timeInMillis = initialDateMillis
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }
                    currentOnDateSelected(selectedCal.timeInMillis)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dialog.setOnDismissListener {
                currentOnDismiss()
            }
            dialog.show()
            onDispose {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        }
        return
    }

    // Material 3 Cylindrical Wheel Date Picker Dialog when Heavy Effects are enabled (OFF)
    var currentMillis by remember(initialDateMillis) { mutableLongStateOf(initialDateMillis) }
    val dateFormatter = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    val isLight = isAppLightMode()
    val modalBg = if (isLight) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = modalBg,
            shadowElevation = if (isLight) 12.dp else 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Date",
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = dateFormatter.format(Date(currentMillis)),
                    color = AppAccentColor(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                InlineDatePicker(
                    initialDateMillis = currentMillis,
                    onDateSelected = {
                        currentMillis = it
                    },
                    textColor = textColor
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = AppSecondaryTextColor())
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDateSelected(currentMillis)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InlineDatePicker(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    textColor: Color = AppTextColor()
) {
    val context = LocalContext.current
    var selectedMillis by remember(initialDateMillis) { mutableLongStateOf(initialDateMillis) }
    val dateFormatter = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val formattedDate = remember(selectedMillis) { dateFormatter.format(Date(selectedMillis)) }

    val isLight = isAppLightMode()
    val disableHeavy = isHeavyEffectsDisabled()
    val accent = AppAccentColor()

    val surfaceBg = if (isLight) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (disableHeavy) {
            // Fast Mode: Native Android DatePickerDialog Pill + Prev/Next Day Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceBg,
                    shadowElevation = 2.dp,
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedMillis
                            add(Calendar.DAY_OF_MONTH, -1)
                        }
                        selectedMillis = cal.timeInMillis
                        onDateSelected(selectedMillis)
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Day",
                            tint = textColor
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceBg,
                    shadowElevation = 2.dp,
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedMillis }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedMillis
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                selectedMillis = newCal.timeInMillis
                                onDateSelected(selectedMillis)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Open Calendar",
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formattedDate,
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceBg,
                    shadowElevation = 2.dp,
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedMillis
                            add(Calendar.DAY_OF_MONTH, 1)
                        }
                        selectedMillis = cal.timeInMillis
                        onDateSelected(selectedMillis)
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Day",
                            tint = textColor
                        )
                    }
                }
            }
        } else {
            // Smooth Scrolling Cylindrical Wheel Date Picker (Day / Month / Year)
            val cal = remember(selectedMillis) { Calendar.getInstance().apply { timeInMillis = selectedMillis } }
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = cal.get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDays)

            val days = remember(maxDays) { (1..maxDays).toList() }
            val monthNames = remember { DateFormatSymbols.getInstance(Locale.US).shortMonths.take(12) }
            val years = remember { (2020..2035).toList() }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = surfaceBg,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day Wheel
                    WheelPicker(
                        items = days,
                        selectedItem = currentDay,
                        onItemSelected = { d ->
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = selectedMillis
                                set(Calendar.DAY_OF_MONTH, d.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                            }
                            selectedMillis = newCal.timeInMillis
                            onDateSelected(selectedMillis)
                        },
                        modifier = Modifier.weight(1f),
                        itemHeight = 36.dp,
                        visibleItemsCount = 3
                    ) { item, isSelected ->
                        Text(
                            text = String.format(Locale.US, "%02d", item),
                            color = if (isSelected) textColor else AppSecondaryTextColor(),
                            fontSize = if (isSelected) 16.sp else 13.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }

                    // Month Wheel
                    WheelPicker(
                        items = (0..11).toList(),
                        selectedItem = currentMonth,
                        onItemSelected = { m ->
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = selectedMillis
                                val prevDay = get(Calendar.DAY_OF_MONTH)
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.MONTH, m)
                                set(Calendar.DAY_OF_MONTH, prevDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                            }
                            selectedMillis = newCal.timeInMillis
                            onDateSelected(selectedMillis)
                        },
                        modifier = Modifier.weight(1.3f),
                        itemHeight = 36.dp,
                        visibleItemsCount = 3
                    ) { item, isSelected ->
                        Text(
                            text = monthNames.getOrElse(item) { "" },
                            color = if (isSelected) accent else AppSecondaryTextColor(),
                            fontSize = if (isSelected) 16.sp else 13.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }

                    // Year Wheel
                    WheelPicker(
                        items = years,
                        selectedItem = currentYear.coerceIn(2020, 2035),
                        onItemSelected = { y ->
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = selectedMillis
                                val prevDay = get(Calendar.DAY_OF_MONTH)
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.YEAR, y)
                                set(Calendar.DAY_OF_MONTH, prevDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                            }
                            selectedMillis = newCal.timeInMillis
                            onDateSelected(selectedMillis)
                        },
                        modifier = Modifier.weight(1.2f),
                        itemHeight = 36.dp,
                        visibleItemsCount = 3
                    ) { item, isSelected ->
                        Text(
                            text = item.toString(),
                            color = if (isSelected) textColor else AppSecondaryTextColor(),
                            fontSize = if (isSelected) 16.sp else 13.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Quick Today / Yesterday Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .clickable {
                        val now = System.currentTimeMillis()
                        selectedMillis = now
                        onDateSelected(now)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Today", color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .clickable {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_MONTH, -1)
                        }
                        selectedMillis = cal.timeInMillis
                        onDateSelected(selectedMillis)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Yesterday", color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
