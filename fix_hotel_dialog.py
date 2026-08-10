import re

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'r') as f:
    content = f.read()

target = """    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        modifier = Modifier.padding(16.dp).navigationBarsPadding(),
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(28.dp),
        title = {"""

replacement = """    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.LaunchedEffect(dialogWindow) {
            dialogWindow?.let { window ->
                window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getDialogBackground())
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                // Top header bar with a Back button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(getGlassIconButtonBg(), CircleShape)
                            .bounceClick()
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTextColor()
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (hotel == null) {
                            if (prefLanguage == "bn") "নতুন হোটেল যুক্ত করুন" else "Register Establishment"
                        } else {
                            if (prefLanguage == "bn") "হোটেল সংশোধন" else "Edit Establishment Details"
                        },
                        color = AppTextColor(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))"""

target2 = """        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {"""

replacement2 = """                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {"""

target3 = """            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {"""

replacement3 = """            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {"""

target4 = """        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_hotel_btn")) {
                Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AppTextColor())
            }
        }
    )"""

replacement4 = """                TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_hotel_btn")) {
                    Text(if (prefLanguage == "bn") "বাতিল" else "Cancel", color = AppTextColor())
                }
            }
        }
        }
    }"""

content = content.replace(target, replacement)
content = content.replace(target2, replacement2)

# For target 3 and 4 we might have multiple matches or something? Let's check if the replace worked.
idx = content.find("confirmButton = {")
if idx != -1:
    content = content.replace(target3, replacement3)
    content = content.replace(target4, replacement4)

with open('app/src/main/java/com/example/ui/screens/HotelManagementTab.kt', 'w') as f:
    f.write(content)

