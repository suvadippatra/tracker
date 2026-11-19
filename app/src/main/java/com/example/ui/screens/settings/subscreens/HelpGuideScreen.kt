package com.example.ui.screens.settings.subscreens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.components.*

private enum class HelpCategory(val id: String, val labelEn: String, val labelBn: String) {
    ALL("all", "All Guides", "সব গাইড"),
    HOTEL("hotel", "Hotel Khata", "হোটেল খাতা"),
    EXTRAS("extras", "Extras & UPI", "এক্সট্রা ও UPI"),
    ELECTRICITY("electricity", "Electricity", "বিদ্যুৎ বিল"),
    NOTES("notes", "Sticky Notes", "স্টিকি নোটস"),
    BACKUP("backup", "Backups & Sync", "ব্যাকআপ ও সিঙ্ক"),
    TIPS("tips", "Shortcuts", "শর্টকাট")
}

private data class HelpActionRow(
    val badgeEn: String,
    val badgeBn: String,
    val detailEn: String,
    val detailBn: String
)

private data class HelpModuleSection(
    val id: String,
    val category: HelpCategory,
    val icon: ImageVector,
    val accentColor: Color,
    val titleEn: String,
    val titleBn: String,
    val summaryEn: String,
    val summaryBn: String,
    val actions: List<HelpActionRow>,
    val proTipEn: String,
    val proTipBn: String
)

/**
 * Developer-crafted structured Help & Documentation screen.
 * Strictly Back-navigated (no close/criss 'X' button) with persisted scroll state,
 * interactive module filters, button/action reference tags, and EN/BN parity.
 */
