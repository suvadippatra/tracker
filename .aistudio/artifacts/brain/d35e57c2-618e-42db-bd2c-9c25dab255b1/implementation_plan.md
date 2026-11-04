# Implementation Plan: Android 8 Full-Screen Fix, Sticky Wall Tab, Profile Account Sync & UI Overhaul

## 1. Android 8 Full-Screen Bottom Crop Fix & Frame-Drop Optimization
- **Root Cause of Android 8 Bottom Crop**:
  - In Jetpack Compose's internal `DialogLayout.internalOnMeasure`, setting `usePlatformDefaultWidth = false` causes `DialogLayout` to ignore the window's `EXACTLY realHeight` measure spec and clamp its child `AndroidComposeView` to `configuration.screenHeightDp * density`. On Android 8 (API 26–27), `screenHeightDp` excludes the status bar and navigation bar (~72dp), causing the dialog surface to stop above the bottom of the screen and expose the underlying view with a white/rounded border.
- **Solution in `DialogEdgeToEdge.kt`**:
  - Keep `DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)` and `com.example.ui.components.DialogEdgeToEdge()` on full-screen overlays as required by project rules.
  - Inside `DialogEdgeToEdge()`, after setting `window.setLayout(realWidth, realHeight)`, `window.setBackgroundDrawable(ColorDrawable(bgColor))`, and zeroing all padding/insets up the view tree (`decorView`, `android.R.id.content`, `DialogLayout`, `AndroidComposeView`), update the `DialogLayout` measurement behavior (synchronizing `configuration.screenHeightDp`/`screenWidthDp` to physical `realMetrics` dimensions and enabling exact measure pass-through on `DialogLayout`) so `AndroidComposeView` always measures and renders at `100%` of `realMetrics.heightPixels` on Android 8–10.
- **Frame-Drop & Lag Optimization (All Android Versions)**:
  - Eliminate per-frame `OnGlobalLayoutListener` re-layout loops in `DialogEdgeToEdge.kt` by only applying window layout changes when dimensions or paddings actually differ.
  - Cache expensive date formatting, regex parsing, and list sorting using `remember` and `derivedStateOf` across inner screens (`HotelMealHistoryDialog`, `AddMealScreen`, `SettingsScreen`, `MenuEditorScreen`, `ExtraExpensesTab`).
  - Optimize native shadow modifiers inside scrolling `LazyColumn`s to avoid redundant offscreen hardware layer allocations during scroll.

---

## 2. Call & Contact Popups, Short Notes, and Unified Hotel/Account Info UI
- **Database Schema Updates (`AppDatabase` Migration 34 → 35)**:
  - `HotelEntity`: Add `notes: String = ""`, `whatsappNumber: String = ""`.
  - `BankAccountEntity` & `OwnerAccountEntity`: Add `notes: String = ""`, `phoneNumber: String = ""`, `whatsappNumber: String = ""`, `email: String = ""`.
- **Hotel Call Icon Popup**:
  - Clicking the Call icon on a Hotel opens a clean M3 popup offering **Phone Call** (`ACTION_DIAL`) and **WhatsApp Chat** (`https://wa.me/...`).
  - Detect Wi-Fi-only tablets (`!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)` or `TelephonyManager.PHONE_TYPE_NONE`) and automatically hide the Phone Call button, showing **WhatsApp Only**.
- **Account Contact Icon Popup & Editor Fields**:
  - Add editable **Phone / SMS Number**, **WhatsApp Number**, **Email Address**, and **Short Notes** fields to the Add/Edit Account screen (`AccountEditorScreen.kt`).
  - Clicking the Contact action on an Account opens a popup with **Mail**, **WhatsApp**, and **Text Message (SMS)** options.
- **Short Notes on Hotels & Accounts**:
  - Add a **Short Notes** field in `EditHotelScreen.kt` and `AccountEditorScreen.kt`.
  - Display the saved note inside both the Hotel Info popup/panel and Account Info popup/panel.
