# Implementation Plan: UI Bounds, Bill Calculator, Sticky Notes Keep Upgrade, Granular Exports, Khata OCR Import & System Enhancements

## 1. Tablet & Letterbox Full-Screen Dialog Bounds Fix
- **`DialogEdgeToEdge.kt`**:
  - Fix the tablet/letterboxed overflow glitch where `getRealMetrics()` stretched dialogs across the entire physical display (`2560x1600`) even when the app was letterboxed or in compat/freeform bounds (`isInMultiWindowMode == false`).
  - Prioritize `activity.window.decorView.width` and `activity.window.decorView.height` whenever `decor.width > 0 && decor.height > 0`, as `decorView` reflects the exact active window container dimensions across Android 8–15, tablet letterbox mode, split-screen, and freeform windows.
  - Ensure triple-dot overflow menus and right-aligned action buttons inside Settings and sub-dialogs remain strictly within the app window.

## 2. Electricity Bill Calculator Logic & Hide Default Rate When Policies Exist
- **Hide Default Rate (`UtilityConfigurationScreen.kt`)**:
  - When one or more active rate policies (`rates.isNotEmpty()`) exist, hide the **Default Rate per Unit** input field in Service Parameters (showing only Fixed Service Charge, or only showing Default Rate as an initial fallback when no rate policies are configured).
- **Exact Group-by-Group & Smooth Day-Interpolation Bill Calculation (`MeterReadingViewModel.kt`)**:
  - Strictly calculate consumption between the selected **Start Date** (`startDay`) and **End Date** (`endDay`) without pulling in prior readings before `startDay` (removing the `Case 2` override that shifted `15 Aug (971.0)` back to `27 Jul (934.5)`).
  - Use **day-based linear interpolation** (`dayStart` in days) for dates that fall between two recorded readings:
    - Example: If `15 Aug = 971.0`, `17 Oct = 1091.0`, and `20 Oct = 1094.0`, then for `15 Aug` to `18 Oct`:
      - Complete reading interval `15 Aug → 17 Oct` = `1091.0 - 971.0 = 120.0 kWh`.
      - Incomplete interval `17 Oct → 18 Oct` = `((1094.0 - 1091.0) / 3 days) × (18 - 17) = 1.0 kWh`.
      - Total = `121.0 kWh` (or cumulative interpolated reading `1092.0 - 971.0 = 121.0 kWh`).
  - Group consumption strictly by rate-policy change boundaries (`effectiveDate` cutoffs falling within `[startDay, endDay]`):
    - For uniform rate periods, compute directly from the interpolated reading at the group's start date to the interpolated reading at the group's end date.
    - When a rate policy changes mid-period, split at the exact rate policy `effectiveDate` using day-level linear interpolation and compute each rate group's units and cost separately.

## 3. Sticky Notes Tab: Gesture Differentiation, Keep-Style Plain Editor, 8-Icon Bottom Toolbar, Custom Tags & Wobble Layout
- **Scroll vs. 3-Second Long-Press Gesture Differentiation (`StickyNotesTab.kt`)**:
  - Replace the gesture detector on `StickyFloatingCard` with a pointer input handler that tracks `touchSlop` movement without consuming move events: if the user scrolls/drags beyond `viewConfiguration.touchSlop` before 3,000 ms, cancel the long-press timer immediately so vertical and horizontal page scrolling is completely smooth and uninterrupted.
- **Google Keep-Style Plain Editor & Viewer (No Subtitle, No Inner Cards)**:
  - Remove the **Subtitle** field from the Sticky Note UI and remove the three separate card containers in the editor/viewer.
  - Display **Title** and **Body** seamlessly on a plain Material 3 surface background (like Google Keep), with optional tag chips and media attachments.
- **Single-Line 8-Icon Bottom Formatting Toolbar (Mobile & Tablet Optimized)**:
  - Place all 8 formatting options in a single bottom bar using crisp icons only:
    1. **Bold** (`*text*` — wraps current selection or inserts/toggles `*` markers at cursor)
    2. **Italic** (`-text-` — wraps current selection or inserts/toggles `-` markers at cursor)
    3. **Align Left**
    4. **Align Center**
    5. **Align Right**
    6. **Align Justify**
    7. **Insert Table** (opens interactive grid builder / inserts pipe table)
    8. **Attach File** (attaches image/document)
  - Track text selection (`TextFieldValue` with `TextRange` selection) in the editor so tapping **Bold** or **Italic** wraps the highlighted text or inserts paired markers at the cursor position immediately.
- **Custom Tags & Fixed Right Search/Settings Top Bar**:
  - Add a `tags: String` (comma-separated custom tags) field to `StickyNoteEntity` with migration/default support, allowing users to add/remove custom tags on any Sticky Note.
  - Update the Sticky tab top bar: place a horizontally scrollable filter chip row on the left (`"All"`, `"{tag 1}"`, `"{tag 2}"`, ...) while keeping the **Search** and **Settings** icons fixed on the right.
