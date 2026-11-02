package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.QrFountainEncoder
import com.example.data.ExportResult
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
    
    // Initially paused as required: user must click start QR frames to start streaming
    var isPaused by remember { mutableStateOf(true) }
    var hasStarted by remember { mutableStateOf(false) }

    val cycleSize = encoder.k + 2

    // Auto-calculate optimal FPS based on total chunks
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
    val bgGradient = getAppBackgroundGradient()

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
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgGradient)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Modern Custom Header / Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor
                            )
                        }
                        
                        Text(
                            text = "QR Sync Stream",
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Transparent spacer to balance the back button
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        val isLandscape = maxWidth > maxHeight
                        
                        // Format display name as "sending [itemName]"
                        val displayItemName = remember(itemName) {
                            if (itemName.startsWith("sending", ignoreCase = true)) {
                                itemName
                            } else {
                                "sending $itemName"
                            }
                        }

                        // Theme-sensitive status pills and banners
                        val statusBgColor = when {
                            !hasStarted -> if (isLight) Color(0xFF0070F3).copy(alpha = 0.12f) else Color(0xFF00E5FF).copy(alpha = 0.18f)
                            isPaused -> if (isLight) Color(0xFFE65100).copy(alpha = 0.12f) else Color(0xFFFF9800).copy(alpha = 0.18f)
                            else -> if (isLight) Color(0xFF2E7D32).copy(alpha = 0.12f) else Color(0xFF4CAF50).copy(alpha = 0.18f)
                        }
                        val statusTextColor = when {
                            !hasStarted -> if (isLight) Color(0xFF0070F3) else Color(0xFF00E5FF)
                            isPaused -> if (isLight) Color(0xFFE65100) else Color(0xFFFFB74D)
                            else -> if (isLight) Color(0xFF2E7D32) else Color(0xFF81C784)
                        }
                        val bannerBgColor = if (isLight) accentColor.copy(alpha = 0.1f) else Color(0xFF1E2330)
                        val bannerTextColor = if (isLight) accentColor else Color(0xFF00E5FF)

                        if (isLandscape) {
                            // Landscape Layout: Side-by-Side horizontal columns
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Pane: Info, Status, Controls, Close Button
                                Column(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .fillMaxHeight()
                                        
                                        .padding(end = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Status Pill
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

                                    // Sending Information Banner
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = bannerBgColor,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = displayItemName,
                                                color = bannerTextColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center
                                            )
                                            val cyclePos = packetIndex % cycleSize
                                            val frameLabel = when {
                                                cyclePos == 0 -> "INIT Header"
                                                cyclePos <= encoder.k -> "Chunk $cyclePos/${encoder.k}"
                                                else -> "Recovery"
                                            }
                                            Text(
                                                text = frameLabel,
                                                color = secondaryTextColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    // Controls Panel
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            onClick = { if (packetIndex > 0) packetIndex-- },
                                            enabled = isPaused && packetIndex > 0,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isPaused && packetIndex > 0) {
                                                        if (isLight) Color.Black.copy(alpha = 0.08f) else Color(0xFF232838)
                                                    } else {
                                                        if (isLight) Color.Black.copy(alpha = 0.03f) else Color(0xFF191D28)
                                                    }
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Previous Frame",
                                                tint = if (isPaused && packetIndex > 0) textColor else secondaryTextColor.copy(alpha = 0.3f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

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
                                                containerColor = when {
                                                    !hasStarted -> accentColor
                                                    isPaused -> accentColor
                                                    else -> if (isLight) Color(0xFFE65100) else Color(0xFFFF9800)
                                                },
                                                contentColor = if (isLight) Color.White else Color.Black
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (!hasStarted || isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = when {
                                                    !hasStarted -> "Start QR Frames"
                                                    isPaused -> "Resume Frames"
                                                    else -> "Pause"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        IconButton(
                                            onClick = { packetIndex++ },
                                            enabled = isPaused,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isPaused) {
                                                        if (isLight) Color.Black.copy(alpha = 0.08f) else Color(0xFF232838)
                                                    } else {
                                                        if (isLight) Color.Black.copy(alpha = 0.03f) else Color(0xFF191D28)
                                                    }
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Next Frame",
                                                tint = if (isPaused) textColor else secondaryTextColor.copy(alpha = 0.3f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Speed Slider
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Speed: ${fps.toInt()} FPS",
                                            color = accentColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Slider(
                                            value = fps,
                                            onValueChange = { fps = it },
                                            valueRange = 1f..15f,
                                            steps = 13,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor,
                                                inactiveTrackColor = if (isLight) Color.Black.copy(alpha = 0.1f) else Color(0xFF2A3040)
                                            )
                                        )
                                    }

                                    

                                    // Close Action
                                    OutlinedButton(
                                        onClick = onDismiss,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Text(
                                            "Close Transfer",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                // Right Pane: QR Code Area (Fills available height)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight(0.95f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color.White)
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (currentBitmap != null) {
                                            Image(
                                                bitmap = currentBitmap!!.asImageBitmap(),
                                                contentDescription = "QR Stream Frame",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            CircularProgressIndicator(
                                                color = accentColor,
                                                strokeWidth = 3.dp,
                                                modifier = Modifier.size(44.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Portrait Layout: Stacked vertically with dynamic QR sizing
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Status Pill
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
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }

                                // Sending Information Banner
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bannerBgColor,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = displayItemName,
                                            color = bannerTextColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val cyclePos = packetIndex % cycleSize
                                        val frameLabel = when {
                                            cyclePos == 0 -> "INIT Header"
                                            cyclePos <= encoder.k -> "Chunk $cyclePos/${encoder.k}"
                                            else -> "Recovery"
                                        }
                                        Text(
                                            text = frameLabel,
                                            color = secondaryTextColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Middle Section: QR Code Area (Dynamic expansion to remaining space)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White)
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (currentBitmap != null) {
                                            Image(
                                                bitmap = currentBitmap!!.asImageBitmap(),
                                                contentDescription = "QR Stream Frame",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            CircularProgressIndicator(
                                                color = accentColor,
                                                strokeWidth = 3.dp,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }

                                // Controls: tightly packed horizontal row
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = { if (packetIndex > 0) packetIndex-- },
                                        enabled = isPaused && packetIndex > 0,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPaused && packetIndex > 0) {
                                                    if (isLight) Color.Black.copy(alpha = 0.08f) else Color(0xFF232838)
                                                } else {
                                                    if (isLight) Color.Black.copy(alpha = 0.03f) else Color(0xFF191D28)
                                                }
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Previous Frame",
                                            tint = if (isPaused && packetIndex > 0) textColor else secondaryTextColor.copy(alpha = 0.3f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

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
                                            containerColor = when {
                                                !hasStarted -> accentColor
                                                isPaused -> accentColor
                                                else -> if (isLight) Color(0xFFE65100) else Color(0xFFFF9800)
                                            },
                                            contentColor = if (isLight) Color.White else Color.Black
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (!hasStarted || isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = when {
                                                !hasStarted -> "Start QR Frames"
                                                isPaused -> "Resume Frames"
                                                else -> "Pause"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    IconButton(
                                        onClick = { packetIndex++ },
                                        enabled = isPaused,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPaused) {
                                                    if (isLight) Color.Black.copy(alpha = 0.08f) else Color(0xFF232838)
                                                } else {
                                                    if (isLight) Color.Black.copy(alpha = 0.03f) else Color(0xFF191D28)
                                                }
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Next Frame",
                                            tint = if (isPaused) textColor else secondaryTextColor.copy(alpha = 0.3f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Compact Speed Slider
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = "Speed: ${fps.toInt()} FPS",
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Slider(
                                        value = fps,
                                        onValueChange = { fps = it },
                                        valueRange = 1f..15f,
                                        steps = 13,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp),
                                        colors = SliderDefaults.colors(
                                            thumbColor = accentColor,
                                            activeTrackColor = accentColor,
                                            inactiveTrackColor = if (isLight) Color.Black.copy(alpha = 0.1f) else Color(0xFF2A3040)
                                        )
                                    )
                                }

                                // Compact Close Action
                                OutlinedButton(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                ) {
                                    Text(
                                        "Close Transfer",
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
