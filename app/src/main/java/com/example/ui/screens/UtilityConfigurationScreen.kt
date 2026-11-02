package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ElectricityRate
import com.example.ui.components.*
import com.example.ui.components.SlidingDatePickerModal
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.viewmodel.MeterReadingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityConfigurationScreen(
    viewModel: MeterReadingViewModel,
    financialViewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val userProfile by financialViewModel.userProfile.collectAsStateWithLifecycle()
    val language = userProfile?.language ?: "en"
    val currency = userProfile?.currencySymbol ?: "₹"

    val allRates by viewModel.allElectricityRates.collectAsStateWithLifecycle()
    val currentFixedCharge by viewModel.fixedServiceCharge.collectAsStateWithLifecycle()

    var chargeInput by remember(currentFixedCharge) { mutableStateOf(currentFixedCharge.toString()) }

    var rateInput by remember { mutableStateOf("") }
    var rateDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showRateDatePicker by remember { mutableStateOf(false) }

    var rateToDelete by remember { mutableStateOf<ElectricityRate?>(null) }

    val formatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    androidx.activity.compose.BackHandler {
        onDismiss()
    }

    AppThemeProvider(financialViewModel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(userProfile?.themeMode ?: "Auto", userProfile?.bgPreset ?: "Midnight Glow"))
                .pointerInput(Unit) { detectTapGestures { } }
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Scaffold(
                modifier = Modifier.fillMaxHeight().widthIn(max = 840.dp),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (language == "bn") "ইউটিলিটি কনফিগারেশন" else "Utility Configuration",
                                    color = AppTextColor(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = if (language == "bn") "বিদ্যুৎ চার্জ ও রেট পলিসি পরিচালনা করুন" else "Manage electricity rates & fixed service charges",
                                    color = AppTextColor().copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .background(getGlassBorder().copy(alpha = 0.1f), CircleShape)
                                    .bounceClick()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppTextColor()
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Service Parameters
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = if (language == "bn") "সার্ভিস ও সাধারণ পরামিতি" else "Service Parameters",
                                        color = AppTextColor(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                HorizontalDivider(color = AppTextColor().copy(alpha = 0.1f))

                                GlassTextField(
                                    value = chargeInput,
                                    onValueChange = { chargeInput = it },
                                    label = { Text(if (language == "bn") "স্থির সার্ভিস চার্জ ($currency)" else "Fixed Service Charge ($currency)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = AccentCyan) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        val charge = chargeInput.toDoubleOrNull() ?: 0.0
                                        viewModel.updateCalculationParameters(charge)
                                        android.widget.Toast.makeText(
                                            financialViewModel.getApplication(),
                                            if (language == "bn") "প্যারামিটার সংরক্ষিত হয়েছে" else "Parameters saved successfully",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (language == "bn") "প্যারামিটার সংরক্ষণ করুন" else "Save Parameters",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Add New Rate Policy
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddChart,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = if (language == "bn") "নতুন রেট পলিসি যুক্ত করুন" else "Add New Rate Policy",
                                        color = AppTextColor(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Text(
                                    text = if (language == "bn")
                                        "একটি নতুন ইউনিটের দাম এবং কার্যকরের তারিখ বেছে নিন। বিদ্যুৎ বিল প্রোরটা হিসাব অনুযায়ী অটো-স্প্লিট হবে।"
                                    else
                                        "Specify unit rate and effective date. Electricity bills will be split pro-rata across policy changes automatically.",
                                    color = AppTextColor().copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    GlassTextField(
                                        value = rateInput,
                                        onValueChange = { rateInput = it },
                                        placeholder = { Text("Rate/Unit", fontSize = 13.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AppTextColor().copy(alpha = 0.08f))
                                            .clickable { showRateDatePicker = true }
                                            .padding(vertical = 14.dp, horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = AccentCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = formatter.format(Date(rateDateMillis)),
                                                color = AccentCyan,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            val r = rateInput.toDoubleOrNull()
                                            if (r != null) {
                                                viewModel.insertElectricityRate(r, rateDateMillis)
                                                rateInput = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(AccentCyan, RoundedCornerShape(12.dp))
                                            .bounceClick()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                                    }
                                }
                            }
                        }
                    }

                    // Historical Rate Policies List Header
                    item {
                        Text(
                            text = if (language == "bn") "সক্রিয় ও ঐতিহাসিক রেট নীতিসমূহ" else "Active Rate Policies & History",
                            color = AppTextColor().copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (allRates.isEmpty()) {
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 16.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (language == "bn") "কোনো নির্দিষ্ট নীতি যুক্ত নেই। ডিফল্ট রেট প্রয়োগ হবে।" else "No custom rate policies. Default rate will apply.",
                                        color = AppTextColor().copy(alpha = 0.5f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(allRates.sortedByDescending { it.effectiveDate }, key = { it.id }) { rate ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "$currency${rate.ratePerUnit} / Unit",
                                            color = AppTextColor(),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${if (language == "bn") "কার্যকর:" else "Effective:"} ${formatter.format(Date(rate.effectiveDate))}",
                                            color = AppTextColor().copy(alpha = 0.6f),
                                            fontSize = 12.sp
                                        )
                                    }

                                    var showMenu by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                        IconButton(
                                            onClick = { showMenu = true },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Options",
                                                tint = AppTextColor().copy(alpha = 0.6f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false },
                                            modifier = Modifier.background(getDialogBackground())
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Move to Bin", color = AccentPink) },
                                                onClick = { showMenu = false; rateToDelete = rate },
                                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = AccentPink) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showRateDatePicker) {
                SlidingDatePickerModal(
                    initialDateMillis = rateDateMillis,
                    onDateSelected = {
                        rateDateMillis = it
                        showRateDatePicker = false
                    },
                    onDismiss = { showRateDatePicker = false },
                    textColor = AppTextColor(),
                    backgroundColor = getDialogBackground()
                )
            }

            rateToDelete?.let { rate ->
                Tier1DeleteDialog(
                    itemTitle = "$currency${rate.ratePerUnit} Rate Policy",
                    onConfirm = {
                        viewModel.deleteElectricityRate(rate)
                        rateToDelete = null
                    },
                    onDismiss = { rateToDelete = null }
                )
            }
        }
    }
}
