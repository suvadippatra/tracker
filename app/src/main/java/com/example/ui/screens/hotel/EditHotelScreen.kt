package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.components.*
import com.example.ui.viewmodel.FinancialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHotelModal(
    hotel: HotelEntity?,
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit,
    onManageMenu: (HotelEntity) -> Unit
) {
    val context = LocalContext.current
    var editHotelName by remember(hotel) { mutableStateOf(hotel?.name ?: "") }
    var editHotelAdvanced by remember(hotel) { mutableStateOf(hotel?.advancedPayment?.toString() ?: "") }
    var editPhone by remember(hotel) { mutableStateOf(hotel?.phoneNumber ?: "") }
    var editWhatsapp by remember(hotel) { mutableStateOf(hotel?.whatsappNumber ?: "") }
    var editLocation by remember(hotel) { mutableStateOf(hotel?.location ?: "") }
    var editUpiId by remember(hotel) { mutableStateOf(hotel?.upiId ?: "") }
    var editNotes by remember(hotel) { mutableStateOf(hotel?.notes ?: "") }
    var editQrCodeUris by remember(hotel) { mutableStateOf(hotel?.qrCodeUri?.split(",")?.filter { it.isNotBlank() } ?: emptyList()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val qrCodeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            editQrCodeUris = editQrCodeUris + uris.mapNotNull { com.example.ui.components.copyUriToPersistentFile(context, it, "QR") }
        }
    }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBengali = userProfile?.language == "bn"

    val hasUnsavedChanges = if (hotel == null) {
        editHotelName.isNotBlank() || editHotelAdvanced.isNotBlank() || editPhone.isNotBlank() ||
            editWhatsapp.isNotBlank() || editLocation.isNotBlank() || editUpiId.isNotBlank() ||
            editNotes.isNotBlank() || editQrCodeUris.isNotEmpty()
    } else {
        editHotelName != hotel.name ||
            editHotelAdvanced != hotel.advancedPayment.toString() ||
            editPhone != hotel.phoneNumber ||
            editWhatsapp != hotel.whatsappNumber ||
            editLocation != hotel.location ||
            editUpiId != hotel.upiId ||
            editNotes != hotel.notes ||
            editQrCodeUris.joinToString(",") != hotel.qrCodeUri
    }

    val handleBack = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onDismiss()
        }
    }
    BackHandler { handleBack() }

    if (showDiscardDialog) {
        UnsavedChangesWarningDialog(
            isBengali = isBengali,
            onKeepEditing = { showDiscardDialog = false },
            onDiscard = {
                showDiscardDialog = false
                onDismiss()
            }
        )
    }

    val themeMode = userProfile?.themeMode ?: "Auto"
    val bgPreset = userProfile?.bgPreset ?: "Midnight Glow"
    val customBgUri = userProfile?.customBgUri ?: ""
    val isLiquidGlass = userProfile?.enableLiquidGlassTheme ?: false

    Dialog(
        onDismissRequest = { handleBack() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()
        Box(modifier = Modifier.fillMaxSize()) {
        
        androidx.compose.runtime.CompositionLocalProvider(
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
                        .background(com.example.ui.components.getAppBackgroundGradient(themeMode, bgPreset)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (customBgUri.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = customBgUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isAppLightMode()) Color.White.copy(alpha = 0.40f)
                                    else Color.Black.copy(alpha = 0.50f)
                                )
                        )
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0.dp),
                        topBar = {
                            Surface(
                                color = if (!isLiquidGlass) {
                                    if (isAppLightMode()) com.example.ui.theme.M3SurfaceLight.copy(alpha = 0.95f) else com.example.ui.theme.M3SurfaceDark.copy(alpha = 0.95f)
                                } else {
                                    if (isAppLightMode()) Color.White.copy(alpha = 0.85f) else Color(0xFF121620).copy(alpha = 0.85f)
                                },
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .dialogSafeTopPadding(extraTop = 8.dp)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { handleBack() },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(getGlassIconButtonBg(), CircleShape)
                                            .bounceClick()
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronLeft,
                                            contentDescription = if (isBengali) "ফিরে যান" else "Back",
                                            tint = AppTextColor(),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isBengali) (if (hotel == null) "নতুন হোটেল" else "হোটেল সংশোধন") else (if (hotel == null) "Add Hotel" else "Edit Hotel Info"),
                                        color = AppTextColor(),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    
                                    // Save Button in TopBar
                                    FilledIconButton(
                                        onClick = {
                                            val adv = editHotelAdvanced.toDoubleOrNull() ?: 0.0
                                            if (editHotelName.isNotBlank()) {
                                                if (hotel == null) {
                                                    viewModel.addHotel(
                                                        name = editHotelName,
                                                        advancedPayment = adv,
                                                        phoneNumber = editPhone,
                                                        location = editLocation,
                                                        upiId = editUpiId,
                                                        qrCodeUri = editQrCodeUris.joinToString(","),
                                                        notes = editNotes,
                                                        whatsappNumber = editWhatsapp
                                                    )
                                                } else {
                                                    viewModel.editHotel(hotel.copy(
                                                        name = editHotelName,
                                                        advancedPayment = adv,
                                                        phoneNumber = editPhone,
                                                        location = editLocation,
                                                        upiId = editUpiId,
                                                        qrCodeUri = editQrCodeUris.joinToString(","),
                                                        notes = editNotes,
                                                        whatsappNumber = editWhatsapp
                                                    ))
                                                }
                                                onDismiss()
                                            }
                                        },
                                        enabled = editHotelName.isNotBlank(),
                                        modifier = Modifier.size(40.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = AppAccentColor(),
                                            contentColor = AppOnAccentColor()
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Save",
                                            tint = if (editHotelName.isNotBlank()) AppOnAccentColor() else AppSecondaryTextColor()
                                        )
                                    }

                            if (hotel != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                var showMenu by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More Options",
                                            tint = AppTextColor()
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        offset = androidx.compose.ui.unit.DpOffset(x = (-12).dp, y = 4.dp),
                                        modifier = Modifier.widthIn(min = 190.dp).background(getDialogBackground())
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(if (isBengali) "ট্র্যাশ বিনে সরান" else "Move to Trash Bin", color = AccentPink, fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                showMenu = false
                                                showDeleteConfirm = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 680.dp)
                                .verticalScroll(rememberScrollState())
                                .dialogSafeBottomPadding(extraBottom = 16.dp)
                                .imePadding()
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        ) {
                            if (showDeleteConfirm) {
                                com.example.ui.components.Tier1DeleteDialog(
                                    itemTitle = hotel?.name ?: "Hotel",
                                    onConfirm = {
                                        hotel?.let { viewModel.deleteHotel(it) }
                                        showDeleteConfirm = false
                                        onDismiss()
                                    },
                                    onDismiss = { showDeleteConfirm = false }
                                )
                            }
                            GlassTextField(
                                value = editHotelName,
                                onValueChange = { editHotelName = it },
                                label = { Text(if (isBengali) "হোটেলের নাম" else "Hotel Name", color = AppTextColor().copy(alpha = 0.6f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassTextField(
                                value = editHotelAdvanced,
                                onValueChange = { editHotelAdvanced = it },
                                label = { Text(if (isBengali) "অগ্রিম টাকা (₹)" else "Advanced Payment (₹)", color = AppTextColor().copy(alpha = 0.6f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text(if (isBengali) "মোবাইল নম্বর (কল করার জন্য)" else "Mobile Number (Phone Call)", color = AppTextColor().copy(alpha = 0.6f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassTextField(
                                value = editWhatsapp,
                                onValueChange = { editWhatsapp = it },
                                label = { Text(if (isBengali) "হোয়াটসঅ্যাপ নম্বর" else "WhatsApp Number (if different or for chat)", color = AppTextColor().copy(alpha = 0.6f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassTextField(
                                    value = editLocation,
                                    onValueChange = { editLocation = it },
                                    label = { Text(if (isBengali) "ম্যাপ লিঙ্ক / ঠিকানা" else "Maps Link / Location", color = AppTextColor().copy(alpha = 0.6f)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = {
                                        val intent = if (editLocation.startsWith("http://") || editLocation.startsWith("https://")) {
                                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(editLocation))
                                        } else {
                                            val query = if (editLocation.isNotBlank()) android.net.Uri.encode(editLocation) else "Restaurants"
                                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$query"))
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(AccentCyan.copy(alpha=0.15f), RoundedCornerShape(12.dp))
                                        .bounceClick()
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = "Open Maps", tint = AccentCyan)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassTextField(
                                value = editUpiId,
                                onValueChange = { editUpiId = it },
                                label = { Text(if (isBengali) "ইউপিআই আইডি (UPI ID)" else "UPI ID", color = AppTextColor().copy(alpha = 0.6f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassTextField(
                                value = editNotes,
                                onValueChange = { editNotes = it },
                                label = { Text(if (isBengali) "সংক্ষিপ্ত নোট / মন্তব্য" else "Short Notes / Remarks", color = AppTextColor().copy(alpha = 0.6f)) },
                                singleLine = false,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = if (isBengali) "কিউআর কোড (${editQrCodeUris.size})" else "QR Codes (${editQrCodeUris.size})",
                                color = AppTextColor(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                editQrCodeUris.forEachIndexed { index, uri ->
                                    Box(
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 72.dp)
                                            .background(AppTextColor().copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(32.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("QR ${index + 1}", color = AppTextColor(), fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            IconButton(onClick = { editQrCodeUris = editQrCodeUris.toMutableList().apply { removeAt(index) } }, modifier = Modifier.size(24.dp).bounceClick()) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = AccentPink)
                                            }
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clickable { qrCodeLauncher.launch("image/*") }
                                        .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add QR", tint = AccentCyan, modifier = Modifier.size(32.dp))
                                }
                            }

                            if (hotel != null) {
                                val allMenus by viewModel.allHotelMenus.collectAsStateWithLifecycle()
                                val hotelMenus = remember(allMenus, hotel.id) {
                                    allMenus.filter { it.hotelId == hotel.id && !it.isDeleted }
                                }
                                var showInlineAddMenu by remember { mutableStateOf(false) }

                                if (showInlineAddMenu) {
                                    AddMenuItemDialog(
                                        hotelId = hotel.id,
                                        onDismiss = { showInlineAddMenu = false },
                                        onSave = { newItem ->
                                            viewModel.addMenu(newItem)
                                            showInlineAddMenu = false
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))
                                HorizontalDivider(color = AppTextColor().copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBengali) "মেনু পরিচালনা (${hotelMenus.size})" else "Menu Items (${hotelMenus.size})",
                                            color = AppTextColor(),
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isBengali) "এখান থেকেই সরাসরি খাবার ও নতুন দামের তারিখ এডিট করুন।" else "Edit dishes & price effective dates directly alongside hotel details.",
                                            color = AppTextColor().copy(alpha = 0.6f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { showInlineAddMenu = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = AppAccentColor().copy(alpha = 0.16f),
                                            contentColor = AppAccentColor()
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isBengali) "আইটেম যোগ" else "Add Dish", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (hotelMenus.isNotEmpty()) {
                                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            hotelMenus.forEachIndexed { idx, item ->
                                                MenuTableRow(
                                                    item = item,
                                                    onSave = { updatedItem, effectiveDate ->
                                                        viewModel.updateMenu(updatedItem, effectiveDate)
                                                    },
                                                    onViewHistory = {
                                                        val adv = editHotelAdvanced.toDoubleOrNull() ?: 0.0
                                                        val updatedHotel = hotel.copy(
                                                            name = editHotelName.ifBlank { hotel.name },
                                                            advancedPayment = adv,
                                                            phoneNumber = editPhone,
                                                            location = editLocation,
                                                            upiId = editUpiId,
                                                            qrCodeUri = editQrCodeUris.joinToString(","),
                                                            notes = editNotes,
                                                            whatsappNumber = editWhatsapp
                                                        )
                                                        viewModel.editHotel(updatedHotel)
                                                        onManageMenu(updatedHotel)
                                                    },
                                                    onDelete = { viewModel.deleteMenu(item) }
                                                )
                                                if (idx < hotelMenus.lastIndex) {
                                                    HorizontalDivider(color = AppTextColor().copy(alpha = 0.06f))
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                Button(
                                    onClick = {
                                        val adv = editHotelAdvanced.toDoubleOrNull() ?: 0.0
                                        val updatedHotel = hotel.copy(
                                            name = editHotelName.ifBlank { hotel.name },
                                            advancedPayment = adv,
                                            phoneNumber = editPhone,
                                            location = editLocation,
                                            upiId = editUpiId,
                                            qrCodeUri = editQrCodeUris.joinToString(","),
                                            notes = editNotes,
                                            whatsappNumber = editWhatsapp
                                        )
                                        viewModel.editHotel(updatedHotel)
                                        onManageMenu(updatedHotel)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 52.dp)
                                        .bounceClick(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppAccentColor(),
                                        contentColor = AppOnAccentColor()
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
                                ) {
                                    Icon(Icons.Default.RestaurantMenu, contentDescription = "Manage Menu", tint = AppOnAccentColor())
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isBengali) "সম্পূর্ণ মেনু এডিটর ও ইতিহাস খুলুন" else "Open Full Menu Editor & Price History",
                                        color = AppOnAccentColor(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }
                }
            }
        }
    }
}
}
}
