package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AppBoldFontFamily
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.ThemePreferences
import com.example.ui.theme.getAppTypography
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun AppThemeProvider(viewModel: FinancialViewModel, content: @Composable () -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val customFontScale = ThemePreferences.getFontScale(userProfile.textSize)
    val customDensity = remember(currentDensity.density, customFontScale) {
        androidx.compose.ui.unit.Density(
            density = currentDensity.density,
            fontScale = customFontScale
        )
    }

    CompositionLocalProvider(
        LocalAppTheme provides userProfile.themeMode,
        LocalAppBgPreset provides userProfile.bgPreset,
        LocalAppCustomBgUri provides userProfile.customBgUri,
        LocalAppLiquidGlassTheme provides userProfile.enableLiquidGlassTheme,
        LocalDisableHeavyEffects provides userProfile.disableHeavyEffects,
        LocalBoldTextEnabled provides userProfile.isBoldTextEnabled,
        LocalStickyCardWidthPercent provides userProfile.stickyCardWidthPercent.coerceIn(50, 80),
        LocalStickyPreviewLines provides userProfile.stickyPreviewLines.coerceIn(4, 10),
        androidx.compose.ui.platform.LocalDensity provides customDensity,
        content = content
    )
}
