with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

target = """    fun importHotelViaQr(payload: ByteArray, key: ByteArray, iv: ByteArray, onResult: (com.example.data.ImportResult) -> Unit) {
        androidx.lifecycle.viewModelScope.launch {"""

repl = """    fun importHotelViaQr(payload: ByteArray, key: ByteArray, iv: ByteArray, onResult: (com.example.data.ImportResult) -> Unit) {
        androidx.lifecycle.viewModelScope.launch {"""

# Ah, let's just use regex to replace it
import re
content = re.sub(
    r'androidx\.lifecycle\.viewModelScope\.launch',
    r'viewModelScope.launch',
    content
)

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
