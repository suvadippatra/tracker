# Android 8 Performance Optimization, Flat Material 3 Redesign & Manual GitHub APK/AAB Build Workflow

## Overview
To eliminate UI lag and slow response times on Android 8 devices while keeping the familiar layout, navigation, and triple-dot action menus intact, we will transition the app's rendering layer to **flat Material 3 surfaces**, replace heavy 3D cylindrical wheel pickers and canvas charts with **fast lightweight and native Android controls**, introduce a **Disable Heavy Effects & Use Native Controls** toggle in Settings, update the in-app **Help / Application Overview & Guide**, and configure a manual **GitHub Actions compilation workflow** supporting **AAB**, **Universal APK**, and **arm64-v8a APK** outputs.

---

## 1. Complete Inventory of Application Pages & Per-Page Optimizations

### A. Opening Scene & Primary Navigation Pages (5 Core Opening Views)
When launching the app and interacting with the main workspace, the opening scene provides the following primary pages:

1. **App Lock & Biometric / PIN Unlock Page** (plus **Security Question Recovery Reset Modal**)
   - *Current bottleneck*: Full-screen blurred radial glow canvases, spring-animated PIN dots, and heavy glass keypad buttons.
   - *Optimization*: Replace radial canvas glows with a clean, flat Material 3 surface background and instantaneous key press feedback so unlocking is immediate on Android 8.
2. **Hotels Page (Tab 1 — "Hotels" / "হোটেল")**
   - *Current bottleneck*: Multi-stop translucent gradient hotel pager cards with continuous `graphicsLayer` scale/alpha interpolation, glass action buttons, and multi-layered meal history rows.
   - *Optimization*: Keep the exact layout (Hotel Ledger Pager with triple-dot menu, Frequently Ordered Meals strip, Recent Meal Logs list, and Add Meal button) while rendering cards as crisp, opaque Material 3 surfaces with 1dp borders and flat tonal badges.
3. **Extra Expenses Page (Tab 2 — "Extra" / "খরচ")**
   - *Current bottleneck*: Translucent Total Available Balance hero card, horizontal bank accounts pager with per-frame scale effects, and heavy transaction row wrappers.
   - *Optimization*: Preserve the Total Available Balance card (Income/Expense split), Accounts carousel with triple-dot menus, Category filter chips, Recent Transactions list, and Floating Action Button (`+`), using flat opaque Material 3 card colors and zero-overdraw list items.
4. **Bill Tracking — Log Reading Page (Tab 3, View A — "Log Reading" / "রিডিং এন্ট্রি")**
   - *Current bottleneck*: Five simultaneous 3D cylindrical `WheelPicker` columns for the meter reading dial and three cylindrical wheels for the date/time selector, running 8 continuous scroll-physics coroutines at once.
   - *Optimization*: Replace the 5-wheel 3D meter dial with a fast **Lightweight Digit Stepper & Direct Keypad Card**, and replace the 3D date/time wheel with a clean date/time selector that launches Android's native `DatePickerDialog` and `TimePickerDialog` when native controls are enabled.
5. **Bill Tracking — Bill Summary Page (Tab 3, View B — "Bill Summary" / "বিল সারসংক্ষেপ")**
   - *Current bottleneck*: Nested translucent cards for Billing Period Selection, Projected Utility Bill calculation, and Paid/Unpaid bill history cards with inline wheel date pickers.
   - *Optimization*: Convert the Billing Period selector, Projected Bill card, and Generated Bills history list into flat Material 3 cards with fast date selection dialogs.

---

### B. Top Header Bar Pages (Accessible Directly from the Opening Scene)
6. **Global Search Page ("Search everything...")**
   - *Optimization*: Replace translucent filter chips (`All`, `Expenses`, `Meals & Menus`, `Hotels`, `Accounts`, `Bills`), Recent Search badges, and Quick Suggestion cards with flat Material 3 chips and lightweight result rows for instant typing response.
7. **Owner Profile & Personal Bank Accounts Page** (plus **Owner QR Quick Viewer Modal**, **Add/Edit Personal Account Modal**, and **Account Details Popup**)
   - *Optimization*: Flatten the profile summary banner and bank account cards while keeping one-tap copy, share, and QR preview actions intact.
8. **Settings Hub Page**
   - *Optimization*: Render the profile card and settings category rows (`Account & Profile`, `Appearance & Visuals`, `Security & Privacy`, `Data & Backups`, `Recycle Bin`, `About Traker`) on solid Material 3 containers with instant sub-page transitions when heavy effects are disabled.

---

### C. Hotel Module Sub-Pages & Modals
9. **Add / Log Meal Flow Page**
   - *Optimization*: Keep the Day/Night toggle, Date selector, Dish selection grid, Supplementary Add-ons, and bottom Meal Group summary bar, replacing the heavy sliding wheel date modal with a fast calendar/native date picker and flat dish cards.
10. **Menu Management & Analytics Page** (plus **Add/Edit Dish Modal** and **CSV Import/Export**)
    - *Optimization*: Replace heavy multi-layer canvas price/category charts and translucent dish cards with lightweight flat Material 3 progress/bar indicators and fast-scrolling menu item rows.
11. **Meal Record Ledger (Meal History) Page**
    - *Optimization*: Keep the month selector, **List View** vs. **Day-Wise View** toggle, and Monthly Payment Status header badge, rendered with flat Material 3 cards for smooth scrolling through large monthly ledgers.
