with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

if "coil.compose.AsyncImage" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\\nimport coil.compose.AsyncImage")

old_qr_view = """                if (h.qrCodeUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "QR/URI: ${h.qrCodeUri}", color = AccentCyan, fontSize = 14.sp)
                }"""
                
new_qr_view = """                if (h.qrCodeUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "QR Codes", color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        h.qrCodeUri.split(",").filter { it.isNotBlank() }.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "QR Code",
                                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
                            )
                        }
                    }
                }"""

content = content.replace(old_qr_view, new_qr_view)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
