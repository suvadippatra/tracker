import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

start_marker = "// PRIMARY GLASS LEDGER CARD"
end_marker = "            Spacer(modifier = Modifier.height(16.dp))\n\n            // BEHAVIORAL ANALYTICS CAROUSEL HEADER"

if start_marker in content and end_marker in content:
    pre = content.split(start_marker)[0]
    post = content.split(end_marker, 1)[1]
    
    new_card = """// PRIMARY GLASS LEDGER CARD
            activeHotel?.let { hotel ->
                Box {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = hotel.name,
                                    color = AppTextColor(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Call",
                                        tint = AppTextColor(),
                                        modifier = Modifier.size(24.dp).padding(end = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = AppTextColor(),
                                        modifier = Modifier.size(24.dp).padding(end = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More",
                                        tint = AppTextColor(),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { showHotelDropdown = true }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "monthly expenditure",
                                    color = AppTextColor().copy(alpha = 0.9f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("₹ ", color = AppTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                                    RollingNumberText(
                                        value = monthlyTotal,
                                        style = TextStyle(
                                            color = AppTextColor(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showHotelDropdown,
                        onDismissRequest = { showHotelDropdown = false },
                        modifier = Modifier
                            .background(getDialogBackground())
                            .border(0.5.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                    ) {
                        hotels.forEach { h ->
                            DropdownMenuItem(
                                text = { Text(h.name, color = AppTextColor()) },
                                onClick = {
                                    viewModel.activeHotelId.value = h.id
                                    showHotelDropdown = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Edit this hotel", color = AccentCyan) },
                            onClick = {
                                editHotelName = hotel.name
                                editHotelAdvanced = hotel.advancedPayment.toString()
                                showEditHotelDialog = true
                                showHotelDropdown = false
                            }
                        )
                    }
                }
            } ?: run {
                // Empty state if no hotel selected/available
                EmptyStateCard(message = "No Hotels configured. Create one in Settings!")
            }

"""
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(pre + new_card + end_marker + post)
    print("Rewritten successfully")
else:
    print("Markers not found")
