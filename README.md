# Bolo Bhai

Local, offline Hinglish voice-dictation desktop app for macOS & Windows.
Kotlin Multiplatform + Compose Multiplatform Desktop (JVM). See `BoloBhai.md` for the full spec.

> **Tagline:** "Bolo Bhai, baaki hum likh denge."

## Status

**Milestone 1 — Skeleton & Design System: complete.** Theme tokens (dark/light/system),
bundled fonts (Inter / Space Grotesk / JetBrains Mono), sidebar shell, navigable placeholder
screens, tray icon, and `settings.json` persistence. No dictation yet (Milestones 2+).

## Requirements

- JDK 17+ (built and tested on Temurin 21)
- No local Gradle needed — use the wrapper (`./gradlew`). If the wrapper can't download
  its distribution on your network, install Gradle 8.9 manually and substitute `gradle` for
  `./gradlew` in the commands below.

## Build & run

```bash
./gradlew :composeApp:run          # launch the app
./gradlew :composeApp:desktopTest  # run unit tests
./gradlew :composeApp:createDistributable   # build a runnable app image
./gradlew :composeApp:packageDmg   # macOS installer
./gradlew :composeApp:packageMsi   # Windows installer
```

## Module layout

```
composeApp/
  commonMain/   app/ (AppState, Screen)  data/ (Settings, SettingsRepo)
                domain/  ui/theme (tokens, type, BoloTheme)
                ui/components  ui/screens  ui/ (Sidebar, Shell)
                composeResources/font, composeResources/drawable
  desktopMain/  Main.kt (entry, tray, window), platform/ (Platform)
  commonTest/   unit tests (TDD)
```

Architecture: one `AppState` (`StateFlow`s) drives the UI; `domain/` is pure Kotlin.
This project follows **TDD** — write the failing test first.
