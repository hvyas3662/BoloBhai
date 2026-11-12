# Sidecar binaries

These are the local inference engines Bolo Bhai launches as child processes. They are **not**
checked into git (too large / platform-specific). Drop them here; `appResourcesRootDir` in
`composeApp/build.gradle.kts` bundles the matching OS folder into the installer so they land in the
app's install folder, and `platform/Sidecars.kt` resolves them at runtime.

Each engine gets its **own subfolder** with its own DLLs/dylibs — whisper.cpp and llama.cpp ship
different `ggml` versions that must not share a directory. Layout (Compose copies the folder
matching the build's OS/arch):

```
sidecars/
  windows/
    whisper/   whisper-server.exe + ggml*.dll, whisper.dll
    llama/     llama-server.exe   + ggml*.dll, llama*.dll, mtmd.dll, …
  macos-arm64/
    whisper/   whisper-server + *.dylib        (build on a Mac)
    llama/     llama-server   + *.dylib        (build/extract on a Mac)
    bolobhai-mac-helper
  macos-x64/   (same as arm64)
```

**Windows binaries are already provided** (whisper.cpp `b5454` CPU build, llama.cpp `b12308` CPU
build). They are gitignored (large), so on a fresh clone re-download them:
- **whisper** — https://github.com/ggml-org/whisper.cpp/releases → `whisper-bin-x64.zip` → put
  `whisper-server.exe` + `whisper.dll` + `ggml*.dll` in `windows/whisper/`.
- **llama** — https://github.com/ggml-org/llama.cpp/releases → `llama-*-bin-win-cpu-x64.zip` →
  extract everything into `windows/llama/`.

**macOS binaries must be prepared on a Mac** — the `.dylib`s use symlinks + executable bits + code
signing that don't survive extraction on Windows. On a Mac: download the llama.cpp
`bin-macos-<arch>.tar.gz`, build whisper.cpp's `whisper-server`, and build
`bolobhai-mac-helper` from `../mac-helper/` (`swift build -c release`).

Models are **not** here — they download on first run into `<app-data>/models/`.

Servers bind a random free localhost port (OS-assigned, so no clash with dev tools), start with the
app, and are killed when the app quits.
