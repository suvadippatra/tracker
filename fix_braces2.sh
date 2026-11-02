#!/bin/bash
perl -0777 -pi -e 's/\s*\}\s*\}\s*\}\s*\/\/\ 2\. Hotel Management/\n        }\n        \/\/ 2. Hotel Management/s' app/src/main/java/com/example/ui/screens/settings/subscreens/ProfileSettingsScreen.kt
