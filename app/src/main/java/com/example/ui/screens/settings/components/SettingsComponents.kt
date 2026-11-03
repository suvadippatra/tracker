package com.example.ui.screens.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*

@Composable
fun PixelSettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isLight = isAppLightMode()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title.uppercase(),
                color = AppAccentColor(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = getCardBackground(),
            shadowElevation = if (isLight) 4.dp else 6.dp,
            tonalElevation = if (isLight) 1.dp else 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun PixelSettingsItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
    content: @Composable (() -> Unit)? = null
) {
    val appAccent = AppAccentColor()
    val isLight = isAppLightMode()
    val iconBgColor = appAccent.copy(alpha = if (isLight) 0.12f else 0.16f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = appAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = AppTextColor(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = AppSecondaryTextColor(),
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            if (content != null) {
                Spacer(modifier = Modifier.width(8.dp))
                content()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next",
                    tint = AppSecondaryTextColor().copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = AppSecondaryTextColor().copy(alpha = 0.10f),
                modifier = Modifier.padding(start = if (icon != null) 68.dp else 16.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun PixelSettingsSwitchItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = false
) {
    val appAccent = AppAccentColor()
    val isLight = isAppLightMode()
    val iconBgColor = appAccent.copy(alpha = if (isLight) 0.12f else 0.16f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (checked) appAccent else AppSecondaryTextColor(),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = AppTextColor(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = AppSecondaryTextColor(),
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = appAccent,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = if (isLight) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                    uncheckedTrackColor = if (isLight) Color(0x18000000) else Color(0x22FFFFFF),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = AppSecondaryTextColor().copy(alpha = 0.10f),
                modifier = Modifier.padding(start = if (icon != null) 68.dp else 16.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun PixelSettingsOptionItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = false
) {
    val appAccent = AppAccentColor()
    val isLight = isAppLightMode()
    val highlightBg = if (selected) appAccent.copy(alpha = if (isLight) 0.10f else 0.14f) else Color.Transparent

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(highlightBg)
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = appAccent,
                    unselectedColor = AppSecondaryTextColor()
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) appAccent else AppSecondaryTextColor(),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = AppTextColor(),
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = AppSecondaryTextColor().copy(alpha = 0.10f),
                modifier = Modifier.padding(start = 56.dp, end = 16.dp)
            )
        }
    }
}
