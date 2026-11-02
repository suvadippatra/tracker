package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

/**
 * "Care Comes to Life" Splash Screen
 * 
 * Replaces generic circular loading and sweeping spinners with an elegant,
 * professional entrance animation tailored to the Traker logo:
 * 1. Background softly fades in with a warm, restrained gradient (dark/light adaptive).
 * 2. The complete PNG logo gently floats upward while fading in.
 * 3. The logo settles into its natural resting position.
 * 4. The orange heart in the center receives a delicate, warm cardiac pulse and soft glow aura.
 * 5. The application title and subtitle fade in cleanly below the logo.
 * 6. Smoothly dissolves into the application home screen without delay or black-screen stutter.
 */
@Composable
fun SplashScreen(
    isInitialized: Boolean,
    language: String = "en",
    disableHeavyEffects: Boolean = false,
    onFinish: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    // Color definitions for light and dark modes
    val bgBrush = if (isSystemDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A1317), // Deep teal-black
                Color(0xFF0F1B22), // Soft dark neutral
                Color(0xFF091216)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFCFCFD), // Warm off-white
                Color(0xFFF3F6F9), // Light neutral
                Color(0xFFEBF0F5)
            )
        )
    }

    val titleColor = if (isSystemDark) Color(0xFFE8EEF1) else Color(0xFF132F38)
    val subtitleColor = if (isSystemDark) Color(0xFF9FB2BC) else Color(0xFF5A727C)
    val heartWarmOrange = Color(0xFFFF9500)

    // Animation progress states
    var startAnimation by remember { mutableStateOf(false) }
    var isFadingOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
        // Allow the "Care Comes to Life" animation to complete gracefully (~1400ms)
        // If reduced motion is requested, complete briskly in ~450ms
        val animationHoldTime = if (disableHeavyEffects) 450L else 1450L
        delay(animationHoldTime)

        // Wait until database initialization finishes (safety fallback: max 2.2s total)
        var elapsed = 0L
        while (!isInitialized && elapsed < 800L) {
            delay(100L)
            elapsed += 100L
        }

        // Trigger smooth dissolve out
        isFadingOut = true
        delay(260L)
        onFinish()
    }

    // 1. Overall splash container alpha (for smooth exit dissolve)
    val containerAlpha by animateFloatAsState(
        targetValue = if (isFadingOut) 0f else 1f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "splash_exit"
    )

    // 2. Logo Fade & Gentle Float
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (disableHeavyEffects) 250 else 600,
            delayMillis = if (disableHeavyEffects) 0 else 120,
            easing = FastOutSlowInEasing
        ),
        label = "logo_alpha"
    )

    val logoOffsetY by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else if (disableHeavyEffects) 0.dp else 22.dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_offset_y"
    )

    // 3. Heart Pulse & Soft Glow ("Care Comes to Life")
    val heartGlowAlpha by animateFloatAsState(
        targetValue = if (startAnimation && !disableHeavyEffects) 0.42f else 0f,
        animationSpec = keyframes {
            durationMillis = 1100
            0.0f at 0 using LinearEasing
            0.0f at 450 using FastOutSlowInEasing
            0.55f at 750 using FastOutSlowInEasing // Peak warm glow
            0.20f at 1050 using FastOutSlowInEasing // Settles into calm resting ambient
        },
        label = "heart_glow_alpha"
    )

    val heartPulseScale by animateFloatAsState(
        targetValue = if (startAnimation && !disableHeavyEffects) 1.0f else 0.92f,
        animationSpec = keyframes {
            durationMillis = 1100
            0.92f at 0
            0.92f at 450
            1.16f at 750 using FastOutSlowInEasing // Tender heartbeat expansion
            1.00f at 1050 using FastOutSlowInEasing // Natural settle
        },
        label = "heart_pulse_scale"
    )

    // 4. App Name & Subtitle Fade-in
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (disableHeavyEffects) 250 else 550,
            delayMillis = if (disableHeavyEffects) 100 else 650,
            easing = FastOutSlowInEasing
        ),
        label = "text_alpha"
    )

    val textOffsetY by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else if (disableHeavyEffects) 0.dp else 10.dp,
        animationSpec = tween(
            durationMillis = if (disableHeavyEffects) 250 else 550,
            delayMillis = if (disableHeavyEffects) 100 else 650,
            easing = FastOutSlowInEasing
        ),
        label = "text_offset_y"
    )

    // Responsive dimensions
    val logoSize = when {
        isLandscape -> 96.dp
        isTablet -> 160.dp
        else -> 136.dp
    }

    val spacingBetweenLogoAndText = if (isLandscape) 10.dp else 20.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(containerAlpha)
            .background(bgBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = 24.dp)
        ) {
            // Logo Container with Heart Glow Effect
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .offset(y = logoOffsetY)
                    .alpha(logoAlpha),
                contentAlignment = Alignment.Center
            ) {
                // Heart Pulse Glow Layer
                // In the Traker logo, the orange heart is situated at approx (50% X, 43% Y)
                if (heartGlowAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val heartCenterX = size.width * 0.50f
                                val heartCenterY = size.height * 0.435f
                                val glowRadius = (size.width * 0.28f) * heartPulseScale

                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            heartWarmOrange.copy(alpha = heartGlowAlpha),
                                            heartWarmOrange.copy(alpha = heartGlowAlpha * 0.45f),
                                            Color.Transparent
                                        ),
                                        center = Offset(heartCenterX, heartCenterY),
                                        radius = glowRadius
                                    ),
                                    radius = glowRadius,
                                    center = Offset(heartCenterX, heartCenterY)
                                )
                            }
                    )
                }

                // Authentic Original Logo PNG (No SVG, no vector distortion, original colors preserved)
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Traker Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(spacingBetweenLogoAndText))

            // Application Name & Caring Mission Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = textOffsetY)
                    .alpha(textAlpha)
            ) {
                Text(
                    text = if (language == "bn") "ট্রেকার" else "Traker",
                    fontSize = if (isTablet) 28.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    letterSpacing = 1.2.sp,
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (language == "bn") "সেবা, সহায়তা ও আর্থিক হিসাব" else "Care, Assistance & Daily Ledger",
                    fontSize = if (isTablet) 14.sp else 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = subtitleColor,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}
