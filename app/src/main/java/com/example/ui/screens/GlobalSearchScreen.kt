@file:Suppress("DEPRECATION")
package com.example.ui.screens

import androidx.activity.compose.BackHandler
import com.example.ui.components.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SearchResult
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun GlobalSearchScreen(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedSearchFilter.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()

    val hotels by viewModel.allHotels.collectAsStateWithLifecycle()
    val expenses by viewModel.allExtraExpenses.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val menuItems by viewModel.allMenuItems.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBengali = userProfile?.language == "bn"

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val categories = remember {
        listOf("All", "Sticky", "Expenses", "Meals & Menus", "Hotels", "Accounts", "Bills")
    }

    val quickSuggestions = remember(hotels, expenses, bankAccounts, menuItems, isBengali) {
        val list = mutableListOf<Pair<String, String>>()
        
        // 1. Add actual active hotels
        hotels.filter { !it.isDeleted }.forEach { hotel ->
            if (hotel.name.isNotBlank()) {
                list.add(hotel.name to "🏨")
            }
        }
        
        // 2. Add actual active bank accounts
        bankAccounts.filter { !it.isDeleted }.forEach { account ->
            if (account.name.isNotBlank()) {
                list.add(account.name to "💳")
            }
        }
        
        // 3. Add actual active menu items configured by the user
        menuItems.filter { !it.isDeleted }.forEach { menu ->
            val name = if (isBengali) menu.banglaName else menu.englishName
            if (name.isNotBlank()) {
                list.add(name to (if (menu.emoji.isNotBlank()) menu.emoji else "🍽️"))
            }
        }

        // 4. Add actual active extra expenses configured by the user
        expenses.filter { !it.isDeleted }.forEach { expense ->
            if (expense.name.isNotBlank()) {
                list.add(expense.name to "🛒")
            }
        }
        
        // Deduplicate by lowercased trimmed name and take top 10 items
        list.distinctBy { it.first.lowercase().trim() }.take(10)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getAppBackgroundGradient(LocalAppTheme.current))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 840.dp)
                .imePadding()
        ) {
            // Search Input Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .dialogSafeTopPadding(extraTop = 4.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.setSearchQuery("")
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(getGlassIconButtonBg(), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Back",
                        tint = AppTextColor(),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(getDialogBackground().copy(alpha = 0.85f))
                        .border(1.dp, getGlassBorder(), RoundedCornerShape(26.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = AppTextColor(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(AccentCyan),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    viewModel.addSearchHistory(searchQuery)
                                }
                                keyboardController?.hide()
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search expenses, meals, hotels, bills...",
                                        color = AppTextColor().copy(alpha = 0.45f),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setSearchQuery("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = AppTextColor().copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it }) { category ->
                    val isSelected = selectedFilter == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) AccentCyan.copy(alpha = 0.25f)
                                else getDialogBackground().copy(alpha = 0.5f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) AccentCyan else getGlassBorder(),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.setSearchFilter(category) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.8f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Main Content Area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (searchQuery.isEmpty()) {
                    // Recent Search History
                    if (searchHistory.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Recent Searches",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AppTextColor().copy(alpha = 0.7f)
                                    )
                                    TextTextButton(
                                        onClick = { viewModel.clearSearchHistory() },
                                        text = "Clear All"
                                    )
                                }
                                FlowRowLayout(
                                    items = searchHistory,
                                    onItemClick = { term -> viewModel.setSearchQuery(term) },
                                    onRemoveClick = { term -> viewModel.removeSearchHistoryItem(term) }
                                )
                            }
                        }
                             // Quick Suggestions Shortcuts
                    item {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = if (isBengali) "আপনার ব্যবহার অনুযায়ী খুঁজুন" else "Suggested Searches",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AppTextColor().copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (quickSuggestions.isEmpty()) {
                                Text(
                                    text = if (isBengali) "কোনো সাম্প্রতিক ব্যবহার নেই। আপনার তৈরি হোটেল, ব্যাংক অ্যাকাউন্ট বা খরচ এখানে শর্টকাট হিসেবে প্রদর্শিত হবে।" else "No recent usage found. Your configured hotels, bank accounts, and meals will appear here as search shortcuts.",
                                    color = AppTextColor().copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(quickSuggestions, key = { it.first }) { (title, emoji) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(getDialogBackground().copy(alpha = 0.6f))
                                                .border(0.5.dp, getGlassBorder(), RoundedCornerShape(16.dp))
                                                .clickable { viewModel.setSearchQuery(title) }
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(emoji, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    title,
                                                    color = AppTextColor(),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }             }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.TravelExplore,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = AccentCyan.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Search dishes, meal logs, expenses, hotels, accounts & bills",
                                    color = AppTextColor().copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (results.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = AppTextColor().copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No matches found for \"$searchQuery\"",
                                color = AppTextColor().copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Try searching for a different dish name, expense category, or amount",
                                color = AppTextColor().copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Result Header Summary
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Found ${results.size} result${if (results.size > 1) "s" else ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                            if (selectedFilter != "All") {
                                Text(
                                    "Filtered by: $selectedFilter",
                                    fontSize = 11.sp,
                                    color = AppTextColor().copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    items(results, key = { "${it.type}_${it.id}_${it.title}" }) { result ->
                        SearchItemCard(
                            result = result,
                            query = searchQuery,
                            onClick = {
                                viewModel.addSearchHistory(searchQuery)
                                when (result.type) {
                                    "Hotel" -> {
                                        viewModel.activeHotelId.value = result.hotelId ?: result.id
                                        viewModel.activeTab.value = "Hotels"
                                    }
                                    "Expense", "Income" -> {
                                        viewModel.activeTab.value = "Extras"
                                    }
                                    "Meal", "Menu" -> {
                                        result.hotelId?.let { viewModel.activeHotelId.value = it }
                                        viewModel.activeTab.value = "Hotels"
                                    }
                                    "Account" -> {
                                        viewModel.activeTab.value = "Extras"
                                    }
                                    "Meter" -> {
                                        viewModel.activeTab.value = "Bill"
                                    }
                                    "Sticky" -> {
                                        viewModel.activeTab.value = "Sticky"
                                    }
                                }
                                onDismiss()
                            }
                        )
                    }
                }
            }

            // Quick Action Footer Bar inside Search Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getDialogBackground().copy(alpha = 0.9f))
                    .border(0.5.dp, getGlassBorder(), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.triggerQuickAction("ADD_EXPENSE")
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Expense", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.triggerQuickAction("ADD_MEAL_LOG")
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isAppLightMode()) com.example.ui.theme.M3AccentEmerald else Color(0xFF00E676)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Meal Log", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchItemCard(
    result: SearchResult,
    query: String,
    onClick: () -> Unit
) {
    val (icon, badgeColor) = when (result.type) {
        "Hotel" -> Icons.Default.Hotel to Color(0xFF00E5FF)
        "Expense" -> Icons.Default.ReceiptLong to Color(0xFFFF4081)
        "Income" -> Icons.Default.TrendingUp to Color(0xFF00E676)
        "Meal" -> Icons.Default.Restaurant to Color(0xFFFF9100)
        "Menu" -> Icons.Default.Fastfood to Color(0xFFFFD700)
        "Account" -> Icons.Default.AccountBalance to Color(0xFF26A69A)
        "Meter" -> Icons.Default.ElectricBolt to Color(0xFFB388FF)
        "Sticky" -> Icons.Default.StickyNote2 to Color(0xFFFFB300)
        else -> Icons.Default.Info to Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(getDialogBackground().copy(alpha = 0.6f))
            .border(0.5.dp, getGlassBorder(), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = result.type.uppercase(),
                            color = badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    if (result.dateOrExtra.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result.dateOrExtra,
                            color = AppTextColor().copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                HighlightedText(
                    text = result.title,
                    query = query,
                    color = AppTextColor(),
                    highlightColor = AccentCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = result.subtitle,
                    color = AppTextColor().copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (result.amount != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "₹${result.amount}",
                    color = if (result.type == "Income") Color(0xFF00E676) else AppTextColor(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun HighlightedText(
    text: String,
    query: String,
    color: Color,
    highlightColor: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight
) {
    if (query.isBlank() || !text.contains(query, ignoreCase = true)) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    val annotated = remember(text, query) {
        buildAnnotatedString {
            var start = 0
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()
            while (start < text.length) {
                val index = lowerText.indexOf(lowerQuery, start)
                if (index == -1) {
                    append(text.substring(start))
                    break
                }
                if (index > start) {
                    append(text.substring(start, index))
                }
                val matchEnd = index + query.length
                pushStyle(
                    SpanStyle(
                        color = highlightColor,
                        fontWeight = FontWeight.Black,
                        background = highlightColor.copy(alpha = 0.2f)
                    )
                )
                append(text.substring(index, matchEnd))
                pop()
                start = matchEnd
            }
        }
    }
    Text(
        text = annotated,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun FlowRowLayout(
    items: List<String>,
    onItemClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit
) {
    @OptIn(ExperimentalLayoutApi::class)
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(getDialogBackground().copy(alpha = 0.6f))
                    .border(0.5.dp, getGlassBorder(), RoundedCornerShape(16.dp))
                    .clickable { onItemClick(item) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = AppTextColor().copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        item,
                        color = AppTextColor().copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = AppTextColor().copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onRemoveClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun TextTextButton(onClick: () -> Unit, text: String) {
    Text(
        text = text,
        color = AccentCyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    )
}
