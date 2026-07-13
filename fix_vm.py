import re
with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "r") as f:
    content = f.read()

old_code = """    val allMenuItems: StateFlow<List<MenuEntity>> = repository.allMenuItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())"""

new_code = """    val allMenuItems: StateFlow<List<MenuEntity>> = activeHotelId
        .flatMapLatest { id -> repository.getMenuForHotel(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())"""

content = content.replace(old_code, new_code)

# Ensure flatMapLatest is imported (it usually is if they use it for transactions)

with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "w") as f:
    f.write(content)
