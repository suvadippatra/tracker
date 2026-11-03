package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.components.LocalAppTheme
import com.example.ui.components.LocalAppBgPreset
import com.example.ui.components.LocalAppCustomBgUri
import com.example.ui.components.LocalAppLiquidGlassTheme
import com.example.ui.components.LocalDisableHeavyEffects

@Composable
fun AppThemeProvider(viewModel: FinancialViewModel, content: @Composable () -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val customFontScale = when (userProfile.textSize) {
        "Small" -> 0.90f
        "Large" -> 1.10f
        else -> 1.0f
    }
    CompositionLocalProvider(
        LocalAppTheme provides userProfile.themeMode,
        LocalAppBgPreset provides userProfile.bgPreset,
        LocalAppCustomBgUri provides userProfile.customBgUri,
        LocalAppLiquidGlassTheme provides userProfile.enableLiquidGlassTheme,
        LocalDisableHeavyEffects provides userProfile.disableHeavyEffects,
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
            density = currentDensity.density,
            fontScale = customFontScale
        )
    ) {
        androidx.compose.material3.ProvideTextStyle(
            value = androidx.compose.ui.text.TextStyle(
                fontFamily = com.example.ui.theme.AppFontFamily
            ),
            content = content
        )
    }
}

