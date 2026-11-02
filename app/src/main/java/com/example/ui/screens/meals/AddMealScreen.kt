package com.example.ui.screens

import com.example.utils.Formatters
import com.example.ui.components.*
import com.example.ui.components.SlidingDatePickerModal
import com.example.ui.components.InlineDatePicker
import androidx.compose.foundation.Canvas
import kotlin.random.Random
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.viewmodel.MeterReadingViewModel
import com.example.ui.viewmodel.MeterReadingUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddMealFlowModal(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val date by viewModel.addMealDate.collectAsStateWithLifecycle()
    val isNight by viewModel.addMealIsNight.collectAsStateWithLifecycle()
    val quantities by viewModel.orderQuantities.collectAsStateWithLifecycle()
    val selectedMainId by viewModel.selectedMainDishId.collectAsStateWithLifecycle()
    val menuItems by viewModel.allMenuItems.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val committedGroups by viewModel.committedMealGroups.collectAsStateWithLifecycle()
    val editingTxId by viewModel.editingTransactionId.collectAsStateWithLifecycle()

    val themeMode = userProfile?.themeMode ?: "Auto"
    val bgPreset = userProfile?.bgPreset ?: "Midnight Glow"
    val customBgUri = userProfile?.customBgUri ?: ""
    val isLiquidGlass = userProfile?.enableLiquidGlassTheme ?: false
    val isBengali = userProfile?.language == "bn"

    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    var showAllDishes by remember { mutableStateOf(false) }
    var isAddonsSectionCollapsed by remember { mutableStateOf(false) }

    // Filter menu dynamically using remember to avoid redundant work
    val filteredDishes = remember(menuItems, isNight, showAllDishes) {
        menuItems.filter {
            if (showAllDishes) {
                true
            } else {
                if (isNight) it.isNightMenu else it.isDayMenu
            }
        }
    }
    val mainDishes = remember(filteredDishes) { filteredDishes.filter { !it.isAddOn } }
    val addOnDishes = remember(filteredDishes) { filteredDishes.filter { it.isAddOn } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()
        Box(modifier = Modifier.fillMaxSize()) {
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
                        .background(getAppBackgroundGradient(themeMode, bgPreset))
                        .pointerInput(Unit) { detectTapGestures { } },
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

                    // BOTTOM ACTION & TOTAL SUMMARY ROW (LOCKED AT BOTTOM)
                val hasDraftSelection = quantities.any { it.value > 0 }
                val draftSubtotal = quantities.mapNotNull { (id, qty) ->
                    val item = menuItems.find { it.id == id }
                    if (item != null && qty > 0) item.price * qty else null
                }.sum()
                val committedSubtotal = committedGroups.sumOf { it.subtotal }
                val overallTotal = committedSubtotal + draftSubtotal
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0.dp),
                        topBar = {
                            Surface(
                                color = if (isAppLightMode()) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
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
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = if (isBengali) "ফিরে যান" else "Back",
                                            tint = AppTextColor()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isBengali) {
                                            if (editingTxId != null) "খাবার রেকর্ড সম্পাদনা" else "খাবার রেকর্ড যোগ করুন"
                                        } else {
                                            if (editingTxId != null) "Edit Meal Record" else "Add Meal Record"
                                        },
                                        color = AppTextColor(),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Glowy Save Button
                                    GlassButton(
                                        onClick = {
                                            viewModel.saveHotelTransaction()
                                            onDismiss()
                                        },
                                        cornerRadius = 10.dp,
                                        borderBrush = Brush.linearGradient(listOf(AccentCyan, AccentPink)),
                                        modifier = Modifier.testTag("save_meal_button")
                                    ) {
                                        Text(
                                            text = if (isBengali) "সংরক্ষণ" else "Save",
                                            color = AppTextColor(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            Surface(
                                color = if (isAppLightMode()) com.example.ui.theme.M3SurfaceLight else com.example.ui.theme.M3SurfaceDark,
                                shadowElevation = 8.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(10.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // COMMITTED MEAL GROUPS CARDS DISPLAY (IN BOTTOM BAR)
                                        if ((userProfile?.mealGroupPosition ?: "BottomBar") != "PageBottom" && committedGroups.isNotEmpty()) {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                contentPadding = PaddingValues(bottom = 8.dp)
                                            ) {
                                                itemsIndexed(committedGroups, key = { _, group -> group.id }, contentType = { _, _ -> "meal_group_compact" }) { index, group ->
                                                    MealGroupCard(
                                                        group = group,
                                                        index = index,
                                                        onRemove = { viewModel.removeMealGroup(group.id) },
                                                        isCompact = true
                                                    )
                                                }
                                            }
                                        }
                                
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { viewModel.commitCurrentSelectionGroup() },
                                                enabled = hasDraftSelection,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AccentCyan,
                                                    disabledContainerColor = AccentCyan.copy(alpha = 0.2f)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                                modifier = Modifier.bounceClick()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = if (hasDraftSelection) Color.Black else AppTextColor().copy(alpha = 0.4f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isBengali) "গ্রুপ যোগ করুন" else "Add Group",
                                                    color = if (hasDraftSelection) Color.Black else AppTextColor().copy(alpha = 0.4f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = if (isBengali) "মোট হিসাব" else "TOTAL LEDGER",
                                                    color = AppTextColor().copy(alpha = 0.6f),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "₹${String.format(Locale.US, "%,.2f", overallTotal)}",
                                                    color = AccentGreen,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                    // TOP SECTION: DATE CONTROLS
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            InlineDatePicker(
                                initialDateMillis = date,
                                onDateSelected = { newDate ->
                                    viewModel.setAddMealDate(newDate)
                                },
                                textColor = AppTextColor()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // DAY/NIGHT ICON-BASED TOGGLE SWITCH
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Menu Period Selection",
                            color = AppTextColor(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                        )

                        val isAppLight = isAppLightMode()
                        Row(
                            modifier = Modifier
                                .background(
                                    if (isAppLight) Color(0x1A000000) else Color(0x1F000000),
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isAppLight) Color(0x33000000) else getGlassBorder(),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Day Icon Button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(
                                        if (!isNight) Brush.horizontalGradient(listOf(Color(0xFFFF9800), Color(0xFFFF5722)))
                                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable { if (isNight) viewModel.toggleDayNight() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = "Day",
                                    tint = if (!isNight) Color.White else AppTextColor().copy(alpha = 0.5f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Day",
                                    color = if (!isNight) Color.White else AppTextColor().copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Night Icon Button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(
                                        if (isNight) Brush.horizontalGradient(listOf(Color(0xFF3F51B5), Color(0xFF673AB7)))
                                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable { if (!isNight) viewModel.toggleDayNight() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = "Night",
                                    tint = if (isNight) Color.White else AppTextColor().copy(alpha = 0.5f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Night",
                                    color = if (isNight) Color.White else AppTextColor().copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MAIN DISHES LIST SECTION
                    Text(
                        text = "Main Dishes (Select One to View Add-ons)",
                        color = AppTextColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        mainDishes.forEach { dish ->
                            DishCard(
                                dish = dish,
                                qty = quantities[dish.id] ?: 0,
                                isSelected = selectedMainId == dish.id,
                                onSelect = { viewModel.selectMainDish(dish.id) },
                                onUpdateQty = { delta -> viewModel.updateItemQuantity(dish.id, delta) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // View More Dishes menu item / toggle button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(AccentCyan.copy(alpha = 0.15f), AccentPink.copy(alpha = 0.15f))
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .clickable { showAllDishes = !showAllDishes }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (showAllDishes) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showAllDishes) "View Less Dishes" else "View More Dishes (Show ${if (isNight) "Day" else "Night"} Dishes)",
                                color = AppTextColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // DYNAMICALLY RENDERED ADD-ON DISHES TRAY BELOW
                    val shouldShowAddons = userProfile.enableSupplementaryAddons && selectedMainId != null && addOnDishes.isNotEmpty()
                    AnimatedVisibility(
                        visible = shouldShowAddons,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val activeMainName = menuItems.find { it.id == selectedMainId }?.let {
                                if (userProfile.language == "bn" && it.banglaName.isNotBlank()) it.banglaName else it.englishName
                            } ?: ""

                            // Section Header with Hide/Show Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (userProfile.language == "bn") "$activeMainName এর সাপ্লিমেন্টারি অ্যাড-অন" else "Supplementary Add-ons for $activeMainName",
                                        color = AccentCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Interactive Hide / Show Button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAddonsSectionCollapsed) AccentCyan.copy(alpha = 0.15f) else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isAddonsSectionCollapsed) AccentCyan.copy(alpha = 0.4f) else getGlassBorder()),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { isAddonsSectionCollapsed = !isAddonsSectionCollapsed }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isAddonsSectionCollapsed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isAddonsSectionCollapsed) "Show" else "Hide",
                                            tint = if (isAddonsSectionCollapsed) AccentCyan else AppSecondaryTextColor(),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isAddonsSectionCollapsed) (if (userProfile.language == "bn") "দেখান" else "Show") else (if (userProfile.language == "bn") "লুকান" else "Hide"),
                                            color = if (isAddonsSectionCollapsed) AccentCyan else AppSecondaryTextColor(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (!isAddonsSectionCollapsed) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 16.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        addOnDishes.forEach { addOn ->
                                            val addOnQty = quantities[addOn.id] ?: 0

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                    Text(
                                                        text = addOn.banglaName.ifBlank { addOn.englishName },
                                                        color = AppTextColor(),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${addOn.englishName} • ₹${addOn.price.toInt()}",
                                                        color = AppSecondaryTextColor(),
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (addOnQty > 0) {
                                                        IconButton(
                                                            onClick = { viewModel.updateItemQuantity(addOn.id, -1) },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .background(getGlassIconButtonBg(), CircleShape)
                                                        ) {
                                                            Icon(Icons.Default.Remove, contentDescription = "-", tint = AppTextColor(), modifier = Modifier.size(14.dp))
                                                        }
                                                        Text(
                                                            text = "$addOnQty",
                                                            color = AppTextColor(),
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.updateItemQuantity(addOn.id, 1) },
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .background(if (addOnQty > 0) AccentCyan else getGlassIconButtonBg(), CircleShape)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = "+",
                                                            tint = if (addOnQty > 0) Color(0xFF0F0B26) else AppTextColor(),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            if (addOn != addOnDishes.last()) {
                                                HorizontalDivider(color = getGlassBorder().copy(alpha = 0.3f), thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // COMMITTED MEAL GROUPS CARDS DISPLAY (IF SET TO PAGE BOTTOM)
                    if (userProfile.mealGroupPosition == "PageBottom" && committedGroups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            itemsIndexed(committedGroups, key = { _, group -> group.id }, contentType = { _, _ -> "meal_group" }) { index, group ->
                                MealGroupCard(
                                    group = group,
                                    index = index,
                                    onRemove = { viewModel.removeMealGroup(group.id) },
                                    isCompact = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
