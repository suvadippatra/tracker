import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

start_marker = "    // Dialog for Editing Hotel Parameters"
end_marker = "    if (showEditHotelDialog && activeHotel != null) {"

new_content = """    // Modal Sheet for Editing Hotel Parameters
    if (showEditHotelDialog && activeHotel != null) {
        EditHotelModal(
            hotel = activeHotel!!,
            viewModel = viewModel,
            onDismiss = { showEditHotelDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHotelModal(
    hotel: com.example.data.model.HotelEntity,
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    var editHotelName by remember { mutableStateOf(hotel.name) }
    var editHotelAdvanced by remember { mutableStateOf(hotel.advancedPayment.toString()) }
    var editPhone by remember { mutableStateOf(hotel.phoneNumber) }
    var editLocation by remember { mutableStateOf(hotel.location) }
    var editUpiId by remember { mutableStateOf(hotel.upiId) }

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
            Text("Edit Hotel Info", color = AppTextColor(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
            OutlinedTextField(
                value = editLocation,
                onValueChange = { editLocation = it },
                label = { Text("Location", color = AppTextColor().copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTextColor(), unfocusedTextColor = AppTextColor(),
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Menu Chart", color = AppTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = Icons.Default.Fastfood,
                    contentDescription = "Edit Menu",
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp).clickable { /* TODO Edit Menu Flow */ }
                )
            }
            Text("Edit menu chart and items from settings.", color = AppTextColor().copy(alpha = 0.6f), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AppTextColor().copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val adv = editHotelAdvanced.toDoubleOrNull() ?: 0.0
                        viewModel.editHotel(hotel.copy(
                            name = editHotelName,
                            advancedPayment = adv,
                            phoneNumber = editPhone,
                            location = editLocation,
                            upiId = editUpiId
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
}
"""

if start_marker in content:
    pre = content.split(start_marker)[0]
    post = content.split("    // Dialog for Editing Hotel Parameters", 1)[1]
    post = post.split("    }\n}\n", 1)[1]
    
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(pre + new_content + post)
    print("Replaced Edit Hotel Dialog")
else:
    print("Markers not found")
