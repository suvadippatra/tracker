package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.UserProfileEntity

private val accentColorCache = java.util.concurrent.ConcurrentHashMap<Triple<String, Boolean, Boolean>, Color>()

fun getAppAccentColor(userProfile: UserProfileEntity?): Color {
    val bgPreset = userProfile?.bgPreset ?: "Default"
    val isGlass = userProfile?.enableLiquidGlassTheme ?: false
    val isLight = when (bgPreset) {
        "Soft Minimal Light", "Daylight Ocean" -> true
        "Midnight Blue", "Emerald Dark", "Sunset Warm", "Purple Twilight", "Pure Black" -> false
        else -> {
            if (userProfile?.themeMode == "Auto") true else userProfile?.themeMode == "Light"
        }
    }
    return getAppAccentColor(bgPreset, isGlass, isLight)
}

fun getAppAccentColor(bgPreset: String?, isLiquidGlass: Boolean, isLightMode: Boolean = true): Color {
    val preset = bgPreset ?: "Default"
    val key = Triple(preset, isLiquidGlass, isLightMode)
    return accentColorCache.getOrPut(key) {
        if (isLiquidGlass) {
            when (preset) {
                "Midnight Blue", "Ocean", "Ocean Blue", "M3 Ocean Blue" -> GlassMidnightBlue
                "Emerald Dark", "Emerald", "Emerald Green", "M3 Emerald" -> GlassEmeraldGlow
                "Sunset Warm", "Gold", "Amber Gold", "M3 Amber" -> GlassGoldenHour
                "Rose", "Crimson", "Crimson Red", "M3 Crimson" -> GlassElectricSunset
                "Obsidian", "Pure Black", "Default" -> GlassNeonCyan
                "Purple Twilight", "Purple", "Deep Purple", "M3 Purple" -> GlassPurpleTwilight
                "Soft Minimal Light" -> M3AccentOceanBlue
                "M3 Cyan Teal", "Cyan Teal" -> GlassNeonCyan
                else -> GlassNeonCyan
            }
        } else {
            if (isLightMode) {
                when (preset) {
                    "Emerald", "Emerald Green", "M3 Emerald", "Emerald Dark" -> EmeraldPrimaryLight
                    "Ocean", "Ocean Blue", "M3 Ocean Blue", "Midnight Blue" -> OceanPrimaryLight
                    "Rose", "Crimson", "Crimson Red", "M3 Crimson" -> RosePrimaryLight
                    "Gold", "Amber", "Amber Gold", "Sunset Warm", "M3 Amber" -> GoldPrimaryLight
                    "Obsidian", "Pure Black", "Default" -> ObsidianPrimaryLight
                    "Purple", "Deep Purple", "M3 Purple", "Purple Twilight" -> M3AccentPurple
                    "Cyan Teal", "M3 Cyan Teal", "Soft Minimal Light" -> M3AccentCyanTeal
                    else -> OceanPrimaryLight
                }
            } else {
                when (preset) {
                    "Emerald", "Emerald Green", "M3 Emerald", "Emerald Dark" -> EmeraldPrimaryDark
                    "Ocean", "Ocean Blue", "M3 Ocean Blue", "Midnight Blue" -> OceanPrimaryDark
                    "Rose", "Crimson", "Crimson Red", "M3 Crimson" -> RosePrimaryDark
                    "Gold", "Amber", "Amber Gold", "Sunset Warm", "M3 Amber" -> GoldPrimaryDark
                    "Obsidian", "Pure Black", "Default" -> ObsidianPrimaryDark
                    "Purple", "Deep Purple", "M3 Purple", "Purple Twilight" -> M3AccentPurple
                    "Cyan Teal", "M3 Cyan Teal", "Soft Minimal Light" -> M3AccentCyanTeal
                    else -> OceanPrimaryDark
                }
            }
        }
    }
}

