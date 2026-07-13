import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

old_header = """    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
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
                        .padding(2.dp)
                        .background(if (isAppLightMode()) Color.White else Color(0xFF0F0B26), shape = CircleShape)
                        .clip(CircleShape)
                        .clickable { onSettingsClicked() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = AccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = greeting,
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
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
                        .border(1.dp, getGlassBorder(), CircleShape)
                        .testTag("search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AppTextColor(),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onSettingsClicked,
                    modifier = Modifier
                        .bounceClick()
                        .size(44.dp)
                        .background(getGlassIconButtonBg(), CircleShape)
                        .border(1.dp, getGlassBorder(), CircleShape)
                        .testTag("settings_button")
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
    }"""

new_header = """    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Avatar with high-contrast colored glass ring
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(AccentCyan, AccentPink)),
                                shape = CircleShape
                            )
                            .padding(2.dp)
                            .background(if (isAppLightMode()) Color.White else Color(0xFF0F0B26), shape = CircleShape)
                            .clip(CircleShape)
                            .clickable { onSettingsClicked() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.name.isNotBlank()) {
                            Text(
                                text = userProfile.name.first().uppercase(),
                                color = AccentCyan,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                tint = AccentCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = greeting,
                            color = AppTextColor().copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = userProfile.name.ifBlank { "User" },
                            color = AppTextColor(),
                            fontSize = 18.sp,
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
                            .size(40.dp)
                            .background(getGlassIconButtonBg(), CircleShape)
                            .border(1.dp, getGlassBorder(), CircleShape)
                            .testTag("search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AppTextColor(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSettingsClicked,
                        modifier = Modifier
                            .bounceClick()
                            .size(40.dp)
                            .background(getGlassIconButtonBg(), CircleShape)
                            .border(1.dp, getGlassBorder(), CircleShape)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AppTextColor(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }"""

if old_header in code:
    code = code.replace(old_header, new_header)
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(code)
    print("Header updated again")
else:
    print("Header not found")

