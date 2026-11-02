import re

with open("app/src/main/java/com/example/ui/components/QrScannerDialog.kt", "r") as f:
    content = f.read()

# Replace the camera binding block
old_block = """                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }"""

new_block = """                    try {
                        cameraProvider.unbindAll()
                        try {
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

content = content.replace(old_block, new_block)

with open("app/src/main/java/com/example/ui/components/QrScannerDialog.kt", "w") as f:
    f.write(content)

