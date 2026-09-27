# __DISPLAY_NAME__

![iOS 18+](https://img.shields.io/badge/iOS-18%2B-blue.svg?style=for-the-badge&logo=apple)
![macOS 15+](https://img.shields.io/badge/macOS-15%2B-blue.svg?style=for-the-badge&logo=apple)
![Android 11+](https://img.shields.io/badge/Android-11%2B-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white)
![JVM 21+](https://img.shields.io/badge/JVM-21%2B-orange.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Kotlin 2.4](https://img.shields.io/badge/Kotlin-2.4-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=for-the-badge)

> **TODO:** one-paragraph description of what __DISPLAY_NAME__ does, behind one
> Kotlin Multiplatform API for iOS, macOS, Android, and the JVM.

UI is out of scope — __DISPLAY_NAME__ is the headless `:src` KMP module
(see [`CLAUDE.md`](CLAUDE.md) §1). Each platform app consumes it natively; see
[`apps/`](apps/) for samples on every platform.

## Modules

| Module | Coordinate | What it is |
|--------|-----------|-----------|
| `:src` | `com.happycodelucky.__PROJECT_NAME__:__PROJECT_NAME__` | The library. |
| `:src-testing` | `com.happycodelucky.__PROJECT_NAME__:__PROJECT_NAME__-testing` | Public test fakes + helpers for consumers. |

## Quick example

```kotlin
import com.happycodelucky.src.Greeter

println(Greeter().greet())   // "Hello from <platform>"
```

Replace the placeholder `Greeter` with your real API.

## Install

### Gradle (KMP / Android / JVM)

<!-- x-release-version-start -->
```kotlin
// gradle/libs.versions.toml
[libraries]
__PROJECT_NAME__ = { module = "com.happycodelucky.__PROJECT_NAME__:__PROJECT_NAME__", version = "0.0.0" }

// build.gradle.kts (commonMain)
implementation(libs.__PROJECT_NAME__)
```
<!-- x-release-version-end -->

### Swift (SPM)

Add this repository as a package dependency, pinned to a release tag. The
XCFramework ships as a GitHub Release asset (see
[`.github/PUBLISHING.md`](.github/PUBLISHING.md)).

<!-- x-release-version-start -->
```swift
.package(url: "https://github.com/happycodelucky/__PROJECT_NAME__.git", from: "0.0.0")
```
<!-- x-release-version-end -->

## Development

Everything runs through [mise](https://mise.jdx.dev):

```bash
brew install mise
mise trust && mise install

mise run check         # ktlint + detekt + every test target — the done gate
mise run test:jvm      # fast inner loop
mise run build:src     # assemble the library
mise run open:macos    # build XCFramework + xcodegen + open Xcode (or open:ios / open:android)
mise tasks             # full task list
```

See [`CLAUDE.md`](CLAUDE.md) for conventions and [`CONTRIBUTING.md`](CONTRIBUTING.md)
to get started.

### One-time CI setup

- **GitHub Pages (docs site):** the Docs workflow deploys `docs/` to Pages
  after each successful release (pushes to `main` only build it). It
  auto-enables Pages on first run (`configure-pages` with `enablement: true`),
  which needs **Settings → Actions → General → Workflow permissions → Read and
  write**. If your org blocks auto-enablement, enable it manually:
  **Settings → Pages → Source: GitHub Actions**.
- **Releases:** set the four Maven Central credentials on the
  `continuous-deployment` environment, and let the release PR be opened:
  **Settings → Actions → General → Allow GitHub Actions to create and approve
  pull requests** (or configure a GitHub App) — see
  [`.github/PUBLISHING.md`](.github/PUBLISHING.md). Every PR then carries a
  changeset (`mise run changeset`; [`.changeset/README.md`](.changeset/README.md)).

## License

[Apache 2.0](LICENSE)