- **Unify Hotel & Account Info Cards and Popups**:
  - Redesign the **Account Info Card** in `ExtraExpensesTab.kt` to match the visual structure and styling of the **Hotel Info Card** in `HotelManagementTab.kt`.
  - Redesign the **Hotel Info Popup** to match the clean layout and action structure of the **Account Info Popup**.
  - Increase the font size of the ongoing month's Hotel bill amount on the Hotel Info Card for better prominence.
  - Fix the right-edge clipping of the triple-dot (`⋮`) `DropdownMenu` ("Move to Trash") in `EditHotelScreen.kt` and across all screens by anchoring the `DropdownMenu`inside the action button's `Box` container.

---

## 3. Typography: Bold Text Toggle & Text Size Control Fix
- **Bundled Bold Font & Typography Scaling (`Type.kt`, `Theme.kt`, `TrakerApp.kt`)**:
  - Use `font-util` to ensure complete font weight support and add `isBoldTextEnabled: Boolean = false` to `UserProfileEntity`.
  - Add a **Bold Text** toggle switch in **Settings → Appearance & Visual Style**. When enabled, upgrade all `Typography` styles and `ProvideTextStyle` weights across the app (e.g., `Normal` → `SemiBold`/`Bold`, `Medium` → `Bold`, `Bold` → `ExtraBold`).
  - Fix **Text Size Control** (`Small`, `Medium`, `Large`, `Extra Large`) by dynamically scaling both `MaterialTheme.typography` font sizes/line heights and `LocalDensity` `fontScale` so every text element (including hardcoded `sp` sizes and M3 typography tokens) scales proportionally.

---

## 4. Tablet Layout Optimizations
- **Homepage Tabs on Tablet (`HotelManagementTab.kt` & `ExtraExpensesTab.kt`)**:
  - On tablet/expanded layouts (`screenWidthDp >= 600` or landscape split view), populate the left column of the **Hotel** tab with the full **Hotel Information & Quick Summary Panel** (including notes, contact actions, ongoing month total, advance balance, and UPI/QR access) instead of leaving blank space.
  - Similarly, populate the left column of the **Extra** tab with the **Account Information & Summary Panel** (notes, contact actions, initial vs. current balance, bank/IFSC details, and QR code).
- **Tablet Settings & Sidebar**:
  - Optimize `SettingsScreen.kt` for tablets using a responsive two-pane or wide-card layout and ensure the top-left navigation rail uses the standard triple-line (`☰`) menu button consistently.

---

## 5. Meal Record Ledger Cleanup & Paid Status Bug Fixes
- **Single "Adding One by One" Mode (`HotelMealHistoryDialog.kt`)**:
  - Remove the mode toggle (`Summed View` vs. `Adding One by One`) and permanently use the **Adding One by One** expression view (`140 + 50 = ₹190`).
  - Re-architect the top header and month navigation bar so the top-left `<` Back button and the `< Month >` selector are clearly separated with generous spacing, preventing accidental touches.
  - Format meal group labels as `"1."`, `"2."`, etc. instead of `"Group 1"`, `"Group 2"`.
- **Paid Status Popup Layout Fix**:
  - Fix the `Payment Status & Date` dialog so the `"Save Status"` button has `minHeight = 48.dp`, `singleLine = true`, and balanced horizontal weight so the text never wraps or clips vertically.
- **Paid Status Backup & Restore Fix (`SyncEngine.kt` & `FinancialViewModel.kt`)**:
  - Include `hotelPayments` (`HotelPaymentEntity` records containing monthly Paid/Unpaid status and payment dates) in the JSON export payload, QR split chunks, and `SyncEngine` import/merge flow so monthly paid statuses are 100% preserved across backups and restores.

---