12. **Hotel Monthly Payment Status Modal**
    - *Optimization*: Replace the inline 3D wheel date picker with a fast lightweight/native date picker while preserving Paid/Unpaid toggles, amount, and notes.
13. **Register / Edit Hotel Modal**, **Hotel Details Info Bottom Sheet**, and **Full-Screen QR Preview Modal**
    - *Optimization*: Simplify form containers and bottom sheet surfaces into solid Material 3 cards while preserving QR image picking, UPI details, and ledger actions.

---

### D. Extra Expenses & Accounts Module Sub-Pages & Modals
14. **Add / Log Transaction Page (Expense & Income)**
    - *Optimization*: Keep the Expense/Income toggle, Amount input, Title, Category chips, Account selector, Date picker, and Attachments bar (Photo, PDF, Voice Note recorder), using flat Material 3 inputs and native/lightweight date selection.
15. **Detailed Expense History Page**
    - *Optimization*: Streamline the month-by-month swipeable ledger, account filter pills, and search bar so large transaction histories scroll at 60 FPS on Android 8.
16. **Account Manager Page** & **Add / Edit Bank Account Modal**
    - *Optimization*: Render bank account cards and balance adjustment forms with flat Material 3 surfaces while preserving triple-dot actions and custom color accents.
17. **Attachment Viewer Modal (Image Viewer, PDF Launcher & Voice Note Player)**
    - *Optimization*: Keep full attachment playback and viewing on a clean, solid full-screen surface.

---

### E. Bill & Utility Module Sub-Pages & Modals
18. **Utility & Meter Rates Configuration Page**
    - *Optimization*: Flatten the Fixed Service Charge card and Tiered/Historical Rate cards, replacing heavy date wheels with fast native/lightweight date pickers when adding new effective rate dates.
19. **Edit Meter Reading Modal** & **Update Bill Payment Status Modal**
    - *Optimization*: Replace 3D wheel pickers inside edit dialogs with lightweight numeric inputs and native date/time pickers.

---

### F. Settings Sub-Pages, Recycle Bin, QR Sync & Help Pages
20. **Account & Profile Settings Page** — Flat Material 3 form inputs for owner name, email, phone, and profile avatar.
21. **Appearance & Visuals Settings Page** — Theme Mode (`Dark`, `Light`, `System`), Accent Color palette, Language (`English` / `বাংলা`), Currency symbol, plus the new **"Disable Heavy Effects & Use Native Controls"** performance switch.
22. **Security & Privacy Settings Page** (plus **PIN Setup Modal** and **Recovery Question Setup Modal**) — Clean flat toggles for App Lock and Biometric unlock.
23. **Data & Backups Settings Page** (plus **Sync Conflict Resolution Page**, **Animated QR Export Stream Page**, and **Camera QR Scanner Page**) — Fast backup/restore controls and full-screen edge-to-edge QR transfer overlays.
24. **Recycle Bin Page** — Fast-switching tabs across deleted `Hotels`, `Accounts`, `Menu`, `Meals`, `Expenses`, and `Readings` with one-tap Restore or Permanent Delete.
25. **About Traker & Help Guide Page** (plus **Application Overview & Guide Modal**, **Privacy Policy Modal**, and **Open Source Licenses Modal**) — Updated in both English and Bengali to document all performance features, native controls, and page optimizations.

---

## 2. Core Technical Changes

### 1. Flat Material 3 Surface Rendering & Overdraw Removal
- **Background & Card Engine**: Replace the multi-layered radial canvas background and multi-stop translucent `Brush.linearGradient` + double-border wrappers with single-pass solid Material 3 surfaces (`surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`) and a clean 1dp solid border.
- **Zero-Allocation Text & Pager Transitions**: Replace per-digit animated `RollingNumberText` with direct single-node `Text` rendering and disable per-frame `graphicsLayer` pager scaling and spring animations when performance mode is enabled.

### 2. Settings Toggle: "Disable Heavy Effects & Use Native Controls"
- Add a persisted `disableHeavyEffects` setting in **Settings → Appearance & Visuals** (automatically defaulting to **ON** on Android 8 / API ≤ 27 devices, and toggleable anytime on any device).
- When enabled:
  - Disables all screen transition animations, spring physics, and continuous infinite animations.
  - Uses native Android `DatePickerDialog` and `TimePickerDialog` for date and time selection.
  - Uses flat Material 3 surfaces across all 25 pages and modals.

### 3. Manual GitHub Actions Workflow for AAB, Universal APK, and arm64-v8a APK
- **Gradle ABI Support**: Configure the build script to support optional ABI filtering (`-PabiTarget=arm64-v8a`) alongside universal APK and Android App Bundle (`.aab`) tasks.
- **Manual Workflow (`workflow_dispatch`)**: Configure the GitHub Actions workflow so you can trigger builds manually from the GitHub **Actions** tab with choices for:
  - **Target Output**: `all` (default — builds all three), `aab`, `universal-apk`, or `arm64-v8a-apk`.
  - **Build Variant**: `release` (signed) or `debug`.
- Uploads clear downloadable artifacts (`traker-release-bundle-aab`, `traker-universal-apk`, `traker-arm64-v8a-apk`) and optionally attaches them to a GitHub Release tag.
