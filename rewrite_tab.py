import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

start_marker = "// MODULE A: HOTEL MANAGEMENT"
end_marker = "// ==========================================\n// MODULE B: ADD NEW MEAL FLOW"

if start_marker in content and end_marker in content:
    pre = content.split(start_marker)[0]
    post = content.split(end_marker)[1]
    
    new_module = """// MODULE A: HOTEL MANAGEMENT
// ==========================================
@Composable
fun HotelManagementTab(viewModel: FinancialViewModel) {
    val hotels by viewModel.allHotels.collectAsState()
    val activeHotelId by viewModel.activeHotelId.collectAsState()
    val activeHotel by viewModel.activeHotel.collectAsState()
    val monthlyTotal by viewModel.activeMonthTotal.collectAsState()
    val popularMeals by viewModel.popularMeals.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var showHotelDropdown by remember { mutableStateOf(false) }
    var showMealHistory by remember { mutableStateOf(false) }
    var showAddMealFlow by remember { mutableStateOf(false) }

    // Dialog edit fields
    var showEditHotelDialog by remember { mutableStateOf(false) }
    var editHotelName by remember { mutableStateOf("") }
    var editHotelAdvanced by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // PRIMARY GLASS LEDGER CARD
            activeHotel?.let { hotel ->
                Box {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = hotel.name,
                                    color = AppTextColor(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Call",
                                        tint = AppTextColor(),
                                        modifier = Modifier.size(24.dp).padding(end = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = AppTextColor(),
                                        modifier = Modifier.size(24.dp).padding(end = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More",
                                        tint = AppTextColor(),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { showHotelDropdown = true }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "monthly expenditure",
                                    color = AppTextColor().copy(alpha = 0.9f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("₹ ", color = AppTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                                    RollingNumberText(
                                        value = monthlyTotal,
                                        style = TextStyle(
                                            color = AppTextColor(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showHotelDropdown,
                        onDismissRequest = { showHotelDropdown = false },
                        modifier = Modifier
                            .background(getDialogBackground())
                            .border(0.5.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                    ) {
                        hotels.forEach { h ->
                            DropdownMenuItem(
                                text = { Text(h.name, color = AppTextColor()) },
                                onClick = {
                                    viewModel.activeHotelId.value = h.id
                                    showHotelDropdown = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Edit this hotel", color = AccentCyan) },
                            onClick = {
                                editHotelName = hotel.name
                                editHotelAdvanced = hotel.advancedPayment.toString()
                                showEditHotelDialog = true
                                showHotelDropdown = false
                            }
                        )
                    }
                }
            } ?: run {
                // Empty state if no hotel selected/available
                EmptyStateCard(message = "No Hotels configured. Create one in Settings!")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BEHAVIORAL ANALYTICS CAROUSEL HEADER
            Text(
                text = "Frequently Ordered Meals",
                color = AppTextColor(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (popularMeals.isEmpty()) {
                // Informational Preset Recommendations
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        PresetMealCard(
                            title = "Veg Thali",
                            description = "Most Ordered Afternoon Meal",
                            tag = "Lunch Choice",
                            color = AccentCyan
                        )
                    }
                    item {
                        PresetMealCard(
                            title = "Roti & Chicken",
                            description = "High Frequency Night Choice",
                            tag = "Dinner Peak",
                            color = AccentPink
                        )
                    }
                    item {
                        PresetMealCard(
                            title = "Alu Paratha",
                            description = "Sunday Special Recommendation",
                            tag = "Weekend Fav",
                            color = AccentGold
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(popularMeals) { (name, count) ->
                        PresetMealCard(
                            title = name,
                            description = "Frequently ordered dish at this establishment",
                            tag = "$count times",
                            color = AccentCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // VIEW MEAL HISTORY TRIGGER BUTTON
            activeHotel?.let {
                GlassButton(
                    onClick = { showMealHistory = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    cornerRadius = 24.dp
                ) {
                    Text(
                        text = "View Meal History",
                        color = AppTextColor(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar and FAB
        }

        // FAB FOR ADDING NEW MEAL
        if (activeHotel != null) {
            FloatingActionButton(
                onClick = {
                    viewModel.resetOrderFlow()
                    showAddMealFlow = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 16.dp)
                    .bounceClick()
                    .testTag("add_meal_fab"),
                shape = CircleShape,
                containerColor = Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(AccentCyan, AccentPink)),
                            shape = CircleShape
                        )
                        .padding(2.dp)
                        .background(if (isAppLightMode()) Color.White else Color(0xFF0F0B26), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Meal",
                        tint = AccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    // Modal Overlays
    if (showMealHistory) {
        MealHistoryModal(
            viewModel = viewModel,
            onDismiss = { showMealHistory = false }
        )
    }

    if (showAddMealFlow) {
        AddMealFlow(
            viewModel = viewModel,
            onDismiss = { showAddMealFlow = false }
        )
    }

    // Modal to Edit active hotel info
    if (showEditHotelDialog && activeHotel != null) {
        var nameError by remember { mutableStateOf(false) }
        var advancedError by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showEditHotelDialog = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Edit Active Hotel",
                        color = AppTextColor(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = editHotelName,
                        onValueChange = { editHotelName = it; nameError = false },
                        label = { Text("Hotel Name", color = AppTextColor().copy(alpha = 0.6f)) },
                        isError = nameError,
                        textStyle = TextStyle(color = AppTextColor()),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = getGlassBorder(),
                            cursorColor = AccentCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editHotelAdvanced,
                        onValueChange = { editHotelAdvanced = it; advancedError = false },
                        label = { Text("Advanced Payment", color = AppTextColor().copy(alpha = 0.6f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = advancedError,
                        textStyle = TextStyle(color = AppTextColor()),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = getGlassBorder(),
                            cursorColor = AccentCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditHotelDialog = false }) {
                            Text("Cancel", color = AppTextColor().copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val advancedVal = editHotelAdvanced.toDoubleOrNull()
                                if (editHotelName.isBlank()) nameError = true
                                if (advancedVal == null || advancedVal < 0) advancedError = true
                                if (!nameError && !advancedError) {
                                    viewModel.updateActiveHotel(editHotelName, advancedVal ?: 0.0)
                                    showEditHotelDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text("Save", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

"""
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(pre + start_marker + "\n" + new_module + end_marker + post)
    print("Rewritten successfully")
else:
    print("Markers not found")