- **Continuous Wobbling Animation & Alternating / Unequal-Height Wall Arrangement**:
  - Keep the gentle harmonic wobble (`shoPhase` rotation + bobbing) active on Sticky Note cards on the Sticky tab even when `disableHeavyEffects` is enabled for the rest of the app.
  - **Mobile Portrait**: Arrange Sticky cards alternating leftward (`Alignment.Start`) and rightward (`Alignment.End`) down the column so the left/right zig-zag offset naturally reflects the configured card width percentage (`50%–80%`).
  - **Tablet / Landscape Wall Mode**: Render a multi-column staggered wall where adjacent columns start at unequal top vertical offsets (e.g., staggered top spacers per column) so cards never align on a rigid top row while remaining horizontally and vertically scrollable.

## 4. Bold Font (`Archivo Black` & `Archivo`) with Distinct Bold-in-Bold Support
- **Font Installation & Typography (`app/src/main/res/font/`, `Type.kt`, `AppThemeProvider.kt`)**:
  - Install **Archivo Black** (`archivo_black.ttf`) and **Archivo** (`archivo.ttf`) locally via `font-util`.
  - When **Bold Text** mode is enabled in Settings:
    - Apply `Archivo` Bold / `Archivo Black` across the app's typography scale AND provide `CompositionLocal` / style enforcement so composables with explicit `FontWeight.Normal` or `FontWeight.Medium` also render in bold typography.
    - In Sticky Notes (`parseStickyFormattedText`), when Bold Mode is ON, regular note text uses `Archivo` Bold while `*bold*` spans use `Archivo Black` (`FontWeight.Black`) with a subtle size/letter-spacing boost and high-contrast emphasis so `*bold*` formatted spans are unmistakably bolder than normal text even when global Bold Mode is active.

## 5. Granular Daily-Use Exports, Settings Export & Duplicate Removal
- **`DataBackupSettingsScreen.kt` & `SyncDialogs.kt`**:
  - Remove the duplicate Full App Data export card so there is only one unified Full Backup option.
  - Add a dedicated **Settings Only** export/import option (`SETTINGS` scope: exports theme, font settings, sticky width, utility parameters, and profile configuration).
  - Upgrade **Specific Category Exports** for daily use:
    - **Hotel Data**: Choose a specific Hotel (or All Hotels) AND choose the sub-category:
      1. *Whole Hotel* (Info + Menu + Order History)
      2. *Order History Only*
      3. *Menu Items Only*
      4. *Hotel Info Only*
    - **Account / Extra Data**: Choose a specific Bank Account (or All Accounts) AND choose:
      1. *Whole Account* (Account Info + Expense Transactions)
      2. *Transactions History Only*
      3. *Account Info Only*
    - **Electricity Bill Data**: Choose *All Utility Data*, *Meter Readings Only*, *Generated Bills Only*, or *Rate Policies Only*.

## 6. Default Hotel & Default Account Selection + First-Item Tab Ordering
- **`ProfileSettingsScreen.kt`, `HotelManagementTab.kt`, & `ExpenseTrackingTab.kt`**:
  - Fix the Default Hotel selector in Account & Profile (`UserSettingsDialog` / `ProfileSettingsScreen.kt`) so all existing hotels are listed and selectable as Default Hotel, and add a matching **Default Bank Account** selector (`defaultAccountId` in `UserProfileEntity`).
  - Sort the hotels list in `HotelManagementTab` and `HotelViewModel` so the user's **Default Hotel** always appears **first** (index 0) on launch, with remaining hotels accessible by scrolling right.
  - Sort the bank accounts list in `ExpenseTrackingTab` and `ExpenseViewModel` so the user's **Default Bank Account** always appears **first** (index 0) on launch, with remaining accounts accessible by scrolling.

## 7. Recycle Bin Search & Full Deleted Entity Coverage (Including Electricity Rate Policies)
- **`Entities.kt`, `Daos.kt`, `UtilityConfigurationScreen.kt`, & `BinScreen.kt`**:
  - Add `isDeleted: Boolean = false` and `deletedAt: Long? = null` to `ElectricityRate`.
  - When a rate policy is removed from `UtilityConfigurationScreen`, soft-delete it (`isDeleted = true`, `deletedAt = System.currentTimeMillis()`) instead of hard-deleting it.
  - Add `"Bills"` and `"Rates"` tabs to `BinScreen.kt` so deleted Utility Bills and deleted Electricity Rate Policies appear in the Recycle Bin with full **Restore** and **Permanent Delete** functionality.
  - Verify the Recycle Bin search bar filters across all 9 categories (`Hotels`, `Accounts`, `Menu`, `Meals`, `Expenses`, `Readings`, `Bills`, `Rates`, `Sticky`).

