@file:Suppress("DEPRECATION")
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val currentDefaultRate by viewModel.defaultElectricityRate.collectAsStateWithLifecycle()

    var chargeInput by remember(currentFixedCharge) { mutableStateOf(currentFixedCharge.toString()) }
    var defaultRateInput by remember(currentDefaultRate) { mutableStateOf(currentDefaultRate.toString()) }

    var rateInput by remember { mutableStateOf("") }
    var rateDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showRateDatePicker by remember { mutableStateOf(false) }

    var rateToDelete by remember { mutableStateOf<ElectricityRate?>(null) }

    val formatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    androidx.activity.compose.BackHandler {
        onDismiss()
    }

    AppThemeProvider(financialViewModel) {
            val primaryAccent = AppAccentColor()
            val onPrimaryAccent = AppOnAccentColor()
            val isLight = isAppLightMode()
            val tonalBtnBg = if (isLight) Color(0xFFD3E3FD) else Color(0xFF004A77)
            val tonalBtnFg = if (isLight) Color(0xFF041E49) else Color(0xFFC2E7FF)

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isLight) M3BackgroundLight else M3BackgroundDark
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(getAppBackgroundGradient(userProfile?.themeMode ?: "Auto", userProfile?.bgPreset ?: "Default"))
                        .pointerInput(Unit) { detectTapGestures { } },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxHeight().widthIn(max = 840.dp),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0.dp),
                        topBar = {
                            Box(modifier = Modifier.fillMaxWidth().dialogSafeTopPadding(extraTop = 4.dp)) {
                                com.example.ui.screens.settings.components.SettingsPageHeader(
                                    title = if (language == "bn") "ইউটিলিটি কনফিগারেশন" else "Utility Configuration",
                                    onBack = onDismiss
                                )
                            }
                        }
                    ) { padding ->
                        val utilityConfigListState = rememberPersistedLazyListState("utility_config")
                        LazyColumn(
                            state = utilityConfigListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(
                                top = 12.dp,
                                bottom = 120.dp + rememberSplitScreenSafeBottomPadding()
                            )
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
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(tonalBtnBg, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bolt,
                                                    contentDescription = null,
                                                    tint = primaryAccent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Text(
                                                text = if (language == "bn") "সার্ভিস ও সাধারণ পরামিতি" else "Service Parameters",
                                                color = AppTextColor(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }

                                        HorizontalDivider(color = AppTextColor().copy(alpha = 0.08f))

                                        GlassTextField(
                                            value = chargeInput,
                                            onValueChange = { chargeInput = it },
                                            label = { Text(if (language == "bn") "স্থির সার্ভিস চার্জ ($currency)" else "Fixed Service Charge ($currency)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = primaryAccent) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Button(
                                            onClick = {
                                                val charge = chargeInput.toDoubleOrNull() ?: 0.0
                                                viewModel.updateCalculationParameters(charge, currentDefaultRate)
                                                val pendingPolicyRate = rateInput.toDoubleOrNull()
                                                if (pendingPolicyRate != null && pendingPolicyRate > 0.0) {
                                                    viewModel.insertElectricityRate(pendingPolicyRate, rateDateMillis)
                                                    rateInput = ""
                                                }
                                                android.widget.Toast.makeText(
                                                    financialViewModel.getApplication(),
                                                    if (language == "bn") "প্যারামিটার সংরক্ষিত হয়েছে" else "Parameters saved successfully",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = primaryAccent,
                                                contentColor = onPrimaryAccent
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .bounceClick()
                                        ) {
                                            Text(
                                                text = if (language == "bn") "প্যারামিটার সংরক্ষণ করুন" else "Save Parameters",
                                                color = onPrimaryAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Add New Rate Policy
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
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(tonalBtnBg, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddChart,
                                                    contentDescription = null,
                                                    tint = primaryAccent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
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
                                            color = AppSecondaryTextColor(),
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

                                            Surface(
                                                onClick = { showRateDatePicker = true },
                                                shape = RoundedCornerShape(16.dp),
                                                color = tonalBtnBg,
                                                shadowElevation = 2.dp,
                                                modifier = Modifier
                                                    .weight(1.3f)
                                                    .height(52.dp)
                                                    .bounceClick()
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CalendarToday,
                                                            contentDescription = null,
                                                            tint = tonalBtnFg,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = formatter.format(Date(rateDateMillis)),
                                                            color = tonalBtnFg,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            FilledIconButton(
                                                onClick = {
                                                    val r = rateInput.toDoubleOrNull()
                                                    if (r != null) {
                                                        viewModel.insertElectricityRate(r, rateDateMillis)
                                                        rateInput = ""
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .bounceClick(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = primaryAccent,
                                                    contentColor = onPrimaryAccent
                                                )
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Add", tint = onPrimaryAccent)
                                            }
                                        }
                                    }
                                }
                            }

                            // Historical Rate Policies List Header
                            item {
                                Text(
                                    text = if (language == "bn") "সক্রিয় ও ঐতিহাসিক রেট নীতিসমূহ" else "Active Rate Policies & History",
                                    color = AppHeadingColor(),
                                    fontSize = 15.sp,
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
                                                color = AppSecondaryTextColor(),
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
                                                    color = AppSecondaryTextColor(),
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
                                                        tint = AppSecondaryTextColor(),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showMenu,
                                                    onDismissRequest = { showMenu = false },
                                                    modifier = Modifier.background(getDialogBackground())
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Move to Bin", color = AccentPink, fontWeight = FontWeight.Medium) },
                                                        onClick = { showMenu = false; rateToDelete = rate }
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
    }
