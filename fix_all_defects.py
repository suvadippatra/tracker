import re

def fix_main_app_layout():
    with open("app/src/main/java/com/example/ui/screens/MainAppLayout.kt", "r") as f:
        content = f.read()
    
    content = content.replace(
        "enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),",
        "enter = androidx.compose.animation.fadeIn(animationSpec = tween(300)),"
    ).replace(
        "exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))",
        "exit = androidx.compose.animation.fadeOut(animationSpec = tween(300))"
    )
    
    with open("app/src/main/java/com/example/ui/screens/MainAppLayout.kt", "w") as f:
        f.write(content)

def fix_profile_settings_screen():
    with open("app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt", "r") as f:
        content = f.read()

    # 1. EditProfileModal Dialog background and padding
    # Remove DialogEdgeToEdge and change background to getDialogBackground()
    content = content.replace(
        "usePlatformDefaultWidth = false,\n            decorFitsSystemWindows = false",
        "usePlatformDefaultWidth = true,\n            decorFitsSystemWindows = true"
    )
    content = content.replace(
        "com.example.ui.components.DialogEdgeToEdge()\n",
        ""
    )
    content = content.replace(
        ".background(com.example.ui.components.getAppBackgroundGradient())",
        ".background(com.example.ui.components.getDialogBackground())"
    ).replace(
        ".background(com.example.ui.components.getAppBackgroundGradient(themeMode, bgPreset))",
        ".background(com.example.ui.components.getDialogBackground())"
    )
    content = content.replace(
        ".statusBarsPadding()",
        ""
    )
    # Give the surface some nice rounded corners and padding so it looks like a dialog
    content = content.replace(
        "Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent)",
        "Surface(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 24.dp), shape = RoundedCornerShape(24.dp), color = Color.Transparent)"
    )

    # 2. Fix the Edit Profile Button in the Profile Card
    # It was inside the Column by mistake.
    bad_card_snippet = """                            }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.size(36.dp).background(getGlassIconButtonBg(), CircleShape).border(1.dp, getGlassBorder(), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = AccentCyan, modifier = Modifier.size(18.dp))
                        }
                    }"""
    good_card_snippet = """                            }
                        } // Closes the Column here
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.size(36.dp).background(getGlassIconButtonBg(), CircleShape).border(1.dp, getGlassBorder(), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = AccentCyan, modifier = Modifier.size(18.dp))
                        }
                    }"""
    content = content.replace(bad_card_snippet, good_card_snippet)

    with open("app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt", "w") as f:
        f.write(content)

def fix_edit_hotel_modal():
    with open("app/src/main/java/com/example/ui/screens/EditHotelModal.kt", "r") as f:
        content = f.read()

    content = content.replace(
        ".background(com.example.ui.components.getAppBackgroundGradient(themeMode, bgPreset))",
        ".background(com.example.ui.components.getDialogBackground())"
    )
    content = content.replace(
        ".statusBarsPadding()",
        ""
    )

    with open("app/src/main/java/com/example/ui/screens/EditHotelModal.kt", "w") as f:
        f.write(content)

def fix_hotel_management_tab():
    with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "r") as f:
        content = f.read()

    # Replace FullScreenOverlay with Dialog
    overlay_start = """    com.example.ui.components.FullScreenOverlay(
        viewModel = viewModel,
        onDismiss = onDismiss
    ) {"""
    dialog_start = """    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = true
        )
    ) {"""
    content = content.replace(overlay_start, dialog_start)

    with open("app/src/main/java/com/example/ui/screens/HotelManagementTab.kt", "w") as f:
        f.write(content)

def fix_qr_export_dialog():
    with open("app/src/main/java/com/example/ui/components/QrExportDialog.kt", "r") as f:
        content = f.read()

    # Remove the aspectRatio from portrait
    content = content.replace(
        ".aspectRatio(1f)\n                                        .clip(RoundedCornerShape(18.dp))",
        ".clip(RoundedCornerShape(18.dp))"
    )
    # Let's also check if weight is appropriate in both places (it is).
    
    with open("app/src/main/java/com/example/ui/components/QrExportDialog.kt", "w") as f:
        f.write(content)

fix_main_app_layout()
fix_profile_settings_screen()
fix_edit_hotel_modal()
fix_hotel_management_tab()
fix_qr_export_dialog()

print("Patches applied.")
