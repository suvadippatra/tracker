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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import com.example.ui.components.*
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.screens.settings.components.PixelSettingsOptionItem
import com.example.ui.screens.settings.components.PixelSettingsSwitchItem
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun AppearanceSettingsScreen(
    viewModel: FinancialViewModel,
    onOpenUtilityConfig: () -> Unit,
    onBack: () -> Unit = {}
) {
    BackHandler(enabled = true) { onBack() }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile.language

    val themeModes = listOf(
        Triple("Auto", if (prefLanguage == "bn") "সিস্টেম অনুযায়ী (অটো)" else "System Default (Auto)", Icons.Default.BrightnessAuto),
        Triple("Dark", if (prefLanguage == "bn") "ডার্ক মোড (পিওর ব্ল্যাক)" else "Dark Mode (Pure Black)", Icons.Default.DarkMode),
        Triple("Light", if (prefLanguage == "bn") "লাইট মোড (উজ্জ্বল)" else "Light Mode (Clean)", Icons.Default.LightMode)
    )

    val textSizes = listOf(
        Pair("Small", if (prefLanguage == "bn") "ছোট" else "Small"),
        Pair("Medium", if (prefLanguage == "bn") "সাধারণ" else "Medium"),
        Pair("Large", if (prefLanguage == "bn") "বড়" else "Large"),
        Pair("Extra Large", if (prefLanguage == "bn") "অতি বড়" else "XL")
    )

    val isLightMode = isAppLightMode()

    val m3AccentPresets = if (isLightMode) {
        listOf(
            Triple("M3 Ocean Blue", if (prefLanguage == "bn") "ওশান ব্লু" else "Ocean Blue", com.example.ui.theme.OceanPrimaryLight),
            Triple("M3 Emerald", if (prefLanguage == "bn") "পান্না সবুজ" else "Emerald Green", com.example.ui.theme.EmeraldPrimaryLight),
            Triple("M3 Crimson", if (prefLanguage == "bn") "ক্রিমসন লাল" else "Crimson Red", com.example.ui.theme.RosePrimaryLight),
            Triple("M3 Amber", if (prefLanguage == "bn") "অ্যাম্বার গোল্ড" else "Amber Gold", com.example.ui.theme.GoldPrimaryLight),
            Triple("M3 Purple", if (prefLanguage == "bn") "ডিপ পার্পল" else "Deep Purple", com.example.ui.theme.M3AccentPurple),
            Triple("M3 Cyan Teal", if (prefLanguage == "bn") "সায়ান টিল" else "Cyan Teal", com.example.ui.theme.M3AccentCyanTeal)
        )
    } else {
        listOf(
            Triple("M3 Ocean Blue", if (prefLanguage == "bn") "ওশান ব্লু" else "Ocean Blue", com.example.ui.theme.OceanPrimaryDark),
            Triple("M3 Emerald", if (prefLanguage == "bn") "পান্না ডার্ক" else "Emerald Dark", com.example.ui.theme.EmeraldPrimaryDark),
            Triple("M3 Crimson", if (prefLanguage == "bn") "ক্রিমসন ডার্ক" else "Crimson Dark", com.example.ui.theme.RosePrimaryDark),
            Triple("M3 Amber", if (prefLanguage == "bn") "অ্যাম্বার ডার্ক" else "Amber Dark", com.example.ui.theme.GoldPrimaryDark),
            Triple("M3 Purple", if (prefLanguage == "bn") "পার্পল ডার্ক" else "Deep Purple", com.example.ui.theme.M3AccentPurple),
            Triple("M3 Cyan Teal", if (prefLanguage == "bn") "সায়ান গ্লো" else "Cyan Glow", com.example.ui.theme.M3AccentCyanTeal)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Performance & Dial Control Mode
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "পারফরম্যান্স ও পিকার কন্ট্রোল" else "Performance & Picker Controls") {
                PixelSettingsSwitchItem(
                    icon = Icons.Default.Speed,
                    title = if (prefLanguage == "bn") "ভারী ইফেক্ট বন্ধ ও নেটিভ কন্ট্রোল (Android 8 ফাস্ট মোড)" else "Disable Heavy Effects & Use Native Controls",
                    subtitle = if (prefLanguage == "bn") {
                        "চালু (ON): নেটিভ ডেট/টাইম ডায়ালগ ও +/- ডিজিটাল স্টেপার (সবচেয়ে দ্রুত)। বন্ধ (OFF): স্ক্রলযোগ্য সিলিন্ডার হুইল ডায়াল ও নেটিভ অ্যানিমেশন।"
                    } else {
                        "ON: Instant native Android Date/Time dialogs & +/- digital stepper. OFF: Scrollable cylindrical wheel dials & smooth native animations."
                    },
                    checked = userProfile.disableHeavyEffects,
                    onCheckedChange = { isChecked ->
                        viewModel.toggleDisableHeavyEffects(isChecked)
                    },
                    showDivider = false
                )
            }
        }

        // 2. Theme Mode Selection
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "ডিসপ্লে থিম মোড" else "Display Theme Mode") {
                themeModes.forEachIndexed { index, (modeKey, label, icon) ->
                    val isSelected = userProfile.themeMode == modeKey
                    PixelSettingsOptionItem(
                        icon = icon,
                        title = label,
                        selected = isSelected,
                        onClick = {
                            viewModel.saveUserProfile(userProfile.copy(themeMode = modeKey))
                        },
                        showDivider = index < themeModes.size - 1
                    )
                }
            }
        }

        // 3. Text Scaling & Bold Text
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "টেক্সট সাইজ ও বোল্ড ফন্ট" else "Typography, Bold Text & Scale") {
                PixelSettingsSwitchItem(
                    icon = Icons.Default.FormatBold,
                    title = if (prefLanguage == "bn") "বোল্ড টেক্সট (Bold Text)" else "Bold Text",
                    subtitle = if (prefLanguage == "bn") {
                        "অ্যাপের সমস্ত লেখার ওজন ও স্পষ্টতা বাড়াতে বোল্ড ফন্ট ব্যবহার করুন"
                    } else {
                        "Render all text across the app with heavier, high-contrast bold font weights"
                    },
                    checked = userProfile.isBoldTextEnabled,
                    onCheckedChange = { isChecked ->
                        viewModel.toggleBoldText(isChecked)
                    },
                    showDivider = true
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = if (prefLanguage == "bn") "অ্যাপের সামগ্রিক ফন্ট সাইজ সামঞ্জস্য করুন" else "Adjust system text size across all views",
                        color = AppSecondaryTextColor(),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        textSizes.forEach { (sizeKey, label) ->
                            val isSelected = userProfile.textSize == sizeKey
                            val appAccent = AppAccentColor()
                            val pillBg = if (isSelected) appAccent else getSettingsItemBg()
                            val pillTextColor = if (isSelected) Color.White else AppTextColor()
                            val pillBorder = if (isSelected) appAccent else getGlassBorder()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(pillBg)
                                    .border(1.dp, pillBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.saveUserProfile(userProfile.copy(textSize = sizeKey))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = pillTextColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3b. Sticky Notes Customization
        item {
            val widthPercent = userProfile.stickyCardWidthPercent.coerceIn(50, 80)
            val previewLines = userProfile.stickyPreviewLines.coerceIn(4, 10)
            PixelSettingsGroup(title = if (prefLanguage == "bn") "স্টিকি নোটস কার্ড ও প্রিভিউ" else "Sticky Notes Card & Preview") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (prefLanguage == "bn") "স্টিকি কার্ডের প্রস্থ (সর্বোচ্চ ৮০%)" else "Sticky Card Width (Max 80%)",
                            color = AppTextColor(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$widthPercent%",
                            color = AppAccentColor(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = widthPercent.toFloat(),
                        onValueChange = { newVal ->
                            viewModel.updateStickyPreferences(newVal.toInt().coerceIn(50, 80), previewLines)
                        },
                        valueRange = 50f..80f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccentColor(),
                            activeTrackColor = AppAccentColor()
                        )
                    )

                    HorizontalDivider(color = getGlassBorder().copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (prefLanguage == "bn") "প্রিভিউ লাইন সংখ্যা (৪ - ১০)" else "Preview Lines (4 to 10 lines)",
                                color = AppTextColor(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (prefLanguage == "bn") "টেক্সট লাইন এবং টেবিল সারি উভয়ই গণনা করা হয়" else "Counts both text lines and table rows (Default: 10)",
                                color = AppSecondaryTextColor(),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "$previewLines lines",
                            color = AppAccentColor(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = previewLines.toFloat(),
                        onValueChange = { newVal ->
                            viewModel.updateStickyPreferences(widthPercent, newVal.toInt().coerceIn(4, 10))
                        },
                        valueRange = 4f..10f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccentColor(),
                            activeTrackColor = AppAccentColor()
                        )
                    )
                }
            }
        }

        // 4. Material 3 Accent Palette
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "মেটেরিয়াল ৩ কালার প্যালেট" else "Material 3 Accent Colors") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    m3AccentPresets.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (accentKey, label, color) ->
                                val isSelected = userProfile.bgPreset == accentKey || (userProfile.bgPreset == "Default" && accentKey == "M3 Ocean Blue")
                                val isLight = isAppLightMode()
                                val itemBg = if (isSelected) color.copy(alpha = if (isLight) 0.12f else 0.20f) else getSettingsItemBg()
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(itemBg)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) color else getGlassBorder(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.saveUserProfile(userProfile.copy(bgPreset = accentKey))
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        color = AppTextColor(),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 5. Interface Features
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "ইন্টারফেস ও অর্ডার সেটিংস" else "Interface & Order Controls") {
                PixelSettingsSwitchItem(
                    icon = Icons.Default.RestaurantMenu,
                    title = if (prefLanguage == "bn") "সাপ্লিমেন্টারি অ্যাড-অন" else "Supplementary Add-ons",
                    subtitle = if (prefLanguage == "bn") "নির্বাচিত খাবারের সাথে অতিরিক্ত পার্শ্ব পদের পরামর্শ" else "Suggest accompanying side dishes for selected meals",
                    checked = userProfile.enableSupplementaryAddons,
                    onCheckedChange = { isChecked ->
                        viewModel.saveUserProfile(userProfile.copy(enableSupplementaryAddons = isChecked))
                    },
                    showDivider = true
                )

                PixelSettingsItem(
                    icon = Icons.Default.Dashboard,
                    title = if (prefLanguage == "bn") "অর্ডার সামারি পজিশন" else "Order Summary Position",
                    subtitle = if (userProfile.mealGroupPosition == "BottomBar") {
                        if (prefLanguage == "bn") "নিচের বারে ডক করা" else "Docked at Bottom Bar"
                    } else {
                        if (prefLanguage == "bn") "পৃষ্ঠার নিচে এম্বেড করা" else "Embedded at Page Bottom"
                    },
                    onClick = {
                        val newPos = if (userProfile.mealGroupPosition == "BottomBar") "PageBottom" else "BottomBar"
                        viewModel.saveUserProfile(userProfile.copy(mealGroupPosition = newPos))
                    }
                )
            }
        }

        // 6. Utility & Rates Config
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "ইউটিলিটি ও মিটার সেটিংস" else "Utility & Billing Configuration") {
                PixelSettingsItem(
                    icon = Icons.Default.ElectricMeter,
                    title = if (prefLanguage == "bn") "মিটার রিডিং ও বিল কনফিগারেশন" else "Meter Reading & Rates Config",
                    subtitle = if (prefLanguage == "bn") "বিদ্যুৎ ও ইউটিলিটি টেমপ্লেট পরিচালনা করুন" else "Manage electricity rates and billing templates",
                    onClick = onOpenUtilityConfig
                )
            }
        }
    }
}
