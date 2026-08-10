package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuManagementScreen(
    hotel: HotelEntity,
    viewModel: FinancialViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    val menuItems by viewModel.allMenuItems.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddMenu by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getAppBackgroundGradient(com.example.ui.components.LocalAppTheme.current))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(getGlassIconButtonBg(), CircleShape)
                        .bounceClick()
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTextColor())
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hotel.name,
                        color = AppTextColor(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        lineHeight = 26.sp,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Manage categories and items",
                        color = AppTextColor().copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                IconButton(
                    onClick = { csvImportLauncher.launch("text/*") },
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Import CSV", tint = AppTextColor())
                }
                IconButton(
                    onClick = { csvExportLauncher.launch("menu_${hotel.name.replace(" ", "_")}.csv") },
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = AppTextColor())
                }
            }

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
                                .background(Color.Black.copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Item", color = AppTextColor(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                            Text("Type", color = AppTextColor(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("Price", color = AppTextColor(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        }
                        
                        HorizontalDivider(color = AppTextColor().copy(alpha = 0.1f))

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(menuItems, key = { it.id }) { item ->
                                MenuTableRow(
                                    item = item,
                                    onSave = { updatedItem -> viewModel.updateMenu(updatedItem) },
                                    onDelete = { viewModel.deleteMenu(item) }
                                )
                                HorizontalDivider(color = AppTextColor().copy(alpha = 0.05f))
                            }
                        }
                }
            }
            
            // Bottom Add Area
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = { showAddMenu = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Item", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showAddMenu) {
            AddMenuItemDialog(
                hotelId = hotel.id,
                onDismiss = { showAddMenu = false },
                onSave = { newItem ->
                    viewModel.addMenu(newItem)
                    showAddMenu = false
                }
            )
        }
    }
}

@Composable
fun MenuTableRow(
    item: MenuEntity,
    onSave: (MenuEntity) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    
    var editName by remember { mutableStateOf(item.englishName) }
    var editPrice by remember { mutableStateOf(item.price.toString()) }
    var editType by remember { mutableStateOf(item.type) }
    var editEmoji by remember { mutableStateOf(item.emoji) }

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
                    // Day / Night / Common Toggle Buttons instead of typing "d a y"
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
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val p = editPrice.toDoubleOrNull() ?: item.price
                        if (editName.isNotBlank()) {
                            onSave(item.copy(
                                englishName = editName,
                                banglaName = editName,
                                price = p,
                                emoji = editEmoji,
                                type = editType,
                                isDayMenu = editType.equals("Day", ignoreCase = true) || editType.equals("Common", ignoreCase = true),
                                isNightMenu = editType.equals("Night", ignoreCase = true) || editType.equals("Common", ignoreCase = true)
                            ))
                            isEditing = false
                        }
                    },
                    modifier = Modifier.size(32.dp).background(AccentCyan.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = AccentCyan, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).background(AccentPink.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentPink, modifier = Modifier.size(16.dp))
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
                        onSave(item.copy(
                            type = nextType,
                            isDayMenu = nextType == "Day" || nextType == "Common",
                            isNightMenu = nextType == "Night" || nextType == "Common"
                        ))
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
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTextColor().copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun AddMenuItemDialog(
    hotelId: Int,
    onDismiss: () -> Unit,
    onSave: (MenuEntity) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newEmoji by remember { mutableStateOf("🍽️") }
    var newType by remember { mutableStateOf("Day") } // Day, Night, Common

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
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
                modifier = Modifier.padding(24.dp),
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AccentCyan else AppTextColor().copy(alpha = 0.08f))
                                .border(1.dp, if (isSelected) AccentCyan else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { newType = typeOpt }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = typeOpt,
                                    tint = if (isSelected) Color.Black else AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    typeOpt,
                                    color = if (isSelected) Color.Black else AppTextColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                            if (newName.isNotBlank()) {
                                onSave(MenuEntity(
                                    hotelId = hotelId,
                                    banglaName = newName,
                                    englishName = newName,
                                    price = p,
                                    isDayMenu = newType == "Day" || newType == "Common",
                                    isNightMenu = newType == "Night" || newType == "Common",
                                    isAddOn = false,
                                    emoji = newEmoji,
                                    type = newType
                                ))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp).bounceClick()
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
