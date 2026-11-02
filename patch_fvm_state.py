import re

path = "app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt"
with open(path, "r") as f:
    content = f.read()

state_code = """
sealed class RootOverlayState {
    object None : RootOverlayState()
    object AddMeal : RootOverlayState()
    data class ManageMenu(val hotel: com.example.data.model.HotelEntity) : RootOverlayState()
    data class EditHotel(val hotel: com.example.data.model.HotelEntity?) : RootOverlayState()
    object OwnerProfile : RootOverlayState()
    data class AddOwnerAccount(val account: com.example.data.model.OwnerAccountEntity?) : RootOverlayState()
    data class EditBankAccount(val account: com.example.data.model.BankAccountEntity?) : RootOverlayState()
}
"""

if "RootOverlayState" not in content:
    imports = "import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow\n"
    if "import kotlinx.coroutines.flow.MutableStateFlow" not in content:
        content = content.replace("import kotlinx.coroutines.flow.*", imports + "import kotlinx.coroutines.flow.*")
    
    # insert before FinancialViewModel class
    content = content.replace("class FinancialViewModel", state_code + "\nclass FinancialViewModel")

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
    content = content.replace("class FinancialViewModel(application: Application) : AndroidViewModel(application) {", "class FinancialViewModel(application: Application) : AndroidViewModel(application) {" + vm_state)

with open(path, "w") as f:
    f.write(content)

print("ViewModel patched.")
