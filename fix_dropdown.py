import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# 1. Add hotelMenuOffsets
old_state = "var openDropdownHotelId by remember { mutableStateOf<Int?>(null) }"
new_state = "var openDropdownHotelId by remember { mutableStateOf<Int?>(null) }\n    var hotelMenuOffsets by remember { mutableStateOf(mapOf<Int, androidx.compose.ui.geometry.Offset>()) }"
content = content.replace(old_state, new_state)

# 2. Modify the Icon to record its position and remove DropdownMenu from here
old_icon_menu = """                                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "More",
                                                tint = AppTextColor(),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable { openDropdownHotelId = hotel.id }
                                            )
                                            DropdownMenu(
                                                expanded = openDropdownHotelId == hotel.id,
                                                onDismissRequest = { openDropdownHotelId = null },
                                                modifier = Modifier
                                                    .background(getDialogBackground())
                                                    .border(0.5.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Edit this hotel", color = AccentCyan) },
                                                    onClick = {
                                                        hotelToEdit = hotel
                                                        openDropdownHotelId = null
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                    onClick = {
                                                        pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                        openDropdownHotelId = null
                                                    }
                                                )
                                            }
                                        }"""

new_icon = """                                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "More",
                                                tint = AppTextColor(),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .onGloballyPositioned { coordinates ->
                                                        val pos = coordinates.positionInRoot()
                                                        if (hotelMenuOffsets[hotel.id] != pos) {
                                                            hotelMenuOffsets = hotelMenuOffsets.toMutableMap().apply { put(hotel.id, pos) }
                                                        }
                                                    }
                                                    .clickable { openDropdownHotelId = hotel.id }
                                            )
                                        }"""
content = content.replace(old_icon_menu, new_icon)

# 3. Add the DropdownMenu at the end of the root Box
old_fab_end = """                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isAppLightMode()) Color.White else Color(0xFF0F0B26), shape = CircleShape)
                        .border(2.dp, AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Meal",
                        tint = AccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }"""

new_fab_end = """                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isAppLightMode()) Color.White else Color(0xFF0F0B26), shape = CircleShape)
                        .border(2.dp, AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Meal",
                        tint = AccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        
        if (openDropdownHotelId != null) {
            val hId = openDropdownHotelId
            val h = hotels.find { it.id == hId }
            val offset = hotelMenuOffsets[hId] ?: androidx.compose.ui.geometry.Offset.Zero
            if (h != null) {
                Box(
                    modifier = Modifier
                        .offset { androidx.compose.ui.unit.IntOffset(offset.x.toInt(), offset.y.toInt()) }
                        .size(24.dp)
                ) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { openDropdownHotelId = null },
                        modifier = Modifier
                            .background(getDialogBackground())
                            .border(0.5.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit this hotel", color = AccentCyan) },
                            onClick = {
                                hotelToEdit = h
                                openDropdownHotelId = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                            onClick = {
                                pdfExportLauncher.launch("transactions_${h.name.replace(" ", "_")}.pdf")
                                openDropdownHotelId = null
                            }
                        )
                    }
                }
            }
        }
    }"""
content = content.replace(old_fab_end, new_fab_end)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)

