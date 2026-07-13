with open("app/src/main/java/com/example/data/model/Entities.kt", "r") as f:
    content = f.read()

content = content.replace("val banglaName: String,", "val hotelId: Int = 1,\n    val banglaName: String,")

with open("app/src/main/java/com/example/data/model/Entities.kt", "w") as f:
    f.write(content)
