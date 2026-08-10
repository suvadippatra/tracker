package com.example.ui.components

import androidx.compose.foundation.background
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
    textColor: Color,
    backgroundColor: Color
) {
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

    val dynamicDays = (1..maxDaysInMonth).toList()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(backgroundColor, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select Date", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
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
                        Text(
                            text = String.format(Locale.getDefault(), "%02d", item),
                            color = if (isSelected) textColor else textColor.copy(alpha = 0.5f),
                            fontSize = if (isSelected) 22.sp else 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    
                    // Month Picker
                    WheelPicker(
                        items = monthsStr,
                        selectedItem = monthsStr[selectedMonth],
                        onItemSelected = { selectedMonth = monthsStr.indexOf(it) },
                        modifier = Modifier.weight(1f),
                        itemHeight = 40.dp,
                        visibleItemsCount = 5
                    ) { item, isSelected ->
                        Text(
                            text = item,
                            color = if (isSelected) textColor else textColor.copy(alpha = 0.5f),
                            fontSize = if (isSelected) 22.sp else 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    
                    // Year Picker
                    WheelPicker(
                        items = years,
                        selectedItem = selectedYear,
                        onItemSelected = { selectedYear = it },
                        modifier = Modifier.weight(1f),
                        itemHeight = 40.dp,
                        visibleItemsCount = 5
                    ) { item, isSelected ->
                        Text(
                            text = item.toString(),
                            color = if (isSelected) textColor else textColor.copy(alpha = 0.5f),
                            fontSize = if (isSelected) 22.sp else 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = textColor.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, selectedYear)
                                set(Calendar.MONTH, selectedMonth)
                                set(Calendar.DAY_OF_MONTH, selectedDay)
                            }
                            onDateSelected(newCal.timeInMillis)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.2f))
                    ) {
                        Text("Confirm", color = textColor)
                    }
                }
            }
            
            // Selector overlay line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.Center)
                    .background(textColor.copy(alpha = 0.05f))
            )
        }
    }
}


@Composable
fun InlineDatePicker(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    textColor: Color
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

    val dynamicDays = (1..maxDaysInMonth).toList()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
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
                Text(
                    text = String.format(Locale.getDefault(), "%02d", item),
                    color = if (isSelected) textColor else textColor.copy(alpha = 0.5f),
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Month
            WheelPicker(
                items = monthsStr,
                selectedItem = monthsStr[selectedMonth],
                onItemSelected = { selectedMonth = monthsStr.indexOf(it) },
                modifier = Modifier.weight(1f),
                itemHeight = 40.dp,
                visibleItemsCount = 3
            ) { item, isSelected ->
                Text(
                    text = item,
                    color = if (isSelected) textColor else textColor.copy(alpha = 0.5f),
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Year
            WheelPicker(
                items = years,
                selectedItem = selectedYear,
                onItemSelected = { selectedYear = it },
                modifier = Modifier.weight(1f),
                itemHeight = 40.dp,
                visibleItemsCount = 3
            ) { item, isSelected ->
                Text(
                    text = item.toString(),
                    color = if (isSelected) textColor else textColor.copy(alpha = 0.5f),
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.Center)
                .background(textColor.copy(alpha = 0.05f))
        )
    }
}
