# Sidecar binaries

These are the local inference engines Bolo Bhai launches as child processes. They are **not**
checked into git (too large / platform-specific). Drop them here; `appResourcesRootDir` in
`composeApp/build.gradle.kts` bundles the matching OS folder into the installer so they land in the
app's install folder, and `platform/Sidecars.kt` resolves them at runtime.

Layout (Compose copies the folder matching the build's OS/arch):

```
sidecars/
  windows/      whisper-server.exe   llama-server.exe
  macos-arm64/  whisper-server       llama-server       bolobhai-mac-helper
  macos-x64/    whisper-server       llama-server       bolobhai-mac-helper
```

- **whisper-server** — from https://github.com/ggml-org/whisper.cpp (Releases → Windows build, or
  build the `whisper-server` target).
- **llama-server** — from https://github.com/ggml-org/llama.cpp (Releases, or build `llama-server`).
- **bolobhai-mac-helper** — build from `mac-helper/` on a Mac (`swift build -c release`).

Models are **not** here — they download on first run into `<app-data>/models/`.

Servers bind a random free localhost port (OS-assigned, so no clash with dev tools), start with the
app, and are killed when the app quits.
