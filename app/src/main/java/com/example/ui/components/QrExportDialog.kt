package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onDismiss: () -> Unit
) {
    val encoder = remember { QrFountainEncoder(exportData.payload, sessionId, exportData.key, exportData.iv) }
    var packetIndex by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    val cycleSize = encoder.k + 2
    val totalInitAndData = encoder.k + 1

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
            generateQrCode(packetBytes, 450)
        }
    }

    LaunchedEffect(isPaused, packetIndex) {
        if (!isPaused) {
            delay(300) // Comfortable 3.3 fps rate for optimal scanner detection
            packetIndex++
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF16181F)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "QR Sync Stream",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPaused) "Paused • Step through using < >" else "Streaming • Keep visible to scanner",
                            color = if (isPaused) Color(0xFFFFB74D) else Color(0xFF81C784),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPaused) Color(0xFFFF9800).copy(0.2f) else Color(0xFF4CAF50).copy(0.2f)
                    ) {
                        Text(
                            text = if (isPaused) "PAUSED" else "LIVE",
                            color = if (isPaused) Color(0xFFFFB74D) else Color(0xFF81C784),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Container with Navigation arrows when paused
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentBitmap != null) {
                        Image(
                            bitmap = currentBitmap!!.asImageBitmap(),
                            contentDescription = "QR Stream",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current packet metadata badge
                val cyclePos = packetIndex % cycleSize
                val packetType = when {
                    cyclePos == 0 -> "Header & Security Key (INIT)"
                    cyclePos <= encoder.k -> "Data Chunk $cyclePos of ${encoder.k}"
                    else -> "Error Recovery Frame (REPAIR)"
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF232733),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frame #${packetIndex + 1}",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = packetType,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation controls for Pause Mode (< Previous, Play/Pause, Next >)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    IconButton(
                        onClick = {
                            if (packetIndex > 0) {
                                packetIndex--
                            }
                        },
                        enabled = isPaused && packetIndex > 0,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPaused && packetIndex > 0) Color(0xFF2C3242) else Color(0xFF1E222D)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous QR Code",
                            tint = if (isPaused && packetIndex > 0) Color.White else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Play/Pause Button
                    Button(
                        onClick = { isPaused = !isPaused },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaused) Color(0xFF00E5FF) else Color(0xFFFF9800)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPaused) "Resume Stream" else "Pause Stream",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Next Button
                    IconButton(
                        onClick = {
                            packetIndex++
                        },
                        enabled = isPaused,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPaused) Color(0xFF2C3242) else Color(0xFF1E222D)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next QR Code",
                            tint = if (isPaused) Color.White else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Transfer", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

