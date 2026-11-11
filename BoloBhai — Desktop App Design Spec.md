# Bolo Bhai — Desktop App Design Spec

Nov 11, 2026 · @Harshal Vyas

## Overview

Bolo Bhai is an offline voice-dictation app for macOS and Windows: hold **fn** (Mac) or **Alt** (Windows), speak Hinglish, and cleaned-up text is pasted into whatever app you are typing in. Tagline: "Bolo Bhai, baaki hum likh denge."

- **Platforms:** macOS and Windows desktop only, landscape windows. No mobile.
- **Engines:** whisper.cpp for speech-to-text, a small local LLM (Qwen3-4B by default) for clean-up. Nothing goes to the cloud.
- **Where it lives:** a menu-bar / tray app. The hotkeys work system-wide, even when the main window is closed.
- **Main window:** 1440×900 by default, sidebar plus content.
- **Source:** the Claude Design canvas "Bolo Bhai — Desktop App", 35 artboards in 6 rows. Every artboard is interactive in Play mode.

## Screen inventory

The canvas holds 35 artboards: 25 app windows at 1440×900, 8 floating-pill scenes at 720×260 and 2 tray menus at 360×640.

| Row | Artboards | What they show |
| --- | --- | --- |
| Dictation flow | History (home) · Hold fn · Double-tap fn (hands-free) · Processing · Pasted + saved | One dictation end to end inside the app window |
| Floating pill + tray | Hold fn · Press 3 → Email · Hands-free · Processing · Pasted · LLM timeout · Hold Alt (Windows) · Double-tap Alt (Windows) · Menu bar menu · Tray menu (light) | The pill over another app, plus the tray menu |
| History actions & modals | More menu / re-process · Show original + audio player · Delete confirmation | Card actions and dialogs |
| App screens | Quick Profiles 1–5 · Empty slot 5 · All Profiles editor · Dictionary · Models (downloading) · Settings | Every sidebar screen |
| First-time setup (Mac) | Welcome · Permissions · Hotkey test · Download models · Ready | The 5-step onboarding |
| Light theme (Windows) | History · Fn not found → Alt fallback · Recording · Quick Profiles · Models · Settings | The same screens in light theme with Windows keys |

## Hotkeys and the floating pill

One key does everything: hold to talk, double-tap for hands-free, and press 1–5 while recording to switch profile.

| Gesture | Mac | Windows | Result |
| --- | --- | --- | --- |
| Hold the key | fn | Alt (or Fn / Right Ctrl) | Records while held. Release stops and processes automatically. |
| Double-tap within 400 ms | fn fn | Alt Alt | Hands-free: finger off, still recording. Press once more to stop and process. |
| 1–5 while recording | 1–5 | 1–5 | Switches the Quick Profile. The digit is not typed into the app. |
| Esc while recording | Esc | Esc | Cancels and discards the audio. |
| Tap that is too short | — | — | Discarded, with a "Too short" toast. |

**The pill** is a single rounded bar, 48 px tall and about 460 px wide, at the bottom-centre of the screen. It never takes focus from the app you are typing in. From left to right:

- Red recording dot, `0:03` timer and a 14-bar live waveform.
- Mode label: "Release fn" (or "Release Alt") when held, "Hands-free" after a double-tap.
- Five 24 px keys 1–5. The active one is orange, an empty slot is dashed. A key pops when pressed.
- Short profile name (Hinglish, List, Email, WhatsApp).
- A blue stop button (hands-free only) and a grey cancel ✕.

| Pill state | What it shows |
| --- | --- |
| Hold recording | dot · timer · waveform · "Release fn" · keys · name · ✕ |
| Hands-free | the same, with "Hands-free" and a stop button |
| Profile switched | the pressed key (e.g. 3) pops with an orange ring, the name becomes "Email" |
| Processing | spinner · "Processing" · keys locked on the chosen profile |
| Pasted | green check · "Pasted" · Cmd+V / Ctrl+V, auto-hides |
| LLM timeout | red icon · "LLM timeout · raw text pasted" · red Retry |

## Dictation flow

A dictation goes from key press to pasted text in six steps, and it lands in History either way.

1. **Record:** hold or double-tap the key. The pill appears and the audio is captured at 16 kHz mono.
2. **Transcribe:** whisper.cpp runs with language fixed to Hindi (`hi`). The custom words are passed in as a hint.
3. **Correct:** dictionary fixes are applied, e.g. trilogic → Triologic, thales → Tiles, almary → Almirah.
4. **Clean up:** the active profile's .md file is the LLM's instructions (temperature 0.2, 15 s limit).
5. **Paste:** the text goes in with Cmd+V / Ctrl+V and the old clipboard is put back. Toast: "Pasted into the active app (Cmd+V) · saved to History".
6. **Save:** a new card at the top of History, holding the audio, the original transcript, the output and the profile used.

