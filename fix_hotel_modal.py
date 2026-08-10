import re

with open("app/src/main/java/com/example/ui/screens/EditHotelModal.kt", "r") as f:
    content = f.read()

old_top_bar = r"""            topBar = \{
                Row\(
                    modifier = Modifier
                        \.fillMaxWidth\(\)
                        \.background\(getDialogBackground\(\)\)
                        \.padding\(horizontal = 24\.dp, vertical = 16\.dp\),
                    verticalAlignment = Alignment\.CenterVertically
                \) \{
                    IconButton\(
                        onClick = onDismiss,
                        modifier = Modifier
                            \.size\(40\.dp\)
                            \.background\(getGlassIconButtonBg\(\), CircleShape\)
                            \.bounceClick\(\)
                    \) \{
                        Icon\(
                            Icons\.Default\.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTextColor\(\)
                        \)
                    \}
                    Spacer\(modifier = Modifier\.width\(16\.dp\)\)
                    Text\(
                        text = if \(hotel == null\) "Add Hotel" else "Edit Hotel Info",
                        color = AppTextColor\(\),
                        fontSize = 22\.sp,
                        fontWeight = FontWeight\.Bold,
                    \)
                    Spacer\(modifier = Modifier\.weight\(1f\)\)
                    Button\(
                        onClick = \{
                            // Save logic
                            val adv = editHotelAdvanced\.toDoubleOrNull\(\) \?: 0\.0
                            if \(editHotelName\.isNotBlank\(\)\) \{
                                if \(hotel == null\) \{
                                    viewModel\.addHotel\(HotelEntity\(
                                        name = editHotelName,
                                        advancedPayment = adv,
                                        phoneNumber = editPhone,
                                        location = editLocation,
                                        upiId = editUpiId,
                                        qrCodeUri = editQrCodeUris\.joinToString\(","\)
                                    \)\)
                                \} else \{
                                    viewModel\.updateHotel\(hotel\.copy\(
                                        name = editHotelName,
                                        advancedPayment = adv,
                                        phoneNumber = editPhone,
                                        location = editLocation,
                                        upiId = editUpiId,
                                        qrCodeUri = editQrCodeUris\.joinToString\(","\)
                                    \)\)
                                \}
                                onDismiss\(\)
                            \}
                        \},
                        colors = ButtonDefaults\.buttonColors\(containerColor = AccentCyan\),
                        shape = RoundedCornerShape\(12\.dp\),
                        modifier = Modifier\.height\(48\.dp\)\.bounceClick\(\)
                    \) \{
                        Text\(if \(hotel == null\) "Add Hotel" else "Save Changes", color = Color\.Black, fontWeight = FontWeight\.Bold\)
                    \}
                \}
            \}"""

new_top_bar = """            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(getDialogBackground())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
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
                        text = if (hotel == null) "Add Hotel" else "Edit Hotel Info",
                        color = AppTextColor(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }"""

content = re.sub(old_top_bar, new_top_bar, content, flags=re.DOTALL)

old_bottom = r"""                    if \(hotel != null\) \{
                        Spacer\(modifier = Modifier\.height\(32\.dp\)\)
                        Divider\(color = AppTextColor\(\)\.copy\(alpha = 0\.1f\)\)"""

new_bottom = """                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            // Save logic
                            val adv = editHotelAdvanced.toDoubleOrNull() ?: 0.0
                            if (editHotelName.isNotBlank()) {
                                if (hotel == null) {
                                    viewModel.addHotel(HotelEntity(
                                        name = editHotelName,
                                        advancedPayment = adv,
                                        phoneNumber = editPhone,
                                        location = editLocation,
                                        upiId = editUpiId,
                                        qrCodeUri = editQrCodeUris.joinToString(",")
                                    ))
                                } else {
                                    viewModel.updateHotel(hotel.copy(
                                        name = editHotelName,
                                        advancedPayment = adv,
                                        phoneNumber = editPhone,
                                        location = editLocation,
                                        upiId = editUpiId,
                                        qrCodeUri = editQrCodeUris.joinToString(",")
                                    ))
                                }
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick()
                    ) {
                        Text(if (hotel == null) "Add Hotel" else "Save Changes", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    
                    if (hotel != null) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Divider(color = AppTextColor().copy(alpha = 0.1f))"""

content = re.sub(old_bottom, new_bottom, content)

with open("app/src/main/java/com/example/ui/screens/EditHotelModal.kt", "w") as f:
    f.write(content)
print("Updated successfully!")
