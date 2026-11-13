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
    private const val KEY_BOLD_TEXT = "is_bold_text_enabled"
    private const val KEY_STICKY_WIDTH_PERCENT = "sticky_card_width_percent"
    private const val KEY_STICKY_PREVIEW_LINES = "sticky_preview_lines"

    fun getFontScale(textSize: String?): Float {
        return when (textSize) {
            "Small" -> 0.88f
            "Large" -> 1.14f
            "Extra Large" -> 1.26f
            else -> 1.0f
        }
    }

    fun getFontScale(context: Context): Float {
        val textSize = try {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_TEXT_SIZE, "Medium") ?: "Medium"
        } catch (_: Exception) {
            "Medium"
        }
        return getFontScale(textSize)
    }

    fun isBoldText(context: Context): Boolean {
        return try {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_BOLD_TEXT, false)
        } catch (_: Exception) {
            false
        }
    }

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
                .putBoolean(KEY_BOLD_TEXT, profile.isBoldTextEnabled)
                .putInt(KEY_STICKY_WIDTH_PERCENT, profile.stickyCardWidthPercent.coerceIn(50, 80))
                .putInt(KEY_STICKY_PREVIEW_LINES, profile.stickyPreviewLines.coerceIn(4, 10))
                .apply()
        } catch (_: Exception) {}
    }

    fun isLightMode(context: Context): Boolean {
        val themeMode = try {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_THEME_MODE, "Auto") ?: "Auto"
        } catch (_: Exception) {
            "Auto"
        }
        return when (themeMode) {
            "Light" -> true
            "Dark" -> false
            else -> {
                val nightMask = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightMask != android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun getInitialProfile(context: Context): UserProfileEntity {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val bgPreset = prefs.getString(KEY_BG_PRESET, "Default") ?: "Default"
            val themeMode = prefs.getString(KEY_THEME_MODE, "Auto") ?: "Auto"
            val customBgUri = prefs.getString(KEY_CUSTOM_BG_URI, "") ?: ""
            val textSize = prefs.getString(KEY_TEXT_SIZE, "Medium") ?: "Medium"
            val enableStars = prefs.getBoolean(KEY_ENABLE_STARS, true)
            val language = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
            val enableLiquidGlass = prefs.getBoolean(KEY_ENABLE_LIQUID_GLASS, false)
            val disableHeavyEffects = prefs.getBoolean(KEY_DISABLE_HEAVY_EFFECTS, true)
            val isBoldText = prefs.getBoolean(KEY_BOLD_TEXT, false)
            val stickyWidth = prefs.getInt(KEY_STICKY_WIDTH_PERCENT, 80).coerceIn(50, 80)
            val stickyLines = prefs.getInt(KEY_STICKY_PREVIEW_LINES, 10).coerceIn(4, 10)

            UserProfileEntity(
                bgPreset = bgPreset,
                themeMode = themeMode,
                customBgUri = customBgUri,
                textSize = textSize,
                enableStarAnimation = enableStars,
                language = language,
                enableLiquidGlassTheme = enableLiquidGlass,
                disableHeavyEffects = disableHeavyEffects,
                isBoldTextEnabled = isBoldText,
                stickyCardWidthPercent = stickyWidth,
                stickyPreviewLines = stickyLines,
                appVersion = com.example.BuildConfig.VERSION_NAME
            )
        } catch (_: Exception) {
            UserProfileEntity(
                bgPreset = "Default",
                themeMode = "Auto",
                enableLiquidGlassTheme = false,
                disableHeavyEffects = true,
                appVersion = com.example.BuildConfig.VERSION_NAME
            )
        }
    }
}
