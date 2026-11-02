import re

file_path = "app/src/main/java/com/example/ui/components/QrScannerDialog.kt"
with open(file_path, "r") as f:
    content = f.read()

# Change QrScannerDialog signature and implementation to capture external lifecycle
old_qr = """@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onSuccess: (ByteArray, ByteArray, ByteArray) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)"""

new_qr = """@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onSuccess: (ByteArray, ByteArray, ByteArray) -> Unit
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)"""

content = content.replace(old_qr, new_qr)

old_scanner_call = "ScannerContent(onDismiss, onSuccess)"
new_scanner_call = "ScannerContent(onDismiss, onSuccess, lifecycleOwner)"
content = content.replace(old_scanner_call, new_scanner_call)

old_scanner_sig = """@Composable
fun ScannerContent(
    onDismiss: () -> Unit,
    onSuccess: (ByteArray, ByteArray, ByteArray) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current"""

new_scanner_sig = """@Composable
fun ScannerContent(
    onDismiss: () -> Unit,
    onSuccess: (ByteArray, ByteArray, ByteArray) -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current"""

content = content.replace(old_scanner_sig, new_scanner_sig)

with open(file_path, "w") as f:
    f.write(content)
