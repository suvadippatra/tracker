with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

others_section = """
                    if (otherPeriodDishes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Other Dishes (${if (isNight) "Day" else "Night"})",
                            color = AppTextColor().copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val rows = otherPeriodDishes.chunked(2)
                            rows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    row.forEach { dish ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            DishCard(
                                                dish = dish,
                                                qty = quantities[dish.id] ?: 0,
                                                isSelected = selectedMainId == dish.id,
                                                onSelect = { viewModel.selectMainDish(dish.id) },
                                                onUpdateQty = { delta -> viewModel.updateItemQuantity(dish.id, delta) }
                                            )
                                        }
                                    }
                                    if (row.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
"""

content = content.replace("                    Spacer(modifier = Modifier.height(24.dp))\n\n                    // BILLING FOOTER", others_section + "\n                    // BILLING FOOTER")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
