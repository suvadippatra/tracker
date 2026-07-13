with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_csv = """    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write("Name,Price,Type,Emoji\\n")
                    menuItems.forEach { m ->
                        writer.write("${m.englishName},${m.price},${m.type},${m.emoji}\\n")
                    }
                }
                android.widget.Toast.makeText(context, "Menu Exported", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export Failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val parts = line.split(",")
                        if (parts.size >= 4) {
                            val name = parts[0]
                            val price = parts[1].toDoubleOrNull() ?: 0.0
                            val type = parts[2]
                            val emoji = parts[3]
                            viewModel.addMenu(com.example.data.model.MenuEntity(
                                banglaName = name,
                                englishName = name,
                                price = price,
                                isDayMenu = type == "Day" || type == "Common",
                                isNightMenu = type == "Night" || type == "Common",
                                isAddOn = false,
                                emoji = emoji,
                                type = type
                            ))
                        }
                    }
                }
                android.widget.Toast.makeText(context, "Menu Imported", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import Failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }"""

new_csv = """    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        for (char in line) {
            if (char == '\\"') {
                inQuotes = !inQuotes
            } else if (char == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current = StringBuilder()
            } else {
                current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result.map { it.replace("\"\"", "\"") }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write("Name,Price,Type,Emoji\\n")
                    menuItems.forEach { m ->
                        val safeName = if (m.englishName.contains(",") || m.englishName.contains("\\"")) {
                            "\\"" + m.englishName.replace("\\"", "\\"\\"") + "\\""
                        } else {
                            m.englishName
                        }
                        writer.write("${safeName},${m.price},${m.type},${m.emoji}\\n")
                    }
                }
                android.widget.Toast.makeText(context, "Menu Exported", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export Failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                    lines.drop(1).forEach { line ->
                        if (line.isNotBlank()) {
                            val parts = parseCsvLine(line)
                            if (parts.size >= 4) {
                                val name = parts[0]
                                val price = parts[1].toDoubleOrNull() ?: 0.0
                                val type = parts[2]
                                val emoji = parts[3]
                                viewModel.addMenu(com.example.data.model.MenuEntity(
                                    banglaName = name,
                                    englishName = name,
                                    price = price,
                                    isDayMenu = type.equals("Day", ignoreCase = true) || type.equals("Common", ignoreCase = true),
                                    isNightMenu = type.equals("Night", ignoreCase = true) || type.equals("Common", ignoreCase = true),
                                    isAddOn = false,
                                    emoji = emoji,
                                    type = type
                                ))
                            }
                        }
                    }
                }
                android.widget.Toast.makeText(context, "Menu Imported", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import Failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }"""

content = content.replace(old_csv, new_csv)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
