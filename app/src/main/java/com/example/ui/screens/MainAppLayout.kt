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
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccentCyan)
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
fun MainAppLayout(viewModel: FinancialViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isBottomBarVisible by viewModel.isBottomBarVisible.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val meterViewModel: MeterReadingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    var showSettings by remember { mutableStateOf(false) }
    var showQRViewer by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showOwnerProfile by remember { mutableStateOf(false) }
    var showBin by remember { mutableStateOf(false) }
    var showUtilConfig by remember { mutableStateOf(false) }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent, // Let the background gradient bleed through
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        topBar = {
            if (isBottomBarVisible) {
                GlassHeader(
                    userProfile = userProfile,
                    onSettingsClicked = { showSettings = true },
                    onSearchClicked = { showSearch = true },
                    onProfileClicked = { showOwnerProfile = true },
                    onGreetingDoubleClicked = { showQRViewer = true }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
            ) {
                GlassBottomNavigation(
                    activeTab = activeTab,
                    onTabSelected = { viewModel.activeTab.value = it },
                    language = userProfile?.language ?: "en"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (isBottomBarVisible) innerPadding.calculateTopPadding() else 0.dp
                    )
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        val tabOrder = listOf("Hotels", "Extras", "Bill")
                        val initialIndex = tabOrder.indexOf(initialState)
                        val targetIndex = tabOrder.indexOf(targetState)
                        val direction = if (targetIndex > initialIndex) 1 else -1

                        (slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = 350f
                            ),
                            initialOffsetX = { fullWidth -> (fullWidth * 0.15f * direction).toInt() }
                        ) + fadeIn(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = 350f
                            )
                        ) + scaleIn(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = 350f
                            ),
                            initialScale = 0.92f
                        )) togetherWith (
                            slideOutHorizontally(
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 350f
                                ),
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.15f * direction).toInt() }
                            ) + fadeOut(
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 350f
                                )
                            ) + scaleOut(
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 350f
                                ),
                                targetScale = 0.92f
                            )
                        )
                    },
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        "Hotels" -> HotelManagementTab(viewModel)
                        "Extras" -> ExtraExpensesTab(viewModel)
                        "Bill" -> {
                            userProfile?.let { profile ->
                                BillTrackingTab(
                                    viewModel = meterViewModel,
                                    userProfile = profile,
                                    financialViewModel = viewModel,
                                    onOpenUtilityConfig = { showUtilConfig = true }
                                )
                            }
                        }
                    }
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
    
    if (showOwnerProfile) {
        OwnerProfileScreen(
            viewModel = viewModel,
            onDismiss = { showOwnerProfile = false }
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
            onDismiss = { showQRViewer = false }
        )
    }

    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false },
            onOpenBin = { showBin = true;   },
            onOpenOwnerProfile = { showOwnerProfile = true;   },
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
                            onTap = { onProfileClicked() },
                            onDoubleTap = { onGreetingDoubleClicked() }
                        )
                    }
                    .padding(4.dp)
            ) {
                Column {
                    if (userProfile.name.isNotBlank()) {
                        Text(
                            text = greeting,
                            color = AppTextColor().copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = userProfile.name,
                            color = AccentCyan,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    } else {
                        Text(
                            text = greeting,
                            color = AccentCyan,
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


// ==========================================
// BOTTOM NAVIGATION BAR
// ==========================================
@Composable
fun GlassBottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    language: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            containerAlpha = 4.5f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Hotels", "Extras", "Bill").forEach { tab ->
                    val isSelected = activeTab == tab
                    val tabColor by animateColorAsState(
                        targetValue = if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.5f),
                        animationSpec = tween(300),
                        label = "tab_color"
                    )
                    val activeBackground = if (isSelected) Color(0x2900E5FF) else Color.Transparent

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick()
                            .clip(RoundedCornerShape(18.dp))
                            .background(activeBackground)
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
                                    "Extras" -> Icons.Default.AccountBalance
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
