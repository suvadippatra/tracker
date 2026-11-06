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
import com.example.BuildConfig
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

private data class PublicRepoReference(
    val name: String,
    val category: String,
    val license: String,
    val urlOrSource: String
)

@Composable
fun AboutSettingsScreen(
    viewModel: FinancialViewModel,
    onBack: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile.language
    val appVersion = BuildConfig.VERSION_NAME.ifBlank { "0.0.1" }
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
            appVersion = appVersion,
            onDismiss = { activeLegalDialog = null }
        )
    }

    val quickHelpItems = remember {
        listOf(
            HelpGuideQuickItem(
                icon = Icons.Default.StickyNote2,
                titleEn = "Theme-Aware Sticky Cards, Pin/Clip Badge (📌📎) & Borderless Tables",
                titleBn = "ডিফল্ট সাদা/কালো স্টিকি কার্ড, পিন/ক্লিপ আইকন (📌📎) ও বর্ডারলেস টেবিল",
                descEn = "Default sticky cards match Light/Dark theme (White/Black) while supporting custom colors. Landing cards display a realistic Pin (📌) or Paperclip (📎) badge without cluttered edit/delete buttons. Opened notes show Title in the top bar, Sub-heading before body text, a Triple-Dot (⋮) menu (Edit, Remove, Show Preview, Attachment, Share, Attached Icon), borderless visual tables, and single-tap/long-press attachment actions.",
                descBn = "ডিফল্ট স্টিকি কার্ড থিম অনুযায়ী সাদা বা কালো হয়। ল্যান্ডিং কার্ডে পিন (📌) বা ক্লিপ (📎) আইকন দেখায়। নোট খুললে টপ বারে শিরোনাম, মূল লেখার আগে সাব-হেডিং, ট্রিপল-ডট (⋮) মেনু, বর্ডারলেস টেবিল এবং অ্যাটাচমেন্ট প্রিভিউ ও লং-প্রেস অপশন পাওয়া যায়।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.QrCodeScanner,
                titleEn = "Uncropped Offline Khata Scanner & Existing vs New Date Resolver",
                titleBn = "আনক্রপড অফলাইন খাতা স্ক্যানার ও পুরনো বনাম নতুন তারিখ সমাধান",
                descBn = "খাতার ছবি ১৬:৯ ক্রপ ছাড়াই সম্পূর্ণ দেখা যায় এবং ট্যাবলেটে ফ্লিকার ছাড়াই কাজ করে। `9.10.26 - 60+50`-এর মতো যেকোনো ফরম্যাট বোঝে এবং ডাটাবেসে তারিখ আগে থেকেই থাকলে পুরনো রেকর্ড রাখবেন নাকি নতুন ৪-অপশন মেনু কম্বিনেশন নেবেন তা বেছে নেওয়ার সুযোগ দেয়।",
                descEn = "Displays captured Khata photos fully uncropped with tap-to-zoom and zero tablet flicker. Parses flexible lines like '9.10.26 - 60+50' even when spaces vary, and when a date already exists in the database, lets you choose between Keeping Existing or Using New (with up to 4 dish combination options per price)."
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.RestaurantMenu,
                titleEn = "Compact Add Meal Grid, Top-Bar Day/Night Icon & Custom Calculator",
                titleBn = "কমপ্যাক্ট অ্যাড মিল গ্রিড, টপ-বার দিন/রাত আইকন ও ক্যালকুলেটর কিবোর্ড",
                descEn = "Add Meal features a single-icon Day/Night toggle in the top bar, 2-to-4 column responsive dish cards, smooth in-place expansion for 'View More Dishes', and a Custom Meal + Amount builder at the bottom powered by an on-screen Calculator Keyboard (+, -, ×, ÷, 00, Enter).",
                descBn = "অ্যাড মিল পেজে টপ বারে দিন/রাত আইকন, ২-৪ কলামের ডিশ কার্ড, স্মুথ 'View More Dishes' অ্যানিমেশন এবং নিচে অন-স্ক্রিন ক্যালকুলেটর কিবোর্ডসহ কাস্টম খাবার ও দাম যোগ করার সুবিধা রয়েছে।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.ElectricMeter,
                titleEn = "No-Wrap Projected Utility Bill Card & Multi-Region Rate Breakdown",
                titleBn = "প্রজেক্টেড ইউটিলিটি বিল কার্ড ও বিস্তারিত রেট রিজিওন ব্রেকডাউন",
                descEn = "Displays Date Range, Reading Range (start → end kWh), Total Units @ Rate, and Total Price cleanly without text wrapping, repeating for each rate region when multiple rate policies apply.",
                descBn = "প্রতিটি রেট পলিসি অঞ্চলের জন্য তারিখের সীমা, রিডিং সীমা (শুরু → শেষ ইউনিট), মোট ইউনিট ও মোট মূল্য কোনো টেক্সট ভাঙা ছাড়াই স্পষ্টভাবে প্রদর্শন করে।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.Image,
                titleEn = "2MB Auto-Compression, Payment Status Image Attachment & Split-Screen",
                titleBn = "২ এমবি অটো-কম্প্রেশন, পেমেন্ট স্ট্যাটাস ছবি সংযুক্তি ও স্প্লিট-স্ক্রিন",
                descEn = "All image attachments (Sticky, Transactions, Meal Payment Status popup, Profile Photo) are automatically compressed under 2MB JPEG. Meal Payment Status supports image proofs synced across Universal Transfer, Whole Hotel Data & Order History. Android 8 split-screen keeps the bottom navigation bar visible.",
                descBn = "সব ছবির অ্যাটাচমেন্ট স্বয়ংক্রিয়ভাবে ২ এমবি-র নিচে JPEG কম্প্রেস হয়। পেমেন্ট স্ট্যাটাস পপআপে ছবি সংযুক্ত করার সুবিধা ও এক্সপোর্ট সিঙ্ক যোগ করা হয়েছে এবং অ্যান্ড্রয়েড ৮ স্প্লিট-স্ক্রিনে বটম নেভিগেশন বার সর্বদা দৃশ্যমান থাকে।"
            )
        )
    }

    val publicReferences = remember {
        listOf(
            PublicRepoReference(
                name = "Android Jetpack Compose & Material 3",
                category = "Adaptive UI Framework, Split-Screen Insets & Zero-Alloc Theme Cache",
                license = "Apache License 2.0",
                urlOrSource = "https://developer.android.com/jetpack/compose"
            ),
            PublicRepoReference(
                name = "AndroidX Room Persistence Library (SQLite)",
                category = "Local Relational Database (v36 Schema)",
                license = "Apache License 2.0",
                urlOrSource = "https://developer.android.com/training/data-storage/room"
            ),
            PublicRepoReference(
                name = "Google Fonts: Archivo Black & Archivo",
                category = "Bundled Offline Typography Assets (OFL)",
                license = "SIL Open Font License 1.1",
                urlOrSource = "https://fonts.google.com/specimen/Archivo+Black"
            ),
            PublicRepoReference(
                name = "Android Graphics Bitmap & ExifInterface Pipeline",
                category = "2MB Automatic JPEG Downsampling, Compression & Offline Optical Matrix",
                license = "Apache License 2.0",
                urlOrSource = "https://developer.android.com/topic/performance/graphics"
            ),
            PublicRepoReference(
                name = "ZXing (Zebra Crossing) & CameraX",
                category = "Offline QR Stream Encoding, Decoding & Optical Capture",
                license = "Apache License 2.0",
                urlOrSource = "https://github.com/zxing/zxing"
            ),
            PublicRepoReference(
                name = "Coil (Coroutine Image Loader)",
                category = "Asynchronous Local Media & Downsampled Bitmap Rendering",
                license = "Apache License 2.0",
                urlOrSource = "https://github.com/coil-kt/coil"
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hero Card with Static Non-Editable App Version Badge
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
                            color = colorResource(id = com.example.R.color.ic_launcher_background)
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

                        // Static read-only version pill (non-editable)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = appAccent.copy(alpha = if (isLight) 0.12f else 0.2f),
                            modifier = Modifier.testTag("app_version_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (prefLanguage == "bn") {
                                        "সংস্করণ $appVersion • মেটেরিয়াল ৩"
                                    } else {
                                        "Version $appVersion • Material 3"
                                    },
                                    color = appAccent,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (prefLanguage == "bn") {
                                "অফলাইন-ফার্স্ট স্মার্ট আর্থিক, হোটেল খাতা ও বিদ্যুৎ বিল ব্যবস্থাপনা"
                            } else {
                                "Offline-first smart financial, hotel khata & electricity bill tracker"
                            },
                            color = AppSecondaryTextColor(),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

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
                                icon = Icons.Default.Security,
                                label = if (prefLanguage == "bn") "জিরো API কী" else "Zero Secrets/Keys",
                                accent = appAccent,
                                isLight = isLight
                            )
                        }
                    }
                }
            }
        }

        // 2. Help & Complete User Guide Section
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
                                        "সম্পূর্ণ ব্যবহারবিধি ও নতুন আপডেট খুলুন (v$appVersion)"
                                    } else {
                                        "Open Full Help & Feature Guide (v$appVersion)"
                                    },
                                    color = AppTextColor(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (prefLanguage == "bn") {
                                        "খাতা স্ক্যানার, ভিজ্যুয়াল টেবিল, মেনু কার্যকর তারিখ ও বিল নির্দেশিকা পড়ুন"
                                    } else {
                                        "Detailed walkthrough of Khata Scanner, Visual Tables, Menu Effective Dates & Bill Math"
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

        // 3. Public Knowledge, Open-Source Repositories & Databases Reference List
        item {
            PixelSettingsGroup(
                title = if (prefLanguage == "bn") "ব্যবহৃত ওপেন-সোর্স রিপোজিটরি, ডাটাবেস ও পাবলিক নলেজ" else "Public Knowledge, Open-Source Repos & Databases"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (prefLanguage == "bn") {
                            "এই অ্যাপটি নির্মাণে নিচের ওপেন-সোর্স লাইব্রেরি, লোকাল ডাটাবেস এবং পাবলিক ডকুমেন্টেশন ব্যবহৃত হয়েছে (কোনো ব্যক্তিগত API কী বা গোপন তথ্য কোডে নেই):"
                        } else {
                            "Reference list of open-source repositories, local database engines, typography assets, and public specifications used in this project (zero hardcoded keys or personal data in source):"
                        },
                        color = AppSecondaryTextColor(),
                        style = MaterialTheme.typography.bodySmall
                    )

                    publicReferences.forEachIndexed { idx, ref ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ref.name,
                                    color = AppTextColor(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = appAccent.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = ref.license,
                                        color = appAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = ref.category,
                                color = AppSecondaryTextColor(),
                                fontSize = 12.sp
                            )
                            Text(
                                text = ref.urlOrSource,
                                color = appAccent.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                        if (idx < publicReferences.lastIndex) {
                            HorizontalDivider(color = AppSecondaryTextColor().copy(alpha = 0.10f))
                        }
                    }
                }
            }
        }

        // 4. Privacy & Security Commitments
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
                                text = if (prefLanguage == "bn") "সম্পূর্ণ অফলাইন ও গিটহাব প্রাইভেসি অডিট" else "Zero Cloud Transmission & GitHub Privacy Audited",
                                color = AppTextColor(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (prefLanguage == "bn") "আপনার কোনো হিসাব, ব্যক্তিগত তথ্য বা API কী কোডে বা সার্ভারে প্রকাশ করা হয় না।" else "No personal data, credentials, or API keys are embedded in source code or sent to any server.",
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

        // 5. Legal, Licenses & Developer Profile (Read-Only App Version)
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "আইনি ও ডেভেলপার তথ্য" else "Legal & Developer Info") {
                PixelSettingsItem(
                    icon = Icons.Default.Numbers,
                    title = if (prefLanguage == "bn") "অ্যাপ সংস্করণ (App Version)" else "App Version",
                    subtitle = "v$appVersion",
                    onClick = {},
                    showDivider = true
                )
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
                    subtitle = if (prefLanguage == "bn") "Apache 2.0 ও ওপেন-সোর্স লাইব্রেরি" else "Apache 2.0 & Third-party acknowledgments",
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
