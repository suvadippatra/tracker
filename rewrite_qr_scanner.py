import re

with open("app/src/main/java/com/example/ui/components/QrScannerDialog.kt", "r") as f:
    content = f.read()

# Make sure cameraError and cameraProviderRef are declared
target_vars = """    var isDecoding by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Point camera at sender QR...") }
    val coroutineScope = rememberCoroutineScope()"""

replacement_vars = """    var isDecoding by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Point camera at sender QR...") }
    val coroutineScope = rememberCoroutineScope()
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraProviderRef by remember { mutableStateOf<androidx.camera.lifecycle.ProcessCameraProvider?>(null) }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraProviderRef?.unbindAll()
        }
    }"""

if target_vars in content:
    content = content.replace(target_vars, replacement_vars)
else:
    print("Could not find target vars")

# Add the camera error overlay
target_box = """    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView("""

replacement_box = """    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraError != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.Error, contentDescription = "Camera Error", tint = Color.Red, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(cameraError!!, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Close")
                }
            }
        } else {
        AndroidView("""

if target_box in content:
    content = content.replace(target_box, replacement_box)
else:
    print("Could not find target box")

# Fix the end braces
target_end = """                Text("Cancel")
            }
        }
    }
}"""

replacement_end = """                Text("Cancel")
            }
        }
        }
    }
}"""

# Actually, the file currently ends with:
#                 Text("Cancel")
#             }
#         }
#         }
#     }
# }

with open("app/src/main/java/com/example/ui/components/QrScannerDialog.kt", "w") as f:
    f.write(content)
