package com.example.ui.screens.settings.subscreens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BankAccountEntity
import com.example.data.model.OwnerAccountEntity
import com.example.data.model.UserProfileEntity
import com.example.ui.components.*
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun ProfileSettingsScreen(
    viewModel: FinancialViewModel,
    onOpenUtilityConfig: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val ownerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val allHotels by viewModel.allHotels.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<OwnerAccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<OwnerAccountEntity?>(null) }
    var showDefaultHotelDialog by remember { mutableStateOf(false) }

    val prefLanguage = userProfile.language

    // Profile Edit Dialog
    if (showEditProfileDialog) {
        EditProfileModal(
            currentProfile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedName, updatedContact, updatedAvatar, updatedCurrency ->
                viewModel.saveUserProfile(
                    userProfile.copy(
                        name = updatedName,
                        contact = updatedContact,
                        avatar = updatedAvatar,
                        currencySymbol = updatedCurrency
                    )
                )
                showEditProfileDialog = false
                Toast.makeText(context, if (prefLanguage == "bn") "প্রোফাইল আপডেট হয়েছে" else "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        LanguageSelectionModal(
            currentLanguage = userProfile.language,
            onDismiss = { showLanguageDialog = false },
            onSelect = { langCode ->
                viewModel.saveUserProfile(userProfile.copy(language = langCode))
                showLanguageDialog = false
            }
        )
    }

    // Default Hotel Selection Dialog
    if (showDefaultHotelDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultHotelDialog = false },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (prefLanguage == "bn") "ডিফল্ট হোটেল নির্বাচন করুন" else "Select Default Hotel",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allHotels.filter { !it.isDeleted }.forEach { hotel ->
                        val isSelected = userProfile.defaultHotelId == hotel.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else getCardBackground())
                                .border(1.dp, if (isSelected) AccentCyan else getGlassBorder(), RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.saveUserProfile(userProfile.copy(defaultHotelId = hotel.id))
                                    showDefaultHotelDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.saveUserProfile(userProfile.copy(defaultHotelId = hotel.id))
                                    showDefaultHotelDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = hotel.name, color = AppTextColor(), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultHotelDialog = false }) {
                    Text(if (prefLanguage == "bn") "বন্ধ করুন" else "Close", color = AccentCyan)
                }
            }
        )
    }

    // Add or Edit Owner Bank Account Dialog
    if (showAddAccountDialog || accountToEdit != null) {
        OwnerAccountEditorModal(
            account = accountToEdit,
            prefLanguage = prefLanguage,
            onDismiss = {
                showAddAccountDialog = false
                accountToEdit = null
            },
            onSave = { name, mobile, accName, accNumber, ifsc, branch, upi, qrUri ->
                if (accountToEdit != null) {
                    val updated = accountToEdit!!.copy(
                        ownerName = name,
                        ownerMobile = mobile,
                        accountName = accName,
                        accountNumber = accNumber,
                        ifscCode = ifsc,
                        branchName = branch,
                        upiId = upi,
                        qrCodeUri = qrUri
                    )
                    viewModel.updateOwnerAccount(updated)
                    Toast.makeText(context, if (prefLanguage == "bn") "অ্যাকাউন্ট আপডেট হয়েছে" else "Account updated", Toast.LENGTH_SHORT).show()
                } else {
                    val newAcc = OwnerAccountEntity(
                        ownerName = name,
                        ownerMobile = mobile,
                        accountName = accName,
                        accountNumber = accNumber,
                        ifscCode = ifsc,
                        branchName = branch,
                        upiId = upi,
                        qrCodeUri = qrUri
                    )
                    viewModel.addOwnerAccount(newAcc)
                    // Also create a linked virtual bank account for expense tracking if name provided
                    if (accName.isNotBlank() || name.isNotBlank()) {
                        viewModel.addVirtualAccount(
                            name = if (accName.isNotBlank()) accName else name,
                            balance = 0.0,
                            upiId = upi,
                            qrCodeUri = qrUri,
                            accountNumber = accNumber,
                            ifsc = ifsc,
                            branchName = branch
                        )
                    }
                    Toast.makeText(context, if (prefLanguage == "bn") "নতুন অ্যাকাউন্ট তৈরি হয়েছে" else "New account created", Toast.LENGTH_SHORT).show()
                }
                showAddAccountDialog = false
                accountToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (accountToDelete != null) {
        ConfirmDeleteDialog(
            itemName = accountToDelete?.accountName?.ifBlank { accountToDelete?.ownerName } ?: "Account",
            requireTypedConfirmation = false,
            onConfirm = {
                accountToDelete?.let { viewModel.deleteOwnerAccount(it) }
                accountToDelete = null
                Toast.makeText(context, if (prefLanguage == "bn") "অ্যাকাউন্ট মুছে ফেলা হয়েছে" else "Account deleted", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { accountToDelete = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Personal Profile Card (Interactive)
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "ব্যক্তিগত তথ্য" else "Personal Information") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(getCardBackground())
                        .border(1.dp, getGlassBorder(), RoundedCornerShape(16.dp))
                        .clickable { showEditProfileDialog = true }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Badge
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AccentCyan.copy(alpha = 0.2f))
                                .border(1.5.dp, AccentCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (userProfile.name.isNotBlank()) userProfile.name.take(1).uppercase() else "👤",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (userProfile.name.isNotBlank()) userProfile.name else (if (prefLanguage == "bn") "নাম সেট করা হয়নি" else "Set Owner Name"),
                                color = AppTextColor(),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (userProfile.contact.isNotBlank()) userProfile.contact else (if (prefLanguage == "bn") "ফোন নম্বর যোগ করুন" else "Add contact phone"),
                                color = AppSecondaryTextColor(),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AccentGold.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Currency: ${userProfile.currencySymbol}",
                                        color = AccentGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(getGlassIconButtonBg(), CircleShape)
                                .border(1.dp, getGlassBorder(), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = AccentCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // 2. Bank Accounts & Payment Details Section
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "ব্যাংক ও পেমেন্ট অ্যাকাউন্ট" else "Bank & Payment Accounts") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Add Account Action Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentCyan.copy(alpha = 0.12f))
                            .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                accountToEdit = null
                                showAddAccountDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (prefLanguage == "bn") "নতুন ব্যাংক অ্যাকাউন্ট যুক্ত করুন" else "Add Bank / Owner Account",
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentCyan)
                    }

                    // Existing accounts list
                    val nonDeletedAccounts = ownerAccounts.filter { !it.isDeleted }
                    if (nonDeletedAccounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(getCardBackground())
                                .border(1.dp, getGlassBorder(), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (prefLanguage == "bn") "কোনো ব্যাংক অ্যাকাউন্ট যুক্ত করা হয়নি" else "No bank accounts added yet",
                                color = AppSecondaryTextColor(),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        nonDeletedAccounts.forEach { acc ->
                            AccountCardItem(
                                account = acc,
                                prefLanguage = prefLanguage,
                                onEdit = { accountToEdit = acc },
                                onDelete = { accountToDelete = acc }
                            )
                        }
                    }
                }
            }
        }

        // 3. App Preferences Section
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "পছন্দসমূহ" else "Preferences") {
                PixelSettingsItem(
                    icon = Icons.Default.Language,
                    title = if (prefLanguage == "bn") "ভাষা নির্বাচন" else "App Language",
                    subtitle = when (userProfile.language) {
                        "bn" -> "বাংলা (Bengali)"
                        "hi" -> "हिन्दी (Hindi)"
                        else -> "English"
                    },
                    onClick = { showLanguageDialog = true }
                )
                Spacer(modifier = Modifier.height(4.dp))
                PixelSettingsItem(
                    icon = Icons.Default.Apartment,
                    title = if (prefLanguage == "bn") "ডিফল্ট হোটেল" else "Default Hotel",
                    subtitle = allHotels.find { it.id == userProfile.defaultHotelId }?.name ?: (if (prefLanguage == "bn") "হোটেল ১" else "Hotel 1"),
                    onClick = { showDefaultHotelDialog = true }
                )
            }
        }

        // 4. Utilities & Billing Configuration Section
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "ইউটিলিটি ও মিটার সেটিংস" else "Utility & Billing Configuration") {
                PixelSettingsItem(
                    icon = Icons.Default.ElectricMeter,
                    title = if (prefLanguage == "bn") "মিটার রিডিং ও বিল কনফিগারেশন" else "Meter Reading & Rates Config",
                    subtitle = if (prefLanguage == "bn") "বিদ্যুৎ ও ইউটিলিটি টেমপ্লেট পরিচালনা করুন" else "Manage electricity rates and billing templates",
                    onClick = onOpenUtilityConfig
                )
            }
        }
    }
}

