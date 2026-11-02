package com.example.ui.components

import android.Manifest
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.QrFountainDecoder
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onSuccess: (ByteArray, ByteArray, ByteArray) -> Unit
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
        com.example.ui.components.DialogEdgeToEdge()
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
    onSuccess: (ByteArray, ByteArray, ByteArray) -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    val decoder = remember { QrFountainDecoder() }
    
    var receivedCount by remember { mutableIntStateOf(0) }
    var requiredCount by remember { mutableIntStateOf(0) }
    var isDecoding by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Point camera at sender QR...") }
    val coroutineScope = rememberCoroutineScope()
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraProviderRef by remember { mutableStateOf<androidx.camera.lifecycle.ProcessCameraProvider?>(null) }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraProviderRef?.unbindAll()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraError != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
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
                                    
                                imageAnalysis.setAnalyzer(analysisExecutor, ZXingAnalyzer { packetBytes ->
                                    if (isDecoding) return@ZXingAnalyzer
                                    
                                    val isProcessed = decoder.processPacket(packetBytes)
                                    if (isProcessed) {
                                        coroutineScope.launch(Dispatchers.Main) {
                                            receivedCount = decoder.receivedChunks.size
                                            requiredCount = decoder.k
                                            
                                            if (decoder.isInitialized) {
                                                statusText = "Sync in progress: ${decoder.receivedChunks.size}/${decoder.k} chunks"
                                            }
                                            
                                            val payload = decoder.tryDecode()
                                            if (payload != null && !isDecoding) {
                                                isDecoding = true
                                                statusText = "Payload reconstructed! Importing..."
                                                onSuccess(payload, decoder.key!!, decoder.iv!!)
                                            }
                                        }
                                    }
                                })
                                
                                cameraProvider.unbindAll()
                                try {
                                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                                } catch (e: Exception) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
            
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Supports continuous stream and manual (< >) stepping",
                color = Color(0xFF81C784),
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
        }
    }
}
