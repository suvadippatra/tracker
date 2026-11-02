with open("app/src/main/java/com/example/ui/theme/Theme.kt", "r") as f:
    text = f.read()

import re

# replace accentColorCache definition
text = text.replace("ConcurrentHashMap<Pair<String, Boolean>, Color>()", "ConcurrentHashMap<Triple<String, Boolean, Boolean>, Color>()")

# replace getAppAccentColor signatures
# Note: getAppAccentColor(userProfile: UserProfileEntity?) doesn't matter much as it's not used but let's leave it.
# Actually let's just replace the whole function block for getAppAccentColor(bgPreset: String?, isLiquidGlass: Boolean)

target_func_start = text.find("fun getAppAccentColor(bgPreset: String?, isLiquidGlass: Boolean): Color {")
target_func_end = text.find("fun buildAppColorScheme(")

if target_func_start != -1 and target_func_end != -1:
    new_func = """fun getAppAccentColor(bgPreset: String?, isLiquidGlass: Boolean, isLightMode: Boolean = true): Color {
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

"""
    text = text[:target_func_start] + new_func + text[target_func_end:]
    
    with open("app/src/main/java/com/example/ui/theme/Theme.kt", "w") as f:
        f.write(text)
    print("Updated Theme.kt")
else:
    print("Target not found")
