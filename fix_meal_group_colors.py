import re

with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "r") as f:
    content = f.read()

target1 = """                                    Card(
                                        modifier = Modifier.width(140.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = AppTextColor().copy(alpha = 0.15f))
                                    ) {"""

replacement1 = """                                    val isLight = com.example.ui.components.isAppLightMode()
                                    Card(
                                        modifier = Modifier.width(140.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E2638))
                                    ) {"""

if target1 in content:
    content = content.replace(target1, replacement1)
    print("Replaced 1")
else:
    print("Target 1 not found")

target2 = """                                Card(
                                    modifier = Modifier.width(150.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E2638) else Color(0xFFE2E8F0))
                                ) {"""

replacement2 = """                                val isLight = com.example.ui.components.isAppLightMode()
                                Card(
                                    modifier = Modifier.width(150.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E2638))
                                ) {"""

if target2 in content:
    content = content.replace(target2, replacement2)
    print("Replaced 2")
else:
    print("Target 2 not found")


with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "w") as f:
    f.write(content)
