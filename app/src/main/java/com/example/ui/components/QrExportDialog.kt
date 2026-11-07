package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExportResult
import com.example.data.QrFountainEncoder
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

    val encoder = remember { QrFountainEncoder(exportData.payload, sessionId, exportData.key, exportData.iv) }
    var packetIndex by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Two-finger pinch-to-zoom factor (1.0f = default, up to 1.45f = full width/height edge-to-edge)
    var qrZoomFactor by remember { mutableFloatStateOf(1f) }

    // Initially paused so user can align scanner before starting stream
    var isPaused by remember { mutableStateOf(true) }
    var hasStarted by remember { mutableStateOf(false) }

    val cycleSize = encoder.k + 2

    val optimalFps = remember(encoder.k) {
        when {
            encoder.k < 15 -> 10f
            encoder.k < 40 -> 7f
            else -> 4f
        }
    }
    var fps by remember { mutableFloatStateOf(optimalFps) }

    fun getPacketBytesForIndex(idx: Int): ByteArray {
        val cyclePos = idx % cycleSize
        return when {
            cyclePos == 0 -> encoder.getInitPacket()
            cyclePos <= encoder.k -> encoder.getSystematicPacket(cyclePos - 1)
            else -> encoder.getRepairPacket(idx)
        }
    }

    LaunchedEffect(packetIndex) {
        val packetBytes = getPacketBytesForIndex(packetIndex)
        currentBitmap = withContext(Dispatchers.Default) {
            generateQrCode(packetBytes, 512)
        }
    }

    LaunchedEffect(isPaused, packetIndex, fps) {
        if (!isPaused) {
            delay((1000f / fps).toLong())
            packetIndex++
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

    val statusBgColor = when {
        !hasStarted -> if (isLight) Color(0xFF0284C7).copy(alpha = 0.12f) else Color(0xFF38BDF8).copy(alpha = 0.18f)
        isPaused -> if (isLight) Color(0xFFD97706).copy(alpha = 0.12f) else Color(0xFFFBBF24).copy(alpha = 0.18f)
        else -> if (isLight) Color(0xFF16A34A).copy(alpha = 0.12f) else Color(0xFF4ADE80).copy(alpha = 0.18f)
    }
    val statusTextColor = when {
        !hasStarted -> if (isLight) Color(0xFF0369A1) else Color(0xFF38BDF8)
        isPaused -> if (isLight) Color(0xFFB45309) else Color(0xFFFBBF24)
        else -> if (isLight) Color(0xFF15803D) else Color(0xFF4ADE80)
    }

    val cyclePos = packetIndex % cycleSize
    val frameLabel = when {
        cyclePos == 0 -> "INIT Header"
        cyclePos <= encoder.k -> "Chunk $cyclePos / ${encoder.k}"
        else -> "Recovery Packet"
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
        com.example.ui.components.DialogEdgeToEdge()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = surfaceBg
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .dialogSystemBarsPadding(extraTop = 4.dp),
                containerColor = surfaceBg,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .background(getGlassIconButtonBg(), androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.ChevronLeft,
                                contentDescription = "Back",
                                tint = textColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "QR Sync Stream",
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBgColor
                        ) {
                            Text(
                                text = when {
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
                            // Left: Unclipped Square QR Code Box with Two-Finger Pinch-to-Zoom up to Full Height
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, _, zoom, _ ->
                                            qrZoomFactor = (qrZoomFactor * zoom).coerceIn(0.85f, 1.35f)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val maxAllowedSide = minOf(maxWidth, maxHeight)
                                val baseSide = maxAllowedSide * 0.88f
                                val zoomedSide = (baseSide * qrZoomFactor).coerceIn(baseSide * 0.85f, maxAllowedSide)
                                val innerPad = (12.dp * (1.35f - qrZoomFactor).coerceIn(0.15f, 1f))
                                Surface(
                                    modifier = Modifier.size(zoomedSide),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.5.dp, getGlassBorder()),
                                    shadowElevation = 2.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPad),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (currentBitmap != null) {
                                            Image(
                                                bitmap = currentBitmap!!.asImageBitmap(),
                                                contentDescription = "QR Stream Frame",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
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

                            // Right: Stream Info & Controls
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
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

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
                                            if (!hasStarted) {
                                                hasStarted = true
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
                                        modifier = Modifier.height(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (!hasStarted || isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = when {
                                                !hasStarted -> "Start QR Stream"
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

                                Spacer(modifier = Modifier.height(14.dp))

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
                            }
                        }
                    } else {
                        // Portrait Layout: Clean Material 3 vertical stack with guaranteed unclipped QR Code
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Info Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = getCardBackground(),
                                border = BorderStroke(1.dp, getGlassBorder()),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                        Text(
                                            text = "Point receiver camera at QR code (Pinch to Zoom)",
                                            color = secondaryTextColor,
                                            fontSize = 11.sp
                                        )
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
                            }

                            // Center QR Code Container — Guaranteed 1:1 Square with Two-Finger Pinch-to-Zoom up to Full Width
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, _, zoom, _ ->
                                            qrZoomFactor = (qrZoomFactor * zoom).coerceIn(0.85f, 1.35f)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val maxAllowedSide = minOf(maxWidth, maxHeight)
                                val baseSide = minOf(maxWidth * 0.86f, maxHeight * 0.92f)
                                val qrSide = (baseSide * qrZoomFactor).coerceIn(baseSide * 0.85f, maxAllowedSide)
                                val innerPad = (14.dp * (1.35f - qrZoomFactor).coerceIn(0.15f, 1f))
                                Surface(
                                    modifier = Modifier.size(qrSide),
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.5.dp, getGlassBorder()),
                                    shadowElevation = 2.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPad),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (currentBitmap != null) {
                                            Image(
                                                bitmap = currentBitmap!!.asImageBitmap(),
                                                contentDescription = "QR Stream Frame",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
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

                            // Bottom Controls Card
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
                                                if (!hasStarted) {
                                                    hasStarted = true
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
                                        ) {
                                            Icon(
                                                imageVector = if (!hasStarted || isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = when {
                                                    !hasStarted -> "Start QR Frames"
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
