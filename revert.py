import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# fix the broken IconButton on line 1471
broken_icon = """                IconButton(onClick = {
                        val intent = if (editLocation.startsWith("http://") || editLocation.startsWith("https://"), modifier = Modifier.bounceClick()) {
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(editLocation))
                        } else {
                            val query = if (editLocation.isNotBlank()) android.net.Uri.encode(editLocation) else "Restaurants"
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$query"))
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.background(AccentCyan.copy(alpha=0.1f), CircleShape)
                ) {"""

fixed_icon = """                IconButton(onClick = {
                        val intent = if (editLocation.startsWith("http://") || editLocation.startsWith("https://")) {
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(editLocation))
                        } else {
                            val query = if (editLocation.isNotBlank()) android.net.Uri.encode(editLocation) else "Restaurants"
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$query"))
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.background(AccentCyan.copy(alpha=0.1f), CircleShape).bounceClick()
                ) {"""
content = content.replace(broken_icon, fixed_icon)

# Revert hapticClickable
content = content.replace(".hapticClickable {", ".clickable {")

# Fix modifier issues
content = re.sub(r'modifier\s*=\s*Modifier\.background\(AccentCyan\.copy\(alpha=0\.1f\),\s*CircleShape\), modifier = Modifier\.bounceClick\(\)', r'modifier = Modifier.background(AccentCyan.copy(alpha=0.1f), CircleShape).bounceClick()', content)
content = re.sub(r'modifier\s*=\s*Modifier\.background\(AccentCyan\.copy\(alpha\s*=\s*0\.1f\),\s*RoundedCornerShape\(8\.dp\)\), modifier = Modifier\.bounceClick\(\)', r'modifier = Modifier.background(AccentCyan.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).bounceClick()', content)
content = re.sub(r'modifier\s*=\s*Modifier\.background\(Color\(0x15FF4081\),\s*CircleShape\), modifier = Modifier\.bounceClick\(\)', r'modifier = Modifier.background(Color(0x15FF4081), CircleShape).bounceClick()', content)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
