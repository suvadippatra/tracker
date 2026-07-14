package com.example.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

val LocalAppTheme = compositionLocalOf { "Auto" }

// Glassmorphism design tokens
val GlassBackground = Color(0x26FFFFFF)
val GlassBorderTop = Color(0x66FFFFFF)
val GlassBorderBottom = Color(0x13FFFFFF)

// Accent Colors
val AccentGold = Color(0xFFD84315)
val AccentCyan = Color(0xFF0091EA)
val AccentPink = Color(0xFFC2185B)
val AccentGreen = Color(0xFF00B0FF)

// Backdrop Gradients
val DarkBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF070414),
        Color(0xFF0F0B26),
        Color(0xFF1B0E3D),
        Color(0xFF2C0F4A)
    )
)

val LightBackgroundGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF81D4FA),
        Color(0xFFCE93D8),
        Color(0xFFFFFFCC80)
    )
)

@Composable
fun isAppLightMode(): Boolean {
    val themeMode = LocalAppTheme.current
    return if (themeMode == "Auto") !isSystemInDarkTheme() else themeMode == "Light"
}

@Composable
fun AppTextColor(): Color {
    return if (isAppLightMode()) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
}

@Composable
fun getAppBackgroundGradient(themeMode: String): Brush {
    val isLight = if (themeMode == "Auto") !isSystemInDarkTheme() else themeMode == "Light"
    return if (isLight) LightBackgroundGradient else DarkBackgroundGradient
}

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
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val isLight = isAppLightMode()
    val bgColors = if (isLight) {
        listOf(Color(0xB3FFFFFF), Color(0x80FFFFFF))
    } else {
        listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))
    }
    val borderColors = if (isLight) {
        listOf(Color(0xFFFFFFFF), Color(0x4D000000))
    } else {
        listOf(GlassBorderTop, GlassBorderBottom)
    }

    Box(
        modifier = modifier
            .slideInOnScroll()
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
                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
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

@Composable
fun AttachmentActionChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(19.dp))
            .clickable { onClick() }
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
fun EmptyStateCard(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = AppTextColor().copy(alpha = 0.1f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = AppTextColor().copy(alpha = 0.5f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Modifier.slideInOnScroll(): Modifier {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "slideIn"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "fadeIn"
    )
    return this.graphicsLayer {
        translationY = offsetY
        this.alpha = alpha
    }
}
