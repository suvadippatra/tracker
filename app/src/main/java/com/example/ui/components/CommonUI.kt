package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import java.util.Locale

val LocalAppTheme = compositionLocalOf { "Auto" }
val LocalAppBgPreset = compositionLocalOf { "Default" }
val LocalAppCustomBgUri = compositionLocalOf { "" }
val LocalAppLiquidGlassTheme = compositionLocalOf { false }
val LocalDisableHeavyEffects = compositionLocalOf { true }
val LocalBoldTextEnabled = compositionLocalOf { false }
val LocalStickyCardWidthPercent = compositionLocalOf { 80 }
val LocalStickyPreviewLines = compositionLocalOf { 10 }
val LocalBottomBarInset = compositionLocalOf { 0.dp }

@Composable
fun isHeavyEffectsDisabled(): Boolean = LocalDisableHeavyEffects.current

@Composable
fun isLiquidGlassEnabled(): Boolean = false

// Glassmorphism design tokens (retained for binary compatibility, mapped to M3 surfaces)
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
        Color(0xFF000000),
        Color(0xFF000000),
        Color(0xFF050505),
        Color(0xFF0D0D0D)
    )
)

val LightBackgroundGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFF8FAFC),
        Color(0xFFF1F5F9),
        Color(0xFFE2E8F0)
    )
)

val MidnightBlueGradient = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0284C7)))
val EmeraldDarkGradient = Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF022C22), Color(0xFF0F766E)))
val SunsetWarmGradient = Brush.verticalGradient(listOf(Color(0xFF451A03), Color(0xFF78350F), Color(0xFF9A3412)))
val PurpleTwilightGradient = Brush.verticalGradient(listOf(Color(0xFF2E1065), Color(0xFF3B0764), Color(0xFF581C87)))
val SoftMinimalLightGradient = Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE2E8F0)))

@Composable
fun isAppLightMode(): Boolean {
    val themeMode = LocalAppTheme.current
    return if (themeMode == "Auto") !isSystemInDarkTheme() else themeMode == "Light"
}

fun getOnColorForBackground(bg: Color): Color {
    return if (bg.luminance() > 0.55f) Color(0xFF0F172A) else Color.White
}

@Composable
fun AppOnAccentColor(accent: Color = AppAccentColor()): Color {
    return remember(accent) { getOnColorForBackground(accent) }
}

@Composable
fun AppAccentColor(): Color {
    val bgPreset = LocalAppBgPreset.current
    val isLight = isAppLightMode()
    return remember(bgPreset, isLight) {
        com.example.ui.theme.getAppAccentColor(bgPreset, false, isLight)
    }
}

@Composable
fun AppTextColor(): Color {
    return if (isAppLightMode()) Color(0xFF0F172A) else Color(0xFFFFFFFF)
}

@Composable
fun AppSecondaryTextColor(): Color {
    return if (isAppLightMode()) Color(0xFF334155) else Color(0xFFCBD5E1)
}

@Composable
fun AppTertiaryTextColor(): Color {
    return if (isAppLightMode()) Color(0xFF475569) else Color(0xFF94A3B8)
}

@Composable
fun AppHeadingColor(): Color {
    return if (isAppLightMode()) Color(0xFF0F172A) else Color(0xFFFFFFFF)
}

@Composable
fun AppPrimaryBlueColor(): Color {
    return AppAccentColor()
}

@Composable
fun AppPaidTextColor(): Color {
    return if (isAppLightMode()) com.example.ui.theme.HighContrastPaidTextLight else com.example.ui.theme.HighContrastPaidTextDark
}

@Composable
fun AppPaidBgColor(): Color {
    return if (isAppLightMode()) com.example.ui.theme.HighContrastPaidBgLight else com.example.ui.theme.HighContrastPaidBgDark
}

@Composable
fun AppPaidBorderColor(): Color {
    return if (isAppLightMode()) com.example.ui.theme.HighContrastPaidBorderLight else com.example.ui.theme.HighContrastPaidBorderDark
}

