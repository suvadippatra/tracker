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
    val isLockedManual by viewModel.isLockedManual.collectAsStateWithLifecycle()
    var isUnlocked by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var initialLockChecked by remember { mutableStateOf(false) }

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
        if (isInitialized) {
            if (!userProfile.appLockEnabled) {
                isUnlocked = true
                viewModel.unlockApp()
            } else if (!initialLockChecked) {
                isUnlocked = false
                initialLockChecked = true
            }
        }
    }

    val showLockScreen = isInitialized && userProfile.appLockEnabled && (!isUnlocked || isLockedManual)

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val customFontScale = when (userProfile.textSize) {
        "Small" -> 0.85f
        "Large" -> 1.15f
        else -> 1.0f
    }

    CompositionLocalProvider(
        LocalAppTheme provides userProfile.themeMode,
        LocalAppBgPreset provides userProfile.bgPreset,
        LocalAppCustomBgUri provides userProfile.customBgUri,
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
            density = currentDensity.density,
            fontScale = customFontScale
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient())
        ) {
            if (userProfile.customBgUri.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = userProfile.customBgUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isAppLightMode()) Color.White.copy(alpha = 0.40f)
                            else Color.Black.copy(alpha = 0.50f)
                        )
                )
            }
            if (userProfile.enableStarAnimation && userProfile.customBgUri.isBlank()) { MovingStarsBackground(isLight = isAppLightMode()) }
            if (!isInitialized) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(42.dp)
                    )
                }
            } else if (showLockScreen) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppLockScreen(
                        correctPin = userProfile.appLockPin,
                        language = userProfile.language,
                        userName = userProfile.name,
                        biometricEnabled = userProfile.biometricEnabled,
                        securityQuestion = userProfile.securityQuestion,
                        securityAnswer = userProfile.securityAnswer,
                        onUnlock = {
                            isUnlocked = true
                            initialLockChecked = true
                            viewModel.unlockApp()
                        }
                    )
                }
            } else {
                MainAppLayout(viewModel)
            }
        }
    }
}


// smooth number roll
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
