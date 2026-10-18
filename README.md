# CashRound 💵

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-SDK%2026%2B-green.svg?logo=android)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-brightgreen.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Build](https://img.shields.io/badge/Tests-56%20Passed-success.svg)](#running-unit-tests)

**CashRound** is a modern, intuitive, and precise Android utility application built with Jetpack Compose and Material 3. It simplifies cash calculations, bill/coin denomination rounding, and currency management for retail, point-of-sale (POS) operations, cashiers, and personal budgeting.

---

## ✨ Features

- **Exact Cash Denomination Rounding**:
  - **Round to Nearest**: Rounds to the closest valid denomination multiple (half-up rule).
  - **Round Up**: Rounds up to the next valid denomination multiple.
  - **Round Down**: Rounds down to the previous valid denomination multiple.
- **Flexible Denominations**:
  - Quick-select common denomination presets (`$0.05`, `$0.10`, `$0.25`, `$0.50`, `$1`, `$5`, `$10`, `$20`, `$50`, `$100`).
  - Custom denomination input for unique currencies, coins, or physical cash notes.
- **Multi-Currency Support**:
  - Built-in support for major world currencies: **USD ($)**, **EUR (€)**, **GBP (£)**, **PKR (Rs.)**, **AED**, **SAR**, **CAD (CA$)**, **AUD (A$)**, **INR (₹)**, **JPY (¥)**, and **CNY (¥)**.
  - Custom Currency option allowing configuration of custom currency code, symbol, and decimal precision (0–4 decimal places).
- **Interactive Calculation Visualizer**:
  - Visual breakdown showing **Original Amount**, **Rounded Amount**, **Adjustment Difference (+/-)**, and denomination status.
- **History & Session Persistence**:
  - Save rounding calculations to history with timestamps.
  - Restore previous calculations instantly or manage history items.
  - Option to persist the last calculation session across app restarts.
- **Sharing & Copying**:
  - Copy formatted result summary to clipboard or share via device intent.
- **Modern Material 3 UI & Theming**:
  - Dynamic Color support (Material You).
  - System, Light, and Dark theme modes.
  - Haptic feedback and smooth UI state transitions.
- **Zero Floating-Point Inaccuracy**:
  - Domain arithmetic uses Long minor units (integer math) with arithmetic overflow checks.

---

## 🧮 Math & Rounding Logic

For an amount $A \ge 0$ and denomination $D > 0$:
$$A = q \times D + r \quad (0 \le r < D)$$

- **Down**: $q \times D$
- **Up**: $q \times D$ if $r = 0$, else $(q + 1) \times D$
- **Nearest**: $q \times D$ if $r < D - r$, else $(q + 1) \times D$ *(rounds half-up when $2r = D$)*

---

## 🛠️ Tech Stack & Architecture

- **Language**: 100% [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/)
- **Architecture**: Unidirectional Data Flow (UDF) with MVVM (`CashRoundViewModel` + `StateFlow`)
- **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **Data Persistence**: SharedPreferences via Key-Value repository abstraction
- **Min SDK**: `26` (Android 8.0)
- **Target / Compile SDK**: `37` (Android 16 / Latest)

---

## 📁 Project Structure

```text
CashRound/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/cashround/
│   │   │   │   ├── data/           # KeyValueStore abstractions
│   │   │   │   ├── domain/         # Core calculator, parser, formatter & validator
│   │   │   │   ├── model/          # Data models, UI states & Enums
│   │   │   │   ├── repository/     # History and Settings repositories
│   │   │   │   ├── ui/             # Jetpack Compose UI screens (Home, History, Settings, About)
│   │   │   │   └── util/           # Platform helpers & accessibility utilities
│   │   │   └── AndroidManifest.xml
│   │   └── test/                   # Comprehensive JUnit unit tests (56 tests)
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml          # Version Catalog
└── build.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer recommended.
- **JDK**: Version 11 or higher.
- **Android SDK**: API Level 37 installed.

### Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/CashRound-.git
   cd CashRound-
   ```

2. Open the project in **Android Studio**.

3. Build and run on an emulator or physical device running Android 8.0 (API 26) or higher:
   ```bash
   ./gradlew installDebug
   ```

---

## 🧪 Running Unit Tests

Run the complete test suite using Gradle:

```bash
./gradlew test
```

All 56 unit tests cover:
- Core arithmetic rounding edge cases (nearest, up, down)
- Denomination parser & minor unit conversion
- Currency configuration & formatting
- History repository and settings persistence
- ViewModel state management & input validation

---

## 📄 License

```text
Copyright 2025 CashRound

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License.
```