@Composable
fun AccountCardItem(
    account: OwnerAccountEntity,
    prefLanguage: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(getCardBackground())
            .border(1.dp, getGlassBorder(), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = account.accountName.ifBlank { account.ownerName },
                        color = AppTextColor(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentPink, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (account.accountNumber.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "A/C: ",
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp
                    )
                    Text(
                        text = account.accountNumber,
                        color = AppTextColor(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (account.ifscCode.isNotBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "IFSC: ",
                            color = AppSecondaryTextColor(),
                            fontSize = 12.sp
                        )
                        Text(
                            text = account.ifscCode,
                            color = AppTextColor(),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (account.upiId.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "UPI: ",
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp
                    )
                    Text(
                        text = account.upiId,
                        color = AccentGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (account.branchName.isNotBlank() || account.ownerMobile.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (account.branchName.isNotBlank()) {
                        Text(
                            text = "Branch: ${account.branchName}",
                            color = AppSecondaryTextColor(),
                            fontSize = 11.sp
                        )
                    }
                    if (account.ownerMobile.isNotBlank()) {
                        Text(
                            text = "Ph: ${account.ownerMobile}",
                            color = AppSecondaryTextColor(),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileModal(
    currentProfile: UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, contact: String, avatar: String, currency: String) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.name) }
    var contact by remember { mutableStateOf(currentProfile.contact) }
    var avatar by remember { mutableStateOf(currentProfile.avatar) }
    var currency by remember { mutableStateOf(currentProfile.currencySymbol) }

    val currencyOptions = listOf("₹", "$", "€", "£", "৳", "¥", "د.إ")
    val avatarOptions = listOf("avatar_premium", "avatar_classic", "avatar_modern", "avatar_tech", "avatar_royal")

    BackHandler { onDismiss() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Edit Profile",
                color = AppTextColor(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Owner / Business Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = getGlassBorder()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Phone / Mobile") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = getGlassBorder()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Currency Symbol:",
                    color = AppTextColor(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencyOptions.forEach { symbol ->
                        val isSelected = currency == symbol
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentCyan else getCardBackground())
                                .border(1.dp, if (isSelected) AccentCyan else getGlassBorder(), RoundedCornerShape(8.dp))
                                .clickable { currency = symbol },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbol,
                                color = if (isSelected) Color.Black else AppTextColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, contact, avatar, currency) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppSecondaryTextColor())
            }
        }
    )
}

@Composable
fun LanguageSelectionModal(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf(
        Triple("en", "English", "Default language"),
        Triple("bn", "বাংলা (Bengali)", "সম্পূর্ণ বাংলা ইন্টারফেস"),
        Triple("hi", "हिन्दी (Hindi)", "हिंदी भाषा इंटरफ़ेस")
    )

    BackHandler { onDismiss() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Select App Language", color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                languages.forEach { (code, name, subtitle) ->
                    val isSelected = currentLanguage == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else getCardBackground())
                            .border(1.dp, if (isSelected) AccentCyan else getGlassBorder(), RoundedCornerShape(12.dp))
                            .clickable { onSelect(code) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(code) },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = name, color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = subtitle, color = AppSecondaryTextColor(), fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentCyan)
            }
        }
    )
}

@Composable
fun OwnerAccountEditorModal(
    account: OwnerAccountEntity?,
    prefLanguage: String,
    onDismiss: () -> Unit,
    onSave: (name: String, mobile: String, accName: String, accNum: String, ifsc: String, branch: String, upi: String, qrUri: String) -> Unit
) {
    var ownerName by remember(account) { mutableStateOf(account?.ownerName ?: "") }
    var ownerMobile by remember(account) { mutableStateOf(account?.ownerMobile ?: "") }
    var accountName by remember(account) { mutableStateOf(account?.accountName ?: "") }
    var accountNumber by remember(account) { mutableStateOf(account?.accountNumber ?: "") }
    var ifscCode by remember(account) { mutableStateOf(account?.ifscCode ?: "") }
    var branchName by remember(account) { mutableStateOf(account?.branchName ?: "") }
    var upiId by remember(account) { mutableStateOf(account?.upiId ?: "") }
    var qrCodeUri by remember(account) { mutableStateOf(account?.qrCodeUri ?: "") }

    BackHandler { onDismiss() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (account == null) {
                    if (prefLanguage == "bn") "নতুন ব্যাংক অ্যাকাউন্ট যুক্ত করুন" else "Add Bank Account"
                } else {
                    if (prefLanguage == "bn") "অ্যাকাউন্ট সম্পাদনা" else "Edit Bank Account"
                },
                color = AppTextColor(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Bank / Account Name (e.g. SBI Current)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = getGlassBorder()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = getGlassBorder()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ifscCode,
                        onValueChange = { ifscCode = it },
                        label = { Text("IFSC Code") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = getGlassBorder()
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it },
                        label = { Text("Branch Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = getGlassBorder()
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID (e.g. name@upi)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = getGlassBorder()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Account Holder Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = getGlassBorder()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (accountName.isNotBlank() || ownerName.isNotBlank()) {
                        onSave(ownerName, ownerMobile, accountName, accountNumber, ifscCode, branchName, upiId, qrCodeUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (account == null) "Add Account" else "Update",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppSecondaryTextColor())
            }
        }
    )
}
