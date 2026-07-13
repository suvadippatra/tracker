import re
with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "r") as f:
    content = f.read()

old_add = """    fun addHotel(name: String, advancedPayment: Double, phoneNumber: String = "") {
        viewModelScope.launch {
            repository.insertHotel(HotelEntity(name = name, advancedPayment = advancedPayment, phoneNumber = phoneNumber))
        }
    }"""

new_add = """    fun addHotel(name: String, advancedPayment: Double, phoneNumber: String = "") {
        viewModelScope.launch {
            val id = repository.insertHotelWithId(HotelEntity(name = name, advancedPayment = advancedPayment, phoneNumber = phoneNumber))
            // Copy menu from hotel 1 to the new hotel
            val baseMenu = repository.getMenuForHotel(1).kotlinx.coroutines.flow.firstOrNull() ?: emptyList()
            baseMenu.forEach { 
                repository.insertMenu(it.copy(id = 0, hotelId = id.toInt()))
            }
        }
    }"""

# Wait, this requires insertHotel to return Long and repository to expose it!
