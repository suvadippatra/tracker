with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "r") as f:
    content = f.read()

old_func = """    fun updatePreferences(
        language: String,
        themeMode: String,
        showAdvancedPayment: Boolean,
        appLockEnabled: Boolean,
        appLockPin: String,
        defaultHotelId: Int
    ) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(
                current.copy(
                    language = language,
                    themeMode = themeMode,
                    showAdvancedPayment = showAdvancedPayment,
                    appLockEnabled = appLockEnabled,
                    appLockPin = appLockPin,
                    defaultHotelId = defaultHotelId
                )
            )
        }
    }"""

new_func = """    fun updatePreferences(
        language: String,
        themeMode: String,
        showAdvancedPayment: Boolean,
        appLockEnabled: Boolean,
        appLockPin: String,
        defaultHotelId: Int,
        enableStarAnimation: Boolean
    ) {
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                repository.saveUserProfile(
                    current.copy(
                        language = language,
                        themeMode = themeMode,
                        showAdvancedPayment = showAdvancedPayment,
                        appLockEnabled = appLockEnabled,
                        appLockPin = appLockPin,
                        defaultHotelId = defaultHotelId,
                        enableStarAnimation = enableStarAnimation
                    )
                )
            }
        }
    }"""

content = content.replace(old_func, new_func)

with open("app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt", "w") as f:
    f.write(content)
