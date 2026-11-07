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
                titleEn = "3D Push-Pin & Paperclip Sticky Notes, Damper Scroll & Bottom Color Dots",
                titleBn = "৩ডি পুশ-পিন ও পেপারক্লিপ স্টিকি কার্ড, ড্যাম্পার স্ক্রল ও বটম কালার ডটস",
                descEn = "Sticky cards feature custom-drawn 3D glossy Blue Push-Pin and Metallic Gold/Copper Paperclip badges overlapping the card edge, smooth slow-motion damper scroll, zero load/click lag, centered heading in the opened note top bar (without pin icon), and page color dots at the bottom end line of the scrollable page.",
                descBn = "স্টিকি কার্ডে বাস্তবসম্মত ৩ডি নীল পুশ-পিন ও সোনালী পেপারক্লিপ গ্রাফিক্স, ড্যাম্পার স্লো-মোশন স্ক্রল, দ্রুত লোডিং, নোটের টপ বারে মাঝখানে শিরোনাম এবং পেজের একদম নিচে কালার ডটস যুক্ত করা হয়েছে।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.QrCodeScanner,
                titleEn = "ML Kit + Google Lens Khata OCR Auto-Repair & QR Send Pinch-to-Zoom",
                titleBn = "এমএল কিট + গুগল লেন্স খাতা OCR অটো-রিপেয়ার ও QR পিঞ্চ-টু-জুম",
                descBn = "হাতের লেখার খাতা স্ক্যানের জন্য অন-ডিভাইস Google ML Kit ও Google Lens পেস্ট বাটন যোগ করা হয়েছে, যা `6p 10 ( 26 -> 70 + 20` বা `H1of2645`-এর মতো ভাঙা লেখাও স্বয়ংক্রিয়ভাবে ঠিক করে। এছাড়া QR Send পেজে দুই আঙুলে পিঞ্চ করে পূর্ণ স্ক্রিন পর্যন্ত জুম করা যায় এবং Universal Transfer কার্ডে মোট ডেটা সাইজ দেখা যায়।",
                descEn = "Integrates on-device Google ML Kit Text Recognition plus a 'Paste / Google Lens Text' button with intelligent handwritten OCR error repair (fixing artifacts like '6p 10 ( 26' or 'H1of2645'). QR Send supports two-finger pinch-to-zoom up to full width/height, and Universal Transfer displays exact transferable data size."
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.Notifications,
                titleEn = "Legal UPI Payment Auto-Popup, Monthly Reminders & Native Voice STT",
                titleBn = "ইউপিআই পেমেন্ট অটো-পপআপ, মাসিক রিমাইন্ডার ও ভয়েস স্পিচ-টু-টেক্সট",
                descEn = "Extras page includes an opt-in UPI Payment Detector (Google Pay, PhonePe, Paytm, BHIM) that automatically opens a popup to log purpose, category, account, and screenshot (<= 2MB), a Monthly Payment Reminder scheduler, redesigned Account Cards, and native offline-capable English/Bengali Voice Dictation (STT) in Add Transaction.",
                descBn = "Extras পেজে অপ্ট-ইন UPI পেমেন্ট ডিটেক্টর (অটো-পপআপসহ), মাসিক পেমেন্ট রিমাইন্ডার, নতুন ডিজাইনের অ্যাকাউন্ট কার্ড এবং Add Transaction পেজে বাংলা ও ইংরেজি ভয়েস টাইপিং (STT) যুক্ত করা হয়েছে।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.ElectricMeter,
                titleEn = "Electricity Bill 'Paid Amount Edit' (Partial Carry-Over) & Hotel Status Export",
                titleBn = "বিদ্যুৎ বিলের 'Paid Amount Edit' (আংশিক পেমেন্ট ক্যারি-ওভার) ও হোটেল স্ট্যাটাস এক্সপোর্ট",
                descEn = "Recorded Bill History replaces 'Mark as Unpaid & Note' with 'Paid Amount Edit' so you can log partial/half payments and automatically carry forward the remaining unpaid balance to the next month's bill. Hotel Monthly Payment Status dialog now includes a direct Export/Share button.",
                descBn = "বিদ্যুৎ বিলের ইতিহাসে 'Paid Amount Edit'-এর মাধ্যমে আংশিক বা অর্ধেক পেমেন্ট এন্ট্রি করলে বাকি টাকা স্বয়ংক্রিয়ভাবে পরের মাসের বিলে যোগ হয়। হোটেলের মাসিক পেমেন্ট স্ট্যাটাস পপআপে সরাসরি এক্সপোর্ট/শেয়ার বাটন যুক্ত হয়েছে।"
            ),
            HelpGuideQuickItem(
                icon = Icons.Default.Image,
                titleEn = "2MB Auto-Compression, Payment Status Image Attachment & Split-Screen",
                titleBn = "২ এমবি অটো-কম্প্রেশন, পেমেন্ট স্ট্যাটাস ছবি সংযুক্তি ও স্প্লিট-স্ক্রিন",
                descEn = "All image attachments (Sticky, Transactions, Meal Payment Status popup, Profile Photo) are automatically compressed under 2MB JPEG. Hardware-accelerated RGBA_8888 rendering ensures zero lag and clean GPU frame buffers on Android 8 through Android 15.",
                descBn = "সব ছবির অ্যাটাচমেন্ট স্বয়ংক্রিয়ভাবে ২ এমবি-র নিচে JPEG কম্প্রেস হয় এবং অ্যান্ড্রয়েড ৮ থেকে ১৫ পর্যন্ত সব ডিভাইসে দ্রুত ও ল্যাগ-মুক্ত পারফরম্যান্স নিশ্চিত করা হয়েছে।"
            )
        )
    }

    val publicReferences = remember {
        listOf(
            PublicRepoReference(
                name = "Android Jetpack Compose & Material 3",
                category = "Adaptive UI Framework, Damper Scroll, Pinch-to-Zoom & 3D Canvas Graphics",
                license = "Apache License 2.0",
                urlOrSource = "https://developer.android.com/jetpack/compose"
            ),
            PublicRepoReference(
                name = "Google ML Kit Text Recognition (On-Device OCR v16.0.1)",
                category = "Offline Handwritten & Printed Khata Optical Character Recognition",
                license = "Android Software Development Kit License",
                urlOrSource = "https://developers.google.com/ml-kit/vision/text-recognition/v2"
            ),
            PublicRepoReference(
                name = "Android SpeechRecognizer & NotificationListenerService APIs",
                category = "Native Keyless Voice-to-Text (EN/BN) & Opt-In UPI Payment Auto-Detection",
                license = "Apache License 2.0",
                urlOrSource = "https://developer.android.com/reference/android/speech/SpeechRecognizer"
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
