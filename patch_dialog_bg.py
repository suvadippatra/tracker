import re

files = [
    "app/src/main/java/com/example/ui/screens/EditHotelModal.kt",
    "app/src/main/java/com/example/ui/screens/SettingsScreen.kt"
]

for file in files:
    with open(file, 'r') as f:
        content = f.read()
    
    # We want to add window.setBackgroundDrawableResource(android.R.color.transparent)
    # after window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    
    if "window.setBackgroundDrawableResource(android.R.color.transparent)" not in content:
        content = content.replace(
            "window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)",
            "window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)\n                window.setBackgroundDrawableResource(android.R.color.transparent)"
        )
        
        with open(file, 'w') as f:
            f.write(content)
