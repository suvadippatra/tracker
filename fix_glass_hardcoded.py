import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    text = f.read()

# Fix GlassSwitch
old_switch = """    Box(
        modifier = Modifier
            .width(100.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Color(0x1F000000))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(17.dp))
            .clickable { onCheckedChange(!checked) }
    )"""

new_switch = """    val isLight = isAppLightMode()
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (isLight) Color(0x1A000000) else Color(0x1F000000))
            .border(1.dp, if (isLight) Color(0xFFFFFFFF) else Color(0x33FFFFFF), RoundedCornerShape(17.dp))
            .clickable { onCheckedChange(!checked) }
    )"""
text = text.replace(old_switch, new_switch)

old_switch_thumb_bg = """                .background(
                    brush = if (checked) {
                        Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF00E676)))
                    } else {
                        Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF)))
                    },
                    shape = RoundedCornerShape(15.dp)
                )"""

new_switch_thumb_bg = """                .background(
                    brush = if (checked) {
                        Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF00E676)))
                    } else {
                        if (isLight) Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xE6FFFFFF)))
                        else Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF)))
                    },
                    shape = RoundedCornerShape(15.dp)
                )"""
text = text.replace(old_switch_thumb_bg, new_switch_thumb_bg)

# Fix DayNight switch
old_daynight = """                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(Color(0x1F000000))
                                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(19.dp))
                                .clickable { viewModel.toggleDayNight() }
                        )"""

new_daynight = """                        val isAppLight = isAppLightMode()
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (isAppLight) Color(0x1A000000) else Color(0x1F000000))
                                .border(1.dp, if (isAppLight) Color(0xFFFFFFFF) else Color(0x33FFFFFF), RoundedCornerShape(19.dp))
                                .clickable { viewModel.toggleDayNight() }
                        )"""
text = text.replace(old_daynight, new_daynight)

# Fix Input fields (CustomTextField?)
# Let's see if there's a custom text field
text_field = re.search(r'fun GlassTextField\(.*?\n\}', text, re.DOTALL)
if text_field:
    print("Found GlassTextField!")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(text)

print("Updated Glass components")
