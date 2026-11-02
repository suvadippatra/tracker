#!/bin/bash
perl -0777 -pi -e 's/        \}\n                \}\n            \}\n        \}\n        \/\/ 2. Hotel Management & Defaults Section/        \/\/ 2. Hotel Management & Defaults Section/g' app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt
