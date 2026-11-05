package com.example.ui.screens
import com.example.ui.components.*
import com.example.ui.components.SlidingDatePickerModal
import com.example.ui.components.InlineDatePicker
import androidx.compose.foundation.Canvas
import kotlin.random.Random
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.viewmodel.MeterReadingViewModel
import com.example.ui.viewmodel.MeterReadingUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun ImportFormatItem(title: String, columns: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppAccentColor())
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Text(columns, fontSize = 11.sp, color = AppTextColor().copy(alpha = 0.8f), fontFamily = FontFamily.Monospace)
        }
    }
}


@Composable
fun TabContentArea(
    activeTab: String,
    viewModel: FinancialViewModel,
    meterViewModel: MeterReadingViewModel,
    userProfile: com.example.data.model.UserProfileEntity?,
    onOpenUtilityConfig: () -> Unit
) {
    when (activeTab) {
        "Hotels" -> HotelManagementTab(viewModel)
        "Extras" -> ExtraExpensesTab(viewModel)
        "Bill" -> {
            userProfile?.let { profile ->
                BillTrackingTab(
                    viewModel = meterViewModel,
                    userProfile = profile,
                    financialViewModel = viewModel,
                    onOpenUtilityConfig = onOpenUtilityConfig
                )
            }
        }
        "Sticky" -> StickyNotesTab(viewModel)
    }
}