## 6. New 4th Homepage Tab: "Sticky" (Floating Sticky Notes & Wall Layout)
- **Navigation & Top Header (`MainAppLayout.kt`)**:
  - Add **`Sticky`** as the 4th homepage tab alongside `Hotel`, `Extra`, and `Bill`.
  - **Mobile Bottom Navigation**: Display the tab **Icon above the Text** (WhatsApp-style vertical tab layout) so all 4 tabs fit cleanly with zero text truncation.
  - **Sticky Tab Top Header**: Display only the **Search** (`🔍`) and **Settings** (`⚙️`) action icons in the top right (hide the Greeting text on the Sticky tab).
  - **Universal Search (`GlobalSearchScreen.kt`)**: Index and search all Sticky cards by title, subtitle, formatted body text, and table contents.
- **Adaptive Layout & Settings Controls**:
  - **Settings Sliders (`AppearanceSettingsScreen.kt`)**:
    - **Sticky Card Width Slider**: Configurable up to **80%** of screen width (e.g., `50%` to `80%`).
    - **Sticky Preview Lines Slider**: Configurable from **4 to 10 lines** (default **10 lines**). Both normal text lines and table rows count toward this line limit.
  - **Portrait Mobile vs. Tablet / Landscape / Foldable Wall Layout**:
    - On portrait mobile: Smooth vertically scrollable column of floating rounded Material 3 cards with width controlled by the Settings percentage slider (up to 80%).
    - On tablet, foldable, or mobile landscape (where horizontal UI width ≥ vertical card height): Arrange cards across the rectangular canvas like **sticky notes on a wall** (multi-row/masonry wall grid) that is **horizontally and vertically scrollable**.
  - **Simple Harmonic Oscillator (SHO) Floating Motion**:
    - Apply a lightweight, phase-offset sinusoidal `graphicsLayer { translationY = ...; rotationZ = ... }` animation (Simple Harmonic Oscillator) to each card so cards gently bob like floating notes on water with zero layout recomposition or frame drops.
  - **3-Second Long-Press Drag-to-Rearrange**:
    - Holding a card for **3 seconds** activates single-card drag-and-drop mode with haptic/visual elevation feedback, allowing the user to rearrange its position on the wall/list and persisting `sortOrder` in Room.
- **Card Content, Rich Text & Interactive Tables (`StickyNoteEntity` & `StickyNoteScreen.kt`)**:
  - **Structure**: Larger **Title**, smaller **Subtitle**, **horizontal divider line**, and body text (normal app font size).
  - **Preview Visibility Rule**: If Title or Subtitle is blank, omit them (and any redundant divider) in Preview mode, but always show their input fields in Edit mode.Preview mode displays text and tables only (up to the 4–10 line limit) and hides attachments.
  - **Rich Text Formatting**:
    - Inline `*bold*` and `-italic-` parser & toolbar buttons.
    - Paragraph alignment controls: **Left**, **Center**, **Right**, and **Justify**.
    - **Interactive Table Builder + Pipe Syntax**: Support both an interactive row/column grid builder dialog/toolbar AND markdown pipe syntax (`| Col 1 | Col 2 |`) rendered as clean Material 3 bordered table cells.
- **Full-Screen Sticky Viewer / Editor & File Attachments**:
  - Clicking a card opens its full-screen rich viewer; clicking the `+` FAB opens the editor for a new card.
  - **Top-Right Triple-Dot (`⋮`) Menu**: Provides **Edit**, **Remove** (moves to Recycle Bin/Trash with full restore & permanent delete support in `BinScreen.kt`), and **Attach File**.
  - **Attachment Rules & Storage**:
    - **Images**: Stored in internal app storage. If ≤ 1 MB, store directly; if > 1 MB and ≤ 10 MB, automatically downscale/compress JPEG quality to fit under **1 MB**; if > 10 MB, block import and display a clear `"File exceeds 10 MB limit"` message without crashing.
    - **PDFs**: Strictly enforce a **1 MB** maximum size (show clear error if > 1 MB; no PDF compression).
  - **Attachment Grid & Actions**:
    - Displayed at the bottom of the full-screen card view as compact rounded-corner boxes (`2 columns` on mobile, `3–4 columns` on tablet) with the file name up to **2 lines**.
    - **Single Click**: Open file in viewer (`FileProvider` `Intent.ACTION_VIEW` / in-app image preview).
    - **Long Press**: Open action menu with **Rename**, **Remove**, **Download** (save to device `Downloads`), and **Share** (`Intent.ACTION_SEND`).

