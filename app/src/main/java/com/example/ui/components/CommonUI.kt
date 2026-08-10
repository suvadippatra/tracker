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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import java.util.Locale

val LocalAppTheme = compositionLocalOf { "Auto" }
val LocalAppBgPreset = compositionLocalOf { "Default" }
val LocalAppCustomBgUri = compositionLocalOf { "" }

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
        Color(0xFFFFCC80)
    )
)

val MidnightBlueGradient = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0284C7)))
val EmeraldDarkGradient = Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF022C22), Color(0xFF0F766E)))
val SunsetWarmGradient = Brush.verticalGradient(listOf(Color(0xFF451A03), Color(0xFF78350F), Color(0xFF9A3412)))
val PurpleTwilightGradient = Brush.verticalGradient(listOf(Color(0xFF2E1065), Color(0xFF3B0764), Color(0xFF581C87)))
val SoftMinimalLightGradient = Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE2E8F0)))

@Composable
fun isAppLightMode(): Boolean {
    val bgPreset = LocalAppBgPreset.current
    return when (bgPreset) {
        "Soft Minimal Light" -> true
        "Midnight Blue", "Emerald Dark", "Sunset Warm", "Purple Twilight" -> false
        else -> {
            val themeMode = LocalAppTheme.current
            if (themeMode == "Auto") !isSystemInDarkTheme() else themeMode == "Light"
        }
    }
}

@Composable
fun AppTextColor(): Color {
    return if (isAppLightMode()) Color(0xFF0F172A) else Color(0xFFFFFFFF)
}

@Composable
fun getAppBackgroundGradient(
    themeMode: String = LocalAppTheme.current,
    bgPreset: String = LocalAppBgPreset.current
): Brush {
    return when (bgPreset) {
        "Midnight Blue" -> MidnightBlueGradient
        "Emerald Dark" -> EmeraldDarkGradient
        "Sunset Warm" -> SunsetWarmGradient
        "Purple Twilight" -> PurpleTwilightGradient
        "Soft Minimal Light" -> SoftMinimalLightGradient
        else -> {
            val isLight = if (themeMode == "Auto") !isSystemInDarkTheme() else themeMode == "Light"
            if (isLight) LightBackgroundGradient else DarkBackgroundGradient
        }
    }
}

@Composable
fun getDialogBackground(): Color {
    return if (isAppLightMode()) Color(0xF8F8FAFC) else Color(0xFF140D2D)
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
    containerAlpha: Float = 1f,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isLight = isAppLightMode()
    val bgColors = if (isLight) {
        listOf(
            Color.White.copy(alpha = (0.94f * containerAlpha).coerceIn(0f, 1f)),
            Color.White.copy(alpha = (0.88f * containerAlpha).coerceIn(0f, 1f))
        )
    } else {
        val baseDark1 = Color(0xFF120C28).copy(alpha = (0.82f * containerAlpha).coerceIn(0f, 1f))
        val baseDark2 = Color(0xFF1C133A).copy(alpha = (0.75f * containerAlpha).coerceIn(0f, 1f))
        listOf(baseDark1, baseDark2)
    }
    val borderColors = if (isLight) {
        listOf(Color(0xFFFFFFFF), Color(0x4D000000))
    } else {
        listOf(GlassBorderTop, GlassBorderBottom)
    }

    Box(
        modifier = modifier
            .slideInOnScroll()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh
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
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun Modifier.slideInOnScroll(): Modifier {
    return this
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val isLight = isAppLightMode()
    
    val bgColor = if (isLight) {
        Color.Black.copy(alpha = 0.03f)
    } else {
        Color.White.copy(alpha = 0.05f)
    }
    
    val borderBrush = if (isError) {
        SolidColor(AccentPink)
    } else if (isFocused) {
        Brush.sweepGradient(listOf(AccentCyan, AccentCyan.copy(alpha = 0.6f)))
    } else {
        Brush.verticalGradient(
            colors = if (isLight) listOf(Color(0x33000000), Color(0x13000000))
                     else listOf(GlassBorderTop, GlassBorderBottom)
        )
    }
    
    val borderWidth = if (isError || isFocused) 1.5.dp else 1.dp
    
    Column(modifier = modifier) {
        if (label != null) {
            Box(modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)) {
                CompositionLocalProvider(
                    LocalTextStyle provides TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextColor().copy(alpha = 0.7f)
                    )
                ) {
                    label()
                }
            }
        }
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = bgColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = borderWidth,
                    brush = borderBrush,
                    shape = RoundedCornerShape(12.dp)
                )
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(
                color = AppTextColor(),
                fontSize = 15.sp
            ),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            enabled = enabled,
            readOnly = readOnly,
            cursorBrush = SolidColor(if (isLight) Color.Black else Color.White),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        leadingIcon()
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            CompositionLocalProvider(
                                LocalTextStyle provides TextStyle(
                                    color = AppTextColor().copy(alpha = 0.4f),
                                    fontSize = 15.sp
                                )
                            ) {
                                placeholder()
                            }
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        trailingIcon()
                    }
                }
            }
        )
    }
}


@Composable
fun ConfirmDeleteDialog(
    itemName: String,
    requireTypedConfirmation: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var typedText by remember { mutableStateOf("") }
    val isConfirmed = !requireTypedConfirmation || typedText.equals("delete", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (requireTypedConfirmation) "Critical Delete" else "Confirm Delete") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Are you sure you want to delete '$itemName'? It will be moved to the Recycle Bin.")
                if (requireTypedConfirmation) {
                    Text("Type 'delete' below to confirm.", color = AccentPink)
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { typedText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isConfirmed) {
                        onConfirm()
                        onDismiss()
                    }
                },
                enabled = isConfirmed,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPink)
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FullScreenQRPreviewDialog(
    qrUri: String,
    title: String = "",
    upiId: String = "",
    onDismiss: () -> Unit
) {
    if (qrUri.isBlank()) return

    androidx.activity.compose.BackHandler { onDismiss() }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .clickable(enabled = false) {}
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (upiId.isNotBlank()) {
                            Text(
                                text = "UPI: $upiId",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = qrUri,
                        contentDescription = "Full Screen QR Code",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Tap anywhere or press back to close",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
