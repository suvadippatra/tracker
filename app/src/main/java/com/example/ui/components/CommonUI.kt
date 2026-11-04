package com.example.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
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
    val fastMode = isHeavyEffectsDisabled()
    val shape = RoundedCornerShape(cornerRadius)
    val cardBg = if (isLight) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark
    val shadowElev = if (fastMode) (if (isLight) 2.dp else 3.dp) else (if (isLight) 4.dp else 6.dp)
    val ambientShadow = if (isLight) Color(0x1F0F172A) else Color(0x66000000)
    val spotShadow = if (isLight) Color(0x290F172A) else Color(0x88000000)

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElev,
                shape = shape,
                clip = true,
                ambientColor = ambientShadow,
                spotColor = spotShadow
            )
            .background(cardBg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

fun Modifier.UARTShadow(
    elevation: Dp,
    shape: androidx.compose.ui.graphics.Shape,
    ambientColor: Color,
    spotColor: Color
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = ambientColor,
    spotColor = spotColor
)

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
    val fastMode = isHeavyEffectsDisabled()
    val shape = RoundedCornerShape(cornerRadius)
    val buttonBg = if (isLight) com.example.ui.theme.M3SurfaceHighLight else com.example.ui.theme.M3SurfaceHighDark
    val ambientShadow = if (isLight) Color(0x180F172A) else Color(0x55000000)
    val spotShadow = if (isLight) Color(0x220F172A) else Color(0x77000000)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .shadow(
                elevation = if (fastMode) 1.5.dp else (if (isLight) 2.5.dp else 4.dp),
                shape = shape,
                clip = true,
                ambientColor = ambientShadow,
                spotColor = spotShadow
            )
            .background(buttonBg)
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
            .shadow(
                elevation = 1.5.dp,
                shape = shape,
                clip = true,
                ambientColor = if (isLight) Color(0x180F172A) else Color(0x44000000),
                spotColor = if (isLight) Color(0x220F172A) else Color(0x66000000)
            )
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false
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
                .shadow(
                    elevation = if (isFocused) 2.5.dp else (if (isLight) 1.dp else 2.dp),
                    shape = fieldShape,
                    clip = true,
                    ambientColor = if (isLight) Color(0x140F172A) else Color(0x44000000),
                    spotColor = if (isLight) Color(0x1C0F172A) else Color(0x66000000)
                )
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

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
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
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

fun copyUriToPersistentFile(context: android.content.Context, uri: android.net.Uri, prefix: String = "QR"): String {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return uri.toString()
        val file = java.io.File(context.filesDir, "${prefix}_${System.currentTimeMillis()}.png")
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
            .shadow(
                elevation = if (selected) 2.5.dp else 1.dp,
                shape = shape,
                clip = true,
                ambientColor = if (isLight) Color(0x160F172A) else Color(0x44000000),
                spotColor = if (isLight) Color(0x220F172A) else Color(0x66000000)
            )
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

