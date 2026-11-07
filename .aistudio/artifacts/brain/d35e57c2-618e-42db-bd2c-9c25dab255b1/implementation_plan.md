# Traker Performance, APK Size Reduction & 16-Point Feature Upgrade Plan

This plan reduces the compiled APK size from **~15 MB back to ~3.2 MB**, fixes Khata OCR and offline voice capture, makes Sticky Notes load instantaneously even with hundreds of notes, and implements all 16 requested UI, workflow, backup, and billing enhancements.

---

## User Review & Confirmed Decisions

> [!IMPORTANT]
> All three product decisions from our discussion have been locked in:
> 1. **Historical Partial Bill Payment Residual Handling**:
>    - If you edit the **most recent (just-previous) month's bill**, the remaining unpaid balance automatically carries forward into the ongoing/upcoming month since the next bill is not yet finalized.
>    - If you edit an **older historical month** (e.g., editing August when September/October bills already exist), a mandatory **"Transfer Residual Balance To Month"** selector appears. Saving is only allowed once you pick which existing bill receives the residual amount, preventing misalignment across past months.
>    - Displays the timely-payment reminder quote inside the dialog: *"“Timely settlement today buys peace of mind tomorrow — clear small dues early so future months stay effortless.”"*
> 2. **Log Reading Page Interactive Electricity Graph**:
>    - Placed at the top of the **Log Reading** tab (and toggleable from **Settings → Appearance**).
>    - Combines a **Daily kWh Consumption Bar + Trend Line Chart** (highlighting whether daily usage is rising or falling versus your rolling average) with an interactive **Region Selector (Start Reading → End Reading)** that shows total $\Delta\text{ kWh}$ and exact split cost across multiple electricity rate policies.
> 3. **Privacy-Safe GitHub Buttons & Concise Help/About Redesign**:
>    - All personal names, `@suvadippatra` handles, emails, and raw URLs are removed from visible UI text.
>    - Two clean buttons with embedded links are shown: **"Give a Star on GitHub"** (`https://github.com/suvadippatra/tracker/`) and **"New Issue or Request Feature"** (`https://github.com/suvadippatra/tracker/issues/new/choose`).

---

## 1. Core Size, OCR, Voice & Sticky Notes Optimizations

### A. APK Size Reduction (~15 MB → ~3.2 MB) & Back Callback Warning Fix
- **Why APK Size Jumped**: `com.google.mlkit:text-recognition:16.0.1` bundles ~3 MB of native C++ OCR libraries across 4 CPU ABIs (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) = **~12 MB**.
- **Solution**:
  - Replace the bundled artifact with **Google Play Services Unbundled ML Kit** (`com.google.android.gms:play-services-mlkit-text-recognition:19.0.1`, ~250 KB), which uses the shared OCR engine already present in Android's Google Play Services.
  - Add `<meta-data android:name="com.google.mlkit.vision.DEPENDENCIES" android:value="ocr" />` and `android:enableOnBackInvokedCallback="true"` in `AndroidManifest.xml` (resolving the `WindowOnBackDispatcher` warning).

### B. Khata Book Scanner Fix (Printed & Handwritten Text)
- **EXIF Orientation Auto-Correction**: Read `ExifInterface` orientation when decoding gallery/camera images and rotate the `Bitmap` upright before displaying and before running ML Kit / optical recognition.
- **Full-Resolution Camera Capture**: Replace low-res `TakePicturePreview()` with full-resolution `ActivityResultContracts.TakePicture()` backed by a temporary `FileProvider` cache file (downsampled cleanly to 1600px).
- **Raw Printed Text Preservation**: Always populate the Step 1 editor box with **all recognized lines** (including general printed text) instead of discarding lines that don't immediately match `Date - Price`, and apply the smart handwritten date/price normalizer when transitioning to Step 2.

### C. Reliable Voice Recording + Automatic Speech-to-Text Synthesis (Item 13)
- **Record First, Synthesize Automatically**:
  - Available on **both** the **Add Transaction** screen and the **UPI Auto-Detected Payment Popup**.
  - When you record a voice note (which is 100% reliable even in noisy places or when in a hurry), the audio file (`.m4a`) is always saved safely as a voice attachment **while simultaneously running `SpeechRecognizer` synthesis** (with automatic fallback from `bn-IN` / `en-IN` → `en-US` → device default locale) to populate the text note automatically.
  - Includes a one-tap shortcut to Android's **Offline Speech Recognition Settings** if an offline language pack is missing.

