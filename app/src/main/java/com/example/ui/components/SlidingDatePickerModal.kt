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

    // Lightweight Material 3 fallback dialog when heavy effects are enabled or Activity context is unavailable
    var currentMillis by remember(initialDateMillis) { mutableLongStateOf(initialDateMillis) }
    val dateFormatter = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

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
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = dateFormatter.format(Date(currentMillis)),
                    color = AppAccentColor(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                InlineDatePicker(
                    initialDateMillis = currentMillis,
                    onDateSelected = {
                        currentMillis = it
                        onDateSelected(it)
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

    val isGlass = isLiquidGlassEnabled()
    val isLight = isAppLightMode()
    val accent = AppAccentColor()

    val surfaceBg = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark
    } else {
        if (isLight) Color.White.copy(alpha = 0.55f) else Color(0xFF1E2638).copy(alpha = 0.65f)
    }
    val borderCol = if (!isGlass) {
        if (isLight) com.example.ui.theme.M3OutlineLight else com.example.ui.theme.M3OutlineDark
    } else {
        getGlassBorder()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Previous Day Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceBg)
                    .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                    .clickable {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedMillis
                            add(Calendar.DAY_OF_MONTH, -1)
                        }
                        selectedMillis = cal.timeInMillis
                        onDateSelected(selectedMillis)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Day",
                    tint = textColor
                )
            }

            // Center Date Pill (Opens Native Android DatePickerDialog)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceBg)
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .clickable {
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

            // Next Day Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceBg)
                    .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                    .clickable {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedMillis
                            add(Calendar.DAY_OF_MONTH, 1)
                        }
                        selectedMillis = cal.timeInMillis
                        onDateSelected(selectedMillis)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Day",
                    tint = textColor
                )
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
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
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
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
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
