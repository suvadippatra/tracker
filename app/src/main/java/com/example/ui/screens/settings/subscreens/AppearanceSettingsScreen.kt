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

    val isGlassEnabled = userProfile.enableLiquidGlassTheme

    val themeModes = listOf(
        Triple("Auto", if (prefLanguage == "bn") "সিস্টেম অনুযায়ী (অটো)" else "System Default (Auto)", Icons.Default.BrightnessAuto),
        Triple("Dark", if (prefLanguage == "bn") "ডার্ক মোড (পিওর ব্ল্যাক)" else "Dark Mode (Pure Black)", Icons.Default.DarkMode),
        Triple("Light", if (prefLanguage == "bn") "লাইট মোড (উজ্জ্বল)" else "Light Mode (Clean)", Icons.Default.LightMode)
    )

    val textSizes = listOf(
        Pair("Small", if (prefLanguage == "bn") "ছোট" else "Small"),
        Pair("Medium", if (prefLanguage == "bn") "সাধারণ" else "Medium"),
        Pair("Large", if (prefLanguage == "bn") "বড়" else "Large")
    )

    val isLightMode = isAppLightMode()

    val bgPresets = if (isLightMode) {
        listOf(
            Triple("Soft Minimal Light", if (prefLanguage == "bn") "সফট মিনিমাল লাইট" else "Soft Minimal", listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))),
            Triple("Sunset Warm", if (prefLanguage == "bn") "সানসেট ওয়ার্ম" else "Sunset Warm", listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5))),
            Triple("Midnight Blue", if (prefLanguage == "bn") "ওশান গ্লো" else "Ocean Glow", listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE)))
        )
    } else {
        listOf(
            Triple("Default", if (prefLanguage == "bn") "পিওর ব্ল্যাক" else "Pure Black", listOf(Color(0xFF000000), Color(0xFF101010))),
            Triple("Midnight Blue", if (prefLanguage == "bn") "মিডনাইট ব্লু" else "Midnight Blue", listOf(Color(0xFF0F172A), Color(0xFF0284C7))),
            Triple("Emerald Dark", if (prefLanguage == "bn") "ইমারেল্ড ডার্ক" else "Emerald Dark", listOf(Color(0xFF064E3B), Color(0xFF0F766E))),
            Triple("Sunset Warm", if (prefLanguage == "bn") "সানসেট ওয়ার্ম" else "Sunset Warm", listOf(Color(0xFF451A03), Color(0xFF9A3412))),
            Triple("Purple Twilight", if (prefLanguage == "bn") "পার্পল টুইলাইট" else "Purple Twilight", listOf(Color(0xFF2E1065), Color(0xFF581C87)))
        )
    }

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
        // 1. Performance & Android 8 Mode + Visual Style
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "পারফরম্যান্স ও ইউজার ইন্টারফেস স্টাইল" else "Performance & Visual Style") {
                PixelSettingsSwitchItem(
                    icon = Icons.Default.Speed,
                    title = if (prefLanguage == "bn") "ভারী ইফেক্ট বন্ধ ও নেটিভ কন্ট্রোল (Android 8 ফাস্ট মোড)" else "Disable Heavy Effects & Use Native Controls",
                    subtitle = if (prefLanguage == "bn") {
                        "অ্যান্ড্রয়েড ৮+ এ দ্রুত গতির জন্য ফ্ল্যাট মেটেরিয়াল ৩ সারফেস, নেটিভ ডেট/টাইম পিকার ও হালকা ডিজিটাল স্টেপার ব্যবহার করুন"
                    } else {
                        "Use flat Material 3 surfaces, native Android date/time dialogs, and lightweight controls for instant response on Android 8+"
                    },
                    checked = userProfile.disableHeavyEffects,
                    onCheckedChange = { isChecked ->
                        viewModel.toggleDisableHeavyEffects(isChecked)
                    },
                    showDivider = true
                )
                PixelSettingsSwitchItem(
                    icon = Icons.Default.BlurOn,
                    title = if (prefLanguage == "bn") "লিকুইড গ্লাস স্টাইল" else "Liquid Glass Visual Style",
                    subtitle = if (prefLanguage == "bn") {
                        "স্বচ্ছ গ্লাসমরফিজম ও অ্যাম্বিয়েন্ট গ্র্যাডিয়েন্ট ব্যাকগ্রাউন্ড সক্ষম করুন"
                    } else {
                        "Enable translucent glassmorphism and ambient gradient backdrops"
                    },
                    checked = isGlassEnabled && !userProfile.disableHeavyEffects,
                    onCheckedChange = { isChecked ->
                        viewModel.toggleLiquidGlassTheme(isChecked)
                    }
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

        // 3. Text Scaling
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "টেক্সট সাইজ ও স্কেলিং" else "Typography & Text Scale") {
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

        // 4. Ambient Backgrounds / Material 3 Accent Palette
        item {
            if (isGlassEnabled) {
                PixelSettingsGroup(title = if (prefLanguage == "bn") "অ্যাম্বিয়েন্ট ব্যাকগ্রাউন্ড প্রিসেট" else "Ambient Glass Backgrounds") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bgPresets.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { (presetKey, label, colors) ->
                                    val isSelected = userProfile.bgPreset == presetKey || (presetKey == "Default" && userProfile.bgPreset.isBlank())
                                    val appAccent = AppAccentColor()
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Brush.horizontalGradient(colors))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) appAccent else Color.White.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                viewModel.saveUserProfile(userProfile.copy(bgPreset = presetKey))
                                            }
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(appAccent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
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
            } else {
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
        }

        // 5. Animation & Interface Features
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "অ্যানিমেশন ও ইন্টারফেস" else "Animation & Interface Controls") {
                if (isGlassEnabled) {
                    PixelSettingsSwitchItem(
                        icon = Icons.Default.AutoAwesome,
                        title = if (prefLanguage == "bn") "ব্যাকগ্রাউন্ড স্টার অ্যানিমেশন" else "Ambient Star Animation",
                        subtitle = if (prefLanguage == "bn") "পটভূমিতে মৃদু নক্ষত্র প্রভাব" else "Subtle background sparkle effect",
                        checked = userProfile.enableStarAnimation,
                        onCheckedChange = { isChecked ->
                            viewModel.saveUserProfile(userProfile.copy(enableStarAnimation = isChecked))
                        },
                        showDivider = true
                    )
                }

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
