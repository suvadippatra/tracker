package com.example.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.components.AppTextColor
import com.example.ui.components.AccentCyan
import com.example.ui.components.AccentPink
import com.example.ui.components.GlassTextField
import com.example.ui.components.bounceClick
import com.example.ui.components.getDialogBackground
import com.example.ui.components.getGlassIconButtonBg
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
    var editLocation by remember(hotel) { mutableStateOf(hotel?.location ?: "") }
    var editUpiId by remember(hotel) { mutableStateOf(hotel?.upiId ?: "") }
    var editQrCodeUris by remember(hotel) { mutableStateOf(hotel?.qrCodeUri?.split(",")?.filter { it.isNotBlank() } ?: emptyList()) }

    val qrCodeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            editQrCodeUris = editQrCodeUris + uris.map { it.toString() }
        }
    }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBengali = userProfile?.language == "bn"

    androidx.activity.compose.BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.components.getAppBackgroundGradient(com.example.ui.components.LocalAppTheme.current))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
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
                                contentDescription = if (isBengali) "ফিরে যান" else "Back",
                                tint = AppTextColor()
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isBengali) (if (hotel == null) "নতুন হোটেল" else "হোটেল সংশোধন") else (if (hotel == null) "Add Hotel" else "Edit Hotel Info"),
                            color = AppTextColor(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Save Button in TopBar
                        Button(
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
                                            qrCodeUri = editQrCodeUris.joinToString(",")
                                        )
                                    } else {
                                        viewModel.editHotel(hotel.copy(
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
                            enabled = editHotelName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCyan,
                                disabledContainerColor = AccentCyan.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.height(38.dp).bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBengali) "সংরক্ষণ" else "Save",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (hotel != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(38.dp).background(getGlassIconButtonBg(), CircleShape).bounceClick()
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
                                    modifier = Modifier.background(getDialogBackground())
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (isBengali) "ট্র্যাশ বিনে সরান" else "Move to Trash Bin", color = AccentPink) },
                                        onClick = {
                                            showMenu = false
                                            viewModel.deleteHotel(hotel)
                                            onDismiss()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = AccentPink,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp) // Optimized for tablet and large screens
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    
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
                        label = { Text(if (isBengali) "মোবাইল নম্বর" else "Mobile Number", color = AppTextColor().copy(alpha = 0.6f)) },
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
                        IconButton(onClick = {
                            val intent = if (editLocation.startsWith("http://") || editLocation.startsWith("https://")) {
                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(editLocation))
                            } else {
                                val query = if (editLocation.isNotBlank()) android.net.Uri.encode(editLocation) else "Restaurants"
                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$query"))
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .size(56.dp)
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
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
                                    .height(80.dp)
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
                                .height(80.dp)
                                .width(80.dp)
                                .clickable { qrCodeLauncher.launch("image/*") }
                                .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add QR", tint = AccentCyan, modifier = Modifier.size(32.dp))
                        }
                    }

                    if (hotel != null) {
                        Spacer(modifier = Modifier.height(32.dp))
                        HorizontalDivider(color = AppTextColor().copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (isBengali) "মেনু পরিচালনা" else "Menu Management",
                            color = AppTextColor(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isBengali) "এই হোটেলের সমস্ত খাবার আইটেম, বিভাগ এবং দাম পরিচালনা করুন।" else "Manage and edit all food items, categories, and prices for this hotel.",
                            color = AppTextColor().copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onManageMenu(hotel) },
                            modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                        ) {
                            Icon(Icons.Default.RestaurantMenu, contentDescription = "Manage Menu", tint = AccentCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isBengali) "মেনু এডিটর খুলুন" else "Open Menu Editor",
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
