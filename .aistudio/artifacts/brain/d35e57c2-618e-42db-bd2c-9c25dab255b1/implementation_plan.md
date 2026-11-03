# Traker — Billing Interpolation, Material 3 Light Mode Polish, Android 8–10 Edge-to-Edge Fix, History Ledgers & Normalized QR/Data Export Plan

This plan details the architecture and UX upgrades to resolve inactive/interpolated electricity billing math (with unit tests), eliminate Light Mode shadow/border artifacts, fix Android 8–10 top/bottom cropping and visual bleed across overlays and dialogs, upgrade the Meal History ledger and introduce a matching Account History ledger (with left/right swipe and Summed vs. "Adding One by One" expression modes), refine all editor and triple-menu screens to a Google Material 3 standard with `<` back navigation, normalize the data & QR export engine, and fix the GitHub Actions release build.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and design decisions have been confirmed from your answers and latest revision instructions:

- **Confirmed Decision 1 — Inactive Meter Readings & Date Interpolation**:
  When a selected date range includes dates not explicitly present in recorded meter readings, consumption between recorded dates is linearly interpolated (`r1 + fraction * (r2 - r1)`). When readings stop updating after the last unpaid reading/bill and the user selects up to a later date, the calculator carries over the previous unpaid balance plus `₹0` for the inactive days after the last reading (rather than resetting to zero).
- **Confirmed Decision 2 — Light Mode Material 3 Surfaces, Shadows & Editor Screens**:
  All cards, sub-tab pills (`Log Reading` / `Bill Summary`), meter dial steppers, and editor screens (`Edit Profile`, `Add/Edit Hotel`, `Account Manager`, `Menu Editor`, `Utility Configuration`) will use **clean, opaque Material 3 surface containers** with native Android hardware elevation shadows (preventing the dark inner box artifact caused by applying `shadowElevation` to translucent backgrounds in Light Mode), rounded text inputs, balanced top margins, and M3 primary blue/tonal buttons.
- **Confirmed Decision 3 — Triple-Line Drawer Menus & `<` Back Navigation**:
  All triple-line overflow/drawer menus (`Hotel`, `Extra/Wallet`, `Bill`) will use **text-only action rows** (no leading icons, no duplicate emojis or repeated labels) with smooth slide + fade reveal animations. Across all screens and dialogs, back buttons will use the `<` chevron (`ChevronLeft` / `ArrowBackIosNew`) inside a soft circular container.
- **Confirmed Decision 4 — Android 8–10 Full-Screen Cropping & Visual Leak Fix**:
  Eliminate top/bottom bar cropping and background page bleed on Android 8–10 across Add/Edit Hotel & Account pages, History pages, and Settings sub-pages by enforcing physical screen dimensions via `getRealMetrics()` + `FLAG_LAYOUT_NO_LIMITS` in `DialogEdgeToEdge.kt`, restoring missing `DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)` across all dialogs, and rendering full-screen overlays edge-to-edge without secondary OS window inset clipping.
