import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_history = """fun MealHistoryModal(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val transactions by viewModel.activeHotelTransactions.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Dialog("""

new_history = """fun MealHistoryModal(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val transactions by viewModel.activeHotelTransactions.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    var monthOffset by remember { mutableStateOf(0) }
    
    val filteredTransactions = remember(transactions, monthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        val targetMonth = cal.get(Calendar.MONTH)
        val targetYear = cal.get(Calendar.YEAR)
        
        transactions.filter {
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = it.timestamp
            txCal.get(Calendar.MONTH) == targetMonth && txCal.get(Calendar.YEAR) == targetYear
        }
    }
    
    val currentDisplayMonth = remember(monthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        monthFormat.format(cal.time)
    }

    Dialog("""

content = content.replace(old_history, new_history)

old_list = """                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        EmptyStateCard(message = "No past meal logs registered for this hotel.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transactions) { tx ->"""

new_list = """                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { monthOffset -= 1 }) {
                        Icon(Icons.Default.ArrowBack, "Previous Month", tint = AppTextColor())
                    }
                    Text(currentDisplayMonth, color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { monthOffset += 1 }, enabled = monthOffset < 0) {
                        Icon(Icons.Default.ArrowForward, "Next Month", tint = if (monthOffset < 0) AppTextColor() else AppTextColor().copy(alpha = 0.3f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (filteredTransactions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        EmptyStateCard(message = "No past meal logs for $currentDisplayMonth.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTransactions) { tx ->"""

content = content.replace(old_list, new_list)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
