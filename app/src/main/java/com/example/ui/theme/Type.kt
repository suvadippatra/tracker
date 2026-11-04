package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val AppFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

/**
 * Dedicated bold font family using Montserrat Bold for high-contrast bold typography.
 */
val MontserratBoldFontFamily = FontFamily(
    Font(R.font.montserrat, FontWeight.Normal),
    Font(R.font.montserrat, FontWeight.Medium),
    Font(R.font.montserrat, FontWeight.SemiBold),
    Font(R.font.montserrat, FontWeight.Bold),
    Font(R.font.montserrat, FontWeight.ExtraBold)
)

val AppBoldFontFamily = MontserratBoldFontFamily

fun getAppTypography(textSize: String = "Medium", isBold: Boolean = false): Typography {
    val family = if (isBold) AppBoldFontFamily else AppFontFamily
    return Typography(
        displayLarge = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.25).sp
        ),
        displaySmall = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 34.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 21.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 19.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.25.sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.3.sp
        )
    )
}

val Typography = getAppTypography("Medium", false)