- **Confirmed Decision 5 — Dual-Mode Meal History & New Account History Ledgers**:
  Upgrade the **Meal History** page (`HotelManagementTab`) and create a matching **Account History** page (`ExtraExpensesTab`) featuring smooth left/right swipe month navigation and a two-mode toggle:
  1. **Summed View**: Clean transaction/meal cards showing itemized summaries, meal group subtotals, and summed totals.
  2. **"Adding One by One" Expression View**: Day-wise and meal-group-aware additive math expressions (`120 + 45 + 30 = ₹195`) showing how individual meal groups, dishes, or account expenses add up step by step.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Accurate Multi-Date & Inactive Billing Engine**: Calculates electricity bills across any two dates using piecewise linear interpolation across rate policy boundaries, while preserving unpaid balances when readings stop being logged. Verified by a dedicated unit test suite.
  2. **True Google Material 3 Light & Dark Mode Parity**: Fixes the translucent-surface shadow bleed shown in the `Log Reading` / `Bill Summary` toggle and removes remaining borders on `MeterDialPicker`, `EmptyStateCard`, and editor dialogs.
  3. **Android 8–10 Full-Screen & Dialog Geometry Fix**: Ensures all full-screen overlays (`EditProfile`, `MenuEditor`, `UtilityConfiguration`, `MealHistory`, `AccountHistory`, `BinScreen`, `Settings` sub-screens) and padded modal dialogs (`EditHotel`, `AccountEditor`, `HotelMonthlyPaymentDialog`) stretch 100% edge-to-edge on Android 8–10 with zero top/bottom cropping or background page leakage.
  4. **Interactive Meal & Account History Ledgers**: Provides responsive left/right swipe month navigation and two distinct calculation views (**Summed View** vs. **"Adding One by One" Expression View** with full meal-group support) in both the Hotel section and the Account/Extra section.
  5. **Normalized Data & QR Fountain Export Engine**: Exports 100% of independent base entities (including `electricityRates`, utility parameters `fixedServiceCharge` & `defaultElectricityRate`, `ownerAccounts`, `customCategories`, and `userProfile` preferences) while stripping redundant derived fields so payloads are smaller and automatically recalculated on import. Enhances QR Fountain packet splitting with adaptive chunk sizing.
  6. **CI/CD Release Pipeline Fix**: Removes `-x lintVitalRelease` from the GitHub Actions release workflow so Gradle 9.3.1 does not fail with `Task 'lintVitalRelease' not found`.
- **Target Audience / Persona**: Users tracking daily hotel meal expenses, multi-account wallet transactions, and pro-rata electricity bills completely offline across Android 8–15 phones and tablets.
- **Key Value**: Reliable billing math under every date-selection scenario, zero visual clipping on older Android 8–10 devices, transparent step-by-step expense math in both Hotel and Account ledgers, and native Google Material 3 visual polish.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Electricity Logging & Bill Summary (`BillTrackingTab`)**:
     - Toggling between **Log Reading** and **Bill Summary** uses solid opaque Material 3 pill containers (`#D3E3FD` active / `#FFFFFF` inactive in Light Mode) without translucent shadow bleed.
     - The 6-digit `MeterDialPicker` (`0000.00`) and `Type Reading` chip use clean borderless tonal surfaces with soft elevation.
     - Selecting any two dates in **Billing Period Selection** dynamically computes interpolated units, rate-wise breakdown, and carried-over unpaid balances.
  2. **Meal History & Account History Ledgers (`HotelManagementTab` & `ExtraExpensesTab`)**:
     - Swiping left or right smoothly transitions between months (`AnimatedContent` horizontal slide + gesture tracking that cooperates with vertical list scrolling).
     - Tapping the top-right **View Mode Toggle** switches between:
       - **Mode A — Summed View**: Displays individual records with meal group breakdowns (`Group 1 + Group 2 = Total`) or categorized account transactions and summed totals.
       - **Mode B — "Adding One by One" Expression View**: Groups entries by date and meal group, explicitly rendering the step-by-step addition expression (e.g., `03 Nov → (80 + 40) + 60 = ₹180` for multi-group meals, or `150 + 75 + 220 = ₹445` for daily account expenses) alongside the summed day total.
  3. **Editor & Configuration Screens (`EditProfile`, `EditHotel`, `AccountManager`, `MenuEditor`, `UtilityConfiguration`)**:
     - Consistent top bar with a circular `<` back button (`40.dp` tonal circle), balanced top spacing (`16.dp` below top bar), fully rounded `16.dp` borderless Material 3 text fields, elevated borderless section cards, and deep Material 3 primary blue action buttons (`#0B57D0`).
     - On Android 8–10, full-screen pages cover 100% of physical screen height with no top/bottom crop, while Add/Edit Hotel and Account dialogs render with clean padded modal cards over a full-screen edge-to-edge scrim.
  4. **Triple-Line Menus**:
     - Tapping the triple-line menu button opens a cleanly animated popup/sheet featuring text-only rows (no leading icons, no duplicate emojis or redundant subtitles) with smooth vertical slide + fade transitions.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Native Google Material 3 Expressive — crisp, borderless elevated surfaces in Light Mode (`#FCFCFD` background, `#FFFFFF` elevated cards, `#EFF4F9` tonal containers) and deep slate surfaces in Dark Mode (`#0A1317` background, `#141F26` elevated cards).
  - *Color Palette & Mood*:
    - `--background`: `#FCFCFD` (Light) / `#0A1317` (Dark)
    - `--surface-card`: `#FFFFFF` (Light) / `#141F26` (Dark)
    - `--primary`: `#0B57D0` (Light M3 Deep Primary Blue) / `#7FCFFF` (Dark M3 Primary)
    - `--secondary-container`: `#D3E3FD` (Light Active Pill) / `#004A77` (Dark Active Pill)
  - *Typography & Hierarchy*: **Plus Jakarta Sans** across all 15 Material 3 typography slots with unified scaling (`0.90x`, `1.0x`, `1.10x`).
  - *Component Styling & Layout*: Zero `BorderStroke` on cards, buttons, steppers, and chips. Native Android `Surface(shadowElevation = ...)` used exclusively on opaque surface colors.
