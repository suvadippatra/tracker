# Project Rules & Custom Instructions

## Persistent Rules (MANDATORY)
- **App Logo & Assets**: Never create a logo yourself; the user will provide the logo design. Do NOT touch the logo file (`app_logo.png`, `app_logo_512px.png`, etc.) and the logo-related XML files (`ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `ic_launcher.xml`, etc.) unless the user explicitly tells you to do so.
- **App Package Name**: Do NOT change the app package name (`applicationId` / namespace) which the user has set.
- **Edge-to-Edge Overlays & Popups (CRITICAL)**: To create full-screen overlays (e.g., QR scanners/exporters, Account forms), use `androidx.compose.ui.window.Dialog`. However, `Dialog` on older Android versions (like Android 8) naturally creates a secondary OS-level window that restricts window height. Note: Add/Edit pages like "Add Hotel" should NOT be full-screen, they should use standard dialogs with padding.
  - **What to do**: 
    1. Apply `usePlatformDefaultWidth = false` AND `decorFitsSystemWindows = false` inside `DialogProperties`.
    2. Call `com.example.ui.components.DialogEdgeToEdge()` inside the `Dialog`'s content block. `DialogEdgeToEdge` dynamically calculates `getRealMetrics()` physical screen dimensions and forces `width` and `height` across the window and parent view tree, ensuring the background gradient stretches 100% edge-to-edge with no gaps or transparent holes at the bottom on any Android version.
  - **Example Implementation**:
    ```kotlin
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.DialogEdgeToEdge()
        // Overlay Content Here
    }
    ```