@Composable
fun AppUnpaidTextColor(): Color {
    return if (isAppLightMode()) com.example.ui.theme.HighContrastUnpaidTextLight else com.example.ui.theme.HighContrastUnpaidTextDark
}

@Composable
fun AppUnpaidBgColor(): Color {
    return if (isAppLightMode()) com.example.ui.theme.HighContrastUnpaidBgLight else com.example.ui.theme.HighContrastUnpaidBgDark
}

@Composable
fun AppUnpaidBorderColor(): Color {
    return if (isAppLightMode()) com.example.ui.theme.HighContrastUnpaidBorderLight else com.example.ui.theme.HighContrastUnpaidBorderDark
}

@Composable
fun getAppBackgroundGradient(
    themeMode: String = LocalAppTheme.current,
    bgPreset: String = LocalAppBgPreset.current
): Brush {
    if (!isLiquidGlassEnabled()) {
        val isLight = if (themeMode == "Auto") !isSystemInDarkTheme() else themeMode == "Light"
        return SolidColor(if (isLight) com.example.ui.theme.M3BackgroundLight else com.example.ui.theme.M3BackgroundDark)
    }
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
    if (!isLiquidGlassEnabled()) {
        return if (isAppLightMode()) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark
    }
    val bgPreset = LocalAppBgPreset.current
    return when (bgPreset) {
        "Emerald Dark" -> Color(0xFF042F2E).copy(alpha = 0.98f)
        "Midnight Blue" -> Color(0xFF0F172A).copy(alpha = 0.98f)
        "Sunset Warm" -> Color(0xFF2B1104).copy(alpha = 0.98f)
        "Purple Twilight" -> Color(0xFF1B0A33).copy(alpha = 0.98f)
        "Soft Minimal Light" -> Color(0xFFFFFFFF).copy(alpha = 0.98f)
        else -> if (isAppLightMode()) Color(0xFFF8FAFC).copy(alpha = 0.96f) else Color(0xFF121620).copy(alpha = 0.96f)
    }
}

@Composable
fun getSettingsItemBg(): Color {
    if (!isLiquidGlassEnabled()) {
        return if (isAppLightMode()) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark
    }
    val bgPreset = LocalAppBgPreset.current
    return when (bgPreset) {
        "Emerald Dark" -> Color(0xFF064E3B).copy(alpha = 0.45f)
        "Midnight Blue" -> Color(0xFF1E293B).copy(alpha = 0.50f)
        "Sunset Warm" -> Color(0xFF451A03).copy(alpha = 0.45f)
        "Purple Twilight" -> Color(0xFF2E1065).copy(alpha = 0.45f)
        "Soft Minimal Light" -> Color.White.copy(alpha = 0.65f)
        else -> if (isAppLightMode()) Color.White.copy(alpha = 0.45f) else Color(0xFF1A2234).copy(alpha = 0.50f)
    }
}

@Composable
fun getSettingsItemBorder(): Color {
    return Color.Transparent
}

@Composable
fun getCardBackground(): Color {
    if (!isLiquidGlassEnabled()) {
        return if (isAppLightMode()) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark
    }
    val bgPreset = LocalAppBgPreset.current
    return when (bgPreset) {
        "Emerald Dark" -> Color(0xFF042F2E).copy(alpha = 0.60f)
        "Midnight Blue" -> Color(0xFF0F172A).copy(alpha = 0.60f)
        "Sunset Warm" -> Color(0xFF2B1104).copy(alpha = 0.60f)
        "Purple Twilight" -> Color(0xFF1B0A33).copy(alpha = 0.60f)
        "Soft Minimal Light" -> Color.White.copy(alpha = 0.65f)
        else -> if (isAppLightMode()) Color.White.copy(alpha = 0.50f) else Color(0xFF161B26).copy(alpha = 0.60f)
    }
}

@Composable
fun getGlassBorder(): Color {
    return Color.Transparent
}