- **Interactive Feedback & Motion**:
  - Triple-line menus animate in/out with `slideInVertically + fadeIn` (`tween(180ms, FastOutSlowInEasing)`).
  - Month transitions in Meal History and Account History animate smoothly with horizontal slide + fade when swiping left/right.
  - All back navigation buttons render `<` (`Icons.Default.ChevronLeft`) inside a circular tonal button.
  - Mandatory Help Section inside the About page is updated to document all new billing, history ledger, Android 8–10 compatibility, UI, and export enhancements.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Opaque Tonal Surfaces for Elevated Components in Light Mode**:
  - *Chosen Approach*: Always resolve active/selected card and pill colors (such as the `Log Reading` / `Bill Summary` switcher and button containers) to opaque composite colors (`#D3E3FD` for selected pill in Light Mode, `#FFFFFF` for unselected) before applying `shadowElevation`.
  - *Why*: Android's hardware `RenderNode` shadow caster draws a dark ambient/spot shadow shape directly behind the view bounds. If the surface color has `alpha < 1.0f`, the dark gray shadow bleeds through the face of the button.
- **Decision 2 — Fixing Android 8–10 Top/Bottom Cropping & Visual Bleed**:
  - *Chosen Approach*:
    1. Upgrade `DialogEdgeToEdge.kt` to query physical screen dimensions via `windowManager.defaultDisplay.getRealMetrics(DisplayMetrics())` (and `currentWindowMetrics` on API 30+), explicitly set `win.setLayout(realWidth, realHeight)`, apply `WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS` on API 26–29 (Android 8–10) so the OS dialog window never clips at the status bar or navigation bar, and enforce `realWidth`/`realHeight` up the `decorView` and parent view hierarchy.
    2. Restore `DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)` + `DialogEdgeToEdge()` on every `Dialog` that was missing them (`UnifiedHotelDialog`, `EditHotelModal`, `UnifiedAccountDialog`, `EditProfileDialog`, `HotelMonthlyPaymentDialog`, `BinScreen`, etc.), while keeping Add/Edit Hotel and Account forms as properly padded Material 3 dialogs over a 100% edge-to-edge backdrop.
  - *Why*: On Android 8–10 (API 26–29), `MATCH_PARENT` inside a secondary `Dialog` window is bounded by the non-inset dialog frame, cropping the top and bottom and letting the underlying screen leak through. Forcing `getRealMetrics()` dimensions with `FLAG_LAYOUT_NO_LIMITS` guarantees full physical coverage on every Android version.
- **Decision 3 — Unpaid Balance Carry-Over & Piecewise Interpolation in `MeterReadingViewModel`**:
  - *Chosen Approach*: Separate the calculation into:
    1. **Carried-Over Unpaid Balance**: Sum of any unpaid `UtilityBill` records (or unpaid meter reading intervals prior to the selected window) that remain unsettled, plus
    2. **Interpolated Window Usage**: Linearly interpolated meter readings between `startDate` and `endDate` clamped at the latest recorded reading (so inactive days after the last reading add `0.0` units and `₹0.00`), split pro-rata across any `ElectricityRate` effective dates in the window.