---

## 7. Add Transaction Page: Image, PDF & 2-Minute Voice Recording (`AddTransactionScreen.kt`)
- **Image Attachment**: Automatically compress images up to **2 MB** (with a >10 MB safety guard).
- **PDF Attachment**: Allow attaching PDF documents with **no file size limit** (copied safely via streamed I/O into app storage).
- **Voice Message Recorder (Up to 2 Minutes)**:
  - Add `RECORD_AUDIO` permission to `AndroidManifest.xml` and request it at runtime via `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`.
  - Implement a built-in voice recorder using `MediaRecorder` (`MPEG_4` / `AAC` low-bitrate high-compression `.m4a` audio) with a live elapsed timer, **2:00 auto-stop limit**, and built-in `MediaPlayer` playback/delete controls inside the transaction details & attachment list.

---

## 8. Owner Profile Overhaul & Linked vs. Sandboxed Accounts (`OwnerProfileScreen.kt`)
- **Personal Information Card Cleanup**:
  - Remove the `Currency: ₹` badge and remove the top-right Edit pencil icon from the Personal Information card (as highlighted in the user's screenshot).
- **Profile Accounts vs. Extras Accounts Architecture**:
  - Update `OwnerAccountEntity` with `linkedBankAccountId: Int? = null`.
  - When adding an account in the Profile screen, offer two choices:
    1. **Link an Existing Account from Extras**: Creates a two-way synced link with the selected `BankAccountEntity` on the Extras tab (edits to name, UPI ID, QR code, account number, IFSC, branch, contact info, or notes in either place automatically sync to the other).
    2. **Create a Sandboxed Profile Account**: Creates an independent personal account stored only in Profile (`linkedBankAccountId = null`). Users can later link a sandboxed account to the Extras page or de-link an already linked account.
  - **De-Linking Flow**:
    - When de-linking an account that is linked to an Extras account with transaction history, prompt the user with a dialog offering:
      1. **Move Extras Account & History to Recycle Bin**
      2. **Download Transaction History (CSV/JSON)**
      3. **Export via App Export Tool** (noting that media attachments are excluded for compact export size)
      - Keep the personal account details intact in Profile while handling the Extras account + transaction history according to the user's choice.
- **Double-Tap Greeting UPI QR Code**:
  - Ensure double-tapping the Greeting header on the home screen opens `OwnerQRViewerDialog` bound specifically to the **Default Account set on the Profile page** (`userProfile.defaultOwnerAccountId`).

---

## 9. Built-In Help & User Guide Update (`AboutSettingsScreen.kt`)
- Update the Help & User Guide inside the **About & Help** page to thoroughly document:
  - The new **Sticky** tab, wall layout on tablets/landscape, SHO floating animation, 3-second long-press drag reordering, `*bold*` / `-italic-` / alignment / table formatting, and 1 MB image/PDF attachment rules.
  - Transaction attachments (2 MB auto-compressed images, unlimited PDFs, and 2-minute compressed voice notes).
  - Profile linked vs. sandboxed accounts and de-link history options.
  - Hotel & Account call/WhatsApp/SMS/email contact popups and short notes.
  - Single-mode Meal Record Ledger (`Adding One by One`) and Paid Status backup support.
  - Bold Text mode and dynamic Text Size scaling.
