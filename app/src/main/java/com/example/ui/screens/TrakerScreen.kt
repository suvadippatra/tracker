package com.example.ui.screens

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
import androidx.compose.ui.text.input.KeyboardType
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

// Glassmorphism design tokens
val GlassBackground = Color(0x26FFFFFF) // 15% opacity white
val GlassBorderTop = Color(0x66FFFFFF) // 40% opacity white for upper highlights
val GlassBorderBottom = Color(0x13FFFFFF) // 7% opacity white
val GlassShadowColor = Color(0x1F000000)

// Vibrant Backdrop Gradients
val DarkBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF070414), // Deep indigo midnight
        Color(0xFF0F0B26), // Midnight sky
        Color(0xFF1B0E3D), // Soft purple dusk
        Color(0xFF2C0F4A)  // Liquid violet
    )
)

val LightBackgroundGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF81D4FA), // Deep Sky Blue
        Color(0xFFCE93D8), // Vibrant Purple
        Color(0xFFFFCC80)  // Saturated Orange
    )
)

@Composable
fun getAppBackgroundGradient(themeMode: String): Brush {
    val isLight = if (themeMode == "Auto") !androidx.compose.foundation.isSystemInDarkTheme() else themeMode == "Light"
    return if (isLight) LightBackgroundGradient else DarkBackgroundGradient
}

val LocalAppTheme = compositionLocalOf { "Auto" }

@Composable
fun getDialogBackground(): Color {
    return if (isAppLightMode()) Color(0xFFFFFFFF) else Color(0xFF140D2D)
}

@Composable
fun getGlassBorder(): Color {
    return if (isAppLightMode()) Color(0x40000000) else Color(0x33FFFFFF)
}

@Composable
fun getGlassIconButtonBg(): Color {
    return if (isAppLightMode()) Color(0x1A000000) else Color(0x33FFFFFF)
}

@Composable
fun isAppLightMode(): Boolean {
    val themeMode = LocalAppTheme.current
    return if (themeMode == "Auto") !androidx.compose.foundation.isSystemInDarkTheme() else themeMode == "Light"
}

@Composable
fun AppTextColor(): Color {
    return if (isAppLightMode()) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
}

object Localizer {
    private val strings = mapOf(
        "en" to mapOf(
            "init_app" to "Initializing Pocket...",
            "configurations" to "Configurations",
            "profile" to "Profile",
            "hotels" to "Hotels",
            "accounts" to "Accounts",
            "preferences" to "Preferences",
            "edit_profile" to "Edit Profile Data",
            "display_name" to "Display Name",
            "contact_no" to "Contact Number",
            "apply_profile" to "Apply Profile Updates",
            "app_language" to "App Language",
            "app_theme" to "App Theme",
            "default_hotel" to "Default Hotel",
            "show_adv" to "Show Advanced Payment Card",
            "app_lock" to "App Lock",
            "save_pref" to "Save Preferences",
            "reg_hotel" to "Register Establishment",
            "hotel_name" to "Hotel Name",
            "adv_bal" to "Advanced Balance (₹)",
            "save_hotel" to "Save Hotel",
            "reg_hotels" to "Registered Hotels",
            "reg_acc" to "Register Virtual Account",
            "acc_name" to "Account Name (e.g. GPay, UPI)",
            "start_bal" to "Starting Balance (₹)",
            "save_acc" to "Save Account",
            "reg_accounts" to "Registered Accounts",
            "spending_month" to "Your spending this month",
            "adv_payment" to "Advanced Payment",
            "view_meals" to "View Meal History",
            "add_meal" to "Add Meal",
            "add_expense" to "Add Expense",
            "liquid_bal" to "Liquid Balance",
            "recent_tx" to "Recent Transactions",
            "view_all" to "View All History",
            "extra_expenses" to "Extras"
        ),
        "bn" to mapOf(
            "init_app" to "পকেট চালু হচ্ছে...",
            "configurations" to "কনফিগারেশন",
            "profile" to "প্রোফাইল",
            "hotels" to "হোটেল",
            "accounts" to "অ্যাকাউন্ট",
            "preferences" to "পছন্দসমূহ",
            "edit_profile" to "প্রোফাইল পরিবর্তন করুন",
            "display_name" to "নাম",
            "contact_no" to "যোগাযোগ নম্বর",
            "apply_profile" to "প্রোফাইল আপডেট করুন",
            "app_language" to "অ্যাপের ভাষা",
            "app_theme" to "অ্যাপ থিম",
            "default_hotel" to "ডিফল্ট হোটেল",
            "show_adv" to "অগ্রিম পেমেন্ট কার্ড দেখান",
            "app_lock" to "অ্যাপ লক",
            "save_pref" to "পছন্দ সংরক্ষণ করুন",
            "reg_hotel" to "নতুন হোটেল যুক্ত করুন",
            "hotel_name" to "হোটেলের নাম",
            "adv_bal" to "অগ্রিম ব্যালেন্স (₹)",
            "save_hotel" to "হোটেল সংরক্ষণ করুন",
            "reg_hotels" to "নিবন্ধিত হোটেলসমূহ",
            "reg_acc" to "ভার্চুয়াল অ্যাকাউন্ট যুক্ত করুন",
            "acc_name" to "অ্যাকাউন্টের নাম (যেমন- GPay, UPI)",
            "start_bal" to "শুরুর ব্যালেন্স (₹)",
            "save_acc" to "অ্যাকাউন্ট সংরক্ষণ করুন",
            "reg_accounts" to "নিবন্ধিত অ্যাকাউন্টসমূহ",
            "spending_month" to "এই মাসে আপনার খরচ",
            "adv_payment" to "অগ্রিম পেমেন্ট",
            "view_meals" to "খাবারের ইতিহাস দেখুন",
            "add_meal" to "খাবার যোগ করুন",
            "add_expense" to "খরচ যোগ করুন",
            "liquid_bal" to "টোটাল ব্যালেন্স",
            "recent_tx" to "সাম্প্রতিক লেনদেনসমূহ",
            "view_all" to "সব ইতিহাস দেখুন",
            "extra_expenses" to "অন্যান্য খরচ"
        )
    )

    fun getString(key: String, lang: String): String {
        return strings[lang]?.get(key) ?: strings["en"]?.get(key) ?: key
    }
}

@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeText: String = "ON",
    inactiveText: String = "OFF"
) {
    val isLight = isAppLightMode()
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (isLight) Color(0x1A000000) else Color(0x1F000000))
            .border(1.dp, if (isLight) Color(0xFFFFFFFF) else getGlassBorder(), RoundedCornerShape(17.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        val thumbOffset by animateDpAsState(
            targetValue = if (checked) 50.dp else 2.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "switch_thumb"
        )

        Box(
            modifier = Modifier
                .padding(vertical = 2.dp)
                .offset(x = thumbOffset)
                .width(48.dp)
                .fillMaxHeight()
                .background(
                    brush = if (checked) {
                        Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF00E676)))
                    } else {
                        if (isLight) Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xE6FFFFFF)))
                        else Brush.linearGradient(listOf(Color(0x33FFFFFF), getGlassIconButtonBg()))
                    },
                    shape = RoundedCornerShape(15.dp)
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = inactiveText,
                color = if (!checked) AppTextColor() else AppTextColor().copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = activeText,
                color = if (checked) AppTextColor() else AppTextColor().copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AppLockScreen(
    correctPin: String,
    language: String,
    userName: String,
    onUnlock: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Lock Icon with glowing background
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    if (isError) Color(0x33FF4081) else Color(0x1F00E5FF),
                    CircleShape
                )
                .border(1.dp, if (isError) AccentPink else AccentCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = if (isError) AccentPink else AccentCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (language == "bn") "${userName.ifBlank { "ব্যবহারকারী" }} এর পকেট লক করা আছে" else "${userName.ifBlank { "User" }}'s Pocket is Locked",
            color = AppTextColor(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isError) {
                if (language == "bn") "ভুল পিন, আবার চেষ্টা করুন" else "Incorrect PIN, please try again"
            } else {
                if (language == "bn") "চালিয়ে যেতে ৪-ডিজিট পিন দিন" else "Enter 4-digit PIN to continue"
            },
            color = if (isError) AccentPink else AppTextColor().copy(alpha = 0.6f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Passcode Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val active = i < enteredPin.length
                val color = if (isError) AccentPink else if (active) AccentCyan else AppTextColor().copy(alpha = 0.2f)
                val size by animateDpAsState(if (active) 16.dp else 12.dp)

                Box(
                    modifier = Modifier
                        .size(size)
                        .background(color, CircleShape)
                        .border(0.5.dp, AppTextColor().copy(alpha = 0.4f), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Keypad Grid
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    row.forEach { digit ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(getGlassIconButtonBg())
                                .border(0.5.dp, getGlassBorder(), CircleShape)
                                .clickable { isError = false
                                    when (digit) {
                                        "C" -> enteredPin = ""
                                        "⌫" -> {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        }
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += digit
                                                if (enteredPin.length == 4) {
                                                    if (enteredPin == correctPin) {
                                                        onUnlock()
                                                    } else {
                                                        isError = true
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit,
                                color = AppTextColor(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

val AccentGold = Color(0xFFD84315)
val AccentCyan = Color(0xFF0091EA)
val AccentPink = Color(0xFFC2185B)
val AccentGreen = Color(0xFF00B0FF)



@Composable
fun MovingStarsBackground(isLight: Boolean) {
    val starCount = if (isLight) 25 else 45
    val starColor = if (isLight) Color.White.copy(alpha = 1.0f) else Color.White.copy(alpha = 0.4f)
    val shadowColor = Color.Black.copy(alpha = 0.15f)
    
    // Instead of instantiating 50 animations which might be heavy, we'll animate a global time value
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val globalTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val stars = remember(isLight) {
        List(starCount) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 3f + 1f,
                speed = Random.nextFloat() * 0.5f + 0.1f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (star in stars) {
            // Calculate new Y position
            val currentY = (star.y * h - globalTime * star.speed * 50f) % h
            val y = if (currentY < 0) currentY + h else currentY
            val x = star.x * w

            // Add a subtle twinkle effect
            val twinkle = kotlin.math.sin(globalTime * 0.1f * star.speed + star.x * 10f).toFloat() * 0.5f + 0.5f
            
            if (isLight) {
                drawCircle(
                    color = shadowColor.copy(alpha = shadowColor.alpha * twinkle),
                    radius = star.size + 1f,
                    center = Offset(x, y + 1f)
                )
            }
            drawCircle(
                color = starColor.copy(alpha = starColor.alpha * twinkle),
                radius = star.size,
                center = Offset(x, y)
            )
        }
    }
}

data class Star(val x: Float, val y: Float, val size: Float, val speed: Float)

@Composable
fun TrakerApp(viewModel: FinancialViewModel) {
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    var isUnlocked by remember { mutableStateOf(false) }

    val isLight = if (userProfile.themeMode == "Auto") !androidx.compose.foundation.isSystemInDarkTheme() else userProfile.themeMode == "Light"
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is android.app.Activity) break
                context = context.baseContext
            }
            if (context is android.app.Activity) {
                val window = context.window
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
            }
        }
    }

    // Synchronize locked state on setup
    LaunchedEffect(isInitialized, userProfile.appLockEnabled) {
        if (isInitialized && !userProfile.appLockEnabled) {
            isUnlocked = true
        }
    }

    CompositionLocalProvider(LocalAppTheme provides userProfile.themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(userProfile.themeMode))
        ) {
            if (userProfile.enableStarAnimation) { MovingStarsBackground(isLight = isAppLightMode()) }
            if (!isInitialized) {
            // Elegant Glassmorphic Loader
            Column(
                modifier = Modifier.fillMaxSize().verticalCenterTransition(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = AccentCyan,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (userProfile.language == "bn") "${userProfile.name.ifBlank { "ব্যবহারকারী" }} এর পকেট চালু হচ্ছে..." else "Initializing ${userProfile.name.ifBlank { "User" }}'s Pocket...",
                    color = AppTextColor().copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            if (userProfile.appLockEnabled && !isUnlocked) {
                AppLockScreen(
                    correctPin = userProfile.appLockPin,
                    language = userProfile.language,
                    userName = userProfile.name,
                    onUnlock = { isUnlocked = true }
                )
            } else {
                MainAppLayout(viewModel)
            }
        }
    }
}
}

@Composable
fun MainAppLayout(viewModel: FinancialViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val meterViewModel: MeterReadingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    var showSettings by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent, // Let the background gradient bleed through
        topBar = {
            GlassHeader(
                userProfile = userProfile,
                onSettingsClicked = { showSettings = true },
                onSearchClicked = { showSearch = true }
            )
        },
        bottomBar = {
            GlassBottomNavigation(
                activeTab = activeTab,
                onTabSelected = { viewModel.activeTab.value = it },
                language = userProfile?.language ?: "en"
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val duration = 220
                    fadeIn(animationSpec = tween(duration)) + scaleIn(initialScale = 0.94f, animationSpec = tween(duration)) togetherWith 
                            fadeOut(animationSpec = tween(duration)) + scaleOut(targetScale = 0.94f, animationSpec = tween(duration))
                },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    "Hotels" -> HotelManagementTab(viewModel)
                    "Extras" -> ExtraExpensesTab(viewModel)
                    "Bill" -> {
                        userProfile?.let { profile ->
                            BillTrackingTab(meterViewModel, profile)
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
        }
    }

    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
    }
}

// ==========================================
// CUSTOM GLASSMORPHIC COMPONENT
// ==========================================
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val isLight = isAppLightMode()
    val bgColors = if (isLight) {
        listOf(Color(0xB3FFFFFF), Color(0x80FFFFFF)) // More opaque for solid liquid glass feel in light mode
    } else {
        listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))
    }
    val borderColors = if (isLight) {
        listOf(Color(0xFFFFFFFF), Color(0x4D000000)) // White highlight top, subtle shadow bottom
    } else {
        listOf(GlassBorderTop, GlassBorderBottom)
    }

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(colors = bgColors),
                shape = RoundedCornerShape(cornerRadius)
            )
            .then(
                if (border != null) Modifier.border(border, RoundedCornerShape(cornerRadius))
                else Modifier.border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(colors = borderColors),
                    shape = RoundedCornerShape(cornerRadius)
                )
            )
            .drawBehind {
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset.Zero,
                    size = size
                )
            }
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    borderBrush: Brush? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .bounceClick()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isAppLightMode()) listOf(Color(0xE6FFFFFF), Color(0xB3FFFFFF))
                             else listOf(Color(0x3DFFFFFF), Color(0x18FFFFFF))
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = 1.dp,
                brush = borderBrush ?: Brush.verticalGradient(
                    colors = if (isAppLightMode()) listOf(Color(0xFFFFFFFF), Color(0x26000000))
                             else listOf(GlassBorderTop, GlassBorderBottom)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// Bounce Tactile Feedback Modifier
fun Modifier.bounceClick(): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce"
    )
    val view = androidx.compose.ui.platform.LocalView.current
    this
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val anyPressed = event.changes.any { it.pressed }
                    if (anyPressed != isPressed) {
                        isPressed = anyPressed
                        if (isPressed) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    }
                }
            }
        }
}

