import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_glass_card = """fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val isLight = isAppLightMode()
    val bgColors = if (isLight) {
        listOf(Color(0xB3FFFFFF), Color(0x80FFFFFF)) // More opaque for solid liquid glass feel in light mode
    } else {
        listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))"""

new_glass_card = """import androidx.compose.animation.animateContentSize

fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val isLight = isAppLightMode()
    val bgColors = if (isLight) {
        listOf(Color(0xB3FFFFFF), Color(0x80FFFFFF)) // More opaque for solid liquid glass feel in light mode
    } else {
        listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))
    }
    
    Box(
        modifier = modifier
            .animateContentSize()
            .clip(RoundedCornerShape(cornerRadius))"""

content = content.replace(old_glass_card, new_glass_card)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
