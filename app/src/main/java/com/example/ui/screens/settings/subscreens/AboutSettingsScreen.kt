package com.example.ui.screens.settings.subscreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.screens.LegalInfoDialog
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun AboutSettingsScreen(
    viewModel: FinancialViewModel,
    onBack: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile.language

    var activeLegalDialog by remember { mutableStateOf<String?>(null) }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (activeLegalDialog != null) {
            activeLegalDialog = null
        } else {
            onBack()
        }
    }

    if (activeLegalDialog != null) {
        LegalInfoDialog(
            type = activeLegalDialog!!,
            prefLanguage = prefLanguage,
            onDismiss = { activeLegalDialog = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. App Identity Banner (Hero Banner)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    color = getCardBackground(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(colorResource(id = com.example.R.color.ic_launcher_background), CircleShape)
                                .border(1.5.dp, AppAccentColor().copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.app_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Traker",
                            color = AppTextColor(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (prefLanguage == "bn") "সংস্করণ ১.৩.০ (Pure Material 3 ও ডুয়াল-মোড স্পিড)" else "Version 1.3.0 (Pure Material 3 & Dual-Mode Speed)",
                            color = AppAccentColor(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (prefLanguage == "bn") "অফলাইন-ফার্স্ট স্মার্ট আর্থিক ও হোটেল ব্যয় ব্যবস্থাপনা" else "Offline-first smart expense & hotel management",
                            color = AppSecondaryTextColor(),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 2. Legal & Policies Section
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "আইনি ও পলিসি তথ্য" else "Legal & Policy Information") {
                PixelSettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = if (prefLanguage == "bn") "প্রাইভেসি পলিসি" else "Privacy Policy",
                    subtitle = if (prefLanguage == "bn") "১০০% অফলাইন তথ্য সুরক্ষা নীতি" else "100% offline data security guarantees",
                    onClick = { activeLegalDialog = "privacy" },
                    showDivider = true
                )
                PixelSettingsItem(
                    icon = Icons.Default.Gavel,
                    title = if (prefLanguage == "bn") "লাইসেন্স ও শর্তাবলী" else "Licenses & Open Source",
                    subtitle = if (prefLanguage == "bn") "Apache 2.0 ও থার্ড-পার্টি লাইব্রেরি" else "Apache 2.0 & Third-party acknowledgments",
                    onClick = { activeLegalDialog = "license" },
                    showDivider = true
                )
                PixelSettingsItem(
                    icon = Icons.Default.Info,
                    title = if (prefLanguage == "bn") "অ্যাপ্লিকেশন পরিচিতি ও ব্যবহারবিধি" else "Application Overview & Guide",
                    subtitle = if (prefLanguage == "bn") "সম্পূর্ণ ব্যবহারের নির্দেশিকা ও ডেভেলপার তথ্য" else "Complete user guide, how to use & developer info",
                    onClick = { activeLegalDialog = "info" },
                    showDivider = true
                )
                PixelSettingsItem(
                    icon = Icons.Default.AccountCircle,
                    title = if (prefLanguage == "bn") "ডেভেলপার প্রোফাইল" else "Developer Profile",
                    subtitle = "GitHub: @suvadippatra",
                    onClick = { activeLegalDialog = "info" }
                )
            }
        }

        // 3. Privacy & Security Guarantees
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "গোপনীয়তার নিশ্চয়তা" else "Privacy Commitments") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val appAccent = AppAccentColor()
                    val isLight = isAppLightMode()
                    val iconBg = appAccent.copy(alpha = if (isLight) 0.12f else 0.16f)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = appAccent, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (prefLanguage == "bn") "সম্পূর্ণ অফলাইন ডাটাবেস" else "Zero Cloud Transmission",
                                color = AppTextColor(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (prefLanguage == "bn") "আপনার কোনো হিসাব বা ব্যক্তিগত তথ্য সার্ভারে পাঠানো হয় না।" else "Your financial accounts and transactions never leave this physical device.",
                                color = AppSecondaryTextColor(),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    HorizontalDivider(color = getGlassBorder().copy(alpha = 0.35f), modifier = Modifier.padding(start = 56.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = appAccent, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (prefLanguage == "bn") "ডিভাইস-লেভেল এনক্রিপশন" else "Encrypted Local Storage",
                                color = AppTextColor(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (prefLanguage == "bn") "রুম ডাটাবেস ও বায়োমেট্রিক সিকিউরিটি দিয়ে সুরক্ষিত।" else "Protected with SQLite Room and biometric authentication.",
                                color = AppSecondaryTextColor(),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
