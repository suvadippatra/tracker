import re

with open("app/src/main/java/com/example/ui/components/QrScannerDialog.kt", "r") as f:
    content = f.read()

target = """        var receivedCount by remember { mutableIntStateOf(0) }
    var requiredCount by remember { mutableIntStateOf(0) }
    var isDecoding by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Point camera at sender QR...") }"""

replacement = """        var receivedCount by remember { mutableIntStateOf(0) }
    var requiredCount by remember { mutableIntStateOf(0) }
    var isDecoding by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Point camera at sender QR...") }
    var cameraError by remember { mutableStateOf<String?>(null) }"""

content = content.replace(target, replacement)

target2 = """                        try {
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                        } catch (e: Exception) {
                            try {
                                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
                            } catch (e2: Exception) {
                                android.widget.Toast.makeText(ctx, "No camera found or supported on this device.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }"""

replacement2 = """                        try {
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                        } catch (e: Exception) {
                            try {
                                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
                            } catch (e2: Exception) {
                                coroutineScope.launch(Dispatchers.Main) {
                                    cameraError = "No camera found or supported on this device."
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        coroutineScope.launch(Dispatchers.Main) {
                            cameraError = "Camera initialization failed: ${e.message}"
                        }
                    }"""

content = content.replace(target2, replacement2)

target3 = """        Box(modifier = Modifier.fillMaxSize()) {
        AndroidView("""

replacement3 = """        Box(modifier = Modifier.fillMaxSize()) {
        if (cameraError != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Error, contentDescription = "Camera Error", tint = Color.Red, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(cameraError!!, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Close")
                }
            }
        } else {
        AndroidView("""

content = content.replace(target3, replacement3)

target4 = """                Text("Cancel")
            }
        }
    }
}"""

replacement4 = """                Text("Cancel")
            }
        }
        }
    }
}"""

content = content.replace(target4, replacement4)

with open("app/src/main/java/com/example/ui/components/QrScannerDialog.kt", "w") as f:
    f.write(content)
