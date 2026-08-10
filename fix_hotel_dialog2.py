import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target = """            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentCyan.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hotel == null) Icons.Default.Add else Icons.Default.Edit,
                        contentDescription = null,
                        tint = AccentCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (hotel == null) {
                        if (prefLanguage == "bn") "নতুন হোটেল যুক্ত করুন" else "Register Establishment"
                    } else {
                        if (prefLanguage == "bn") "হোটেল সংশোধন" else "Edit Establishment Details"
                    },
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },"""

content = content.replace(target, "")

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
