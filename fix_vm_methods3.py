with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "fun addHotel(" in line and "HotelEntity)" in line:
        new_lines.append("    fun addHotel(name: String, advancedPayment: Double, phoneNumber: String, location: String, upiId: String, qrCodeUri: String) = viewModelScope.launch { repository.insertHotel(com.example.data.model.HotelEntity(name = name, advancedPayment = advancedPayment, phoneNumber = phoneNumber, location = location, upiId = upiId, qrCodeUri = qrCodeUri)) }\n")
    elif "fun addVirtualAccount(" in line and "BankAccountEntity)" in line:
        new_lines.append("    fun addVirtualAccount(name: String, balance: Double, upiId: String, qrCodeUri: String, accountNumber: String, ifsc: String, branchName: String) = viewModelScope.launch { repository.insertBankAccount(com.example.data.model.BankAccountEntity(name = name, balance = balance, upiId = upiId, qrCodeUri = qrCodeUri, accountNumber = accountNumber, ifsc = ifsc, branchName = branchName)) }\n")
    elif "fun addCategory(" in line and "TransactionCategoryEntity)" in line:
        new_lines.append("    fun addCategory(name: String, type: String) = viewModelScope.launch { repository.insertCategory(com.example.data.model.TransactionCategoryEntity(name = name, type = type)) }\n")
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.writelines(new_lines)
