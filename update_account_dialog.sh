sed -i '/fun UnifiedAccountDialog(/,/^}$/c\
@OptIn(ExperimentalMaterial3Api::class)\
@Composable\
fun UnifiedAccountDialog(\
    account: BankAccountEntity?,\
    prefLanguage: String,\
    accountsCount: Int,\
    onDismiss: () -> Unit,\
    onSave: (name: String, balance: Double) -> Unit,\
    onDelete: (() -> Unit)? = null\
) {\
    var accName by remember(account) { mutableStateOf(account?.name ?: "") }\
    var accBal by remember(account) { mutableStateOf(account?.balance?.toString() ?: "") }\
\
    Dialog(\
        onDismissRequest = onDismiss,\
        properties = DialogProperties(\
            usePlatformDefaultWidth = false,\
            decorFitsSystemWindows = false\
        )\
    ) {\
        Scaffold(\
            modifier = Modifier.fillMaxSize(),\
            containerColor = getDialogBackground(),\
            contentWindowInsets = WindowInsets.safeDrawing,\
            topBar = {\
                Row(\
                    modifier = Modifier\
                        .fillMaxWidth()\
                        .background(getDialogBackground())\
                        .padding(horizontal = 24.dp, vertical = 16.dp),\
                    verticalAlignment = Alignment.CenterVertically\
                ) {\
                    IconButton(\
                        onClick = onDismiss,\
                        modifier = Modifier\
                            .size(40.dp)\
                            .background(getGlassIconButtonBg(), CircleShape)\
                            .bounceClick()\
                    ) {\
                        Icon(\
                            Icons.Default.ArrowBack,\
                            contentDescription = "Back",\
                            tint = AppTextColor()\
                        )\
                    }\
                    Spacer(modifier = Modifier.width(16.dp))\
                    Text(\
                        text = if (account == null) {\
                            if (prefLanguage == "bn") "নতুন অ্যাকাউন্ট যুক্ত করুন" else "Add Virtual Account"\
                        } else {\
                            if (prefLanguage == "bn") "অ্যাকাউন্ট সংশোধন" else "Edit Account Details"\
                        },\
                        color = AppTextColor(),\
                        fontSize = 22.sp,\
                        fontWeight = FontWeight.Bold,\
                        modifier = Modifier.weight(1f)\
                    )\
                }\
            },\
            bottomBar = {\
                Surface(\
                    color = getDialogBackground(),\
                    shadowElevation = 16.dp,\
                    modifier = Modifier.fillMaxWidth()\
                ) {\
                    Row(\
                        modifier = Modifier\
                            .fillMaxWidth()\
                            .padding(horizontal = 24.dp, vertical = 16.dp),\
                        horizontalArrangement = Arrangement.End,\
                        verticalAlignment = Alignment.CenterVertically\
                    ) {\
                        if (account != null && accountsCount > 1 && onDelete != null) {\
                            var showMenu by remember { mutableStateOf(false) }\
                            Box(modifier = Modifier.wrapContentSize(Alignment.BottomStart)) {\
                                IconButton(\
                                    onClick = { showMenu = true },\
                                    modifier = Modifier.background(AppTextColor().copy(alpha = 0.1f), CircleShape).bounceClick()\
                                ) {\
                                    Icon(\
                                        imageVector = Icons.Default.MoreVert,\
                                        contentDescription = "More Options",\
                                        tint = AppTextColor()\
                                    )\
                                }\
                                DropdownMenu(\
                                    expanded = showMenu,\
                                    onDismissRequest = { showMenu = false },\
                                    modifier = Modifier.background(getDialogBackground())\
                                ) {\
                                    DropdownMenuItem(\
                                        text = { Text(if (prefLanguage == "bn") "মুছে ফেলুন" else "Delete", color = AccentPink) },\
                                        onClick = {\
                                            showMenu = false\
                                            onDelete()\
                                        },\
                                        leadingIcon = {\
                                            Icon(\
                                                imageVector = Icons.Default.Delete,\
                                                contentDescription = null,\
                                                tint = AccentPink,\
                                                modifier = Modifier.size(18.dp)\
                                            )\
                                        }\
                                    )\
                                }\
                            }\
                            Spacer(modifier = Modifier.weight(1f))\
                        } else {\
                            Spacer(modifier = Modifier.weight(1f))\
                        }\
                        TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_account_btn").bounceClick()) {\
                            Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AccentPink, fontWeight = FontWeight.Bold)\
                        }\
                        Spacer(modifier = Modifier.width(16.dp))\
                        Button(\
                            onClick = {\
                                if (accName.isNotBlank()) {\
                                    val bal = accBal.toDoubleOrNull() ?: 0.0\
                                    onSave(accName, bal)\
                                }\
                            },\
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),\
                            shape = RoundedCornerShape(12.dp),\
                            modifier = Modifier.height(48.dp).testTag("save_account_btn").bounceClick()\
                        ) {\
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))\
                            Spacer(modifier = Modifier.width(4.dp))\
                            Text(\
                                text = if (account == null) {\
                                    if (prefLanguage == "bn") "যুক্ত করুন" else "Register"\
                                } else {\
                                    if (prefLanguage == "bn") "সংরক্ষণ" else "Save"\
                                },\
                                color = Color.Black,\
                                fontWeight = FontWeight.Bold\
                            )\
                        }\
                    }\
                }\
            }\
        ) { innerPadding ->\
            Box(\
                modifier = Modifier\
                    .fillMaxSize()\
                    .padding(innerPadding),\
                contentAlignment = Alignment.TopCenter\
            ) {\
                Column(\
                    modifier = Modifier\
                        .fillMaxSize()\
                        .widthIn(max = 600.dp)\
                        .verticalScroll(rememberScrollState())\
                        .padding(horizontal = 24.dp, vertical = 24.dp),\
                    verticalArrangement = Arrangement.spacedBy(16.dp)\
                ) {\
                    GlassTextField(\
                        value = accName,\
                        onValueChange = { accName = it },\
                        label = { Text(if (prefLanguage == "bn") "অ্যাকাউন্টের নাম" else "Account Name") },\
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },\
                        singleLine = true,\
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),\
                        modifier = Modifier.fillMaxWidth().testTag("account_name_input")\
                    )\
                    GlassTextField(\
                        value = accBal,\
                        onValueChange = { accBal = it },\
                        label = {\
                            Text(\
                                if (account == null) {\
                                    if (prefLanguage == "bn") "প্রারম্ভিক ব্যালেন্স (₹)" else "Starting Balance (₹)"\
                                } else {\
                                    if (prefLanguage == "bn") "তরল ব্যালেন্স (₹)" else "Liquid Balance (₹)"\
                                }\
                            )\
                        },\
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },\
                        keyboardOptions = KeyboardOptions(\
                            keyboardType = KeyboardType.Number,\
                            imeAction = ImeAction.Done\
                        ),\
                        singleLine = true,\
                        modifier = Modifier.fillMaxWidth().testTag("account_balance_input")\
                    )\
                }\
            }\
        }\
    }\
}' app/src/main/java/com/example/ui/screens/SettingsScreen.kt
