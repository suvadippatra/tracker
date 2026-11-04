# Implementation Plan: Window Resize Stability, Sticky Note Editor Fix, Montserrat Bold & High-Contrast Buttons

## Status: Completed

1. **Window Resizing & Multi-Window Stability (COMPLETED)**:
   - Updated `DialogEdgeToEdge.kt` to dynamically calculate target window dimensions (`getTargetDimensions()`) on every layout pass.
   - Handled Android multi-window/split-screen mode (`activity.isInMultiWindowMode`), using active decor/window dimensions rather than stale physical display metrics.
   - Guarded against infinite layout loops during soft keyboard IME transitions by monitoring window `decorView` dimensions rather than shifting content height.
   - Fixed `dialogSystemBarsPadding` and `dialogSafeBottomPadding` to avoid applying full-screen navigation bar fallbacks in split-screen or resized windows.

2. **Sticky Note In-Memory Draft & Typing Fix (COMPLETED)**:
   - Fixed premature dismissal bug in `StickyNotesTab.kt`: `LaunchedEffect` now checks `if (currentId <= 0 || isCreatingNewNote) return@LaunchedEffect`, preventing newly opened drafts from being dismissed by background Room emissions.
   - Keyed draft editing states (`title`, `subtitle`, `bodyText`, `textAlignment`, `attachments`) by `note.id` only, completely eliminating typing resets, cursor jumping, and crashes while typing.
   - Applied clean save and exit callbacks ensuring notes persist reliably without race conditions.

3. **Dedicated Montserrat Bold Typography (COMPLETED)**:
   - Downloaded and installed `montserrat.ttf` into `app/src/main/res/font/montserrat.ttf` using the `font-util` CLI.
   - Configured `MontserratBoldFontFamily` in `Type.kt` and wired it into `AppBoldFontFamily`. When the "Bold Text" switch is toggled in Appearance settings, the entire application renders with crisp Montserrat Bold typography.

4. **Automated High-Contrast Button Text & Icons (COMPLETED)**:
   - Updated "+ Add New Hotel" button on the empty hotel page in `HotelManagementTab.kt` to use `AppOnAccentColor()`, replacing hardcoded black text that was unreadable on dark accent colors.
   - Updated "Save Hotel" button, "QR Send" and "Confirm" buttons across `HotelManagementTab.kt`, `DataBackupSettingsScreen.kt`, and `SyncDialogs.kt` with dynamic high-contrast colors.
   - Ensured all buttons automatically calculate luminance and show clean White on dark colors and Slate/Black on bright colors.

5. **Updated In-App Help & User Guide Documentation (COMPLETED)**:
   - Updated `AboutSettingsScreen.kt` and `LegalInfoDialog` in `SettingsScreen.kt` in both English and Bengali.
   - Documented the new Montserrat Bold typography, Sticky Note draft reliability, dynamic window resizing/split-screen support, and automated button text contrast.

6. **Build & Test Verification (COMPLETED)**:
   - `compile_applet` passed.
   - `:app:testDebugUnitTest` passed with 0 errors.
   - `assembleDebug` completed and APK updated for emulator preview.
