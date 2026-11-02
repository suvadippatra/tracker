#!/bin/bash
# Remove utility config block from ProfileSettingsScreen.kt
perl -0777 -pi -e 's/\s*PixelSettingsGroup\(title = if \(prefLanguage == "bn"\) "ইউটিলিটি ও মিটার সেটিংস" else "Utility & Billing Configuration"\) \{\s*PixelSettingsItem\(\s*icon = Icons\.Default\.ElectricMeter,\s*title = if \(prefLanguage == "bn"\) "মিটার রিডিং ও বিল কনফিগারেশন" else "Meter Reading & Rates Config",\s*subtitle = if \(prefLanguage == "bn"\) "বিদ্যুৎ ও ইউটিলিটি টেমপ্লেট পরিচালনা করুন" else "Manage electricity rates and billing templates",\s*onClick = onOpenUtilityConfig\s*\)\s*\}\s*//gs' app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt

# Change parameter from onOpenUtilityConfig to onOpenOwnerProfile
sed -i 's/onOpenUtilityConfig: () -> Unit/onOpenOwnerProfile: () -> Unit/' app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt

# Replace clickable action
perl -pi -e 's/\.clickable \{ showEditProfileDialog = true \}/.clickable { onOpenOwnerProfile() }/g' app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt

