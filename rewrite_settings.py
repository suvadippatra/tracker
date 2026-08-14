import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Extract everything from `UnifiedAccountDialog` to the end of the file.
match = re.search(r'(@Composable\s+fun UnifiedAccountDialog.*)', content, re.DOTALL)
if match:
    preserved_dialogs = match.group(1)
else:
    preserved_dialogs = ""

new_imports = """package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.screens.settings.components.PixelSettingsItem
import com.example.ui.screens.settings.subscreens.ProfileSettingsScreen
import com.example.ui.screens.settings.subscreens.AppearanceSettingsScreen
import com.example.ui.screens.settings.subscreens.SecuritySettingsScreen
import com.example.ui.screens.settings.subscreens.DataBackupSettingsScreen
import com.example.ui.screens.settings.subscreens.AboutSettingsScreen
import kotlinx.coroutines.delay
import com.example.data.model.UserProfileEntity
import com.example.ui.components.GlassCard
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

enum class SecurityPinMode { SET_NEW, VERIFY, REMOVE }
"""

settings_dialog_code = """
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsDialog(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit,
    onOpenBin: () -> Unit,
    onOpenOwnerProfile: () -> Unit,
    onOpenUtilityConfig: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("main") }
    var contentVisible by remember { mutableStateOf(false) }

    // Option A: Deferred Loading Animation. The empty scaffold appears first, content fades in shortly after.
    LaunchedEffect(Unit) {
        delay(150) // wait for the dialog/scaffold animation to finish
        contentVisible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF1E1E1E)) // Dark theme default bg
            ) {
                // Header (Instantly visible)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentScreen != "main") {
                        IconButton(onClick = { currentScreen = "main" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when(currentScreen) {
                            "main" -> "Settings"
                            "profile" -> "Account & Profile"
                            "appearance" -> "Appearance"
                            "security" -> "Security"
                            "data" -> "Data & Backup"
                            "about" -> "About"
                            else -> "Settings"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Content (Fades in)
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut()
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            slideInHorizontally { width -> if (targetState == "main") -width else width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> if (targetState == "main") width else -width } + fadeOut()
                        },
                        label = "SettingsNav"
                    ) { screen ->
                        when (screen) {
                            "main" -> SettingsMainList(
                                onNavigate = { currentScreen = it },
                                onOpenBin = onOpenBin,
                                onOpenOwnerProfile = onOpenOwnerProfile
                            )
                            "profile" -> ProfileSettingsScreen(viewModel, onOpenUtilityConfig)
                            "appearance" -> AppearanceSettingsScreen(viewModel)
                            "security" -> SecuritySettingsScreen(viewModel)
                            "data" -> DataBackupSettingsScreen(viewModel)
                            "about" -> AboutSettingsScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsMainList(
    onNavigate: (String) -> Unit,
    onOpenBin: () -> Unit,
    onOpenOwnerProfile: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            PixelSettingsItem(
                icon = Icons.Default.Person,
                title = "Account & Profile",
                subtitle = "Manage owner details and utility config",
                onClick = { onNavigate("profile") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            PixelSettingsItem(
                icon = Icons.Default.Palette,
                title = "Appearance & Customization",
                subtitle = "Theme, background, and text size",
                onClick = { onNavigate("appearance") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            PixelSettingsItem(
                icon = Icons.Default.Security,
                title = "Security & Privacy",
                subtitle = "App Lock and Biometrics",
                onClick = { onNavigate("security") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            PixelSettingsItem(
                icon = Icons.Default.Storage,
                title = "Data & Backups",
                subtitle = "Export, Import, CSV management",
                onClick = { onNavigate("data") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            PixelSettingsItem(
                icon = Icons.Default.DeleteSweep,
                title = "Recycle Bin",
                subtitle = "Restore deleted items",
                onClick = onOpenBin
            )
            Spacer(modifier = Modifier.height(8.dp))
            PixelSettingsItem(
                icon = Icons.Default.Info,
                title = "About & Legal",
                subtitle = "Version, Privacy Policy",
                onClick = { onNavigate("about") }
            )
        }
    }
}
"""

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(new_imports + "\n" + settings_dialog_code + "\n" + preserved_dialogs)

print("SettingsScreen.kt rewritten successfully!")
