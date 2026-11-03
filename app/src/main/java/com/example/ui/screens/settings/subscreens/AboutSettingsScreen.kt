package com.example.ui.screens.settings.subscreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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

private data class HelpGuideQuickItem(
    val icon: ImageVector,
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String
)

@Composable
fun AboutSettingsScreen(
    viewModel: FinancialViewModel,
    onBack: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile.language
    val isLight = isAppLightMode()
    val appAccent = AppAccentColor()

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

    val quickHelpItems = remember {
        listOf(
            HelpGuideQuickItem(
                icon = Icons.Default.ElectricMeter,
                titleEn = "Smart Bill & Meter Reading Calculator",
                titleBn = "স্মার্ট বিল ও মিটার রিডিং ক্যালকুলেটর",
                descEn = "Interpolates multiple meter readings between any two dates accurately. If readings stop updating after the last unpaid bill, the total preserves your unpaid balance + ₹0 new usage.",
                descBn = "যেকোনো দুটি তারিখের মধ্যে একাধিক মিটার রিডিং নিখুঁতভাবে গণনা করে। রিডিং আপডেট বন্ধ থাকলেও বকেয়া বিলের সাথে ₹০ নতুন চার্জ যোগ করে সঠিক হিসাব দেখায়।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.AutoAwesome,
                titleEn = "Borderless Native Shadows & Plus Jakarta Sans",
                titleBn = "বর্ডারলেস নেটিভ শ্যাডো ও প্লাস জাকার্তা স্যান্স ফন্ট",
                descEn = "All harsh card and button borders are removed in favor of smooth Android hardware elevation shadows and unified Plus Jakarta Sans typography.",
                descBn = "সকল কার্ড ও বাটনের বর্ডার সরিয়ে অ্যান্ড্রয়েডের নেটিভ শ্যাডো এবং আধুনিক Plus Jakarta Sans ফন্ট যুক্ত করা হয়েছে।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.DarkMode,
                titleEn = "Instant Light & Dark Splash Detection",
                titleBn = "তাৎক্ষণিক লাইট ও ডার্ক স্প্ল্যাশ ডিটেকশন",
                descEn = "Detects your system or saved theme before the first frame renders, completely eliminating any black screen flash before the splash animation.",
                descBn = "অ্যানিমেশন শুরুর আগেই লাইট বা ডার্ক থিম শনাক্ত করে, ফলে অ্যাপ চালুর সময় কোনো কালো স্ক্রিন দেখা যায় না।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.Speed,
                titleEn = "Dual-Mode UI & Tablet Menu Navigation",
                titleBn = "ডুয়াল-মোড ইউআই ও ট্যাবলেট নেভিগেশন",
                descEn = "Switch between Liquid Glass and Material 3 with Fast/Smooth animation speed, plus a unified triple-line menu button on tablet sidebars.",
                descBn = "লিকুইড গ্লাস ও মেটেরিয়াল ৩ থিমের সাথে ফাস্ট/স্মুথ অ্যানিমেশন স্পিড এবং ট্যাবলেটের সাইডবারে ট্রিপল-লাইন মেনু বাটন।"
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Redesigned Modern Hero Card (Native Elevation Shadow, Zero Borders)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = getCardBackground(),
                    shadowElevation = if (isLight) 4.dp else 6.dp,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(84.dp),
                            shape = CircleShape,
                            color = colorResource(id = com.example.R.color.ic_launcher_background),
                            shadowElevation = 6.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.app_logo),
                                    contentDescription = "App Logo",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Traker",
                            color = AppTextColor(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = appAccent.copy(alpha = if (isLight) 0.12f else 0.2f)
                        ) {
                            Text(
                                text = if (prefLanguage == "bn") {
                                    "সংস্করণ ০.০.১ • নেটিভ শ্যাডো ও মেটেরিয়াল ৩"
                                } else {
                                    "Version 0.0.1 • Native Shadow & Material 3"
                                },
                                color = appAccent,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (prefLanguage == "bn") {
                                "অফলাইন-ফার্স্ট স্মার্ট আর্থিক, হোটেল ও বিদ্যুৎ বিল ব্যবস্থাপনা"
                            } else {
                                "Offline-first smart financial, hotel & electricity bill tracker"
                            },
                            color = AppSecondaryTextColor(),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // 2x2 Feature Pill Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AboutFeatureBadge(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.CloudOff,
                                label = if (prefLanguage == "bn") "১০০% অফলাইন" else "100% Offline",
                                accent = appAccent,
                                isLight = isLight
                            )
                            AboutFeatureBadge(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Bolt,
                                label = if (prefLanguage == "bn") "স্মার্ট মিটার বিল" else "Smart Bill Calc",
                                accent = appAccent,
                                isLight = isLight
                            )
                        }
                    }
                }
            }
        }

        // 2. Help & Complete User Guide Section (Embedded inside About Page)
        item {
            PixelSettingsGroup(
                title = if (prefLanguage == "bn") "সহায়তা ও ব্যবহারের নির্দেশিকা (Help Section)" else "Help & User Guide"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary interactive banner to launch full documentation modal
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { activeLegalDialog = "info" }
                            .testTag("open_full_help_guide_button"),
                        shape = RoundedCornerShape(16.dp),
                        color = appAccent.copy(alpha = if (isLight) 0.10f else 0.18f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(appAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Complete Help Guide",
                                    tint = appAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (prefLanguage == "bn") {
                                        "সম্পূর্ণ ব্যবহারবিধি ও নতুন আপডেট খুলুন"
                                    } else {
                                        "Open Full Help & Feature Guide"
                                    },
                                    color = AppTextColor(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (prefLanguage == "bn") {
                                        "সকল ফিচার, বিল ক্যালকুলেটর ও ব্যাকআপ নির্দেশিকা বিস্তারিত পড়ুন"
                                    } else {
                                        "Detailed walkthrough of all modules, bill math & latest updates"
                                    },
                                    color = AppSecondaryTextColor(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = appAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // In-page Help Cards for Immediate Reference
                    quickHelpItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(appAccent.copy(alpha = if (isLight) 0.12f else 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = appAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (prefLanguage == "bn") item.titleBn else item.titleEn,
                                    color = AppTextColor(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (prefLanguage == "bn") item.descBn else item.descEn,
                                    color = AppSecondaryTextColor(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (index < quickHelpItems.lastIndex) {
                            HorizontalDivider(
                                color = AppSecondaryTextColor().copy(alpha = 0.10f),
                                modifier = Modifier.padding(start = 52.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Privacy & Security Commitments
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "গোপনীয়তার নিশ্চয়তা" else "Privacy Commitments") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (prefLanguage == "bn") "আপনার কোনো হিসাব বা ব্যক্তিগত তথ্য সার্ভারে পাঠানো হয় না।" else "Your financial accounts and transactions never leave this physical device.",
                                color = AppSecondaryTextColor(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    HorizontalDivider(
                        color = AppSecondaryTextColor().copy(alpha = 0.12f),
                        modifier = Modifier.padding(start = 56.dp)
                    )

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
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (prefLanguage == "bn") "রুম ডাটাবেস ও বায়োমেট্রিক সিকিউরিটি দিয়ে সুরক্ষিত।" else "Protected with SQLite Room and biometric authentication.",
                                color = AppSecondaryTextColor(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // 4. Legal, Licenses & Developer Profile
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "আইনি ও ডেভেলপার তথ্য" else "Legal & Developer Info") {
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
                    icon = Icons.Default.AccountCircle,
                    title = if (prefLanguage == "bn") "ডেভেলপার প্রোফাইল" else "Developer Profile",
                    subtitle = "GitHub: @suvadippatra",
                    onClick = { activeLegalDialog = "info" }
                )
            }
        }
    }
}

@Composable
private fun AboutFeatureBadge(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    accent: Color,
    isLight: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isLight) Color(0xFFF3F6F9) else Color(0xFF1A242D)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = AppTextColor(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
