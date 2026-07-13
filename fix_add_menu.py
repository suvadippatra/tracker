with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

content = content.replace("viewModel.addMenu(com.example.data.model.MenuEntity(\n                                banglaName = name,", "viewModel.addMenu(com.example.data.model.MenuEntity(\n                                hotelId = viewModel.activeHotelId.value,\n                                banglaName = name,")
content = content.replace("viewModel.addMenu(com.example.data.model.MenuEntity(\n                                        banglaName = newName,", "viewModel.addMenu(com.example.data.model.MenuEntity(\n                                        hotelId = viewModel.activeHotelId.value,\n                                        banglaName = newName,")

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
