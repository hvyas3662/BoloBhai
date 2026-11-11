import Cocoa
import CoreGraphics
import Foundation

// bolobhai-mac-helper (BoloBhai.md §6.4): one JSON object per line.
//   stdout (helper -> app): {"ev":"fn","down":true,"t":<ms>} | {"ev":"key","k":"1".."5"|"esc"}
//   stdin  (app -> helper): {"cmd":"capture","on":bool} | {"cmd":"paste"}
//                           | {"cmd":"activationPolicy","policy":"accessory"|"regular"}
//                           | {"cmd":"perm","which":"accessibility"|"input"|"mic"}
//                           | {"cmd":"openPrefs","pane":"accessibility"|"input"|"mic"}
//
// UNVERIFIED: written on Windows; must be built and tested on a Mac (swift build -c release).

// Keycodes we swallow while recording (digits 1–5 top row + keypad, Esc).
let digitTop: [Int64: String] = [18: "1", 19: "2", 20: "3", 21: "4", 23: "5"]
let digitPad: [Int64: String] = [83: "1", 84: "2", 85: "3", 86: "4", 87: "5"]
let escKeycode: Int64 = 53

var capturing = false
var lastFn = false
let out = FileHandle.standardOutput

func emit(_ obj: [String: Any]) {
    guard let data = try? JSONSerialization.data(withJSONObject: obj),
          var line = String(data: data, encoding: .utf8) else { return }
    line += "\n"
    out.write(line.data(using: .utf8)!)
}

func nowMs() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }

// --- Event tap ---------------------------------------------------------------

let tapCallback: CGEventTapCallBack = { _, type, event, _ in
    switch type {
    case .flagsChanged:
        let fnDown = event.flags.contains(.maskSecondaryFn)
        if fnDown != lastFn {
            lastFn = fnDown
            emit(["ev": "fn", "down": fnDown, "t": nowMs()])
        }
    case .keyDown, .keyUp:
        if capturing {
            let code = event.getIntegerValueField(.keyboardEventKeycode)
            if let d = digitTop[code] ?? digitPad[code] {
                if type == .keyDown { emit(["ev": "key", "k": d]) }
                return nil // swallow
            }
            if code == escKeycode {
                if type == .keyDown { emit(["ev": "key", "k": "esc"]) }
                return nil // swallow
            }
        }
    default:
        break
    }
    return Unmanaged.passUnretained(event)
}

let mask = (1 << CGEventType.flagsChanged.rawValue) |
           (1 << CGEventType.keyDown.rawValue) |
           (1 << CGEventType.keyUp.rawValue)

guard let tap = CGEvent.tapCreate(
    tap: .cgSessionEventTap, place: .headInsertEventTap, options: .defaultTap,
    eventsOfInterest: CGEventMask(mask), callback: tapCallback, userInfo: nil
) else {
    emit(["ev": "error", "msg": "tapCreate failed — needs Accessibility + Input Monitoring"])
    exit(1)
}
let runLoopSource = CFMachPortCreateRunLoopSource(kCFAllocatorDefault, tap, 0)
CFRunLoopAddSource(CFRunLoopGetCurrent(), runLoopSource, .commonModes)
CGEvent.tapEnable(tap: tap, enable: true)

// --- Commands (paste, capture, activation policy, permissions) ---------------

func postCmdV() {
    let src = CGEventSource(stateID: .combinedSessionState)
    let down = CGEvent(keyboardEventSource: src, virtualKey: 0x09, keyDown: true) // V
    down?.flags = .maskCommand
    let up = CGEvent(keyboardEventSource: src, virtualKey: 0x09, keyDown: false)
    up?.flags = .maskCommand
    down?.post(tap: .cgSessionEventTap)
    up?.post(tap: .cgSessionEventTap)
}

func prefsURL(_ pane: String) -> String {
    switch pane {
    case "accessibility": return "x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility"
    case "input": return "x-apple.systempreferences:com.apple.preference.security?Privacy_ListenEvent"
    default: return "x-apple.systempreferences:com.apple.preference.security?Privacy_Microphone"
    }
}

func handle(_ cmd: [String: Any]) {
    switch cmd["cmd"] as? String {
    case "capture":
        capturing = (cmd["on"] as? Bool) ?? false
    case "paste":
        DispatchQueue.main.async { postCmdV() }
    case "activationPolicy":
        let regular = (cmd["policy"] as? String) == "regular"
        DispatchQueue.main.async { NSApp.setActivationPolicy(regular ? .regular : .accessory) }
    case "openPrefs":
        if let url = URL(string: prefsURL((cmd["pane"] as? String) ?? "mic")) {
            DispatchQueue.main.async { NSWorkspace.shared.open(url) }
        }
    case "perm":
        let which = (cmd["which"] as? String) ?? "mic"
        let granted = which == "accessibility" ? AXIsProcessTrusted() : false
        emit(["ev": "perm", "which": which, "granted": granted])
    default:
        break
    }
}

// Read stdin lines on a background thread; commands run on the main run loop.
DispatchQueue.global().async {
    while let line = readLine(strippingNewline: true) {
        guard let data = line.data(using: .utf8),
              let obj = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else { continue }
        handle(obj)
    }
    exit(0) // app closed the pipe
}

NSApplication.shared.setActivationPolicy(.accessory)
CFRunLoopRun()
