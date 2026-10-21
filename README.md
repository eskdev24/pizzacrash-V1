# PizzaCrash 🍕

An original, colorful, premium-quality **native Android match-3 puzzle game**. Players match pizza ingredients, complete objectives, earn stars, unlock levels, and explore 10 themed worlds (200 levels total).

## Project Overview

| | |
|---|---|
| **Genre** | Match-3 puzzle |
| **Platform** | Android phones (portrait) |
| **Language** | Java |
| **UI framework** | XML layouts + Material Components |
| **Build system** | Gradle (Groovy DSL), AGP 8.5.2, Gradle wrapper 8.7 |
| **Package / applicationId** | `com.example.pizzacrash` (migrated from `com.example.myapplication`) |
| **SDK** | compileSdk 34 · minSdk 24 · targetSdk 34 · Java 17 |

The game board is rendered with a custom Android `View` (Canvas drawing); no game engines or cross-platform frameworks are used.

## Game Structure

- **10 themed worlds × 20 levels = 200 levels**, all data-driven from level/world configuration (never hardcoded per-scene).
- Worlds: Pizza Town → Italian Village → Desert Oasis → Ocean Bay → Snowy Mountains → City Skyline → Tropical Island → Volcano Land → Candy Forest → Pizza Kingdom.
- Core loop: swap adjacent ingredient tiles → match 3+ → clear & collect → satisfy level objectives within a move limit → earn 1–3 stars → unlock the next level.
- Boosters (hammer, swap, bomb, rainbow), coin economy, daily rewards, and local save/persistence.

## Repository Layout

```
├── app/                     # Main (and only) Android module
│   └── src/main/
│       ├── java/com/example/myapplication/   # Game source code
│       ├── res/             # XML layouts, drawables, themes, values
│       └── assets/          # Level/world config data
├── gradle/                  # Gradle wrapper
├── build.gradle             # Root build script
├── settings.gradle          # Module + repository configuration
├── PROJECT_STATUS.md        # Current stage, verification results, approval log
└── README.md                # This file
```

## Getting Started

### Requirements
- Android Studio (Giraffe or newer recommended)
- JDK 17
- Android SDK Platform 34 + Build-Tools 34.0.0

### Build & Run
1. Open this folder in **Android Studio** and let Gradle sync.
2. Select a device/emulator (API 24+, portrait phone profile).
3. Press **Run**, or from the command line:

```bash
./gradlew assembleDebug          # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew test                   # JVM unit tests (gameplay logic)
```

If building from a plain shell (not Android Studio), point Gradle at an Android SDK:

```bash
export ANDROID_HOME=/path/to/android-sdk
```

## Development Workflow

Development is **incremental and UI-reference-driven**: reference images are supplied one at a time; each screen is analyzed, specified, implemented natively, verified, and then requires explicit approval before the next milestone begins. See `PROJECT_STATUS.md` for the current stage and roadmap status.

## Documentation

- `PROJECT_STATUS.md` — completed work, current stage, outstanding issues, approval log.

## Notes

- No real-money purchases, accounts, cloud sync, or analytics are included.
- All artwork and content must be original to PizzaCrash's identity.
