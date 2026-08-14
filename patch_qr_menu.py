with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target1 = """                                                            DropdownMenuItem(
                                                                text = { Text(if (isBengali) "নতুন হোটেল যোগ করুন" else "+ Add New Hotel", color = AccentCyan) },
                                                                onClick = {
                                                                    showCreateHotelModal = true
                                                                    openDropdownHotelId = null
                                                                }
                                                            )"""

repl1 = """                                                            DropdownMenuItem(
                                                                text = { Text(if (isBengali) "নতুন হোটেল যোগ করুন" else "+ Add New Hotel", color = AccentCyan) },
                                                                onClick = {
                                                                    showCreateHotelModal = true
                                                                    openDropdownHotelId = null
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text(if (isBengali) "কিউআর মাধ্যমে যোগ করুন" else "+ Import via QR", color = AccentCyan) },
                                                                onClick = {
                                                                    showQrScanner = true
                                                                    openDropdownHotelId = null
                                                                }
                                                            )"""

content = content.replace(target1, repl1)

target2 = """                                                        DropdownMenuItem(
                                                            text = { Text(if (isBengali) "নতুন হোটেল যোগ করুন" else "+ Add New Hotel", color = AccentCyan) },
                                                            onClick = {
                                                                showCreateHotelModal = true
                                                                openDropdownHotelId = null
                                                            }
                                                        )"""
repl2 = """                                                        DropdownMenuItem(
                                                            text = { Text(if (isBengali) "নতুন হোটেল যোগ করুন" else "+ Add New Hotel", color = AccentCyan) },
                                                            onClick = {
                                                                showCreateHotelModal = true
                                                                openDropdownHotelId = null
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text(if (isBengali) "কিউআর মাধ্যমে যোগ করুন" else "+ Import via QR", color = AccentCyan) },
                                                            onClick = {
                                                                showQrScanner = true
                                                                openDropdownHotelId = null
                                                            }
                                                        )"""
content = content.replace(target2, repl2)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
