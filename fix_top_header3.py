import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

header_start = content.find("@Composable\nfun GlassHeader(")
header_end = content.find("// ==========================================\n// BOTTOM NAVIGATION BAR")

old_header = content[header_start:header_end]

# Rewrite the header to look cleaner and more premium without the huge padding block that might look bad
new_header = """@Composable
fun GlassHeader(
    userProfile: UserProfileEntity,
    onSettingsClicked: () -> Unit,
    onSearchClicked: () -> Unit
) {
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> if (userProfile.language == "bn") "শুভ সকাল 🌅" else "Good morning 🌅"
            in 12..16 -> if (userProfile.language == "bn") "শুভ দুপুর ☀️" else "Good afternoon ☀️"
            in 17..20 -> if (userProfile.language == "bn") "শুভ সন্ধ্যা 🌆" else "Good evening 🌆"
            else -> if (userProfile.language == "bn") "শুভ রাত্রি 🌙" else "Good evening 🌙"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Profile Avatar with high-contrast colored glass ring
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(AccentCyan, AccentPink)),
                            shape = CircleShape
                        )
                        .padding(2.5.dp)
                        .background(if (isAppLightMode()) Color.White else Color(0xFF0F0B26), shape = CircleShape)
                        .clip(CircleShape)
                        .clickable { onSettingsClicked() },
                    contentAlignment = Alignment.Center
                ) {
                    if (userProfile.name.isNotBlank()) {
                        Text(
                            text = userProfile.name.first().uppercase(),
                            color = AccentCyan,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = AccentCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = greeting,
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = userProfile.name.ifBlank { "User" },
                        color = AppTextColor(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSearchClicked,
                    modifier = Modifier
                        .bounceClick()
                        .size(44.dp)
                        .background(getGlassIconButtonBg(), CircleShape)
                        .border(0.5.dp, getGlassBorder(), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AppTextColor(),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSettingsClicked,
                    modifier = Modifier
                        .bounceClick()
                        .size(44.dp)
                        .background(getGlassIconButtonBg(), CircleShape)
                        .border(0.5.dp, getGlassBorder(), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = AppTextColor(),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

"""

content = content[:header_start] + new_header + content[header_end:]

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)