If the LLM fails or takes over 15 s, the corrected raw text is pasted instead. The pill shows the timeout state and the History card gets a red Retry. In the app window, the sidebar's **Hold to talk** button stands in for the key: press and hold to talk, double-click for hands-free, click once more to stop.

## History

History is the home screen: every past dictation, newest first, grouped under Today and Yesterday, with filter chips that start on **All**.

**Header:** "History", the dictation count and a search box ("Search output or original…"). Under it, a row of filter chips, each with a count bubble:

- **All** (selected by default)
- One chip per profile, with its 1–5 key badge if it has one: Hinglish Clean, Numbered List, Email, WhatsApp Message, Meeting Notes
- **Raw (no LLM)**, shown only when such items exist
- **Failed**, in red

A selected chip turns blue (Failed turns red). Filter and search work together.

**Each card shows:**

- A profile tag with its slot number, the time and the duration. Version tabs (`v1 · Hinglish Clean`, `v2 · Email`) appear when a card has been re-processed.
- The output text, up to 3 lines. Click to expand.
- Buttons: **Copy output**, **Show original** (the raw Whisper text in a dashed panel with its own Copy), **Play audio** (round play button and a 48-bar waveform you can click to seek) and **⋯ More**.

| More menu item | What it does |
| --- | --- |
| Re-process with profile ▸ | Opens a list of every profile. Runs the LLM again on the same transcript and adds a new version tab. |
| Re-transcribe audio | Runs Whisper again with the current dictionary |
| Save audio as .wav | Saves e.g. `bolobhai-5.wav` |
| Delete item | Opens the confirm dialog "Delete this dictation?" with Cancel / Delete |

A failed card has a red border and shows the raw transcript, with "Post-processing failed · raw transcript was pasted" and a red **Retry** button. Empty states: "No dictations yet" with a Start button, or "Nothing matches — Try another word, or pick All above."

## Quick Profiles 1–5 and All Profiles

Five number keys each hold one .md profile; slot 1 is the default and slot 5 starts empty.

| Slot | Profile | File | Badge |
| --- | --- | --- | --- |
| 1 | Hinglish Clean | hinglish-clean.md | Default |
| 2 | Numbered List | numbered-list.md | Active (last used) |
| 3 | Email | email.md | Ready |
| 4 | WhatsApp Message | whatsapp-message.md | Ready |
| 5 | — | assign a .md profile | Empty |

**Quick Profiles screen:** five slot cards across the top (big key, badge, name, file, "hold fn + 3"). The selected card is outlined in orange. Below them is the editor for the selected slot:

- **Left:** "Profile in this slot" dropdown, Display name, a shortcuts box (macOS `hold fn + n`, Windows `hold Alt + n`, hands-free `n`), and the buttons **Save slot n**, **Use now** and **Clear slot**.
- **Right:** the LLM instructions as a monospace text box, with the file path, e.g. `~/.bolobhai/profiles/email.md`.
- **Empty slot:** "Slot 5 khaali hai" and a **New profile for slot 5** button.

**All Profiles screen:** every .md file in the profiles folder, with **Open profiles folder** and **New profile** buttons.

- **Left list:** each profile's slot badge, name, file and how many times it was used.
- **Right editor:** Name, File (read-only), Quick slot (Not on a key / Slot 1–5) and the system prompt text box.
- **Buttons:** Delete profile, **Test on last dictation** (shows a Preview box) and Save.
- A fifth shipped profile, **Meeting Notes**, is not on a key by default.

The active profile can also be changed from the sidebar's "Active profile" box, the pill's keys and the tray menu.

## Dictionary, Models and Settings

Three sidebar screens control how the speech is heard, which models run, and how the app behaves.

**Dictionary** (saved in `dictionary.yaml`):

- **Custom words:** chips with ✕ (Triologic, Bolo Bhai, Kotlin, standup) plus an "Add word" field. They are passed to Whisper as a hint.
- **Corrections:** rows of wrong → right (trilogic → Triologic, thales → Tiles, almary → Almirah), with Add and ✕. They are applied before the LLM.
- **Try it:** paste a raw transcript and see the corrected text live.

**Models:** everything runs on this computer (Metal on Mac, Vulkan / CPU on Windows), and the header shows disk used.

