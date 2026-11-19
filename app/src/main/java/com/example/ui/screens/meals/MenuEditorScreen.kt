@file:Suppress("DEPRECATION")
package com.example.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.components.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HotelEntity
import com.example.data.model.MenuEntity
import com.example.ui.components.AppTextColor
import com.example.ui.components.AccentCyan
import com.example.ui.components.AccentPink
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.components.bounceClick
import com.example.ui.components.getAppBackgroundGradient
import com.example.ui.components.getGlassIconButtonBg
import com.example.ui.viewmodel.FinancialViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MenuManagementScreen(
    hotel: HotelEntity,
    viewModel: FinancialViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    val allMenus by viewModel.allHotelMenus.collectAsStateWithLifecycle()
    val menuItems = remember(allMenus, hotel.id) {
        allMenus.filter { it.hotelId == hotel.id && !it.isDeleted }
    }
    val hotelHistory by viewModel.getHistoryForHotel(hotel.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddMenu by remember { mutableStateOf(false) }
    var showHotelMenuHistory by remember { mutableStateOf(false) }
    var selectedItemForHistory by remember { mutableStateOf<MenuEntity?>(null) }

    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write("Name,Price,Type,Emoji\n")
                    menuItems.forEach { m ->
                        val safeName = m.englishName.replace(",", " ")
                        val safeEmoji = m.emoji.replace(",", " ")
                        writer.write("${safeName},${m.price},${m.type},${safeEmoji}\n")
                    }
                }
                android.widget.Toast.makeText(context, "Menu Exported Successfully", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export Failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val parts = line.split(",")
                        if (parts.size >= 4) {
                            val name = parts[0].trim()
                            val price = parts[1].trim().toDoubleOrNull() ?: 0.0
                            val type = parts[2].trim()
                            val emoji = parts[3].trim()
                            viewModel.addMenu(MenuEntity(
                                hotelId = hotel.id,
                                banglaName = name,
                                englishName = name,
                                price = price,
                                isDayMenu = type.equals("Day", ignoreCase = true) || type.equals("Common", ignoreCase = true),
                                isNightMenu = type.equals("Night", ignoreCase = true) || type.equals("Common", ignoreCase = true),
                                isAddOn = false,
                                emoji = emoji,
                                type = type
                            ))
                        }
                    }
                }
                android.widget.Toast.makeText(context, "Menu Imported Successfully", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import Failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBn = userProfile?.language == "bn"
    val themeMode = userProfile?.themeMode ?: "Auto"
    val bgPreset = userProfile?.bgPreset ?: "Midnight Glow"
    val customBgUri = userProfile?.customBgUri ?: ""
    val isLiquidGlass = userProfile?.enableLiquidGlassTheme ?: false

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
            CompositionLocalProvider(
                LocalAppTheme provides themeMode,
                LocalAppBgPreset provides bgPreset,
                LocalAppCustomBgUri provides customBgUri,
                LocalAppLiquidGlassTheme provides isLiquidGlass
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isAppLightMode()) com.example.ui.theme.M3BackgroundLight else com.example.ui.theme.M3BackgroundDark
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(getAppBackgroundGradient(themeMode, bgPreset)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding(),
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets(0.dp),
                            topBar = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .dialogSafeTopPadding(extraTop = 4.dp)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = onBack,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(getGlassIconButtonBg(), CircleShape)
                                            .bounceClick()
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = AppTextColor(), modifier = Modifier.size(26.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = hotel.name,
                                            color = AppTextColor(),
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isBn) "খাবার আইটেম ও মূল্য পরিবর্তনের ইতিহাস" else "Manage menu items & price change history",
                                            color = AppTextColor().copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = { showHotelMenuHistory = true },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(getGlassIconButtonBg(), CircleShape)
                                            .bounceClick()
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = "Menu Change History", tint = AppAccentColor())
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { csvImportLauncher.launch("text/*") },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(getGlassIconButtonBg(), CircleShape)
                                            .bounceClick()
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = "Import CSV", tint = AppTextColor())
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { csvExportLauncher.launch("menu_${hotel.name.replace(" ", "_")}.csv") },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(getGlassIconButtonBg(), CircleShape)
                                            .bounceClick()
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = AppTextColor())
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .padding(top = 8.dp)
                            ) {
                                // Table Content
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Table Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isAppLightMode()) Color(0xFFE8EEF5) else Color.White.copy(alpha = 0.06f))
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Item", color = AppTextColor(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                                            Text("Type", color = AppTextColor(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            Text("Price", color = AppTextColor(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        }
                                        
                                        HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f))

                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(bottom = 24.dp)
                                        ) {
                                            items(menuItems, key = { it.id }) { item ->
                                                MenuTableRow(
                                                    item = item,
                                                    onSave = { updatedItem, effectiveDateMillis ->
                                                        viewModel.updateMenu(updatedItem, effectiveDateMillis)
                                                    },
                                                    onViewHistory = { selectedItemForHistory = item },
                                                    onDelete = { viewModel.deleteMenu(item) }
                                                )
                                                HorizontalDivider(color = AppTextColor().copy(alpha = 0.05f))
                                            }
                                        }
                                    }
                                }
                                
                                // Bottom Add Area
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .dialogSafeBottomPadding(extraBottom = 8.dp)
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Button(
                                        onClick = { showAddMenu = true },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppAccentColor(),
                                            contentColor = AppOnAccentColor()
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Item", tint = AppOnAccentColor())
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add New Item", color = AppOnAccentColor(), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showAddMenu) {
                AddMenuItemDialog(
                    hotelId = hotel.id,
                    onDismiss = { showAddMenu = false },
                    onSave = { newItem, effectiveDateMillis ->
                        viewModel.addMenu(newItem, effectiveDateMillis)
                        showAddMenu = false
                    }
                )
            }

            var historyToDelete by remember { mutableStateOf<com.example.data.model.MenuModificationHistoryEntity?>(null) }

            if (historyToDelete != null) {
                val delItem = historyToDelete!!
                AlertDialog(
                    onDismissRequest = { historyToDelete = null },
                    containerColor = getDialogBackground(),
                    shape = RoundedCornerShape(22.dp),
                    title = {
                        Text(
                            text = if (isBn) "মূল্য পরিবর্তনের এন্ট্রি মুছবেন?" else "Delete Price History Entry?",
                            color = AppTextColor(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    },
                    text = {
                        Text(
                            text = if (isBn) "আপনি কি \"${delItem.itemName}\" এর এই রেকর্ডটি (${delItem.changeSummary}) মুছে ফেলতে চান?"
                            else "Remove this modification record for \"${delItem.itemName}\" (${delItem.changeSummary})?",
                            color = AppSecondaryTextColor(),
                            fontSize = 13.5.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteMenuHistory(delItem)
                                historyToDelete = null
                            }
                        ) {
                            Text(if (isBn) "মুছে ফেলুন" else "Delete", color = AccentPink, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { historyToDelete = null }) {
                            Text(if (isBn) "বাতিল" else "Cancel", color = AppSecondaryTextColor())
                        }
                    }
                )
            }

            if (showHotelMenuHistory || selectedItemForHistory != null) {
                val itemSpecific = selectedItemForHistory
                val historyToDisplay = remember(hotelHistory, itemSpecific) {
                    if (itemSpecific != null) hotelHistory.filter { it.menuItemId == itemSpecific.id }
                    else hotelHistory
                }
                AlertDialog(
                    onDismissRequest = {
                        showHotelMenuHistory = false
                        selectedItemForHistory = null
                    },
                    containerColor = getDialogBackground(),
                    shape = RoundedCornerShape(24.dp),
                    title = {
                        Column {
                            Text(
                                text = if (itemSpecific != null) {
                                    if (isBn) "${itemSpecific.emoji} ${itemSpecific.englishName} - পরিবর্তনের ইতিহাস"
                                    else "${itemSpecific.emoji} ${itemSpecific.englishName} - Change History"
                                } else {
                                    if (isBn) "মেনু ও মূল্য পরিবর্তনের ইতিহাস" else "Menu Modification History"
                                },
                                color = AppTextColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBn) "ভুল এন্ট্রি মুছতে সেটির উপর লং-প্রেস করুন।"
                                else "Long-press any entry to remove a wrong price history record.",
                                color = AppAccentColor(),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    },
                    text = {
                        if (historyToDisplay.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBn) "এখনও কোনো দাম বা নামের পরিবর্তন রেকর্ড করা হয়নি।" else "No price or name modifications recorded yet.",
                                    color = AppSecondaryTextColor(),
                                    fontSize = 13.5.sp
                                )
                            }
                        } else {
                            val sdf = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                historyToDisplay.forEach { rec ->
                                    val dateStr = runCatching { sdf.format(java.util.Date(rec.modifiedAt)) }.getOrDefault("")
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = getCardBackground(),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, getGlassBorder()),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = { historyToDelete = rec }
                                            )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = rec.itemName,
                                                    color = AppTextColor(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = dateStr,
                                                    color = AppSecondaryTextColor(),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (rec.changeSummary.isNotBlank()) {
                                                Text(
                                                    text = rec.changeSummary,
                                                    color = AccentGreen,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                if (rec.oldPrice != rec.newPrice) {
                                                    Text(
                                                        text = if (isBn) "দাম: ₹${rec.oldPrice.toInt()} → ₹${rec.newPrice.toInt()}" else "Price: ₹${rec.oldPrice.toInt()} → ₹${rec.newPrice.toInt()}",
                                                        color = AccentGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                } else {
                                                    Text(
                                                        text = "₹${rec.newPrice.toInt()}",
                                                        color = AppSecondaryTextColor(),
                                                        fontSize = 12.5.sp
                                                    )
                                                }
                                                if (rec.oldType != rec.newType) {
                                                    Text(
                                                        text = "${rec.oldType} → ${rec.newType}",
                                                        color = AppAccentColor(),
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.5.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showHotelMenuHistory = false
                                selectedItemForHistory = null
                            }
                        ) {
                            Text(if (isBn) "বন্ধ করুন" else "Close", color = AppAccentColor(), fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }

@Composable
fun MenuTableRow(
    item: MenuEntity,
    onSave: (MenuEntity, Long) -> Unit,
    onViewHistory: () -> Unit = {},
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    
    var editName by remember(item.englishName) { mutableStateOf(item.englishName) }
    var editPrice by remember(item.price) { mutableStateOf(item.price.toString()) }
    var editType by remember(item.type) { mutableStateOf(item.type) }
    var editEmoji by remember(item.emoji) { mutableStateOf(item.emoji) }
    var effectiveDateMillis by remember(item.id) { mutableLongStateOf(System.currentTimeMillis()) }
    var showEffectiveDatePicker by remember { mutableStateOf(false) }
    var showDiscardRowDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val hasRowChanges = isEditing && (
        editName != item.englishName ||
            editPrice != item.price.toString() ||
            !editType.equals(item.type, ignoreCase = true) ||
            editEmoji != item.emoji
    )

    if (isEditing) {
        BackHandler {
            if (hasRowChanges) {
                showDiscardRowDialog = true
            } else {
                isEditing = false
            }
        }
    }

    if (showDiscardRowDialog) {
        UnsavedChangesWarningDialog(
            onKeepEditing = { showDiscardRowDialog = false },
            onDiscard = {
                showDiscardRowDialog = false
                editName = item.englishName
                editPrice = item.price.toString()
                editType = item.type
                editEmoji = item.emoji
                isEditing = false
            }
        )
    }

    if (showEffectiveDatePicker) {
        SlidingDatePickerModal(
            initialDateMillis = effectiveDateMillis,
            onDateSelected = { picked ->
                effectiveDateMillis = picked
                showEffectiveDatePicker = false
            },
            onDismiss = { showEffectiveDatePicker = false }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isEditing) isEditing = true }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditing) {
            Column(modifier = Modifier.weight(4f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GlassTextField(
                        value = editEmoji, onValueChange = { editEmoji = it },
                        modifier = Modifier.width(55.dp),
                        singleLine = true
                    )
                    GlassTextField(
                        value = editName, onValueChange = { editName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "Day" to Icons.Default.WbSunny,
                        "Night" to Icons.Default.NightsStay,
                        "Common" to Icons.Default.Brightness4
                    ).forEach { (tOpt, icon) ->
                        val selected = editType.equals(tOpt, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) AccentCyan else AppTextColor().copy(alpha = 0.08f))
                                .clickable { editType = tOpt }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tOpt,
                                    tint = if (selected) Color.Black else AppTextColor(),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = tOpt,
                                    color = if (selected) Color.Black else AppTextColor(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    GlassTextField(
                        value = editPrice, onValueChange = { editPrice = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(75.dp),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Effective Date selector for price changes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppAccentColor().copy(alpha = 0.10f))
                        .clickable { showEffectiveDatePicker = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Effective Date",
                            tint = AppAccentColor(),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "* Effective Date (Required):",
                            color = AppSecondaryTextColor(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = runCatching { dateFormat.format(java.util.Date(effectiveDateMillis)) }.getOrDefault("Today"),
                        color = AppAccentColor(),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.width(44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        val p = editPrice.toDoubleOrNull() ?: item.price
                        if (editName.isNotBlank()) {
                            onSave(
                                item.copy(
                                    englishName = editName,
                                    banglaName = editName,
                                    price = p,
                                    emoji = editEmoji,
                                    type = editType,
                                    isDayMenu = editType.equals("Day", ignoreCase = true) || editType.equals("Common", ignoreCase = true),
                                    isNightMenu = editType.equals("Night", ignoreCase = true) || editType.equals("Common", ignoreCase = true)
                                ),
                                effectiveDateMillis
                            )
                            isEditing = false
                        }
                    },
                    modifier = Modifier.size(32.dp).background(AccentCyan.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = AccentCyan, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = {
                        if (hasRowChanges) {
                            showDiscardRowDialog = true
                        } else {
                            isEditing = false
                        }
                    },
                    modifier = Modifier.size(28.dp).background(AccentPink.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AccentPink, modifier = Modifier.size(14.dp))
                }
            }
        } else {
            Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
                Text(item.emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.englishName, color = AppTextColor(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            // Day/Night Toggle Badge instead of plain "d a y" text
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (item.type) {
                            "Night" -> Color(0xFF3F51B5).copy(alpha = 0.25f)
                            "Common" -> AccentCyan.copy(alpha = 0.25f)
                            else -> Color(0xFFFF9800).copy(alpha = 0.25f)
                        }
                    )
                    .clickable {
                        val nextType = when (item.type) {
                            "Day" -> "Night"
                            "Night" -> "Common"
                            else -> "Day"
                        }
                        onSave(
                            item.copy(
                                type = nextType,
                                isDayMenu = nextType == "Day" || nextType == "Common",
                                isNightMenu = nextType == "Night" || nextType == "Common"
                            ),
                            System.currentTimeMillis()
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (item.type) {
                            "Night" -> Icons.Default.NightsStay
                            "Common" -> Icons.Default.Brightness4
                            else -> Icons.Default.WbSunny
                        },
                        contentDescription = item.type,
                        tint = when (item.type) {
                            "Night" -> Color(0xFF9FA8DA)
                            "Common" -> AccentCyan
                            else -> Color(0xFFFFCC80)
                        },
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.type,
                        color = AppTextColor(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text("₹${item.price.toInt()}", color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                var showMenu by remember { mutableStateOf(false) }
                var showDeleteConfirm by remember { mutableStateOf(false) }
                
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = AppTextColor().copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(com.example.ui.components.getDialogBackground())) {
                        DropdownMenuItem(
                            text = { Text("Edit", color = AppTextColor(), fontWeight = FontWeight.Medium) },
                            onClick = { showMenu = false; isEditing = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Price & Change History", color = AppAccentColor(), fontWeight = FontWeight.Medium) },
                            onClick = { showMenu = false; onViewHistory() }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Bin", color = AccentPink, fontWeight = FontWeight.Medium) },
                            onClick = { showMenu = false; showDeleteConfirm = true }
                        )
                    }
                }
                
                if (showDeleteConfirm) {
                    com.example.ui.components.Tier1DeleteDialog(
                        itemTitle = item.englishName,
                        onConfirm = { onDelete(); showDeleteConfirm = false },
                        onDismiss = { showDeleteConfirm = false }
                    )
                }
            }
        }
    }
}

@Composable
fun AddMenuItemDialog(
    hotelId: Int,
    onDismiss: () -> Unit,
    onSave: (MenuEntity, Long) -> Unit
) {
    BackHandler { onDismiss() }

    var newName by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newEmoji by remember { mutableStateOf("🍽️") }
    var newType by remember { mutableStateOf("Day") } // Day, Night, Common
    var effectiveDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    if (showDatePicker) {
        SlidingDatePickerModal(
            initialDateMillis = effectiveDateMillis,
            onDateSelected = { picked ->
                effectiveDateMillis = picked
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .imePadding()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false, onClick = {}),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add New Item", color = AppTextColor(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassTextField(
                        value = newEmoji, onValueChange = { newEmoji = it },
                        label = { Text("Emoji", color = AppTextColor().copy(alpha = 0.5f)) },
                        modifier = Modifier.weight(0.3f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    GlassTextField(
                        value = newName, onValueChange = { newName = it },
                        label = { Text("Name", color = AppTextColor().copy(alpha = 0.5f)) },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }
                
                GlassTextField(
                    value = newPrice, onValueChange = { newPrice = it },
                    label = { Text("Price (₹)", color = AppTextColor().copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Mandatory Effective Date Picker (defaults to today)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppAccentColor().copy(alpha = 0.10f))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Effective Date",
                            tint = AppAccentColor(),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "* Effective Date",
                            color = AppTextColor(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = runCatching { dateFormat.format(java.util.Date(effectiveDateMillis)) }.getOrDefault("Today"),
                        color = AppAccentColor(),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Toggle for type with Day/Night icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Day" to Icons.Default.WbSunny,
                        "Night" to Icons.Default.NightsStay,
                        "Common" to Icons.Default.Brightness4
                    ).forEach { (typeOpt, icon) ->
                        val isSelected = newType == typeOpt
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { newType = typeOpt },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AppAccentColor() else getGlassIconButtonBg(),
                            shadowElevation = if (isSelected) 3.dp else 0.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = typeOpt,
                                        tint = if (isSelected) AppOnAccentColor() else AppAccentColor(),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        typeOpt,
                                        color = if (isSelected) AppOnAccentColor() else AppTextColor(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss, modifier = Modifier.bounceClick()) {
                        Text("Cancel", color = AccentPink, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val p = newPrice.toDoubleOrNull() ?: 0.0
                            if (newName.isNotBlank() && effectiveDateMillis > 0L) {
                                onSave(
                                    MenuEntity(
                                        hotelId = hotelId,
                                        banglaName = newName,
                                        englishName = newName,
                                        price = p,
                                        isDayMenu = newType == "Day" || newType == "Common",
                                        isNightMenu = newType == "Night" || newType == "Common",
                                        isAddOn = false,
                                        emoji = newEmoji,
                                        type = newType,
                                        lastUpdatedMillis = effectiveDateMillis
                                    ),
                                    effectiveDateMillis
                                )
                            }
                        },
                        enabled = newName.isNotBlank() && effectiveDateMillis > 0L,
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor(), contentColor = AppOnAccentColor()),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp).bounceClick()
                    ) {
                        Text("Save", color = AppOnAccentColor(), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