fun buildAppColorScheme(
    preset: String?,
    isLiquidGlass: Boolean,
    darkTheme: Boolean
): ColorScheme {
    val p = preset ?: "Default"

    return if (darkTheme) {
        when {
            p.contains("Emerald", ignoreCase = true) -> darkColorScheme(
                primary = if (isLiquidGlass) GlassEmeraldGlow else EmeraldPrimaryDark,
                onPrimary = Color(0xFF00391C),
                primaryContainer = EmeraldPrimaryContainerDark,
                onPrimaryContainer = EmeraldOnPrimaryContainerDark,
                secondary = EmeraldSecondaryDark,
                onSecondary = Color(0xFF00382B),
                secondaryContainer = Color(0xFF00513F),
                onSecondaryContainer = Color(0xFF70F7D4),
                tertiary = Color(0xFF80D5D3),
                background = M3BackgroundDark,
                surface = M3SurfaceDark,
                surfaceVariant = M3SurfaceHighDark,
                outline = M3OutlineDark,
                outlineVariant = M3OutlineVariantDark,
                onBackground = Color.White,
                onSurface = Color.White
            )
            p.contains("Rose", ignoreCase = true) || p.contains("Crimson", ignoreCase = true) -> darkColorScheme(
                primary = if (isLiquidGlass) GlassElectricSunset else RosePrimaryDark,
                onPrimary = Color.White,
                primaryContainer = RosePrimaryContainerDark,
                onPrimaryContainer = RoseOnPrimaryContainerDark,
                secondary = RoseSecondaryDark,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFF7A1D38),
                onSecondaryContainer = Color(0xFFFFD9E2),
                tertiary = Color(0xFFFFB2B8),
                background = M3BackgroundDark,
                surface = M3SurfaceDark,
                surfaceVariant = M3SurfaceHighDark,
                outline = M3OutlineDark,
                outlineVariant = M3OutlineVariantDark,
                onBackground = Color.White,
                onSurface = Color.White
            )
            p.contains("Gold", ignoreCase = true) || p.contains("Amber", ignoreCase = true) || p.contains("Sunset", ignoreCase = true) -> darkColorScheme(
                primary = if (isLiquidGlass) GlassGoldenHour else GoldPrimaryDark,
                onPrimary = Color(0xFF0F172A),
                primaryContainer = GoldPrimaryContainerDark,
                onPrimaryContainer = GoldOnPrimaryContainerDark,
                secondary = GoldSecondaryDark,
                onSecondary = Color(0xFF0F172A),
                secondaryContainer = Color(0xFF5F3B00),
                onSecondaryContainer = Color(0xFFFFDF9E),
                tertiary = Color(0xFFE5C18D),
                background = M3BackgroundDark,
                surface = M3SurfaceDark,
                surfaceVariant = M3SurfaceHighDark,
                outline = M3OutlineDark,
                outlineVariant = M3OutlineVariantDark,
                onBackground = Color.White,
                onSurface = Color.White
            )
            p.contains("Obsidian", ignoreCase = true) || p == "Pure Black" || (p == "Default" && !isLiquidGlass) -> darkColorScheme(
                primary = if (isLiquidGlass) GlassNeonCyan else ObsidianPrimaryDark,
                onPrimary = if (isLiquidGlass) Color(0xFF0F172A) else Color.White,
                primaryContainer = ObsidianPrimaryContainerDark,
                onPrimaryContainer = ObsidianOnPrimaryContainerDark,
                secondary = ObsidianSecondaryDark,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFF334155),
                onSecondaryContainer = Color(0xFFCBD5E1),
                tertiary = Color(0xFF94A3B8),
                background = M3BackgroundDark,
                surface = M3SurfaceDark,
                surfaceVariant = M3SurfaceHighDark,
                outline = M3OutlineDark,
                outlineVariant = M3OutlineVariantDark,
                onBackground = Color.White,
                onSurface = Color.White
            )
            p.contains("Purple", ignoreCase = true) -> darkColorScheme(
                primary = if (isLiquidGlass) GlassPurpleTwilight else Color(0xFFA78BFA),
                onPrimary = if (isLiquidGlass) Color.White else Color(0xFF1E1035),
                primaryContainer = Color(0xFF4F378B),
                onPrimaryContainer = Color(0xFFEADDFF),
                secondary = Color(0xFFCCC2DC),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFF4A4458),
                onSecondaryContainer = Color(0xFFE8DEF8),
                tertiary = Color(0xFFEFB8C8),
                background = M3BackgroundDark,
                surface = M3SurfaceDark,
                surfaceVariant = M3SurfaceHighDark,
                outline = M3OutlineDark,
                outlineVariant = M3OutlineVariantDark,
                onBackground = Color.White,
                onSurface = Color.White
            )
            else -> darkColorScheme(
                // Ocean / Default
                primary = if (isLiquidGlass) GlassMidnightBlue else OceanPrimaryDark,
                onPrimary = Color(0xFF0F172A),
                primaryContainer = OceanPrimaryContainerDark,
                onPrimaryContainer = OceanOnPrimaryContainerDark,
                secondary = OceanSecondaryDark,
                onSecondary = Color(0xFF0F172A),
                secondaryContainer = Color(0xFF004B70),
                onSecondaryContainer = Color(0xFFC7E7FF),
                tertiary = Color(0xFFA5C8FF),
                background = M3BackgroundDark,
                surface = M3SurfaceDark,
                surfaceVariant = M3SurfaceHighDark,
                outline = M3OutlineDark,
                outlineVariant = M3OutlineVariantDark,
                onBackground = Color.White,
                onSurface = Color.White
            )
        }
    } else {
        when {
            p.contains("Emerald", ignoreCase = true) -> lightColorScheme(
                primary = EmeraldPrimaryLight,
                onPrimary = Color.White,
                primaryContainer = EmeraldPrimaryContainerLight,
                onPrimaryContainer = EmeraldOnPrimaryContainerLight,
                secondary = EmeraldSecondaryLight,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFCCFBF1),
                onSecondaryContainer = Color(0xFF115E59),
                tertiary = Color(0xFF0D9488),
                background = M3BackgroundLight,
                surface = M3SurfaceLight,
                surfaceVariant = M3SurfaceHighLight,
                outline = M3OutlineLight,
                outlineVariant = M3OutlineVariantLight,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
            p.contains("Rose", ignoreCase = true) || p.contains("Crimson", ignoreCase = true) -> lightColorScheme(
                primary = RosePrimaryLight,
                onPrimary = Color.White,
                primaryContainer = RosePrimaryContainerLight,
                onPrimaryContainer = RoseOnPrimaryContainerLight,
                secondary = RoseSecondaryLight,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFFD9E2),
                onSecondaryContainer = Color(0xFF3B0717),
                tertiary = Color(0xFF984061),
                background = M3BackgroundLight,
                surface = M3SurfaceLight,
                surfaceVariant = M3SurfaceHighLight,
                outline = M3OutlineLight,
                outlineVariant = M3OutlineVariantLight,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
            p.contains("Gold", ignoreCase = true) || p.contains("Amber", ignoreCase = true) || p.contains("Sunset", ignoreCase = true) -> lightColorScheme(
                primary = GoldPrimaryLight,
                onPrimary = Color.White,
                primaryContainer = GoldPrimaryContainerLight,
                onPrimaryContainer = GoldOnPrimaryContainerLight,
                secondary = GoldSecondaryLight,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFFDF9E),
                onSecondaryContainer = Color(0xFF2B1700),
                tertiary = Color(0xFF7A5812),
                background = M3BackgroundLight,
                surface = M3SurfaceLight,
                surfaceVariant = M3SurfaceHighLight,
                outline = M3OutlineLight,
                outlineVariant = M3OutlineVariantLight,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
            p.contains("Obsidian", ignoreCase = true) || p == "Pure Black" || (p == "Default" && !isLiquidGlass) -> lightColorScheme(
                primary = ObsidianPrimaryLight,
                onPrimary = Color.White,
                primaryContainer = ObsidianPrimaryContainerLight,
                onPrimaryContainer = ObsidianOnPrimaryContainerLight,
                secondary = ObsidianSecondaryLight,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF1F5F9),
                onSecondaryContainer = Color(0xFF1E293B),
                tertiary = Color(0xFF475569),
                background = M3BackgroundLight,
                surface = M3SurfaceLight,
                surfaceVariant = M3SurfaceHighLight,
                outline = M3OutlineLight,
                outlineVariant = M3OutlineVariantLight,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
            p.contains("Purple", ignoreCase = true) -> lightColorScheme(
                primary = M3AccentPurple,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEDE9FE),
                onPrimaryContainer = Color(0xFF5B21B6),
                secondary = Color(0xFF625B71),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE8DEF8),
                onSecondaryContainer = Color(0xFF1D192B),
                tertiary = Color(0xFF7D5260),
                background = M3BackgroundLight,
                surface = M3SurfaceLight,
                surfaceVariant = M3SurfaceHighLight,
                outline = M3OutlineLight,
                outlineVariant = M3OutlineVariantLight,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
            else -> lightColorScheme(
                // Ocean / Default
                primary = OceanPrimaryLight,
                onPrimary = Color.White,
                primaryContainer = OceanPrimaryContainerLight,
                onPrimaryContainer = OceanOnPrimaryContainerLight,
                secondary = OceanSecondaryLight,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFC7E7FF),
                onSecondaryContainer = Color(0xFF001E30),
                tertiary = Color(0xFF006399),
                background = M3BackgroundLight,
                surface = M3SurfaceLight,
                surfaceVariant = M3SurfaceHighLight,
                outline = M3OutlineLight,
                outlineVariant = M3OutlineVariantLight,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    preset: String? = null,
    isLiquidGlass: Boolean = false,
    accentColor: Color? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        accentColor != null && preset == null -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = accentColor,
                    secondary = accentColor,
                    tertiary = Purple80,
                    background = M3BackgroundDark,
                    surface = M3SurfaceDark,
                    surfaceVariant = M3SurfaceHighDark,
                    outline = M3OutlineDark,
                    outlineVariant = M3OutlineVariantDark,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onPrimary = Color.White
                )
            } else {
                lightColorScheme(
                    primary = accentColor,
                    secondary = accentColor,
                    tertiary = Purple40,
                    background = M3BackgroundLight,
                    surface = M3SurfaceLight,
                    surfaceVariant = M3SurfaceHighLight,
                    outline = M3OutlineLight,
                    outlineVariant = M3OutlineVariantLight,
                    onBackground = Color(0xFF0F172A),
                    onSurface = Color(0xFF0F172A),
                    onPrimary = Color.White
                )
            }
        }
        else -> buildAppColorScheme(preset, isLiquidGlass, darkTheme)
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}


