with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("@Composable\nfun HotelManagementTab", "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun HotelManagementTab")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
