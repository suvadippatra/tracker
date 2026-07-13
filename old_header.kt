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
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Avatar with high-contrast colored glass ring
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(AccentCyan, AccentPink)),
                                shape = CircleShape
                            )
                            .padding(2.dp)
                            .background(Color(0xFF0F0B26), shape = CircleShape)
                            .clip(CircleShape)
                            .clickable { onSettingsClicked() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = AccentCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = greeting,
                            color = AppTextColor().copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (userProfile.language == "bn") "স্মৃতি পকেট" else "Smriti Pocket",
                            color = AppTextColor(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSearchClicked,
                        modifier = Modifier
                            .bounceClick()
                            .background(getGlassBorder(), CircleShape)
                            .testTag("search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AppTextColor()
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSettingsClicked,
                        modifier = Modifier
                            .bounceClick()
                            .background(getGlassBorder(), CircleShape)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AppTextColor()
                        )
                    }
                }
            }
        }
