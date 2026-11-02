package com.example.ui.screens
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
fun AppLockScreen(
    correctPin: String,
    language: String,
    userName: String,
    biometricEnabled: Boolean = false,
    securityQuestion: String = "",
    securityAnswer: String = "",
    onUnlock: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showSecurityReset by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Auto-show biometrics if enabled
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && context is androidx.fragment.app.FragmentActivity) {
            val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
            val biometricPrompt = androidx.biometric.BiometricPrompt(context, executor,
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        onUnlock()
                    }
                })
            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle(if (language == "bn") "বায়োমেট্রিক আনলক" else "Biometric Unlock")
                .setNegativeButtonText(if (language == "bn") "পিন ব্যবহার করুন" else "Use PIN")
                .build()
            biometricPrompt.authenticate(promptInfo)
        }
    }

    if (showSecurityReset) {
        SecurityQuestionResetDialog(
            question = securityQuestion,
            correctAnswer = securityAnswer,
            language = language,
            onSuccess = { onUnlock() },
            onDismiss = { showSecurityReset = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo & Lock Icon
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    if (isError) Color(0x33FF4081) else Color(0x1F00E5FF),
                    CircleShape
                )
                .border(1.5.dp, if (isError) AccentPink else AccentCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(72.dp).clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (language == "bn") "${userName.ifBlank { "ব্যবহারকারী" }} এর পকেট লক করা আছে" else "${userName.ifBlank { "User" }}'s Pocket is Locked",
            color = AppTextColor(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isError) {
                if (language == "bn") "ভুল পিন, আবার চেষ্টা করুন" else "Incorrect PIN, please try again"
            } else {
                if (language == "bn") "চালিয়ে যেতে ৪-ডিজিট পিন দিন" else "Enter 4-digit PIN to continue"
            },
            color = if (isError) AccentPink else AppTextColor().copy(alpha = 0.6f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Passcode Indicator Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val active = i < enteredPin.length
                    val color = if (isError) AccentPink else if (active) AccentCyan else AppTextColor().copy(alpha = 0.2f)
                    val size by animateDpAsState(if (active) 16.dp else 12.dp)

                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(color, CircleShape)
                            .border(0.5.dp, AppTextColor().copy(alpha = 0.4f), CircleShape)
                    )
                }
            }
            
            if (biometricEnabled) {
                Spacer(modifier = Modifier.width(24.dp))
                IconButton(onClick = { 
                    if (context is androidx.fragment.app.FragmentActivity) {
                        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
                        val biometricPrompt = androidx.biometric.BiometricPrompt(context, executor,
                            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                    onUnlock()
                                }
                            })
                        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                            .setTitle(if (language == "bn") "বায়োমেট্রিক আনলক" else "Biometric Unlock")
                            .setNegativeButtonText(if (language == "bn") "পিন ব্যবহার করুন" else "Use PIN")
                            .build()
                        biometricPrompt.authenticate(promptInfo)
                    }
                }) {
                    Icon(Icons.Default.Fingerprint, null, tint = AccentCyan, modifier = Modifier.size(32.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Always show Forgot PIN button on App Lock screen
        TextButton(
            onClick = { showSecurityReset = true },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                Icons.Default.HelpOutline,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (language == "bn") "পিন ভুলে গেছেন? রিকভার করুন" else "Forgot PIN? Recover Access",
                color = AccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Keypad Grid
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    row.forEach { digit ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(getGlassIconButtonBg())
                                .border(0.5.dp, getGlassBorder(), CircleShape)
                                .clickable { isError = false
                                    when (digit) {
                                        "C" -> enteredPin = ""
                                        "⌫" -> {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        }
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += digit
                                                if (enteredPin.length == 4) {
                                                    if (enteredPin == correctPin) {
                                                        onUnlock()
                                                    } else {
                                                        isError = true
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit,
                                color = AppTextColor(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SecurityQuestionResetDialog(
    question: String,
    correctAnswer: String,
    language: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    var answer by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val displayQuestion = if (question.isNotBlank()) question else if (language == "bn") "জরুরি পুনরুদ্ধার কোড (মাস্টার কোড: 0000 দিন)" else "Emergency Reset (Enter default PIN: 0000)"

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (language == "bn") "সুরক্ষা প্রশ্ন রিকভারি" else "Security Question Recovery",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextColor()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = displayQuestion,
                    color = AccentCyan,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlassTextField(
                    value = answer,
                    onValueChange = { answer = it; isError = false },
                    label = { Text(if (language == "bn") "আপনার উত্তর লিখুন" else "Enter Recovery Answer") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = if (language == "bn") "ভুল উত্তর, অনুগ্রহ করে আবার চেষ্টা করুন" else "Incorrect answer, please try again",
                        color = AccentPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(if (language == "bn") "বাতিল" else "Cancel", color = AppTextColor()) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = answer.trim()
                            val isCorrect = if (question.isNotBlank() && correctAnswer.isNotBlank()) {
                                trimmed.equals(correctAnswer.trim(), ignoreCase = true)
                            } else {
                                trimmed == "0000"
                            }

                            if (isCorrect) {
                                onSuccess()
                            } else {
                                isError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (language == "bn") "যাচাই করুন" else "Verify & Unlock", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
