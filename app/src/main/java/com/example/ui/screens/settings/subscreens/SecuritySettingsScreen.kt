package com.example.ui.screens.settings.subscreens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.RecoverySetupDialog
import com.example.ui.screens.SecurityPinDialog
import com.example.ui.screens.SecurityPinMode
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.screens.settings.components.PixelSettingsSwitchItem
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun SecuritySettingsScreen(
    viewModel: FinancialViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val prefLanguage = userProfile.language

    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogMode by remember { mutableStateOf(SecurityPinMode.SET_NEW) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (showPinDialog) {
            showPinDialog = false
        } else if (showRecoveryDialog) {
            showRecoveryDialog = false
        } else {
            onBack()
        }
    }

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
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. App Lock Controls
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "অ্যাপ সুরক্ষা ও পাসকোড" else "App Security & Passcode") {
                PixelSettingsSwitchItem(
                    icon = if (userProfile.appLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                    title = if (prefLanguage == "bn") "পিন লক (পাসকোড)" else "PIN Lock (4-Digit Passcode)",
                    subtitle = if (userProfile.appLockEnabled) {
                        if (prefLanguage == "bn") "সক্রিয় - অ্যাপ খোলার সময় পিন লাগবে" else "Active - Requires 4-digit PIN on launch"
                    } else {
                        if (prefLanguage == "bn") "নিষ্ক্রিয় - কোনো পাসকোড নেই" else "Disabled - No passcode required"
                    },
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
                    showDivider = userProfile.appLockEnabled
                )

                if (userProfile.appLockEnabled) {
                    PixelSettingsItem(
                        icon = Icons.Default.Password,
                        title = if (prefLanguage == "bn") "পাসকোড পরিবর্তন করুন" else "Change 4-Digit PIN",
                        subtitle = if (prefLanguage == "bn") "নতুন ৪-সংখ্যার পাসকোড সেট করুন" else "Update existing 4-digit passcode",
                        onClick = {
                            pinDialogMode = SecurityPinMode.CHANGE
                            showPinDialog = true
                        }
                    )
                }
            }
        }

        // 2. Biometric Authentication
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "বায়োমেট্রিক আনলক" else "Biometric Authentication") {
                PixelSettingsSwitchItem(
                    icon = Icons.Default.Fingerprint,
                    title = if (prefLanguage == "bn") "ফিঙ্গারপ্রিন্ট / ফেস আনলক" else "Fingerprint / Face Unlock",
                    subtitle = if (userProfile.biometricEnabled) {
                        if (prefLanguage == "bn") "সক্রিয় - বায়োমেট্রিক দিয়ে দ্রুত আনলক করুন" else "Enabled - Quick unlock with biometric"
                    } else {
                        if (prefLanguage == "bn") "নিষ্ক্রিয়" else "Disabled - Use PIN only"
                    },
                    checked = userProfile.biometricEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked && !userProfile.appLockEnabled) {
                            pinDialogMode = SecurityPinMode.SET_NEW
                            showPinDialog = true
                        } else {
                            viewModel.saveUserProfile(userProfile.copy(biometricEnabled = isChecked))
                        }
                    }
                )
            }
        }

        // 3. Security Recovery Method
        item {
            PixelSettingsGroup(title = if (prefLanguage == "bn") "পাসকোড রিকভারি পদ্ধতি" else "PIN Recovery Setup") {
                PixelSettingsItem(
                    icon = Icons.Default.HelpOutline,
                    title = if (prefLanguage == "bn") "নিরাপত্তা প্রশ্ন ও উত্তর" else "Security Question & Answer",
                    subtitle = if (userProfile.securityQuestion.isNotBlank()) {
                        "Question: ${userProfile.securityQuestion}"
                    } else {
                        if (prefLanguage == "bn") "কোনো প্রশ্ন সেট করা নেই (সেট করতে ট্যাপ করুন)" else "Not configured (Tap to setup)"
                    },
                    onClick = { showRecoveryDialog = true }
                )
            }
        }
    }
}
