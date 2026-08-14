with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

# 1. State for QrScannerDialog
target_state = '    var showHotelDetailsBottomSheet by remember { mutableStateOf(false) }'
repl_state = '    var showHotelDetailsBottomSheet by remember { mutableStateOf(false) }\n    var showQrScanner by remember { mutableStateOf(false) }'
content = content.replace(target_state, repl_state)

# 2. Add button in the top bar (which is inside a Row with text "Hotel Registry")
# Let's find it.
target_top_bar = """            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBengali) "হোটেল তালিকা" else "Hotel Registry",
                    color = AppTextColor(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { viewModel.triggerQuickAction("ADD_HOTEL") },"""

repl_top_bar = """            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBengali) "হোটেল তালিকা" else "Hotel Registry",
                    color = AppTextColor(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showQrScanner = true },
                        modifier = Modifier
                            .background(AccentCyan.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Receive via QR", tint = AccentCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.triggerQuickAction("ADD_HOTEL") },"""

content = content.replace(target_top_bar, repl_top_bar)
if target_top_bar not in content and repl_top_bar in content:
    print("Top bar already patched")

# Wait, button doesn't have closing brace for Row here, let's fix it by regex or simpler string replace
# Let's search for the exact block in HotelManagementTab
