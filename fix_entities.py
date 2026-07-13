import re

with open("app/src/main/java/com/example/data/model/Entities.kt", "r") as f:
    content = f.read()

content = content.replace("val name: String,\n    val advancedPayment: Double = 0.0", "val name: String,\n    val advancedPayment: Double = 0.0,\n    val phoneNumber: String = \"\",\n    val location: String = \"\",\n    val upiId: String = \"\"")

content = content.replace("val isAddOn: Boolean", "val isAddOn: Boolean,\n    val emoji: String = \"🍽️\",\n    val type: String = \"Common\"")

with open("app/src/main/java/com/example/data/model/Entities.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/data/AppDatabase.kt", "r") as f:
    content = f.read()
content = content.replace("version = 3", "version = 4")
with open("app/src/main/java/com/example/data/AppDatabase.kt", "w") as f:
    f.write(content)

print("Entities updated")
