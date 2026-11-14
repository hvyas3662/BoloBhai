# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**Bolo Bhai** — a local, offline Hinglish voice-dictation desktop app for macOS & Windows. Kotlin Multiplatform + Compose Multiplatform Desktop (JVM). Hold a hotkey anywhere → record → transcribe with whisper.cpp → apply dictionary corrections → clean up with a local LLM (llama.cpp) using a Markdown "profile" as the system prompt → paste into the focused app → save to History.

**`BoloBhai.md` is the complete Phase-1 spec and source of truth.** When behavior is unclear or you need exact strings/sizes/colors/edge cases, read it — section references (`§6.3`, `§13.1`, etc.) appear throughout the code. Where the spec and your defaults disagree, follow the spec.

## Commands

Use the Gradle wrapper (`./gradlew`, or `gradle` if the wrapper can't fetch its distribution). JDK 17+ (built on Temurin 21).

```bash
./gradlew :composeApp:run                    # launch the app
./gradlew :composeApp:desktopTest            # run all unit tests
./gradlew :composeApp:createDistributable    # runnable app image
./gradlew :composeApp:packageMsi             # Windows per-user installer
./gradlew :composeApp:packageDmg             # macOS installer
```

Run a single test class/method:
```bash
./gradlew :composeApp:desktopTest --tests "com.bolobhai.domain.HotkeyStateMachineTest"
./gradlew :composeApp:desktopTest --tests "com.bolobhai.domain.HotkeyStateMachineTest.double tap enters hands-free"
```

**This project follows TDD** (spec §18) — write the failing test first. Tests live in `commonTest` (pure logic, the bulk) and `desktopTest` (JVM/JNA-specific, e.g. `WinKeysTest`, `MacHelperTest`).

## Architecture

**Single source of truth:** one `AppState` (`app/AppState.kt`) exposes `StateFlow`s that drive every window — main window, the floating recording pill, and the tray menu. All three read the same state; there is no per-window state.

**Layering (enforced by source set, not just convention):**
- `commonMain/domain/` — **pure Kotlin, no UI or platform imports**, so it's unit-testable. Houses the business logic: `HotkeyStateMachine` (hold / double-tap-hands-free / digit-swallow / Esc state machine, §6.3), `DictionaryApply`, `TextCleanup` + `LlmCleanup` (strip LLM preambles/`<think>`), `Profile` parse/write, `HistoryView`, `ModelView`, `Retention`, `GpuBackend`.
- `commonMain/data/` — repos + persistence: `SettingsRepo` (JSON), `ProfilesRepo` + `ProfilesWatcher` (WatchService hot-reload of `profiles/*.md`), `HistoryRepo` (SQLite + FTS5), `DictionaryRepo` (YAML), `ModelRepo`/`ModelCatalog` + `SidecarRepo`/`SidecarCatalog` (resumable downloads with SHA-256).
- `commonMain/sidecar/` — HTTP clients for the two local inference servers: `WhisperClient`, `LlamaClient`, and `ProcessSupervisor` (health-check + restart with back-off).
- `commonMain/ui/` — `theme/` (design tokens from §12, exact colors in `Tokens.kt`), `components/`, `screens/`, `pill/`, plus `Shell`, `Sidebar`, `OnboardingScreen`.
- `desktopMain/` — the only place with platform/JVM code: `Main.kt` (entry point — wires everything, owns the tray + windows), `app/HotkeyController` (orchestrates the record→transcribe→clean→paste pipeline, §9), `platform/win/*` (JNA: `WinKeyboardHook` LL hook, `WinPaste` SendInput), `platform/mac/MacHelper` (talks to the Swift helper over JSON-lines), `platform/Sidecars.kt` + launchers, `GpuDetect`.

**The pipeline** (`HotkeyController` + `domain`): gesture → `AudioRecorder` (16 kHz mono PCM) → `WhisperClient` → `DictionaryApply` → if post-processing on, `LlamaClient` with the active profile as system prompt (15 s timeout → fall back to raw text) → paste → save a `history` row + `history_version`. Read §9 for exact failure handling.

**Sidecars are external, not in git.** Two local HTTP servers (whisper-server, llama-server from whisper.cpp/llama.cpp) run as child processes on random free localhost ports, started at launch and killed on quit. They live in `sidecars/<os>/{whisper,llama}/` (each engine its own subfolder — they ship incompatible `ggml` versions). Compose bundles the matching OS folder via `appResourcesRootDir`. See `sidecars/README.md` for how to re-fetch the binaries on a fresh clone. **Models** (whisper + LLM `.gguf`) are never bundled — downloaded on first run into `<app-data>/models/`.

**App data** lives in `%APPDATA%\BoloBhai\` (Windows) / `~/.bolobhai/` (macOS): `settings.json`, `profiles/*.md`, `dictionary.yaml`, `history.db`, `audio/`, `models/`, `sidecars/`.

## Platform status

Windows core is implemented (LL keyboard hook, SendInput paste, MSI). **macOS core (M2) is not done** — it needs a Swift helper built on a Mac (`native/mac-helper/`); `MacHelper.kt` is the Kotlin side of the JSON-lines bridge. Without sidecar binaries + downloaded models, the app runs but degrades gracefully ("Speech engine not installed").

# Skill precedence
PL Coding Skills (android-*, kmp-*, design, pr-quiz) always take priority. When their architecture/patterns conflict with ponytail's minimalism, follow the PL skill's pattern even if it means more code. Ponytail applies only on top of that — trimming genuine bloat without breaking the prescribed pattern.

# gstack
Use the `/browse` skill from gstack for all web browsing. Never use `mcp__claude-in-chrome__*` tools.

# Behavioral guidelines

Reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
