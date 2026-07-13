with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Fix global states
old_states = """    var showHotelDropdown by remember { mutableStateOf(false) }
    var showMealHistory by remember { mutableStateOf(false) }
    var showAddMealFlow by remember { mutableStateOf(false) }

    // Dialog edit fields
    var showEditHotelDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf<HotelEntity?>(null) }"""

new_states = """    var openDropdownHotelId by remember { mutableStateOf<Int?>(null) }
    var showMealHistory by remember { mutableStateOf(false) }
    var showAddMealFlow by remember { mutableStateOf(false) }

    // Dialog edit fields
    var hotelToEdit by remember { mutableStateOf<com.example.data.model.HotelEntity?>(null) }
    var showInfoDialog by remember { mutableStateOf<com.example.data.model.HotelEntity?>(null) }"""
content = content.replace(old_states, new_states)

# Fix horizontal pager beyondBoundsPageCount
old_pager = """                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {"""
new_pager = """                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    beyondBoundsPageCount = 1
                ) {"""
content = content.replace(old_pager, new_pager)

# Fix Box wrapContentSize and Dropdown
old_dropdown = """                                        Box {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "More",
                                                tint = AppTextColor(),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable { showHotelDropdown = true }
                                            )
                                            if (hotel.id == activeHotelId) {
                                                DropdownMenu(
                                                    expanded = showHotelDropdown,
                                                    onDismissRequest = { showHotelDropdown = false },
                                                    modifier = Modifier
                                                        .background(getDialogBackground())
                                                        .border(0.5.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Edit this hotel", color = AccentCyan) },
                                                        onClick = {
                                                            showEditHotelDialog = true
                                                            showHotelDropdown = false
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                        onClick = {
                                                            pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                            showHotelDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }"""

new_dropdown = """                                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "More",
                                                tint = AppTextColor(),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable { openDropdownHotelId = hotel.id }
                                            )
                                            DropdownMenu(
                                                expanded = openDropdownHotelId == hotel.id,
                                                onDismissRequest = { openDropdownHotelId = null },
                                                modifier = Modifier
                                                    .background(getDialogBackground())
                                                    .border(0.5.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Edit this hotel", color = AccentCyan) },
                                                    onClick = {
                                                        hotelToEdit = hotel
                                                        openDropdownHotelId = null
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Export transactions (PDF)", color = AppTextColor()) },
                                                    onClick = {
                                                        pdfExportLauncher.launch("transactions_${hotel.name.replace(" ", "_")}.pdf")
                                                        openDropdownHotelId = null
                                                    }
                                                )
                                            }
                                        }"""
content = content.replace(old_dropdown, new_dropdown)

# Fix hotelToEdit dialog call
old_edit_dialog = """    // Modal Sheet for Editing Hotel Parameters
    if (showEditHotelDialog && activeHotel != null) {
        EditHotelModal(
            hotel = activeHotel!!,
            viewModel = viewModel,
            onDismiss = { showEditHotelDialog = false }
        )
    }"""
new_edit_dialog = """    // Modal Sheet for Editing Hotel Parameters
    if (hotelToEdit != null) {
        EditHotelModal(
            hotel = hotelToEdit!!,
            viewModel = viewModel,
            onDismiss = { hotelToEdit = null }
        )
    }"""
content = content.replace(old_edit_dialog, new_edit_dialog)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