- **Decision 4 — Unified Dual-Mode History Ledgers for Both Meals & Accounts**:
  - *Chosen Approach*: Both **Meal History** (`HotelManagementTab`) and the new **Account History** (`ExtraExpensesTab`) share a cohesive ledger architecture:
    - **Left/Right Swipe Navigation**: Uses horizontal drag detection that coexists cleanly with `LazyColumn` vertical scrolling, paired with `AnimatedContent` month transitions and `<` / `>` month header buttons.
    - **Mode 1 (Summed View)**: Shows individual transactions with summed amounts, category/account badges (for accounts) or meal group summaries (for hotels).
    - **Mode 2 ("Adding One by One" Expression View)**: Aggregates transactions day-by-day and group-by-group, displaying both the explicit step-by-step addition expression (e.g., `Group 1 (40 + 25) + Group 2 (50 + 20) = ₹135` and `Day Total: 135 + 90 = ₹225` for meals; `120 + 350 + 80 = ₹550` for account expenses) and expandable/scannable item details.
- **Decision 5 — Normalized Independent-Only Export Schema & Adaptive QR Splitting**:
  - *Chosen Approach*: Include all independent base entities (`electricityRates`, `fixedServiceCharge`, `defaultElectricityRate`, `ownerAccounts`, `customCategories`, `userProfile`) while omitting derived fields (`dateText`, `unitsConsumed`, `mealGroups` subtotals, derived bank `balance`) and recomputing them on import. Dynamically size QR fountain chunks (`420–550` bytes) for faster camera scanning.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         UI & Presentation Layer                            │
│                                                                            │
│  ┌──────────────────────────┐  ┌────────────────────────────────────────┐  │
│  │   BillTrackingTab        │  │  Editor, Config & Sub-Screens          │  │
│  │  • Solid M3 Sub-Tabs     │  │  • EditProfile / EditHotel (Padded M3) │  │
│  │  • Borderless DialPicker │  │  • AccountManager / MenuEditor         │  │
│  │  • Date Range Selector   │  │  • UtilityConfig / Settings Sub-Pages  │  │
│  └────────────┬─────────────┘  │  • DialogEdgeToEdge (getRealMetrics)   │  │
│               │                └───────────────────┬────────────────────┘  │
│               ▼                                    ▼                       │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  Dual-Mode History Ledgers (MealHistoryModal & AccountHistoryModal)  │  │
│  │  • Left/Right Swipe Month Pager + '<' Back Button                    │  │
│  │  • Mode 1: Summed View  |  Mode 2: "Adding One by One" Expression    │  │
│  │  • Meal-Group-Aware Formulas: (a + b) + (c + d) = Day Total          │  │
│  └─────────────────────────────────┬────────────────────────────────────┘  │
│                                    ▼                                       │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  Triple-Line Drawer Menus (Hotel / Extra / Bill)                     │  │
│  │  • Text-only rows (no icons, no duplicate emojis) + Slide Animation  │  │
│  └─────────────────────────────────┬────────────────────────────────────┘  │
└────────────────────────────────────┼───────────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     ViewModel & Domain Calculation Layer                   │
│                                                                            │
│  ┌─────────────────────────────────┐  ┌─────────────────────────────────┐  │
│  │     MeterReadingViewModel       │  │       FinancialViewModel        │  │
│  │  • estimateReadingAt()          │  │  • Recalculate Bank Balances    │  │
│  │  • calculateDetailedBill()      │  │  • Derive Meal Group Subtotals  │  │
│  │  • Unpaid Carry-Over + 0 Idle   │  │  • Sync Independent Entities    │  │
│  └────────────────┬────────────────┘  └────────────────┬────────────────┘  │
└───────────────────┼────────────────────────────────────┼───────────────────┘
                    │                                    │
                    ▼                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                 Persistence & Normalized Transfer Engine                   │
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  DataTransferManager & QrExportManager / QrFountainEncoder           │  │
│  │  • Independent Base Data: Hotels, Menus, Orders(raw), Banks(init),   │  │
│  │    OwnerAccounts, Categories, Expenses, Readings(total), Bills,      │  │
│  │    ElectricityRates, UtilityPrefs(fixedCharge, defaultRate), Profile │  │
│  │  • Derived Data Stripped on Export & Recomputed on Import            │  │
│  │  • Adaptive QR Fountain Chunking (420B–550B) + CRC32 Verification    │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

