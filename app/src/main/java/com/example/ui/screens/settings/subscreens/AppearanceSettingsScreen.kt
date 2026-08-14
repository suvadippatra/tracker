package com.example.ui.screens.settings.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun AppearanceSettingsScreen(viewModel: FinancialViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile.language

    val themeModes = listOf(
        Pair("Auto", if (prefLanguage == "bn") "সিস্টেম অনুযায়ী (অটো)" else "System Default (Auto)"),
        Pair("Dark", if (prefLanguage == "bn") "ডার্ক মোড (পিওর ব্ল্যাক)" else "Dark Mode (Pure Black)"),
        Pair("Light", if (prefLanguage == "bn") "লাইট মোড (উজ্জ্বল)" else "Light Mode (Clean)")
    )

    val textSizes = listOf(
        Pair("Small", if (prefLanguage == "bn") "ছোট (কম্প্যাক্ট)" else "Small (Compact)"),
        Pair("Medium", if (prefLanguage == "bn") "সাধারণ (স্ট্যান্ডার্ড)" else "Medium (Standard)"),
        Pair("Large", if (prefLanguage == "bn") "বড় (সহজ পাঠযোগ্য)" else "Large (Spacious)")
    )

    val bgPresets = listOf(
        Triple("Default", "Pure Black", listOf(Color(0xFF000000), Color(0xFF101010))),
        Triple("Midnight Blue", "Midnight Blue", listOf(Color(0xFF0F172A), Color(0xFF0284C7))),
        Triple("Emerald Dark", "Emerald Dark", listOf(Color(0xFF064E3B), Color(0xFF0F766E))),
        Triple("Sunset Warm", "Sunset Warm", listOf(Color(0xFF451A03), Color(0xFF9A3412))),
        Triple("Purple Twilight", "Purple Twilight", listOf(Color(0xFF2E1065), Color(0xFF581C87))),
        Triple("Soft Minimal Light", "Soft Minimal Light", listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Theme Selection
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "থিম ও ডিসপ্লে মোড" else "Theme & Display Mode") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    themeModes.forEach { (mode, label) ->
                        val isSelected = userProfile.themeMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AccentCyan.copy(alpha = 0.18f) else getCardBackground())
                                .border(1.dp, if (isSelected) AccentCyan else getGlassBorder(), RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.saveUserProfile(userProfile.copy(themeMode = mode))
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.saveUserProfile(userProfile.copy(themeMode = mode)) },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentCyan)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = when (mode) {
                                    "Dark" -> Icons.Default.DarkMode
                                    "Light" -> Icons.Default.LightMode
                                    else -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = null,
                                tint = if (isSelected) AccentCyan else AppSecondaryTextColor(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                color = AppTextColor(),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // 2. Text Scaling
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "ফন্ট ও টেক্সট সাইজ" else "Text Size & Typography") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    textSizes.forEach { (sizeKey, label) ->
                        val isSelected = userProfile.textSize == sizeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else getCardBackground())
                                .border(1.dp, if (isSelected) AccentCyan else getGlassBorder(), RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.saveUserProfile(userProfile.copy(textSize = sizeKey))
                                }
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Aa",
                                    fontSize = when(sizeKey) {
                                        "Small" -> 14.sp
                                        "Medium" -> 18.sp
                                        else -> 22.sp
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) AccentCyan else AppTextColor()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sizeKey,
                                    fontSize = 12.sp,
                                    color = if (isSelected) AccentCyan else AppSecondaryTextColor(),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Background Wallpaper Presets
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "ব্যাকগ্রাউন্ড প্রিসেট" else "Background Presets") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bgPresets.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (presetKey, title, colors) ->
                                val isSelected = userProfile.bgPreset == presetKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.verticalGradient(colors))
                                        .border(2.dp, if (isSelected) AccentCyan else getGlassBorder(), RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.saveUserProfile(userProfile.copy(bgPreset = presetKey))
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = title,
                                            color = if (presetKey == "Soft Minimal Light") Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(AccentCyan),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(14.dp)
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
        }

        // 4. Animation & Layout Preferences
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "অ্যানিমেশন ও ইন্টারফেস" else "Animation & Layout Controls") {
                // Star Animation Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(getCardBackground())
                        .border(1.dp, getGlassBorder(), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentGold, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (prefLanguage == "bn") "ব্যাকগ্রাউন্ড স্টার অ্যানিমেশন" else "Ambient Star Animation",
                                color = AppTextColor(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (prefLanguage == "bn") "পটভূমিতে মৃদু নক্ষত্র প্রভাব" else "Subtle background sparkle effect",
                                color = AppSecondaryTextColor(),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = userProfile.enableStarAnimation,
                        onCheckedChange = { isChecked ->
                            viewModel.saveUserProfile(userProfile.copy(enableStarAnimation = isChecked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AccentCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Meal Group Position
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(getCardBackground())
                        .border(1.dp, getGlassBorder(), RoundedCornerShape(12.dp))
                        .clickable {
                            val newPos = if (userProfile.mealGroupPosition == "BottomBar") "PageBottom" else "BottomBar"
                            viewModel.saveUserProfile(userProfile.copy(mealGroupPosition = newPos))
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Dashboard, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (prefLanguage == "bn") "অর্ডার সামারি পজিশন" else "Order Summary Position",
                                color = AppTextColor(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (userProfile.mealGroupPosition == "BottomBar") "Docked at Bottom Bar" else "Embedded at Page Bottom",
                                color = AppSecondaryTextColor(),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(Icons.Default.SwapVert, contentDescription = "Toggle", tint = AccentCyan)
                }
            }
        }
    }
}
