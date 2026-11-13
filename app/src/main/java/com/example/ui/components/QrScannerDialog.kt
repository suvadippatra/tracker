package com.example.ui.components

import android.Manifest
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.QrFountainDecoder
import com.example.data.QrImportManager
import com.example.data.QrManifestItem
import com.example.data.QrMediaCodec
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onSuccess: (ByteArray, ByteArray, ByteArray, Map<String, String>) -> Unit
) {
    BackHandler { onDismiss() }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (cameraPermissionState.status.isGranted) {
                ScannerContent(onDismiss, onSuccess, lifecycleOwner)
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Camera permission is required.", color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Grant Permission")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerContent(
    onDismiss: () -> Unit,
    onSuccess: (ByteArray, ByteArray, ByteArray, Map<String, String>) -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    val decoder = remember { QrFountainDecoder() }
    val coroutineScope = rememberCoroutineScope()

    var receivedCount by remember { mutableIntStateOf(0) }
    var requiredCount by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }

    // Phase 2 Payment QR state (activated when qrManifest is non-empty)
    var isPhase2PaymentQr by remember { mutableStateOf(false) }
    var qrManifest by remember { mutableStateOf<List<QrManifestItem>>(emptyList()) }
    var decodedPayload by remember { mutableStateOf<ByteArray?>(null) }
    var decodedKey by remember { mutableStateOf<ByteArray?>(null) }
    var decodedIv by remember { mutableStateOf<ByteArray?>(null) }

    val capturedPaymentUris = remember { mutableStateMapOf<String, String>() }
    var activeCueKey by remember { mutableStateOf<String?>(null) }
    var activeCueWarning by remember { mutableStateOf<String?>(null) }
    var lastCapturedSignature by remember { mutableStateOf("") }
    var lastCapturedAtMs by remember { mutableLongStateOf(0L) }
    var isReconstructingQr by remember { mutableStateOf(false) }

    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderRef?.unbindAll()
        }
    }

    fun isCurrentFountainPacket(text: String): Boolean {
        if (text.startsWith("S1|")) return true
        if (text.startsWith("P|") || text.startsWith("R|")) {
            val parts = text.split("|", limit = 4)
            if (parts.size >= 3 && parts[1] == decoder.sessionId) return true
        }
        return false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Info, contentDescription = "Camera Error", tint = Color.Red, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(cameraError!!, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Close")
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    try {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val executor = ContextCompat.getMainExecutor(ctx)
                        val analysisExecutor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                cameraProviderRef = cameraProvider
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(
                                    analysisExecutor,
                                    ZXingAnalyzer(
                                        onQrCodeScanned = { packetBytes ->
                                            if (isCompleted || isPhase2PaymentQr) return@ZXingAnalyzer

                                            val isProcessed = decoder.processPacket(packetBytes)
                                            if (isProcessed) {
                                                coroutineScope.launch(Dispatchers.Main) {
                                                    if (isCompleted || isPhase2PaymentQr) return@launch
                                                    receivedCount = decoder.receivedChunks.size
                                                    requiredCount = decoder.k

                                                    val payload = decoder.tryDecode()
                                                    val key = decoder.key
                                                    val iv = decoder.iv
                                                    if (payload != null && key != null && iv != null) {
                                                        val manifest = withContext(Dispatchers.IO) {
                                                            QrImportManager.peekQrManifest(payload, key, iv)
                                                        }
                                                        if (manifest.isEmpty()) {
                                                            isCompleted = true
                                                            onSuccess(payload, key, iv, emptyMap())
                                                        } else {
                                                            decodedPayload = payload
                                                            decodedKey = key
                                                            decodedIv = iv
                                                            qrManifest = manifest
                                                            isPhase2PaymentQr = true
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onQrResult = { result ->
                                            if (isCompleted || !isPhase2PaymentQr) return@ZXingAnalyzer
                                            val text = result.text ?: return@ZXingAnalyzer
                                            if (isCurrentFountainPacket(text)) return@ZXingAnalyzer

                                            coroutineScope.launch(Dispatchers.Main) {
                                                if (isCompleted || !isPhase2PaymentQr) return@launch

                                                if (text.startsWith("TQRCUE|")) {
                                                    val parts = text.split("|")
                                                    val type = parts.getOrNull(1) ?: return@launch
                                                    val id = parts.getOrNull(2) ?: return@launch
                                                    val slot = parts.getOrNull(3) ?: "0"
                                                    val name = parts.getOrNull(5) ?: "Payment QR"
                                                    val cueKey = "$type:$id:$slot"
                                                    activeCueKey = cueKey
                                                    activeCueWarning = "Incoming Payment QR: $type • $name"
                                                    return@launch
                                                }

                                                if (isReconstructingQr) return@launch
                                                val now = System.currentTimeMillis()
                                                if (activeCueKey == null && text == lastCapturedSignature && (now - lastCapturedAtMs) < 1800L) {
                                                    return@launch
                                                }

                                                val targetItem = activeCueKey?.let { cue ->
                                                    qrManifest.find { it.key == cue }
                                                } ?: qrManifest.firstOrNull { !capturedPaymentUris.containsKey(it.key) }
                                                    ?: return@launch

                                                if (activeCueKey == null && capturedPaymentUris.containsKey(targetItem.key)) {
                                                    return@launch
                                                }

                                                isReconstructingQr = true
                                                val savedUri = withContext(Dispatchers.IO) {
                                                    QrMediaCodec.reconstructExactQrPngFromResult(context, result)
                                                }
                                                isReconstructingQr = false

                                                if (!savedUri.isNullOrBlank()) {
                                                    capturedPaymentUris[targetItem.key] = savedUri
                                                    lastCapturedSignature = text
                                                    lastCapturedAtMs = System.currentTimeMillis()
                                                    activeCueKey = null
                                                    activeCueWarning = "Reconstructed QR: ${targetItem.entityName}"

                                                    if (capturedPaymentUris.size >= qrManifest.size &&
                                                        decodedPayload != null && decodedKey != null && decodedIv != null
                                                    ) {
                                                        isCompleted = true
                                                        onSuccess(
                                                            decodedPayload!!,
                                                            decodedKey!!,
                                                            decodedIv!!,
                                                            capturedPaymentUris.toMap()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                )

                                cameraProvider.unbindAll()
                                try {
                                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                                } catch (_: Exception) {
                                    try {
                                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
                                    } catch (e2: Exception) {
                                        coroutineScope.launch(Dispatchers.Main) {
                                            cameraError = "No camera found or supported on this device: ${e2.message}"
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                coroutineScope.launch(Dispatchers.Main) {
                                    cameraError = "Camera binding failed: ${e.message}"
                                }
                            }
                        }, executor)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        coroutineScope.launch(Dispatchers.Main) {
                            cameraError = "Camera initialization error: ${e.message}"
                        }
                    }

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xEE16181F))
                    .dialogSafeBottomPadding(extraBottom = 8.dp)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isPhase2PaymentQr) {
                    Text(
                        text = "Scanning Transfer Stream",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val progress = if (requiredCount == 0) 0f else (receivedCount.toFloat() / requiredCount).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF2C3242)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (requiredCount == 0) "Waiting for Header / Key packet..." else "Collected $receivedCount of $requiredCount required chunks (${(progress * 100).toInt()}%)",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = "Stage 2: Scan Payment QRs (${capturedPaymentUris.size}/${qrManifest.size})",
                        color = Color(0xFF00E5FF),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val progress = if (qrManifest.isEmpty()) 1f else (capturedPaymentUris.size.toFloat() / qrManifest.size).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF4ADE80),
                        trackColor = Color(0xFF2C3242)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    val nextPending = qrManifest.firstOrNull { !capturedPaymentUris.containsKey(it.key) }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFCD34D),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = activeCueWarning ?: if (nextPending != null) {
                                    "Tap 'Next: Payment QRs' on sender & scan Warning + QR for ${nextPending.entityName}"
                                } else {
                                    "All Payment QRs reconstructed!"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        qrManifest.forEach { item ->
                            val isCaptured = capturedPaymentUris.containsKey(item.key)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCaptured) Color(0xFF16A34A).copy(alpha = 0.25f) else Color(0xFF2C3242),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCaptured) Color(0xFF4ADE80) else Color(0xFF475569)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isCaptured) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4ADE80),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "${item.entityType}: ${item.entityName}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Cancel")
                    }

                    if (isPhase2PaymentQr && decodedPayload != null && decodedKey != null && decodedIv != null) {
                        Button(
                            onClick = {
                                if (!isCompleted) {
                                    isCompleted = true
                                    onSuccess(
                                        decodedPayload!!,
                                        decodedKey!!,
                                        decodedIv!!,
                                        capturedPaymentUris.toMap()
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp).testTag("qr_finish_partial_import_btn")
                        ) {
                            Text(
                                text = "Import (${capturedPaymentUris.size}/${qrManifest.size})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
