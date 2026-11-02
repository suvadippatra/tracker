import re

with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "r") as f:
    content = f.read()

# Replace first Card
old_1 = """                                Card(
                                    modifier = Modifier.width(150.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = AppTextColor().copy(alpha = 0.15f))
                                ) {"""
new_1 = """                                com.example.ui.components.GlassCard(
                                    modifier = Modifier.width(150.dp),
                                    cornerRadius = 16.dp
                                ) {"""
content = content.replace(old_1, new_1)

# Replace second Card
old_2 = """                                        Card(
                                            modifier = Modifier.width(140.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = AppTextColor().copy(alpha = 0.15f))
                                        ) {"""
new_2 = """                                        com.example.ui.components.GlassCard(
                                            modifier = Modifier.width(140.dp),
                                            cornerRadius = 14.dp
                                        ) {"""
content = content.replace(old_2, new_2)

with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "w") as f:
    f.write(content)
