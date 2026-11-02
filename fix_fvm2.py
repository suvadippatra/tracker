import re

path = "app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt"
with open(path, "r") as f:
    content = f.read()

vm_state = """
    private val _rootOverlayState = MutableStateFlow<RootOverlayState>(RootOverlayState.None)
    val rootOverlayState: StateFlow<RootOverlayState> = _rootOverlayState

    fun showRootOverlay(state: RootOverlayState) {
        _rootOverlayState.value = state
    }

    fun closeRootOverlay() {
        _rootOverlayState.value = RootOverlayState.None
    }
"""

if "_rootOverlayState" not in content:
    content = content.replace(") : AndroidViewModel(application) {", ") : AndroidViewModel(application) {" + vm_state)
    with open(path, "w") as f:
        f.write(content)
