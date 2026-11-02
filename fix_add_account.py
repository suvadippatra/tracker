import re

with open("app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt", "r") as f:
    content = f.read()

# I will replace the AlertDialog of AddAccountDialog entirely
# Find the start of AlertDialog(
start_idx = content.find("    AlertDialog(\n        onDismissRequest = onDismiss,")
if start_idx == -1:
    print("Could not find AddAccountDialog AlertDialog")
    exit(1)

# Find the end of AddAccountDialog
# The function ends around 1750, it ends with:
#         dismissButton = {
#             TextButton(onClick = onDismiss) {
#                 Text("Cancel", color = AppSecondaryTextColor())
#             }
#         }
#     )
# }
end_str = '        }\n    )\n}'
end_idx = content.find(end_str, start_idx) + len(end_str)

if end_idx < start_idx:
    print("Could not find end of AddAccountDialog")
    exit(1)

old_alert_dialog = content[start_idx:end_idx]

# We need to construct a new full screen dialog
new_dialog = """    val themeMode = com.example.ui.components.LocalAppTheme.current
    val bgPreset = com.example.ui.components.LocalAppBgPreset.current
    val customBgUri = com.example.ui.components.LocalAppCustomBgUri.current
    val isLiquidGlass = com.example.ui.components.LocalAppLiquidGlassTheme.current

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()
        androidx.compose.runtime.CompositionLocalProvider(
            com.example.ui.components.LocalAppTheme provides themeMode,
            com.example.ui.components.LocalAppBgPreset provides bgPreset,
            com.example.ui.components.LocalAppCustomBgUri provides customBgUri,
            com.example.ui.components.LocalAppLiquidGlassTheme provides isLiquidGlass
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                Box(
                    modifier = Modifier.fillMaxSize().background(com.example.ui.components.getAppBackgroundGradient(themeMode, bgPreset)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize().systemBarsPadding(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0.dp),
                        topBar = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp).background(com.example.ui.components.getGlassIconButtonBg(), CircleShape).clip(CircleShape)) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTextColor())
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (account == null) {
                                        if (prefLanguage == "bn") "নতুন অ্যাকাউন্ট যুক্ত করুন" else "Add Account"
                                    } else {
                                        if (prefLanguage == "bn") "অ্যাকাউন্ট সম্পাদনা" else "Edit Account Details"
                                    },
                                    color = AppTextColor(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (accountName.isNotBlank() || ownerName.isNotBlank()) {
                                            val bal = initialBalanceStr.toDoubleOrNull() ?: 0.0
                                            onSave(accountName, bal, details, qrCodeUri, ownerName, accountNumber, ifscCode, branchName, upiId)
                                        }
                                    },
                                    modifier = Modifier.size(40.dp).background(AccentCyan, CircleShape).clip(CircleShape)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(innerPadding)
                                .padding(horizontal = 24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            // Account Name
                            com.example.ui.components.GlassTextField(
                                value = accountName,
                                onValueChange = { accountName = it },
                                label = { Text(if (prefLanguage == "bn") "অ্যাকাউন্টের নাম" else "Account Name (e.g. Cash, HDFC)", color = AppTextColor().copy(alpha=0.6f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Account Balance
                            com.example.ui.components.GlassTextField(
                                value = initialBalanceStr,
                                onValueChange = { initialBalanceStr = it },
                                label = { Text(if (prefLanguage == "bn") "প্রারম্ভিক ব্যালেন্স (ঐচ্ছিক)" else "Initial Balance (Optional)", color = AppTextColor().copy(alpha=0.6f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Details
                            com.example.ui.components.GlassTextField(
                                value = details,
                                onValueChange = { details = it },
                                label = { Text(if (prefLanguage == "bn") "বিবরণ (ঐচ্ছিক)" else "Details (Optional)", color = AppTextColor().copy(alpha=0.6f)) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                                maxLines = 3
                            )
                            
                            // Image/QR
                            Card(
                                colors = CardDefaults.cardColors(containerColor = com.example.ui.components.getSettingsItemBg()),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, com.example.ui.components.getGlassBorder()),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = if (prefLanguage == "bn") "অ্যাকাউন্টের ছবি / QR কোড" else "Account Image / QR Code",
                                        color = AppTextColor(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (qrCodeUri.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.White)
                                                    .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                coil.compose.AsyncImage(
                                                    model = qrCodeUri,
                                                    contentDescription = "Account Image",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedButton(
                                                    onClick = { imagePicker.launch("image/*") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                                                ) {
                                                    Text(if (prefLanguage == "bn") "পরিবর্তন" else "Change Image", fontSize = 11.sp)
                                                }
                                                TextButton(
                                                    onClick = { qrCodeUri = "" },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = AccentPink)
                                                ) {
                                                    Text(if (prefLanguage == "bn") "মুছুন" else "Remove", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { imagePicker.launch("image/*") },
                                            modifier = Modifier.fillMaxWidth().height(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (prefLanguage == "bn") "ছবি বা QR কোড যোগ করুন" else "Add Image / QR Code", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            
                            // Optional Bank & UPI Info
                            Text(
                                text = if (prefLanguage == "bn") "ঐচ্ছিক ব্যাংক বিবরণ (যদি থাকে):" else "Optional Bank & UPI Details:",
                                color = AppTextColor().copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            com.example.ui.components.GlassTextField(
                                value = accountNumber,
                                onValueChange = { accountNumber = it },
                                label = { Text("Account Number", color = AppTextColor().copy(alpha=0.6f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                com.example.ui.components.GlassTextField(
                                    value = ifscCode,
                                    onValueChange = { ifscCode = it },
                                    label = { Text("IFSC Code", color = AppTextColor().copy(alpha=0.6f)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                com.example.ui.components.GlassTextField(
                                    value = branchName,
                                    onValueChange = { branchName = it },
                                    label = { Text("Branch Name", color = AppTextColor().copy(alpha=0.6f)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            com.example.ui.components.GlassTextField(
                                value = upiId,
                                onValueChange = { upiId = it },
                                label = { Text("UPI ID (e.g. name@upi)", color = AppTextColor().copy(alpha=0.6f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            com.example.ui.components.GlassTextField(
                                value = ownerName,
                                onValueChange = { ownerName = it },
                                label = { Text("Account Holder Name", color = AppTextColor().copy(alpha=0.6f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}"""

content = content[:start_idx] + new_dialog + content[end_idx:]

with open("app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt", "w") as f:
    f.write(content)

print("Replaced AddAccountDialog")
