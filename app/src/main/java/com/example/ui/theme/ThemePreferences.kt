package com.example.ui.theme

import android.content.Context
import com.example.data.model.UserProfileEntity

object ThemePreferences {
    private const val PREF_NAME = "app_theme_cache"
    private const val KEY_BG_PRESET = "bg_preset"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_CUSTOM_BG_URI = "custom_bg_uri"
    private const val KEY_TEXT_SIZE = "text_size"
    private const val KEY_ENABLE_STARS = "enable_stars"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_ENABLE_LIQUID_GLASS = "enable_liquid_glass"
    private const val KEY_DISABLE_HEAVY_EFFECTS = "disable_heavy_effects"

    fun save(context: Context, profile: UserProfileEntity) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_BG_PRESET, profile.bgPreset)
                .putString(KEY_THEME_MODE, profile.themeMode)
                .putString(KEY_CUSTOM_BG_URI, profile.customBgUri)
                .putString(KEY_TEXT_SIZE, profile.textSize)
                .putBoolean(KEY_ENABLE_STARS, profile.enableStarAnimation)
                .putString(KEY_LANGUAGE, profile.language)
                .putBoolean(KEY_ENABLE_LIQUID_GLASS, profile.enableLiquidGlassTheme)
                .putBoolean(KEY_DISABLE_HEAVY_EFFECTS, profile.disableHeavyEffects)
                .apply()
        } catch (_: Exception) {}
    }

    fun getInitialProfile(context: Context): UserProfileEntity {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val bgPreset = prefs.getString(KEY_BG_PRESET, "Sunset Warm") ?: "Sunset Warm"
            val themeMode = prefs.getString(KEY_THEME_MODE, "Dark") ?: "Dark"
            val customBgUri = prefs.getString(KEY_CUSTOM_BG_URI, "") ?: ""
            val textSize = prefs.getString(KEY_TEXT_SIZE, "Medium") ?: "Medium"
            val enableStars = prefs.getBoolean(KEY_ENABLE_STARS, true)
            val language = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
            val enableLiquidGlass = prefs.getBoolean(KEY_ENABLE_LIQUID_GLASS, false)
            val disableHeavyEffects = prefs.getBoolean(KEY_DISABLE_HEAVY_EFFECTS, true)

            UserProfileEntity(
                bgPreset = bgPreset,
                themeMode = themeMode,
                customBgUri = customBgUri,
                textSize = textSize,
                enableStarAnimation = enableStars,
                language = language,
                enableLiquidGlassTheme = enableLiquidGlass,
                disableHeavyEffects = disableHeavyEffects
            )
        } catch (_: Exception) {
            UserProfileEntity(bgPreset = "Sunset Warm", themeMode = "Dark", enableLiquidGlassTheme = false, disableHeavyEffects = true)
        }
    }
}
