# Sample apps

Four samples that consume the `:src` library across every target. They're demo
scaffolding — not published, not in the library check gate.

| App | Path | Consumes the library via |
|---|---|---|
| iOS | `apps/ios` | SPM local binary target (root `Package.swift`) |
| macOS | `apps/macos` | SPM local binary target (root `Package.swift`) |
| Android | `apps/android` (`:androidApp`) | Gradle project dependency (`project(":src")`) |
| JVM CLI | `apps/jvm-cli` (`:jvm-cli`) | Gradle project dependency (`project(":src")`) |

## Apple apps (iOS / macOS)

The `.xcodeproj` files are **generated** from `project.yml` via xcodegen and are
gitignored — the YAML is the source of truth. Xcode never runs Gradle; it
consumes the library as a local Swift Package (the root `Package.swift`) whose
binary target is the XCFramework Gradle builds.

```bash
# Rebuild the debug XCFramework, point Package.swift at it, regenerate the
# .xcodeproj, and open Xcode:
mise run open:ios       # or: mise run open:macos

# After editing Kotlin, re-run to pick up the changes:
mise run spm:dev

# Before committing, restore the committed Package.swift:
mise run spm:restore
```

Requires `xcodegen` (`brew install xcodegen`; mise provisions it via `mise install`)
and a recent Xcode that SKIE supports.

Add the platform capabilities your library needs to each app's `project.yml`
(Info.plist keys, entitlements) — the templates ship minimal.

## Android

```bash
mise run open:android   # opens apps/android in Android Studio
./gradlew :androidApp:installDebug   # build + install on a connected device
```

## JVM CLI

```bash
mise run build:jvm
./gradlew :jvm-cli:run
```
