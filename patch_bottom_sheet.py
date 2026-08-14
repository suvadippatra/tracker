import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

# We want to add the button above "Close & Edit buttons"
target = """            // Close & Edit buttons"""

replacement = """
            // Export via QR Button
            Button(
                onClick = { 
                    viewModel.exportHotelViaQr(hotel.id) { result ->
                        when(result) {
                            is com.example.data.ExportResult.Success -> {
                                android.widget.Toast.makeText(context, "Export packaged: ${result.compressedSize} bytes. AES Key ready.", android.widget.Toast.LENGTH_LONG).show()
                                android.util.Log.d("QRExport", "Key: ${result.key.contentToString()}, Payload: ${result.payload.size} bytes")
                            }
                            is com.example.data.ExportResult.Error -> {
                                android.widget.Toast.makeText(context, "Export failed: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).bounceClick(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA68C8)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isBengali) "কিউআর মাধ্যমে এক্সপোর্ট" else "Export via QR", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Close & Edit buttons"""

if target in content:
    new_content = content.replace(target, replacement)
    
    # We also need to add viewModel parameter to HotelDetailsBottomSheet
    sig_target = """fun HotelDetailsBottomSheet(
    hotel: com.example.data.model.HotelEntity,
    isBengali: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
)"""
    sig_repl = """fun HotelDetailsBottomSheet(
    hotel: com.example.data.model.HotelEntity,
    isBengali: Boolean,
    viewModel: com.example.ui.viewmodel.FinancialViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
)"""
    new_content = new_content.replace(sig_target, sig_repl)
    
    # And update callers
    caller_target = """HotelDetailsBottomSheet(
                        hotel = hotel,
                        isBengali = isBengali,
                        onDismiss = { showInfoDialog = null },
                        onEdit = { 
                            showInfoDialog = null
                            hotelToEdit = hotel 
                        }
                    )"""
    caller_repl = """HotelDetailsBottomSheet(
                        hotel = hotel,
                        isBengali = isBengali,
                        viewModel = viewModel,
                        onDismiss = { showInfoDialog = null },
                        onEdit = { 
                            showInfoDialog = null
                            hotelToEdit = hotel 
                        }
                    )"""
    new_content = new_content.replace(caller_target, caller_repl)

    with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
        f.write(new_content)
    print("Patched HotelManagementTab.kt successfully")
else:
    print("Could not find target")
