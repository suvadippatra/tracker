#!/bin/bash
sed -i 's/fun AppearanceSettingsScreen(/fun AppearanceSettingsScreen(\n    viewModel: FinancialViewModel,\n    onOpenUtilityConfig: () -> Unit,\n    onBack: () -> Unit = {}\n) {\n    val old = 1/g' app/src/main/java/com/example/ui/screens/settings/subscreens/AppearanceSettingsScreen.kt
