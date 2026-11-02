import re

with open("app/src/main/java/com/example/ui/screens/ExtraExpensesTab.kt", "r") as f:
    content = f.read()

# We need to replace the content of if (showQRDialog) { ... }
# Let's find the boundaries of the dialog content.
start_str = """                        if (showQRDialog) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current"""

# The dialog ends where `if (showFullScreenQR)` starts
end_str = """                        if (showFullScreenQR) {"""

parts = content.split(start_str)
if len(parts) > 1:
    before = parts[0]
    rest = parts[1]
    
    parts_end = rest.split(end_str)
    if len(parts_end) > 1:
        after = parts_end[1]
        
        new_dialog = """                        if (showQRDialog) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            
                            ModalBottomSheet(
                                onDismissRequest = { showQRDialog = false },
                                containerColor = getSettingsItemBg(),
                                dragHandle = { BottomSheetDefaults.DragHandle() },
                                windowInsets = WindowInsets.safeDrawing
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .background(AccentCyan.copy(alpha = 0.2f), CircleShape)
                                                    .border(1.dp, AccentCyan.copy(alpha = 0.5f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = account.name,
                                                    color = AppTextColor(),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Account Details & Information",
                                                    color = AppSecondaryTextColor(),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { showQRDialog = false },
                                            modifier = Modifier.size(36.dp).background(getGlassIconButtonBg(), CircleShape).bounceClick()
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTextColor(), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Details Card
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = getSettingsItemBg()),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, getGlassBorder()),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            
                                            // Account Number
                                            if (account.accountNumber.isNotBlank()) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Numbers, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("Account Number", color = AppSecondaryTextColor(), fontSize = 11.sp)
                                                            Text(account.accountNumber, color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(account.accountNumber))
                                                            android.widget.Toast.makeText(context, "Account # copied!", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(32.dp).bounceClick()
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AccentCyan, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                HorizontalDivider(color = getGlassBorder())
                                            }

                                            // IFSC Code
                                            if (account.ifsc.isNotBlank()) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Code, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("IFSC Code", color = AppSecondaryTextColor(), fontSize = 11.sp)
                                                            Text(account.ifsc, color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(account.ifsc))
                                                            android.widget.Toast.makeText(context, "IFSC copied!", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(32.dp).bounceClick()
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AccentCyan, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                HorizontalDivider(color = getGlassBorder())
                                            }

                                            // UPI ID
                                            if (account.upiId.isNotBlank()) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("UPI ID", color = AppSecondaryTextColor(), fontSize = 11.sp)
                                                            Text(account.upiId, color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(account.upiId))
                                                            android.widget.Toast.makeText(context, "UPI ID copied!", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(32.dp).bounceClick()
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AccentCyan, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Financial Summary Card
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = getSettingsItemBg()),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, getGlassBorder()),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Text("Financial Summary", color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Opening Balance", color = AppSecondaryTextColor(), fontSize = 12.sp)
                                                Text("₹${"%,.0f".format(accInitBal)}", color = AppTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            }
                                            
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Total Income", color = AppSecondaryTextColor(), fontSize = 12.sp)
                                                Text("+₹${"%,.0f".format(accIncome)}", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Total Expense", color = AppSecondaryTextColor(), fontSize = 12.sp)
                                                Text("-₹${"%,.0f".format(accExpense)}", color = AccentPink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            }
                                            
                                            HorizontalDivider(color = getGlassBorder())

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Net Balance", color = AppTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text("₹${"%,.0f".format(computedAccBalance)}", color = AccentCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // QR Code
                                    if (account.qrCodeUri.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = getSettingsItemBg()),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, getGlassBorder()),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Payment QR Code", color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(180.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(Color.White)
                                                        .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                                        .clickable { showFullScreenQR = true }
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AsyncImage(
                                                        model = account.qrCodeUri,
                                                        contentDescription = "QR Code",
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Button(
                                        onClick = { showQRDialog = false },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).bounceClick(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                                    ) {
                                        Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                        
""" + end_str + after
        
        with open("app/src/main/java/com/example/ui/screens/ExtraExpensesTab.kt", "w") as f:
            f.write(before + new_dialog)
            
