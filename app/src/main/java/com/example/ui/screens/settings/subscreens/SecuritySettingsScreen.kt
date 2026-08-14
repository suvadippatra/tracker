package com.example.ui.screens.settings.subscreens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.screens.RecoverySetupDialog
import com.example.ui.screens.SecurityPinDialog
import com.example.ui.screens.SecurityPinMode
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun SecuritySettingsScreen(viewModel: FinancialViewModel) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile.language

    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogMode by remember { mutableStateOf(SecurityPinMode.SET_NEW) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    // Security PIN Modal
    if (showPinDialog) {
        SecurityPinDialog(
            mode = pinDialogMode,
            currentSavedPin = userProfile.appLockPin,
            initialQuestion = userProfile.securityQuestion,
            initialAnswer = userProfile.securityAnswer,
            prefLanguage = prefLanguage,
            onDismiss = { showPinDialog = false },
            onSuccess = { newPin, enabled, question, answer ->
                showPinDialog = false
                if (enabled && newPin != null) {
                    viewModel.saveUserProfile(
                        userProfile.copy(
                            appLockEnabled = true,
                            appLockPin = newPin,
                            securityQuestion = if (question.isNotBlank()) question else userProfile.securityQuestion,
                            securityAnswer = if (answer.isNotBlank()) answer else userProfile.securityAnswer
                        )
                    )
                    viewModel.unlockApp()
                    Toast.makeText(
                        context,
                        if (prefLanguage == "bn") "অ্যাপ লক ও রিকভারি মেথড সফলভাবে সক্রিয় করা হয়েছে" else "App Lock & Recovery successfully configured",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.saveUserProfile(
                        userProfile.copy(
                            appLockEnabled = false,
                            appLockPin = "",
                            biometricEnabled = false
                        )
                    )
                    viewModel.unlockApp()
                    Toast.makeText(
                        context,
                        if (prefLanguage == "bn") "অ্যাপ লক নিষ্ক্রিয় করা হয়েছে" else "App Lock disabled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // Recovery Method Setup Modal
    if (showRecoveryDialog) {
        RecoverySetupDialog(
            currentQuestion = userProfile.securityQuestion,
            currentAnswer = userProfile.securityAnswer,
            prefLanguage = prefLanguage,
            onDismiss = { showRecoveryDialog = false },
            onSave = { question, answer ->
                viewModel.saveUserProfile(
                    userProfile.copy(
                        securityQuestion = question,
                        securityAnswer = answer
                    )
                )
                showRecoveryDialog = false
                Toast.makeText(
                    context,
                    if (prefLanguage == "bn") "রিকভারি মেথড সংরক্ষিত হয়েছে" else "Recovery method saved",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. App Lock Controls
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "অ্যাপ সুরক্ষা ও পাসকোড" else "App Security & Passcode") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // App Lock Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(getCardBackground())
                            .border(1.dp, getGlassBorder(), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (userProfile.appLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (userProfile.appLockEnabled) AccentGreen else AppSecondaryTextColor(),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (prefLanguage == "bn") "পিন লক (পাসকোড)" else "PIN Lock (4-Digit Passcode)",
                                    color = AppTextColor(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (userProfile.appLockEnabled) {
                                        if (prefLanguage == "bn") "সক্রিয় - অ্যাপ খোলার সময় পিন লাগবে" else "Active - Requires 4-digit PIN on launch"
                                    } else {
                                        if (prefLanguage == "bn") "নিষ্ক্রিয় - কোনো পাসকোড নেই" else "Disabled - No passcode required"
                                    },
                                    color = if (userProfile.appLockEnabled) AccentGreen else AppSecondaryTextColor(),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = userProfile.appLockEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    pinDialogMode = SecurityPinMode.SET_NEW
                                    showPinDialog = true
                                } else {
                                    pinDialogMode = SecurityPinMode.DISABLE
                                    showPinDialog = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AccentGreen
                            )
                        )
                    }

                    // Change PIN Button (if enabled)
                    if (userProfile.appLockEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(getGlassIconButtonBg())
                                .border(1.dp, getGlassBorder(), RoundedCornerShape(12.dp))
                                .clickable {
                                    pinDialogMode = SecurityPinMode.CHANGE
                                    showPinDialog = true
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Password, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (prefLanguage == "bn") "পাসকোড পরিবর্তন করুন" else "Change 4-Digit PIN",
                                    color = AppTextColor(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppSecondaryTextColor())
                        }
                    }
                }
            }
        }

        // 2. Biometric Authentication
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "বায়োমেট্রিক আনলক" else "Biometric Authentication") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(getCardBackground())
                        .border(1.dp, getGlassBorder(), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (userProfile.biometricEnabled) AccentCyan else AppSecondaryTextColor(),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (prefLanguage == "bn") "ফিঙ্গারপ্রিন্ট / ফেস আনলক" else "Fingerprint / Face Unlock",
                                color = AppTextColor(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (userProfile.biometricEnabled) {
                                    if (prefLanguage == "bn") "সক্রিয় - দ্রুত আনলক করুন" else "Enabled - Quick unlock with biometric"
                                } else {
                                    if (prefLanguage == "bn") "নিষ্ক্রিয়" else "Disabled"
                                },
                                color = AppSecondaryTextColor(),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Switch(
                        checked = userProfile.biometricEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked && !userProfile.appLockEnabled) {
                                // Enable app lock PIN first
                                pinDialogMode = SecurityPinMode.SET_NEW
                                showPinDialog = true
                            } else {
                                viewModel.saveUserProfile(userProfile.copy(biometricEnabled = isChecked))
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AccentCyan
                        )
                    )
                }
            }
        }

        // 3. Security Recovery Method
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "পাসকোড রিকভারি পদ্ধতি" else "PIN Recovery Setup") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(getCardBackground())
                        .border(1.dp, getGlassBorder(), RoundedCornerShape(12.dp))
                        .clickable { showRecoveryDialog = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (prefLanguage == "bn") "নিরাপত্তা প্রশ্ন ও উত্তর" else "Security Question & Answer",
                                    color = AppTextColor(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (userProfile.securityQuestion.isNotBlank()) {
                                        "Question: ${userProfile.securityQuestion}"
                                    } else {
                                        if (prefLanguage == "bn") "কোনো প্রশ্ন সেট করা নেই (সেট করতে ট্যাপ করুন)" else "Not configured (Tap to setup)"
                                    },
                                    color = if (userProfile.securityQuestion.isNotBlank()) AccentCyan else AppSecondaryTextColor(),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentGold, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
