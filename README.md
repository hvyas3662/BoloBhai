# Bolo Bhai

Local, offline Hinglish voice-dictation desktop app for macOS & Windows.
Kotlin Multiplatform + Compose Multiplatform Desktop (JVM). See `BoloBhai.md` for the full spec.

> **Tagline:** "Bolo Bhai, baaki hum likh denge."

## Status

Built milestone-by-milestone (see `BoloBhai.md` §19); the app runs after each.

- **M1 Skeleton & design system** — tokens, fonts, sidebar shell, tray, settings persistence.
- **Windows core** — global LL keyboard hook (Alt / Right Ctrl / Fn probe), recording pill (all
  states), 16 kHz recorder, whisper-server sidecar client, SendInput Ctrl+V paste, MSI packaging.
- **M3 Post-processing & profiles** — llama-server sidecar, profiles folder + hot-reload +
  defaults, Quick Profiles 1–5, 15 s LLM-timeout fallback.
- **M4 History** — SQLite + FTS5, day-grouped cards, filter chips, search, versions, show-original,
  audio player (waveform + seek), More menu (re-process / re-transcribe / save .wav / delete), Retry.
- **M5 Dictionary / Models / Settings / Onboarding** — `dictionary.yaml` (prompt + corrections),
  model catalog with resumable download + SHA-256, retention job, launch-at-login, 5-step onboarding.
- **M7 Polish** — start/stop sounds, pill fade/slide-in, DMG/MSI config, this README.

Not yet done: **M2 Mac core** (Swift helper, event tap, Cmd+V — needs a Mac).

Dictation needs the sidecar binaries + downloaded models (below); without them the pill shows
"Speech engine not installed" and the app degrades gracefully.

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

## Sidecars & models

Two local HTTP servers are launched as child processes; they are **not** bundled in the repo:

- Place the binaries at `sidecars/<os>-<arch>/` (e.g. `sidecars/windows-x64/whisper-server.exe`
  and `llama-server.exe`). For packaging, point Compose's `appResourcesRootDir` at this folder so
  they ship inside the installer.
- Models are downloaded on first run (Onboarding → Models) from Hugging Face into
  `<app-data>/models/`, with resume (`.part` + HTTP Range) and SHA-256 verification. Catalog:
  `composeApp/src/desktopMain/resources/models.json`.

App data lives in `%APPDATA%\BoloBhai\` (Windows) / `~/.bolobhai/` (macOS): `settings.json`,
`profiles/*.md`, `dictionary.yaml`, `history.db`, `audio/`, `models/`.

## Packaging

```bash
./gradlew :composeApp:packageMsi   # Windows: per-user MSI, Start-menu shortcut
./gradlew :composeApp:packageDmg   # macOS: DMG
```

Icons live in `resources/icons/` (`.ico`, `.icns`, 1024 px PNG). macOS release builds should be
signed + notarized with the audio-input entitlement and `NSMicrophoneUsageDescription`; unsigned
dev builds still run. Models are never bundled.

## Module layout

```
composeApp/
  commonMain/   app/ (AppState, ModelsController, Screen)
                data/ (Settings, Profiles, History (SQLite), Dictionary, ModelCatalog/Repo)
                domain/ (pure: state machine, text/dictionary/history/model helpers)
                ui/theme  ui/components  ui/screens  ui/ (Sidebar, Shell, Onboarding)
                audio/ (recorder, WAV, waveform, sounds)
  desktopMain/  Main.kt (entry, tray, window), platform/ (Platform, win/*), sidecar/, app/HotkeyController
  commonTest/   unit tests (TDD) — 129 tests
```

Architecture: one `AppState` (`StateFlow`s) drives the UI; `domain/` is pure Kotlin.
This project follows **TDD** — write the failing test first.