| Kind | Model | Size | RAM | State on canvas |
| --- | --- | --- | --- | --- |
| Speech to text | Whisper large-v3-turbo | 1.6 GB | \~2 GB | Active |
| Speech to text | Whisper large-v3-turbo q5\_0 | 550 MB | \~1 GB | Download |
| Post-processing LLM | Qwen3-4B-Instruct-2507 Q4\_K\_M | 2.5 GB | 3–4 GB | Active |
| Post-processing LLM | Gemma 3 4B IT Q4\_K\_M ("Often better at Hindi") | 2.5 GB | 3–4 GB | Downloading, with progress bar and Pause |

Model rows have Download, Pause / Resume, Use and Delete. The screen also has **Spoken language** (Hindi / Hinglish by default, English, or Auto-detect with a warning) and a **Post-processing with LLM** switch.

**Settings:**

| Group | Settings |
| --- | --- |
| Hotkey | Dictation key: fn on Mac; Fn / Alt / Right Ctrl on Windows. Double-tap window slider, 250–600 ms, default 400. A reference list of the gestures. |
| Audio | Microphone picker. Start / stop sound switch. |
| General | Theme: Dark / Light / System. Launch at login switch. |
| History & storage | Keep audio for: 7 days / 30 days / Forever / Don't save. **Clear all history** with a confirm dialog. |
| Header | **Run first-time setup again** button |

## First-time setup and tray menu

Setup is five steps with a progress bar; Continue stays disabled until each step is done.

1. **Welcome:** large icon, "Bolo Bhai", the tagline, and three tiles: Speak Hinglish · Fully offline · Profiles on 1–5. Button: "Get started".
2. **Permissions:** Microphone, Accessibility and Input Monitoring on Mac (Microphone only on Windows). Each row turns to a green "Allowed".
3. **Test your hotkey:** two large key caps and "Press fn twice to test".
   - Mac shows a tip: set System Settings › Keyboard › "Press Globe key to" = Do Nothing.
   - On Windows, if Fn isn't seen: "Fn is handled by this laptop's keyboard firmware… Switched to double-tap Alt", with an Alt / Right Ctrl choice.
4. **Download models:** choose the speech model (large-v3-turbo 1.6 GB or q5\_0 550 MB). Two progress bars end in "SHA-256 verified". Button: "Download 4.1 GB".
5. **Ready:** "Sab set hai. Ab bolo!", four hotkey tiles (hold · hands-free · 1–5 · Esc) and the five slot chips. Button: "Start dictating".

**Tray / menu-bar menu** (316 px wide, dark and light versions), top to bottom:

- Header: icon, "Bolo Bhai" and a green "Ready · offline".
- Primary **Start dictation** row, labelled `hold fn`.
- PROFILE section: five rows, each with its key and `fn + n`. The active one is orange and the empty slot is greyed.
- Open History · Open profiles folder · New profile… · Settings… (Cmd+, / Ctrl+,) · Quit Bolo Bhai.

## Visual design system

The look is taken from the app icon: a dark navy ground, **blue** for actions and selection, **orange** for the active Quick Profile. There are no gradients and no emoji, and icons are thin outline strokes.

| Token | Dark | Light | Used for |
| --- | --- | --- | --- |
| bg | #0B1020 | #F2F4F8 | Window background |
| side | #10172A | #FFFFFF | Sidebar |
| card | #151D33 | #FFFFFF | Cards |
| card2 | #1B2440 | #F4F6FA | Inner panels, keys |
| border | #263152 | #DCE2EC | Borders, dividers |
| text | #E9EEF8 | #111827 | Main text |
| muted | #9EAAC4 | #525D74 | Secondary text |
| blue | #53C4F4 | #0A6FAD | Primary buttons, selected chips |
| orange | #F6964F | #A64B0A | Active slot key |
| red | #FF7E7E | #B42323 | Failed, delete, Retry |
| green | #4ADE80 | #1F8A4C | Status dot, Pasted |
| rec | #FF5F5F | #D12C2C | Recording dot |

- **Type:** Space Grotesk for headings (28/700 page titles), Inter for UI text (13–15), JetBrains Mono for keys, timers and file paths.
- **Shapes:** cards radius 14, buttons and inputs radius 10, filter chips and the pill fully rounded.
- **Sizes:** buttons 38 px (44 px large), inputs 40 px, switch 44×26.
- **Feedback:** toasts sit at the bottom-centre of the window. Destructive actions open a 420 px confirm dialog with a red button.
- **Theme:** Dark is the default. Light and System are chosen in Settings or with the sidebar sun/moon button.
