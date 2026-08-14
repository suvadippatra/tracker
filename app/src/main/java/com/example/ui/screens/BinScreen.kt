package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.BorderStroke
import com.example.data.model.BankAccountEntity
import com.example.data.model.HotelEntity
import com.example.data.model.MenuEntity
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.bounceClick
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun BinScreen(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val deletedHotels by viewModel.deletedHotels.collectAsStateWithLifecycle()
    val deletedBankAccounts by viewModel.deletedBankAccounts.collectAsStateWithLifecycle()
    val deletedMenuItems by viewModel.deletedMenuItems.collectAsStateWithLifecycle()
    val deletedTransactions by viewModel.deletedTransactions.collectAsStateWithLifecycle()
    val deletedExtraExpenses by viewModel.deletedExtraExpenses.collectAsStateWithLifecycle()
    val deletedMeterReadings by viewModel.deletedMeterReadings.collectAsStateWithLifecycle()
    val deletedUtilityBills by viewModel.deletedUtilityBills.collectAsStateWithLifecycle()

    var activeSection by remember { mutableStateOf("Hotels") }
    var searchQuery by remember { mutableStateOf("") }
    var showEmptyBinConfirmation by remember { mutableStateOf(false) }
    var itemToDeletePermanently by remember { mutableStateOf<Any?>(null) }

    val isBn = userProfile?.language == "bn"
    val translatedSectionName = when (activeSection) {
        "Hotels" -> if (isBn) "হোটেল" else "Hotels"
        "Accounts" -> if (isBn) "অ্যাকাউন্ট" else "Accounts"
        "Menu" -> if (isBn) "মেনু" else "Menu"
        "Meals" -> if (isBn) "খাবার" else "Meals"
        "Expenses" -> if (isBn) "খরচ/আয়" else "Expenses"
        "Readings" -> if (isBn) "মিটার রিডিং" else "Readings"
        else -> activeSection
    }

    val filteredHotels = remember(deletedHotels, searchQuery) {
        deletedHotels.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.location.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredBankAccounts = remember(deletedBankAccounts, searchQuery) {
        deletedBankAccounts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.accountNumber.contains(searchQuery, ignoreCase = true) ||
            it.upiId.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredMenuItems = remember(deletedMenuItems, searchQuery) {
        deletedMenuItems.filter {
            it.englishName.contains(searchQuery, ignoreCase = true) ||
            it.banglaName.contains(searchQuery, ignoreCase = true) ||
            it.type.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredTransactions = remember(deletedTransactions, searchQuery) {
        deletedTransactions.filter {
            it.itemsSerialized.contains(searchQuery, ignoreCase = true) ||
            it.totalAmount.toString().contains(searchQuery)
        }
    }
    val filteredExtraExpenses = remember(deletedExtraExpenses, searchQuery) {
        deletedExtraExpenses.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.amount.toString().contains(searchQuery)
        }
    }
    val filteredMeterReadings = remember(deletedMeterReadings, searchQuery) {
        deletedMeterReadings.filter {
            it.unitsConsumed.toString().contains(searchQuery) ||
            it.totalReading.toString().contains(searchQuery)
        }
    }

    val isCurrentSectionEmpty = when (activeSection) {
        "Hotels" -> deletedHotels.isEmpty()
        "Accounts" -> deletedBankAccounts.isEmpty()
        "Menu" -> deletedMenuItems.isEmpty()
        "Meals" -> deletedTransactions.isEmpty()
        "Expenses" -> deletedExtraExpenses.isEmpty()
        "Readings" -> deletedMeterReadings.isEmpty()
        "Bills" -> deletedUtilityBills.isEmpty()
        else -> true
    }

    androidx.activity.compose.BackHandler {
        onDismiss()
    }
    
    com.example.ui.components.AppThemeProvider(viewModel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getDialogBackground())
                .pointerInput(Unit) { detectTapGestures { } }
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Scaffold(
                modifier = Modifier.fillMaxHeight().widthIn(max = 840.dp),
                containerColor = Color.Transparent,
                contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(getGlassBorder().copy(alpha = 0.1f), CircleShape).bounceClick()
                        ) {
                            Icon(Icons.Default.ArrowBack, if (isBn) "ফিরে যান" else "Back", tint = AppTextColor())
                        }
                        Text(if (isBn) "রিসাইকেল বিন" else "Recycle Bin", color = AppTextColor(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        
                        if (!isCurrentSectionEmpty) {
                            IconButton(
                                onClick = { showEmptyBinConfirmation = true },
                                modifier = Modifier.background(AccentPink.copy(alpha = 0.1f), CircleShape).bounceClick()
                            ) {
                                Icon(Icons.Default.DeleteSweep, if (isBn) "বিভাগ খালি করুন" else "Empty Section", tint = AccentPink)
                            }
                        } else {
                            Box(modifier = Modifier.size(48.dp))
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    // Section Selector Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .border(1.dp, getGlassBorder().copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            "Hotels" to (if (isBn) "হোটেল" else "Hotels"),
                            "Accounts" to (if (isBn) "অ্যাকাউন্ট" else "Accounts"),
                            "Menu" to (if (isBn) "মেনু" else "Menu"),
                            "Meals" to (if (isBn) "খাবার" else "Meals"),
                            "Expenses" to (if (isBn) "খরচ/আয়" else "Expenses"),
                            "Readings" to (if (isBn) "মিটার রিডিং" else "Readings")
                        )
                        tabs.forEach { (section, label) ->
                            val isSelected = activeSection == section
                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentCyan.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { 
                                        activeSection = section
                                        searchQuery = "" // Clear search when switching tabs
                                    }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Redesigned Search Input Field
                    GlassTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(if (isBn) "মুছে ফেলা $translatedSectionName খুঁজুন..." else "Search deleted $activeSection...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = AppTextColor().copy(alpha = 0.4f)) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, if (isBn) "মুছুন" else "Clear", tint = AppTextColor().copy(alpha = 0.5f))
                                }
                            }
                        } else null
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // List items container
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        when (activeSection) {
                            "Hotels" -> {
                                if (filteredHotels.isEmpty()) {
                                    item { 
                                        EmptyBinState(
                                            if (isBn) {
                                                if (searchQuery.isEmpty()) "কোন মুছে ফেলা হোটেল নেই"
                                                else "'$searchQuery'-এর সাথে মানানসই কোন মুছে ফেলা হোটেল পাওয়া যায়নি"
                                            } else {
                                                if (searchQuery.isEmpty()) "No deleted hotels" 
                                                else "No deleted hotels matching '$searchQuery'"
                                            }
                                        ) 
                                    }
                                } else {
                                    items(filteredHotels, key = { it.id }) { hotel ->
                                        val locText = if (hotel.location.isBlank()) (if (isBn) "কোন অবস্থান নেই" else "No location") else hotel.location
                                        val phoneText = if (hotel.phoneNumber.isBlank()) (if (isBn) "কোন ফোন নম্বর নেই" else "No phone") else hotel.phoneNumber
                                        BinItemCard(
                                            title = hotel.name,
                                            subtitle = "$locText • $phoneText",
                                            infoLabel = if (isBn) "অগ্রিম: ₹${hotel.advancedPayment.toInt()}" else "Advance: ₹${hotel.advancedPayment.toInt()}",
                                            onRestore = { viewModel.restoreHotel(hotel) },
                                            onDeletePermanently = { itemToDeletePermanently = hotel }
                                        )
                                    }
                                }
                            }
                            "Accounts" -> {
                                if (filteredBankAccounts.isEmpty()) {
                                    item { 
                                        EmptyBinState(
                                            if (isBn) {
                                                if (searchQuery.isEmpty()) "কোন মুছে ফেলা ব্যাংক অ্যাকাউন্ট নেই"
                                                else "'$searchQuery'-এর সাথে মানানসই কোন মুছে ফেলা অ্যাকাউন্ট পাওয়া যায়নি"
                                            } else {
                                                if (searchQuery.isEmpty()) "No deleted bank accounts" 
                                                else "No deleted accounts matching '$searchQuery'"
                                            }
                                        ) 
                                    }
                                } else {
                                    items(filteredBankAccounts, key = { it.id }) { acc ->
                                        val accNumText = if (acc.accountNumber.isBlank()) (if (isBn) "কোন অ্যাকাউন্ট নম্বর নেই" else "No A/C No") else "A/C: ${acc.accountNumber}"
                                        val ifscText = if (acc.ifsc.isBlank()) "" else " • IFSC: ${acc.ifsc}"
                                        BinItemCard(
                                            title = acc.name,
                                            subtitle = "$accNumText$ifscText",
                                            infoLabel = if (isBn) "ব্যালেন্স: ₹${acc.balance}" else "Balance: ₹${acc.balance}",
                                            onRestore = { viewModel.restoreVirtualAccount(acc) },
                                            onDeletePermanently = { itemToDeletePermanently = acc }
                                        )
                                    }
                                }
                            }
                            "Menu" -> {
                                if (filteredMenuItems.isEmpty()) {
                                    item { 
                                        EmptyBinState(
                                            if (isBn) {
                                                if (searchQuery.isEmpty()) "কোন মুছে ফেলা মেনু আইটেম নেই"
                                                else "'$searchQuery'-এর সাথে মানানসই কোন মুছে ফেলা মেনু আইটেম পাওয়া যায়নি"
                                            } else {
                                                if (searchQuery.isEmpty()) "No deleted menu items" 
                                                else "No deleted menu items matching '$searchQuery'"
                                            }
                                        ) 
                                    }
                                } else {
                                    items(filteredMenuItems, key = { it.id }) { menu ->
                                        val typeText = when (menu.type) {
                                            "FOOD" -> if (isBn) "খাবার" else "FOOD"
                                            "OTHERS" -> if (isBn) "অন্যান্য" else "OTHERS"
                                            else -> menu.type
                                        }
                                        val sub = if (menu.banglaName == menu.englishName) typeText else "${menu.banglaName} • $typeText"
                                        BinItemCard(
                                            title = "${menu.emoji} ${menu.englishName}",
                                            subtitle = sub,
                                            infoLabel = if (isBn) "মূল্য: ₹${menu.price}" else "Price: ₹${menu.price}",
                                            onRestore = { viewModel.restoreMenu(menu) },
                                            onDeletePermanently = { itemToDeletePermanently = menu }
                                        )
                                    }
                                }
                            }
                            "Meals" -> {
                                if (filteredTransactions.isEmpty()) {
                                    item {
                                        EmptyBinState(
                                            if (isBn) {
                                                if (searchQuery.isEmpty()) "কোন মুছে ফেলা খাবারের অর্ডার নেই"
                                                else "'$searchQuery'-এর সাথে মানানসই কোন খাবারের অর্ডার পাওয়া যায়নি"
                                            } else {
                                                if (searchQuery.isEmpty()) "No deleted meal orders"
                                                else "No deleted meal orders matching '$searchQuery'"
                                            }
                                        )
                                    }
                                } else {
                                    items(filteredTransactions, key = { it.id }) { tx ->
                                        val items = com.example.data.model.TransactionSerializer.deserialize(tx.itemsSerialized)
                                        val itemNames = items.joinToString(", ") { "${it.name} x${it.quantity}" }
                                        val formattedDate = try {
                                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                                            sdf.format(java.util.Date(tx.timestamp))
                                        } catch (e: Exception) { "" }
                                        val isNightText = if (tx.isNight) (if (isBn) "রাত" else "Night") else (if (isBn) "দিন" else "Day")
                                        BinItemCard(
                                            title = if (itemNames.isNotBlank()) itemNames else (if (isBn) "খাবারের অর্ডার #${tx.id}" else "Meal Order #${tx.id}"),
                                            subtitle = "$formattedDate • $isNightText",
                                            infoLabel = if (isBn) "মোট: ₹${tx.totalAmount}" else "Total: ₹${tx.totalAmount}",
                                            onRestore = { viewModel.restoreTransaction(tx) },
                                            onDeletePermanently = { itemToDeletePermanently = tx }
                                        )
                                    }
                                }
                            }
                            "Expenses" -> {
                                if (filteredExtraExpenses.isEmpty()) {
                                    item {
                                        EmptyBinState(
                                            if (isBn) {
                                                if (searchQuery.isEmpty()) "কোন মুছে ফেলা খরচ বা আয় নেই"
                                                else "'$searchQuery'-এর সাথে মানানসই কোন মুছে ফেলা লেনদেন পাওয়া যায়নি"
                                            } else {
                                                if (searchQuery.isEmpty()) "No deleted expenses/income"
                                                else "No deleted expenses matching '$searchQuery'"
                                            }
                                        )
                                    }
                                } else {
                                    items(filteredExtraExpenses, key = { it.id }) { ex ->
                                        val formattedDate = try {
                                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                                            sdf.format(java.util.Date(ex.timestamp))
                                        } catch (e: Exception) { "" }
                                        val catText = when (ex.category) {
                                            "ROOM_SERVICE" -> if (isBn) "রুম সার্ভিস" else "ROOM_SERVICE"
                                            "LAUNDRY" -> if (isBn) "লন্ড্রি" else "LAUNDRY"
                                            "REST_RENT" -> if (isBn) "রেস্টুরেন্ট ভাড়া" else "REST_RENT"
                                            "SALARY" -> if (isBn) "বেতন" else "SALARY"
                                            "UTILITY" -> if (isBn) "ইউটিলিটি বিল" else "UTILITY"
                                            "SUPPLY" -> if (isBn) "সরবরাহ খরচ" else "SUPPLY"
                                            "REPAIR" -> if (isBn) "মেরামত" else "REPAIR"
                                            "INVEST" -> if (isBn) "বিনিয়োগ" else "INVEST"
                                            "LOAN" -> if (isBn) "ঋণ" else "LOAN"
                                            "OTHERS" -> if (isBn) "অন্যান্য" else "OTHERS"
                                            else -> ex.category
                                        }
                                        val subtitleText = "$catText • $formattedDate" + if (ex.notes.isNotBlank()) " (${ex.notes})" else ""
                                        val typeLabelText = if (ex.type == "INCOME") {
                                            if (isBn) "আয়: ₹${ex.amount}" else "Income: ₹${ex.amount}"
                                        } else {
                                            if (isBn) "খরচ: ₹${ex.amount}" else "Expense: ₹${ex.amount}"
                                        }
                                        BinItemCard(
                                            title = ex.name,
                                            subtitle = subtitleText,
                                            infoLabel = typeLabelText,
                                            onRestore = { viewModel.restoreExtraExpense(ex) },
                                            onDeletePermanently = { itemToDeletePermanently = ex }
                                        )
                                    }
                                }
                            }
                            "Readings" -> {
                                if (filteredMeterReadings.isEmpty()) {
                                    item {
                                        EmptyBinState(
                                            if (isBn) {
                                                if (searchQuery.isEmpty()) "কোন মুছে ফেলা মিটার রিডিং নেই"
                                                else "'$searchQuery'-এর সাথে মানানসই কোন মিটার রিডিং পাওয়া যায়নি"
                                            } else {
                                                if (searchQuery.isEmpty()) "No deleted meter readings"
                                                else "No deleted readings matching '$searchQuery'"
                                            }
                                        )
                                    }
                                } else {
                                    items(filteredMeterReadings, key = { it.id }) { r ->
                                        val formattedDate = try {
                                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                                            sdf.format(java.util.Date(r.readingDate))
                                        } catch (e: Exception) { "" }
                                        val isPaidLabel = if (r.isPaid) (if (isBn) "পরিশোধিত" else "Paid") else (if (isBn) "অপরিশোধিত" else "Unpaid")
                                        BinItemCard(
                                            title = if (isBn) "${r.unitsConsumed} ইউনিট ব্যবহৃত হয়েছে" else "${r.unitsConsumed} Units Consumed",
                                            subtitle = if (isBn) "রিডিং: ${r.totalReading} • $formattedDate" else "Reading: ${r.totalReading} • $formattedDate",
                                            infoLabel = isPaidLabel,
                                            onRestore = { viewModel.restoreMeterReading(r) },
                                            onDeletePermanently = { itemToDeletePermanently = r }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Double confirmation dialog for Permanent Deletion of a single item
    if (itemToDeletePermanently != null) {
        val item = itemToDeletePermanently!!
        val name = when (item) {
            is HotelEntity -> item.name
            is BankAccountEntity -> item.name
            is MenuEntity -> item.englishName
            is com.example.data.model.HotelTransactionEntity -> if (isBn) "খাবারের অর্ডার (₹${item.totalAmount})" else "Meal Order (₹${item.totalAmount})"
            is com.example.data.model.ExtraExpenseEntity -> "${item.name} (₹${item.amount})"
            is com.example.data.model.MeterReading -> if (isBn) "${item.unitsConsumed} ইউনিট ব্যবহৃত মিটার রিডিং" else "${item.unitsConsumed} Units Reading"
                    is com.example.data.model.UtilityBill -> if (isBn) "${item.totalUnits} ইউনিটের বিল" else "Bill for ${item.totalUnits} Units"
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { itemToDeletePermanently = null },
            title = { Text(if (isBn) "স্থায়ীভাবে মুছে ফেলবেন?" else "Delete Permanently?", color = AppTextColor(), fontWeight = FontWeight.Bold) },
            text = { Text(if (isBn) "আপনি কি নিশ্চিত যে আপনি '$name' স্থায়ীভাবে মুছে ফেলতে চান? এই তথ্য আর পুনরুদ্ধার করা যাবে না।" else "Are you sure you want to permanently delete '$name'? This data cannot be recovered.", color = AppTextColor()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (item) {
                            is HotelEntity -> viewModel.deleteHotelPermanently(item)
                            is BankAccountEntity -> viewModel.deleteVirtualAccountPermanently(item)
                            is MenuEntity -> viewModel.deleteMenuPermanently(item)
                            is com.example.data.model.HotelTransactionEntity -> viewModel.deleteTransactionPermanently(item)
                            is com.example.data.model.ExtraExpenseEntity -> viewModel.deleteExtraExpensePermanently(item)
                            is com.example.data.model.MeterReading -> viewModel.deleteReading(item)
                            is com.example.data.model.UtilityBill -> viewModel.deleteUtilityBillPermanently(item)
                        }
                        itemToDeletePermanently = null
                    }
                ) {
                    Text(if (isBn) "স্থায়ীভাবে মুছুন" else "Delete Permanently", color = AccentPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeletePermanently = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel", color = AppTextColor().copy(alpha = 0.6f))
                }
            },
            containerColor = getDialogBackground()
        )
    }

    // Confirmation dialog for Emptying the active section completely
    if (showEmptyBinConfirmation) {
        AlertDialog(
            onDismissRequest = { showEmptyBinConfirmation = false },
            title = { Text(if (isBn) "$translatedSectionName ট্র্যাশ খালি করবেন?" else "Empty $activeSection Trash?", color = AppTextColor(), fontWeight = FontWeight.Bold) },
            text = { Text(if (isBn) "এটি স্থায়ীভাবে বর্তমান $translatedSectionName ট্যাবের সমস্ত মুছে ফেলা আইটেম মুছে ফেলবে। এই কাজটি পুনরুদ্ধারযোগ্য নয়।" else "This will permanently delete ALL soft-deleted items currently in the $activeSection tab. This action is irreversible.", color = AppTextColor()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyBinConfirmation = false
                        when (activeSection) {
                            "Hotels" -> viewModel.emptyDeletedHotelsPermanently()
                            "Accounts" -> viewModel.emptyDeletedBankAccountsPermanently()
                            "Menu" -> viewModel.emptyDeletedMenuItemsPermanently()
                            "Meals" -> viewModel.emptyDeletedTransactionsPermanently()
                            "Expenses" -> viewModel.emptyDeletedExpensesPermanently()
                            "Readings" -> viewModel.emptyDeletedReadingsPermanently()
                            "Bills" -> viewModel.emptyDeletedUtilityBillsPermanently()
                        }
                    }
                ) {
                    Text(if (isBn) "বিভাগ খালি করুন" else "Empty Section", color = AccentPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyBinConfirmation = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel", color = AppTextColor().copy(alpha = 0.6f))
                }
            },
            containerColor = getDialogBackground()
        )
    }
}

@Composable
fun BinItemCard(
    title: String,
    subtitle: String,
    infoLabel: String,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = AppTextColor().copy(alpha = 0.5f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = infoLabel,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRestore,
                    modifier = Modifier
                        .size(36.dp)
                        .background(AccentGreen.copy(alpha = 0.12f), CircleShape)
                        .bounceClick()
                ) {
                    Icon(Icons.Default.Restore, "Restore", tint = AccentGreen, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onDeletePermanently,
                    modifier = Modifier
                        .size(36.dp)
                        .background(AccentPink.copy(alpha = 0.12f), CircleShape)
                        .bounceClick()
                ) {
                    Icon(Icons.Default.DeleteForever, "Delete Permanently", tint = AccentPink, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyBinState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DeleteOutline, null, tint = AppTextColor().copy(alpha = 0.2f), modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(message, color = AppTextColor().copy(alpha = 0.4f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