@Composable
fun GlassSideNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    language: String,
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isLight = isAppLightMode()
    var isRailExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    val railWidth by animateDpAsState(
        targetValue = if (isRailExpanded) 96.dp else 72.dp,
        animationSpec = tween(durationMillis = 200),
        label = "rail_width"
    )
    val surfaceBg = if (isLight) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth),
        color = surfaceBg,
        shadowElevation = if (isLight) 4.dp else 8.dp,
        tonalElevation = if (isLight) 1.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val appAccent = AppAccentColor()
            IconButton(
                onClick = {
                    isRailExpanded = !isRailExpanded
                    onMenuClick?.invoke()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(appAccent.copy(alpha = 0.10f))
                    .testTag("tablet_side_nav_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Navigation Menu",
                    tint = appAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            listOf("Hotels", "Extras", "Bill", "Sticky").forEach { tab ->
                val isSelected = activeTab == tab
                val tabColor = if (isSelected) appAccent else (if (isLight) Color(0xFF334155) else Color(0xFF94A3B8))
                val activeBackground = if (isSelected) appAccent.copy(alpha = 0.16f) else Color.Transparent

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick()
                        .clip(RoundedCornerShape(16.dp))
                        .background(activeBackground)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                "Hotels" -> Icons.Default.Restaurant
                                "Extras" -> Icons.Default.AccountBalance
                                "Bill" -> Icons.Default.Bolt
                                "Sticky" -> Icons.Default.StickyNote2
                                else -> Icons.Default.Settings
                            },
                            contentDescription = tab,
                            tint = tabColor,
                            modifier = Modifier.size(24.dp)
                        )
                        if (isRailExpanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (language == "bn") {
                                    when (tab) {
                                        "Hotels" -> "হোটেল"
                                        "Extras" -> "খরচ"
                                        "Bill" -> "বিল"
                                        "Sticky" -> "স্টিকি"
                                        else -> "সেটিংস"
                                    }
                                } else {
                                    when (tab) {
                                        "Bill" -> "Bill"
                                        "Extras" -> "Extra"
                                        "Hotels" -> "Hotel"
                                        "Sticky" -> "Sticky"
                                        else -> "Util"
                                    }
                                },
                                color = tabColor,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: FinancialViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isBottomBarVisible by viewModel.isBottomBarVisible.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val meterViewModel: MeterReadingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    var showSettings by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showQRViewer by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showSearch by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showOwnerProfile by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showBin by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showUtilConfig by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showSettings) {
        viewModel.registerSubScreen("Settings", showSettings)
    }
    LaunchedEffect(showQRViewer) {
        viewModel.registerSubScreen("QRViewer", showQRViewer)
    }
    LaunchedEffect(showSearch) {
        viewModel.registerSubScreen("Search", showSearch)
    }
    LaunchedEffect(showOwnerProfile) {
        viewModel.registerSubScreen("OwnerProfile", showOwnerProfile)
    }
    LaunchedEffect(showBin) {
        viewModel.registerSubScreen("Bin", showBin)
    }
    LaunchedEffect(showUtilConfig) {
        viewModel.registerSubScreen("UtilConfig", showUtilConfig)
    }

    val stickyNotes by viewModel.allStickyNotes.collectAsStateWithLifecycle()
    val selectedStickyTag by viewModel.selectedStickyTag.collectAsStateWithLifecycle()
    val allStickyTags = remember(stickyNotes) {
        stickyNotes.flatMap { it.tagList }.distinct()
    }
    LaunchedEffect(allStickyTags, selectedStickyTag) {
        if (selectedStickyTag != "ALL" && selectedStickyTag !in allStickyTags) {
            viewModel.selectedStickyTag.value = "ALL"
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600
    val useSideNavigation = isLandscape || isTablet

    if (useSideNavigation) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (isBottomBarVisible) {
                GlassSideNavigation(
                    activeTab = activeTab,
                    onTabSelected = { viewModel.activeTab.value = it },
                    language = userProfile?.language ?: "en"
                )
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (isBottomBarVisible) {
                    GlassHeader(
                        userProfile = userProfile,
                        showGreeting = activeTab != "Sticky",
                        stickyTags = allStickyTags,
                        selectedStickyTag = selectedStickyTag,
                        onStickyTagSelected = { viewModel.selectedStickyTag.value = it },
                        onSettingsClicked = { showSettings = true },
                        onSearchClicked = { showSearch = true },
                        onProfileClicked = { viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.OwnerProfile) },
                        onGreetingDoubleClicked = { showQRViewer = true }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp)
                ) {
                    TabContentArea(
                        activeTab = activeTab,
                        viewModel = viewModel,
                        meterViewModel = meterViewModel,
                        userProfile = userProfile,
                        onOpenUtilityConfig = { showUtilConfig = true }
                    )
                }
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // Let the background gradient bleed through
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
            topBar = {
                GlassHeader(
                    userProfile = userProfile,
                    showGreeting = activeTab != "Sticky",
                    stickyTags = allStickyTags,
                    selectedStickyTag = selectedStickyTag,
                    onStickyTagSelected = { viewModel.selectedStickyTag.value = it },
                    onSettingsClicked = { showSettings = true },
                    onSearchClicked = { showSearch = true },
                    onProfileClicked = { viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.OwnerProfile) },
                    onGreetingDoubleClicked = { showQRViewer = true }
                )
            },
            bottomBar = {
                if (isHeavyEffectsDisabled()) {
                    if (isBottomBarVisible) {
                        GlassBottomNavigation(
                            activeTab = activeTab,
                            onTabSelected = { viewModel.activeTab.value = it },
                            language = userProfile?.language ?: "en"
                        )
                    }
                } else {
                    AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150))
                    ) {
                        GlassBottomNavigation(
                            activeTab = activeTab,
                            onTabSelected = { viewModel.activeTab.value = it },
                            language = userProfile?.language ?: "en"
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    TabContentArea(
                        activeTab = activeTab,
                        viewModel = viewModel,
                        meterViewModel = meterViewModel,
                        userProfile = userProfile,
                        onOpenUtilityConfig = { showUtilConfig = true }
                    )
                }
            }
        }
    }

    if (showSearch) {
        GlobalSearchScreen(
            viewModel = viewModel,
            onDismiss = { showSearch = false }
        )
    }
    
    if (showBin) {
        BinScreen(
            viewModel = viewModel,
            onDismiss = { showBin = false }
        )
    }
    
    if (showQRViewer) {
        OwnerQRViewerDialog(
            viewModel = viewModel,
            onDismiss = { showQRViewer = false },
            onOpenOwnerProfile = {
                showQRViewer = false
                viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.OwnerProfile)
            }
        )
    }

    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false },
            onOpenBin = { showBin = true;   },
            onOpenOwnerProfile = { viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.OwnerProfile);   },
            onOpenUtilityConfig = { showUtilConfig = true }
        )
    }

    if (showUtilConfig) {
        UtilityConfigurationScreen(
            viewModel = meterViewModel,
            financialViewModel = viewModel,
            onDismiss = { showUtilConfig = false }
        )
    }
}


