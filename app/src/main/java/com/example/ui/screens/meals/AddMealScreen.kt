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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val gridColumns = when {
        configuration.screenWidthDp >= 840 -> 4
        configuration.screenWidthDp >= 600 -> 3
        else -> 2
    }

    var showAllDishes by remember { mutableStateOf(false) }
    var isAddonsSectionCollapsed by remember { mutableStateOf(false) }
    var customMealText by remember { mutableStateOf("") }
    var customMealAmountStr by remember { mutableStateOf("") }
    var showCustomAmountKeyboard by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val initialCommittedIds = remember { committedGroups.map { it.id } }
    val hasUnsavedChanges = quantities.any { it.value > 0 } ||
        customMealText.isNotBlank() ||
        customMealAmountStr.isNotBlank() ||
        committedGroups.map { it.id } != initialCommittedIds

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

    // Keep primary period dishes stable at top and extra dishes below so View More never jumps to top
    val primaryMainDishes = remember(menuItems, isNight) {
        menuItems.filter { !it.isAddOn && (if (isNight) it.isNightMenu else it.isDayMenu) }
    }
    val extraMainDishes = remember(menuItems, isNight) {
        menuItems.filter { !it.isAddOn && !(if (isNight) it.isNightMenu else it.isDayMenu) }
    }
    val addOnDishes = remember(menuItems, isNight, showAllDishes) {
        menuItems.filter {
            it.isAddOn && (showAllDishes || (if (isNight) it.isNightMenu else it.isDayMenu))
        }
    }

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
                val pendingCustomAmount = customMealAmountStr.toDoubleOrNull() ?: evaluateCalculatorExpression(customMealAmountStr).toDoubleOrNull() ?: 0.0
                val hasDraftSelection = quantities.any { it.value > 0 } || pendingCustomAmount > 0.0
                val draftSubtotal = quantities.mapNotNull { (id, qty) ->
                    val item = menuItems.find { it.id == id }
                    if (item != null && qty > 0) item.price * qty else null
                }.sum() + (if (pendingCustomAmount > 0.0) pendingCustomAmount else 0.0)
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
                                        .dialogSafeTopPadding(extraTop = 4.dp)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
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
                                        text = if (isBengali) {
                                            if (editingTxId != null) "খাবার সম্পাদনা" else "খাবার যোগ করুন"
                                        } else {
                                            if (editingTxId != null) "Edit Meal" else "Add Meal"
                                        },
                                        color = AppTextColor(),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Single-Icon Day/Night Toggle Button right before Save
                                    IconButton(
                                        onClick = { viewModel.toggleDayNight() },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (isNight) Color(0xFF3F51B5).copy(alpha = 0.22f)
                                                else Color(0xFFFF9800).copy(alpha = 0.22f),
                                                CircleShape
                                            )
                                            .border(
                                                1.dp,
                                                if (isNight) Color(0xFF7986CB).copy(alpha = 0.6f)
                                                else Color(0xFFFFB74D).copy(alpha = 0.6f),
                                                CircleShape
                                            )
                                            .bounceClick()
                                            .testTag("day_night_top_toggle_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny,
                                            contentDescription = if (isNight) "Night Menu (Tap for Day)" else "Day Menu (Tap for Night)",
                                            tint = if (isNight) Color(0xFF9FA8DA) else Color(0xFFFFB300),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Glowy Save Button
                                    GlassButton(
                                        onClick = {
                                            if (pendingCustomAmount > 0.0) {
                                                viewModel.addCustomMealGroup(customMealText, pendingCustomAmount)
                                                customMealText = ""
                                                customMealAmountStr = ""
                                            }
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
                                        .dialogSafeBottomPadding(extraBottom = 4.dp)
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
                                                onClick = {
                                                    if (quantities.any { it.value > 0 }) {
                                                        viewModel.commitCurrentSelectionGroup()
                                                    }
                                                    if (pendingCustomAmount > 0.0) {
                                                        viewModel.addCustomMealGroup(customMealText, pendingCustomAmount)
                                                        customMealText = ""
                                                        customMealAmountStr = ""
                                                        showCustomAmountKeyboard = false
                                                    }
                                                },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // MAIN DISHES MULTI-COLUMN GRID SECTION
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBengali) "মূল খাবার" else "Main Dishes",
                            color = AppTextColor(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isNight) (if (isBengali) "🌙 রাতের মেনু" else "🌙 Night Menu") else (if (isBengali) "☀️ দিনের মেনু" else "☀️ Day Menu"),
                            color = AppAccentColor(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        primaryMainDishes.chunked(gridColumns).forEach { rowDishes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowDishes.forEach { dish ->
                                    DishCard(
                                        dish = dish,
                                        qty = quantities[dish.id] ?: 0,
                                        isSelected = selectedMainId == dish.id,
                                        onSelect = { viewModel.selectMainDish(dish.id) },
                                        onUpdateQty = { delta -> viewModel.updateItemQuantity(dish.id, delta) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(gridColumns - rowDishes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // Extra dishes hidden below the last card that smoothly appear in-place
                        AnimatedVisibility(
                            visible = showAllDishes && extraMainDishes.isNotEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                extraMainDishes.chunked(gridColumns).forEach { rowDishes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowDishes.forEach { dish ->
                                            DishCard(
                                                dish = dish,
                                                qty = quantities[dish.id] ?: 0,
                                                isSelected = selectedMainId == dish.id,
                                                onSelect = { viewModel.selectMainDish(dish.id) },
                                                onUpdateQty = { delta -> viewModel.updateItemQuantity(dish.id, delta) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(gridColumns - rowDishes.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (extraMainDishes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // View More Dishes toggle button
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
                                .padding(vertical = 11.dp, horizontal = 16.dp),
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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

                    // CUSTOM AMOUNT & CUSTOM TEXT SECTION AT BOTTOM OF ADD MEAL PAGE
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = AppAccentColor(),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBengali) "কাস্টম খাবার ও পরিমাণ" else "Custom Meal / Amount",
                                        color = AppTextColor(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (pendingCustomAmount > 0.0) {
                                    Surface(
                                        onClick = {
                                            viewModel.addCustomMealGroup(customMealText, pendingCustomAmount)
                                            customMealText = ""
                                            customMealAmountStr = ""
                                            showCustomAmountKeyboard = false
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = AppAccentColor().copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, AppAccentColor().copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBengali) "গ্রুপে যোগ করুন" else "Add as Group",
                                                color = AppAccentColor(),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassTextField(
                                    value = customMealText,
                                    onValueChange = { customMealText = it },
                                    label = { Text(if (isBengali) "বিবরণ (যেমন: স্পেশাল থালি)" else "Custom Text (e.g. Guest Meal)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showCustomAmountKeyboard = true }
                                ) {
                                    GlassTextField(
                                        value = customMealAmountStr,
                                        onValueChange = { newVal ->
                                            if (newVal.all { c -> c.isDigit() || c == '.' || c == '+' || c == '-' || c == '×' || c == '÷' || c == '*' || c == '/' }) {
                                                customMealAmountStr = newVal
                                            }
                                            showCustomAmountKeyboard = true
                                        },
                                        label = { Text(if (isBengali) "পরিমাণ (₹)" else "Amount (₹)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("custom_meal_amount_input")
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = showCustomAmountKeyboard,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                OnScreenCalculatorKeyboard(
                                    value = customMealAmountStr,
                                    onValueChange = { customMealAmountStr = it },
                                    onEnter = { showCustomAmountKeyboard = false }
                                )
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
