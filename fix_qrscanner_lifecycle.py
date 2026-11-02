import re

with open("app/src/main/java/com/example/ui/components/QrScannerDialog.kt", "r") as f:
    content = f.read()

target = """    var isDecoding by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Point camera at sender QR...") }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize()) {"""

replacement = """    var isDecoding by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Point camera at sender QR...") }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    var cameraProviderRef by remember { mutableStateOf<androidx.camera.lifecycle.ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderRef?.unbindAll()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {"""

content = content.replace(target, replacement)

target2 = """                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {"""

replacement2 = """                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef = cameraProvider
                    val preview = Preview.Builder().build().also {"""

content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/components/QrScannerDialog.kt", "w") as f:
    f.write(content)
