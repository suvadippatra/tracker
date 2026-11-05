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
    val appVersion = userProfile.appVersion.ifBlank { "0.0.1" }
    val isLight = isAppLightMode()
    val appAccent = AppAccentColor()

    var activeLegalDialog by remember { mutableStateOf<String?>(null) }
    var showEditVersionDialog by remember { mutableStateOf(false) }
    var versionDraft by remember(appVersion) { mutableStateOf(appVersion) }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (activeLegalDialog != null) {
            activeLegalDialog = null
        } else if (showEditVersionDialog) {
            showEditVersionDialog = false
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

    if (showEditVersionDialog) {
        AlertDialog(
            onDismissRequest = { showEditVersionDialog = false },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = if (prefLanguage == "bn") "অ্যাপ সংস্করণ (Version) পরিবর্তন" else "Update App Version",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (prefLanguage == "bn") {
                            "এই সংস্করণ নম্বরটি সেটিংস, অ্যাবাউট পেজ, হেল্প গাইড এবং ব্যাকআপে স্বয়ংক্রিয়ভাবে সিঙ্ক হবে।"
                        } else {
                            "Changing the version number here syncs it across About, Help Guide, and exported backups."
                        },
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = versionDraft,
                        onValueChange = { versionDraft = it },
                        singleLine = true,
                        label = { Text(if (prefLanguage == "bn") "সংস্করণ (যেমন 0.0.2)" else "Version (e.g. 0.0.2)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = appAccent,
                            unfocusedBorderColor = getGlassBorder()
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = versionDraft.trim().ifBlank { "0.0.1" }
                        viewModel.updateAppVersion(cleaned)
                        showEditVersionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appAccent,
                        contentColor = AppOnAccentColor(appAccent)
                    )
                ) {
                    Text(if (prefLanguage == "bn") "সংরক্ষণ করুন" else "Save Version", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditVersionDialog = false }) {
                    Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AppSecondaryTextColor())
                }
            }
        )
    }

    val quickHelpItems = remember {
        listOf(
            HelpGuideQuickItem(
                icon = Icons.Default.StickyNote2,
                titleEn = "Google Keep-Style Sticky Notes, Custom Tags & 8-Icon Toolbar",
                titleBn = "গুগল কিপ ধাঁচের স্টিকি নোট, কাস্টম ট্যাগ ও ৮-আইকন টুলবার",
                descEn = "Plain Material 3 surface editor (Title + Body, no inner cards), custom #tags with top-bar tag filter, scroll-safe 3-second hold selection, harmonic wobbling, alternating left/right mobile cards, staggered tablet wall columns, and a single-line 8-icon bottom toolbar (Bold, Italic, Align Left/Center/Right/Justify, Table, Attach).",
                descBn = "প্লেইন মেটেরিয়াল ৩ সারফেস এডিটর (শুধু টাইটেল ও বডি), কাস্টম ট্যাগ ও টপ-বার ট্যাগ ফিল্টার, স্ক্রল-বান্ধব ৩-সেকেন্ড লং-প্রেস সিলেকশন, ওবলিং অ্যানিমেশন, মোবাইল বাম-ডান কার্ড বিন্যাস, ট্যাবলেট অসম কলাম এবং নিচে এক লাইনে ৮টি ফরম্যাটিং আইকন।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.ElectricMeter,
                titleEn = "Strict Date-Range Bill Calculator & Rate Policy Grouping",
                titleBn = "নির্দিষ্ট তারিখ-ভিত্তিক বিল ক্যালকুলেটর ও রেট পলিসি গ্রুপিং",
                descEn = "Strictly calculates electricity units between selected Start and End dates (e.g., 15 Aug to 17 Oct = 120 kWh) with group-by-group rate boundaries and linear interpolation for intermediate dates. Default rate input is hidden in favor of Rate Policies.",
                descBn = "নির্বাচিত শুরুর ও শেষের তারিখের মধ্যে নিখুঁতভাবে ইউনিট হিসাব করে (যেমন ১৫ আগস্ট থেকে ১৭ অক্টোবর = ১২০ ইউনিট), রেট পরিবর্তনের সীমানা ও ইন্টারপোলেশনসহ। ডিফল্ট রেটের পরিবর্তে রেট পলিসি ব্যবহৃত হয়।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.QrCodeScanner,
                titleEn = "Scan Khata to Import & Menu Modification History",
                titleBn = "খাতা স্ক্যান করে ইমপোর্ট এবং মেনু পরিবর্তনের ইতিহাস",
                descEn = "Capture or pick a handwritten hotel khata photo: side-by-side verification of Date-Price lines (e.g., 24 - 60+40), automatic collision marking for dates with existing records, and per-price dish combination solver. Menu price updates are logged in Menu History without altering past months' ledgers.",
                descBn = "হাতে লেখা হোটেলের খাতার ছবি তুলে বা গ্যালারি থেকে নিয়ে পাশাপাশি টেক্সট যাচাই করুন এবং প্রতিটি দামের (যেমন 60+40) জন্য স্বয়ংক্রিয়ভাবে মেনু ডিশ মেলান। মেনুর দাম পরিবর্তন করলে তা ইতিহাসে জমা থাকে এবং আগের মাসের হিসাব বদলায় না।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.FormatBold,
                titleEn = "Archivo Black Bold Font & Bold-in-Bold Hierarchy",
                titleBn = "Archivo Black বোল্ড ফন্ট ও বোল্ডের ভেতর বোল্ড পার্থক্য",
                descEn = "Enabling Bold Text applies Google Font Archivo Black across all UI weights. In Sticky Notes, regular text uses heavy Archivo while *bold* spans render in Archivo Black at 1.12x scale with underline accent so bold formatting stands out clearly.",
                descBn = "বোল্ড টেক্সট চালু করলে সম্পূর্ণ অ্যাপে Archivo Black ফন্ট কার্যকর হয় এবং স্টিকি নোটেও সাধারণ লেখার চেয়ে *বোল্ড* লেখা আলাদাভাবে স্পষ্ট বোঝা যায়।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.Storage,
                titleEn = "Granular Daily Exports, Settings-Only Backup & Full Recycle Bin",
                titleBn = "দৈনন্দিন আলাদা এক্সপোর্ট, সেটিংস ব্যাকআপ ও সম্পূর্ণ রিসাইকেল বিন",
                descEn = "Export Settings separately, or export specific Hotels (Whole, Orders, Menus, Info), Accounts, or Electricity data without duplicate Full Backup cards. Recycle Bin includes a search bar and restores deleted Hotels, Menus, Accounts, Meals, Transactions, Readings, Bills, and Rate Policies.",
                descBn = "সেটিংস আলাদাভাবে এক্সপোর্ট করুন অথবা নির্দিষ্ট হোটেল, অ্যাকাউন্ট বা বিদ্যুৎ ডাটা ক্যাটাগরি অনুযায়ী এক্সপোর্ট করুন। রিসাইকেল বিনে সার্চ বার এবং ডিলিট করা বিল ও রেট পলিসিসহ সবকিছু পুনরুদ্ধারের সুবিধা রয়েছে।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.WbSunny,
                titleEn = "Expressive Sun/Moon Meal Icons & Two-Mode Transaction History",
                titleBn = "সূর্য/চাঁদ মিল আইকন ও দুই-মোড ট্রানজ্যাকশন হিস্ট্রি",
                descEn = "Meal Record Ledger uses clean Sun and Moon icons instead of 'Day •'/'Night •' text, while Transaction History offers 'Keep Each Apart' and 'Merge Daily Transactions' modes. Default Hotel and Default Account always appear first on launch.",
                descBn = "মিল লেজারে দিন ও রাতের জন্য সূর্য ও চাঁদ আইকন এবং ট্রানজ্যাকশন হিস্ট্রিতে 'Keep Each Apart' ও 'Merge Daily Transactions' মোড। ডিফল্ট হোটেল ও ডিফল্ট অ্যাকাউন্ট সবসময় প্রথমে দেখায়।"
            )
        )
    }

    val publicReferences = remember {
        listOf(
            PublicRepoReference(
                name = "Android Jetpack Compose & Material 3",
                category = "UI Framework & Adaptive Window Layouts",
                license = "Apache License 2.0",
                urlOrSource = "https://developer.android.com/jetpack/compose"
            ),
            PublicRepoReference(
                name = "AndroidX Room Persistence Library (SQLite)",
                category = "Local Encrypted-Ready Relational Database (v36 Schema)",
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
        // 1. Redesigned Modern Hero Card (Native Elevation Shadow, Interactive Version Badge)
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
                            onClick = {
                                versionDraft = appVersion
                                showEditVersionDialog = true
                            },
                            shape = RoundedCornerShape(50),
                            color = appAccent.copy(alpha = if (isLight) 0.12f else 0.2f),
                            modifier = Modifier.testTag("edit_app_version_badge")
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
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit App Version",
                                    tint = appAccent,
                                    modifier = Modifier.size(13.dp)
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
                                        "খাতা স্ক্যানার, স্টিকি ট্যাগ, বিল ক্যালকুলেটর ও এক্সপোর্ট নির্দেশিকা পড়ুন"
                                    } else {
                                        "Detailed walkthrough of Khata Scanner, Sticky Tags, Bill Math & Exports"
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

        // 5. Legal, Licenses & Developer Profile
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "আইনি ও ডেভেলপার তথ্য" else "Legal & Developer Info") {
                PixelSettingsItem(
                    icon = Icons.Default.Numbers,
                    title = if (prefLanguage == "bn") "অ্যাপ সংস্করণ (App Version)" else "App Version (Tap to Edit)",
                    subtitle = "v$appVersion",
                    onClick = {
                        versionDraft = appVersion
                        showEditVersionDialog = true
                    },
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
