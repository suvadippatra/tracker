@file:Suppress("DEPRECATION")
package com.example.ui.screens.settings.subscreens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.BankAccountEntity
import com.example.data.model.OwnerAccountEntity
import com.example.ui.screens.SelectVisibleAccountsOnExtraDialog
import com.example.data.model.UserProfileEntity
import com.example.ui.components.*
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun ProfileSettingsScreen(
    viewModel: FinancialViewModel,
    onOpenOwnerProfile: () -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val ownerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val allHotels by viewModel.allHotels.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showCreateHotelModal by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<OwnerAccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<OwnerAccountEntity?>(null) }
    var showDefaultHotelDialog by remember { mutableStateOf(false) }
    var showDefaultOwnerAccountDialog by remember { mutableStateOf(false) }
    var showDefaultLedgerAccountDialog by remember { mutableStateOf(false) }
    var showChooseVisibleAccountsDialog by remember { mutableStateOf(false) }
    var accountToShowDetails by remember { mutableStateOf<OwnerAccountEntity?>(null) }

    val isAnyModalOpen = showEditProfileDialog ||
            showLanguageDialog ||
            showAddAccountDialog ||
            showCreateHotelModal ||
            accountToEdit != null ||
            accountToDelete != null ||
            showDefaultHotelDialog ||
            showDefaultOwnerAccountDialog ||
            showDefaultLedgerAccountDialog ||
            showChooseVisibleAccountsDialog ||
            accountToShowDetails != null

    BackHandler(enabled = true) {
        if (showEditProfileDialog) showEditProfileDialog = false
        else if (showLanguageDialog) showLanguageDialog = false
        else if (showAddAccountDialog) showAddAccountDialog = false
        else if (showCreateHotelModal) showCreateHotelModal = false
        else if (accountToEdit != null) accountToEdit = null
        else if (accountToDelete != null) accountToDelete = null
        else if (showDefaultHotelDialog) showDefaultHotelDialog = false
        else if (showDefaultOwnerAccountDialog) showDefaultOwnerAccountDialog = false
        else if (showDefaultLedgerAccountDialog) showDefaultLedgerAccountDialog = false
        else if (showChooseVisibleAccountsDialog) showChooseVisibleAccountsDialog = false
        else if (accountToShowDetails != null) accountToShowDetails = null
        else onBack()
    }

    val visibleOwnerAccountIds = remember(userProfile.visibleBankAccountsOnExtra) {
        val raw = userProfile.visibleBankAccountsOnExtra
        if (raw.isBlank()) emptySet<Int>()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    val prefLanguage = userProfile.language

    // Profile Edit Dialog
    val defaultOwnerAccount = ownerAccounts.find { it.id == userProfile.defaultOwnerAccountId } ?: ownerAccounts.firstOrNull()

    if (showEditProfileDialog) {
        EditProfileModal(
            currentProfile = userProfile,
            defaultOwnerAccount = defaultOwnerAccount,
            prefLanguage = prefLanguage,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedName, updatedContact, updatedAvatar, updatedCurrency, bankName, accNum, ifsc, branch, upi, qrUri ->
                viewModel.saveUserProfile(
                    userProfile.copy(
                        name = updatedName,
                        contact = updatedContact,
                        avatar = updatedAvatar,
                        currencySymbol = updatedCurrency
                    )
                )

                if (defaultOwnerAccount != null) {
                    val updatedAcc = defaultOwnerAccount.copy(
                        ownerName = updatedName.ifBlank { defaultOwnerAccount.ownerName },
                        ownerMobile = updatedContact.ifBlank { defaultOwnerAccount.ownerMobile },
                        accountName = bankName,
                        accountNumber = accNum,
                        ifscCode = ifsc,
                        branchName = branch,
                        upiId = upi,
                        qrCodeUri = qrUri
                    )
                    viewModel.updateOwnerAccount(updatedAcc)
                } else if (bankName.isNotBlank() || upi.isNotBlank() || accNum.isNotBlank() || qrUri.isNotBlank()) {
                    val newAcc = OwnerAccountEntity(
                        ownerName = updatedName,
                        ownerMobile = updatedContact,
                        accountName = bankName,
                        accountNumber = accNum,
                        ifscCode = ifsc,
                        branchName = branch,
                        upiId = upi,
                        qrCodeUri = qrUri
                    )
                    viewModel.addOwnerAccount(newAcc)
                }

                showEditProfileDialog = false
                Toast.makeText(context, if (prefLanguage == "bn") "প্রোফাইল ও পেমেন্ট তথ্য আপডেট হয়েছে" else "Profile & payment details updated", Toast.LENGTH_SHORT).show()
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
                                .background(if (isSelected) AppAccentColor().copy(alpha = 0.2f) else getCardBackground())
                                .border(1.dp, if (isSelected) AppAccentColor() else getGlassBorder(), RoundedCornerShape(12.dp))
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
                                colors = RadioButtonDefaults.colors(selectedColor = AppAccentColor())
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = hotel.name, color = AppTextColor(), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultHotelDialog = false }) {
                    Text(if (prefLanguage == "bn") "বন্ধ করুন" else "Close", color = AppAccentColor())
                }
            }
        )
    }

    // Create Hotel Modal
    if (showCreateHotelModal) {
        com.example.ui.screens.EditHotelModal(
            hotel = null,
            viewModel = viewModel,
            onDismiss = { showCreateHotelModal = false },
            onManageMenu = { showCreateHotelModal = false }
        )
    }

    // Default Owner Payment Receiving Account Selection Dialog
    if (showDefaultOwnerAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultOwnerAccountDialog = false },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (prefLanguage == "bn") "ডিফল্ট পেমেন্ট রিসিভিং অ্যাকাউন্ট" else "Select Default Payment Receiving Account",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val nonDeletedOwnerAccounts = ownerAccounts.filter { !it.isDeleted }
                    if (nonDeletedOwnerAccounts.isEmpty()) {
                        Text(
                            text = if (prefLanguage == "bn") "কোনো ব্যাংক অ্যাকাউন্ট পাওয়া যায়নি" else "No owner bank accounts found",
                            color = AppSecondaryTextColor(),
                            fontSize = 14.sp
                        )
                    } else {
                        nonDeletedOwnerAccounts.forEach { acc ->
                            val isSelected = userProfile.defaultOwnerAccountId == acc.id
                            val displayName = acc.accountName.ifBlank { acc.ownerName.ifBlank { "Account #${acc.id}" } }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AppAccentColor().copy(alpha = 0.2f) else getCardBackground())
                                    .border(1.dp, if (isSelected) AppAccentColor() else getGlassBorder(), RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.setDefaultOwnerAccount(acc.id)
                                        showDefaultOwnerAccountDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setDefaultOwnerAccount(acc.id)
                                        showDefaultOwnerAccountDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AppAccentColor())
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = displayName, color = AppTextColor(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    if (acc.upiId.isNotBlank()) {
                                        Text(text = "UPI: ${acc.upiId}", color = AccentGold, fontSize = 12.sp)
                                    } else if (acc.accountNumber.isNotBlank()) {
                                        Text(text = "A/C: ${acc.accountNumber}", color = AppSecondaryTextColor(), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultOwnerAccountDialog = false }) {
                    Text(if (prefLanguage == "bn") "বন্ধ করুন" else "Close", color = AppAccentColor())
                }
            }
        )
    }

    // Default Expense Ledger Account Selection Dialog
    if (showDefaultLedgerAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultLedgerAccountDialog = false },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (prefLanguage == "bn") "ডিফল্ট পকেট মানি / লেজার অ্যাকাউন্ট" else "Select Default Ledger Account",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val nonDeletedBankAccounts = bankAccounts.filter { !it.isDeleted }
                    if (nonDeletedBankAccounts.isEmpty()) {
                        Text(
                            text = if (prefLanguage == "bn") "কোনো পকেট মানি অ্যাকাউন্ট পাওয়া যায়নি" else "No ledger bank accounts found",
                            color = AppSecondaryTextColor(),
                            fontSize = 14.sp
                        )
                    } else {
                        nonDeletedBankAccounts.forEach { acc ->
                            val isSelected = userProfile.defaultBankAccountId == acc.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AppAccentColor().copy(alpha = 0.2f) else getCardBackground())
                                    .border(1.dp, if (isSelected) AppAccentColor() else getGlassBorder(), RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.setDefaultBankAccount(acc.id)
                                        showDefaultLedgerAccountDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setDefaultBankAccount(acc.id)
                                        showDefaultLedgerAccountDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AppAccentColor())
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = acc.name, color = AppTextColor(), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultLedgerAccountDialog = false }) {
                    Text(if (prefLanguage == "bn") "বন্ধ করুন" else "Close", color = AppAccentColor())
                }
            }
        )
    }

    // Payment details and QR viewer modal
    if (accountToShowDetails != null) {
        ShowPaymentDetailsModal(
            account = accountToShowDetails!!,
            prefLanguage = prefLanguage,
            onDismiss = { accountToShowDetails = null }
        )
    }

    // Add or Edit Owner Bank Account Page (Sandboxed by default unless linked to Extras)
    if (showAddAccountDialog || accountToEdit != null) {
        val linkedBank = accountToEdit?.linkedBankAccountId?.let { linkedId ->
            bankAccounts.find { it.id == linkedId && !it.isDeleted }
        }
        val targetBank = accountToEdit?.let { ownerAcc ->
            BankAccountEntity(
                id = linkedBank?.id ?: 0,
                name = ownerAcc.accountName,
                balance = linkedBank?.balance ?: 0.0,
                initialBalance = linkedBank?.initialBalance ?: 0.0,
                details = linkedBank?.details ?: ownerAcc.notes,
                notes = ownerAcc.notes,
                phoneNumber = ownerAcc.ownerMobile,
                whatsappNumber = ownerAcc.whatsappNumber,
                email = ownerAcc.email,
                accountNumber = ownerAcc.accountNumber,
                ifsc = ownerAcc.ifscCode,
                branchName = ownerAcc.branchName,
                upiId = ownerAcc.upiId,
                qrCodeUri = ownerAcc.qrCodeUri
            )
        }

        com.example.ui.screens.UnifiedAccountDialog(
            viewModel = viewModel,
            account = targetBank,
            prefLanguage = prefLanguage,
            accountsCount = ownerAccounts.size,
            onDismiss = {
                showAddAccountDialog = false
                accountToEdit = null
            },
            onSaveExtended = { accName, accBal, details, upi, qrUri, accNum, ifscCode, branch, notes, phone, whatsapp, email ->
                if (accountToEdit != null) {
                    val updated = accountToEdit!!.copy(
                        accountName = accName,
                        ownerMobile = phone.ifBlank { accountToEdit!!.ownerMobile },
                        whatsappNumber = whatsapp,
                        email = email,
                        notes = notes.ifBlank { details },
                        accountNumber = accNum,
                        ifscCode = ifscCode,
                        branchName = branch,
                        upiId = upi,
                        qrCodeUri = qrUri
                    )
                    viewModel.updateOwnerAccount(updated)

                    if (linkedBank != null) {
                        viewModel.editVirtualAccount(
                            linkedBank.copy(
                                name = accName,
                                initialBalance = accBal,
                                details = details,
                                notes = notes,
                                phoneNumber = phone,
                                whatsappNumber = whatsapp,
                                email = email,
                                qrCodeUri = qrUri,
                                upiId = upi,
                                accountNumber = accNum,
                                ifsc = ifscCode,
                                branchName = branch
                            )
                        )
                    }
                    Toast.makeText(context, if (prefLanguage == "bn") "অ্যাকাউন্ট আপডেট হয়েছে" else "Account updated", Toast.LENGTH_SHORT).show()
                } else {
                    // Sandboxed by default in Profile
                    val ownerNameVal = userProfile.name.ifBlank { accName }
                    val newAcc = OwnerAccountEntity(
                        ownerName = ownerNameVal,
                        ownerMobile = phone.ifBlank { userProfile.contact },
                        whatsappNumber = whatsapp,
                        email = email,
                        notes = notes.ifBlank { details },
                        accountName = accName,
                        accountNumber = accNum,
                        ifscCode = ifscCode,
                        branchName = branch,
                        upiId = upi,
                        qrCodeUri = qrUri,
                        linkedBankAccountId = null
                    )
                    viewModel.addOwnerAccount(newAcc)
                    Toast.makeText(context, if (prefLanguage == "bn") "নতুন প্রোফাইল অ্যাকাউন্ট তৈরি হয়েছে" else "New profile account created", Toast.LENGTH_SHORT).show()
                }
                showAddAccountDialog = false
                accountToEdit = null
            },
            onDelete = if (accountToEdit != null && ownerAccounts.size > 1) {
                {
                    accountToEdit?.let { viewModel.deleteOwnerAccount(it) }
                    showAddAccountDialog = false
                    accountToEdit = null
                }
            } else null
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
        // 1. Personal Profile Card (Interactive - without Currency badge or Edit pencil button)
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "ব্যক্তিগত তথ্য" else "Personal Information") {
                val appAccent = AppAccentColor()
                val isLight = isAppLightMode()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenOwnerProfile() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Badge
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(appAccent.copy(alpha = if (isLight) 0.12f else 0.18f))
                            .border(1.5.dp, appAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userProfile.name.isNotBlank()) userProfile.name.take(1).uppercase() else "👤",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = appAccent
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (userProfile.name.isNotBlank()) userProfile.name else (if (prefLanguage == "bn") "নাম সেট করা হয়নি" else "Set Owner Name"),
                            color = AppTextColor(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (userProfile.contact.isNotBlank()) userProfile.contact else (if (prefLanguage == "bn") "ফোন নম্বর যোগ করুন" else "Add contact phone"),
                            color = AppSecondaryTextColor(),
                            fontSize = 13.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Profile",
                        tint = AppSecondaryTextColor(),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        // 2. Hotel Management & Defaults Section
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "হোটেল ও ডিফল্ট নির্বাচন" else "Hotels & Default Selection") {
                val appAccent = AppAccentColor()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Add Hotel Button
                        Button(
                            onClick = { showCreateHotelModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = appAccent, contentColor = AppOnAccentColor(appAccent)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .wrapContentHeight()
                                .bounceClick()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = AppOnAccentColor(appAccent), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (prefLanguage == "bn") "হোটেল যোগ" else "Add Hotel",
                                color = AppOnAccentColor(appAccent),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Choose Default Hotel Button
                        val defaultHotelName = allHotels.find { it.id == userProfile.defaultHotelId }?.name ?: (if (prefLanguage == "bn") "ডিফল্ট হোটেল" else "Default Hotel")
                        OutlinedButton(
                            onClick = { showDefaultHotelDialog = true },
                            border = BorderStroke(1.dp, appAccent.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .wrapContentHeight()
                                .bounceClick()
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = defaultHotelName,
                                color = AppTextColor(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3. Bank Accounts & Payment Details Section (Financial Accounts)
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "ফাইন্যান্সিয়াল অ্যাকাউন্টস" else "Financial Accounts") {
                val appAccent = AppAccentColor()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (prefLanguage == "bn") "ইনকাম ও ব্যয়ের হিসাব এবং পেমেন্ট রিসিভিং অ্যাকাউন্টস:" else "Accounts for tracking income, outcome status, and receiving payments:",
                        color = AppSecondaryTextColor(),
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Add Owner Account Button
                        Button(
                            onClick = {
                                accountToEdit = null
                                showAddAccountDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appAccent, contentColor = AppOnAccentColor(appAccent)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .wrapContentHeight()
                                .bounceClick()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = AppOnAccentColor(appAccent), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (prefLanguage == "bn") "অ্যাকাউন্ট যোগ" else "Add Account",
                                color = AppOnAccentColor(appAccent),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Choose Default Receiving Account Button
                        val defaultOwnerAcc = ownerAccounts.find { it.id == userProfile.defaultOwnerAccountId }
                        val defaultOwnerAccName = defaultOwnerAcc?.accountName?.ifBlank { defaultOwnerAcc?.ownerName } ?: (if (prefLanguage == "bn") "ডিফল্ট রিসিভিং" else "Default Acc")
                        OutlinedButton(
                            onClick = { showDefaultOwnerAccountDialog = true },
                            border = BorderStroke(1.dp, appAccent.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .wrapContentHeight()
                                .bounceClick()
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = defaultOwnerAccName,
                                color = AppTextColor(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Existing accounts list
                    val nonDeletedAccounts = ownerAccounts.filter { !it.isDeleted }
                    nonDeletedAccounts.forEach { acc ->
                        AccountCardItem(
                            viewModel = viewModel,
                            account = acc,
                            isDefault = (acc.id == userProfile.defaultOwnerAccountId),
                            isVisibleOnExtra = (acc.linkedBankAccountId != null || acc.id in visibleOwnerAccountIds),
                            prefLanguage = prefLanguage,
                            onEdit = { accountToEdit = acc },
                            onDelete = { accountToDelete = acc },
                            onShowDetails = { accountToShowDetails = acc }
                        )
                    }
                }
            }
        }

        // 4. App Preferences Section
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
            }
        }

        // 4. Utilities & Billing Configuration Section
        item {}
    }
}

@Composable
fun AccountCardItem(
    viewModel: FinancialViewModel,
    account: OwnerAccountEntity,
    isDefault: Boolean = false,
    isVisibleOnExtra: Boolean = false,
    prefLanguage: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowDetails: () -> Unit
) {
    val context = LocalContext.current
    val isLinkedToExtras = account.linkedBankAccountId != null
    var showDelinkDialog by remember { mutableStateOf(false) }

    val exportAccountCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && account.linkedBankAccountId != null) {
            viewModel.exportDataCsv(
                type = com.example.data.BackupDataType.SINGLE_BANK_ACCOUNT,
                bankAccountId = account.linkedBankAccountId
            ) { csv ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Transaction history downloaded! Account de-linked.", Toast.LENGTH_SHORT).show()
                    viewModel.delinkOwnerAccountFromExtras(account, moveExtrasAccountToBin = true)
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showDelinkDialog) {
        com.example.ui.screens.DelinkAccountFromExtrasDialog(
            accountName = account.accountName.ifBlank { account.ownerName },
            onMoveToBin = {
                showDelinkDialog = false
                viewModel.delinkOwnerAccountFromExtras(account, moveExtrasAccountToBin = true)
                Toast.makeText(context, "Extras account & history moved to Recycle Bin", Toast.LENGTH_SHORT).show()
            },
            onDownloadHistory = {
                showDelinkDialog = false
                val safeName = account.accountName.ifBlank { "account" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                exportAccountCsvLauncher.launch("${safeName}_history.csv")
            },
            onOpenExportTool = {
                showDelinkDialog = false
                viewModel.delinkOwnerAccountFromExtras(account, moveExtrasAccountToBin = false)
                Toast.makeText(
                    context,
                    "Account de-linked. Open Settings > Data & Backups to export (Note: only QR, JSON, CSV, or Copy-Paste exports exclude media attachments).",
                    Toast.LENGTH_LONG
                ).show()
            },
            onDismiss = { showDelinkDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(getCardBackground())
            .border(1.dp, if (isDefault) AccentGold.copy(alpha = 0.6f) else getGlassBorder(), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = AppAccentColor(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDefault) {
                        Surface(
                            color = AccentGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (prefLanguage == "bn") "ডিফল্ট" else "Default",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Surface(
                        color = if (isLinkedToExtras) AccentGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isLinkedToExtras) AccentGreen.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable {
                                if (isLinkedToExtras) showDelinkDialog = true
                                else {
                                    viewModel.linkOwnerAccountToExtras(account)
                                    Toast.makeText(context, "Linked to Extras page!", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isLinkedToExtras) Icons.Default.Link else Icons.Default.LinkOff,
                                contentDescription = null,
                                tint = if (isLinkedToExtras) AccentGreen else Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLinkedToExtras) {
                                    if (prefLanguage == "bn") "লিংকড (Extras)" else "Linked to Extras"
                                } else {
                                    if (prefLanguage == "bn") "প্রোফাইল ওনলি" else "Profile Only"
                                },
                                color = if (isLinkedToExtras) AccentGreen else AppSecondaryTextColor(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    var showMenu by remember { mutableStateOf(false) }
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = AppTextColor().copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = androidx.compose.ui.unit.DpOffset(x = (-12).dp, y = 4.dp),
                            modifier = Modifier.background(getDialogBackground())
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (prefLanguage == "bn") "সম্পাদনা করুন" else "Edit", color = AppTextColor(), fontWeight = FontWeight.Medium) },
                                onClick = { showMenu = false; onEdit() }
                            )
                            if (isLinkedToExtras) {
                                DropdownMenuItem(
                                    text = { Text(if (prefLanguage == "bn") "এক্সট্রা পেজ থেকে ডি-লিংক করুন" else "De-link from Extras Page", color = AccentPink, fontWeight = FontWeight.SemiBold) },
                                    onClick = { showMenu = false; showDelinkDialog = true }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(if (prefLanguage == "bn") "এক্সট্রা পেজে লিংক করুন" else "Link to Extras Page", color = AccentGreen, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.linkOwnerAccountToExtras(account)
                                        Toast.makeText(context, "Linked to Extras page!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (prefLanguage == "bn") "বিনে সরান" else "Move to Bin", color = AccentPink, fontWeight = FontWeight.Medium) },
                                onClick = { showMenu = false; showDeleteConfirm = true }
                            )
                        }
                    }

                    if (showDeleteConfirm) {
                        com.example.ui.components.Tier1DeleteDialog(
                            itemTitle = account.accountName.ifBlank { account.ownerName },
                            onConfirm = { onDelete(); showDeleteConfirm = false },
                            onDismiss = { showDeleteConfirm = false }
                        )
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

            Spacer(modifier = Modifier.height(2.dp))

            // Button to show details & QR code
            OutlinedButton(
                onClick = onShowDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 40.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AppAccentColor().copy(alpha = 0.4f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (prefLanguage == "bn") "পেমেন্ট তথ্য ও QR কোড দেখুন" else "Show Payment Details & QR",
                    color = AppAccentColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
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
                            .border(2.dp, AppAccentColor(), RoundedCornerShape(16.dp))
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
                        if (account.branchName.isNotBlank()) DetailRowItem(if (prefLanguage == "bn") " শাখা:" else "Branch:", account.branchName)
                        if (account.upiId.isNotBlank()) DetailRowItem(if (prefLanguage == "bn") "UPI ID:" else "UPI ID:", account.upiId, AppAccentColor())
                        if (account.ownerName.isNotBlank()) DetailRowItem(if (prefLanguage == "bn") "নাম:" else "Name:", account.ownerName)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun DetailRowItem(
    label: String,
    value: String,
    valueColor: Color = AppTextColor(),
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = AppSecondaryTextColor(), fontSize = 12.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun EditProfileModal(
    currentProfile: UserProfileEntity,
    defaultOwnerAccount: OwnerAccountEntity? = null,
    prefLanguage: String = "en",
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        contact: String,
        avatar: String,
        currency: String,
        bankName: String,
        accNum: String,
        ifsc: String,
        branch: String,
        upi: String,
        qrUri: String
    ) -> Unit
) {
    BackHandler { onDismiss() }

    var name by remember { mutableStateOf(currentProfile.name) }
    var contact by remember { mutableStateOf(currentProfile.contact) }
    var avatar by remember { mutableStateOf(currentProfile.avatar) }
    var currency by remember { mutableStateOf(currentProfile.currencySymbol) }

    var bankName by remember { mutableStateOf(defaultOwnerAccount?.accountName ?: "") }
    var accNum by remember { mutableStateOf(defaultOwnerAccount?.accountNumber ?: "") }
    var ifsc by remember { mutableStateOf(defaultOwnerAccount?.ifscCode ?: "") }
    var branch by remember { mutableStateOf(defaultOwnerAccount?.branchName ?: "") }
    var upi by remember { mutableStateOf(defaultOwnerAccount?.upiId ?: "") }
    var qrUri by remember { mutableStateOf(defaultOwnerAccount?.qrCodeUri ?: "") }
    val context = androidx.compose.ui.platform.LocalContext.current

    val qrPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { qrUri = com.example.ui.components.copyUriToPersistentFile(context, it, "QR") }
    }

    BackHandler { onDismiss() }

    val themeMode = com.example.ui.components.LocalAppTheme.current
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
        CompositionLocalProvider(
            com.example.ui.components.LocalAppTheme provides themeMode,
            com.example.ui.components.LocalAppBgPreset provides bgPreset,
            com.example.ui.components.LocalAppCustomBgUri provides customBgUri,
            com.example.ui.components.LocalAppLiquidGlassTheme provides isLiquidGlass
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isAppLightMode()) com.example.ui.theme.M3BackgroundLight else com.example.ui.theme.M3BackgroundDark
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(com.example.ui.components.getAppBackgroundGradient(themeMode, bgPreset))
                        .pointerInput(Unit) { detectTapGestures { } },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .dialogSystemBarsPadding(extraTop = 8.dp)
                            .imePadding(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0.dp),
                        topBar = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(getGlassIconButtonBg(), CircleShape)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = AppTextColor(), modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (prefLanguage == "bn") "প্রোফাইল তথ্য ও পেমেন্ট বিবরণী" else "Edit Profile & Payment Details",
                                    color = AppTextColor(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledIconButton(
                                    onClick = { onSave(name, contact, avatar, currency, bankName, accNum, ifsc, branch, upi, qrUri) },
                                    modifier = Modifier.size(40.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = AppAccentColor(),
                                        contentColor = AppOnAccentColor()
                                    )
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = AppOnAccentColor())
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(innerPadding)
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                Text(
                    text = if (prefLanguage == "bn") "মালিকের সাধারণ তথ্য" else "Owner Profile Info",
                    color = AppAccentColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (prefLanguage == "bn") "মালিকের নাম / ব্যবসার নাম" else "Owner / Business Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AppAccentColor(),
                        unfocusedBorderColor = getGlassBorder(),
                        focusedContainerColor = getCardBackground(),
                        unfocusedContainerColor = getCardBackground()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text(if (prefLanguage == "bn") "মোবাইল নম্বর" else "Contact Phone / Mobile") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AppAccentColor(),
                        unfocusedBorderColor = getGlassBorder(),
                        focusedContainerColor = getCardBackground(),
                        unfocusedContainerColor = getCardBackground()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (prefLanguage == "bn") "মুদ্রা (Currency):" else "Currency:",
                    color = AppTextColor(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    color = if (isAppLightMode()) Color(0xFFD3E3FD) else AppAccentColor().copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₹ (INR - Indian Rupee)",
                            color = AppTextColor(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = getGlassBorder().copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = if (prefLanguage == "bn") "প্রোফাইল লিঙ্কড ব্যাংক ও UPI বিবরণী" else "Profile Linked Bank & UPI Details",
                    color = AppAccentColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text(if (prefLanguage == "bn") "ব্যাংকের নাম / অ্যাকাউন্ট লেবেল" else "Bank Name / Label") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AppAccentColor(),
                        unfocusedBorderColor = getGlassBorder(),
                        focusedContainerColor = getCardBackground(),
                        unfocusedContainerColor = getCardBackground()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accNum,
                    onValueChange = { accNum = it },
                    label = { Text(if (prefLanguage == "bn") "অ্যাকাউন্ট নম্বর" else "Account Number") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AppAccentColor(),
                        unfocusedBorderColor = getGlassBorder(),
                        focusedContainerColor = getCardBackground(),
                        unfocusedContainerColor = getCardBackground()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ifsc,
                        onValueChange = { ifsc = it },
                        label = { Text(if (prefLanguage == "bn") "IFSC কোড" else "IFSC Code") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AppAccentColor(),
                            unfocusedBorderColor = getGlassBorder(),
                            focusedContainerColor = getCardBackground(),
                            unfocusedContainerColor = getCardBackground()
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = branch,
                        onValueChange = { branch = it },
                        label = { Text(if (prefLanguage == "bn") "শাখা" else "Branch") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AppAccentColor(),
                            unfocusedBorderColor = getGlassBorder(),
                            focusedContainerColor = getCardBackground(),
                            unfocusedContainerColor = getCardBackground()
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = upi,
                    onValueChange = { upi = it },
                    label = { Text(if (prefLanguage == "bn") "UPI ID (যেমন: example@upi)" else "UPI ID (e.g. name@upi)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextColor(),
                        unfocusedTextColor = AppTextColor(),
                        focusedBorderColor = AppAccentColor(),
                        unfocusedBorderColor = getGlassBorder(),
                        focusedContainerColor = getCardBackground(),
                        unfocusedContainerColor = getCardBackground()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // UPI QR Code Upload/Preview Box
                Text(
                    text = if (prefLanguage == "bn") "UPI QR কোড ছবি:" else "UPI QR Code Image:",
                    color = AppTextColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(getCardBackground())
                        .border(1.dp, getGlassBorder(), RoundedCornerShape(12.dp))
                        .clickable { qrPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (qrUri.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            coil.compose.AsyncImage(
                                model = qrUri,
                                contentDescription = "QR Code",
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                            Column {
                                Text(if (prefLanguage == "bn") "QR কোড সংযুক্ত আছে" else "QR Code Attached", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(if (prefLanguage == "bn") "ট্যাপ করে অন্য ছবি নির্বাচন করুন" else "Tap to change QR image", color = AppSecondaryTextColor(), fontSize = 11.sp)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = AppAccentColor())
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (prefLanguage == "bn") "+ UPI QR কোড আপলোড করুন" else "+ Upload UPI QR Code Image", color = AppAccentColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                        Spacer(modifier = Modifier.height(48.dp))
                    } // closes Box from 1343
                } // closes main vertical scroll Column
            } // closes Scaffold
        } // closes Box gradient
    } // closes Surface
} // closes CompositionLocalProvider
} // closes Dialog
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
                            .background(if (isSelected) AppAccentColor().copy(alpha = 0.2f) else getCardBackground())
                            .border(1.dp, if (isSelected) AppAccentColor() else getGlassBorder(), RoundedCornerShape(12.dp))
                            .clickable { onSelect(code) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(code) },
                            colors = RadioButtonDefaults.colors(selectedColor = AppAccentColor())
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
                Text("Close", color = AppAccentColor())
            }
        }
    )
}

@Composable
fun OwnerAccountEditorModal(
    account: OwnerAccountEntity?,
    prefLanguage: String,
    onDismiss: () -> Unit,
    onSave: (
        accName: String,
        accBal: Double,
        details: String,
        qrUri: String,
        holderName: String,
        accNum: String,
        ifsc: String,
        branch: String,
        upi: String
    ) -> Unit
) {
    BackHandler { onDismiss() }

    var accountName by remember(account) { mutableStateOf(account?.accountName ?: "") }
    var initialBalanceStr by remember(account) { mutableStateOf("") }
    var details by remember(account) { mutableStateOf("") }
    var qrCodeUri by remember(account) { mutableStateOf(account?.qrCodeUri ?: "") }
    var ownerName by remember(account) { mutableStateOf(account?.ownerName ?: "") }
    var accountNumber by remember(account) { mutableStateOf(account?.accountNumber ?: "") }
    var ifscCode by remember(account) { mutableStateOf(account?.ifscCode ?: "") }
    var branchName by remember(account) { mutableStateOf(account?.branchName ?: "") }
    var upiId by remember(account) { mutableStateOf(account?.upiId ?: "") }
    val context = androidx.compose.ui.platform.LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { qrCodeUri = com.example.ui.components.copyUriToPersistentFile(context, it, "QR") }
    }

    BackHandler { onDismiss() }

    val themeMode = com.example.ui.components.LocalAppTheme.current
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
        CompositionLocalProvider(
            com.example.ui.components.LocalAppTheme provides themeMode,
            com.example.ui.components.LocalAppBgPreset provides bgPreset,
            com.example.ui.components.LocalAppCustomBgUri provides customBgUri,
            com.example.ui.components.LocalAppLiquidGlassTheme provides isLiquidGlass
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isAppLightMode()) com.example.ui.theme.M3BackgroundLight else com.example.ui.theme.M3BackgroundDark
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(com.example.ui.components.getAppBackgroundGradient(themeMode, bgPreset))
                        .pointerInput(Unit) { detectTapGestures { } },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .dialogSystemBarsPadding(extraTop = 8.dp)
                            .imePadding(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0.dp),
                        topBar = {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(getGlassIconButtonBg(), CircleShape)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = AppTextColor(), modifier = Modifier.size(24.dp))
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
                                    fontSize = 18.sp
                                )
                            }
                        },
                bottomBar = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = AppSecondaryTextColor())
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (accountName.isNotBlank() || ownerName.isNotBlank()) {
                                    val bal = initialBalanceStr.toDoubleOrNull() ?: 0.0
                                    onSave(accountName, bal, details, qrCodeUri, ownerName, accountNumber, ifscCode, branchName, upiId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor(), contentColor = AppOnAccentColor()),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = if (account == null) "Add Account" else "Update",
                                color = AppOnAccentColor(),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) { pv ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pv)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Account Name
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text(if (prefLanguage == "bn") "অ্যাকাউন্টের নাম" else "Account Name (e.g. Cash, HDFC)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AppAccentColor(),
                            unfocusedBorderColor = getGlassBorder(),
                            focusedContainerColor = getCardBackground(),
                            unfocusedContainerColor = getCardBackground()
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Account Balance
                    OutlinedTextField(
                        value = initialBalanceStr,
                        onValueChange = { initialBalanceStr = it },
                        label = { Text(if (prefLanguage == "bn") "প্রারম্ভিক ব্যালেন্স (₹)" else "Account Balance (₹)") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AppAccentColor(),
                            unfocusedBorderColor = getGlassBorder(),
                            focusedContainerColor = getCardBackground(),
                            unfocusedContainerColor = getCardBackground()
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Account Details (Notes / Purpose)
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text(if (prefLanguage == "bn") "বিবরণ (ঐচ্ছিক)" else "Details (Optional)") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AppAccentColor(),
                            unfocusedBorderColor = getGlassBorder(),
                            focusedContainerColor = getCardBackground(),
                            unfocusedContainerColor = getCardBackground()
                        ),
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                    )
                    
                    // QR Code / Image section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(getCardBackground())
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (prefLanguage == "bn") "পেমেন্ট রিসিভার QR কোড (ঐচ্ছিক)" else "Payment Receiver QR (Optional)",
                                color = AppTextColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            if (qrCodeUri.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                    ) {
                                        AsyncImage(
                                            model = qrCodeUri,
                                            contentDescription = "Account Image",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(
                                            onClick = { imagePicker.launch("image/*") },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppAccentColor())
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
                                    border = BorderStroke(1.dp, AppAccentColor().copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppAccentColor())
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

                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AppAccentColor(),
                            unfocusedBorderColor = getGlassBorder(),
                            focusedContainerColor = getCardBackground(),
                            unfocusedContainerColor = getCardBackground()
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it },
                            label = { Text("IFSC Code") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AppTextColor(),
                                unfocusedTextColor = AppTextColor(),
                                focusedBorderColor = AppAccentColor(),
                                unfocusedBorderColor = getGlassBorder(),
                                focusedContainerColor = getCardBackground(),
                                unfocusedContainerColor = getCardBackground()
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = branchName,
                            onValueChange = { branchName = it },
                            label = { Text("Branch Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AppTextColor(),
                                unfocusedTextColor = AppTextColor(),
                                focusedBorderColor = AppAccentColor(),
                                unfocusedBorderColor = getGlassBorder(),
                                focusedContainerColor = getCardBackground(),
                                unfocusedContainerColor = getCardBackground()
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("UPI ID (e.g. name@upi)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AppAccentColor(),
                            unfocusedBorderColor = getGlassBorder(),
                            focusedContainerColor = getCardBackground(),
                            unfocusedContainerColor = getCardBackground()
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Account Holder Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextColor(),
                            unfocusedTextColor = AppTextColor(),
                            focusedBorderColor = AppAccentColor(),
                            unfocusedBorderColor = getGlassBorder(),
                            focusedContainerColor = getCardBackground(),
                            unfocusedContainerColor = getCardBackground()
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
}
}
