import re

with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "r") as f:
    content = f.read()

# Replace GlassCard for meal group in PageBottom
content = content.replace('''                                GlassCard(
                                    modifier = Modifier.width(150.dp),
                                    cornerRadius = 16.dp
                                ) {''', '''                                Card(
                                    modifier = Modifier.width(150.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.9f))
                                ) {''')

# Replace GlassCard for meal group in bottom bar
content = content.replace('''                                    GlassCard(
                                        modifier = Modifier.width(140.dp),
                                        cornerRadius = 14.dp
                                    ) {''', '''                                    Card(
                                        modifier = Modifier.width(140.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.9f))
                                    ) {''')

with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "w") as f:
    f.write(content)
