import re
with open('app/src/main/java/com/example/ui/screens/EditHotelModal.kt', 'r') as f:
    content = f.read()

replacement = """                    Text(
                        text = if (hotel == null) "Add Hotel" else "Edit Hotel Info",
                        color = AppTextColor(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (hotel != null) {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(40.dp).background(getGlassIconButtonBg(), CircleShape).bounceClick()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = AppTextColor()
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(getDialogBackground())
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Move to Trash Bin", color = AccentPink) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.deleteHotel(hotel)
                                        onDismiss()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = AccentPink,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }"""

content = re.sub(
    r'                    Text\([\s\S]*?modifier = Modifier\.weight\(1f\)[\s\S]*?\)',
    replacement,
    content
)

with open('app/src/main/java/com/example/ui/screens/EditHotelModal.kt', 'w') as f:
    f.write(content)
print("Done")
