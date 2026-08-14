with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

# Remove it from HotelDetailsBottomSheet
target_remove = """    if (showQrScanner) {
        com.example.ui.components.QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onSuccess = { payload: ByteArray, key: ByteArray, iv: ByteArray ->
                showQrScanner = false
                viewModel.importHotelViaQr(payload, key, iv) { result ->
                    when (result) {
                        is com.example.data.ImportResult.Success -> {
                            android.widget.Toast.makeText(context, "Hotel imported successfully!", android.widget.Toast.LENGTH_LONG).show()
                        }
                        is com.example.data.ImportResult.Error -> {
                            android.widget.Toast.makeText(context, "Import failed: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }"""
content = content.replace(target_remove, "")

# Now add it to the end of HotelManagementTab composable
# Let's find the end of HotelManagementTab
# The last part of HotelManagementTab before HotelDetailsBottomSheet is:
target_add = """    if (showCreateHotelModal || hotelToEdit != null) {
        EditHotelModal(
            hotel = hotelToEdit,
            viewModel = viewModel,
            onDismiss = {
                showCreateHotelModal = false
                hotelToEdit = null
            },
            onManageMenu = {
                manageMenuForHotel = it
                showCreateHotelModal = false
                hotelToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)"""

repl_add = """    if (showCreateHotelModal || hotelToEdit != null) {
        EditHotelModal(
            hotel = hotelToEdit,
            viewModel = viewModel,
            onDismiss = {
                showCreateHotelModal = false
                hotelToEdit = null
            },
            onManageMenu = {
                manageMenuForHotel = it
                showCreateHotelModal = false
                hotelToEdit = null
            }
        )
    }

    if (showQrScanner) {
        val context = androidx.compose.ui.platform.LocalContext.current
        com.example.ui.components.QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onSuccess = { payload: ByteArray, key: ByteArray, iv: ByteArray ->
                showQrScanner = false
                viewModel.importHotelViaQr(payload, key, iv) { result ->
                    when (result) {
                        is com.example.data.ImportResult.Success -> {
                            android.widget.Toast.makeText(context, "Hotel imported successfully!", android.widget.Toast.LENGTH_LONG).show()
                        }
                        is com.example.data.ImportResult.Error -> {
                            android.widget.Toast.makeText(context, "Import failed: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)"""
content = content.replace(target_add, repl_add)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)