## 8. Meal Record Ledger Expressive Icons & Transaction History Two-Mode Toggle
- **Meal Record Ledger (`HotelManagementTab.kt`)**:
  - Replace the `"Day •"` and `"Night •"` text prefixes on meal ledger cards with expressive **Sun** (`Icons.Default.WbSunny`) and **Moon** (`Icons.Default.NightsStay`) icons and spacious, easy-to-scan item formatting.
- **Transaction History Two-Mode Toggle (`ExpenseTrackingTab.kt`)**:
  - Add a clean mode toggle in the Transaction History header/filter bar allowing the user to switch between:
    1. **Keep Each Apart**: Lists every individual transaction record separately in chronological order.
    2. **Merge Daily Transactions**: Groups and merges transactions occurring on the same calendar day into a consolidated daily summary card with expandable item breakdown.

## 9. Menu Item Modification History & Historical Price Integrity
- **`Entities.kt`, `Daos.kt`, `HotelViewModel.kt`, & `HotelManagementTab.kt`**:
  - Create a `MenuModificationHistoryEntity` table (`id`, `menuItemId`, `hotelId`, `itemName`, `oldPrice`, `newPrice`, `oldType`, `newType`, `modifiedAt`) that automatically logs a history entry whenever a menu item's price, name, or meal type is edited.
  - Add a **"Modification History"** option/button on menu items in the Menu Management screen to view the chronological timeline of price and detail changes.
  - Ensure historical `MealTransaction` records strictly preserve their recorded `totalCost` and `itemsSummary` from the time of ordering and are never retroactively altered when a menu item's price is updated.

## 10. "Scan Khata to Import" (Handwritten Khata OCR & Menu Combination Matcher)
- **`KhataScannerDialog.kt` & `DataBackupSettingsScreen.kt`**:
  - Add a **"Scan Khata to Import"** card in Data & Backups (and Hotel Menu/Ledger import actions) supporting both **Camera Capture** and **Gallery Image Picker**.
  - **Safe Downsampling & Native OCR Pipeline**:
    - Decode and downsample high-resolution images on `Dispatchers.IO` (capping max dimension to `1600px` with `inSampleSize`) so large photos never cause OOM or UI freezes.
    - Process the image using on-device text recognition / handwriting line extraction with graceful error handling (if native recognition fails or finds no text, display a clear message and allow immediate manual line entry or retry without hanging).
  - **Step 1 — Split-Screen Side-by-Side Verification**:
    - Display the captured Khata photo on one half and the editable recognized text lines (`dd/mm/yy -> price1 + price2`) on the other half so the user can visually compare and edit any line before matching.
  - **Step 2 — Date Collision Check & Separate Price-to-Menu Combination Matcher**:
    - Parse each line into a **Date** (highlighting dates that already have existing meal entries in the database) and **separate Dish Prices** separated by `+` (e.g. `60+40` is evaluated as a `₹60` dish entry and a `₹40` dish entry).
    - For each price value (e.g., `60`, then `40`), compute all valid combinations of the selected hotel's menu items that sum to that exact price:
      - **1 exact combination**: Auto-select the menu item/combination.
      - **2 to 4 combinations**: Display the 2–4 guessed combinations as interactive chips for one-tap selection.
      - **More than 4 combinations (or 0 matches)**: Provide a direct menu item picker/selector so the user can choose the exact dish(es).
    - Confirming imports all matched entries directly into the selected hotel's meal ledger for the respective dates.

## 11. GitHub Privacy Audit, Editable App Version & Public Knowledge/Repos Reference List
- **Zero Personal Data / Secret Exposure Audit**:
  - Audit all source and configuration files to verify no personal phone numbers, UPI IDs, emails, or API keys exist in tracked files.
- **Centralized Editable App Version (`UserProfileEntity`, `AboutSettingsScreen.kt`, `SettingsScreen.kt`)**:
  - Add `appVersion: String = "2.4.0"` to `UserProfileEntity` and provide an edit action in the About page so changing the version in one place automatically updates the Settings footer, About page badge, and backup metadata.
- **Public Knowledge, Open-Source Repositories & Databases Reference List**:
  - Add a dedicated **"Open-Source Repositories, Libraries & Public Knowledge References"** section in `AboutSettingsScreen.kt` (and `LegalInfoDialog`) listing Android Jetpack Compose, Material Design 3, Room SQLite Database, ZXing QR Engine, Google Fonts (`Archivo Black`, `Montserrat`), and public mathematical/interpolation models used in the app.
- **Bilingual Help & User Guide Update**:
  - Update the built-in Help & User Guide (both English and বাংলা) in `AboutSettingsScreen.kt` and `SettingsScreen.kt` to document every new feature and improvement.
