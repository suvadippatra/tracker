with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

prefix = """                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "More",
                                                tint = AppTextColor(),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable { showHotelDropdown = true }
                                            )
                    }
                }
            } else {"""

restored_content = """                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "More",
                                                tint = AppTextColor(),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable { showHotelDropdown = true }
                                            )
                                            if (hotel.id == activeHotelId) {
                                                DropdownMenu(
                                                    expanded = showHotelDropdown,
                                                    onDismissRequest = { showHotelDropdown = false },
                                                    modifier = Modifier
                                                        .background(getDialogBackground())
                                                        .border(0.5.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Edit this hotel", color = AccentCyan) },
                                                        onClick = {
                                                            showEditHotelDialog = true
                                                            showHotelDropdown = false
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                        onClick = {
                                                            pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                            showHotelDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
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
                                            value = if (hotel.id == activeHotelId) monthlyTotal else 0.0,
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
                    }
                }
            } else {"""

content = content.replace(prefix, restored_content)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
