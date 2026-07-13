import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Define the new EditHotelModal block
new_block = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHotelModal(
    hotel: com.example.data.model.HotelEntity,
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var editHotelName by remember { mutableStateOf(hotel.name) }
    var editHotelAdvanced by remember { mutableStateOf(hotel.advancedPayment.toString()) }
    var editPhone by remember { mutableStateOf(hotel.phoneNumber) }
    var editLocation by remember { mutableStateOf(hotel.location) }
    var editUpiId by remember { mutableStateOf(hotel.upiId) }
    var editQrCodeUri by remember { mutableStateOf(hotel.qrCodeUri) }
    var showAddMenu by remember { mutableStateOf(false) }

    val qrCodeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            editQrCodeUri = uri.toString()
        }
    }

    val menuItems by viewModel.allMenuItems.collectAsState(initial = emptyList())
    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
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
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Edit Hotel Info", color = AppTextColor(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = editHotelName,
                onValueChange = { editHotelName = it },
                label = { Text("Hotel Name", color = AppTextColor().copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = editHotelAdvanced,
                onValueChange = { editHotelAdvanced = it },
                label = { Text("Advanced Payment (₹)", color = AppTextColor().copy(alpha = 0.6f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = editPhone,
                onValueChange = { editPhone = it },
                label = { Text("Mobile Number", color = AppTextColor().copy(alpha = 0.6f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editLocation,
                    onValueChange = { editLocation = it },
                    label = { Text("Location", color = AppTextColor().copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val query = if (editLocation.isNotBlank()) android.net.Uri.encode(editLocation) else "Restaurants"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$query"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.background(AccentCyan.copy(alpha=0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Open Maps", tint = AccentCyan)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = editUpiId,
                onValueChange = { editUpiId = it },
                label = { Text("UPI ID", color = AppTextColor().copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editQrCodeUri,
                    onValueChange = { editQrCodeUri = it },
                    label = { Text("QR Code/UPI Scan URI", color = AppTextColor().copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { qrCodeLauncher.launch("image/*") },
                    modifier = Modifier.background(AccentCyan.copy(alpha=0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = "Pick QR Code", tint = AccentCyan)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Menu Chart", color = AppTextColor(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { csvImportLauncher.launch("text/csv") }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import CSV", tint = AppTextColor())
                    }
                    IconButton(onClick = { csvExportLauncher.launch("menu_${hotel.name.replace(" ", "_")}.csv") }) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = AppTextColor())
                    }
                    IconButton(onClick = { showAddMenu = !showAddMenu }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Menu", tint = AccentCyan)
                    }
                }
            }
            Text("Edit menu chart and items. You can add Day, Night, or Common meals.", color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                if (menuItems.isEmpty()) {
                    Text("No items added yet. Click '+' or import via CSV.", color = AppTextColor().copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                }
                menuItems.forEach { menuItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(getDialogBackground(), RoundedCornerShape(8.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${menuItem.emoji} ${menuItem.englishName}", color = AppTextColor(), fontWeight = FontWeight.Bold)
                            Text("₹${menuItem.price} • ${menuItem.type}", color = AppTextColor().copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentPink, modifier = Modifier.size(24.dp).clickable { viewModel.deleteMenu(menuItem) })
                    }
                }
            }
            
            if (showAddMenu) {
                var newName by remember { mutableStateOf("") }
                var newPrice by remember { mutableStateOf("") }
                var newEmoji by remember { mutableStateOf("🍽️") }
                var newType by remember { mutableStateOf("Day") } // Day, Night, Common
                
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add New Menu Item", color = AccentCyan, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newEmoji, onValueChange = { newEmoji = it },
                                label = { Text("Emoji", color = AppTextColor().copy(alpha = 0.5f)) },
                                modifier = Modifier.weight(0.3f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                            )
                            OutlinedTextField(
                                value = newName, onValueChange = { newName = it },
                                label = { Text("Name", color = AppTextColor().copy(alpha = 0.5f)) },
                                modifier = Modifier.weight(0.7f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newPrice, onValueChange = { newPrice = it },
                                label = { Text("Price (₹)", color = AppTextColor().copy(alpha = 0.5f)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.5f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor())
                            )
                            Row(modifier = Modifier.weight(0.5f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Day", "Night", "Common").forEach { typeOpt ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (newType == typeOpt) AccentCyan else Color.Transparent)
                                            .border(1.dp, AccentCyan, RoundedCornerShape(8.dp))
                                            .clickable { newType = typeOpt }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(typeOpt, color = if (newType == typeOpt) Color.Black else AccentCyan, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val p = newPrice.toDoubleOrNull() ?: 0.0
                                if (newName.isNotBlank()) {
                                    viewModel.addMenu(com.example.data.model.MenuEntity(
                                        banglaName = newName,
                                        englishName = newName,
                                        price = p,
                                        isDayMenu = newType == "Day" || newType == "Common",
                                        isNightMenu = newType == "Night" || newType == "Common",
                                        isAddOn = false,
                                        emoji = newEmoji,
                                        type = newType
                                    ))
                                    showAddMenu = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text("Save Menu Item", color = Color.Black)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AccentPink)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        val advanced = editHotelAdvanced.toDoubleOrNull() ?: hotel.advancedPayment
                        viewModel.updateHotel(hotel.copy(
                            name = editHotelName,
                            advancedPayment = advanced,
                            phoneNumber = editPhone,
                            location = editLocation,
                            upiId = editUpiId,
                            qrCodeUri = editQrCodeUri
                        ))
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("Save Changes", color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}"""

# We'll use a regex search and replace for the EditHotelModal block
import re
pattern = re.compile(r'@OptIn\(ExperimentalMaterial3Api::class\)\s*@Composable\s*fun EditHotelModal.*?^}', re.MULTILINE | re.DOTALL)
content = pattern.sub(new_block, content)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