@Composable
fun HelpGuideScreen(
    prefLanguage: String,
    onBack: () -> Unit
) {
    BackHandler(enabled = true) { onBack() }

    val isBn = prefLanguage == "bn"
    val isLight = isAppLightMode()
    val appAccent = AppAccentColor()
    val appVersion = BuildConfig.VERSION_NAME.ifBlank { "0.0.1" }
    var selectedCategory by remember { mutableStateOf(HelpCategory.ALL) }
    val listState = rememberPersistedLazyListState("help_guide_screen")

    val sections = remember(appAccent) {
        listOf(
            HelpModuleSection(
                id = "hotel_khata",
                category = HelpCategory.HOTEL,
                icon = Icons.Default.Restaurant,
                accentColor = Color(0xFF00897B),
                titleEn = "Hotel Khata & Daily Meals",
                titleBn = "হোটেল খাতা ও দৈনিক খাবার হিসাব",
                summaryEn = "Track daily meal orders per hotel, maintain menu rates, log advance or partial settlements, and inspect itemized monthly ledgers.",
                summaryBn = "প্রতিটি হোটেলের দৈনিক খাবারের হিসাব, মেনু দর, অগ্রিম বা আংশিক পেমেন্ট এবং মাসিক খাতা পরিচালনা করুন।",
                actions = listOf(
                    HelpActionRow(
                        badgeEn = "+ Add Meal / Order",
                        badgeBn = "+ খাবার যোগ করুন",
                        detailEn = "Tap any hotel card to log meals with quantity, menu rate, and optional receipt attachment.",
                        detailBn = "যেকোনো হোটেল কার্ডে ট্যাপ করে পরিমাণ, মেনু দর ও রসিদসহ খাবারের এন্ট্রি দিন।"
                    ),
                    HelpActionRow(
                        badgeEn = "Meal History",
                        badgeBn = "হিস্টোরি দেখুন",
                        detailEn = "Review chronological meals, filter by month, edit past entries, or settle pending dues.",
                        detailBn = "তারিখ অনুযায়ী খাবারের তালিকা দেখুন, এডিট করুন এবং বকেয়া পরিশোধ করুন।"
                    ),
                    HelpActionRow(
                        badgeEn = "Khata Text & OCR Import",
                        badgeBn = "খাতা টেক্সট ও স্ক্যান",
                        detailEn = "Import handwritten/printed khata pages or structured meal text directly into a hotel ledger.",
                        detailBn = "হাতে লেখা বা প্রিন্ট করা খাতার ছবি কিংবা টেক্সট থেকে সরাসরি এন্ট্রি ইম্পোর্ট করুন।"
                    )
                ),
                proTipEn = "Long-press a hotel or open its menu to share a clean monthly payment summary with receipts.",
                proTipBn = "হোটেলের মেনু থেকে রসিদসহ পরিষ্কার মাসিক পেমেন্ট রিপোর্ট শেয়ার করা যায়।"
            ),
            HelpModuleSection(
                id = "extras_upi",
                category = HelpCategory.EXTRAS,
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = Color(0xFF1E88E5),
                titleEn = "Extra Expenses & On-Device UPI Engine",
                titleBn = "এক্সট্রা খরচ ও অফলাইন UPI অটো-ডিটেক্ট",
                summaryEn = "Record personal expenses and income across multiple bank/cash accounts, backed by a 100% on-device UPI notification and optional bank SMS detector.",
                summaryBn = "একাধিক ব্যাংক ও ক্যাশ অ্যাকাউন্টের আয়-ব্যয় হিসাব রাখুন এবং সম্পূর্ণ অফলাইন UPI ও ব্যাংক SMS অটো-ডিটেক্ট ব্যবহার করুন।",
                actions = listOf(
                    HelpActionRow(
                        badgeEn = "UPI Notification Listener",
                        badgeBn = "UPI নোটিফিকেশন লিসেনার",
                        detailEn = "Captures debits & credits from PhonePe, Google Pay, Paytm, BharatPe, BHIM, CRED, Amazon Pay, JioFinance, MobiKwik, and Indian bank apps locally.",
                        detailBn = "PhonePe, GPay, Paytm, BharatPe, BHIM, CRED ও ব্যাংক অ্যাপের পেমেন্ট সম্পূর্ণ অফলাইনে শনাক্ত করে।"
                    ),
                    HelpActionRow(
                        badgeEn = "Bank SMS Fallback",
                        badgeBn = "ব্যাংক SMS ব্যাকআপ",
                        detailEn = "Optional Settings toggle that reads transactional bank alerts (HDFC, SBI, ICICI, Axis, Kotak, etc.) and merges matching UPI records within 2 minutes to attach exact VPA & 12-digit RRN.",
                        detailBn = "সেটিংস থেকে চালু করলে ব্যাংক SMS পড়ে ২ মিনিটের মধ্যে একই পেমেন্ট মার্জ করে VPA ও ১২-ডিজিট RRN যুক্ত করে।"
                    ),
                    HelpActionRow(
                        badgeEn = "Voice Memo & Receipt",
                        badgeBn = "ভয়েস মেমো ও স্ক্রিনশট",
                        detailEn = "Attach screenshots or record quick voice notes directly from the auto-detected payment popup.",
                        detailBn = "পেমেন্ট পপআপ থেকেই সরাসরি স্ক্রিনশট বা ভয়েস নোট সংযুক্ত করুন।"
                    )
                ),
                proTipEn = "Enable Battery Exemption in Settings > Smart Automation so Xiaomi, Vivo, Oppo, or Samsung do not pause background capture.",
                proTipBn = "ব্যাকগ্রাউন্ড ডিটেকশন সচল রাখতে সেটিংসের Battery Exemption চালু করে দিন।"
            ),
            HelpModuleSection(
                id = "electricity_billing",
                category = HelpCategory.ELECTRICITY,
                icon = Icons.Default.ElectricMeter,
                accentColor = Color(0xFFF57C00),
                titleEn = "Electricity Meter & Partial Bill Carry-Over",
                titleBn = "বিদ্যুৎ মিটার ও আংশিক বিল ক্যারি-ওভার",
                summaryEn = "Log sub-meter readings, visualize daily kWh consumption across multi-rate slabs, and carry forward remaining balances cleanly.",
                summaryBn = "সাব-মিটার রিডিং লিখুন, দৈনিক ইউনিট ব্যবহারের গ্রাফ দেখুন এবং আংশিক বিলের বকেয়া পরবর্তী মাসে স্থানান্তর করুন।",
                actions = listOf(
                    HelpActionRow(
                        badgeEn = "Quick Reading Dial",
                        badgeBn = "দ্রুত রিডিং এন্ট্রি",
                        detailEn = "Enter the latest meter reading right above the live consumption chart to auto-calculate units consumed.",
                        detailBn = "গ্রাফের ঠিক উপরেই বর্তমান রিডিং লিখে স্বয়ংক্রিয়ভাবে ব্যবহৃত ইউনিট হিসাব করুন।"
                    ),
                    HelpActionRow(
                        badgeEn = "Multi-Rate & Fixed Charges",
                        badgeBn = "মাল্টি-রেট ও ফিক্সড চার্জ",
                        detailEn = "Configure per-unit tariff rates and monthly meter rent in Account & Utility settings.",
                        detailBn = "অ্যাকাউন্ট ও ইউটিলিটি সেটিংস থেকে ইউনিট প্রতি দর ও মিটার ভাড়া নির্ধারণ করুন।"
                    ),
                    HelpActionRow(
                        badgeEn = "Partial Payment Carry-Over",
                        badgeBn = "বকেয়া ক্যারি-ওভার",
                        detailEn = "Pay part of a monthly bill and transfer the unpaid residual directly into a chosen target month.",
                        detailBn = "মাসিক বিলের আংশিক পরিশোধ করে বাকি অংশ নির্দিষ্ট পরবর্তী মাসে যুক্ত করুন।"
                    )
                ),
                proTipEn = "Tap points on the daily consumption graph to inspect exact date-wise units and rate segments.",
                proTipBn = "দৈনিক গ্রাফের যেকোনো বিন্দুতে ট্যাপ করে সেই দিনের ইউনিট ও রেট দেখুন।"
            ),
            HelpModuleSection(
                id = "sticky_notes",
                category = HelpCategory.NOTES,
                icon = Icons.Default.StickyNote2,
                accentColor = Color(0xFF8E24AA),
                titleEn = "Rich Sticky Notes & Pinned Reminders",
                titleBn = "স্টিকি নোটস ও পিন করা রিমাইন্ডার",
                summaryEn = "Capture formatted notes, checklists, and color-coded cards with instant search and pinned top priority.",
                summaryBn = "রঙিন কার্ড, চেকলিস্ট, ফরম্যাটেড টেক্সট ও পিন সুবিধাসহ দ্রুত নোট সংরক্ষণ করুন।",
                actions = listOf(
                    HelpActionRow(
                        badgeEn = "Pin to Top",
                        badgeBn = "উপরে পিন করুন",
                        detailEn = "Keep urgent shopping lists, rent notes, or account numbers pinned at the top of the board.",
                        detailBn = "জরুরি হিসাব বা তালিকা সবসময় নোটস বোর্ডের উপরে পিন করে রাখুন।"
                    ),
                    HelpActionRow(
                        badgeEn = "Rich Formatting & Search",
                        badgeBn = "ফরম্যাটিং ও সার্চ",
                        detailEn = "Use bold, highlight, and checklist spans with instant zero-lag filtering.",
                        detailBn = "বোল্ড, হাইলাইট ও চেকলিস্ট ব্যবহার করুন এবং তাৎক্ষণিক সার্চ করুন।"
                    )
                ),
                proTipEn = "Deleted notes move to the Recycle Bin so you can restore them anytime before permanent cleanup.",
                proTipBn = "মুছে ফেলা নোট রিসাইকেল বিনে জমা থাকে, যেখান থেকে সহজেই ফেরত আনা যায়।"
            ),
            HelpModuleSection(
                id = "data_backup_sync",
                category = HelpCategory.BACKUP,
                icon = Icons.Default.Storage,
                accentColor = Color(0xFF2E7D32),
                titleEn = "Reliable Backups, QR Transfer & CSV Export",
                titleBn = "নিরাপদ ব্যাকআপ, QR ট্রান্সফার ও CSV এক্সপোর্ট",
                summaryEn = "Export everything or pick individual modules via ZIP archives (with photos & voice notes), device-to-device QR streaming, clipboard JSON, or Excel-ready CSV.",
                summaryBn = "সম্পূর্ণ ডাটা বা নির্দিষ্ট মডিউল ZIP (ছবি ও অডিওসহ), QR স্ক্যান, JSON কপি-পেস্ট অথবা Excel CSV হিসেবে এক্সপোর্ট করুন।",
                actions = listOf(
                    HelpActionRow(
                        badgeEn = "ZIP Media Backup",
                        badgeBn = "ZIP মিডিয়া ব্যাকআপ",
                        detailEn = "Recommended full backup format. Bundles Room database tables alongside receipt photos and voice recordings.",
                        detailBn = "পূর্ণাঙ্গ ব্যাকআপের সেরা মাধ্যম—ডাটাবেসের সাথে রসিদের ছবি ও ভয়েস নোটও সংরক্ষিত থাকে।"
                    ),
                    HelpActionRow(
                        badgeEn = "QR Send & Scan",
                        badgeBn = "QR সেন্ড ও স্ক্যান",
                        detailEn = "Transfer selected modules offline between two phones using automatic multi-frame QR streaming and smart duplicate review.",
                        detailBn = "ইন্টারনেট ছাড়াই দুই ফোনের মধ্যে QR স্ক্যান করে ডাটা ট্রান্সফার ও ডুপ্লিকেট রিভিউ করুন।"
                    ),
                    HelpActionRow(
                        badgeEn = "Copy / Paste JSON & CSV",
                        badgeBn = "JSON কপি-পেস্ট ও CSV",
                        detailEn = "Use JSON for instant text transfer or export CSV spreadsheets for viewing in Excel and Google Sheets.",
                        detailBn = "দ্রুত টেক্সট ট্রান্সফারের জন্য JSON এবং Excel-এ দেখার জন্য CSV এক্সপোর্ট ব্যবহার করুন।"
                    )
                ),
                proTipEn = "Before applying an import, Traker previews new records, updates, and duplicate entries so your ledger stays accurate.",
                proTipBn = "ইম্পোর্ট চূড়ান্ত করার আগে নতুন এন্ট্রি, আপডেট ও ডুপ্লিকেট প্রিভিউ দেখানো হয়।"
            ),
            HelpModuleSection(
                id = "navigation_updates",
                category = HelpCategory.TIPS,
                icon = Icons.Default.CheckCircle,
                accentColor = Color(0xFF546E7A),
                titleEn = "Navigation, Privacy & In-App Updates",
                titleBn = "নেভিগেশন, গোপনীয়তা ও ইন-অ্যাপ আপডেট",
                summaryEn = "Designed for fast single-handed mobile use as well as wide tablet/desktop layouts, with zero cloud tracking and direct GitHub release updates.",
                summaryBn = "মোবাইল ও ট্যাবলেট উভয়ের জন্য অপ্টিমাইজড, কোনো ক্লাউড ট্র্যাকিং ছাড়াই সম্পূর্ণ অফলাইন ও সরাসরি GitHub আপডেট সুবিধাসহ।",
                actions = listOf(
                    HelpActionRow(
                        badgeEn = "Smart Back Navigation",
                        badgeBn = "স্মার্ট ব্যাক নেভিগেশন",
                        detailEn = "Back returns to the Hotel tab from other tabs before exiting, and remembers your exact scroll position across Settings and subpages.",
                        detailBn = "অন্য ট্যাব থেকে Back চাপলে আগে হোটেল ট্যাবে ফিরে আসে এবং সেটিংসের স্ক্রল পজিশন মনে রাখে।"
                    ),
                    HelpActionRow(
                        badgeEn = "GitHub Release Updater",
                        badgeBn = "GitHub রিলিজ আপডেটার",
                        detailEn = "Use 'Check & Download Update' on the About page to automatically fetch and install the latest compiled APK.",
                        detailBn = "About পেজের আপডেট বাটনে ট্যাপ করলেই সর্বশেষ কম্পাইলড APK ডাউনলোড ও ইনস্টল করা যাবে।"
                    ),
                    HelpActionRow(
                        badgeEn = "PIN & Biometric Lock",
                        badgeBn = "পিন ও বায়োমেট্রিক লক",
                        detailEn = "Protect your financial records locally in Settings > Security & App Lock with custom auto-lock timeouts.",
                        detailBn = "Security & App Lock থেকে পিন, ফিঙ্গারপ্রিন্ট ও অটো-লক টাইমার সেট করুন।"
                    )
                ),
                proTipEn = "All parsing, storage, OCR, and backup processing run 100% locally on your device.",
                proTipBn = "সমস্ত ডাটা, UPI পার্সিং, OCR ও ব্যাকআপ ১০০% আপনার ফোনেই নিরাপদে প্রক্রিয়াজাত হয়।"
            )
        )
    }

    val filteredSections = remember(selectedCategory, sections) {
        if (selectedCategory == HelpCategory.ALL) sections
        else sections.filter { it.category == selectedCategory }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("help_guide_lazy_column"),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 120.dp + rememberSplitScreenSafeBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Developer-crafted Hero Documentation Header
        item(key = "help_hero") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(22.dp),
                color = getCardBackground(),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = colorResource(id = com.example.R.color.ic_launcher_background)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.app_logo),
                                    contentDescription = "Traker Logo",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBn) "Traker ব্যবহার নির্দেশিকা" else "Traker User Guide",
                                color = AppTextColor(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isBn) {
                                    "প্রতিটি মডিউল, বাটন ও অফলাইন টুলের বিস্তারিত রেফারেন্স"
                                } else {
                                    "Complete reference for modules, controls & offline tools"
                                },
                                color = AppSecondaryTextColor(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HelpMetaChip(
                            icon = Icons.Default.CheckCircle,
                            label = "v$appVersion",
                            accent = appAccent,
                            isLight = isLight
                        )
                        HelpMetaChip(
                            icon = Icons.Default.CloudOff,
                            label = if (isBn) "১০০% অফলাইন" else "100% Offline",
                            accent = Color(0xFF2E7D32),
                            isLight = isLight
                        )
                        HelpMetaChip(
                            icon = Icons.Default.Security,
                            label = if (isBn) "অন-ডিভাইস" else "On-Device",
                            accent = Color(0xFF1E88E5),
                            isLight = isLight
                        )
                    }
                }
            }
        }

        // Interactive Module Filter Bar
        item(key = "help_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HelpCategory.entries.forEach { cat ->
                    val selected = selectedCategory == cat
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = if (isBn) cat.labelBn else cat.labelEn,
                                fontSize = 12.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .testTag("help_filter_${cat.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = appAccent.copy(alpha = if (isLight) 0.16f else 0.28f),
                            selectedLabelColor = appAccent
                        )
                    )
                }
            }
        }

        // Structured Feature Documentation Cards
        items(filteredSections, key = { it.id }) { section ->
            HelpModuleCard(
                section = section,
                isBn = isBn,
                isLight = isLight
            )
        }
    }
}

