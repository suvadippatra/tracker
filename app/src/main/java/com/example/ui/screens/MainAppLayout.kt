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
    }
}

@Composable
fun GlassSideNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    language: String,
    modifier: Modifier = Modifier
) {
    val isGlass = isLiquidGlassEnabled()
    val isLight = isAppLightMode()

    if (!isGlass) {
        val surfaceBg = if (isLight) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark
        val outlineColor = if (isLight) com.example.ui.theme.M3OutlineLight else com.example.ui.theme.M3OutlineDark

        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(96.dp)
                .background(surfaceBg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 16.dp, horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val appAccent = AppAccentColor()
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = appAccent,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(bottom = 12.dp)
                )

                listOf("Hotels", "Extras", "Bill").forEach { tab ->
                    val isSelected = activeTab == tab
                    val tabColor = if (isSelected) appAccent else (if (isLight) Color(0xFF334155) else Color(0xFF94A3B8))
                    val activeBackground = if (isSelected) appAccent.copy(alpha = 0.16f) else Color.Transparent

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                            .clip(RoundedCornerShape(16.dp))
                            .background(activeBackground)
                            .then(
                                if (isSelected) Modifier.border(1.dp, appAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                else Modifier
                            )
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
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = tab,
                                tint = tabColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (language == "bn") {
                                    when (tab) {
                                        "Hotels" -> "হোটেল"
                                        "Extras" -> "খরচ"
                                        "Bill" -> "বিল"
                                        else -> "সেটিংস"
                                    }
                                } else {
                                    when (tab) {
                                        "Bill" -> "Bill"
                                        "Extras" -> "Extra"
                                        "Hotels" -> "Hotel"
                                        else -> "Util"
                                    }
                                },
                                color = tabColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(110.dp),
                cornerRadius = 24.dp,
                containerAlpha = 4.5f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 16.dp, horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val appAccent = AppAccentColor()
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = appAccent,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(bottom = 12.dp)
                    )

                    listOf("Hotels", "Extras", "Bill").forEach { tab ->
                        val isSelected = activeTab == tab
                        val tabColor = if (isSelected) appAccent else (if (isAppLightMode()) Color(0xFF000000) else Color(0xFFFFFFFF))
                        val activeBackground = if (isSelected) appAccent.copy(alpha = 0.25f) else Color.Transparent

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick()
                                .clip(RoundedCornerShape(18.dp))
                                .background(activeBackground)
                                .then(if (isSelected) Modifier.border(1.dp, appAccent.copy(alpha = 0.5f), RoundedCornerShape(18.dp)) else Modifier)
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
                                        else -> Icons.Default.Settings
                                    },
                                    contentDescription = tab,
                                    tint = tabColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (language == "bn") {
                                        when (tab) {
                                            "Hotels" -> "হোটেল"
                                            "Extras" -> "খরচ"
                                            "Bill" -> "বিল"
                                            else -> "সেটিংস"
                                        }
                                    } else {
                                        when (tab) {
                                            "Bill" -> "Bill"
                                            "Extras" -> "Extra"
                                            "Hotels" -> "Hotel"
                                            else -> "Util"
                                        }
                                    },
                                    color = tabColor,
                                    fontSize = 11.sp,
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

    if (!isGlass) {
        // Material 3 Solid Docked App Bar
        val surfaceBg = if (isLight) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark
        val outlineColor = if (isLight) com.example.ui.theme.M3OutlineLight else com.example.ui.theme.M3OutlineDark
        val iconBg = if (isLight) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceBg)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                        val appAccent = AppAccentColor()
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSearchClicked,
                        modifier = Modifier
                            .bounceClick()
                            .size(40.dp)
                            .background(iconBg, CircleShape)
                            .border(1.dp, outlineColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AppTextColor(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSettingsClicked,
                        modifier = Modifier
                            .bounceClick()
                            .size(40.dp)
                            .background(iconBg, CircleShape)
                            .border(1.dp, outlineColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AppTextColor(),
                            modifier = Modifier.size(20.dp)
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSearchClicked,
                        modifier = Modifier
                            .bounceClick()
                            .size(44.dp)
                            .background(getGlassIconButtonBg(), CircleShape)
                            .border(0.5.dp, getGlassBorder(), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AppTextColor(),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSettingsClicked,
                        modifier = Modifier
                            .bounceClick()
                            .size(44.dp)
                            .background(getGlassIconButtonBg(), CircleShape)
                            .border(0.5.dp, getGlassBorder(), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AppTextColor(),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}


// ==========================================
// BOTTOM NAVIGATION BAR
// ==========================================
@Composable
fun GlassBottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    language: String
) {
    val isGlass = isLiquidGlassEnabled()
    val isLight = isAppLightMode()

    if (!isGlass) {
        // Material 3 Solid Docked Bottom Navigation Bar
        val surfaceBg = if (isLight) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark
        val outlineColor = if (isLight) com.example.ui.theme.M3OutlineLight else com.example.ui.theme.M3OutlineDark

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceBg)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val appAccent = AppAccentColor()
                listOf("Hotels", "Extras", "Bill").forEach { tab ->
                    val isSelected = activeTab == tab
                    val tabColor = if (isSelected) appAccent else (if (isLight) Color(0xFF475569) else Color(0xFF94A3B8))
                    val activeBackground = if (isSelected) appAccent.copy(alpha = 0.16f) else Color.Transparent

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick()
                            .clip(RoundedCornerShape(16.dp))
                            .background(activeBackground)
                            .then(
                                if (isSelected) Modifier.border(1.dp, appAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                else Modifier
                            )
                            .clickable { onTabSelected(tab) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (tab) {
                                    "Hotels" -> Icons.Default.Restaurant
                                    "Extras" -> Icons.Default.AccountBalanceWallet
                                    "Bill" -> Icons.Default.Bolt
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = tab,
                                tint = tabColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") {
                                    when (tab) {
                                        "Hotels" -> "হোটেল"
                                        "Extras" -> "খরচ"
                                        "Bill" -> "বিল"
                                        else -> "সেটিংস"
                                    }
                                } else {
                                    when (tab) {
                                        "Bill" -> "Bill"
                                        "Extras" -> "Extra"
                                        "Hotels" -> "Hotel"
                                        else -> "Util"
                                    }
                                },
                                color = tabColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Liquid Glass Floating Pill Navigation Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            val appAccent = AppAccentColor()
            val navBg = if (isLight) {
                listOf(Color(0xF5FFFFFF), Color(0xEBFFFFFF))
            } else {
                listOf(Color(0xFA1E2638), Color(0xF5111827))
            }
            val navBorder = if (isLight) {
                listOf(Color(0xFFFFFFFF), Color(0x33000000))
            } else {
                listOf(appAccent.copy(alpha = 0.5f), Color(0x33FFFFFF))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(navBg),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(navBorder),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Hotels", "Extras", "Bill").forEach { tab ->
                        val isSelected = activeTab == tab
                        val tabColor = if (isSelected) appAccent else (if (isLight) Color(0xFF000000) else Color(0xFFFFFFFF))
                        val activeBackground = if (isSelected) appAccent.copy(alpha = 0.22f) else Color.Transparent

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .bounceClick()
                                .clip(RoundedCornerShape(18.dp))
                                .background(activeBackground)
                                .then(if (isSelected) Modifier.border(1.dp, appAccent.copy(alpha = 0.5f), RoundedCornerShape(18.dp)) else Modifier)
                            .clickable { onTabSelected(tab) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (tab) {
                                    "Hotels" -> Icons.Default.Restaurant
                                    "Extras" -> Icons.Default.AccountBalanceWallet
                                    "Bill" -> Icons.Default.Bolt
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = tab,
                                tint = tabColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") {
                                    when (tab) {
                                        "Hotels" -> "হোটেল"
                                        "Extras" -> "খরচ"
                                        "Bill" -> "বিল"
                                        else -> "সেটিংস"
                                    }
                                } else {
                                    when (tab) {
                                        "Bill" -> "Bill"
                                        "Extras" -> "Extra"
                                        "Hotels" -> "Hotel"
                                        else -> "Util"
                                    }
                                },
                                color = tabColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
}