### D. Sticky Notes Instant Loading (<30ms for 500+ Notes)
- **Background Pre-Computation (`Dispatchers.Default`)**: Pre-parse each `StickyNoteEntity` in `FinancialViewModel` into a lightweight `StickyCardPreviewUiModel` (title, sub-heading, pin/clip type, tag list, first 4 table rows, and truncated 250-char formatted preview `AnnotatedString`). Zero JSON or regex parsing runs on the UI thread during composition.
- **Tablet & Mobile Lazy Virtualization**: Replace the non-lazy `Row + verticalScroll` on Tablet/Wall mode with a virtualized `LazyColumn` of multi-column staggered rows so only the 6–10 visible cards on screen are composed.
- **Lightweight Gesture Handling**: Use native `combinedClickable` for single-tap (open) and long-press (rearrange mode) so scrolling is never blocked and per-card coroutine overhead is eliminated.

---

## 2. All 16 User Experience & Functional Enhancements

1. **Extras Tab Two-Column Tablet Layout**:
   - On tablets and landscape screens (`maxWidth >= 600.dp`), arrange Bank Account cards and Transaction History items in a responsive **2-column grid**, matching the Hotel Management tab.
2. **Hide "View Attachment" When No Attachment Exists**:
   - In Transaction History cards and popups, only display the attachment badge/button when `attachmentUri` (or voice note URI) is actually present.
3. **Direct Transaction History Editing & Compact Card Layout**:
   - Add **"Edit Transaction"** to every Transaction History item (via tap or the 3-dot `⋮` menu), opening the transaction editor pre-filled with existing values, category, account, and attachments—matching Hotel Meal History editing.
   - Redesign the Transaction History card to optimize horizontal and vertical density: compact category icon badge on the left, single-row title + category/account pill in the center, and right-aligned price + 3-dot action menu.
4. **First-Name Only on Greetings & Lock Screen**:
   - Extract `userProfile.name.trim().substringBefore(" ")` so both the top greeting (*"Good Morning, FirstName"*) and the Lock Screen welcome header display only your first name.
5. **Broader Monthly Payment Reminder Popup & Edit Saved Reminders**:
   - Expand the Monthly Payment Reminder dialog width (`fillMaxWidth(0.94f).widthIn(max = 560.dp)`) for comfortable reading.
   - Add an **Edit (`✏️`)** action on every saved reminder in the list so you can load it back into the form, modify its heading/amount/day/time/account, and save changes in place.
6. **Enhanced UPI Payment Auto-Detection + Settings Master Toggle**:
   - Improve notification & SMS pattern matching for amount (`₹`, `Rs`, `INR`), counterparty (`paid to`, `sent to`, `VPA`), and bank/app source.
   - Add a **UPI Payment Auto-Detection** switch inside **Settings** (synced with the Extras page) so you can turn background detection ON or OFF anytime.
7. **Partial Bill Payment Residual Selector & Timely Payment Quote (Electricity & Hotel Bills)**:
   - Support partial ("Paid Amount Edit") payments on both **Electricity Bills** and **Hotel Monthly Bills**.
   - Enforce the confirmed rule: if editing the **just-previous month**, carry the residual forward automatically to the current unfinalized period; if editing an **older month**, require selecting the target month bill to receive the residual balance before saving.
   - Display the motivational quote banner encouraging timely payments to avoid future stress.
8. **ZIP Archive (`.zip`) Export & Import with Media Attachments**:
   - In `DataBackupSettingsScreen`, alongside `JSON Copy/Paste` and `QR Send/Receive`, add **"Export as ZIP Archive (.zip)"** and **"Import from ZIP Archive (.zip)"** for every export category (Universal Transfer, Hotel Data, Electricity Data, Transactions/Extras, Sticky Notes, and Settings).
   - The `.zip` archive packages `backup_data.json` together with an `attachments/` folder containing all referenced images, receipts, voice recordings, and documents, and restores them automatically on import.
9. **QR Code Send Auto-Stop After 5 Rounds**:
   - In `QrExportStreamDialog`, count completed cycles (`1..N`). Automatically pause after **5 full rounds** and rest on Frame `1 / N`, showing a **"Play 5 More Rounds"** button if the user wishes to transmit again.
