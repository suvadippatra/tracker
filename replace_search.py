import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    code = f.read()

# Replace GlobalSearchModal with a fully functional one
new_search = """@Composable
fun GlobalSearchModal(viewModel: FinancialViewModel, onDismiss: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val userProfile by viewModel.userProfile.collectAsState()
    val allExpenses by viewModel.allExtraExpenses.collectAsState()
    val allHotels by viewModel.allHotels.collectAsState()
    val allMenuItems by viewModel.allMenuItems.collectAsState()
    
    val filteredExpenses = remember(searchQuery, allExpenses) {
        if (searchQuery.isBlank()) emptyList()
        else allExpenses.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    val filteredHotels = remember(searchQuery, allHotels) {
        if (searchQuery.isBlank()) emptyList()
        else allHotels.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    val filteredMenu = remember(searchQuery, allMenuItems) {
        if (searchQuery.isBlank()) emptyList()
        else allMenuItems.filter { 
            it.englishName.contains(searchQuery, ignoreCase = true) || 
            it.banglaName.contains(searchQuery, ignoreCase = true) 
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(userProfile.themeMode))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color(0x1FFFFFFF), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTextColor())
                    }
                    Text(
                        text = if (userProfile.language == "bn") "অনুসন্ধান" else "Search",
                        color = AppTextColor(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(if (userProfile.language == "bn") "এখানে খুঁজুন..." else "Search here...", color = AppTextColor().copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = AppTextColor().copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = AppTextColor().copy(alpha = 0.5f))
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (searchQuery.isNotEmpty()) {
                    if (filteredExpenses.isEmpty() && filteredHotels.isEmpty() && filteredMenu.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (userProfile.language == "bn") "কোন ফলাফল পাওয়া যায়নি।" else "No results found.",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (filteredHotels.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (userProfile.language == "bn") "হোটেল" else "Hotels",
                                        color = AppTextColor(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(filteredHotels) { hotel ->
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text(text = hotel.name, color = AppTextColor(), fontWeight = FontWeight.Medium, modifier = Modifier.padding(16.dp))
                                    }
                                }
                            }
                            
                            if (filteredMenu.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (userProfile.language == "bn") "খাবার" else "Menu Items",
                                        color = AppTextColor(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(filteredMenu) { menu ->
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = if (userProfile.language == "bn") menu.banglaName else menu.englishName, color = AppTextColor(), fontWeight = FontWeight.Medium)
                                            Text(text = "₹${menu.price}", color = AppTextColor(), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            
                            if (filteredExpenses.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (userProfile.language == "bn") "খরচ" else "Expenses",
                                        color = AppTextColor(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(filteredExpenses) { expense ->
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = expense.name,
                                                    color = AppTextColor(),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val sdf = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                                                Text(
                                                    text = sdf.format(java.util.Date(expense.timestamp)),
                                                    color = AppTextColor().copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = "-₹${expense.amount}",
                                                color = Color(0xFFFF5252),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (userProfile.language == "bn") "হোটেল, খাবার বা খরচ খুঁজতে শুরু করুন।" else "Start typing to search for hotels, meals, or expenses.",
                            color = AppTextColor().copy(alpha = 0.4f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
"""

start_idx = code.find("@Composable\nfun GlobalSearchModal")
if start_idx != -1:
    end_idx = len(code)
    code = code[:start_idx] + new_search
    with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
        f.write(code)
    print("Replaced GlobalSearchModal")
else:
    print("Could not find GlobalSearchModal")
