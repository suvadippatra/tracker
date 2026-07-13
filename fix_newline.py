with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("Modifier\\nimport", "Modifier\\nimport") # wait, no.
