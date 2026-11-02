#!/bin/bash
perl -0777 -pi -e 's/        \}\n    \}\n        item \{/        item {/s' app/src/main/java/com/example/ui/screens/settings/subscreens/AppearanceSettingsScreen.kt
perl -0777 -pi -e 's/            \}\n        \}\n    \}\n\}/            \}\n        \}\n    \}\n\}\n\}/s' app/src/main/java/com/example/ui/screens/settings/subscreens/AppearanceSettingsScreen.kt