@Composable
fun getGlassIconButtonBg(): Color {
    return if (isAppLightMode()) Color(0xFFE8EEF5) else Color(0xFF1E293B)
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
    val fastMode = isHeavyEffectsDisabled() || Build.VERSION.SDK_INT < Build.VERSION_CODES.P
    val shape = RoundedCornerShape(cornerRadius)
    val cardBg = if (isLight) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark

    val baseMod = if (fastMode) {
        val lightBorder = border ?: BorderStroke(
            0.75.dp,
            if (isLight) Color(0x140F172A) else Color(0x22FFFFFF)
        )
        modifier
            .clip(shape)
            .background(cardBg)
            .border(lightBorder, shape)
    } else {
        val shadowElev = if (isLight) 4.dp else 6.dp
        val ambientShadow = if (isLight) Color(0x1F0F172A) else Color(0x66000000)
        val spotShadow = if (isLight) Color(0x290F172A) else Color(0x88000000)
        modifier
            .shadow(
                elevation = shadowElev,
                shape = shape,
                clip = true,
                ambientColor = ambientShadow,
                spotColor = spotShadow
            )
            .background(cardBg)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
    }

    Box(
        modifier = baseMod.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

fun Modifier.UARTShadow(
    elevation: Dp,
    shape: androidx.compose.ui.graphics.Shape,
    ambientColor: Color,
    spotColor: Color
): Modifier = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
    this
} else {
    this.shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = ambientColor,
        spotColor = spotColor
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    borderBrush: Brush? = null,
    content: @Composable RowScope.() -> Unit
) {
    val isLight = isAppLightMode()
    val fastMode = isHeavyEffectsDisabled() || Build.VERSION.SDK_INT < Build.VERSION_CODES.P
    val shape = RoundedCornerShape(cornerRadius)
    val buttonBg = if (isLight) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark

    val btnMod = if (fastMode) {
        modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(shape)
            .background(buttonBg)
    } else {
        val ambientShadow = if (isLight) Color(0x180F172A) else Color(0x55000000)
        val spotShadow = if (isLight) Color(0x220F172A) else Color(0x77000000)
        modifier
            .defaultMinSize(minHeight = 44.dp)
            .shadow(
                elevation = if (isLight) 2.5.dp else 4.dp,
                shape = shape,
                clip = true,
                ambientColor = ambientShadow,
                spotColor = spotShadow
            )
            .background(buttonBg)
    }

    Row(
        modifier = btnMod
            .clickable(onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

fun Modifier.bounceClick(scaleDown: Float = 0.96f): Modifier = this

@Composable
fun AttachmentActionChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val isLight = isAppLightMode()
    val shape = RoundedCornerShape(19.dp)
    val opaqueBg = if (isLight) Color(0xFFEFF4F9) else Color(0xFF1E293B)
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(shape)
            .background(opaqueBg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = AppAccentColor()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                color = AppTextColor(),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
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
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    interactionSource: MutableInteractionSource? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val isLight = isAppLightMode()
    val isBold = LocalBoldTextEnabled.current
    val activeFontFamily = if (isBold) com.example.ui.theme.AppBoldFontFamily else com.example.ui.theme.AppFontFamily
    val fieldShape = RoundedCornerShape(16.dp)

    val bgColor = if (isLight) {
        if (isFocused) Color(0xFFFFFFFF) else Color(0xFFEFF4F9)
    } else {
        if (isFocused) Color(0xFF28313E) else com.example.ui.theme.M3SurfaceHighDark
    }

    val borderBrush: Brush = if (isError) {
        SolidColor(AccentPink)
    } else if (isFocused) {
        SolidColor(AppAccentColor())
    } else {
        SolidColor(Color.Transparent)
    }

    val borderWidth = if (isError || isFocused) 1.5.dp else 0.dp

    Column(modifier = modifier) {
        if (label != null) {
            Box(modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)) {
                CompositionLocalProvider(
                    LocalTextStyle provides TextStyle(
                        fontSize = 13.sp,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
                        fontFamily = activeFontFamily,
                        color = AppSecondaryTextColor()
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
                .clip(fieldShape)
                .background(color = bgColor)
                .then(
                    if (borderWidth > 0.dp) {
                        Modifier.border(
                            width = borderWidth,
                            brush = borderBrush,
                            shape = fieldShape
                        )
                    } else Modifier
                )
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(
                color = AppTextColor(),
                fontSize = 15.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontFamily = activeFontFamily
            ),
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            enabled = enabled,
            readOnly = readOnly,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() },
            cursorBrush = SolidColor(if (isLight) Color.Black else Color.White),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        leadingIcon()
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder != null) {
                            CompositionLocalProvider(
                                LocalTextStyle provides TextStyle(
                                    color = AppSecondaryTextColor().copy(alpha = 0.75f),
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
    requireTypedConfirmation: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var typedText by remember { mutableStateOf("") }
    val isConfirmed = typedText.equals("delete", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        titleContentColor = AppTextColor(),
        textContentColor = AppTextColor(),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Confirm Delete",
                fontWeight = FontWeight.Bold,
                color = AppTextColor()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Are you sure you want to delete '$itemName'? It will be moved to the Recycle Bin.",
                    color = AppTextColor()
                )
                if (requireTypedConfirmation) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Type 'delete' below to confirm.", color = AccentPink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    GlassTextField(
                        value = typedText,
                        onValueChange = { typedText = it },
                        placeholder = { Text("type 'delete'") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isConfirmed || !requireTypedConfirmation) {
                        onConfirm()
                        onDismiss()
                    }
                },
                enabled = isConfirmed || !requireTypedConfirmation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPink,
                    disabledContainerColor = AccentPink.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = AppSecondaryTextColor())
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
        com.example.ui.components.DialogEdgeToEdge()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
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
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val qrSide = remember(maxWidth, maxHeight) {
                        minOf(maxWidth * 0.88f, maxHeight * 0.92f).coerceIn(140.dp, 520.dp)
                    }
                    Surface(
                        modifier = Modifier.size(qrSide),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
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
                    }
                }

                Text(
                    text = "Tap anywhere or press back to close",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

fun copyUriToPersistentFile(
    context: android.content.Context,
    uri: android.net.Uri,
    prefix: String = "QR",
    maxBytes: Long = 2L * 1024L * 1024L,
    maxDimension: Int = 1920
): String {
    try {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: ""
        var originalSize = 0L
        runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                originalSize = pfd.statSize.coerceAtLeast(0L)
            }
        }
        val isLikelyImage = mime.startsWith("image/", ignoreCase = true) ||
            prefix.equals("IMG", ignoreCase = true) ||
            prefix.equals("AVATAR", ignoreCase = true) ||
            prefix.equals("PAY", ignoreCase = true) ||
            prefix.equals("QR", ignoreCase = true) ||
            uri.toString().lowercase(Locale.ROOT).let {
                it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp")
            }

        if (isLikelyImage && (originalSize > maxBytes || prefix.equals("AVATAR", ignoreCase = true) || originalSize == 0L)) {
            val boundsOpts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, boundsOpts) }
            if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                // If file is already under maxBytes and not an avatar and dimensions are reasonable, copy directly
                if (originalSize in 1..maxBytes && !prefix.equals("AVATAR", ignoreCase = true) &&
                    boundsOpts.outWidth <= maxDimension && boundsOpts.outHeight <= maxDimension
                ) {
                    val ext = if (mime.contains("png", true)) "png" else "jpg"
                    val directFile = java.io.File(context.filesDir, "${prefix}_${System.currentTimeMillis()}.$ext")
                    resolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(directFile).use { output -> input.copyTo(output) }
                    }
                    return android.net.Uri.fromFile(directFile).toString()
                }

                var sampleSize = 1
                val targetMaxDim = if (prefix.equals("AVATAR", ignoreCase = true)) 720 else maxDimension
                while ((boundsOpts.outWidth / sampleSize) > targetMaxDim * 2 ||
                    (boundsOpts.outHeight / sampleSize) > targetMaxDim * 2
                ) {
                    sampleSize *= 2
                }
                val decodeOpts = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                }
                val decoded = resolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, decodeOpts)
                }
                if (decoded != null) {
                    val w = decoded.width
                    val h = decoded.height
                    val scaled = if (w > targetMaxDim || h > targetMaxDim) {
                        val ratio = w.toFloat() / h.toFloat()
                        val nw = if (w >= h) targetMaxDim else (targetMaxDim * ratio).toInt().coerceAtLeast(1)
                        val nh = if (h > w) targetMaxDim else (targetMaxDim / ratio).toInt().coerceAtLeast(1)
                        android.graphics.Bitmap.createScaledBitmap(decoded, nw, nh, true)
                    } else decoded

                    val outFile = java.io.File(context.filesDir, "${prefix}_${System.currentTimeMillis()}.jpg")
                    var quality = if (prefix.equals("AVATAR", ignoreCase = true)) 82 else 86
                    var bytes: ByteArray
                    do {
                        val baos = java.io.ByteArrayOutputStream()
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
                        bytes = baos.toByteArray()
                        quality -= 12
                    } while (bytes.size > maxBytes && quality >= 35)

                    java.io.FileOutputStream(outFile).use { it.write(bytes) }
                    if (scaled !== decoded) scaled.recycle()
                    decoded.recycle()
                    return android.net.Uri.fromFile(outFile).toString()
                }
            }
        }

        val inputStream = resolver.openInputStream(uri) ?: return uri.toString()
        val ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
        val file = java.io.File(context.filesDir, "${prefix}_${System.currentTimeMillis()}.$ext")
        java.io.FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        return android.net.Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        return uri.toString()
    }
}

