# Bolo Bhai — Local Voice Dictation App for macOS & Windows (Phase 1 spec, v2)

**App name:** Bolo Bhai
**Tagline:** "Bolo Bhai, baaki hum likh denge."
**Bundle ID:** `com.elevationsoft.bolobhai.app`
**Platforms:** macOS (Apple Silicon first, Intel nice-to-have) and Windows 10/11 x64. **Desktop only — no mobile.** All windows are **landscape**.

> **For the Claude Code instance reading this:** this file is the complete source of truth for Phase 1.
> Build everything in it. Where this file and your defaults disagree, follow this file.
> A visual reference exists as a Claude Design canvas ("Bolo Bhai — Desktop App",
> https://claude.ai/artifact/QEc8RRaVc6X3u7j1NBAX69 — private to the owner unless shared). **You do not need it:**
> every screen, state, color, size and string from that design is written out below (§12–§14).
> Work milestone by milestone (§19), keep the app runnable after each one, and tick the acceptance checklist (§20).

---

## 1. What the app does

The user speaks — mostly **Hinglish** (Hindi + English) — anywhere on their computer:

1. Presses a hotkey (**fn** on Mac, **Alt** on Windows) → a small floating **pill** appears and recording starts.
2. While recording, pressing **1 / 2 / 3 / 4 / 5** switches the post-processing profile (Quick Profiles).
3. On stop, audio is transcribed locally by **whisper.cpp**.
4. Dictionary corrections are applied.
5. The text is cleaned up locally by a small **LLM** (llama.cpp `llama-server`) using the selected **Markdown profile** as the system prompt.
6. The result is **pasted into whatever app the user was typing in**.
7. Everything (audio, raw transcript, every processed version, profile used) is saved and browsable in **History**.

Everything runs **offline** after the one-time model download. No cloud, no paid APIs, no telemetry.

Target machine: Apple M1 Pro 16 GB (and comparable 16 GB Windows laptops).
Target latency: **< 3 s** from key release to paste for a 10 s recording on M1 Pro.

---

## 2. Tech stack

| Area | Choice |
|---|---|
| Language / UI | **Kotlin Multiplatform, Compose Multiplatform Desktop (JVM)**, Material 3 as base, fully custom-themed (§12) |
| Build | Gradle (Kotlin DSL), `org.jetbrains.compose`, `nativeDistributions` → `BoloBhai.dmg`, `BoloBhai.msi` |
| Speech-to-text | **whisper.cpp**, bundled as a sidecar binary `whisper-server` (from whisper.cpp `examples/server`), called over HTTP on `127.0.0.1:<random free port>`. Metal on macOS; Vulkan (fallback CPU) on Windows. |
| Post-processing LLM | **llama.cpp `llama-server`** sidecar, OpenAI-compatible `POST /v1/chat/completions` on `127.0.0.1:<random free port>`. Kept warm (model loaded) while the app runs. |
| Audio capture | `javax.sound.sampled` → **16 kHz, mono, 16-bit PCM**. Live RMS level for the waveform. |
| Native – macOS | Small **Swift helper** executable (`bolobhai-mac-helper`) bundled in the .app, JSON-lines over stdin/stdout (§6.4). |
| Native – Windows | **JNA**: `SetWindowsHookEx(WH_KEYBOARD_LL)` + Raw Input for Fn detection, `SendInput` for paste. |
| Storage | **SQLite** (`org.xerial:sqlite-jdbc`) with FTS5 for search; files for audio, profiles, dictionary, settings. |
| JSON / YAML | `kotlinx.serialization` (JSON), `kaml` (YAML for dictionary + profile frontmatter). |
| HTTP | Ktor client (CIO). |
| Coroutines | `kotlinx.coroutines`; one `AppScope` + `Dispatchers.IO` for sidecars/DB. |
| File watching | `java.nio.file.WatchService` on the profiles folder (debounce 300 ms). |
| Fonts (bundled) | **Inter** (400/500/600/700), **Space Grotesk** (500/600/700), **JetBrains Mono** (500/600). All OFL — ship the .ttf files in resources. |

### 2.1 Models (downloaded on first run from Hugging Face)

| Kind | Name | File | Size | RAM | Default |
|---|---|---|---|---|---|
| STT | Whisper large-v3-turbo | `ggml-large-v3-turbo.bin` — `https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo.bin` | ~1.6 GB | ~2 GB | **yes** |
| STT | Whisper large-v3-turbo q5_0 | `ggml-large-v3-turbo-q5_0.bin` — same repo | ~550 MB | ~1 GB | |
| LLM | Qwen3-4B-Instruct-2507 Q4_K_M | `Qwen3-4B-Instruct-2507-Q4_K_M.gguf` — `https://huggingface.co/unsloth/Qwen3-4B-Instruct-2507-GGUF` | ~2.5 GB | 3–4 GB | **yes** |
| LLM | Gemma 3 4B IT Q4_K_M | `gemma-3-4b-it-Q4_K_M.gguf` — `https://huggingface.co/ggml-org/gemma-3-4b-it-GGUF` | ~2.5 GB | 3–4 GB | "Often better at Hindi" |

- Put the model catalog in `models.json` (in resources) so URLs can be fixed without code changes. **Verify the URLs at build time**; if one 404s, pick the equivalent file from the same org.
- **Checksum:** get the expected SHA-256 from the Hugging Face LFS metadata (`HEAD` the resolve URL → `x-linked-etag` header holds the LFS sha256) and verify after download. Store it next to the file as `<file>.sha256`.
- **Resume:** download to `<file>.part` with HTTP `Range` requests; resume on restart or after Pause.
- Use the **Instruct (non-thinking)** models. LLM settings: `temperature 0.2`, `max_tokens 1024`, `top_p 0.9`, timeout **15 s**.

---

## 3. Folder layout on disk

| | macOS | Windows |
|---|---|---|
| App data root | `~/.bolobhai/` | `%APPDATA%\BoloBhai\` |
| Profiles | `<root>/profiles/*.md` | same |
| Dictionary | `<root>/dictionary.yaml` | same |
| Settings | `<root>/settings.json` | same |
| History DB | `<root>/history.db` | same |
| Audio | `<root>/audio/{id}.wav` | same |
| Models | `<root>/models/` | same |
| Logs | `<root>/logs/bolobhai.log` (rolling, 5 × 2 MB) | same |

---

## 4. Code structure (suggested)

```
bolobhai/
  build.gradle.kts, settings.gradle.kts
  composeApp/src/
    commonMain/kotlin/com/bolobhai/
      app/            App.kt, AppState.kt (single source of truth, StateFlow), Navigation.kt
      ui/theme/       Tokens.kt (colors §12.1), Type.kt, Shapes.kt, BoloTheme.kt
      ui/components/  Buttons, Chip, FilterChip, SlotKey, Switch, Segmented, Toast, ConfirmDialog, Menu, Waveform, Kbd
      ui/screens/     HistoryScreen, QuickProfilesScreen, AllProfilesScreen, DictionaryScreen,
                      ModelsScreen, SettingsScreen, onboarding/*
      ui/pill/        RecordingPill.kt (+ PillWindow)
      domain/         Pipeline.kt, HotkeyStateMachine.kt, ProfileFormatter.kt, Dictionary.kt
      data/           HistoryRepo (SQLite), ProfilesRepo (files+watcher), SettingsRepo, DictionaryRepo, ModelRepo
      sidecar/        WhisperServer.kt, LlamaServer.kt, ProcessSupervisor.kt
      audio/          Recorder.kt, Player.kt, WavIO.kt
    desktopMain/kotlin/com/bolobhai/platform/
      Platform.kt (expect/actual style interfaces)
      mac/            MacHelperBridge.kt (talks to Swift helper), MacPaste.kt, MacPermissions.kt, MacLogin.kt
      win/            WinKeyboardHook.kt (JNA), WinFnProbe.kt (Raw Input), WinPaste.kt, WinLogin.kt
  native/mac-helper/  Package.swift, Sources/main.swift
  sidecars/           macos-arm64/{whisper-server,llama-server}, windows-x64/{whisper-server.exe,llama-server.exe,*.dll}
  resources/          fonts/, icons/ (app icon .icns/.ico/.png), sounds/start.wav, sounds/stop.wav,
                      default-profiles/*.md, models.json, dictionary.default.yaml
```

Architecture: one `AppState` (Kotlin `StateFlow`s) drives all windows: main window, recording pill window, tray. Business logic in `domain/` has no UI or platform imports so it can be unit-tested.

---

## 5. Windows of the app

1. **Main window** — custom window chrome is NOT required; use the standard OS title bar. Default **1440×900**, minimum **1100×700**, resizable, landscape layout only (sidebar + content). Title "Bolo Bhai". It is optional for dictation: closing it only hides it, and the app keeps running in the tray / menu bar with hotkeys active (§6.0).
2. **Recording pill window** — tiny floating window, always on top, **never takes focus** (§8).
3. **Tray / menu-bar icon + menu** (§13.9).
4. **Onboarding** — shown inside the main window on first run (§13.8).

The app has no Dock icon on macOS while the main window is closed (`LSUIElement`-style behaviour via `NSApp.setActivationPolicy(.accessory)` from the helper, switch to `.regular` while the main window is open). On Windows it lives in the system tray.

---

## 6. Hotkeys (core behaviour — implement exactly)

### 6.0 Global, background-first (must-have)

Bolo Bhai is a **background tray / menu-bar app**. The hotkeys are **system-wide** and must work **when the app is not in the foreground**:

- The main window is minimised, hidden behind other apps, or **closed** → fn / Alt still work everywhere.
- The app was started at login and **the main window was never opened** → fn / Alt still work. Only the menu-bar (Mac) / system-tray (Windows) icon is present.
- The user is typing in any other app (browser, WhatsApp Desktop, VS Code, Word, Slack…) → hold / double-tap fn or Alt records, and the result is pasted **into that app**. Bolo Bhai never comes to the front, and the pill never takes focus.
- Closing the main window (red ✕ on Mac, ✕ on Windows) **only hides it**. The process, the hotkey listener and both sidecars keep running. The app quits only via **Quit Bolo Bhai** in the tray menu (or Cmd+Q while the main window is focused; that still asks "Quit Bolo Bhai? Hotkeys will stop working." with a "Keep running in menu bar" default).
- Launch at login (default ON) starts the app **hidden**, straight into the tray. No window is shown unless onboarding is not done yet.
- The hotkey listener lives in its own process/thread (Swift helper on Mac, LL-hook thread on Windows). It does not depend on any Compose window existing or being focused.
- If the listener dies (helper crash, hook removed by Windows after a timeout), the supervisor restarts it within 1 s and logs it. The tray icon briefly shows a warning badge.
- Windows: the LL hook callback must return within a few ms (Windows silently removes slow hooks), so it only posts events to a queue and never does work inline. Re-install the hook on `WM_POWERBROADCAST` resume and on session unlock.
- macOS: after sleep/wake or a permission change, re-create the event tap if `CGEvent.tapIsEnabled` is false (also handle `kCGEventTapDisabledByTimeout` / `ByUserInput` by re-enabling).
- Only one instance may run. A second launch just shows the main window of the running instance (single-instance lock file + local socket).

Tray / menu bar is therefore the app's home:
- **macOS:** `NSStatusItem` in the menu bar (created by the Swift helper or Compose `Tray`). Template mic icon; it turns red while recording. No Dock icon while the main window is closed (activation policy `.accessory`). It becomes `.regular` while the main window is open.
- **Windows:** notification-area (system tray) icon with the same menu. Single left-click opens the menu, double-click opens the main window. On first run show a one-time balloon: "Bolo Bhai is running in the tray. Hold Alt anywhere to dictate."
- (iOS / Android are **not** targets. "Mac" here means macOS desktop only.)

### 6.1 Keys

| Platform | Dictation key | Notes |
|---|---|---|
| macOS | **fn** (Globe) | Only option. Detect `flagsChanged` with `kCGEventFlagMaskSecondaryFn`. |
| Windows | **Fn** if detectable, else **Alt** (default after fallback), or **Right Ctrl** | User picks in Settings (segmented: Fn / Alt / Right Ctrl). |

Also: **Esc** = cancel recording. **1–5** (top row and numpad) = switch profile **while recording**.

### 6.2 Gestures

- **Hold** the key → records while held. **Release → recording stops and processing starts automatically.**
- **Double-tap** the key (two presses within the double-tap window, default **400 ms**, configurable 250–600 ms) → **hands-free**: user lifts the finger, recording continues. **Press the key once more → stop & process.**
- **1 / 2 / 3 / 4 / 5 during recording** (hold or hands-free) → switch the active Quick Profile immediately. The key event is **swallowed** (must not type into the focused app). Ignored if that slot is empty.
- **Esc during recording** → cancel, discard audio, swallowed.
- Digits and Esc are **not** intercepted when not recording.

### 6.3 State machine (`HotkeyStateMachine.kt`, pure Kotlin, unit-tested)

Constants: `TAP_MAX_MS = 280`, `DOUBLE_TAP_MS = settings.doubleTapMs (default 400)`, `MIN_RECORDING_MS = 500`.

```
Idle
  keyDown(K)            -> startRecording(mode=Hold); showPill(Hold); t0=now; state=HoldDown
HoldDown
  keyUp(K) & now-t0 >= TAP_MAX_MS  -> stopAndProcess(); state=Processing
  keyUp(K) & now-t0 <  TAP_MAX_MS  -> keep recording silently; hide nothing yet;
                                       startTimer(DOUBLE_TAP_MS); state=TapWait
TapWait
  keyDown(K) before timer -> mode=HandsFree; pill -> HandsFree; swallowNextKeyUp=true; state=HandsFree
  timer fires             -> cancelRecording(silent); hidePill(); toast "Too short · hold to talk, or double-tap for hands-free"; state=Idle
HandsFree
  keyUp(K) (the 2nd tap's release) -> ignore
  keyDown(K)              -> stopAndProcess(); swallowNextKeyUp=true; state=Processing
Any recording state (HoldDown, TapWait, HandsFree)
  digit 1..5              -> if slot assigned: activeSlot=n; pill pulses that key   (event swallowed)
  Esc                     -> cancelRecording(); toast "Recording cancelled"; state=Idle (swallowed)
Processing
  all hotkey input ignored until pipeline finishes -> state=Idle
stopAndProcess(): if recorded < MIN_RECORDING_MS -> discard + toast "Too short"; else run pipeline (§9)
```

Audio from the first tap is kept, so a hands-free recording starts at the first press.

### 6.4 macOS implementation (Swift helper)

`bolobhai-mac-helper` is a small Swift command-line tool launched by the app; protocol = one JSON object per line.

- Event tap: `CGEvent.tapCreate(tap: .cgSessionEventTap, place: .headInsertEventTap, options: .defaultTap, eventsOfInterest: flagsChanged | keyDown | keyUp)`.
  - `flagsChanged` with `.maskSecondaryFn` toggling → emit `{"ev":"fn","down":true|false,"t":<ms>}`.
  - While the app has told the helper `{"cmd":"capture","on":true}` (= recording), keyDown/keyUp of keycodes for 1–5 (18,19,20,21,23 and keypad 83–87) and Esc (53) → emit `{"ev":"key","k":"1".."5"|"esc"}` and **return nil** (swallow).
- Paste: `{"cmd":"paste"}` → post Cmd+V via `CGEvent(keyboardEventSource:virtualKey:0x09,keyDown:)` with `.maskCommand`.
- Permissions: `{"cmd":"perm","which":"accessibility"|"input"|"mic"}` → returns status; `{"cmd":"openPrefs","pane":...}` opens the right System Settings pane (`x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility`, `...Privacy_ListenEvent`, `...Privacy_Microphone`).
- Activation policy switching (`.accessory` / `.regular`) on command.
- Needs **Accessibility** + **Input Monitoring**; Info.plist has `NSMicrophoneUsageDescription` = "Bolo Bhai records your voice to turn it into text, on this Mac."
- Onboarding tells the user to set System Settings › Keyboard › "Press 🌐 key to" = **Do Nothing**.

### 6.5 Windows implementation (JNA)

- `WH_KEYBOARD_LL` hook on a dedicated thread with a message loop.
- **Alt mode:** track `VK_LMENU`/`VK_RMENU`. Alt alone must **not** open the focused app's menu bar: on Alt key-up after a Bolo Bhai gesture, swallow it and inject a harmless key (`VK 0xFF` down/up, or a Ctrl tap) before releasing Alt so Windows doesn't trigger the menu. Alt combined with any other non-1–5 key (Alt+Tab, Alt+F4 …) must pass through untouched and cancel any gesture in progress.
- **Right Ctrl mode:** `VK_RCONTROL`, same logic.
- **Fn mode:** most laptops handle Fn in firmware. Try (a) LL hook for vendor scan codes, (b) Raw Input (`RegisterRawInputDevices`, usage page 0x01/0x06 and vendor HID pages). The onboarding "Press Fn twice to test" (§13.8 step 3) listens for 5 s; if nothing arrives → **fall back to Alt automatically** and say so.
- Digits 1–5 / numpad 1–5 / Esc swallowed only while recording (`return 1` from the hook).
- Paste via `SendInput` Ctrl+V; make sure no modifier is still logically down first.

---

## 7. Quick Profiles 1–5 (new in v2)

- Five **slots**, keys **1–5**. Each slot holds a reference to one `.md` profile, or is **empty**.
- Default mapping: **1 = hinglish-clean.md (Default)**, **2 = numbered-list.md**, **3 = email.md**, **4 = whatsapp-message.md**, **5 = empty**. `meeting-notes.md` ships unassigned.
- `activeSlot` (default 1) is remembered across restarts; the last used profile stays active.
- A profile can be in at most one slot. Assigning it to a new slot removes it from the old one.
- Change the active profile from: the pill (press 1–5, or click a key), the sidebar "Active profile" box, the tray menu, the Quick Profiles screen ("Use now").
- If the active slot gets cleared → active falls back to slot 1 (or the first non-empty slot).
- Stored in `settings.json` → `quickSlots: {"1":"hinglish-clean.md",...,"5":null}`, `activeSlot: 1`.

---

## 8. Floating recording pill (compact — must cover very little screen)

A single rounded **pill**, height **48 px**, auto width (~460 px max), radius 999, at **bottom-centre of the screen that contains the mouse cursor**, 24 px above the visible-area bottom (above Dock/taskbar).

**Never steals focus:** Compose `Window(undecorated=true, transparent=true, alwaysOnTop=true, focusable=false, resizable=false)`; set `window.focusableWindowState = false` and `isAutoRequestFocus = false`. On macOS additionally have the helper set the NSWindow to a non-activating panel level (`.statusBar`), `collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]`. Shadow `0 16 40 rgba(0,0,0,.5)` (dark) / `rgba(17,24,39,.22)` (light). Fade/slide-in 180 ms.

Layout left → right (gap 10 px, padding 0 7 0 16):

| Element | Spec |
|---|---|
| Rec dot | 8 px circle, `rec` color, pulsing opacity 1 ↔ .35, 1.2 s |
| Timer | JetBrains Mono 13/600, `m:ss`, min-width 36 |
| Mini waveform | 52×22 px, 14 bars, 2 px gap, driven by live mic RMS (smooth 60 ms) |
| Mode label | 11/600 `muted`: **"Release fn"** / **"Release Alt"** in hold, **"Hands-free"** in hands-free |
| Divider | 1×22 px `border` |
| Slot keys 1–5 | 24×24, radius 6, Mono 11/600. Active = `orange` fill + `onAccent` text; assigned = `card2` + 1 px `border`; empty = transparent + 1 px **dashed** border, muted, disabled. Clickable. On change: 250 ms pop (scale 1.35→1) + 3 px `orangeSoft` ring. |
| Profile name | 12/600, min-width 58: short name — Hinglish · List · Email · WhatsApp · Notes · (custom: profile name) · "Raw" when post-processing is off |
| Stop button | **only in hands-free**: 34 px circle, `blue` fill, white/onAccent rounded square icon. "Stop and process". |
| Cancel button | while recording: 34 px circle, `card2` fill, muted ✕. "Cancel recording (Esc)". |

States:

| State | Contents |
|---|---|
| Hold recording | dot · timer · waveform · "Release fn" · keys · name · ✕ |
| Hands-free recording | dot · timer · waveform · "Hands-free" · keys · name · ■ · ✕ |
| Processing | 14 px spinner (`blue` arc) · "Processing" · keys (locked, active highlighted) · name |
| Pasted | green check in 22 px `greenSoft` circle · "Pasted" · "Cmd+V"/"Ctrl+V" (mono, muted) · keys · name — auto-hide after **1.2 s** |
| Failed (LLM timeout/error) | red ⓘ · "LLM timeout · raw text pasted" · **Retry** pill button (red) — red border; auto-hide after **5 s**. Retry re-runs the LLM and updates the History item; it never re-pastes automatically. On success it copies the cleaned text to the clipboard and shows "Cleaned up · copied to clipboard". |

Optional start/stop sounds (`sounds/start.wav`, `stop.wav`, short soft clicks) when enabled in Settings.

---

## 9. Pipeline

```
hotkey gesture (§6)
 -> record 16 kHz mono PCM (in memory + streamed to temp file)
 -> stop
 -> whisper-server  POST /inference  (language = settings.language (default "hi"),
                                      prompt/initial_prompt = custom words joined by ", ",
                                      temperature 0, response_format=json)
 -> dictionary corrections (case-insensitive whole-word "wrong -> right"), then
    custom-word casing fix (whole-word, case-insensitive -> exact casing)
 -> if post-processing ON and active slot has a profile:
      llama-server /v1/chat/completions
        system = profile body (frontmatter stripped)
        user   = corrected transcript
        temperature 0.2, max_tokens 1024, timeout 15 s
      strip surrounding quotes / "Here is…" preambles / <think> blocks defensively
    else: output = corrected transcript (first letter capitalised)
 -> paste into the previously focused app (§10)
 -> save to History (audio + raw + version 1 + profile) and show pill "Pasted"
```

Failure handling:
- **LLM fails or > 15 s** → paste the corrected raw transcript, pill "Failed" state, History status `llm_failed` (card shows red Retry).
- **Whisper fails** → nothing pasted, toast "Couldn't transcribe — audio saved to History", status `stt_failed` (card offers Re-transcribe).
- Empty transcript → nothing pasted, toast "Didn't catch that", not saved.
- Language `auto` is allowed but shows a warning (auto-detect can switch to Spanish/Urdu on short Hinglish clips). Never auto-detect by default.

Sidecar supervisor: start both servers at app launch (after models exist), health-check (`GET /health`), restart on crash with back-off (1 s, 2 s, 5 s, max 3 tries, then show a banner in the main window "Speech engine stopped — Restart"). Choosing a different model in Models restarts the relevant server.

---

## 10. Text insertion (paste)

1. Remember clipboard contents (text; also image/files best-effort).
2. Put the result text on the clipboard.
3. Simulate **Cmd+V** (mac, via helper) / **Ctrl+V** (Windows, `SendInput`) into the app that had focus when recording started (the pill never takes focus, so focus is unchanged).
4. Wait ~250 ms, restore the previous clipboard.
5. If Accessibility permission is missing on Mac → copy to clipboard instead and toast "Copied — enable Accessibility to auto-paste".

---

## 11. Instruction profiles (Markdown files)

- Folder: `~/.bolobhai/profiles/` / `%APPDATA%\BoloBhai\profiles\`. Each `.md` = one profile; **profile id = file name**.
- Optional YAML frontmatter:
  ```
  ---
  name: Hinglish Notes
  ---
  (body = system prompt for the LLM)
  ```
  (An optional `emoji:` key may exist in user files — read it but the UI does not show emoji; it uses the slot number instead.)
- **Hot reload** with WatchService (add / edit / delete / rename). If the file of an assigned slot disappears → slot becomes empty and a toast says so.
- Renaming the display name in the app rewrites the frontmatter `name` and renames the file to `slug(name).md` (keep slot mapping).
- Ship these defaults (copy on first run only if the folder is empty):

**hinglish-clean.md**
```
---
name: Hinglish Clean
---
I speak Hinglish (Hindi + English). The input may be in Devanagari.
- Write everything in Roman (English) letters. Never use Devanagari.
  Example: "कल मीटिंग है" -> "kal meeting hai".
- Do not translate. Keep Hindi words as I said them.
- I only speak Hindi and English. Never output any other language. If text looks like
  another language, it is a recognition mistake: rewrite it as the Hinglish I most likely said.
- Fix punctuation and obvious recognition mistakes. Keep names and tech terms exact.
- If I count items (first/second/third, pehla/dusra/teesra, 1/2/3), write a numbered list,
  one item per line, without the counting words.
- Follow my spoken corrections.
- Return only the final text. No explanation.
```
**numbered-list.md**
```
---
name: Numbered List
---
Turn what I said into a clean numbered list.
- One item per line.
- Remove counting words (pehla, dusra, teesra, first, second).
- If I said a heading before the items, put it on the first line followed by a colon.
- Keep Hinglish in Roman letters. Do not translate.
- Return only the list.
```
**email.md**
```
---
name: Email
---
Turn my dictation into a short, polite email.
- Add a greeting and a sign-off.
- Keep names, dates and numbers exact.
- Keep my language mix (Hinglish stays Hinglish in Roman letters) unless I speak only English.
- Return only the email body.
```
**whatsapp-message.md**
```
---
name: WhatsApp Message
---
Write a casual WhatsApp message in Roman Hinglish, the way I said it.
- Short sentences. No formal greeting.
- Return only the message.
```
**meeting-notes.md**
```
---
name: Meeting Notes
---
Turn what I said into short meeting notes.
- One bullet per point, starting with "- ".
- Keep owners and dates exact.
- Return only the notes, with "Notes:" as the first line.
```

---

## 12. Design system

Look: calm, dark-navy-first, taken from the app icon (navy tile, blue→orange mic and sound waves). Two accents: **blue** = primary actions/selection, **orange** = Quick Profile / active slot. No gradients, no emoji in UI, inline stroke icons (2 px, round caps) — use a Lucide-style icon set or hand-made `ImageVector`s.

### 12.1 Color tokens (exact)

| Token | Dark | Light |
|---|---|---|
| bg | `#0B1020` | `#F2F4F8` |
| side (sidebar) | `#10172A` | `#FFFFFF` |
| panel (pill, popups) | `#131B30` | `#FFFFFF` |
| card | `#151D33` | `#FFFFFF` |
| card2 (raised/inner) | `#1B2440` | `#F4F6FA` |
| border | `#263152` | `#DCE2EC` |
| text | `#E9EEF8` | `#111827` |
| muted | `#9EAAC4` | `#525D74` |
| input | `#0F1629` | `#F7F9FC` |
| blue | `#53C4F4` | `#0A6FAD` |
| blueSoft | `rgba(83,196,244,.14)` | `rgba(10,111,173,.10)` |
| orange | `#F6964F` | `#A64B0A` |
| orangeSoft | `rgba(246,150,79,.14)` | `rgba(166,75,10,.10)` |
| onAccent (text on blue/orange) | `#06111F` | `#FFFFFF` |
| red | `#FF7E7E` | `#B42323` |
| redBorder | `rgba(255,126,126,.35)` | `rgba(180,35,35,.35)` |
| onRed | `#2A0707` | `#FFFFFF` |
| green | `#4ADE80` | `#1F8A4C` |
| rec (recording dot) | `#FF5F5F` | `#D12C2C` |
| shadow | `rgba(0,0,0,.5)` | `rgba(17,24,39,.16)` |
| overlay (modal scrim) | `rgba(4,7,15,.6)` | `rgba(17,24,39,.35)` |

Theme setting: **Dark (default) / Light / System** (System follows OS appearance live).

### 12.2 Typography
- Headings: **Space Grotesk** — H1 28/700, letter-spacing −0.02em; H2 17/600; onboarding title 40/700.
- Body/UI: **Inter** — 14/400–500 body, 13 buttons, 12 meta, section labels 12/600 UPPERCASE +0.06em `muted`.
- Mono: **JetBrains Mono** — keys, kbd, timers, file paths (11–13).

### 12.3 Shape, spacing, components
- Radii: cards 14, big panels 16–20, buttons/inputs 10, chips 10 (slot chips) / 999 (filter chips, pill), keys 6–7, kbd 6.
- Spacing: content padding 24/32, card padding 18/20, gaps 8/10/12/16/20.
- **Button** 38 px tall (large 44), padding 0 14; variants: default (`card2` + border), primary (`blue`), orange, danger (`red`), ghost.
- **Input** 40 px, `input` bg, 1 px border, focus ring 2 px `blue`.
- **Kbd** mono 12, padding 3/8, `card2` + border.
- **Switch** 44×26, knob 20, on = `blue`.
- **Segmented** container `input` + border, items 32 px, selected `card2` + subtle shadow.
- **Slot key** square with number (sizes 22/24/26/40/44 depending on place).
- **Filter chip** (History) 36 px, radius 999, label + count bubble; selected = `blue` fill; "Failed" chip text `red`, selected = `red` fill.
- **Toast** bottom-centre of main window, `text` color background with `bg` text, check icon, 13/600, auto-hide 2.2 s (sits above the pill area when recording).
- **Confirm dialog** 420 px card, title (H2 19), body muted, buttons Cancel + danger.
- Hover states on everything clickable; keyboard focus visible; all icon-only buttons have tooltips + accessibility names. Contrast ≥ 4.5:1 for text.

---

## 13. Screens (main window)

### 13.0 Shell
- **Sidebar** 264 px, `side` bg, right border.
  1. Logo (40 px app icon, radius 10) + "Bolo Bhai" (Space Grotesk 18/700) + "baaki hum likh denge" (12 muted).
  2. **Record button** (primary, large, full width): label **"Hold to talk"** + kbd `fn`/`Alt`; under it 12 px muted hint "Double-click for hands-free · like fn". Mouse press-and-hold = hold gesture, double-click = hands-free, click while hands-free = stop. Label changes: "Release to process" / "Press to stop" / "Processing…".
  3. Nav (44 px rows, icon + label + mono counter on the right): **History** (item count), **Quick Profiles** ("1–5"), **All Profiles** (count), **Dictionary**, **Models**, **Settings**. Selected = `blueSoft` bg + `blue` text.
  4. Bottom: **Active profile** card — label, row of five 36 px slot keys (active orange; empty dashed → click opens Quick Profiles on that slot), active name "2 · Numbered List".
  5. Status line: green dot + "Offline · Qwen3 warm" (shows the active LLM; "Speech engine starting…" while sidecars boot) + 44 px **theme toggle** (sun in dark, moon in light).

### 13.1 History (home)
Header (padding 24/32, bottom border):
- H1 "History" + muted "N dictations" + search field right (300 px, magnifier icon, placeholder "Search output or original…").
- **Filter chips row — all options, "All" selected by default:**
  `All (n)` · one chip per profile with its slot key badge if assigned and count (e.g. `[1] Hinglish Clean 2`, `[2] Numbered List 1`, `[3] Email 1`, `[4] WhatsApp Message 1`, `Meeting Notes 1`) · `Raw (no LLM)` (only if such items exist) · `Failed (n)` (red).
  Count = items whose **current version** used that profile. Filter + search combine. Filter resets to All after a new dictation.

List (scrolls): grouped by day — **Today**, **Yesterday**, then `Mon, 9 Nov` style dates — newest first. Empty states: "No dictations yet — Hold fn anywhere and start speaking…" with a "Start a recording" button; "Nothing matches — Try another word, or pick All above."

**Card** (radius 14, `card`, padding 18/20, gap 14):
- Meta row 12 muted: profile tag (`blueSoft` pill with slot number badge + profile name) · time `10:42 AM` · duration `0:14` · if >1 version: version tabs `v1 · Hinglish Clean` `v2 · Email` (28 px, selected `card2` + muted border).
- **Output** 15/1.6, pre-line, **max 3 lines**, click to expand/collapse.
- Failed card: red-tinted border, output = raw transcript in muted, row: red ⓘ "Post-processing failed · raw transcript was pasted" + red **Retry** button (re-runs LLM with the same profile; on success status → ok).
- **Show original** panel (dashed border, `card2`): label "ORIGINAL · WHISPER", own Copy button, raw transcript muted.
- **Audio player** (when playing): 40 px round blue play/pause, 48-bar waveform from the real audio (clickable to seek), mono time `0:05 / 0:14`. Only one item plays at a time.
- Action row: **Copy output** (toast "Copied output") · **Show original / Hide original** · **Play audio / Pause** · **⋯ More** (right).
- **More menu** (260 px popover): **Re-process with profile ▸** (expands inline list of all profiles with slot badge → runs LLM on the same raw transcript, saves as a **new version** on the same card and selects it, toast "Re-processed with Email · saved as v2") · **Re-transcribe audio** (whisper again with current dictionary; updates raw, adds a new version with the current profile; disabled when audio isn't kept) · **Save audio as .wav** (native save dialog, default name `bolobhai-{id}.wav`) · **Delete item** (red → confirm dialog "Delete this dictation? The output, original transcript, all versions and the audio file will be removed.").

### 13.2 Quick Profiles
- H1 "Quick Profiles", sub "Five post-processing profiles on number keys. Switch in one tap, even mid-recording." Right: hint box "On Mac · while recording, press [1–5]" (orange kbd).
- Grid of **5 slot cards** (equal columns, min-height 150): big 40 px key, badge (Active / Default / Ready / Empty), profile name (Space Grotesk 16/600), file name mono 11, combo mono 11 "hold fn + 3". Selected card = `orangeSoft` bg + 2 px orange border; empty = dashed.
- **Editor panel** for the selected slot (card, 2 columns):
  - Left (300 px): big orange key + "EDITING SLOT" + name; **Profile in this slot** select ("— Empty —" + all profiles "Name · file.md"); **Display name** input; shortcuts box (macOS `hold fn + n`, Windows `hold Alt + n`, Hands-free `n`); buttons **Save slot n** (primary), **Use now**, **Clear slot** (keeps the file).
  - Right: "Instructions for the LLM" + mono path `~/.bolobhai/profiles/email.md`; monospace textarea (the whole .md incl. frontmatter); note "Saved as Markdown in the profiles folder. Edits made there reload here automatically."
  - Empty slot: dashed placeholder "Slot 5 khaali hai — Pick an existing profile on the left, or create a new one for this slot." + **New profile for slot 5**.
- Save writes the file immediately (debounced autosave 800 ms + explicit Save), toast "Saved email.md to slot 3".

### 13.3 All Profiles
- H1 "All Profiles", sub "Every .md file in ~/.bolobhai/profiles/". Buttons: **Open profiles folder** (Finder/Explorer), **New profile** (creates `new-profile.md` with a starter body, selects it).
- Left list (340 px): cards with slot badge (orange number or dashed "–"), name, file, "N uses". Selected = `blueSoft` + blue border.
- Right editor: **Name**, **File** (read-only, mono), **Quick slot** select (Not on a key / Slot 1…5), textarea "System prompt (Markdown, optional YAML frontmatter)", **Delete profile** (ghost red, confirm, disabled when only one profile left; file goes to OS trash), **Test on last dictation** (runs the real LLM on the newest raw transcript, shows a "PREVIEW" box), **Save**.

### 13.4 Dictionary (`dictionary.yaml`)
```yaml
customWords: [Triologic, Bolo Bhai, Kotlin, standup]
corrections:
  - { wrong: trilogic, right: Triologic }
  - { wrong: thales,   right: Tiles }
  - { wrong: almary,   right: Almirah }
```
- Two cards side by side: **Custom words** (chips with ✕, input "e.g. Kubernetes" + Add) — passed to Whisper as initial prompt; **Corrections** (rows `wrong` struck-through → `right` bold, ✕; add row: Wrong (heard) → Right + Add) — applied after transcription, before the LLM.
- **Try it** card: input with a raw transcript, live "After dictionary" result (`blueSoft` box).
- Changes save to YAML immediately; external edits hot-reload.

### 13.5 Models
- H1 "Models", sub "Everything runs on this computer · Metal on Apple Silicon (or Vulkan / CPU) · 4.1 GB used on disk".
- Two cards: **Speech to text** ("whisper.cpp · Metal") and **Post-processing LLM** ("llama-server on 127.0.0.1 · temperature 0.2 · kept warm"). Each model row: name + tag (Active in blue / note), meta "file · size · RAM", actions by state:
  - not downloaded → **Download**; downloading → progress bar + "46% · 1.15 of 2.50 GB · 41.6 MB/s" + **Pause**; paused → **Resume**; installed → **Use** + 🗑 delete (confirm "Frees 2.5 GB…"); active → no actions.
  - Using a model restarts its server; toast "Gemma 3 4B IT is now active · llama-server restarted". Download finished → toast "… downloaded · SHA-256 verified".
- Settings card: **Spoken language** select — Hindi / Hinglish (hi) [default], English (en), Auto-detect (not advised → orange warning line); **Post-processing with LLM** switch ("Off = paste the Whisper text straight away. Falls back to raw text after 15 s.").

### 13.6 Settings
H1 "Settings" + button **Run first-time setup again**. Cards in a 2-column grid:
- **Hotkey**: Dictation key segmented (Mac: `fn` only, with help "Turn off the Globe key action in System Settings › Keyboard."; Windows: `Fn | Alt | Right Ctrl`, help "Fn works only if your laptop reports it to Windows."); **Double-tap window** slider 250–600 ms step 25 (default 400) with mono value; reference list: Hold — Records while held, processes on release · Double-tap — Hands-free, press once more to stop · 1–5 while recording — Switch profile · Esc — Cancel recording.
- **Audio**: Microphone select (system input devices; default device first); **Start / stop sound** switch.
- **General**: Theme segmented Dark / Light / System; **Launch at login** switch ("Lives in the menu bar / system tray, ready when you are.") — mac: `SMAppService.mainApp` via helper; Windows: `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`.
- **History & storage** (full width): **Keep audio for** segmented 7 days / **30 days** (default) / Forever / Don't save (cleanup runs at startup and every 6 h; transcripts are always kept); **Clear all history** (danger, confirm "Clear all history? All N dictations and their audio files will be deleted. This cannot be undone.").

### 13.7 Toast messages (exact copy)
"Pasted into the active app (Cmd+V) · saved to History" · "Copied output" · "Copied original transcript" · "Recording cancelled" · "Too short · hold to talk, or double-tap for hands-free" · "Profile 3 · Email is active" · "Re-processed with Email · saved as v2" · "Re-transcribing with Whisper…" / "Re-transcribed · dictionary applied" · "Saved bolobhai-12.wav" · "Deleted" · "History cleared" · "Saved email.md" · "Created new-profile.md" · "Correction added" · "Added “Kubernetes”" · "Model deleted" · "Setup complete · hold fn to start".

### 13.8 First-run onboarding (inside main window, 5 steps)
Top: logo + "Bolo Bhai setup" + "Skip for now". A 5-segment progress bar labelled 1. Welcome · 2. Permissions · 3. Hotkey · 4. Models · 5. Ready. Content in an 820 px card (radius 20). Footer: Back · hint text · primary Continue (disabled until the step is satisfied).

1. **Welcome** — 112 px icon, "Bolo Bhai" 40/700, tagline; three tiles: *Speak Hinglish* ("Hindi + English, written back in clean Roman letters."), *Fully offline* ("Whisper and the LLM run on this computer. No cloud, no paid API."), *Profiles on 1–5* ("Press 1–5 while recording to pick how your text is cleaned up."). Button "Get started".
2. **Permissions** — rows with status dot: Mac: Microphone (Allow), Accessibility (Open Settings), Input Monitoring (Open Settings); Windows: Microphone only. Live-poll status every 1 s; row shows green "✓ Allowed" when granted.
3. **Test your hotkey** — Mac: orange tip box about the Globe key setting. Two big 88 px key caps; button "Press fn twice to test" → "Listening for the key…" → success "fn detected. Hold to talk, double-tap for hands-free." Windows: "Press Fn twice to test"; on failure after 5 s: "Fn is handled by this laptop's keyboard firmware, so Windows never sees it. Switched to double-tap Alt." + segmented Alt | Right Ctrl. Real key detection is used here.
4. **Download models** — Speech model segmented "large-v3-turbo · 1.6 GB" | "q5_0 · 550 MB"; rows for Whisper + Qwen3 with progress bars and status (Waiting / 37% / SHA-256 verified); button "Download 4.1 GB"; note "Needs 4.1 GB free disk." / live "1.20 of 4.1 GB · 41.6 MB/s". (Check free disk space first.)
5. **Ready** — "Sab set hai. Ab bolo!" + 4 hotkey tiles (Hold to talk, release to paste — `fn` · Hands-free, tap again to stop — `fn fn` · Switch profile while recording — `1–5` · Cancel recording — `Esc`) + the five slot chips. Button "Start dictating" → History.

`settings.onboardingDone = true` after step 5 (or Skip, which still requires models before dictation works — then the record button shows "Download models first" and links to Models).

### 13.9 Tray / menu-bar menu
Icon: monochrome mic glyph (template image on mac; turns `rec` red while recording). Menu (316 px):
- Header: icon + "Bolo Bhai" + green dot "Ready · offline" (or "Recording…", "Processing…").
- **Start dictation** (primary row, `hold fn` hint) — starts hands-free.
- PROFILE section: five radio rows `[n] Profile name   fn + n` (empty slot greyed).
- Open History · Open profiles folder · New profile… · Settings… (`Cmd+,` / `Ctrl+,`) · Quit Bolo Bhai.
Implement with Compose `Tray` + custom popup window for the styled menu (fallback: native AWT menu with the same items).

---

## 14. Behaviour details & edge cases
- Recording while the main window is focused still pastes into the main window's focused field, if any; otherwise only saves.
- Max recording length 10 min (auto stop & process, toast).
- If mic is unavailable → toast "No microphone — check Settings › Audio" and stay Idle.
- If LLM model is missing → behave as post-processing OFF and show a one-time banner.
- Search uses SQLite FTS5 over raw transcript + all version outputs (prefix match, case-insensitive).
- Times shown in local time, 12-h format with AM/PM; durations `m:ss`.
- All long operations are cancellable and never block the UI thread.
- Logs never contain transcript text unless "debug logging" is enabled (hidden flag in settings.json).

---

## 15. Data model

```sql
CREATE TABLE history (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  created_at      INTEGER NOT NULL,          -- epoch ms
  duration_ms     INTEGER NOT NULL,
  raw_transcript  TEXT    NOT NULL,          -- after dictionary corrections
  audio_path      TEXT,                      -- NULL if not kept / cleaned up
  status          TEXT    NOT NULL CHECK (status IN ('ok','llm_failed','stt_failed')),
  current_version INTEGER NOT NULL DEFAULT 0,
  language        TEXT    NOT NULL,
  whisper_model   TEXT    NOT NULL,
  -- denormalised mirror of the current version (matches the v1 spec columns):
  profile_name    TEXT,
  final_output    TEXT
);
CREATE TABLE history_version (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  history_id  INTEGER NOT NULL REFERENCES history(id) ON DELETE CASCADE,
  idx         INTEGER NOT NULL,              -- 0-based, shown as v1, v2…
  profile_id  TEXT,                          -- file name, NULL = raw / post-processing off
  profile_name TEXT NOT NULL,
  output      TEXT NOT NULL,
  llm_model   TEXT,
  latency_ms  INTEGER,
  created_at  INTEGER NOT NULL
);
CREATE VIRTUAL TABLE history_fts USING fts5(raw, outputs, content='');  -- keep in sync in repo code
CREATE INDEX idx_history_created ON history(created_at DESC);
```

`settings.json` (with defaults):
```json
{
  "onboardingDone": false,
  "theme": "dark",
  "hotkey": { "mac": "fn", "windows": "Alt" },
  "doubleTapMs": 400,
  "microphone": null,
  "sounds": true,
  "launchAtLogin": true,
  "keepAudio": "30d",
  "language": "hi",
  "postProcessing": true,
  "activeWhisperModel": "ggml-large-v3-turbo.bin",
  "activeLlmModel": "Qwen3-4B-Instruct-2507-Q4_K_M.gguf",
  "quickSlots": { "1": "hinglish-clean.md", "2": "numbered-list.md", "3": "email.md", "4": "whatsapp-message.md", "5": null },
  "activeSlot": 1,
  "windowBounds": null,
  "debugLogging": false
}
```

---

## 16. Performance
- Keep both sidecars warm; whisper with `-fa` (flash attention) and `--threads` = performance cores.
- Send audio to whisper as 16 kHz WAV bytes (no re-encoding).
- llama-server: `-c 4096 -ngl 99` on Mac (Metal), `-ngl 99` with Vulkan on Windows when available else CPU; `--parallel 1`.
- Measure and log stage timings (record stop → whisper → LLM → paste); show in debug logs only.

---

## 17. Packaging
- Compose `nativeDistributions`: `TargetFormat.Dmg` (mac), `TargetFormat.Msi` (Windows); package name "Bolo Bhai", vendor "Bolo Bhai", version 1.0.0.
- Bundle the JRE (jlink via Compose), fonts, sounds, default profiles, `models.json`, sidecar binaries for the target OS/arch (`appResourcesRootDir`), and the Swift helper (mac).
- App icon: from the supplied artwork (navy rounded square, blue→orange mic with sound waves). Produce `.icns`, `.ico` and 1024 px PNG; tray icon = simple monochrome mic.
- macOS: hardened runtime, entitlements `com.apple.security.device.audio-input`, sign + notarize when credentials are provided (document the steps; unsigned dev builds must still run). Info.plist: `NSMicrophoneUsageDescription`, `LSUIElement` false (policy switched at runtime).
- Windows: per-user install, Start-menu shortcut, uninstaller keeps `%APPDATA%\BoloBhai` unless the user ticks "remove data".
- Models are **not** bundled (downloaded in onboarding).

---

## 18. Testing
- Unit tests: `HotkeyStateMachine` (hold, short tap discard, double-tap → hands-free → stop, digits during each state, Esc, Alt+Tab pass-through), dictionary correction + casing, profile frontmatter parse/write, slot assignment uniqueness, filter counts, retention cleanup, pipeline fallback on LLM timeout (mock server).
- Integration test with fake whisper/llama HTTP servers.
- Manual checklist per platform in §20.

---

## 19. Milestones (build in this order; app runnable after each)
1. **Skeleton & design system:** project, theme tokens (dark/light/system), fonts, sidebar shell, all screens as navigable placeholders, tray icon, settings persistence.
2. **Mac core:** Swift helper (fn events, digit/Esc swallow, paste, permissions), hotkey state machine, recorder, compact pill (all states), whisper-server sidecar, paste. (No LLM yet: paste corrected raw.)
3. **Post-processing & profiles:** llama-server sidecar, profiles folder + hot reload + defaults, Quick Profiles 1–5 (slots, switching from pill/tray/sidebar), LLM timeout fallback.
4. **History:** SQLite + FTS, cards, filter chips (All default), search, versions, Show original, audio player, More menu (re-process, re-transcribe, save .wav, delete), failed Retry, toasts, confirm dialogs.
5. **Dictionary, Models, Settings, Onboarding:** full screens per §13, model downloads with resume + SHA-256, retention job, launch at login.
6. **Windows:** LL hook (Alt / Right Ctrl / Fn probe + fallback), menu-bar suppression, SendInput paste, Raw Input probe, tray, MSI.
7. **Polish & packaging:** sounds, animations (pill slide/fade, key pop), accessibility names, DMG + MSI, README with build/run steps.

---

## 20. Acceptance checklist
- [ ] With the main window **closed** (only the tray / menu-bar icon running), and with the app started at login and never opened, holding fn / Alt in another app still records, processes and pastes into that app.
- [ ] Closing the main window does not quit the app; only "Quit Bolo Bhai" in the tray does. Hotkeys survive sleep/wake and lock/unlock.
- [ ] Hold fn (Mac) / Alt (Win) anywhere → pill appears bottom-centre without stealing focus; release → processed text is pasted into the focused app within < 3 s for 10 s of speech on M1 Pro.
- [ ] Double-tap → hands-free; finger off keeps recording; one more press stops & processes.
- [ ] Pressing 1–5 while recording switches the profile, highlights the key in the pill, and the digit is NOT typed into the focused app.
- [ ] Esc cancels; tap too short is discarded with a toast.
- [ ] Alt alone never opens an app's menu bar on Windows; Alt+Tab etc. still work.
- [ ] Windows laptop without detectable Fn falls back to Alt in onboarding.
- [ ] LLM failure/timeout pastes raw text, pill shows the failed state, card shows Retry.
- [ ] History: grouped by day, search works on output + original, filter chips show All (default) + every profile + Failed with correct counts.
- [ ] Copy, Show original (+copy), Play/seek, Re-process (creates v2 tab), Re-transcribe, Save .wav, Delete (confirm) all work.
- [ ] Quick Profiles: assign / edit / save / use now / clear / new profile for empty slot; edits appear in the .md file; external edits hot-reload.
- [ ] All Profiles: create, rename (file renamed), assign slot, test on last dictation, delete to trash.
- [ ] Dictionary words reach Whisper; corrections apply before the LLM; Try-it preview is live.
- [ ] Models: download with progress, pause/resume, SHA-256 verify, use (server restarts), delete; language + post-processing toggle work.
- [ ] Settings: key choice, double-tap slider, mic, sounds, theme Dark/Light/System, launch at login, keep-audio retention, clear history.
- [ ] Onboarding 5 steps work on both OSes; tray menu switches profiles and opens screens.
- [ ] Everything works with the network unplugged after models are downloaded.
- [ ] `BoloBhai.dmg` and `BoloBhai.msi` build and install.

---

## 21. Out of scope for Phase 1
Mobile apps, cloud sync, accounts, streaming partial transcripts in the pill, languages other than Hindi/English, custom hotkey combos beyond the listed keys, Linux.
