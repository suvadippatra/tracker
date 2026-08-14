with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target = """                Button(
                    onClick = { viewModel.triggerQuickAction("ADD_HOTEL") },"""

repl = """                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showQrScanner = true },
                        modifier = Modifier
                            .background(AccentCyan.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.QrCodeScanner, contentDescription = "Receive via QR", tint = AccentCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.triggerQuickAction("ADD_HOTEL") },"""

content = content.replace(target, repl)

target_end_row = """                        Text(if (isBengali) "নতুন হোটেল" else "New Hotel")
                    }
                }
            }
        }
    }"""
    
repl_end_row = """                        Text(if (isBengali) "নতুন হোটেল" else "New Hotel")
                    }
                } // End inner Row
            }
        }
    }"""
content = content.replace(target_end_row, repl_end_row)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
