#!/bin/bash
perl -0777 -pi -e 's/(        \}\n    \}\n\})/$1\n\n\@Composable\nfun UtilityConfigAdded() {}\n/s' app/src/main/java/com/example/ui/screens/settings/subscreens/AppearanceSettingsScreen.kt
