import re

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# fix addHotel
content = re.sub(
    r'fun addHotel\(hotel: com\.example\.data\.model\.HotelEntity\) = viewModelScope\.launch \{ repository\.insertHotel\(hotel\) \}',
    r'fun addHotel(name: String, advancedPayment: Double, phoneNumber: String, location: String, upiId: String, qrCodeUri: String) = viewModelScope.launch { repository.insertHotel(com.example.data.model.HotelEntity(name = name, advancedPayment = advancedPayment, phoneNumber = phoneNumber, location = location, upiId = upiId, qrCodeUri = qrCodeUri)) }',
    content
)

# fix addVirtualAccount
content = re.sub(
    r'fun addVirtualAccount\(account: com\.example\.data\.model\.BankAccountEntity\) = viewModelScope\.launch \{ repository\.insertBankAccount\(account\) \}',
    r'fun addVirtualAccount(name: String, balance: Double, upiId: String, qrCodeUri: String, accountNumber: String, ifsc: String, branchName: String) = viewModelScope.launch { repository.insertBankAccount(com.example.data.model.BankAccountEntity(name = name, balance = balance, upiId = upiId, qrCodeUri = qrCodeUri, accountNumber = accountNumber, ifsc = ifsc, branchName = branchName)) }',
    content
)

# fix addCategory
content = re.sub(
    r'fun addCategory\(category: com\.example\.data\.model\.TransactionCategoryEntity\) = viewModelScope\.launch \{ repository\.insertCategory\(category\) \}',
    r'fun addCategory(name: String, type: String) = viewModelScope.launch { repository.insertCategory(com.example.data.model.TransactionCategoryEntity(name = name, type = type)) }',
    content
)

# fix profiles - wait, what were the parameters for updateProfile?
# Let's check OwnerProfileScreen.kt and SettingsScreen.kt to see how they are called.
