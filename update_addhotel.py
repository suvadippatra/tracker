with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("fun addHotel(name: String, advancedPayment: Double) {", "fun addHotel(name: String, advancedPayment: Double, phoneNumber: String = \"\") {")
content = content.replace("repository.insertHotel(HotelEntity(name = name, advancedPayment = advancedPayment))", "repository.insertHotel(HotelEntity(name = name, advancedPayment = advancedPayment, phoneNumber = phoneNumber))")

with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "w") as f:
    f.write(content)
