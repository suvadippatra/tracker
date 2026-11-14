package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExportResult
import com.example.data.QrFountainEncoder
import com.example.data.QrMediaCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun QrExportStreamDialog(
    exportData: ExportResult.Success,
    sessionId: String,
    itemName: String = "Application Data",
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val encoder = remember(exportData, sessionId) {
        QrFountainEncoder(exportData.payload, sessionId, exportData.key, exportData.iv)
    }
    var packetIndex by remember(exportData, sessionId) { mutableIntStateOf(0) }
    var currentBitmap by remember(exportData, sessionId) { mutableStateOf<Bitmap?>(null) }

    // Stage 0 = Fountain Data Stream (zero Base64), Stage 1 = Payment QR Sequence (with pre-QR warnings)
    var stage by remember(exportData, sessionId) { mutableIntStateOf(0) }
    var paymentIndex by remember(exportData, sessionId) { mutableIntStateOf(0) }
    var isShowingWarning by remember(exportData, sessionId) { mutableStateOf(true) }
    var paymentBitmap by remember(exportData, sessionId) { mutableStateOf<Bitmap?>(null) }

    // Two-finger pinch-to-zoom factor
    var qrZoomFactor by remember(exportData, sessionId) { mutableFloatStateOf(1f) }

    // Initially paused so user can align scanner before starting stream
    var isPaused by remember(exportData, sessionId) { mutableStateOf(true) }
    var hasStarted by remember(exportData, sessionId) { mutableStateOf(false) }
    var framesPlayedInRun by remember(exportData, sessionId) { mutableIntStateOf(0) }
    var completedFiveRounds by remember(exportData, sessionId) { mutableStateOf(false) }

    val paymentQrs = exportData.paymentQrs
    val hasPaymentQrs = paymentQrs.isNotEmpty()

    val cycleSize = encoder.k + 2
    val maxFramesForFiveRounds = cycleSize * 5

    val optimalFps = remember(encoder.k) {
        when {
            encoder.k < 15 -> 10f
            encoder.k < 40 -> 7f
            else -> 4f
        }
    }
    var fps by remember(exportData, sessionId) { mutableFloatStateOf(optimalFps) }

    fun getPacketBytesForIndex(idx: Int): ByteArray {
        val cyclePos = idx % cycleSize
        return when {
            cyclePos == 0 -> encoder.getInitPacket()
            cyclePos <= encoder.k -> encoder.getSystematicPacket(cyclePos - 1)
            else -> encoder.getRepairPacket(idx)
        }
    }

    LaunchedEffect(stage, packetIndex, encoder) {
        if (stage == 0) {
            val packetBytes = getPacketBytesForIndex(packetIndex)
            currentBitmap = withContext(Dispatchers.Default) {
                generateQrCode(packetBytes, 512)
            }
        }
    }

    LaunchedEffect(stage, paymentIndex, isShowingWarning, paymentQrs) {
        if (stage == 1 && paymentQrs.isNotEmpty()) {
            val safeIdx = paymentIndex.coerceIn(0, paymentQrs.lastIndex)
            val item = paymentQrs[safeIdx]
            paymentBitmap = withContext(Dispatchers.Default) {
                if (isShowingWarning) {
                    val cueText = "TQRCUE|${item.entityType}|${item.originalId}|${item.slotIndex}|${item.totalSlots}|${item.entityName.take(40)}"
                    QrMediaCodec.reconstructExactQrBitmap(
                        QrMediaCodec.DecodedQrPayload(text = cueText, ecLevel = "M"),
                        targetSize = 720,
                        quietZoneModules = 2
                    )
                } else {
                    QrMediaCodec.reconstructExactQrBitmap(
                        item.decodedQr,
                        targetSize = 900,
                        quietZoneModules = 2
                    )
                }
            }
        }
    }

    LaunchedEffect(stage, isPaused, packetIndex, fps, framesPlayedInRun) {
        if (stage == 0 && !isPaused) {
            delay((1000f / fps).toLong())
            val nextPlayed = framesPlayedInRun + 1
            if (nextPlayed >= maxFramesForFiveRounds) {
                // Completed 5 full rounds: return to beginning frame (0) and pause
                packetIndex = 0
                framesPlayedInRun = 0
                isPaused = true
                completedFiveRounds = true
            } else {
                framesPlayedInRun = nextPlayed
                packetIndex++
            }
        }
    }

    val isLight = isAppLightMode()
    val accentColor = AppAccentColor()
    val textColor = AppTextColor()
    val secondaryTextColor = AppSecondaryTextColor()
    val surfaceBg = if (isLight) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    val displayItemName = remember(itemName) {
        if (itemName.startsWith("sending", ignoreCase = true)) {
            itemName
        } else {
            "Sending: $itemName"
        }
    }

    val currentPaymentItem = if (hasPaymentQrs) paymentQrs[paymentIndex.coerceIn(0, paymentQrs.lastIndex)] else null

    val statusBgColor = when {
        stage == 1 && isShowingWarning -> if (isLight) Color(0xFFF59E0B).copy(alpha = 0.16f) else Color(0xFFFBBF24).copy(alpha = 0.22f)
        stage == 1 -> if (isLight) Color(0xFF16A34A).copy(alpha = 0.14f) else Color(0xFF4ADE80).copy(alpha = 0.2f)
        !hasStarted -> if (isLight) Color(0xFF0284C7).copy(alpha = 0.12f) else Color(0xFF38BDF8).copy(alpha = 0.18f)
        isPaused -> if (isLight) Color(0xFFD97706).copy(alpha = 0.12f) else Color(0xFFFBBF24).copy(alpha = 0.18f)
        else -> if (isLight) Color(0xFF16A34A).copy(alpha = 0.12f) else Color(0xFF4ADE80).copy(alpha = 0.18f)
    }
    val statusTextColor = when {
        stage == 1 && isShowingWarning -> if (isLight) Color(0xFFB45309) else Color(0xFFFCD34D)
        stage == 1 -> if (isLight) Color(0xFF15803D) else Color(0xFF4ADE80)
        !hasStarted -> if (isLight) Color(0xFF0369A1) else Color(0xFF38BDF8)
        isPaused -> if (isLight) Color(0xFFB45309) else Color(0xFFFBBF24)
        else -> if (isLight) Color(0xFF15803D) else Color(0xFF4ADE80)
    }

    val cyclePos = packetIndex % cycleSize
    val currentRound = (framesPlayedInRun / cycleSize).coerceIn(0, 4) + 1
    val frameLabel = when {
        stage == 1 && currentPaymentItem != null -> {
            val slotInfo = if (currentPaymentItem.totalSlots > 1) " • QR ${currentPaymentItem.slotIndex + 1}/${currentPaymentItem.totalSlots}" else ""
            if (isShowingWarning) {
                "Warning ${paymentIndex + 1}/${paymentQrs.size}$slotInfo"
            } else {
                "Payment QR ${paymentIndex + 1}/${paymentQrs.size}$slotInfo"
            }
        }
        completedFiveRounds && isPaused -> "5 Rounds Done • Reset at Frame 1"
        cyclePos == 0 -> "INIT Header (Round $currentRound/5)"
        cyclePos <= encoder.k -> "Chunk $cyclePos/${encoder.k} (Round $currentRound/5)"
        else -> "Recovery Packet (Round $currentRound/5)"
    }

    val activeQrBitmap = if (stage == 0) currentBitmap else paymentBitmap
    val warningBannerText = remember(exportData.skippedPaymentQrNames) {
        if (exportData.skippedPaymentQrNames.isEmpty()) ""
        else "Could not decode QR: ${exportData.skippedPaymentQrNames.joinToString(", ")}"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        DialogEdgeToEdge()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = surfaceBg
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = surfaceBg,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dialogSafeTopPadding(extraTop = 4.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (stage == 1) {
                                    stage = 0
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(getGlassIconButtonBg(), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = "Back",
                                tint = textColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = if (stage == 0) "QR Sync Stream" else "Payment QR Transfer",
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBgColor
                        ) {
                            Text(
                                text = when {
                                    stage == 1 && isShowingWarning -> "WARNING"
                                    stage == 1 -> "PAYMENT QR"
                                    !hasStarted -> "READY"
                                    isPaused -> "PAUSED"
                                    else -> "STREAMING"
                                },
                                color = statusTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textColor
                            )
                        }
                    }
                }
            ) { innerPadding ->
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val isLandscape = maxWidth > maxHeight

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QrStreamDisplayBox(
                                activeBitmap = activeQrBitmap,
                                isPaymentStage = stage == 1,
                                isShowingWarning = isShowingWarning,
                                qrZoomFactor = qrZoomFactor,
                                onZoomFactorChange = { qrZoomFactor = it },
                                accentColor = accentColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = getCardBackground(),
                                    border = BorderStroke(1.dp, getGlassBorder()),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = displayItemName,
                                            color = accentColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = frameLabel,
                                            color = secondaryTextColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (hasPaymentQrs && stage == 0) {
                                            Text(
                                                text = "${paymentQrs.size} Payment QR(s) attached",
                                                color = AccentGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (warningBannerText.isNotEmpty() && stage == 0) {
                                            Text(
                                                text = warningBannerText,
                                                color = Color(0xFFF59E0B),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (stage == 1 && currentPaymentItem != null) {
                                    QrPaymentStageControls(
                                        item = currentPaymentItem,
                                        isShowingWarning = isShowingWarning,
                                        paymentIndex = paymentIndex,
                                        totalPaymentQrs = paymentQrs.size,
                                        accentColor = accentColor,
                                        textColor = textColor,
                                        onPrev = {
                                            if (!isShowingWarning) {
                                                isShowingWarning = true
                                            } else if (paymentIndex > 0) {
                                                paymentIndex--
                                                isShowingWarning = true
                                            } else {
                                                stage = 0
                                            }
                                        },
                                        onNext = {
                                            if (isShowingWarning) {
                                                isShowingWarning = false
                                            } else if (paymentIndex < paymentQrs.lastIndex) {
                                                paymentIndex++
                                                isShowingWarning = true
                                            } else {
                                                onDismiss()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = { if (packetIndex > 0) packetIndex-- },
                                            enabled = isPaused && packetIndex > 0,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Previous Frame"
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (!hasStarted || completedFiveRounds) {
                                                    hasStarted = true
                                                    completedFiveRounds = false
                                                    packetIndex = 0
                                                    framesPlayedInRun = 0
                                                    isPaused = false
                                                } else {
                                                    isPaused = !isPaused
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (!hasStarted || isPaused) accentColor else Color(0xFFE65100),
                                                contentColor = if (!hasStarted || isPaused) AppOnAccentColor(accentColor) else Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(44.dp).testTag("qr_stream_play_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (!hasStarted || isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = when {
                                                    !hasStarted -> "Start QR Stream (5 Rounds)"
                                                    completedFiveRounds && isPaused -> "Start Again (5 Rounds)"
                                                    isPaused -> "Resume Stream"
                                                    else -> "Pause Stream"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        FilledTonalIconButton(
                                            onClick = { packetIndex++ },
                                            enabled = isPaused,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Next Frame"
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Stream Speed: ${fps.toInt()} FPS",
                                            color = textColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Slider(
                                            value = fps,
                                            onValueChange = { fps = it },
                                            valueRange = 1f..15f,
                                            steps = 13,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor
                                            )
                                        )
                                    }

                                    if (hasPaymentQrs) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FilledTonalButton(
                                            onClick = {
                                                isPaused = true
                                                paymentIndex = 0
                                                isShowingWarning = true
                                                stage = 1
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("qr_next_payment_stage_btn")
                                        ) {
                                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "Next: Payment QRs (${paymentQrs.size})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = getCardBackground(),
                                border = BorderStroke(1.dp, getGlassBorder()),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = displayItemName,
                                                color = textColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (hasPaymentQrs && stage == 0) {
                                                Text(
                                                    text = "${paymentQrs.size} Payment QR(s) queued after stream",
                                                    color = accentColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = accentColor.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = frameLabel,
                                                color = accentColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    if (warningBannerText.isNotEmpty() && stage == 0) {
                                        Text(
                                            text = warningBannerText,
                                            color = Color(0xFFF59E0B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            QrStreamDisplayBox(
                                activeBitmap = activeQrBitmap,
                                isPaymentStage = stage == 1,
                                isShowingWarning = isShowingWarning,
                                qrZoomFactor = qrZoomFactor,
                                onZoomFactorChange = { qrZoomFactor = it },
                                accentColor = accentColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )

                            if (stage == 1 && currentPaymentItem != null) {
                                QrPaymentStageControls(
                                    item = currentPaymentItem,
                                    isShowingWarning = isShowingWarning,
                                    paymentIndex = paymentIndex,
                                    totalPaymentQrs = paymentQrs.size,
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    onPrev = {
                                        if (!isShowingWarning) {
                                            isShowingWarning = true
                                        } else if (paymentIndex > 0) {
                                            paymentIndex--
                                            isShowingWarning = true
                                        } else {
                                            stage = 0
                                        }
                                    },
                                    onNext = {
                                        if (isShowingWarning) {
                                            isShowingWarning = false
                                        } else if (paymentIndex < paymentQrs.lastIndex) {
                                            paymentIndex++
                                            isShowingWarning = true
                                        } else {
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = getCardBackground(),
                                    border = BorderStroke(1.dp, getGlassBorder()),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            FilledTonalIconButton(
                                                onClick = { if (packetIndex > 0) packetIndex-- },
                                                enabled = isPaused && packetIndex > 0,
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Previous Frame"
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    if (!hasStarted || completedFiveRounds) {
                                                        hasStarted = true
                                                        completedFiveRounds = false
                                                        packetIndex = 0
                                                        framesPlayedInRun = 0
                                                        isPaused = false
                                                    } else {
                                                        isPaused = !isPaused
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (!hasStarted || isPaused) accentColor else Color(0xFFE65100),
                                                    contentColor = if (!hasStarted || isPaused) AppOnAccentColor(accentColor) else Color.White
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .testTag("qr_stream_play_btn")
                                            ) {
                                                Icon(
                                                    imageVector = if (!hasStarted || isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = when {
                                                        !hasStarted -> "Start QR Frames (5 Rounds)"
                                                        completedFiveRounds && isPaused -> "Start Again (5 Rounds)"
                                                        isPaused -> "Resume Frames"
                                                        else -> "Pause Stream"
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }

                                            FilledTonalIconButton(
                                                onClick = { packetIndex++ },
                                                enabled = isPaused,
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "Next Frame"
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "Speed: ${fps.toInt()} FPS",
                                                color = textColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Slider(
                                                value = fps,
                                                onValueChange = { fps = it },
                                                valueRange = 1f..15f,
                                                steps = 13,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = accentColor,
                                                    activeTrackColor = accentColor
                                                )
                                            )
                                        }

                                        if (hasPaymentQrs) {
                                            FilledTonalButton(
                                                onClick = {
                                                    isPaused = true
                                                    paymentIndex = 0
                                                    isShowingWarning = true
                                                    stage = 1
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp)
                                                    .testTag("qr_next_payment_stage_btn")
                                            ) {
                                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "Next: Payment QRs (${paymentQrs.size}) →",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
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
        }
    }
}

@Composable
private fun QrStreamDisplayBox(
    activeBitmap: Bitmap?,
    isPaymentStage: Boolean,
    isShowingWarning: Boolean,
    qrZoomFactor: Float,
    onZoomFactorChange: (Float) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val cachedImageBitmap = remember(activeBitmap) {
        activeBitmap?.asImageBitmap()
    }
    BoxWithConstraints(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    onZoomFactorChange((qrZoomFactor * zoom).coerceIn(0.85f, 1.35f))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val maxAllowedSide = minOf(maxWidth, maxHeight)
        val baseSide = minOf(maxWidth * 0.88f, maxHeight * 0.92f).coerceAtLeast(120.dp)
        val zoomedSide = (baseSide * qrZoomFactor).coerceIn(baseSide * 0.85f, maxAllowedSide.coerceAtLeast(120.dp))
        val innerPad = (12.dp * (1.35f - qrZoomFactor).coerceIn(0.15f, 1f))
        Surface(
            modifier = Modifier.size(zoomedSide),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            border = BorderStroke(
                width = if (isPaymentStage && isShowingWarning) 2.5.dp else 1.5.dp,
                color = if (isPaymentStage && isShowingWarning) Color(0xFFF59E0B) else getGlassBorder()
            ),
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPad),
                contentAlignment = Alignment.Center
            ) {
                if (cachedImageBitmap != null) {
                    Image(
                        bitmap = cachedImageBitmap,
                        contentDescription = if (!isPaymentStage) "QR Stream Frame" else "Payment QR Code",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None
                    )
                } else {
                    CircularProgressIndicator(
                        color = accentColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QrPaymentStageControls(
    item: QrMediaCodec.PaymentQrTransferItem,
    isShowingWarning: Boolean,
    paymentIndex: Int,
    totalPaymentQrs: Int,
    accentColor: Color,
    textColor: Color,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = getCardBackground(),
        border = BorderStroke(
            width = 1.dp,
            color = if (isShowingWarning) Color(0xFFF59E0B).copy(alpha = 0.7f) else getGlassBorder()
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isShowingWarning) Icons.Default.Warning else Icons.Default.QrCode,
                    contentDescription = null,
                    tint = if (isShowingWarning) Color(0xFFF59E0B) else AccentGreen,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (isShowingWarning) {
                        "Going to present Payment QR for ${item.entityType}: ${item.entityName}"
                    } else {
                        "Payment QR Active: ${item.entityType} • ${item.entityName}"
                    },
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrev,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp).testTag("qr_payment_prev_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when {
                            !isShowingWarning -> "Warning"
                            paymentIndex > 0 -> "Prev"
                            else -> "Data QR"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isShowingWarning) Color(0xFFF59E0B) else accentColor,
                        contentColor = if (isShowingWarning) Color.Black else AppOnAccentColor(accentColor)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("qr_payment_next_btn")
                ) {
                    Text(
                        text = when {
                            isShowingWarning -> "Present Payment QR"
                            paymentIndex < totalPaymentQrs - 1 -> "Next Warning"
                            else -> "Done"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
