with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("writer.write(\"Name,Price,Type,Emoji\\\n\")", "writer.write(\"Name,Price,Type,Emoji\\n\")")
content = content.replace("writer.write(\"${m.englishName},${m.price},${m.type},${m.emoji}\\\n\")", "writer.write(\"${m.englishName},${m.price},${m.type},${m.emoji}\\n\")")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
