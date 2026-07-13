with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

import re

# We will regex replace the `Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { ... val rows = mainDishes.chunked(2) ... }`
# since the Box is quite long, I'll extract it using a regex.
grid_regex = r"Column\(verticalArrangement = Arrangement\.spacedBy\(10\.dp\)\) \{\s*val rows = mainDishes\.chunked\(2\)\s*rows\.forEach \{ row ->\s*Row\([\s\S]*?if \(row\.size < 2\) \{\s*Spacer\(modifier = Modifier\.weight\(1f\)\)\s*\}\s*\}\s*\}\s*\}"

new_grid = """Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val rows = mainDishes.chunked(2)
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
                    }"""

content = re.sub(grid_regex, new_grid, content)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
