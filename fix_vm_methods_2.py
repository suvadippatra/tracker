import re

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'r') as f:
    content = f.read()

# I will replace these three functions directly
old_methods = """
    // Profiles
    fun updateProfile(profile: com.example.data.model.UserProfileEntity) = viewModelScope.launch { repository.saveUserProfile(profile) }
    fun updateCustomisation(profile: com.example.data.model.UserProfileEntity) = viewModelScope.launch { repository.saveUserProfile(profile) }
    fun updatePreferences(profile: com.example.data.model.UserProfileEntity) = viewModelScope.launch { repository.saveUserProfile(profile) }
"""

new_methods = """
    // Profiles
    fun updateProfile(name: String, contact: String, avatar: String) = viewModelScope.launch {
        val current = repository.getUserProfile() ?: com.example.data.model.UserProfileEntity()
        repository.saveUserProfile(current.copy(name = name, contact = contact, avatar = avatar))
    }
    
    fun updateCustomisation(mealGroupPosition: String, bgPreset: String, customBgUri: String) = viewModelScope.launch {
        val current = repository.getUserProfile() ?: com.example.data.model.UserProfileEntity()
        repository.saveUserProfile(current.copy(mealGroupPosition = mealGroupPosition, bgPreset = bgPreset, customBgUri = customBgUri))
    }
    
    fun updatePreferences(
        language: String,
        themeMode: String,
        showAdvancedPayment: Boolean,
        appLockEnabled: Boolean,
        appLockPin: String,
        biometricEnabled: Boolean,
        securityQuestion: String,
        securityAnswer: String,
        defaultHotelId: Int,
        enableStarAnimation: Boolean,
        textSize: String
    ) = viewModelScope.launch {
        val current = repository.getUserProfile() ?: com.example.data.model.UserProfileEntity()
        repository.saveUserProfile(current.copy(
            language = language,
            themeMode = themeMode,
            showAdvancedPayment = showAdvancedPayment,
            appLockEnabled = appLockEnabled,
            appLockPin = appLockPin,
            biometricEnabled = biometricEnabled,
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer,
            defaultHotelId = defaultHotelId,
            enableStarAnimation = enableStarAnimation,
            textSize = textSize
        ))
    }
"""

if old_methods.strip() in content:
    content = content.replace(old_methods.strip(), new_methods.strip())
else:
    # Try finding them individually
    p1 = r'fun updateProfile\(profile: com\.example\.data\.model\.UserProfileEntity\) = viewModelScope\.launch \{ repository\.saveUserProfile\(profile\) \}'
    c1 = 'fun updateProfile(name: String, contact: String, avatar: String) = viewModelScope.launch { val current = repository.getUserProfile() ?: com.example.data.model.UserProfileEntity(); repository.saveUserProfile(current.copy(name = name, contact = contact, avatar = avatar)) }'
    content = re.sub(p1, c1, content)
    
    p2 = r'fun updateCustomisation\(profile: com\.example\.data\.model\.UserProfileEntity\) = viewModelScope\.launch \{ repository\.saveUserProfile\(profile\) \}'
    c2 = 'fun updateCustomisation(mealGroupPosition: String, bgPreset: String, customBgUri: String) = viewModelScope.launch { val current = repository.getUserProfile() ?: com.example.data.model.UserProfileEntity(); repository.saveUserProfile(current.copy(mealGroupPosition = mealGroupPosition, bgPreset = bgPreset, customBgUri = customBgUri)) }'
    content = re.sub(p2, c2, content)
    
    p3 = r'fun updatePreferences\(profile: com\.example\.data\.model\.UserProfileEntity\) = viewModelScope\.launch \{ repository\.saveUserProfile\(profile\) \}'
    c3 = """fun updatePreferences(
        language: String,
        themeMode: String,
        showAdvancedPayment: Boolean,
        appLockEnabled: Boolean,
        appLockPin: String,
        biometricEnabled: Boolean,
        securityQuestion: String,
        securityAnswer: String,
        defaultHotelId: Int,
        enableStarAnimation: Boolean,
        textSize: String
    ) = viewModelScope.launch {
        val current = repository.getUserProfile() ?: com.example.data.model.UserProfileEntity()
        repository.saveUserProfile(current.copy(
            language = language,
            themeMode = themeMode,
            showAdvancedPayment = showAdvancedPayment,
            appLockEnabled = appLockEnabled,
            appLockPin = appLockPin,
            biometricEnabled = biometricEnabled,
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer,
            defaultHotelId = defaultHotelId,
            enableStarAnimation = enableStarAnimation,
            textSize = textSize
        ))
    }"""
    content = re.sub(p3, c3, content)

with open('app/src/main/java/com/example/ui/viewmodel/FinancialViewModel.kt', 'w') as f:
    f.write(content)
