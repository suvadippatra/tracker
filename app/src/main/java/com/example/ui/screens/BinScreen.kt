package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
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

    var activeSection by remember { mutableStateOf("Hotels") }

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
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(getGlassBorder().copy(alpha = 0.1f), CircleShape).bounceClick()
                        ) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = AppTextColor())
                        }
                        Text("Recycle Bin", color = AppTextColor(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Box(modifier = Modifier.size(48.dp))
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    // Section Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Hotels", "Accounts", "Menu").forEach { section ->
                            val isSelected = activeSection == section
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { activeSection = section },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(section, color = if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        when (activeSection) {
                            "Hotels" -> {
                                if (deletedHotels.isEmpty()) {
                                    item { EmptyBinState("No deleted hotels") }
                                } else {
                                    items(deletedHotels) { hotel ->
                                        BinItemCard(
                                            title = hotel.name,
                                            subtitle = hotel.location,
                                            onRestore = { viewModel.restoreHotel(hotel) },
                                            onDeletePermanently = { viewModel.deleteHotelPermanently(hotel) }
                                        )
                                    }
                                }
                            }
                            "Accounts" -> {
                                if (deletedBankAccounts.isEmpty()) {
                                    item { EmptyBinState("No deleted accounts") }
                                } else {
                                    items(deletedBankAccounts) { acc ->
                                        BinItemCard(
                                            title = acc.name,
                                            subtitle = "Balance: ₹${acc.balance}",
                                            onRestore = { viewModel.restoreVirtualAccount(acc) },
                                            onDeletePermanently = { viewModel.deleteVirtualAccountPermanently(acc) }
                                        )
                                    }
                                }
                            }
                            "Menu" -> {
                                if (deletedMenuItems.isEmpty()) {
                                    item { EmptyBinState("No deleted menu items") }
                                } else {
                                    items(deletedMenuItems) { menu ->
                                        BinItemCard(
                                            title = menu.englishName,
                                            subtitle = "₹${menu.price}",
                                            onRestore = { viewModel.restoreMenu(menu) },
                                            onDeletePermanently = { viewModel.deleteMenuPermanently(menu) }
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
}

@Composable
fun BinItemCard(
    title: String,
    subtitle: String,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = AppTextColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = AppTextColor().copy(alpha = 0.5f), fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onRestore, modifier = Modifier.background(AccentGreen.copy(alpha = 0.1f), CircleShape)) {
                    Icon(Icons.Default.Restore, "Restore", tint = AccentGreen, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDeletePermanently, modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)) {
                    Icon(Icons.Default.DeleteForever, "Delete Permanently", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyBinState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DeleteOutline, null, tint = AppTextColor().copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = AppTextColor().copy(alpha = 0.3f), fontSize = 14.sp)
        }
    }
}