@Composable
fun UnsavedChangesWarningDialog(
    isBengali: Boolean = false,
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        containerColor = getDialogBackground(),
        titleContentColor = AppTextColor(),
        textContentColor = AppTextColor(),
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = if (isBengali) "অসংরক্ষিত পরিবর্তন!" else "Unsaved Changes",
                fontWeight = FontWeight.Bold,
                color = AppTextColor(),
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = if (isBengali)
                    "আপনি কিছু তথ্য পরিবর্তন করেছেন যা এখনও সেভ করা হয়নি। আপনি কি সেভ না করেই ফিরে যেতে চান?"
                else
                    "You have unsaved changes. Are you sure you want to go back and discard your edits?",
                color = AppSecondaryTextColor(),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDiscard,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPink,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isBengali) "পরিবর্তন বাতিল করুন" else "Discard Changes",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onKeepEditing,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isBengali) "এডিট চালিয়ে যান" else "Keep Editing",
                    color = AppAccentColor(),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun FullScreenImagePreviewDialog(
    imageUri: String,
    title: String = "",
    onOpenExternal: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (imageUri.isBlank()) return
    androidx.activity.compose.BackHandler { onDismiss() }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            coil.compose.AsyncImage(
                model = imageUri,
                contentDescription = title.ifBlank { "Image Preview" },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .dialogSafeTopPadding(extraTop = 8.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.ifBlank { "Tap anywhere to close" },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (onOpenExternal != null) {
                    TextButton(onClick = onOpenExternal) {
                        Text("Open App", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

fun evaluateCalculatorExpression(rawExpr: String): String {
    val clean = rawExpr
        .replace("₹", "")
        .replace(",", "")
        .replace("×", "*")
        .replace("x", "*")
        .replace("X", "*")
        .replace("÷", "/")
        .replace(" ", "")
        .trim()
    if (clean.isEmpty()) return ""
    return try {
        val tokens = mutableListOf<String>()
        val numBuf = StringBuilder()
        for (i in clean.indices) {
            val c = clean[i]
            if (c.isDigit() || c == '.') {
                numBuf.append(c)
            } else if (c in listOf('+', '-', '*', '/')) {
                if (c == '-' && numBuf.isEmpty() && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/"))) {
                    numBuf.append(c)
                } else {
                    if (numBuf.isNotEmpty()) {
                        tokens.add(numBuf.toString())
                        numBuf.setLength(0)
                    }
                    if (tokens.isNotEmpty() && tokens.last() !in listOf("+", "-", "*", "/")) {
                        tokens.add(c.toString())
                    }
                }
            }
        }
        if (numBuf.isNotEmpty()) tokens.add(numBuf.toString())
        while (tokens.isNotEmpty() && tokens.last() in listOf("+", "-", "*", "/")) {
            tokens.removeAt(tokens.lastIndex)
        }
        if (tokens.isEmpty()) return ""

        // Pass 1: * and /
        val pass1 = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val tk = tokens[i]
            if ((tk == "*" || tk == "/") && pass1.isNotEmpty() && i + 1 < tokens.size) {
                val left = pass1.removeAt(pass1.lastIndex).toDoubleOrNull() ?: 0.0
                val right = tokens[i + 1].toDoubleOrNull() ?: 1.0
                val res = if (tk == "*") left * right else if (right != 0.0) left / right else left
                pass1.add(res.toString())
                i += 2
            } else {
                pass1.add(tk)
                i++
            }
        }

        // Pass 2: + and -
        var total = pass1.firstOrNull()?.toDoubleOrNull() ?: 0.0
        var j = 1
        while (j + 1 < pass1.size) {
            val op = pass1[j]
            val right = pass1[j + 1].toDoubleOrNull() ?: 0.0
            if (op == "+") total += right
            else if (op == "-") total -= right
            j += 2
        }

        if (total.isNaN() || total.isInfinite()) return rawExpr
        val rounded = kotlin.math.round(total * 100.0) / 100.0
        if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
    } catch (_: Exception) {
        rawExpr
    }
}

@Composable
fun OnScreenCalculatorKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    onEnter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLight = isAppLightMode()
    val accent = AppAccentColor()
    val keyBg = if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val opBg = accent.copy(alpha = if (isLight) 0.14f else 0.22f)
    val textColor = AppTextColor()

    fun appendToken(token: String) {
        val operators = listOf("+", "-", "×", "÷")
        if (token in operators) {
            if (value.isBlank()) {
                if (token == "-") onValueChange("-")
                return
            }
            val lastChar = value.last().toString()
            if (lastChar in operators) {
                onValueChange(value.dropLast(1) + token)
            } else {
                onValueChange(value + token)
            }
        } else if (token == ".") {
            val lastNumberSegment = value.split("+", "-", "×", "÷").lastOrNull() ?: ""
            if (!lastNumberSegment.contains(".")) {
                onValueChange(if (lastNumberSegment.isEmpty()) "${value}0." else "$value.")
            }
        } else {
            onValueChange(value + token)
        }
    }

    val rows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf("0", "00", ".", "+")
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF111827))
            .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preview evaluated result if expression contains operators
            val hasOperator = remember(value) {
                value.drop(1).any { it == '+' || it == '-' || it == '×' || it == '÷' || it == '*' || it == '/' }
            }
            if (hasOperator) {
                val preview = remember(value) { evaluateCalculatorExpression(value) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "= ₹$preview",
                        color = accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowKeys.forEach { keyLabel ->
                        val isOp = keyLabel == "+" || keyLabel == "-" || keyLabel == "×" || keyLabel == "÷"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isOp) opBg else keyBg)
                                .clickable { appendToken(keyLabel) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = keyLabel,
                                color = if (isOp) accent else textColor,
                                fontSize = if (isOp) 18.sp else 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom action row: Clear (C), Edit (Backspace), and Enter (= / Confirm)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentPink.copy(alpha = 0.14f))
                        .clickable { onValueChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "C",
                        color = AccentPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(keyBg)
                        .clickable {
                            if (value.isNotEmpty()) {
                                onValueChange(value.dropLast(1))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Edit ⌫",
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent)
                        .clickable {
                            val evaluated = evaluateCalculatorExpression(value)
                            onValueChange(evaluated)
                            onEnter(evaluated)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Enter ↵",
                        color = AppOnAccentColor(accent),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}


fun formatIndianNumbering(number: Long): String {
    val str = number.toString()
    if (str.length <= 3) return str
    val lastThree = str.takeLast(3)
    val remaining = str.dropLast(3)
    val sb = StringBuilder()
    var count = 0
    for (i in remaining.length - 1 downTo 0) {
        if (count == 2) {
            sb.append(',')
            count = 0
        }
        sb.append(remaining[i])
        count++
    }
    return sb.reverse().append(',').append(lastThree).toString()
}

fun formatCurrency(amount: Double): String {
    val isNegative = amount < 0
    val absAmount = kotlin.math.abs(amount)
    val integerPart = absAmount.toLong()
    val decimalPart = kotlin.math.round((absAmount - integerPart) * 100).toLong()
    
    val formattedInteger = formatIndianNumbering(integerPart)
    val formattedDecimal = String.format(java.util.Locale.US, "%02d", decimalPart)
    val sign = if (isNegative) "-" else ""
    return "$sign₹$formattedInteger.$formattedDecimal"
}

fun formatAmount(amount: Double): String {
    val isNegative = amount < 0
    val absAmount = kotlin.math.abs(amount)
    val integerPart = absAmount.toLong()
    val decimalPart = kotlin.math.round((absAmount - integerPart) * 100).toLong()
    
    val formattedInteger = formatIndianNumbering(integerPart)
    val sign = if (isNegative) "-" else ""
    return if (decimalPart > 0) {
        val formattedDecimal = String.format(java.util.Locale.US, "%02d", decimalPart)
        "$sign$formattedInteger.$formattedDecimal"
    } else {
        "$sign$formattedInteger"
    }
}

@Composable
fun M3Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    containerColor: Color = AppAccentColor(),
    contentColor: Color = AppOnAccentColor(containerColor),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = containerColor.copy(alpha = 0.38f),
        disabledContentColor = contentColor.copy(alpha = 0.38f)
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 42.dp).bounceClick(),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 3.dp,
            pressedElevation = 1.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = contentPadding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun M3OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val isLight = isAppLightMode()
    val bgColor = if (isLight) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 42.dp).bounceClick(),
        enabled = enabled,
        shape = shape,
        color = bgColor,
        contentColor = if (isLight) Color(0xFF0F172A) else AppAccentColor(),
        shadowElevation = if (isLight) 2.5.dp else 4.dp
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun M3Card(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        border = null,
        onClick = onClick,
        content = content
    )
}

@Composable
fun M3Chip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val isLight = isAppLightMode()
    val accent = AppAccentColor()
    val bgColor = if (selected) {
        if (isLight) Color(0xFFD3E3FD) else Color(0xFF004A77)
    } else {
        if (isLight) Color(0xFFF1F5F9) else Color(0xFF252525)
    }
    val textColor = if (selected) accent else AppTextColor()
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = textColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

data class PaymentInfoRowItem(
    val label: String,
    val value: String,
    val copyable: Boolean = true
)

data class PaymentContactChip(
    val label: String,
    val value: String,
    val isEmail: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MinimalistPaymentInfoDialog(
    title: String,
    subtitle: String? = null,
    badgeLabel: String? = null,
    qrUriString: String? = null,
    upiId: String = "",
    detailRows: List<PaymentInfoRowItem> = emptyList(),
    contactChips: List<PaymentContactChip> = emptyList(),
    financialSummaryText: String? = null,
    notesText: String? = null,
    emptyQrLabel: String = "No QR code attached",
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onActionCopyToast: ((String) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val isLight = isAppLightMode()
    val accent = AppAccentColor()
    val qrList = remember(qrUriString) {
        (qrUriString ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
    var selectedQrIndex by remember(qrList) { androidx.compose.runtime.mutableIntStateOf(0) }
    var fullScreenQrUri by remember { mutableStateOf<String?>(null) }

    val copyWithFeedback: (String, String) -> Unit = { label, value ->
        clipboard.setText(androidx.compose.ui.text.AnnotatedString(value))
        if (onActionCopyToast != null) {
            onActionCopyToast(label)
        } else {
            android.widget.Toast.makeText(context, "$label copied", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .heightIn(max = 680.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(24.dp),
                color = getDialogBackground(),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title.ifBlank { "Payment Details" },
                                color = AppTextColor(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!subtitle.isNullOrBlank() || !badgeLabel.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!subtitle.isNullOrBlank()) {
                                        Text(
                                            text = subtitle,
                                            color = AppSecondaryTextColor(),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!badgeLabel.isNullOrBlank()) {
                                        Surface(
                                            color = accent.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = badgeLabel,
                                                color = accent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AppTextColor().copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = AppTextColor(),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // QR Viewer Card
                        if (qrList.isNotEmpty()) {
                            val currentQrUri = qrList.getOrElse(selectedQrIndex.coerceIn(0, qrList.lastIndex)) { qrList.first() }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = if (isLight) Color(0xFFF8FAFC) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (qrList.size > 1) {
                                        LazyRowCompatQrSwitcher(
                                            count = qrList.size,
                                            selectedIndex = selectedQrIndex,
                                            accent = accent,
                                            onSelect = { selectedQrIndex = it }
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .size(200.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { fullScreenQrUri = currentQrUri },
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.White,
                                        shadowElevation = 2.dp
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            coil.compose.AsyncImage(
                                                model = currentQrUri,
                                                contentDescription = "Payment QR Code",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tap QR to enlarge",
                                        color = accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = AppTextColor().copy(alpha = 0.05f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inbox,
                                        contentDescription = null,
                                        tint = AppSecondaryTextColor(),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = emptyQrLabel,
                                        color = AppSecondaryTextColor(),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Primary UPI row if provided separately
                        val combinedRows = buildList {
                            if (upiId.isNotBlank() && detailRows.none { it.label.equals("UPI ID", ignoreCase = true) }) {
                                add(PaymentInfoRowItem("UPI ID", upiId, copyable = true))
                            }
                            addAll(detailRows.filter { it.value.isNotBlank() })
                        }

                        if (combinedRows.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = AppTextColor().copy(alpha = 0.04f)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    combinedRows.forEachIndexed { idx, row ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = row.copyable) {
                                                    copyWithFeedback(row.label, row.value)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = row.label,
                                                    color = AppSecondaryTextColor(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = row.value,
                                                    color = AppTextColor(),
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (row.copyable) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Copy",
                                                    color = accent,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        if (idx < combinedRows.lastIndex) {
                                            HorizontalDivider(
                                                color = AppTextColor().copy(alpha = 0.07f),
                                                modifier = Modifier.padding(horizontal = 14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Contact chips
                        val visibleContacts = contactChips.filter { it.value.isNotBlank() }
                        if (visibleContacts.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                visibleContacts.forEach { contact ->
                                    Surface(
                                        modifier = Modifier.clickable {
                                            copyWithFeedback(contact.label, contact.value)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = accent.copy(alpha = 0.10f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${contact.label}: ",
                                                color = accent,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = contact.value,
                                                color = AppTextColor(),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Optional financial summary
                        if (!financialSummaryText.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = AccentCyan.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    text = financialSummaryText,
                                    color = AppTextColor(),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                )
                            }
                        }

                        // Optional notes
                        if (!notesText.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = AppTextColor().copy(alpha = 0.04f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                    Text(
                                        text = "Notes",
                                        color = AppSecondaryTextColor(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = notesText,
                                        color = AppTextColor(),
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Footer Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onEdit != null) {
                            M3OutlinedButton(
                                onClick = onEdit,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Edit", maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
                            }
                        }
                        M3Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (fullScreenQrUri != null) {
        FullScreenQRPreviewDialog(
            qrUri = fullScreenQrUri!!,
            title = title,
            upiId = upiId,
            onDismiss = { fullScreenQrUri = null }
        )
    }
}

@Composable
private fun LazyRowCompatQrSwitcher(
    count: Int,
    selectedIndex: Int,
    accent: Color,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        repeat(count) { idx ->
            val selected = idx == selectedIndex
            Surface(
                modifier = Modifier.clickable { onSelect(idx) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) accent else AppTextColor().copy(alpha = 0.08f)
            ) {
                Text(
                    text = "QR ${idx + 1}",
                    color = if (selected) AppOnAccentColor(accent) else AppTextColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }
    }
}


