import re

with open("app/src/main/java/com/example/data/model/Entities.kt", "r") as f:
    content = f.read()

content = content.replace(
    'val defaultHotelId: Int = 1',
    'val defaultHotelId: Int = 1,\n    val enableStarAnimation: Boolean = true'
)

with open("app/src/main/java/com/example/data/model/Entities.kt", "w") as f:
    f.write(content)
