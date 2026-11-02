package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SlidingDatePickerModal(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    textColor: Color = AppTextColor(),
    backgroundColor: Color = Color.Unspecified
) {
    BackHandler { onDismiss() }

    val calendar = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    
    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) } // 0-based
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    
    val days = (1..31).toList()
    // Short months like Jan(01), Feb(02), Mar(03)...
    val monthsStr = listOf("Jan(01)", "Feb(02)", "Mar(03)", "Apr(04)", "May(05)", "Jun(06)", "Jul(07)", "Aug(08)", "Sep(09)", "Oct(10)", "Nov(11)", "Dec(12)")
    val years = (1900..2100).toList()

    // Determine max days in current month/year
    val maxDaysInMonth = remember(selectedMonth, selectedYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonth)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    
    // Adjust day if selected day exceeds max day for selected month
    LaunchedEffect(maxDaysInMonth) {
        if (selectedDay > maxDaysInMonth) {
            selectedDay = maxDaysInMonth
        }
    }

        LaunchedEffect(selectedDay, selectedMonth, selectedYear) {
        val newCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, selectedDay)
        }
        onDateSelected(newCal.timeInMillis)
    }

    val dynamicDays = (1..maxDaysInMonth).toList()

    val isGlass = isLiquidGlassEnabled()
    val isLight = isAppLightMode()

    val modalBg = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark
    } else {
        if (isLight) Color(0xF5FFFFFF) else Color(0xFA1E2638)
    }

    val modalBorder = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3OutlineLight else com.example.ui.theme.M3OutlineDark
    } else {
        getGlassBorder()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .background(modalBg, RoundedCornerShape(24.dp))
                .border(1.dp, modalBorder, RoundedCornerShape(24.dp))
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
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day Picker
                    WheelPicker(
                        items = dynamicDays,
                        selectedItem = selectedDay,
                        onItemSelected = { selectedDay = it },
                        modifier = Modifier.weight(1f),
                        itemHeight = 40.dp,
                        visibleItemsCount = 5
                    ) { item, isSelected ->
                        val itemColor = if (isSelected) {
                            if (!isGlass) AccentCyan else textColor
                        } else {
                            textColor.copy(alpha = 0.40f)
                        }
                        Text(
                            text = String.format(Locale.getDefault(), "%02d", item),
                            color = itemColor,
                            fontSize = if (isSelected) 20.sp else 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    
                    // Month Picker
                    WheelPicker(
                        items = monthsStr,
                        selectedItem = monthsStr[selectedMonth],
                        onItemSelected = { selectedMonth = monthsStr.indexOf(it) },
                        modifier = Modifier.weight(1.6f),
                        itemHeight = 40.dp,
                        visibleItemsCount = 5
                    ) { item, isSelected ->
                        val itemColor = if (isSelected) {
                            if (!isGlass) AccentCyan else textColor
                        } else {
                            textColor.copy(alpha = 0.40f)
                        }
                        Text(
                            text = item,
                            color = itemColor,
                            fontSize = if (isSelected) 18.sp else 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    
                    // Year Picker
                    WheelPicker(
                        items = years,
                        selectedItem = selectedYear,
                        onItemSelected = { selectedYear = it },
                        modifier = Modifier.weight(1.2f),
                        itemHeight = 40.dp,
                        visibleItemsCount = 5
                    ) { item, isSelected ->
                        val itemColor = if (isSelected) {
                            if (!isGlass) AccentCyan else textColor
                        } else {
                            textColor.copy(alpha = 0.40f)
                        }
                        Text(
                            text = item.toString(),
                            color = itemColor,
                            fontSize = if (isSelected) 20.sp else 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                        onClick = onDismiss,
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
    val calendar = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    val monthsStr = listOf("Jan(01)", "Feb(02)", "Mar(03)", "Apr(04)", "May(05)", "Jun(06)", "Jul(07)", "Aug(08)", "Sep(09)", "Oct(10)", "Nov(11)", "Dec(12)")
    val years = (1900..2100).toList()

    val maxDaysInMonth = remember(selectedMonth, selectedYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonth)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    LaunchedEffect(maxDaysInMonth) {
        if (selectedDay > maxDaysInMonth) {
            selectedDay = maxDaysInMonth
        }
    }

    LaunchedEffect(selectedDay, selectedMonth, selectedYear) {
        val newCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, selectedDay)
        }
        onDateSelected(newCal.timeInMillis)
    }

        LaunchedEffect(selectedDay, selectedMonth, selectedYear) {
        val newCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, selectedDay)
        }
        onDateSelected(newCal.timeInMillis)
    }

    val dynamicDays = (1..maxDaysInMonth).toList()
    val isGlass = isLiquidGlassEnabled()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day
            WheelPicker(
                items = dynamicDays,
                selectedItem = selectedDay,
                onItemSelected = { selectedDay = it },
                modifier = Modifier.weight(1f),
                itemHeight = 40.dp,
                visibleItemsCount = 3
            ) { item, isSelected ->
                val itemColor = if (isSelected) {
                    if (!isGlass) AccentCyan else textColor
                } else {
                    textColor.copy(alpha = 0.40f)
                }
                Text(
                    text = String.format(Locale.getDefault(), "%02d", item),
                    color = itemColor,
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Month (weight 1.6f)
            WheelPicker(
                items = monthsStr,
                selectedItem = monthsStr[selectedMonth],
                onItemSelected = { selectedMonth = monthsStr.indexOf(it) },
                modifier = Modifier.weight(1.6f),
                itemHeight = 40.dp,
                visibleItemsCount = 3
            ) { item, isSelected ->
                val itemColor = if (isSelected) {
                    if (!isGlass) AccentCyan else textColor
                } else {
                    textColor.copy(alpha = 0.40f)
                }
                Text(
                    text = item,
                    color = itemColor,
                    fontSize = if (isSelected) 18.sp else 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Year
            WheelPicker(
                items = years,
                selectedItem = selectedYear,
                onItemSelected = { selectedYear = it },
                modifier = Modifier.weight(1.2f),
                itemHeight = 40.dp,
                visibleItemsCount = 3
            ) { item, isSelected ->
                val itemColor = if (isSelected) {
                    if (!isGlass) AccentCyan else textColor
                } else {
                    textColor.copy(alpha = 0.40f)
                }
                Text(
                    text = item.toString(),
                    color = itemColor,
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