@Composable
private fun HelpMetaChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    isLight: Boolean
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = if (isLight) 0.11f else 0.20f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HelpModuleCard(
    section: HelpModuleSection,
    isBn: Boolean,
    isLight: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = getCardBackground(),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(section.accentColor.copy(alpha = if (isLight) 0.12f else 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = null,
                        tint = section.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) section.titleBn else section.titleEn,
                        color = AppTextColor(),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBn) section.summaryBn else section.summaryEn,
                        color = AppSecondaryTextColor(),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            HorizontalDivider(color = AppSecondaryTextColor().copy(alpha = 0.10f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                section.actions.forEach { action ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = section.accentColor.copy(alpha = if (isLight) 0.10f else 0.18f)
                        ) {
                            Text(
                                text = if (isBn) action.badgeBn else action.badgeEn,
                                color = section.accentColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = if (isBn) action.detailBn else action.detailEn,
                            color = AppTextColor().copy(alpha = 0.88f),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Pro-Tip Callout Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(section.accentColor.copy(alpha = if (isLight) 0.07f else 0.12f))
                    .border(
                        width = 1.dp,
                        color = section.accentColor.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = section.accentColor,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(16.dp)
                )
                Text(
                    text = if (isBn) section.proTipBn else section.proTipEn,
                    color = AppTextColor().copy(alpha = 0.86f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
