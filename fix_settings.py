with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("viewModel.importMenusFromCsv(content, selectedHotelIdForImport)", "viewModel.importMenusFromCsv(content, selectedHotelIdForImport ?: 1)")
content = content.replace("viewModel.importHotelTransactionsFromCsv(content, selectedHotelIdForImport)", "viewModel.importHotelTransactionsFromCsv(content, selectedHotelIdForImport ?: 1)")

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
