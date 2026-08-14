import os

base_dir = "app/src/main/java/com/example/ui/screens/settings"
os.makedirs(f"{base_dir}/components", exist_ok=True)
os.makedirs(f"{base_dir}/subscreens", exist_ok=True)

# 1. components/SettingsComponents.kt
with open(f"{base_dir}/components/SettingsComponents.kt", "w") as f:
    f.write("""package com.example.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AppTextColor
import com.example.ui.screens.AccentCyan

@Composable
fun PixelSettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            color = AccentCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppTextColor().copy(alpha = 0.05f))
                .padding(8.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun PixelSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = AppTextColor().copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = AppTextColor(), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, color = AppTextColor().copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
        if (content != null) {
            content()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next",
                tint = AppTextColor().copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
""")

# 2. subscreens/ProfileSettingsScreen.kt
with open(f"{base_dir}/subscreens/ProfileSettingsScreen.kt", "w") as f:
    f.write("""package com.example.ui.screens.settings.subscreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.viewmodel.FinancialViewModel
import com.example.data.model.UserProfileEntity

@Composable
fun ProfileSettingsScreen(viewModel: FinancialViewModel, onOpenUtilityConfig: () -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
        item {
            PixelSettingsGroup("Personal Info") {
                PixelSettingsItem(
                    icon = Icons.Default.Person,
                    title = "Name",
                    subtitle = userProfile?.name ?: "Not set"
                )
                PixelSettingsItem(
                    icon = Icons.Default.Business,
                    title = "Contact",
                    subtitle = userProfile?.contact ?: "Not set"
                )
            }
        }
        item {
            PixelSettingsGroup("Preferences") {
                PixelSettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = if (userProfile?.language == "bn") "Bengali" else "English"
                )
            }
        }
        item {
            PixelSettingsGroup("Utility & Billing") {
                PixelSettingsItem(
                    icon = Icons.Default.Business,
                    title = "Utility Configuration",
                    subtitle = "Manage meter reading templates",
                    onClick = onOpenUtilityConfig
                )
            }
        }
    }
}
""")

# 3. subscreens/AppearanceSettingsScreen.kt
with open(f"{base_dir}/subscreens/AppearanceSettingsScreen.kt", "w") as f:
    f.write("""package com.example.ui.screens.settings.subscreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.screens.AppTextColor

@Composable
fun AppearanceSettingsScreen(viewModel: FinancialViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
        item {
            PixelSettingsGroup("Display") {
                PixelSettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Theme Mode",
                    subtitle = userProfile?.themeMode ?: "Auto"
                )
                PixelSettingsItem(
                    icon = Icons.Default.FormatSize,
                    title = "Text Size",
                    subtitle = userProfile?.textSize ?: "Medium"
                )
            }
        }
        item {
            PixelSettingsGroup("Background") {
                PixelSettingsItem(
                    icon = Icons.Default.Wallpaper,
                    title = "Background Preset",
                    subtitle = userProfile?.bgPreset ?: "Default"
                )
            }
        }
    }
}
""")

# 4. subscreens/SecuritySettingsScreen.kt
with open(f"{base_dir}/subscreens/SecuritySettingsScreen.kt", "w") as f:
    f.write("""package com.example.ui.screens.settings.subscreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.viewmodel.FinancialViewModel

@Composable
fun SecuritySettingsScreen(viewModel: FinancialViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
        item {
            PixelSettingsGroup("App Security") {
                PixelSettingsItem(
                    icon = Icons.Default.Lock,
                    title = "App Lock",
                    subtitle = if (userProfile?.appLockEnabled == true) "Enabled" else "Disabled"
                )
                PixelSettingsItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometrics",
                    subtitle = if (userProfile?.biometricEnabled == true) "Enabled" else "Disabled"
                )
            }
        }
    }
}
""")

# 5. subscreens/AboutSettingsScreen.kt
with open(f"{base_dir}/subscreens/AboutSettingsScreen.kt", "w") as f:
    f.write("""package com.example.ui.screens.settings.subscreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.screens.settings.components.PixelSettingsGroup
import com.example.ui.screens.settings.components.PixelSettingsItem

@Composable
fun AboutSettingsScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
        item {
            PixelSettingsGroup("App Info") {
                PixelSettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0"
                )
                PixelSettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy"
                )
            }
        }
    }
}
""")

print("Files generated!")
