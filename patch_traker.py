import re

path = "app/src/main/java/com/example/ui/screens/TrakerApp.kt"
with open(path, "r") as f:
    content = f.read()

replacement = """                    MainAppLayout(viewModel)
                    
                    // Root Overlay Architecture (Layer 2)
                    val rootOverlay by viewModel.rootOverlayState.collectAsStateWithLifecycle()
                    androidx.compose.animation.AnimatedVisibility(
                        visible = rootOverlay != com.example.ui.viewmodel.RootOverlayState.None,
                        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(150)),
                        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (val state = rootOverlay) {
                                is com.example.ui.viewmodel.RootOverlayState.AddMeal -> {
                                    AddMealFlowModal(
                                        viewModel = viewModel,
                                        onDismiss = { viewModel.closeRootOverlay() }
                                    )
                                }
                                is com.example.ui.viewmodel.RootOverlayState.ManageMenu -> {
                                    MenuManagementScreen(
                                        hotel = state.hotel,
                                        viewModel = viewModel,
                                        onBack = { viewModel.closeRootOverlay() }
                                    )
                                }
                                is com.example.ui.viewmodel.RootOverlayState.EditHotel -> {
                                    EditHotelModal(
                                        hotel = state.hotel,
                                        viewModel = viewModel,
                                        onDismiss = { viewModel.closeRootOverlay() },
                                        onManageMenu = { viewModel.showRootOverlay(com.example.ui.viewmodel.RootOverlayState.ManageMenu(it)) }
                                    )
                                }
                                is com.example.ui.viewmodel.RootOverlayState.OwnerProfile -> {
                                    OwnerProfileScreen(
                                        viewModel = viewModel,
                                        onDismiss = { viewModel.closeRootOverlay() }
                                    )
                                }
                                is com.example.ui.viewmodel.RootOverlayState.AddOwnerAccount -> {
                                    AddOwnerAccountDialog(
                                        viewModel = viewModel,
                                        account = state.account,
                                        onDismiss = { viewModel.closeRootOverlay() },
                                        onSave = { acc, bal, details -> 
                                            // Handle saving here if needed, or inside the component
                                        }
                                    )
                                }
                                is com.example.ui.viewmodel.RootOverlayState.EditBankAccount -> {
                                    UnifiedAccountDialog(
                                        account = state.account,
                                        prefLanguage = userProfile.language,
                                        accountsCount = 0, // Mock count or fetch from VM if needed inside
                                        viewModel = viewModel,
                                        onDismiss = { viewModel.closeRootOverlay() },
                                        onSave = { name, bal, details, upi, qr, accNum, ifsc, branch ->
                                            // Handled internally in SettingsScreen typically, wait, let's keep UnifiedAccountDialog simple
                                        }
                                    )
                                }
                                else -> {}
                            }
                        }
                    }"""

content = content.replace("MainAppLayout(viewModel)", replacement)
with open(path, "w") as f:
    f.write(content)
print("Patched TrakerApp.kt")
