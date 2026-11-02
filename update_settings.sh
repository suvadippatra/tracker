#!/bin/bash
sed -i 's/"profile" -> ProfileSettingsScreen(viewModel, onOpenUtilityConfig, onBack = { currentScreen = "main" })/"profile" -> ProfileSettingsScreen(viewModel, onOpenOwnerProfile, onBack = { currentScreen = "main" })/g' app/src/main/java/com/example/ui/screens/SettingsScreen.kt
sed -i 's/"appearance" -> AppearanceSettingsScreen(viewModel, onBack = { currentScreen = "main" })/"appearance" -> AppearanceSettingsScreen(viewModel, onOpenUtilityConfig, onBack = { currentScreen = "main" })/g' app/src/main/java/com/example/ui/screens/SettingsScreen.kt
