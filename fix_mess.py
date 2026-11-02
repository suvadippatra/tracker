import re

with open("app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt", "r") as f:
    content = f.read()

# We need to find the start of ShowPaymentDetailsModal
start_show_payment = content.find("@Composable\nfun ShowPaymentDetailsModal(")

# We need to find the start of DetailRowItem
start_detail_row = content.find("@Composable\nprivate fun DetailRowItem(")

# Extract the bad part
bad_part = content[start_show_payment:start_detail_row]

# Reconstruct ShowPaymentDetailsModal as an AlertDialog!
good_show_payment = """@Composable
fun ShowPaymentDetailsModal(
    account: OwnerAccountEntity,
    prefLanguage: String,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (prefLanguage == "bn") "পেমেন্ট পাওয়ার তথ্য ও QR" else "Payment Details & QR",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTextColor())
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (account.qrCodeUri.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(2.dp, AccentCyan, RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = account.qrCodeUri,
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = getCardBackground()),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailRowItem(if (prefLanguage == "bn") "ব্যাংক/লেবেল:" else "Bank/Label:", account.accountName, isBold = true)
                        if (account.accountNumber.isNotBlank()) DetailRowItem(if (prefLanguage == "bn") "অ্যাকাউন্ট নং:" else "Acc No:", account.accountNumber)
                        if (account.ifscCode.isNotBlank()) DetailRowItem(if (prefLanguage == "bn") "IFSC কোড:" else "IFSC:", account.ifscCode)
                        if (account.branchName.isNotBlank()) DetailRowItem(if (prefLanguage == "bn") "শাখা:" else "Branch:", account.branchName)
                        if (account.upiId.isNotBlank()) DetailRowItem(if (prefLanguage == "bn") "UPI ID:" else "UPI ID:", account.upiId, AccentCyan)
                        if (account.ownerName.isNotBlank()) DetailRowItem(if (prefLanguage == "bn") "নাম:" else "Name:", account.ownerName)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

"""

# Now we need to put AddAccountDialog back where it belongs!
# It belongs right after AppLanguageDialog or so.
# Let's search for "fun AddAccountDialog" ... oh wait, there was no `fun AddAccountDialog` originally?
# Yes there was. Let's see if there is one at the end of the file.

content = content[:start_show_payment] + good_show_payment + content[start_detail_row:]

with open("app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt", "w") as f:
    f.write(content)
print("Reverted ShowPaymentDetailsModal")