### Key Implementation Specifications

1. **Android 8–10 Edge-to-Edge & Cropping Fix (`DialogEdgeToEdge.kt` & All Overlays)**:
   - In `DialogEdgeToEdge.kt`:
     - Obtain physical display dimensions using `windowManager.defaultDisplay.getRealMetrics(DisplayMetrics())` (`realWidth`, `realHeight`).
     - Clear `FLAG_TRANSLUCENT_STATUS`, `FLAG_TRANSLUCENT_NAVIGATION`, and `FLAG_LAYOUT_INSET_DECOR`; add `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS` and `FLAG_LAYOUT_NO_LIMITS` on Android 8–10 (`Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q`) so the dialog window spans the entire physical display without top/bottom clipping.
     - Force `win.setLayout(realWidth, realHeight)` and set `layoutParams.width = realWidth`, `layoutParams.height = realHeight` on `decorView`, `android.R.id.content`, and the Compose `dialogView.parent` view chain.
   - Ensure every full-screen overlay (`MealHistoryModal`, `AccountHistoryModal`, `EditProfileDialog`, `MenuEditorScreen`, `UtilityConfigurationScreen`, `BinScreen`, `Settings` sub-dialogs) and padded modal dialog (`EditHotelModal`, `UnifiedHotelDialog`, `UnifiedAccountDialog`, `HotelMonthlyPaymentDialog`) declares `DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)` and calls `DialogEdgeToEdge()`.

2. **Meal History Improvements & New Account History Viewing Page**:
   - **Meal History (`HotelManagementTab.kt`)**:
     - Enhance left/right swipe across the entire body (including over the `LazyColumn` via horizontal drag gesture detection + `AnimatedContent` month transition).
     - Segmented/pill toggle in the header switching between **Summed View** (`Σ Summed`) and **Expression View** (`a + b = c`).
     - In **Expression View**, each day card shows:
       - Individual meal groups and their dish-level additive expression (e.g., `Group 1: 40 + 20 = ₹60`, `Group 2: 50 + 30 = ₹80`).
       - The combined day-wise "adding one by one" expression across all meal groups and sessions on that day (`03.11.2026 → (40 + 20) + (50 + 30) = ₹140` or `60 + 80 = ₹140`).
   - **Account History (`ExtraExpensesTab.kt`)**:
     - Add a **"View Account History"** button and full-screen `AccountHistoryModal` matching the Meal History UX.
     - Supports filtering by active/all accounts, left/right swipe month navigation, and the two view modes:
       - **Summed View**: Chronological transaction cards with category, bank/owner account badge, and summed monthly inflow/outflow/net total.
       - **Expression View**: Day-by-day "adding one by one" mathematical expression (`03.11.2026 → 150 + 80 + 45 = ₹275`) with itemized labels below each day's formula and monthly summary footer.

3. **Included Independent Base Entities & Derived-Data Normalization**:
   - Export all independent base data (`userProfile`, `utilityConfig` [`fixedServiceCharge`, `defaultElectricityRate`], `electricityRates`, `meterReadings` [`id`, `readingDate`, `totalReading`, `isPaid`], `utilityBills`, `ownerAccounts`, `categories`, `hotels`, `menus`, `transactions` [`timestamp`, `isNight`, `itemsSerialized`], `bankAccounts` [`initialBalance`], `expenses`).
   - Strip derived fields on export and automatically recompute `unitsConsumed`, `totalAmount`, `mealGroups` subtotals, and current bank `balance` on import.

4. **Unit Testing Strategy (`MeterReadingBillingTest`)**:
   - Verify multi-reading linear interpolation across non-listed dates, pro-rata rate splitting across `ElectricityRate` changes, and inactive reading unpaid balance carry-over (`previous unpaid bill + ₹0`).
