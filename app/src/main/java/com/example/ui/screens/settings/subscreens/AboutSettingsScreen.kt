package com.example.ui.screens.settings.subscreens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.*
import com.example.ui.screens.LegalInfoDialog
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun AboutSettingsScreen(viewModel: FinancialViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile.language

    var activeLegalDialog by remember { mutableStateOf<String?>(null) }

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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. App Identity Banner (Locked App Logo)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(getGlassIconButtonBg(), CircleShape)
                            .border(1.dp, AccentCyan.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = com.example.R.drawable.app_logo,
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Traker",
                        color = AppTextColor(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (prefLanguage == "bn") "সংস্করণ ১.০.০ (রিলিজ)" else "Version 1.0.0 (Release)",
                        color = AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (prefLanguage == "bn") "অফলাইন-ফার্স্ট স্মার্ট আর্থিক ও হোটেল ব্যয় ব্যবস্থাপনা" else "Offline-first smart expense & hotel management",
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // 2. Legal & Policies Section
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "আইনি ও পলিসি তথ্য" else "Legal & Policy Information") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PixelSettingsItem(
                        icon = Icons.Default.PrivacyTip,
                        title = if (prefLanguage == "bn") "প্রাইভেসি পলিসি" else "Privacy Policy",
                        subtitle = if (prefLanguage == "bn") "১০০% অফলাইন তথ্য সুরক্ষা নীতি" else "100% offline data security guarantees",
                        onClick = { activeLegalDialog = "privacy" }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PixelSettingsItem(
                        icon = Icons.Default.Gavel,
                        title = if (prefLanguage == "bn") "লাইসেন্স ও শর্তাবলী" else "Licenses & Open Source",
                        subtitle = if (prefLanguage == "bn") "Apache 2.0 ও থার্ড-পার্টি লাইব্রেরি" else "Apache 2.0 & Third-party acknowledgments",
                        onClick = { activeLegalDialog = "license" }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PixelSettingsItem(
                        icon = Icons.Default.Info,
                        title = if (prefLanguage == "bn") "অ্যাপ্লিকেশন পরিচিতি" else "Application Overview",
                        subtitle = if (prefLanguage == "bn") "ডেভেলপার ও সিস্টেম তথ্য" else "Developer details & system specifications",
                        onClick = { activeLegalDialog = "info" }
                    )
                }
            }
        }

        // 3. Privacy & Security Guarantees
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "গোপনীয়তার নিশ্চয়তা" else "Privacy Commitments") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (prefLanguage == "bn") "সম্পূর্ণ অফলাইন ডাটাবেস" else "Zero Cloud Transmission",
                                color = AppTextColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (prefLanguage == "bn") "আপনার কোনো হিসাব বা ব্যক্তিগত তথ্য সার্ভারে পাঠানো হয় না।" else "Your financial accounts and transactions never leave this physical device.",
                                color = AppSecondaryTextColor(),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (prefLanguage == "bn") "ডিভাইস-লেভেল এনক্রিপশন" else "Encrypted Local Storage",
                                color = AppTextColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (prefLanguage == "bn") "রুম ডাটাবেস ও বায়োমেট্রিক সিকিউরিটি দিয়ে সুরক্ষিত।" else "Protected with SQLite Room and biometric authentication.",
                                color = AppSecondaryTextColor(),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