// Smooth number roll
@Composable
fun RollingNumberText(
    value: Double,
    prefix: String = "₹",
    style: TextStyle = TextStyle(
        color = AppTextColor(),
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
    )
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "number_roll"
    )
    Text(
        text = "$prefix${String.format(Locale.US, "%,.2f", animatedValue)}",
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// Vertical centered transition helper
fun Modifier.verticalCenterTransition(): Modifier = this.wrapContentSize(Alignment.Center)

// ==========================================
// TOP HEADER COMPONENT
// ==========================================
@Composable
fun GlassHeader(
    userProfile: UserProfileEntity,
    onSettingsClicked: () -> Unit,
    onSearchClicked: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            cornerRadius = 24.dp
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
                                    else -> Icons.Default.Bolt
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
                                        "Extras" -> "অন্যান্য খরচ"
                                        else -> "বিদ্যুৎ বিল"
                                    }
                                } else {
                                    if (tab == "Bill") "Electricity" else tab
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

// ==========================================
// MODULE A: HOTEL MANAGEMENT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelManagementTab(viewModel: FinancialViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val pdfExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val hotel = viewModel.activeHotel.value
            if (hotel != null) {
                try {
                    val txs = viewModel.activeHotelTransactions.value
                    val cal = java.util.Calendar.getInstance()
                    val currentMonth = cal.get(java.util.Calendar.MONTH)
                    val currentYear = cal.get(java.util.Calendar.YEAR)
                    
                    val monthTxs = txs.filter { 
                        val tCal = java.util.Calendar.getInstance()
                        tCal.timeInMillis = it.timestamp
                        tCal.get(java.util.Calendar.MONTH) == currentMonth && tCal.get(java.util.Calendar.YEAR) == currentYear
                    }
                    
                    val pdfDocument = android.graphics.pdf.PdfDocument()
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    val paint = android.graphics.Paint().apply { textSize = 16f; color = android.graphics.Color.BLACK }
                    val boldPaint = android.graphics.Paint().apply { textSize = 20f; color = android.graphics.Color.BLACK; isFakeBoldText = true }
                    
                    canvas.drawText("Transactions - ${hotel.name}", 20f, 40f, boldPaint)
                    var y = 80f
                    
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                    for (t in monthTxs) {
                        val dateStr = sdf.format(java.util.Date(t.timestamp))
                        val typeStr = if (t.isNight) "Night" else "Day"
                        val amt = "Rs.${t.totalAmount}"
                        canvas.drawText("$dateStr | $typeStr | $amt", 20f, y, paint)
                        y += 25f
                        if (y > 800f) break
                    }
                    pdfDocument.finishPage(page)
                    
                    context.contentResolver.openOutputStream(uri)?.use { fos ->
                        pdfDocument.writeTo(fos)
                    }
                    pdfDocument.close()
                    android.widget.Toast.makeText(context, "PDF Exported Successfully", android.widget.Toast.LENGTH_SHORT).show()
                } catch(e: Exception) {
                    android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val hotels by viewModel.allHotels.collectAsStateWithLifecycle()
    val activeHotelId by viewModel.activeHotelId.collectAsStateWithLifecycle()
    val activeHotel by viewModel.activeHotel.collectAsStateWithLifecycle()
    val monthlyTotal by viewModel.activeMonthTotal.collectAsStateWithLifecycle()
    val popularMeals by viewModel.popularMeals.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var openDropdownHotelId by remember { mutableStateOf<Int?>(null) }
    var hotelMenuOffsets by remember { mutableStateOf(mapOf<Int, androidx.compose.ui.geometry.Offset>()) }
    var showMealHistory by remember { mutableStateOf(false) }
    var showAddMealFlow by remember { mutableStateOf(false) }

    // Dialog edit fields
    var hotelToEdit by remember { mutableStateOf<com.example.data.model.HotelEntity?>(null) }
    var showInfoDialog by remember { mutableStateOf<com.example.data.model.HotelEntity?>(null) }
    var editHotelName by remember { mutableStateOf("") }
    var editHotelAdvanced by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                // PRIMARY GLASS LEDGER CARD
                if (hotels.isNotEmpty()) {
                    val initialPage = hotels.indexOfFirst { it.id == activeHotelId }.coerceAtLeast(0)
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                        initialPage = initialPage,
                        pageCount = { hotels.size }
                    )
                    
                    val view = androidx.compose.ui.platform.LocalView.current
                    LaunchedEffect(pagerState, hotels) {
                        androidx.compose.runtime.snapshotFlow { pagerState.currentPage }.collect { page ->
                            if (page in hotels.indices) {
                                if (viewModel.activeHotelId.value != hotels[page].id) {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                    viewModel.activeHotelId.value = hotels[page].id
                                }
                            }
                        }
                    }
                      
                    LaunchedEffect(activeHotelId, hotels) {
                        val index = hotels.indexOfFirst { it.id == activeHotelId }
                        if (index >= 0 && index != pagerState.currentPage) {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                    
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        pageSpacing = 16.dp,
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) { page ->
                        val hotel = hotels[page]
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val scale = 1f - (0.05f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                        val alpha = 1f - (0.3f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                        Box(modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }) {
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = hotel.name,
                                            color = AppTextColor(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Call",
                                                tint = AppTextColor(),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .padding(end = 4.dp)
                                                    .clickable { if (hotel.phoneNumber.isNotBlank()) {
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${hotel.phoneNumber}"))
                                                            context.startActivity(intent)
                                                        } else {
                                                            android.widget.Toast.makeText(context, "No phone number added", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "Info",
                                                tint = AppTextColor(),
                                                modifier = Modifier.size(24.dp).padding(end = 4.dp).clickable { showInfoDialog = hotel }
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "More",
                                                    tint = AppTextColor(),
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clickable { openDropdownHotelId = hotel.id }
                                                )
                                                DropdownMenu(
                                                    expanded = openDropdownHotelId == hotel.id,
                                                    onDismissRequest = { openDropdownHotelId = null },
                                                    modifier = Modifier
                                                        .background(getDialogBackground())
                                                        .border(0.5.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Edit this hotel", color = AccentCyan) },
                                                        onClick = {
                                                            hotelToEdit = hotel
                                                            openDropdownHotelId = null
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                        onClick = {
                                                            pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                            openDropdownHotelId = null
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "monthly expenditure",
                                            color = AppTextColor().copy(alpha = 0.9f),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            RollingNumberText(
                                                value = if (hotel.id == activeHotelId) monthlyTotal else 0.0,
                                                style = TextStyle(
                                                    color = AppTextColor(),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.SansSerif
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty state if no hotel selected/available
                    EmptyStateCard(message = "No Hotels configured. Create one in Settings!")
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // BEHAVIORAL ANALYTICS CAROUSEL HEADER
                Text(
                    text = "Frequently Ordered Meals",
                    color = AppTextColor(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (popularMeals.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text("No frequently ordered meals yet. Add some meals to see insights!", color = AppTextColor().copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(popularMeals, key = { it.first }) { (name, count) ->
                            PresetMealCard(
                                title = name,
                                description = "Frequently ordered dish at this establishment",
                                tag = "$count times",
                                color = AccentCyan
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))

                // VIEW MEAL HISTORY TRIGGER BUTTON
                activeHotel?.let {
                    GlassButton(
                        onClick = { showMealHistory = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = AccentCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Meal History",
                            color = AppTextColor(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar and FAB
            }
        }

        // FAB FOR ADDING NEW MEAL
        if (activeHotel != null) {
            FloatingActionButton(
                onClick = {
                    viewModel.resetOrderFlow()
                    showAddMealFlow = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 16.dp)
                    .bounceClick()
                    .testTag("add_meal_fab"),
                shape = CircleShape,
                containerColor = Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isAppLightMode()) Color.White else Color(0xFF0F0B26), shape = CircleShape)
                        .border(2.dp, AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Meal",
                        tint = AccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        

    }

    // Modal Sheet / Full Dialog for Add Meal
    if (showAddMealFlow) {
        AddMealFlowModal(
            viewModel = viewModel,
            onDismiss = { showAddMealFlow = false }
        )
    }

    // Modal Dialog for Meal History
    
    if (showInfoDialog != null) {
        val h = showInfoDialog!!
        ModalBottomSheet(
            onDismissRequest = { showInfoDialog = null },
            containerColor = getDialogBackground(), // fully opaque background
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 48.dp).fillMaxWidth()) {
                Text(text = "Hotel Details", color = AccentCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Name: ${h.name}", color = AppTextColor(), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Location: ${h.location.ifBlank { "Not provided" }}", color = AppTextColor(), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Advanced Payment: ₹${h.advancedPayment}", color = AppTextColor(), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Phone Number: ${h.phoneNumber.ifBlank { "Not provided" }}", color = AppTextColor(), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "UPI ID: ${h.upiId.ifBlank { "Not provided" }}", color = AppTextColor(), fontSize = 16.sp)
                if (h.qrCodeUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "QR Codes", color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        h.qrCodeUri.split(",").filter { it.isNotBlank() }.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "QR Code",
                                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { showInfoDialog = null }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)) {
                    Text("Close", color = Color.Black)
                }
            }
        }
    }

    if (showMealHistory) {
        MealHistoryModal(
            viewModel = viewModel,
            onDismiss = { showMealHistory = false }
        )
    }

    // Modal Sheet for Editing Hotel Parameters
    if (hotelToEdit != null) {
        EditHotelModal(
            hotel = hotelToEdit!!,
            viewModel = viewModel,
            onDismiss = { hotelToEdit = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHotelModal(
    hotel: com.example.data.model.HotelEntity?,
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var editHotelName by remember(hotel) { mutableStateOf(hotel?.name ?: "") }
    var editHotelAdvanced by remember(hotel) { mutableStateOf(hotel?.advancedPayment?.toString() ?: "") }
    var editPhone by remember(hotel) { mutableStateOf(hotel?.phoneNumber ?: "") }
    var editLocation by remember(hotel) { mutableStateOf(hotel?.location ?: "") }
    var editUpiId by remember(hotel) { mutableStateOf(hotel?.upiId ?: "") }
    var editQrCodeUris by remember(hotel) { mutableStateOf(hotel?.qrCodeUri?.split(",")?.filter { it.isNotBlank() } ?: emptyList()) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showBin by remember { mutableStateOf(false) }
    var editingMenuItem by remember { mutableStateOf<com.example.data.model.MenuEntity?>(null) }

    val qrCodeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            editQrCodeUris = editQrCodeUris + uris.map { it.toString() }
        }
    }

    var editItemImageUri by remember { mutableStateOf<String?>(null) }
    val editItemImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            editItemImageUri = uri.toString()
        }
    }

    var newItemImageUri by remember { mutableStateOf<String?>(null) }
    val newItemImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            newItemImageUri = uri.toString()
        }
    }

    val menuItems by viewModel.allMenuItems.collectAsState(initial = emptyList())
    val deletedMenuItems by viewModel.deletedMenuItems.collectAsState(initial = emptyList())
    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write("Name,Price,Type,Emoji\n")
                    menuItems.forEach { m ->
                        writer.write("${m.englishName},${m.price},${m.type},${m.emoji}\n")
                    }
                }
                android.widget.Toast.makeText(context, "Menu Exported", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export Failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val parts = line.split(",")
                        if (parts.size >= 4) {
                            val name = parts[0]
                            val price = parts[1].toDoubleOrNull() ?: 0.0
                            val type = parts[2]
                            val emoji = parts[3]
                            viewModel.addMenu(com.example.data.model.MenuEntity(
                                hotelId = viewModel.activeHotelId.value,
                                banglaName = name,
                                englishName = name,
                                price = price,
                                isDayMenu = type == "Day" || type == "Common",
                                isNightMenu = type == "Night" || type == "Common",
                                isAddOn = false,
                                emoji = emoji,
                                type = type
                            ))
                        }
                    }
                }
                android.widget.Toast.makeText(context, "Menu Imported", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import Failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (hotel == null) "Add Hotel" else "Edit Hotel Info",
                color = AppTextColor(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = editHotelName,
                onValueChange = { editHotelName = it },
                label = { Text("Hotel Name", color = AppTextColor().copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = editHotelAdvanced,
                onValueChange = { editHotelAdvanced = it },
                label = { Text("Advanced Payment (₹)", color = AppTextColor().copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = editPhone,
                onValueChange = { editPhone = it },
                label = { Text("Mobile Number", color = AppTextColor().copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editLocation,
                    onValueChange = { editLocation = it },
                    label = { Text("Maps Link / Location", color = AppTextColor().copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                        val intent = if (editLocation.startsWith("http://") || editLocation.startsWith("https://")) {
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(editLocation))
                        } else {
                            val query = if (editLocation.isNotBlank()) android.net.Uri.encode(editLocation) else "Restaurants"
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$query"))
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.background(AccentCyan.copy(alpha=0.1f), CircleShape).bounceClick()
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Open Maps", tint = AccentCyan)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = editUpiId,
                onValueChange = { editUpiId = it },
                label = { Text("UPI ID", color = AppTextColor().copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("QR Codes (${editQrCodeUris.size})", color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                editQrCodeUris.forEachIndexed { index, uri ->
                    Box(modifier = Modifier.height(60.dp).background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QR ${index + 1}", color = AppTextColor(), fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { editQrCodeUris = editQrCodeUris.toMutableList().apply { removeAt(index) } }, modifier = Modifier.size(20.dp).bounceClick()) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = AccentPink)
                            }
                        }
                    }
                }
                Box(modifier = Modifier.height(60.dp).width(60.dp).clickable { qrCodeLauncher.launch("image/*") }.background(AccentCyan.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Add QR", tint = AccentCyan)
                }
            }
            
            if (hotel != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Menu Chart", color = AppTextColor(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { csvImportLauncher.launch("text/csv") }, modifier = Modifier.bounceClick()) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import CSV", tint = AppTextColor())
                    }
                    IconButton(onClick = { csvExportLauncher.launch("menu_${hotel.name.replace(" ", "_")}.csv") }) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = AppTextColor())
                    }
                    IconButton(onClick = { showAddMenu = !showAddMenu }, modifier = Modifier.bounceClick()) {
                        Icon(Icons.Default.Add, contentDescription = "Add Menu", tint = AccentCyan)
                    }
                    IconButton(onClick = { showBin = !showBin }, modifier = Modifier.bounceClick()) {
                        Icon(Icons.Default.Delete, contentDescription = "View Bin", tint = if (showBin) AccentPink else AppTextColor())
                    }
                }
            }
            Text("Edit menu chart and items. You can add Day, Night, or Common meals.", color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                if (showBin) {
                    Text("Recycle Bin 🗑️", color = AccentPink, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
                    if (deletedMenuItems.isEmpty()) {
                        Text("Bin is empty. Deleted items stay here so you can bring them back.", color = AppTextColor().copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            deletedMenuItems.forEach { menuItem ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(getDialogBackground(), RoundedCornerShape(8.dp)).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${menuItem.emoji} ${menuItem.englishName}", color = AppTextColor(), fontWeight = FontWeight.Bold)
                                        Text("₹${menuItem.price} • ${menuItem.type}", color = AppTextColor().copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                    TextButton(
                                        onClick = { viewModel.restoreMenu(menuItem) },
                                        modifier = Modifier.bounceClick()
                                    ) {
                                        Text("Restore", color = AccentCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (menuItems.isEmpty()) {
                        Text("No items added yet. Click '+' or import via CSV.", color = AppTextColor().copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                    } else {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .background(AppTextColor().copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Icon / Img", color = AppTextColor().copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                            Text("Name", color = AppTextColor().copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                            Text("Price", color = AppTextColor().copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                            Text("Type", color = AppTextColor().copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.0f))
                            Text("Action", color = AppTextColor().copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            menuItems.forEach { menuItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(getDialogBackground(), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icon / Img Column
                                    Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.CenterStart) {
                                        DishImageAndEmoji(
                                            emoji = menuItem.emoji,
                                            imageUri = menuItem.imageUri,
                                            size = 32.dp
                                        )
                                    }
                                    
                                    // Name Column
                                    Column(modifier = Modifier.weight(1.5f).padding(end = 4.dp)) {
                                        Text(menuItem.englishName, color = AppTextColor(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        if (menuItem.banglaName != menuItem.englishName) {
                                            Text(menuItem.banglaName, color = AppTextColor().copy(alpha = 0.5f), fontSize = 10.sp)
                                        }
                                    }
                                    
                                    // Price Column
                                    Text("₹${menuItem.price.toInt()}", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(0.8f))
                                    
                                    // Type Column
                                    Box(
                                        modifier = Modifier
                                            .weight(1.0f)
                                            .padding(end = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = when (menuItem.type) {
                                                        "Day" -> AccentGold.copy(alpha = 0.15f)
                                                        "Night" -> AccentCyan.copy(alpha = 0.15f)
                                                        else -> AppTextColor().copy(alpha = 0.1f)
                                                    },
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = menuItem.type,
                                                color = when (menuItem.type) {
                                                    "Day" -> AccentGold
                                                    "Night" -> AccentCyan
                                                    else -> AppTextColor()
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    // Action Column
                                    Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                                        IconButton(
                                            onClick = { 
                                                editingMenuItem = menuItem 
                                                editItemImageUri = menuItem.imageUri
                                            }, 
                                            modifier = Modifier.size(28.dp).bounceClick()
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Item", tint = AccentCyan, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (editingMenuItem != null) {
                val item = editingMenuItem!!
                var editName by remember(item) { mutableStateOf(item.englishName) }
                var editPrice by remember(item) { mutableStateOf(item.price.toString()) }
                var editEmoji by remember(item) { mutableStateOf(item.emoji) }
                var editType by remember(item) { mutableStateOf(item.type) }

                AlertDialog(
                    onDismissRequest = { 
                        editingMenuItem = null 
                        editItemImageUri = null
                    },
                    containerColor = getDialogBackground(),
                    title = { Text("Edit Menu Item", color = AccentCyan, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Image Picker/Indicator
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppTextColor().copy(alpha = 0.05f))
                                        .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable { editItemImagePicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!editItemImageUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = editItemImageUri,
                                            contentDescription = null,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.TopEnd)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                .clickable { editItemImageUri = null },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(10.dp))
                                        }
                                    } else {
                                        Icon(Icons.Default.AddAPhoto, contentDescription = "Photo", tint = AccentCyan, modifier = Modifier.size(20.dp))
                                    }
                                }
                                OutlinedTextField(
                                    value = editEmoji, onValueChange = { editEmoji = it },
                                    label = { Text("Emoji", color = AppTextColor().copy(alpha = 0.5f)) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(0.3f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                                )
                                OutlinedTextField(
                                    value = editName, onValueChange = { editName = it },
                                    label = { Text("Name", color = AppTextColor().copy(alpha = 0.5f)) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(0.7f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = editPrice, onValueChange = { editPrice = it },
                                    label = { Text("Price (₹)", color = AppTextColor().copy(alpha = 0.5f)) },
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(0.5f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                                )
                                Row(modifier = Modifier.weight(0.5f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Day", "Night", "Common").forEach { typeOpt ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (editType == typeOpt) AccentCyan else Color.Transparent)
                                                .border(1.dp, AccentCyan, RoundedCornerShape(8.dp))
                                                .clickable { editType = typeOpt }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(typeOpt, color = if (editType == typeOpt) Color.Black else AccentCyan, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                viewModel.deleteMenu(item)
                                editingMenuItem = null
                                editItemImageUri = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPink)
                        ) {
                            Text("Move to Bin", color = Color.White)
                        }
                    },
                    confirmButton = {
                        Row {
                            TextButton(onClick = { 
                                editingMenuItem = null 
                                editItemImageUri = null
                            }) {
                                Text("Cancel", color = AppTextColor())
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val p = editPrice.toDoubleOrNull() ?: item.price
                                    if (editName.isNotBlank()) {
                                        viewModel.updateMenu(item.copy(
                                            englishName = editName,
                                            banglaName = editName,
                                            price = p,
                                            emoji = editEmoji,
                                            type = editType,
                                            isDayMenu = editType == "Day" || editType == "Common",
                                            isNightMenu = editType == "Night" || editType == "Common",
                                            imageUri = editItemImageUri
                                        ))
                                        editingMenuItem = null
                                        editItemImageUri = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                            ) {
                                Text("Save", color = Color.Black)
                            }
                        }
                    }
                )
            }
            
            if (showAddMenu) {
                var newName by remember { mutableStateOf("") }
                var newPrice by remember { mutableStateOf("") }
                var newEmoji by remember { mutableStateOf("🍽️") }
                var newType by remember { mutableStateOf("Day") } // Day, Night, Common
                
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add New Menu Item", color = AccentCyan, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Image Picker/Indicator
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppTextColor().copy(alpha = 0.05f))
                                    .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { newItemImagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!newItemImageUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = newItemImageUri,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            .clickable { newItemImageUri = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                } else {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = "Photo", tint = AccentCyan, modifier = Modifier.size(20.dp))
                                }
                            }
                            OutlinedTextField(
                                value = newEmoji, onValueChange = { newEmoji = it },
                                label = { Text("Emoji", color = AppTextColor().copy(alpha = 0.5f)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(0.3f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                            )
                            OutlinedTextField(
                                value = newName, onValueChange = { newName = it },
                                label = { Text("Name", color = AppTextColor().copy(alpha = 0.5f)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(0.7f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newPrice, onValueChange = { newPrice = it },
                                label = { Text("Price (₹)", color = AppTextColor().copy(alpha = 0.5f)) },
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.5f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                            )
                            Row(modifier = Modifier.weight(0.5f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Day", "Night", "Common").forEach { typeOpt ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (newType == typeOpt) AccentCyan else Color.Transparent)
                                            .border(1.dp, AccentCyan, RoundedCornerShape(8.dp))
                                            .clickable { newType = typeOpt }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(typeOpt, color = if (newType == typeOpt) Color.Black else AccentCyan, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val p = newPrice.toDoubleOrNull() ?: 0.0
                                if (newName.isNotBlank()) {
                                    viewModel.addMenu(com.example.data.model.MenuEntity(
                                        hotelId = viewModel.activeHotelId.value,
                                        banglaName = newName,
                                        englishName = newName,
                                        price = p,
                                        isDayMenu = newType == "Day" || newType == "Common",
                                        isNightMenu = newType == "Night" || newType == "Common",
                                        isAddOn = false,
                                        emoji = newEmoji,
                                        type = newType,
                                        imageUri = newItemImageUri
                                    ))
                                    showAddMenu = false
                                    newItemImageUri = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text("Save Menu Item", color = Color.Black)
                        }
                    }
                }
            }
            } // End of if (hotel != null)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hotel != null) {
                    IconButton(
                        onClick = {
                            viewModel.deleteHotel(hotel)
                            onDismiss()
                        },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Move to Trash Bin",
                            tint = AccentPink
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AccentPink)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        if (editHotelName.isNotBlank()) {
                            val advanced = editHotelAdvanced.toDoubleOrNull() ?: 0.0
                            if (hotel != null) {
                                viewModel.editHotel(hotel.copy(
                                    name = editHotelName,
                                    advancedPayment = advanced,
                                    phoneNumber = editPhone,
                                    location = editLocation,
                                    upiId = editUpiId,
                                    qrCodeUri = editQrCodeUris.joinToString(",")
                                ))
                            } else {
                                viewModel.addHotel(
                                    name = editHotelName,
                                    advancedPayment = advanced,
                                    phoneNumber = editPhone,
                                    location = editLocation,
                                    upiId = editUpiId,
                                    qrCodeUri = editQrCodeUris.joinToString(",")
                                )
                            }
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text(if (hotel == null) "Add Hotel" else "Save Changes", color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PresetMealCard(
    title: String,
    description: String,
    tag: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(110.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            color = color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        color = AppTextColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = description,
                    color = AppTextColor().copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==========================================
// FULL-SCREEN GLASS ADD MEAL MODAL
// ==========================================

@Composable
fun AddMealFlowModal(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    
    val date by viewModel.addMealDate.collectAsStateWithLifecycle()
    val isNight by viewModel.addMealIsNight.collectAsStateWithLifecycle()
    val quantities by viewModel.orderQuantities.collectAsStateWithLifecycle()
    val selectedMainId by viewModel.selectedMainDishId.collectAsStateWithLifecycle()
    val menuItems by viewModel.allMenuItems.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Filter menu dynamically using remember to avoid redundant work
    val filteredDishes = remember(menuItems, isNight) {
        menuItems.filter {
            if (isNight) it.isNightMenu else it.isDayMenu
        }
    }
    val mainDishes = remember(filteredDishes) { filteredDishes.filter { !it.isAddOn } }
    val addOnDishes = remember(filteredDishes) { filteredDishes.filter { it.isAddOn } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(userProfile.themeMode))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.background(getGlassBorder(), CircleShape).bounceClick()
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTextColor())
                    }

                    Text(
                        text = "Add Meal Record",
                        color = AppTextColor(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Glowy Save Button
                    GlassButton(
                        onClick = {
                            viewModel.saveHotelTransaction()
                            onDismiss()
                        },
                        cornerRadius = 10.dp,
                        borderBrush = Brush.linearGradient(listOf(AccentCyan, AccentPink)),
                        modifier = Modifier.testTag("save_meal_button")
                    ) {
                        Text(
                            text = "Save",
                            color = AppTextColor(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // TOP SECTION: DATE/TIME DRUM CONTROLS
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Date Picked",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            InlineDatePicker(
                                initialDateMillis = date,
                                onDateSelected = { newDate ->
                                    // Just update the date directly using viewmodel
                                    // But wait, there is no viewModel.setDate() yet, we should add it if it's missing, or viewModel.addMealDate.value = newDate. 
                                    // Since addMealDate is MutableStateFlow in ViewModel, maybe there's a setter. Let's check ViewModel.
                                    viewModel.setAddMealDate(newDate)
                                },
                                textColor = AppTextColor()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // DAY/NIGHT CUSTOM LIQUID SLIDING SWITCH
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Menu Period Selection",
                            color = AppTextColor(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Highly styled liquid sliding track
                        val isAppLight = isAppLightMode()
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (isAppLight) Color(0x1A000000) else Color(0x1F000000))
                                .border(1.dp, if (isAppLight) Color(0xFFFFFFFF) else getGlassBorder(), RoundedCornerShape(19.dp))
                                .clickable { viewModel.toggleDayNight() }
                        ) {
                            val thumbOffset by animateDpAsState(
                                targetValue = if (isNight) 65.dp else 2.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "thumb"
                            )

                            // Glass sliding thumb
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .offset(x = thumbOffset)
                                    .width(63.dp)
                                    .fillMaxHeight()
                                    .background(
                                        brush = if (isNight) {
                                            Brush.linearGradient(listOf(Color(0xFF3F51B5), Color(0xFF673AB7)))
                                        } else {
                                            Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFFF5722)))
                                        },
                                        shape = RoundedCornerShape(17.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = AppTextColor(),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Underlying Labels
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Day",
                                    color = if (!isNight) AppTextColor() else AppTextColor().copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Night",
                                    color = if (isNight) AppTextColor() else AppTextColor().copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MAIN DISHES GRID SECTION
                    Text(
                        text = "Main Dishes (Select One to View Add-ons)",
                        color = AppTextColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Standard fluid wrapping flow/grid of frosted tiles
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val rows = mainDishes.chunked(2)
                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { dish ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        DishCard(
                                            dish = dish,
                                            qty = quantities[dish.id] ?: 0,
                                            isSelected = selectedMainId == dish.id,
                                            onSelect = { viewModel.selectMainDish(dish.id) },
                                            onUpdateQty = { delta -> viewModel.updateItemQuantity(dish.id, delta) }
                                        )
                                    }
                                }
                                if (row.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // DYNAMICALLY RENDERED ADD-ON DISHES TRAY BELOW
                    AnimatedVisibility(
                        visible = selectedMainId != null,
                        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            val activeMainName = menuItems.find { it.id == selectedMainId }?.englishName ?: ""
                            Text(
                                text = "Supplementary Add-ons for $activeMainName",
                                color = AccentCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    addOnDishes.forEach { addOn ->
                                        val addOnQty = quantities[addOn.id] ?: 0

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = addOn.banglaName,
                                                    color = AppTextColor(),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "${addOn.englishName} • ₹${addOn.price.toInt()}",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (addOnQty > 0) {
                                                    IconButton(
                                                        onClick = { viewModel.updateItemQuantity(addOn.id, -1) },
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .background(getGlassIconButtonBg(), CircleShape)
                                                    ) {
                                                        Icon(Icons.Default.Remove, contentDescription = "-", tint = AppTextColor(), modifier = Modifier.size(12.dp))
                                                    }
                                                    Text(
                                                        text = "$addOnQty",
                                                        color = AppTextColor(),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.updateItemQuantity(addOn.id, 1) },
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(if (addOnQty > 0) AccentCyan else getGlassIconButtonBg(), CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "+",
                                                        tint = if (addOnQty > 0) Color(0xFF0F0B26) else AppTextColor(),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (addOn != addOnDishes.last()) {
                                            HorizontalDivider(color = Color(0x0FFFFFFF), thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // CURRENT TOTAL BILL SUMMARY
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transaction Sum",
                                color = AppTextColor(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val totalBill = quantities.mapNotNull { (id, qty) ->
                                val item = menuItems.find { it.id == id }
                                if (item != null) item.price * qty else null
                            }.sum()

                            Text(
                                text = "₹${String.format(Locale.US, "%,.2f", totalBill)}",
                                color = AccentGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
    

}

// ==========================================
// CHRONOLOGICAL VIEW MEAL HISTORY MODAL
// ==========================================
@Composable
fun MealHistoryModal(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val transactions by viewModel.activeHotelTransactions.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    var monthOffset by remember { mutableStateOf(0) }
    
    val filteredTransactions = remember(transactions, monthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        val targetMonth = cal.get(Calendar.MONTH)
        val targetYear = cal.get(Calendar.YEAR)
        
        transactions.filter {
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = it.timestamp
            txCal.get(Calendar.MONTH) == targetMonth && txCal.get(Calendar.YEAR) == targetYear
        }
    }
    
    val currentDisplayMonth = remember(monthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        monthFormat.format(cal.time)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(userProfile.themeMode))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.background(getGlassBorder(), CircleShape).bounceClick()
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTextColor())
                    }

                    Text(
                        text = "Meal Record Ledger",
                        color = AppTextColor(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(48.dp)) // Equal space balancing
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { monthOffset -= 1 }, modifier = Modifier.bounceClick()) {
                        Icon(Icons.Default.ArrowBack, "Previous Month", tint = AppTextColor())
                    }
                    Text(currentDisplayMonth, color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { monthOffset += 1 }, enabled = monthOffset < 0) {
                        Icon(Icons.Default.ArrowForward, "Next Month", tint = if (monthOffset < 0) AppTextColor() else AppTextColor().copy(alpha = 0.3f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (filteredTransactions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        EmptyStateCard(message = "No past meal logs for $currentDisplayMonth.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTransactions, key = { it.id }) { tx ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (tx.isNight) Icons.Default.NightsStay else Icons.Default.WbSunny,
                                                contentDescription = null,
                                                tint = if (tx.isNight) AccentCyan else AccentGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = sdf.format(Date(tx.timestamp)),
                                                color = AppTextColor(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Expandable simple text list of ordered items
                                        val items = TransactionSerializer.deserialize(tx.itemsSerialized)
                                        val summary = items.joinToString(", ") { "${it.name} x${it.quantity}" }
                                        Text(
                                            text = summary,
                                            color = AppTextColor().copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "₹${tx.totalAmount.toInt()}",
                                            color = AccentGreen,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        IconButton(
                                            onClick = { viewModel.deleteTransaction(tx) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Color(0x15FF4081), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = AccentPink,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// MODULE B: EXTRA EXPENSES TAB
// ==========================================
@Composable
fun ExtraExpensesTab(viewModel: FinancialViewModel) {
    val accounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val expenses by viewModel.allExtraExpenses.collectAsStateWithLifecycle()
    val userCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    var showAddExpense by remember { mutableStateOf(false) }
    var selectedExpenseIdForAttachments by remember { mutableStateOf<Int?>(null) }
    var showDetailedHistory by remember { mutableStateOf(false) }
    
    // Interactive Category Filter & Search
    var searchQuery by remember { mutableStateOf("") }
    val systemCategories = listOf("All", "Food", "Academics", "Travel", "Salary", "Shopping", "Entertainment", "General")
    val allCategoryNames = remember(userCategories) {
        (systemCategories + userCategories.map { it.name }).distinct()
    }
    var activeCategoryFilter by remember { mutableStateOf("All") }

    // Asynchronous Calculations using remember & derivedStateOf
    val totalBalance by remember(accounts) {
        derivedStateOf { accounts.sumOf { it.balance } }
    }
    val totalIncome by remember(expenses) {
        derivedStateOf {
            expenses.filter { it.type == "INCOME" }.sumOf { it.amount }
        }
    }
    val totalExpense by remember(expenses) {
        derivedStateOf {
            expenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        }
    }

    val filteredExpenses by remember(expenses, activeCategoryFilter, searchQuery) {
        derivedStateOf {
            expenses.filter { exp ->
                val matchesCategory = if (activeCategoryFilter == "All") true 
                                     else exp.category.lowercase() == activeCategoryFilter.lowercase()
                val matchesSearch = if (searchQuery.isBlank()) true 
                                   else exp.name.contains(searchQuery, ignoreCase = true) || 
                                        exp.notes.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch
            }
        }
    }

    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    if (showDetailedHistory) {
        ExtraExpenseDetailedHistoryScreen(
            viewModel = viewModel,
            onDismiss = { showDetailedHistory = false }
        )
    } else if (showAddExpense) {
        AddExpenseScreen(
            viewModel = viewModel,
            onDismiss = { showAddExpense = false }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    // Hero Balance Card with split Income and Expense indications
                    // Move this to top as requested
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 12.dp)
                            .clickable { showDetailedHistory = true } // Open detailed history on click
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp
                        ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Available Balance",
                            color = AppTextColor().copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RollingNumberText(
                            value = totalBalance,
                            prefix = "₹",
                            style = TextStyle(
                                color = AppTextColor(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = AppTextColor().copy(alpha = 0.1f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Total Income Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0x1F00E676), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Income",
                                        tint = AccentGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Income", color = AppTextColor().copy(alpha = 0.5f), fontSize = 10.sp)
                                    RollingNumberText(
                                        value = totalIncome,
                                        prefix = "₹",
                                        style = TextStyle(
                                            color = AccentGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Divider Line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(AppTextColor().copy(alpha = 0.1f))
                            )

                            // Total Expense Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0x1FFF4081), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Expense",
                                        tint = AccentPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Expense", color = AppTextColor().copy(alpha = 0.5f), fontSize = 10.sp)
                                    RollingNumberText(
                                        value = totalExpense,
                                        prefix = "₹",
                                        style = TextStyle(
                                            color = AccentPink,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // ACCOUNT BANKING METALLIC CAROUSEL
            Text(
                text = "Virtual Wallet Accounts",
                color = AppTextColor().copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (accounts.isEmpty()) {
                EmptyStateCard(message = "No virtual bank accounts set. Add one in Settings!")
            } else {
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { accounts.size })
                
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    pageSpacing = 16.dp
                ) { page ->
                    val account = accounts[page]
                    var showAccountMenu by remember { mutableStateOf(false) }
                    var showQRDialog by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 22.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = account.name,
                                            color = AppTextColor(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        if (account.upiId.isNotBlank()) {
                                            Text(
                                                text = account.upiId,
                                                color = AppTextColor().copy(alpha = 0.5f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    
                                    Row {
                                        if (account.upiId.isNotBlank() || account.qrCodeUri.isNotBlank()) {
                                            IconButton(onClick = { showQRDialog = true }) {
                                                Icon(Icons.Default.Info, contentDescription = "UPI Info", tint = AccentCyan)
                                            }
                                        }

                                        Box {
                                            IconButton(onClick = { showAccountMenu = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = AppTextColor())
                                            }
                                            DropdownMenu(
                                                expanded = showAccountMenu,
                                                onDismissRequest = { showAccountMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Delete", color = Color.Red) },
                                                    onClick = {
                                                        showAccountMenu = false
                                                        viewModel.deleteVirtualAccount(account)
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                                                )
                                            }
                                        }
                                    }
                                }

                                Column {
                                    Text(
                                        text = "Current Balance",
                                        color = AppTextColor().copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                    RollingNumberText(
                                        value = account.balance,
                                        prefix = "₹",
                                        style = TextStyle(
                                            color = AppTextColor(),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "•••• •••• •••• ${1000 + account.id}",
                                        color = AppTextColor().copy(alpha = 0.3f),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Virtual Wallet",
                                        color = AccentCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (showQRDialog) {
                            Dialog(onDismissRequest = { showQRDialog = false }) {
                                GlassCard(
                                    cornerRadius = 24.dp,
                                    modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("UPI Details", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppTextColor())
                                        Spacer(modifier = Modifier.height(16.dp))
                                        if (account.qrCodeUri.isNotBlank()) {
                                            AsyncImage(
                                                model = account.qrCodeUri,
                                                contentDescription = "QR Code",
                                                modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp))
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                        if (account.upiId.isNotBlank()) {
                                            Text("UPI ID: ${account.upiId}", color = AppTextColor(), fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(20.dp))
                                        }
                                        GlassButton(onClick = { showQRDialog = false }) {
                                            Text("Close")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            // Search Bar & Filter Header
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                cornerRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = AppTextColor().copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        textStyle = TextStyle(color = AppTextColor(), fontSize = 16.sp),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search transactions...", color = AppTextColor().copy(alpha = 0.4f))
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = AppTextColor().copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // Horizontal Scrollable Category Chips for Quick Filter
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategoryNames, key = { it }) { cat ->
                    val isSelected = activeCategoryFilter == cat
                    val chipBg = if (isSelected) AccentCyan.copy(alpha = 0.15f) else Color.Transparent
                    val chipBorder = if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.2f)
                    val chipText = if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.6f)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(50))
                            .clickable { activeCategoryFilter = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            color = chipText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (filteredExpenses.isEmpty()) {
            item {
                EmptyStateCard(message = if (activeCategoryFilter == "All") "No transactions registered yet." else "No transactions found in category '$activeCategoryFilter'")
            }
        } else {
            items(filteredExpenses, key = { it.id }) { exp ->
                val sourceAccountName = accounts.find { it.id == exp.bankAccountId }?.name ?: "Unknown Account"
                val (categoryIcon, categoryColor) = getCategoryIconAndColor(exp.category, exp.type)

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    cornerRadius = 18.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Category Icon wrapped in a circular tonal container
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(categoryColor.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, categoryColor.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIcon,
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = exp.name,
                                    color = AppTextColor(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    val tagBg = if (exp.type == "INCOME") Color(0x1F00E676) else Color(0x1FFF4081)
                                    val tagText = if (exp.type == "INCOME") AccentGreen else AccentPink
                                    Box(
                                        modifier = Modifier
                                            .background(tagBg, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = exp.category,
                                            color = tagText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sourceAccountName,
                                        color = AccentCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = " • ${sdf.format(Date(exp.timestamp))}",
                                        color = AppTextColor().copy(alpha = 0.4f),
                                        fontSize = 10.sp
                                    )
                                }

                                // Display Custom Notes if present
                                if (exp.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = exp.notes,
                                        color = AppTextColor().copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                                
                                // Always show attachment icon if there are attachments (logic simplified to always show for now, or you could collect attachments count)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        selectedExpenseIdForAttachments = exp.id
                                    }
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = AccentGold, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "View Attachments",
                                        color = AccentGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            val amountColor = if (exp.type == "INCOME") AccentGreen else AccentPink
                            val amountPrefix = if (exp.type == "INCOME") "+₹" else "-₹"

                            RollingNumberText(
                                value = exp.amount,
                                prefix = amountPrefix,
                                style = TextStyle(
                                    color = amountColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            var showExpMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showExpMenu = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = AppTextColor().copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showExpMenu,
                                    onDismissRequest = { showExpMenu = false },
                                    modifier = Modifier.background(getDialogBackground())
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = AccentPink) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = AccentPink, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showExpMenu = false
                                            viewModel.deleteExtraExpense(exp)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Bottom-end floating button
    if (accounts.isNotEmpty()) {
        FloatingActionButton(
            onClick = { showAddExpense = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp)
                .testTag("add_expense_fab"),
            containerColor = AccentCyan,
            contentColor = Color.Black,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Transaction", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Attachment Viewer modal
    if (selectedExpenseIdForAttachments != null) {
        AttachmentViewerDialog(
            expenseId = selectedExpenseIdForAttachments!!,
            viewModel = viewModel,
            onDismiss = { selectedExpenseIdForAttachments = null }
        )
    }
}
}
}

@Composable
fun AttachmentViewerDialog(
    expenseId: Int,
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val attachments by viewModel.getAttachmentsForExpense(expenseId).collectAsState(initial = emptyList())
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(24.dp)
            .background(getDialogBackground(), RoundedCornerShape(24.dp))
            .border(0.5.dp, getGlassBorder(), RoundedCornerShape(24.dp)),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Attachments", color = AppTextColor(), fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = AppTextColor())
                }
            }
        },
        text = {
            if (attachments.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No attachments found", color = AppTextColor().copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(attachments, key = { it.uri }) { att ->
                        AttachmentItem(att, context)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentCyan)
            }
        }
    )
}

@Composable
fun AttachmentItem(attachment: AttachmentEntity, context: android.content.Context) {
    val (icon, color) = when (attachment.type) {
        "IMAGE" -> Icons.Default.Image to Color(0xFF64B5F6)
        "PDF" -> Icons.Default.PictureAsPdf to Color(0xFFF06292)
        "VOICE" -> Icons.Default.Mic to Color(0xFF4DB6AC)
        else -> Icons.Default.AttachFile to Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(getDialogBackground().copy(alpha = 0.5f))
            .border(0.5.dp, getGlassBorder(), RoundedCornerShape(12.dp))
            .clickable {
                try {
                    val uri = Uri.parse(attachment.uri)
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, when(attachment.type) {
                            "IMAGE" -> "image/*"
                            "PDF" -> "application/pdf"
                            "VOICE" -> "audio/*"
                            else -> "*/*"
                        })
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Could not open attachment", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(attachment.uri.substringAfterLast("/"), color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(attachment.type, color = color.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// Helper to return beautiful visual signifiers for category items
@Composable
fun getCategoryIconAndColor(category: String, type: String): Pair<ImageVector, Color> {
    return when (category.lowercase(Locale.getDefault())) {
        "food" -> Icons.Default.Restaurant to Color(0xFFFF9800)
        "academics" -> Icons.Default.School to Color(0xFF03A9F4)
        "travel" -> Icons.Default.Flight to Color(0xFF4CAF50)
        "shopping" -> Icons.Default.ShoppingBag to Color(0xFFE91E63)
        "entertainment" -> Icons.Default.Movie to Color(0xFF9C27B0)
        "salary" -> Icons.Default.MonetizationOn to Color(0xFF4CAF50)
        "investment" -> Icons.Default.TrendingUp to Color(0xFF00BCD4)
        "freelance" -> Icons.Default.Computer to Color(0xFF607D8B)
        "refund" -> Icons.Default.AssignmentReturn to Color(0xFFFFEB3B)
        "gifts" -> Icons.Default.CardGiftcard to Color(0xFFFF5722)
        else -> {
            if (type == "INCOME") {
                Icons.Default.TrendingUp to Color(0xFF26A69A)
            } else {
                Icons.Default.Payment to Color(0xFF78909C)
            }
        }
    }
}

// Preset Categories
val ExpenseCategories = listOf("Food", "Academics", "Travel", "Shopping", "Entertainment", "General")
val IncomeCategories = listOf("Salary", "Investment", "Freelance", "Refund", "Gifts", "General")


@Composable
fun AttachmentActionChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddExpenseScreen(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val accounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val userCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val themeMode by LocalAppTheme.current.let { remember { mutableStateOf(it) } }

    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0) }
    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Core Extended Transaction Fields
    var type by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var category by remember { mutableStateOf(ExpenseCategories.first()) }
    var notes by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf(listOf<Pair<Uri, String>>()) } // uri to type

    var showAccountDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingTimer by remember { mutableStateOf(0) }
    val mediaRecorder = remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var currentVoiceUri by remember { mutableStateOf<Uri?>(null) }
    
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { attachments = attachments + (it to "IMAGE") }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { attachments = attachments + (it to "PDF") }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording && recordingTimer < 120) {
                delay(1000)
                recordingTimer++
            }
            if (recordingTimer >= 120) {
                // Stop recording automatically
            }
        } else {
            recordingTimer = 0
        }
    }

    fun startRecording() {
        try {
            val fileName = "REC_${System.currentTimeMillis()}.amr"
            val file = File(context.cacheDir, fileName)
            currentVoiceUri = Uri.fromFile(file)
            
            mediaRecorder.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                android.media.MediaRecorder()
            }.apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.AMR_NB) // Low bitrate as requested
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            Log.e("AddExpenseScreen", "Failed to start recording", e)
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder.value?.apply {
                stop()
                release()
            }
            mediaRecorder.value = null
            isRecording = false
            currentVoiceUri?.let { attachments = attachments + (it to "VOICE") }
        } catch (e: Exception) {
            Log.e("AddExpenseScreen", "Failed to stop recording", e)
        }
    }

    val isFormValid = remember(name, amountStr, category) {
        name.isNotBlank() && amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0 && category.isNotBlank()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTextColor()
                        )
                    }
                    Text(
                        text = if (type == "EXPENSE") "Add Expense" else "Add Income",
                        color = AppTextColor(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Box(modifier = Modifier.size(40.dp))
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                GlassButton(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull()
                        if (isFormValid && amt != null && selectedAccountId != 0) {
                            viewModel.saveExtraExpense(
                                name = name,
                                amount = amt,
                                bankAccountId = selectedAccountId,
                                timestamp = timestamp,
                                receiptUri = null,
                                type = type,
                                category = category,
                                notes = notes,
                                attachments = attachments.map { it.first.toString() to it.second }
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_expense_button")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (type == "EXPENSE") AccentPink else AccentGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Register Transaction",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Transaction Type Switcher Card
            GlassCard(
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Transaction Mode",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        val isExpense = type == "EXPENSE"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isExpense) AccentPink.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    type = "EXPENSE"
                                    category = ExpenseCategories.first()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isExpense) AccentPink else AppTextColor().copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Deduction",
                                    color = if (isExpense) AccentPink else AppTextColor().copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isExpense) AccentGreen.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    type = "INCOME"
                                    category = IncomeCategories.first()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (!isExpense) AccentGreen else AppTextColor().copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Deposit",
                                    color = if (!isExpense) AccentGreen else AppTextColor().copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Amount Input Card
            GlassCard(
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Amount to Log",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "₹",
                            style = TextStyle(
                                color = if (type == "EXPENSE") AccentPink else AccentGreen,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = amountStr,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' }) {
                                    amountStr = input
                                }
                            },
                            textStyle = TextStyle(
                                color = AppTextColor(),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Start
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            cursorBrush = SolidColor(if (type == "EXPENSE") AccentPink else AccentGreen),
                            modifier = Modifier
                                .widthIn(min = 100.dp)
                                .testTag("expense_amount_input"),
                            decorationBox = { innerTextField ->
                                if (amountStr.isEmpty()) {
                                    Text(
                                        text = "0.00",
                                        style = TextStyle(
                                            color = AppTextColor().copy(alpha = 0.2f),
                                            fontSize = 40.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Details Card
            GlassCard(
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Transaction Details",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Title Input Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Title / Purpose") },
                        placeholder = { Text("e.g. Fuel, Lunch, Bonus") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedLabelColor = AccentCyan,
                            unfocusedLabelColor = AppTextColor().copy(alpha = 0.5f),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = AppTextColor().copy(alpha = 0.1f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("expense_title_input")
                    )

                    // Category Selector Input Field
                    Column {
                        Text(
                            text = "Category",
                            color = AppTextColor().copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val (catIcon, catColor) = getCategoryIconAndColor(category, type)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, AppTextColor().copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { showCategoryDropdown = true }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(category, color = AppTextColor(), fontWeight = FontWeight.Medium)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f))
                            }

                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false },
                                modifier = Modifier
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            ) {
                                val cats = if (type == "EXPENSE") ExpenseCategories else IncomeCategories
                                val allCats = (cats + userCategories.filter { it.type == type }.map { it.name }).distinct()
                                
                                allCats.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, color = Color.White) },
                                        onClick = {
                                            category = cat
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                                Divider(color = Color.White.copy(alpha = 0.1f))
                                DropdownMenuItem(
                                    text = { Text("+ Add Custom Category", color = AccentCyan) },
                                    onClick = {
                                        showCategoryDropdown = false
                                        showAddCategoryDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Add, null, tint = AccentCyan) }
                                )
                            }
                        }
                    }

                    // Wallet Linkage Dropdown
                    Column {
                        Text(
                            text = "Virtual Wallet Source",
                            color = AppTextColor().copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val activeAcc = accounts.find { it.id == selectedAccountId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, AppTextColor().copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { showAccountDropdown = true }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(activeAcc?.name ?: "Select Wallet Account", color = AppTextColor(), fontWeight = FontWeight.Medium)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f))
                            }

                            DropdownMenu(
                                expanded = showAccountDropdown,
                                onDismissRequest = { showAccountDropdown = false },
                                modifier = Modifier
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            ) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.name} (Bal: ₹${acc.balance.toInt()})", color = Color.White) },
                                        onClick = {
                                            selectedAccountId = acc.id
                                            showAccountDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Notes input field
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Short Notes (Optional)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedLabelColor = AccentCyan,
                            unfocusedLabelColor = AppTextColor().copy(alpha = 0.5f),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = AppTextColor().copy(alpha = 0.1f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date and Time picker card
            GlassCard(
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Log Date & Time",
                            color = AppTextColor().copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sdf.format(Date(timestamp)),
                            color = AppTextColor(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        cal.set(Calendar.YEAR, y)
                                        cal.set(Calendar.MONTH, m)
                                        cal.set(Calendar.DAY_OF_MONTH, d)
                                        timestamp = cal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                TimePickerDialog(
                                    context,
                                    { _, hr, min ->
                                        cal.set(Calendar.HOUR_OF_DAY, hr)
                                        cal.set(Calendar.MINUTE, min)
                                        timestamp = cal.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-Attachment Section
            GlassCard(
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Attachments & Voice Memo",
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttachmentActionChip(
                            icon = Icons.Default.Image,
                            label = "Image",
                            color = AccentCyan,
                            onClick = { imagePicker.launch("image/*") }
                        )
                        AttachmentActionChip(
                            icon = Icons.Default.PictureAsPdf,
                            label = "PDF",
                            color = AccentGold,
                            onClick = { pdfPicker.launch("application/pdf") }
                        )
                        
                        // Voice Memo Recorder
                        val voiceColor = if (isRecording) Color.Red else AccentPink
                        AttachmentActionChip(
                            icon = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            label = if (isRecording) "Stop (${120 - recordingTimer}s)" else "Voice",
                            color = voiceColor,
                            onClick = {
                                if (isRecording) stopRecording() else startRecording()
                            }
                        )
                    }

                    if (attachments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            attachments.forEach { (uri, type) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when(type) {
                                                "IMAGE" -> Icons.Default.Image
                                                "PDF" -> Icons.Default.PictureAsPdf
                                                else -> Icons.Default.Mic
                                            },
                                            contentDescription = null,
                                            tint = AppTextColor().copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = uri.lastPathSegment ?: "Attachment",
                                            color = AppTextColor(),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 150.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { attachments = attachments.filter { it.first != uri } },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add New Category") },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCatName.isNotBlank()) {
                        viewModel.addCategory(newCatName, type)
                        category = newCatName
                        showAddCategoryDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// ==========================================
// SETTINGS / CONFIGURATION FULL DIALOG
// ==========================================
@Composable
fun SettingsDialog(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val hotels by viewModel.allHotels.collectAsStateWithLifecycle()
    val accounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()

    // Input fields for Profile & Preferences
    var profileName by remember(userProfile) { mutableStateOf(userProfile?.name ?: "") }
    var profileContact by remember(userProfile) { mutableStateOf(userProfile?.contact ?: "") }

    var prefLanguage by remember(userProfile) { mutableStateOf(userProfile?.language ?: "en") }
    var prefThemeMode by remember(userProfile) { mutableStateOf(userProfile?.themeMode ?: "Auto") }
    var prefShowAdvancedPayment by remember(userProfile) { mutableStateOf(userProfile?.showAdvancedPayment ?: false) }
    var prefAppLockEnabled by remember(userProfile) { mutableStateOf(userProfile?.appLockEnabled ?: false) }
    var prefAppLockPin by remember(userProfile) { mutableStateOf(userProfile?.appLockPin ?: "") }
    var pinVisible by remember { mutableStateOf(false) }
    var isPinVerifiedForSettings by remember(userProfile) { mutableStateOf(userProfile?.appLockEnabled != true) }
    var showSecurityUnlockDialog by remember { mutableStateOf(false) }
    var prefDefaultHotelId by remember(userProfile) { mutableStateOf(userProfile?.defaultHotelId ?: 1) }
    var prefEnableStarAnimation by remember(userProfile) { mutableStateOf(userProfile?.enableStarAnimation ?: true) }

    // Dialog trigger states
    var showProfileEditDialog by remember { mutableStateOf(false) }
    var editingHotel by remember { mutableStateOf<HotelEntity?>(null) }
    var showAddHotelDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<BankAccountEntity?>(null) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showLegalInfoType by remember { mutableStateOf<String?>(null) } // "info", "license", "privacy"
    var showOwnerProfile by remember { mutableStateOf(false) }

    LaunchedEffect(prefLanguage, prefThemeMode, prefShowAdvancedPayment, prefAppLockEnabled, prefAppLockPin, prefDefaultHotelId, prefEnableStarAnimation, userProfile) {
        val up = userProfile
        if (up != null && (
            prefLanguage != up.language ||
            prefThemeMode != up.themeMode ||
            prefShowAdvancedPayment != up.showAdvancedPayment ||
            prefAppLockEnabled != up.appLockEnabled ||
            prefAppLockPin != up.appLockPin ||
            prefDefaultHotelId != up.defaultHotelId ||
            prefEnableStarAnimation != up.enableStarAnimation
        )) {
            viewModel.updatePreferences(
                language = prefLanguage,
                themeMode = prefThemeMode,
                showAdvancedPayment = prefShowAdvancedPayment,
                appLockEnabled = prefAppLockEnabled,
                appLockPin = prefAppLockPin,
                defaultHotelId = prefDefaultHotelId,
                enableStarAnimation = prefEnableStarAnimation
            )
        }
    }

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalAppTheme provides prefThemeMode) {
            AnimatedVisibility(
                visible = animateTrigger,
                enter = slideInVertically(initialOffsetY = { it / 8 }) + fadeIn(animationSpec = tween(250)),
                exit = slideOutVertically(targetOffsetY = { it / 8 }) + fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(getAppBackgroundGradient(prefThemeMode))
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Top App Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .background(getGlassIconButtonBg(), CircleShape)
                                .bounceClick()
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = AppTextColor()
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (prefLanguage == "bn") "সেটিংস ও নিয়ন্ত্রণ" else "Settings & Controls",
                            color = AppTextColor(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pixel-style search settings bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(AppTextColor().copy(alpha = 0.06f))
                            .clickable { showProfileEditDialog = true }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = AppTextColor().copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (prefLanguage == "bn") "সেটিংস খুঁজুন..." else "Search settings...",
                                    color = AppTextColor().copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                            // Decorative/Clickable user profile avatar on the right
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AccentCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profileName.take(1).uppercase(),
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Main scrollable settings content (Lazy-loaded for speed)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // Category: Profile
                            PixelSettingsGroup(
                                title = if (prefLanguage == "bn") "মালিকের প্রোফাইল" else "Owner Profile"
                            ) {
                                PixelSettingsItem(
                                    icon = Icons.Default.Phone,
                                    iconBgColor = Color(0xFFE3F2FD),
                                    iconTint = Color(0xFF1976D2),
                                    title = profileName,
                                    subtitle = if (profileContact.isNotBlank()) profileContact else (if (prefLanguage == "bn") "কোনো নম্বর নেই" else "No phone number set"),
                                    onClick = { showProfileEditDialog = true }
                                )
                            }
                        }

                        item {
                            // Category: App Appearance Controls
                            PixelSettingsGroup(
                                title = if (prefLanguage == "bn") "ডিসপ্লে ও অবয়ব" else "Appearance & Design"
                            ) {
                                // Sub-section: Theme Select
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (prefLanguage == "bn") "অ্যাপ থিম" else "App Theme",
                                        color = AppTextColor(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val themeOptions = listOf(
                                            Triple("Auto", Icons.Default.Settings, if (prefLanguage == "bn") "অটো (Auto)" else "Auto"),
                                            Triple("Light", Icons.Default.WbSunny, if (prefLanguage == "bn") "লাইট" else "Light"),
                                            Triple("Dark", Icons.Default.NightsStay, if (prefLanguage == "bn") "ডার্ক" else "Dark")
                                        )

                                        themeOptions.forEach { (mode, icon, label) ->
                                            val isSelected = prefThemeMode == mode
                                            val cardBg = if (isSelected) AccentCyan.copy(alpha = 0.15f) else AppTextColor().copy(alpha = 0.04f)
                                            val borderTint = if (isSelected) AccentCyan else Color.Transparent

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(cardBg)
                                                    .border(1.5.dp, borderTint, RoundedCornerShape(16.dp))
                                                    .clickable { prefThemeMode = mode }
                                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = label,
                                                        tint = if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.7f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = label,
                                                        color = if (isSelected) AccentCyan else AppTextColor(),
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                                // Language Selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (prefLanguage == "bn") "অ্যাপের ভাষা" else "App Language",
                                            color = AppTextColor(),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (prefLanguage == "bn") "ইংরেজি ও বাংলা সমর্থন করে" else "Supports English & Bengali",
                                            color = AppTextColor().copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("en" to "English", "bn" to "বাংলা").forEach { (langCode, langName) ->
                                            val isSel = prefLanguage == langCode
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSel) AccentCyan else AppTextColor().copy(alpha = 0.05f))
                                                    .clickable { prefLanguage = langCode }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = langName,
                                                    color = if (isSel) Color.Black else AppTextColor(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                                // Default Startup Hotel
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (prefLanguage == "bn") "ডিফল্ট স্টার্টআপ হোটেল" else "Default Startup Hotel",
                                        color = AppTextColor(),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (prefLanguage == "bn") "অ্যাপটি খুললে প্রথমে এই হোটেলটি নির্বাচিত থাকবে" else "This hotel is automatically active on startup",
                                        color = AppTextColor().copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(hotels, key = { it.id }) { hotel ->
                                            val isSel = prefDefaultHotelId == hotel.id
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSel) AccentCyan.copy(alpha = 0.2f) else AppTextColor().copy(alpha = 0.05f))
                                                    .border(1.dp, if (isSel) AccentCyan else Color.Transparent, RoundedCornerShape(12.dp))
                                                    .clickable { prefDefaultHotelId = hotel.id }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = hotel.name,
                                                    color = if (isSel) AccentCyan else AppTextColor(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                                // Show Advanced Payment Card Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (prefLanguage == "bn") "অগ্রিম পেমেন্ট কার্ড ড্যাশবোর্ড" else "Show Advanced Payment",
                                            color = AppTextColor(),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (prefLanguage == "bn") "প্রধান স্ক্রিনে অ্যাডভান্সড ডিপোজিট ব্যালেন্স প্রদর্শন" else "Display advanced balance in the main dashboard",
                                            color = AppTextColor().copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    GlassSwitch(
                                        checked = prefShowAdvancedPayment,
                                        onCheckedChange = { prefShowAdvancedPayment = it },
                                        activeText = "ON",
                                        inactiveText = "OFF"
                                    )
                                }

                                HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                                // Enable Star Celebration Animation
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (prefLanguage == "bn") "স্টার অ্যানিমেশন প্রভাব" else "Celebration Star Animations",
                                            color = AppTextColor(),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (prefLanguage == "bn") "লেনদেন বা মিল এন্ট্রি করলে উড়ন্ত স্টারের চমৎকার দৃশ্য" else "Animate flying colorful stars on successful entries",
                                            color = AppTextColor().copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    GlassSwitch(
                                        checked = prefEnableStarAnimation,
                                        onCheckedChange = { prefEnableStarAnimation = it },
                                        activeText = "ON",
                                        inactiveText = "OFF"
                                    )
                                }
                            }
                        }

                        item {
                            // Category: Establishments (Hotels)
                            PixelSettingsGroup(
                                title = if (prefLanguage == "bn") "হোটেল ও রেস্তোরাঁসমূহ" else "Hotels & Establishments"
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    hotels.forEach { hotel ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(AppTextColor().copy(alpha = 0.04f))
                                                .clickable { editingHotel = hotel }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFFF3E0)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Restaurant,
                                                    contentDescription = null,
                                                    tint = Color(0xFFE65100),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = hotel.name,
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Advance: ₹${hotel.advancedPayment.toInt()} • " + 
                                                           (if (hotel.phoneNumber.isNotBlank()) hotel.phoneNumber else "No Phone") +
                                                           (if (hotel.location.isNotBlank()) " • ${hotel.location}" else ""),
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit details",
                                                tint = AccentCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Sibling Adder Button
                                    Button(
                                        onClick = { showAddHotelDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = AccentCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (prefLanguage == "bn") "নতুন হোটেল যোগ করুন" else "Add New Hotel",
                                            color = AccentCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Category: Financial Bank Accounts
                            PixelSettingsGroup(
                                title = if (prefLanguage == "bn") "ভার্চুয়াল ব্যাংক অ্যাকাউন্ট" else "Virtual Bank Accounts"
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    accounts.forEach { acc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(AppTextColor().copy(alpha = 0.04f))
                                                .clickable { editingAccount = acc }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE8F5E9)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalance,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = acc.name,
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Liquid Balance: ₹${acc.balance.toInt()}",
                                                    color = AccentGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit details",
                                                tint = AccentCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Sibling Adder Button
                                    Button(
                                        onClick = { showAddAccountDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = AccentGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (prefLanguage == "bn") "নতুন অ্যাকাউন্ট যোগ করুন" else "Add New Account",
                                            color = AccentGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Category: Owner Accounts & Profiles
                            PixelSettingsGroup(
                                title = if (prefLanguage == "bn") "মালিক অ্যাকাউন্টসমূহ" else "Owner Accounts & UPI"
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PixelSettingsItem(
                                        icon = Icons.Default.AccountCircle,
                                        iconBgColor = Color(0xFFF3E5F5),
                                        iconTint = Color(0xFF7B1FA2),
                                        title = if (prefLanguage == "bn") "মালিক প্রোফাইল পরিচালনা" else "Manage Owner Profiles",
                                        subtitle = if (prefLanguage == "bn") "UPI, QR এবং অ্যাকাউন্ট ডিটেইলস" else "UPI, QR & Account Details",
                                        onClick = { showOwnerProfile = true }
                                    )
                                }
                            }
                        }

                        item {
                            // Category: Security & Pin Lock
                            PixelSettingsGroup(
                                title = if (prefLanguage == "bn") "নিরাপত্তা ও প্রাইভেসি" else "Security & Protection"
                            ) {
                                Column {
                                    if (userProfile?.appLockEnabled == true && !isPinVerifiedForSettings) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "সিকিউরিটি সেটিংস লক করা" else "Security Settings Locked",
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (prefLanguage == "bn") "পরিবর্তন করতে বর্তমান পিন যাচাই করুন" else "Verify current PIN to make modifications",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            GlassButton(
                                                onClick = { showSecurityUnlockDialog = true },
                                                cornerRadius = 12.dp
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Unlock Security",
                                                    tint = AccentCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (prefLanguage == "bn") "আনলক" else "Unlock",
                                                    color = AppTextColor(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (prefLanguage == "bn") "পাসকোড স্ক্রিন লক" else "Enable App Passcode",
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (prefLanguage == "bn") "অ্যাপ অ্যাক্সেস করতে ৪-সংখ্যার পিন লক সক্রিয় করুন" else "Requires a 4-digit PIN lock on opening",
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            GlassSwitch(
                                                checked = prefAppLockEnabled,
                                                onCheckedChange = { prefAppLockEnabled = it },
                                                activeText = "ON",
                                                inactiveText = "OFF"
                                            )
                                        }

                                        if (prefAppLockEnabled) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = prefAppLockPin,
                                                onValueChange = {
                                                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                                        prefAppLockPin = it
                                                    }
                                                },
                                                label = { Text(if (prefLanguage == "bn") "৪-ডিজিট সিকিউরিটি পিন" else "4-digit Security PIN", color = AppTextColor().copy(alpha = 0.5f)) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                trailingIcon = {
                                                    IconButton(onClick = { pinVisible = !pinVisible }) {
                                                        Icon(
                                                            imageVector = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                            contentDescription = if (pinVisible) "Hide PIN" else "Show PIN",
                                                            tint = AccentCyan
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = AppTextColor(),
                                                    unfocusedTextColor = AppTextColor(),
                                                    focusedBorderColor = AccentCyan,
                                                    unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                                                ),
                                                modifier = Modifier.fillMaxWidth().testTag("app_lock_pin_input")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Category: Recycle Bin (Trash)
                            val deletedHotels by viewModel.deletedHotels.collectAsStateWithLifecycle()
                            val deletedAccounts by viewModel.deletedBankAccounts.collectAsStateWithLifecycle()
                            
                            if (deletedHotels.isNotEmpty() || deletedAccounts.isNotEmpty()) {
                                PixelSettingsGroup(
                                    title = if (prefLanguage == "bn") "রিসাইকেল বিন" else "Recycle Bin (Trash)"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = if (prefLanguage == "bn") "সম্পূর্ণ মুছে ফেলার আগে এখান থেকে যেকোনো তথ্য পুনরুদ্ধার করতে পারবেন।" else "You can restore binned items below or permanently delete them.",
                                            color = AppTextColor().copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        
                                        // Deleted Hotels List
                                        deletedHotels.forEach { hotel ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(AppTextColor().copy(alpha = 0.04f))
                                                    .clickable {
                                                        viewModel.restoreHotel(hotel)
                                                        val msg = if (prefLanguage == "bn") "${hotel.name} পুনরুদ্ধার করা হয়েছে" else "${hotel.name} restored to Hotels"
                                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Restaurant,
                                                    contentDescription = null,
                                                    tint = AppTextColor().copy(alpha = 0.5f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = hotel.name,
                                                        color = AppTextColor(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = if (prefLanguage == "bn") "হোটেল" else "Hotel",
                                                        color = AppTextColor().copy(alpha = 0.5f),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                // Restore Button
                                                IconButton(
                                                    onClick = {
                                                        viewModel.restoreHotel(hotel)
                                                        val msg = if (prefLanguage == "bn") "${hotel.name} পুনরুদ্ধার করা হয়েছে" else "${hotel.name} restored to Hotels"
                                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Restore",
                                                        tint = AccentGreen,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                // Permanent Delete Button
                                                IconButton(
                                                    onClick = { viewModel.deleteHotelPermanently(hotel) }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Permanently",
                                                        tint = AccentPink,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Deleted Accounts List
                                        deletedAccounts.forEach { acc ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(AppTextColor().copy(alpha = 0.04f))
                                                    .clickable {
                                                        viewModel.restoreVirtualAccount(acc)
                                                        val msg = if (prefLanguage == "bn") "${acc.name} পুনরুদ্ধার করা হয়েছে" else "${acc.name} restored to Accounts"
                                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalance,
                                                    contentDescription = null,
                                                    tint = AppTextColor().copy(alpha = 0.5f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = acc.name,
                                                        color = AppTextColor(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = if (prefLanguage == "bn") "ব্যাংক অ্যাকাউন্ট" else "Bank Account",
                                                        color = AppTextColor().copy(alpha = 0.5f),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                // Restore Button
                                                IconButton(
                                                    onClick = {
                                                        viewModel.restoreVirtualAccount(acc)
                                                        val msg = if (prefLanguage == "bn") "${acc.name} পুনরুদ্ধার করা হয়েছে" else "${acc.name} restored to Accounts"
                                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Restore",
                                                        tint = AccentGreen,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                // Permanent Delete Button
                                                IconButton(
                                                    onClick = { viewModel.deleteVirtualAccountPermanently(acc) }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Permanently",
                                                        tint = AccentPink,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Category: About & Legal
                            PixelSettingsGroup(
                                title = if (prefLanguage == "bn") "সম্পর্কে ও আইনি তথ্য" else "About & Legal"
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PixelSettingsItem(
                                        icon = Icons.Default.Info,
                                        iconBgColor = Color(0xFFF3E5F5),
                                        iconTint = Color(0xFF7B1FA2),
                                        title = if (prefLanguage == "bn") "অ্যাপ তথ্য" else "App Information",
                                        subtitle = "Version 1.0.0 (Stable)",
                                        onClick = { showLegalInfoType = "info" }
                                    )
                                    PixelSettingsItem(
                                        icon = Icons.Default.Gavel,
                                        iconBgColor = Color(0xFFEFEBE9),
                                        iconTint = Color(0xFF5D4037),
                                        title = if (prefLanguage == "bn") "লাইসেন্স ও শর্তাবলী" else "Licenses & Terms",
                                        subtitle = "Usage terms & credits",
                                        onClick = { showLegalInfoType = "license" }
                                    )
                                    PixelSettingsItem(
                                        icon = Icons.Default.PrivacyTip,
                                        iconBgColor = Color(0xFFE1F5FE),
                                        iconTint = Color(0xFF0288D1),
                                        title = if (prefLanguage == "bn") "প্রাইভেসি পলিসি" else "Privacy Policy",
                                        subtitle = "Your data safety",
                                        onClick = { showLegalInfoType = "privacy" }
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                // =======================================================
                // ALL DIALOGS (Styled like beautiful Pixel dynamic alerts)
                // =======================================================

                // 1. Edit Profile Dialog
                if (showProfileEditDialog) {
                    var nameInput by remember { mutableStateOf(profileName) }
                    var contactInput by remember { mutableStateOf(profileContact) }

                    AlertDialog(
                        onDismissRequest = { showProfileEditDialog = false },
                        containerColor = getDialogBackground(),
                        shape = RoundedCornerShape(28.dp),
                        title = {
                            Text(
                                text = if (prefLanguage == "bn") "প্রোফাইল সংশোধন" else "Edit Profile Details",
                                color = AppTextColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text(if (prefLanguage == "bn") "ডিসপ্লে নাম" else "Display Name") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = AppTextColor(),
                                        unfocusedTextColor = AppTextColor()
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = contactInput,
                                    onValueChange = { contactInput = it },
                                    label = { Text(if (prefLanguage == "bn") "যোগাযোগ নম্বর" else "Contact Number") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = AppTextColor(),
                                        unfocusedTextColor = AppTextColor()
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (nameInput.isNotBlank()) {
                                        profileName = nameInput
                                        profileContact = contactInput
                                        viewModel.updateProfile(nameInput, contactInput, "avatar_premium")
                                        showProfileEditDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                            ) {
                                Text(if (prefLanguage == "bn") "সংরক্ষণ করুন" else "Save", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showProfileEditDialog = false }) {
                                Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AppTextColor())
                            }
                        }
                    )
                }

                // 2. EditHotelModal (The best editing screen, unified for both Add and Edit with superior features!)
                if (showAddHotelDialog || editingHotel != null) {
                    EditHotelModal(
                        hotel = editingHotel,
                        viewModel = viewModel,
                        onDismiss = {
                            showAddHotelDialog = false
                            editingHotel = null
                        }
                    )
                }

                // 3. Unified Account Dialog (For both Add/Edit with superior features)
                if (showAddAccountDialog || editingAccount != null) {
                    UnifiedAccountDialog(
                        account = editingAccount,
                        prefLanguage = prefLanguage,
                        accountsCount = accounts.size,
                        onDismiss = {
                            showAddAccountDialog = false
                            editingAccount = null
                        },
                        onSave = { name, balance ->
                            if (editingAccount != null) {
                                viewModel.editVirtualAccount(editingAccount!!.copy(name = name, balance = balance))
                            } else {
                                viewModel.addVirtualAccount(name, balance)
                            }
                            showAddAccountDialog = false
                            editingAccount = null
                        },
                        onDelete = if (editingAccount != null) {
                            {
                                viewModel.deleteVirtualAccount(editingAccount!!)
                                editingAccount = null
                            }
                        } else null
                    )
                }

                // 4. Security Verification Dialog (To unlock settings modifications)
                if (showSecurityUnlockDialog) {
                    var currentPinInput by remember { mutableStateOf("") }
                    var currentPinError by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { showSecurityUnlockDialog = false },
                        containerColor = getDialogBackground(),
                        shape = RoundedCornerShape(28.dp),
                        title = {
                            Text(
                                text = if (prefLanguage == "bn") "নিরাপত্তা নিশ্চিতকরণ" else "Security Verification",
                                color = AppTextColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (prefLanguage == "bn") "সিকিউরিটি সেটিংস পরিবর্তন করতে আপনার বর্তমান ৪-সংখ্যার পিনটি লিখুন:" else "Please enter your current 4-digit PIN to modify security settings:",
                                    color = AppTextColor().copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = currentPinInput,
                                    onValueChange = {
                                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                            currentPinInput = it
                                            currentPinError = false
                                        }
                                    },
                                    label = { Text(if (prefLanguage == "bn") "বর্তমান পিন" else "Current PIN") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = PasswordVisualTransformation(),
                                    shape = RoundedCornerShape(12.dp),
                                    isError = currentPinError,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = AppTextColor(),
                                        unfocusedTextColor = AppTextColor()
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (currentPinError) {
                                    Text(
                                        text = if (prefLanguage == "bn") "ভুল পিন! অনুগ্রহ করে আবার চেষ্টা করুন।" else "Incorrect PIN! Please try again.",
                                        color = AccentPink,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (currentPinInput == userProfile?.appLockPin) {
                                        isPinVerifiedForSettings = true
                                        showSecurityUnlockDialog = false
                                    } else {
                                        currentPinError = true
                                        currentPinInput = ""
                                    }
                                }
                            ) {
                                Text(
                                    text = if (prefLanguage == "bn") "যাচাই করুন" else "Verify",
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSecurityUnlockDialog = false }) {
                                Text(
                                    text = if (prefLanguage == "bn") "বাতিল" else "Cancel",
                                    color = AppTextColor().copy(alpha = 0.5f)
                                )
                            }
                        }
                    )
                }

                if (showLegalInfoType != null) {
                    LegalInfoDialog(
                        type = showLegalInfoType!!,
                        prefLanguage = prefLanguage,
                        onDismiss = { showLegalInfoType = null }
                    )
                }

                if (showOwnerProfile) {
                    OwnerProfileScreen(
                        viewModel = viewModel,
                        onDismiss = { showOwnerProfile = false }
                    )
                }
            }
        }
    }
}
}

@Composable
fun PixelSettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = AccentCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun PixelSettingsItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AppTextColor(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = AppTextColor().copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = AppTextColor().copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
}

// ==========================================
// PLACEHOLDER / EMPTY STATE CARD
// ==========================================
@Composable
fun EmptyStateCard(message: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AccentCyan.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                color = AppTextColor().copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun DishImageAndEmoji(
    emoji: String,
    imageUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(0x11FFFFFF), RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(emoji, fontSize = 10.sp)
            }
        } else {
            Text(emoji, fontSize = (size.value * 0.5f).sp)
        }
    }
}


@Composable
fun DishCard(
    dish: com.example.data.model.MenuEntity,
    qty: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdateQty: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 130.dp)
            .bounceClick()
            .background(
                brush = if (isSelected) {
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x4000E5FF), Color(0x1F00E5FF)))
                } else {
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x1FFFFFFF), Color(0x09FFFFFF)))
                },
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) AccentCyan else Color(0x2BFFFFFF),
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                DishImageAndEmoji(
                    emoji = dish.emoji,
                    imageUri = dish.imageUri,
                    size = 44.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dish.banglaName,
                        color = AppTextColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dish.englishName,
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "₹${dish.price.toInt()}",
                    color = AccentGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Quantity Controls
            if (qty == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .clickable { onUpdateQty(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add +", color = AppTextColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .border(0.5.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onUpdateQty(-1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "-", tint = AccentCyan, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "$qty",
                        color = AppTextColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onUpdateQty(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "+", tint = AccentCyan, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedHotelDialog(
    hotel: HotelEntity?, // null for register, non-null for edit
    prefLanguage: String,
    hotelsCount: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, advanced: Double, phone: String, loc: String, upi: String, qr: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var hotelName by remember(hotel) { mutableStateOf(hotel?.name ?: "") }
    var hotelAdv by remember(hotel) { mutableStateOf(hotel?.advancedPayment?.toString() ?: "") }
    var hotelPhone by remember(hotel) { mutableStateOf(hotel?.phoneNumber ?: "") }
    var hotelLoc by remember(hotel) { mutableStateOf(hotel?.location ?: "") }
    var hotelUpi by remember(hotel) { mutableStateOf(hotel?.upiId ?: "") }
    var hotelQr by remember(hotel) { mutableStateOf(hotel?.qrCodeUri ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentCyan.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hotel == null) Icons.Default.Add else Icons.Default.Edit,
                        contentDescription = null,
                        tint = AccentCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (hotel == null) {
                        if (prefLanguage == "bn") "নতুন হোটেল যুক্ত করুন" else "Register Establishment"
                    } else {
                        if (prefLanguage == "bn") "হোটেল সংশোধন" else "Edit Establishment Details"
                    },
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = hotelName,
                    onValueChange = { hotelName = it },
                    label = { Text(if (prefLanguage == "bn") "হোটেলের নাম" else "Hotel Name") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("hotel_name_input")
                )
                OutlinedTextField(
                    value = hotelAdv,
                    onValueChange = { hotelAdv = it },
                    label = { Text(if (prefLanguage == "bn") "অগ্রিম জমা (₹)" else "Advanced Deposit (₹)") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("hotel_adv_input")
                )
                OutlinedTextField(
                    value = hotelPhone,
                    onValueChange = { hotelPhone = it },
                    label = { Text(if (prefLanguage == "bn") "ফোন নম্বর" else "Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("hotel_phone_input")
                )
                OutlinedTextField(
                    value = hotelLoc,
                    onValueChange = { hotelLoc = it },
                    label = { Text(if (prefLanguage == "bn") "অবস্থান / ঠিকানা" else "Location / Address") },
                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("hotel_loc_input")
                )
                OutlinedTextField(
                    value = hotelUpi,
                    onValueChange = { hotelUpi = it },
                    label = { Text(if (prefLanguage == "bn") "UPI আইডি (যেমন- upi@upi)" else "UPI ID (e.g. name@bank)") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("hotel_upi_input")
                )
                OutlinedTextField(
                    value = hotelQr,
                    onValueChange = { hotelQr = it },
                    label = { Text(if (prefLanguage == "bn") "QR কোড ফাইল বা লিঙ্ক" else "QR Code File Path / Link") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("hotel_qr_input")
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hotel != null && hotelsCount > 1 && onDelete != null) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                        modifier = Modifier.testTag("delete_hotel_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (prefLanguage == "bn") "মুছে ফেলুন" else "Delete", color = Color.White)
                    }
                }
                Button(
                    onClick = {
                        if (hotelName.isNotBlank()) {
                            val adv = hotelAdv.toDoubleOrNull() ?: 0.0
                            onSave(hotelName, adv, hotelPhone, hotelLoc, hotelUpi, hotelQr)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier.testTag("save_hotel_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hotel == null) {
                            if (prefLanguage == "bn") "যোগ করুন" else "Register"
                        } else {
                            if (prefLanguage == "bn") "সংরক্ষণ" else "Save"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_hotel_btn")) {
                Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AppTextColor())
            }
        }
    )
}

@Composable
fun UnifiedAccountDialog(
    account: BankAccountEntity?, // null for Create Mode, non-null for Edit Mode
    prefLanguage: String,
    accountsCount: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, balance: Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var accName by remember(account) { mutableStateOf(account?.name ?: "") }
    var accBal by remember(account) { mutableStateOf(account?.balance?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (account == null) Icons.Default.Add else Icons.Default.Edit,
                        contentDescription = null,
                        tint = AccentGreen
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (account == null) {
                        if (prefLanguage == "bn") "নতুন অ্যাকাউন্ট যুক্ত করুন" else "Add Virtual Account"
                    } else {
                        if (prefLanguage == "bn") "অ্যাকাউন্ট সংশোধন" else "Edit Account Details"
                    },
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = accName,
                    onValueChange = { accName = it },
                    label = { Text(if (prefLanguage == "bn") "অ্যাকাউন্টের নাম" else "Account Name") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                )
                OutlinedTextField(
                    value = accBal,
                    onValueChange = { accBal = it },
                    label = {
                        Text(
                            if (account == null) {
                                if (prefLanguage == "bn") "প্রারম্ভিক ব্যালেন্স (₹)" else "Starting Balance (₹)"
                            } else {
                                if (prefLanguage == "bn") "তরল ব্যালেন্স (₹)" else "Liquid Balance (₹)"
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("account_balance_input")
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (account != null && accountsCount > 1 && onDelete != null) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                        modifier = Modifier.testTag("delete_account_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (prefLanguage == "bn") "মুছে ফেলুন" else "Delete", color = Color.White)
                    }
                }
                Button(
                    onClick = {
                        if (accName.isNotBlank()) {
                            val bal = accBal.toDoubleOrNull() ?: 0.0
                            onSave(accName, bal)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.testTag("save_account_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (account == null) {
                            if (prefLanguage == "bn") "যুক্ত করুন" else "Register"
                        } else {
                            if (prefLanguage == "bn") "সংরক্ষণ" else "Save"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_account_btn")) {
                Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AppTextColor())
            }
        }
    )
}

@Composable
fun ExtraExpenseDetailedHistoryScreen(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val expenses by viewModel.allExtraExpenses.collectAsStateWithLifecycle()
    val accounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Int?>(null) }

    val filteredExpenses by remember(expenses, searchQuery, selectedAccountId) {
        derivedStateOf {
            expenses.filter { exp ->
                val matchesSearch = searchQuery.isBlank() || exp.name.contains(searchQuery, ignoreCase = true) || exp.notes.contains(searchQuery, ignoreCase = true)
                val matchesAccount = selectedAccountId == null || exp.bankAccountId == selectedAccountId
                matchesSearch && matchesAccount
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF1E293B)).padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        "Detailed History",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    cornerRadius = 12.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search transactions...", color = Color.White.copy(alpha = 0.4f))
                                }
                                innerTextField()
                            }
                        )
                    }
                }
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedAccountId == null,
                            onClick = { selectedAccountId = null },
                            label = { Text("All Accounts") },
                            colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = AccentCyan)
                        )
                    }
                    items(accounts, key = { it.id }) { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) },
                            colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = AccentCyan)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            items(filteredExpenses, key = { it.id }) { expense ->
                val account = accounts.find { it.id == expense.bankAccountId }
                TransactionItem(
                    expense = expense,
                    accountName = account?.name ?: "Unknown",
                    onDelete = { viewModel.deleteExtraExpense(expense) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun TransactionItem(
    expense: ExtraExpenseEntity,
    accountName: String,
    onDelete: () -> Unit
) {
    val isIncome = expense.type == "INCOME"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    
    GlassCard(cornerRadius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isIncome) Color(0x1F00E676) else Color(0x1FFF4081), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (isIncome) AccentGreen else AccentPink,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.name, fontWeight = FontWeight.Bold, color = AppTextColor())
                Text("$accountName • ${expense.category}", fontSize = 10.sp, color = AppTextColor().copy(alpha = 0.5f))
                Text(sdf.format(Date(expense.timestamp)), fontSize = 10.sp, color = AppTextColor().copy(alpha = 0.4f))
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isIncome) "+" else "-"}₹${expense.amount}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (isIncome) AccentGreen else AccentPink
                )
                
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, null, tint = AppTextColor().copy(alpha = 0.5f))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegalInfoDialog(
    type: String,
    prefLanguage: String,
    onDismiss: () -> Unit
) {
    val title = when (type) {
        "info" -> if (prefLanguage == "bn") "অ্যাপ তথ্য" else "App Information"
        "license" -> if (prefLanguage == "bn") "লাইসেন্স ও শর্তাবলী" else "Licenses & Terms"
        "privacy" -> if (prefLanguage == "bn") "প্রাইভেসি পলিসি" else "Privacy Policy"
        else -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(title, color = AppTextColor(), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val content = when (type) {
                    "info" -> if (prefLanguage == "bn") """
                        FinNexus v1.0.0 (Nexus Finance)
                        
                        Nexus Finance একটি আধুনিক, অফলাইন-ফার্স্ট ব্যক্তিগত আর্থিক ব্যবস্থাপনা অ্যাপ। এটি আপনার খরচ ট্র্যাক করা, একাধিক অ্যাকাউন্ট পরিচালনা এবং মিটারের রিডিং নিখুঁতভাবে পর্যবেক্ষণ করতে সাহায্য করে।
                        
                        Jetpack Compose ও Kotlin দিয়ে তৈরি।
                        © ২০২৬ Nexus দেব টিম।
                    """.trimIndent() else """
                        FinNexus v1.0.0 (Nexus Finance)
                        
                        Nexus Finance is a modern, offline-first personal financial management tool designed for individuals and business owners to track expenses, manage multiple accounts, and monitor meter readings with precision.
                        
                        Developed with Jetpack Compose & Kotlin.
                        © 2026 Nexus Dev Team.
                    """.trimIndent()
                    "license" -> if (prefLanguage == "bn") """
                        এই অ্যাপ্লিকেশনটি Apache License 2.0 এর অধীনে লাইসেন্সপ্রাপ্ত।
                        
                        আপনি লাইসেন্সের শর্তাবলী অনুযায়ী এই সফটওয়্যারটি ব্যবহার, বিতরণ এবং পরিবর্তন করতে পারেন।
                        
                        ব্যবহৃত থার্ড-পার্টি লাইব্রেরি:
                        - Jetpack Compose
                        - Room Database
                        - Kotlin Coroutines
                        - Material Design 3
                    """.trimIndent() else """
                        This application is licensed under the Apache License 2.0.
                        
                        You may use, distribute, and modify this software in accordance with the terms of the license.
                        
                        Third-party libraries used:
                        - Jetpack Compose
                        - Room Database
                        - Kotlin Coroutines
                        - Material Design 3
                    """.trimIndent()
                    "privacy" -> if (prefLanguage == "bn") """
                        আপনার প্রাইভেসি আমাদের কাছে সবচেয়ে গুরুত্বপূর্ণ।
                        
                        FinNexus একটি অফলাইন-ফার্স্ট অ্যাপ্লিকেশন। এর অর্থ:
                        ১. আপনার সমস্ত তথ্য শুধুমাত্র আপনার ফোনেই সংরক্ষিত থাকে।
                        ২. কোনো আর্থিক তথ্য কোনো সার্ভারে পাঠানো হয় না।
                        ৩. আমরা আপনার কোনো ব্যক্তিগত তথ্য সংগ্রহ বা বিক্রি করি না।
                        
                        আপনার তথ্য সম্পূর্ণ আপনার নিয়ন্ত্রণে।
                    """.trimIndent() else """
                        Your privacy is our priority.
                        
                        FinNexus is an OFFLINE-FIRST application. This means:
                        1. All your data is stored locally on your device.
                        2. No financial data is sent to any external server.
                        3. We do not collect or sell your personal information.
                        
                        You are in full control of your data.
                    """.trimIndent()
                    else -> ""
                }
                Text(
                    content,
                    color = AppTextColor().copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (prefLanguage == "bn") "বন্ধ করুন" else "Close",
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun OwnerProfileScreen(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val ownerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile.language
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<com.example.data.model.OwnerAccountEntity?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTextColor()
                        )
                    }
                    Text(
                        text = if (prefLanguage == "bn") "মালিক প্রোফাইল" else "Owner Profile",
                        color = AppTextColor(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .background(AccentCyan.copy(alpha = 0.1f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = AccentCyan)
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            if (ownerAccounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateCard(message = if (prefLanguage == "bn") "কোনো মালিক অ্যাকাউন্ট নেই।" else "No owner accounts added yet.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(ownerAccounts, key = { it.id }) { acc ->
                        OwnerAccountCard(
                            account = acc,
                            onEdit = { accountToEdit = acc; showAddDialog = true },
                            onDelete = { viewModel.deleteOwnerAccount(acc) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddOwnerAccountDialog(
            account = accountToEdit,
            onDismiss = { showAddDialog = false; accountToEdit = null },
            onSave = { newAcc ->
                if (accountToEdit == null) {
                    viewModel.addOwnerAccount(newAcc)
                } else {
                    viewModel.updateOwnerAccount(newAcc)
                }
                showAddDialog = false
                accountToEdit = null
            }
        )
    }
}

@Composable
fun OwnerAccountCard(
    account: com.example.data.model.OwnerAccountEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showQR by remember { mutableStateOf(false) }

    GlassCard(cornerRadius = 24.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(account.ownerName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppTextColor())
                    Text(account.ownerMobile, fontSize = 12.sp, color = AppTextColor().copy(alpha = 0.6f))
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = AccentPink, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = AppTextColor().copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            DetailRow(label = "Bank / Account", value = account.accountName)
            DetailRow(label = "A/C Number", value = account.accountNumber)
            DetailRow(label = "IFSC Code", value = account.ifscCode)
            DetailRow(label = "Branch", value = account.branchName)
            DetailRow(label = "UPI ID", value = account.upiId)
            
            if (account.qrCodeUri.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                GlassButton(
                    onClick = { showQR = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View QR Code", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showQR) {
        Dialog(onDismissRequest = { showQR = false }) {
            GlassCard(cornerRadius = 24.dp, modifier = Modifier.padding(20.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("QR Code for ${account.ownerName}", fontWeight = FontWeight.Bold, color = AppTextColor())
                    Spacer(modifier = Modifier.height(20.dp))
                    AsyncImage(
                        model = account.qrCodeUri,
                        contentDescription = "QR",
                        modifier = Modifier.size(250.dp).clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    GlassButton(onClick = { showQR = false }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = AppTextColor().copy(alpha = 0.5f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTextColor())
    }
}

@Composable
fun AddOwnerAccountDialog(
    account: com.example.data.model.OwnerAccountEntity?,
    onDismiss: () -> Unit,
    onSave: (com.example.data.model.OwnerAccountEntity) -> Unit
) {
    var ownerName by remember { mutableStateOf(account?.ownerName ?: "") }
    var ownerMobile by remember { mutableStateOf(account?.ownerMobile ?: "") }
    var accountName by remember { mutableStateOf(account?.accountName ?: "") }
    var accountNumber by remember { mutableStateOf(account?.accountNumber ?: "") }
    var ifscCode by remember { mutableStateOf(account?.ifscCode ?: "") }
    var branchName by remember { mutableStateOf(account?.branchName ?: "") }
    var upiId by remember { mutableStateOf(account?.upiId ?: "") }
    var qrCodeUri by remember { mutableStateOf(account?.qrCodeUri ?: "") }

    val qrPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { qrCodeUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp),
        title = { Text(if (account == null) "Add Owner Account" else "Edit Owner Account", color = AppTextColor()) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ownerMobile, onValueChange = { ownerMobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("Bank / Account Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("A/C Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ifscCode, onValueChange = { ifscCode = it }, label = { Text("IFSC Code") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = branchName, onValueChange = { branchName = it }, label = { Text("Branch Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text("UPI ID") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = { qrPicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCode, null, tint = AccentCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (qrCodeUri.isEmpty()) "Attach QR Code" else "Change QR Code", color = AccentCyan)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        com.example.data.model.OwnerAccountEntity(
                            id = account?.id ?: 0,
                            ownerName = ownerName,
                            ownerMobile = ownerMobile,
                            accountName = accountName,
                            accountNumber = accountNumber,
                            ifscCode = ifscCode,
                            branchName = branchName,
                            upiId = upiId,
                            qrCodeUri = qrCodeUri
                        )
                    )
                }
            ) {
                Text("Save", color = AccentCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppTextColor().copy(alpha = 0.5f))
            }
        }
    )
}



@Composable
fun GlobalSearchScreen(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getAppBackgroundGradient(LocalAppTheme.current))
            .pointerInput(Unit) { detectTapGestures { } } // Consume taps
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Search Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.setSearchQuery("")
                    onDismiss()
                }) {
                    Icon(Icons.Default.ArrowBack, null, tint = AppTextColor())
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(getDialogBackground().copy(alpha = 0.8f))
                        .border(1.dp, getGlassBorder(), RoundedCornerShape(25.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        singleLine = true,
                        textStyle = TextStyle(color = AppTextColor(), fontSize = 16.sp),
                        cursorBrush = SolidColor(AccentCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search through your app...",
                                    color = AppTextColor().copy(alpha = 0.4f),
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, null, tint = AppTextColor().copy(alpha = 0.5f))
                    }
                }
            }

            // Results List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (searchQuery.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Search,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = AppTextColor().copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Type something to search",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (results.isEmpty()) {
                    item {
                        Text(
                            "No results found for \"$searchQuery\"",
                            color = AppTextColor().copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(results, key = { "${it.type}_${it.id}" }) { result ->
                        SearchItem(result) {
                            when (result.type) {
                                "Hotel" -> {
                                    viewModel.activeHotelId.value = result.id
                                    viewModel.activeTab.value = "Hotels"
                                }
                                "Expense" -> {
                                    viewModel.activeTab.value = "Extras"
                                }
                                "Account" -> {
                                    // Switch to a relevant tab or show feedback
                                    // Accounts are currently in settings, so maybe just closing search is enough
                                }
                            }
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchItem(result: SearchResult, onClick: () -> Unit) {
    val (icon, color) = when (result.type) {
        "Hotel" -> Icons.Default.Hotel to Color(0xFF64B5F6)
        "Expense" -> Icons.Default.ReceiptLong to Color(0xFFF06292)
        "Account" -> Icons.Default.AccountBalance to Color(0xFF4DB6AC)
        else -> Icons.Default.Info to Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(getDialogBackground().copy(alpha = 0.5f))
            .border(0.5.dp, getGlassBorder(), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(result.title, color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(result.subtitle, color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
                Text(
                    result.type.uppercase(),
                    color = color.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
