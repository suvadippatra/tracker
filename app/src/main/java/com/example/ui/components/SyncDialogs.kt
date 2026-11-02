package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BankAccountEntity
import com.example.data.model.HotelEntity
import com.example.data.model.OwnerAccountEntity
import com.example.utils.ParentEntityType
import com.example.utils.ParentResolution
import com.example.utils.PendingNewMenuItem
import com.example.utils.UnmappedParent

/**
 * Dialog for resolving unmapped parent entities (Hotels, Bank Accounts, Owner Accounts).
 * Offers options to:
 * 1. Create as New (with sender attribution if available)
 * 2. Force Merge / Map to an existing local entity
 */
@Composable
fun ParentMappingDialog(
    unmappedParents: List<UnmappedParent>,
    availableHotels: List<HotelEntity>,
    availableBankAccounts: List<BankAccountEntity>,
    availableOwnerAccounts: List<OwnerAccountEntity>,
    currentResolutions: Map<String, ParentResolution>,
    onResolutionChanged: (String, ParentResolution) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = getDialogBackground()),
                border = BorderStroke(1.dp, getGlassBorder())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AccentCyan.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Match Account / Hotel",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTextColor()
                                )
                                Text(
                                    text = "${unmappedParents.size} new item(s) found in import data",
                                    fontSize = 12.sp,
                                    color = AppSecondaryTextColor()
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(getGlassIconButtonBg(), CircleShape)
                                .border(1.dp, getGlassBorder(), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = AppTextColor(),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Helpful Hint Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppTextColor().copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, getGlassBorder()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Choose whether to create fresh profiles or merge records into existing hotels or accounts.",
                                fontSize = 11.5.sp,
                                color = AppTextColor().copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable List of Unmapped Parents
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(unmappedParents, key = { it.entityKey }) { parent ->
                            val currentChoice = currentResolutions[parent.entityKey]
                                ?: ParentResolution.CreateNew(parent.senderName)

                            UnmappedParentItemCard(
                                parent = parent,
                                currentResolution = currentChoice,
                                availableHotels = availableHotels,
                                availableBankAccounts = availableBankAccounts,
                                availableOwnerAccounts = availableOwnerAccounts,
                                onChoiceSelected = { resolution ->
                                    onResolutionChanged(parent.entityKey, resolution)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, getGlassBorder()),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTextColor())
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text(
                                "Continue",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnmappedParentItemCard(
    parent: UnmappedParent,
    currentResolution: ParentResolution,
    availableHotels: List<HotelEntity>,
    availableBankAccounts: List<BankAccountEntity>,
    availableOwnerAccounts: List<OwnerAccountEntity>,
    onChoiceSelected: (ParentResolution) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val icon = when (parent.entityType) {
        ParentEntityType.HOTEL -> Icons.Default.Hotel
        ParentEntityType.BANK_ACCOUNT -> Icons.Default.AccountBalance
        ParentEntityType.OWNER_ACCOUNT -> Icons.Default.Person
    }

    val typeLabel = when (parent.entityType) {
        ParentEntityType.HOTEL -> "Hotel"
        ParentEntityType.BANK_ACCOUNT -> "Bank Account"
        ParentEntityType.OWNER_ACCOUNT -> "Owner Account"
    }

    val selectedLabel = when (currentResolution) {
        is ParentResolution.CreateNew -> "[Create as New]"
        is ParentResolution.MapToExisting -> {
            when (parent.entityType) {
                ParentEntityType.HOTEL -> {
                    val match = availableHotels.firstOrNull { it.id == currentResolution.targetLocalId }
                    match?.name?.let { "Merge into: $it" } ?: "Merge into (ID: ${currentResolution.targetLocalId})"
                }
                ParentEntityType.BANK_ACCOUNT -> {
                    val match = availableBankAccounts.firstOrNull { it.id == currentResolution.targetLocalId }
                    match?.name?.let { "Merge into: $it" } ?: "Merge into (ID: ${currentResolution.targetLocalId})"
                }
                ParentEntityType.OWNER_ACCOUNT -> {
                    val match = availableOwnerAccounts.firstOrNull { it.id == currentResolution.targetLocalId }
                    match?.ownerName?.let { "Merge into: $it" } ?: "Merge into (ID: ${currentResolution.targetLocalId})"
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = getCardBackground()),
        border = BorderStroke(1.dp, getGlassBorder())
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Icon, Name, Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parent.importedName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AppTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (parent.importedSubtitle.isNotBlank()) {
                        Text(
                            text = parent.importedSubtitle,
                            fontSize = 11.sp,
                            color = AppSecondaryTextColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentCyan.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = typeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dropdown Selector Box
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { expanded = true }
                        .border(
                            1.dp,
                            if (currentResolution is ParentResolution.CreateNew) AccentCyan.copy(alpha = 0.6f) else getGlassBorder(),
                            RoundedCornerShape(10.dp)
                        ),
                    color = AppTextColor().copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                if (currentResolution is ParentResolution.CreateNew) Icons.Default.AddCircle else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (currentResolution is ParentResolution.CreateNew) AccentCyan else AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = selectedLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppTextColor(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = AppSecondaryTextColor()
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(getDialogBackground())
                        .border(1.dp, getGlassBorder(), RoundedCornerShape(8.dp))
                ) {
                    // Option 1: Create As New
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "[Create as New]",
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        onClick = {
                            onChoiceSelected(ParentResolution.CreateNew(parent.senderName))
                            expanded = false
                        }
                    )

                    HorizontalDivider(color = getGlassBorder())

                    // Options 2..N: Existing local entities for merging
                    when (parent.entityType) {
                        ParentEntityType.HOTEL -> {
                            if (availableHotels.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No existing hotels to merge", color = AppSecondaryTextColor(), fontSize = 12.sp) },
                                    onClick = { expanded = false },
                                    enabled = false
                                )
                            } else {
                                availableHotels.forEach { hotel ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(hotel.name, color = AppTextColor(), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                if (hotel.location.isNotBlank()) {
                                                    Text(hotel.location, color = AppSecondaryTextColor(), fontSize = 10.5.sp)
                                                }
                                            }
                                        },
                                        onClick = {
                                            onChoiceSelected(ParentResolution.MapToExisting(hotel.id))
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        ParentEntityType.BANK_ACCOUNT -> {
                            if (availableBankAccounts.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No existing accounts to merge", color = AppSecondaryTextColor(), fontSize = 12.sp) },
                                    onClick = { expanded = false },
                                    enabled = false
                                )
                            } else {
                                availableBankAccounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(acc.name, color = AppTextColor(), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                if (acc.accountNumber.isNotBlank()) {
                                                    Text("Acc: ${acc.accountNumber}", color = AppSecondaryTextColor(), fontSize = 10.5.sp)
                                                }
                                            }
                                        },
                                        onClick = {
                                            onChoiceSelected(ParentResolution.MapToExisting(acc.id))
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        ParentEntityType.OWNER_ACCOUNT -> {
                            if (availableOwnerAccounts.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No existing owner profiles to merge", color = AppSecondaryTextColor(), fontSize = 12.sp) },
                                    onClick = { expanded = false },
                                    enabled = false
                                )
                            } else {
                                availableOwnerAccounts.forEach { owner ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(owner.ownerName, color = AppTextColor(), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                if (owner.accountNumber.isNotBlank()) {
                                                    Text("Acc: ${owner.accountNumber}", color = AppSecondaryTextColor(), fontSize = 10.5.sp)
                                                }
                                            }
                                        },
                                        onClick = {
                                            onChoiceSelected(ParentResolution.MapToExisting(owner.id))
                                            expanded = false
                                        }
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

/**
 * Dialog for confirming newly discovered Menu items for existing hotels.
 * Displays each item with a toggle checkbox and price preview.
 */
@Composable
fun MenuConfirmationDialog(
    pendingMenuItems: List<PendingNewMenuItem>,
    acceptedKeys: Set<String>,
    onToggleItem: (String, Boolean) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val allSelected = pendingMenuItems.isNotEmpty() && pendingMenuItems.all { acceptedKeys.contains(it.itemKey) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = getDialogBackground()),
                border = BorderStroke(1.dp, getGlassBorder())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AccentCyan.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.RestaurantMenu,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "New Menu Items",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTextColor()
                                )
                                Text(
                                    text = "${pendingMenuItems.size} new dish(es) found for existing hotels",
                                    fontSize = 12.sp,
                                    color = AppSecondaryTextColor()
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(getGlassIconButtonBg(), CircleShape)
                                .border(1.dp, getGlassBorder(), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = AppTextColor(),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Prompt & Select All Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTextColor().copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Include in your hotel menu?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTextColor()
                        )

                        TextButton(
                            onClick = { onToggleAll(!allSelected) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All",
                                fontSize = 12.sp,
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Group by Hotel Name for clear layout
                    val groupedByHotel = pendingMenuItems.groupBy { it.targetHotelName }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        groupedByHotel.forEach { (hotelName, items) ->
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Hotel,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = hotelName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                }
                            }

                            items(items, key = { it.itemKey }) { pendingItem ->
                                val isChecked = acceptedKeys.contains(pendingItem.itemKey)
                                val item = pendingItem.menuItem

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = getCardBackground(),
                                    border = BorderStroke(1.dp, if (isChecked) AccentCyan.copy(alpha = 0.4f) else getGlassBorder()),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleItem(pendingItem.itemKey, !isChecked) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { onToggleItem(pendingItem.itemKey, it) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = AccentCyan,
                                                checkmarkColor = Color.Black
                                            )
                                        )

                                        Spacer(Modifier.width(6.dp))

                                        Text(
                                            text = item.emoji.ifBlank { "🍽️" },
                                            fontSize = 20.sp
                                        )

                                        Spacer(Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            val displayName = listOfNotNull(
                                                item.englishName.ifBlank { null },
                                                item.banglaName.ifBlank { null }
                                            ).joinToString(" / ").ifBlank { "Unnamed Dish" }

                                            Text(
                                                text = displayName,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = AppTextColor()
                                            )

                                            val typeTags = mutableListOf<String>()
                                            if (item.isDayMenu) typeTags.add("Day")
                                            if (item.isNightMenu) typeTags.add("Night")
                                            if (item.isAddOn) typeTags.add("Add-on")

                                            Text(
                                                text = typeTags.joinToString(" • "),
                                                fontSize = 11.sp,
                                                color = AppSecondaryTextColor()
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AccentGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "₹${item.price}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentGreen,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, getGlassBorder()),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTextColor())
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text(
                                "Confirm (${acceptedKeys.size})",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
