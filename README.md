<div align="center">

<img src="app_logo_512px.png" alt="Traker Logo" width="120" height="120" style="border-radius: 28px;" />

# Traker

**Offline-First Smart Financial Ledger, Hotel Khata & Electricity Bill Manager for Android**

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%E2%80%9315%2B-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](#)
[![Database](https://img.shields.io/badge/Room%20SQLite-v36%20Offline-FFCA28?logo=sqlite&logoColor=black)](#)
[![APK Size](https://img.shields.io/badge/APK%20Size-~3.2%20MB-10B981)](#)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20%E2%80%A2%20Zero%20API%20Keys-0EA5E9)](#)

</div>

---

## Overview

**Traker** is a fast, privacy-first Android application built with **Kotlin** and **Jetpack Compose (Material 3)**. It unifies daily hotel/tiffin meal accounting, personal & bank expense tracking, multi-rate electricity meter billing, and rich sticky notes into a single, 100% offline-capable workspace—optimized for both **mobile phones** and **tablets / desktop (DeX)** layouts.

- **100% Offline & Private**: All records, receipts, and voice memos stay locally on your device. No cloud accounts, no telemetry, and zero external API keys required.
- **Lightweight (~3.2 MB APK)**: Uses unbundled Google Play Services ML Kit OCR and R8 resource shrinking for minimal storage and RAM footprint.
- **Bilingual UI**: Seamless switching between **English** and **Bengali (বাংলা)**.
- **Adaptive Layouts**: Automatically scales from single-column handheld views to multi-column split-pane and wall layouts on tablets and foldables.

---

## Core Modules & Features

### 1. Hotel Meal & Khata Management
- **Multi-Hotel Ledger**: Track daily breakfast, lunch, dinner, and custom menu items across multiple hotels or tiffin services.
- **Offline Khata Book OCR Scanner**:
  - Scan printed or handwritten notebook entries (e.g., `9.10.26 - 60+50`) using on-device ML Kit Text Recognition.
  - Includes **EXIF orientation auto-correction**, **full-resolution camera capture**, **adaptive contrast enhancement**, and **multi-angle (`0°`, `90°`, `270°`) OCR passes**.
  - **Smart Dish Matcher**: Automatically resolves prices against your hotel menu, prompts with 4-option price combinations when multiple dishes match, and lets you compare or keep existing database entries on conflicting dates.
- **Monthly Payment Status & Receipts**:
  - Mark months as **Paid**, **Partial**, or **Unpaid** with attached payment receipt images (auto-compressed to `≤ 2 MB` JPEG).
  - **Partial Payment Residual Transfer**: When recording a partial payment on a historical month bill, select which subsequent month receives the remaining unpaid balance.

---

### 2. Extra Expenses, Bank Accounts & Smart UPI Detection
- **Multi-Account Balance Reconciliation**: Manage multiple bank accounts and cash wallets alongside personal owner profiles with QR codes.
- **Compact Transaction Cards & Direct Editing**:
  - Space-optimized transaction cards display category icons, full-width titles, account badges, and conditional attachment chips (shown only when attachments exist).
  - Edit any past transaction directly from the main Extras tab or full Transaction History screen with automatic old/new account balance reconciliation.
- **Opt-In UPI & Bank SMS Auto-Detection**:
  - Detects incoming UPI payment notifications (GPay, PhonePe, Paytm, BHIM) and bank debit SMS alerts, presenting an instant popup to log the expense in one tap.
  - Controlled via a master toggle in **Settings** and the **Extras** quick menu.
- **Voice Memo + Simultaneous Speech-to-Text (STT)**:
  - Dictate notes offline (with automatic online/default recognizer fallback and a direct shortcut to Android's Offline Speech Packs).
  - Record `.m4a` voice memos in **Add Transaction** or the **UPI Payment Popup** while simultaneously synthesizing spoken words into the text note field.
- **Monthly Payment Reminders**:
  - Configure recurring monthly bill/EMI reminders with customizable start dates, in-place editing, and automatic suppression once paid.

---

### 3. Electricity Meter & Utility Billing
- **Quick Dial Meter Logger**: Log daily or periodic kWh readings with a tactile dial picker placed directly above the analytics chart.
- **Interactive Daily Consumption & Rate Region Graph**:
  - Visualizes daily `kWh/day` consumption bars color-coded by active tariff rate slabs (`₹/unit`), average consumption reference lines, and tap-to-inspect tooltips.
  - Can be toggled on or off anytime from **Settings → Appearance & Display**.
- **Detailed Multi-Rate Bill Breakdown**:
  - Computes projected and finalized utility bills with full date ranges, meter reading ranges (`prev → curr`), slab-wise kWh units, service charges, and partial payment carry-over selection.

---

### 4. Zero-Lag Floating Sticky Notes
- **Instant Rendering Engine**:
  - Background-prewarmed preview cache (`Dispatchers.Default`) + contiguous `SpanStyle` run merging + `TextStringSimpleNode` fast-path for instant tab opening and smooth 60/120 Hz scrolling even with 500+ notes.
  - Custom `drawWithCache` 3D Push-Pin (`📌`) and Metallic Paperclip (`📎`) vector badges with zero per-frame allocation overhead.
- **Rich Note Capabilities**:
  - Inline `*bold*` and `_italic_` formatting, per-line alignment (`Left`, `Center`, `Right`, `Justify`), borderless visual grid tables (`Rows × Cols`), custom color palettes, tag filtering, and image/file attachments.
  - **Responsive Wall Mode**: Alternating left/right cards on mobile portrait and a virtualized 2-to-4 column staggered wall on tablets and landscape screens.

---

### 5. Universal Sync, `.zip` Media Backup & 5-Round QR Transfer
- **Complete `.zip` Archive Backup & Restore**:
  - Export and import `.zip` archives that bundle `backup_data.json` together with all attached receipt images, sticky note files, and voice memos (`media/`).
  - Supports granular backup/restore by module (Universal Transfer, Whole Hotel Data, Hotel Order History, Extra Transactions, Utility Bills, or Sticky Notes) as well as JSON and CSV formats.
- **Animated Multi-Frame QR Stream**:
  - Transfer data device-to-device offline via animated QR packets that automatically pause after **5 complete rounds** and reset to frame 1.
- **Recycle Bin & App Lock**:
  - Two-tier soft delete Recycle Bin for safe recovery, plus a 4-digit Security PIN lock with recovery question protection.

---

## Tech Stack & Architecture

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.0+ (Coroutines & StateFlow) |
| **UI Framework** | Jetpack Compose + Material Design 3 (Dynamic & Liquid Glass Themes) |
| **Architecture** | MVVM (ViewModel + Repository + Reactive Room Flows) |
| **Local Database** | AndroidX Room SQLite (Schema v36) |
| **Camera & Media** | AndroidX CameraX, `ExifInterface`, Coil Image Loader, `MediaRecorder` / `MediaPlayer` |
| **QR & Serialization** | ZXing Core (`3.5.3`), GZIP Base64 Packet Framing, Java `ZipOutputStream` / `ZipInputStream` |
| **Khata Import** | Google Lens / Clipboard Paste with Auto-Repair Normalization & Flexible Date Match |
| **Compatibility** | Android 8.0 (API 26) through Android 15+ (API 36) |

---

## Building & Releases

### Local Debug Build
```bash
gradle assembleDebug
```

### Automated GitHub Release & In-Place APK Upgrade
The repository includes a workflow (`.github/workflows/android_release.yml`) that supports **one-step version bumping**:
1. Open **Actions → Build & Release Android APK → Run workflow**.
2. Enter the release tag (e.g. `v0.0.3`).
3. The workflow automatically extracts `versionName` (`0.0.3`) and calculates a monotonically increasing `versionCode` so users can install new releases directly over existing APKs without uninstalling or losing data.

---

## License

This project is open-source and distributed under standard open-source licensing terms. See the in-app **Settings → About & Help → Open Source Knowledge Lists** section for full library attributions.
