# PizzaCrash — Project Status

## Current stage: Phase 0 — Existing project inspection (COMPLETE, awaiting approval to begin Phase 1)

Date of last update: 2026-10-21

## 1. Inspection results (verified against actual files)

### Technology stack (must be preserved per master prompt)
- **Language:** Java (no Kotlin plugin present; `app/src/main/java/com/example/myapplication/MainActivity.java` is the only source file).
- **UI framework:** XML layouts + AppCompat/Material Components (`activity_main.xml`, ConstraintLayout). Jetpack Compose is NOT installed and will not be introduced without explicit approval.
- **Build system:** Gradle Groovy DSL (`build.gradle`, not `.kts`).
  - AGP 8.5.2, Gradle wrapper 8.7, root project name `MyApplication`.
- **Android config:** namespace/applicationId `com.example.myapplication`, compileSdk 34, minSdk 24, targetSdk 34, Java 17.
- **Dependencies:** androidx.appcompat 1.7.0, com.google.android.material 1.12.0, androidx.constraintlayout 2.1.4. No coroutines, no lifecycle, no DataStore/Room yet.
- **Screens:** Single `MainActivity` showing "Hello World!" TextView. Theme `Theme.MyApplication` (Material DayNight with DarkActionBar).
- **Assets:** Default vector launcher foreground, adaptive icon XMLs, backup/data-extraction rules. No game art, no sounds, no level data.
- **Repository hygiene:** Stray tracked file `test.html` (1 byte) exists at repo root — flagged, not removed (preservation policy).

### Build status
- `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (33 tasks executed), producing `app/build/outputs/apk/debug/app-debug.apk` (~5.8 MB). Verified 2026-10-21.
- Note: this environment initially had no Android SDK. A headless SDK was installed at `/opt/android-sdk` (cmdline-tools 11076708, platforms;android-34, build-tools;34.0.0, platform-tools) so builds can be verified via CLI. Builds require `ANDROID_HOME=/opt/android-sdk`.
- **No emulator/device is available in this environment** — runtime UI verification cannot be performed here; the user runs the app in Android Studio. Verification strategy: Gradle build + JVM unit tests for gameplay logic + careful static review of UI code.

## 2. What needs to be established before the first screen (Phase 1 foundation plan)
1. Game design tokens: colors, typography, button/panel styles (XML resources, since project is XML-based).
2. Portrait orientation lock + edge-to-edge/inset handling for MainActivity-equivalent screens.
3. Navigation skeleton: Activities (Splash → Home → WorldMap → Game) or single-Activity + Fragments; recommend plain Activities + Intent extras to stay dependency-light, adding `androidx.lifecycle` only where useful.
4. Core package structure under `com.example.myapplication` (e.g. `game/`, `ui/`, `data/`, `level/`) — package name and applicationId unchanged.
5. Persistence choice: SharedPreferences-backed save repository now (zero new dependencies); upgrade path to DataStore if approved later.
6. Level/world configuration as data-driven Java structures loaded from a JSON asset (needs a JSON approach decision — `org.json` is built into Android, so no new dependency required).
7. Match-3 board rendering: custom `View` with Canvas (best fit for XML architecture and animation control).
8. Unit-test setup: add `testImplementation 'junit:junit:4.13.2'` (standard, currently absent) for deterministic gameplay-rule tests.

## 3. Important technical risks
- No emulator here → visual fidelity vs. reference images can only be judged by the user; must iterate on their feedback.
- Java-only project: heavier ceremony for state models; mitigated by small, focused classes.
- Material theme has an ActionBar; game screens will need NoActionBar variants — a change to themes.xml (additive, non-destructive).
- Adding animations: prefer native ViewPropertyAnimator/Canvas animation over new libraries (Lottie would need approval).
- 200 levels must be data-driven; risk of hardcoding creep — enforce single `LevelConfig` loader.

## 4. Recommended first implementation task (awaiting approval)
**Phase 1 foundation + Splash/Home scaffold**, kept minimal:
- Design tokens (colors.xml additions, styles for buttons/panels, text styles).
- NoActionBar fullscreen-ish game theme; portrait lock.
- Package structure + navigation stubs (HomeActivity with a placeholder "Play" button wired to open WorldMapActivity stub).
- Verify with `assembleDebug` + first unit test module wiring.
Then STOP and wait for the first UI reference image (expected: home/splash screen) before implementing any approved screen.

## Recent changes
- 2026-10-21: Phase 1 foundation scaffold implemented (user approved Phase 0 + first step): design tokens (colors.xml, dimens.xml, styles.xml), Theme.PizzaCrash + Theme.PizzaCrash.Splash themes, portrait manifest with Splash→Home→WorldMap navigation stubs, SaveRepository (SharedPreferences persistence for progression/economy/boosters/settings), world-name strings, junit test dependency. Verified `assembleDebug` + `testDebugUnitTest` → BUILD SUCCESSFUL.
- 2026-10-21: Added `README.md` (project overview, tech stack, build instructions, workflow). No application code changed; `assembleDebug` re-verified BUILD SUCCESSFUL after the addition.

## Approval log
| Milestone | Status | Date |
|---|---|---|
| Phase 0 inspection report | Approved by user | 2026-10-21 |
| Phase 1 foundation scaffold | Implemented — awaiting visual review/approval | 2026-10-21 |