// ==========================================
// TOP HEADER COMPONENT
// ==========================================
@Composable
fun GlassHeader(
    userProfile: UserProfileEntity,
    showGreeting: Boolean = true,
    stickyTags: List<String> = emptyList(),
    selectedStickyTag: String = "ALL",
    onStickyTagSelected: (String) -> Unit = {},
    onSettingsClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onProfileClicked: () -> Unit,
    onGreetingDoubleClicked: () -> Unit
) {
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> if (userProfile.language == "bn") "শুভ সকাল" else "Good morning"
            in 12..16 -> if (userProfile.language == "bn") "শুভ দুপুর" else "Good afternoon"
            in 17..20 -> if (userProfile.language == "bn") "শুভ সন্ধ্যা" else "Good evening"
            else -> if (userProfile.language == "bn") "শুভ রাত্রি" else "Good evening"
        }
    }
    val isGlass = isLiquidGlassEnabled()
    val isLight = isAppLightMode()
    val isBn = userProfile.language == "bn"
    val appAccent = AppAccentColor()

    @Composable
    fun StickyTagFilterRow(modifier: Modifier = Modifier) {
        val tagOptions = remember(stickyTags, isBn) {
            listOf("ALL" to (if (isBn) "সব (All)" else "All")) + stickyTags.map { it to it }
        }
        Row(
            modifier = modifier
                .horizontalScroll(rememberScrollState())
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tagOptions.forEach { (tagKey, label) ->
                val isSelected = selectedStickyTag.equals(tagKey, ignoreCase = true)
                val chipBg = if (isSelected) appAccent else getGlassIconButtonBg()
                val chipFg = if (isSelected) AppOnAccentColor(appAccent) else AppTextColor().copy(alpha = 0.85f)
                Surface(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .clickable { onStickyTagSelected(tagKey) },
                    shape = RoundedCornerShape(17.dp),
                    color = chipBg,
                    border = if (isSelected) null else BorderStroke(1.dp, getGlassBorder())
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tagKey == "ALL") label else "#$label",
                            color = chipFg,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    if (!isGlass) {
        // Material 3 Solid Docked App Bar
        val surfaceBg = if (isLight) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceBg)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = if (showGreeting) 10.dp else 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showGreeting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { onGreetingDoubleClicked() }
                                )
                            }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Column {
                            if (userProfile.name.isNotBlank()) {
                                Text(
                                    text = greeting,
                                    color = AppSecondaryTextColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.3.sp
                                )
                                Text(
                                    text = userProfile.name,
                                    color = appAccent,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                            } else {
                                Text(
                                    text = greeting,
                                    color = appAccent,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                            }
                        }
                    }
                } else {
                    StickyTagFilterRow(modifier = Modifier.weight(1f))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSearchClicked,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AppTextColor(),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = onSettingsClicked,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AppTextColor(),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Liquid Glass Floating Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = if (showGreeting) 12.dp else 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showGreeting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { onGreetingDoubleClicked() }
                                )
                            }
                            .padding(4.dp)
                    ) {
                        Column {
                            if (userProfile.name.isNotBlank()) {
                                Text(
                                    text = greeting,
                                    color = AppTextColor(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = userProfile.name,
                                    color = AppAccentColor(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                            } else {
                                Text(
                                    text = greeting,
                                    color = AppAccentColor(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                        }
                    }
                } else {
                    StickyTagFilterRow(modifier = Modifier.weight(1f))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSearchClicked,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AppTextColor(),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onSettingsClicked,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AppTextColor(),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}


// ==========================================
// BOTTOM NAVIGATION BAR (WhatsApp-style Icon above Text)
// ==========================================
@Composable
fun GlassBottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    language: String
) {
    val isLight = isAppLightMode()
    val surfaceBg = if (isLight) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceBg,
        shadowElevation = if (isLight) 8.dp else 12.dp,
        tonalElevation = if (isLight) 2.dp else 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val appAccent = AppAccentColor()
            listOf("Hotels", "Extras", "Bill", "Sticky").forEach { tab ->
                val isSelected = activeTab == tab
                val tabColor = if (isSelected) appAccent else (if (isLight) Color(0xFF475569) else Color(0xFF94A3B8))
                val activeBackground = if (isSelected) appAccent.copy(alpha = 0.15f) else Color.Transparent

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(activeBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                "Hotels" -> Icons.Default.Restaurant
                                "Extras" -> Icons.Default.AccountBalanceWallet
                                "Bill" -> Icons.Default.Bolt
                                "Sticky" -> Icons.Default.StickyNote2
                                else -> Icons.Default.Settings
                            },
                            contentDescription = tab,
                            tint = tabColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (language == "bn") {
                            when (tab) {
                                "Hotels" -> "হোটেল"
                                "Extras" -> "খরচ"
                                "Bill" -> "বিল"
                                "Sticky" -> "স্টিকি"
                                else -> "সেটিংস"
                            }
                        } else {
                            when (tab) {
                                "Bill" -> "Bill"
                                "Extras" -> "Extra"
                                "Hotels" -> "Hotel"
                                "Sticky" -> "Sticky"
                                else -> "Util"
                            }
                        },
                        color = tabColor,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
