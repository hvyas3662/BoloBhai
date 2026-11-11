// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "bolobhai-mac-helper",
    platforms: [.macOS(.v12)],
    targets: [
        .executableTarget(name: "bolobhai-mac-helper", path: "Sources/bolobhai-mac-helper")
    ]
)
