import re

file_path = "app/src/main/java/com/example/ui/screens/HotelManagementTab.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace GlassCard for meal groups with a solid contrasting card
old_glass_1 = """                                com.example.ui.components.GlassCard(
                                    modifier = Modifier.width(150.dp),
                                    cornerRadius = 16.dp
                                ) {"""
new_solid_1 = """                                Card(
                                    modifier = Modifier.width(150.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E2638) else Color(0xFFE2E8F0))
                                ) {"""
content = content.replace(old_glass_1, new_solid_1)

old_glass_2 = """                                        com.example.ui.components.GlassCard(
                                            modifier = Modifier.width(140.dp),
                                            cornerRadius = 14.dp
                                        ) {"""
new_solid_2 = """                                        Card(
                                            modifier = Modifier.width(140.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E2638) else Color(0xFFE2E8F0))
                                        ) {"""
content = content.replace(old_glass_2, new_solid_2)

with open(file_path, "w") as f:
    f.write(content)