10. **Extras Top Hero Balance Card Landscape/Tablet Optimization**:
    - On wide/landscape screens, switch the Extras Hero Balance card to a balanced **2-pane horizontal layout** (Left: Total Available Balance + Income/Expense pills; Right: Monthly Reminder button + UPI Auto-Detect status + Filter actions) so it takes minimal vertical height.
11. **Redesigned, Concise About & Help Section**:
    - Keep the top App Icon & Version card unchanged.
    - Replace the repetitive long-form text walls below it with a sleek, scannable grid of **short 1-sentence feature cards**, a compact **Open-Source Libraries** chip list, and the two privacy-safe GitHub buttons.
12. **GitHub Actions One-Step Version Bump & Update-Compatible APK**:
    - Update `app/build.gradle.kts` to read `-PappVersionName` and `-PappVersionCode` properties.
    - Update `.github/workflows/android_release.yml` so when you enter `tag_name` (e.g., `v0.0.2`) on the GitHub Actions workflow page, it automatically passes `-PappVersionName=0.0.2` and a monotonically increasing `-PappVersionCode` (computed from the semantic version + `github.run_number`) while signing with the consistent keystore (`debug.keystore.base64`). Downloaded APKs will install directly as an **Update** over existing installations without uninstalling.
13. **Voice Memo + STT Synthesis in UPI Auto-Popup**:
    - Add a built-in Voice Recorder + Auto-STT synthesis bar directly inside `UpiAutoDetectedPaymentPopupDialog`.
14. **Privacy-Safe Embedded GitHub Links**:
    - Remove all developer name / `@suvadippatra` strings from visible UI and dialogs.
    - Add **"Star on GitHub"** (`https://github.com/suvadippatra/tracker/`) and **"New Issue or Request Feature"** (`https://github.com/suvadippatra/tracker/issues/new/choose`) buttons using `LocalUriHandler.current.openUri(...)` without showing raw URLs.
15. **Daily Meter Reading Change Toggle (Appearance Settings) + Interactive Log Reading Graph**:
    - Add a toggle in **Settings → Appearance**: *"Show Daily Meter Usage Rate & Consumption Graph"*.
    - Add an interactive **Consumption Trend & Region Analyzer Graph** at the top of the **Log Reading** page:
      - Visualizes daily `kWh/day` consumption bars + smooth trend line so you can immediately see if today's usage is higher or lower than previous days.
      - Allows selecting any **Start Date/Reading** and **End Date/Reading** region on the chart to inspect total reading difference ($\Delta\text{ kWh}$) and exact multi-rate cost breakdown when the selected region spans 2+ electricity rate policies.
16. **Settings → "View Another Tools" Full-Screen Page**:
    - Add a **"View Another Tools"** item in the main Settings screen that opens an edge-to-edge full-screen page with a clean top bar and empty canvas ready for future tool cards.

---

## 3. Technical Architecture & Data Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         TRAKER ANDROID ARCHITECTURE                          │
├──────────────────────────────────────────────────────────────────────────────┤
│  1. APK SIZE & BUILD PIPELINE (.github/workflows/android_release.yml)        │
│     • Unbundled Play Services ML Kit OCR (~250 KB vs 12 MB bundled)          │
│     • tag_name (e.g. v0.0.2) ──► -PappVersionName & -PappVersionCode         │
│     • Consistent Keystore ──► Direct In-Place APK Update on Device           │
├──────────────────────────────────────────────────────────────────────────────┤
│  2. BACKGROUND PRE-COMPUTATION & MEDIA ZIP ENGINE                            │
│     • Room SQLite (v36) ──► Dispatchers.Default ──► StickyCardPreviewUiModel │
│     • ZIP Exporter/Importer (java.util.zip)                                  │
│       └── Bundles backup_data.json + /attachments/* (Images, Voice, Files)   │
├──────────────────────────────────────────────────────────────────────────────┤
│  3. ADAPTIVE UI & INTERACTIVE ANALYTICS                                      │
│     • Extras Tab: Adaptive 2-Column Tablet Grid + Landscape Hero Card        │
│     • Log Reading Tab: Daily kWh Trend Graph + Multi-Rate Region Selector    │
│     • Electricity & Hotel Bills: Partial Payment + Target Month Selector     │
│     • UPI Listener + Voice Recorder: Saves .m4a + Synthesizes Text via STT   │
└──────────────────────────────────────────────────────────────────────────────┘
```
