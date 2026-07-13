with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

dish_card = """
@Composable
fun DishCard(
    dish: com.example.data.model.MenuEntity,
    qty: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdateQty: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 130.dp)
            .bounceClick()
            .background(
                brush = if (isSelected) {
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x4000E5FF), Color(0x1F00E5FF)))
                } else {
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x1FFFFFFF), Color(0x09FFFFFF)))
                },
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) AccentCyan else Color(0x2BFFFFFF),
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dish.banglaName,
                        color = AppTextColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dish.englishName,
                        color = AppTextColor().copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "₹${dish.price.toInt()}",
                    color = AccentOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Quantity Controls
            if (qty == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .clickable { onUpdateQty(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add +", color = AppTextColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .border(0.5.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onUpdateQty(-1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "-", tint = AccentCyan, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "$qty",
                        color = AppTextColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onUpdateQty(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "+", tint = AccentCyan, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
"""

if "fun DishCard(" not in content:
    content += "\n" + dish_card

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
