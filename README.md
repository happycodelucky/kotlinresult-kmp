# KotlinResult

![iOS · macOS via KMP](https://img.shields.io/badge/iOS%20%C2%B7%20macOS-via%20KMP-blue.svg?style=for-the-badge&logo=apple)
![Android 11+](https://img.shields.io/badge/Android-11%2B-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white)
![JVM 21+](https://img.shields.io/badge/JVM-21%2B-orange.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Kotlin 2.4](https://img.shields.io/badge/Kotlin-2.4-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=for-the-badge)

`kotlin.Result` for Kotlin Multiplatform, usable from Swift.

It's a building block for **KMP libraries**: Swift reaches it through your
library's framework, not on its own — so it ships on Maven Central only, with no
XCFramework or Swift package.

`kotlin.Result` is a value class, and Kotlin/Native's Objective-C export erases
it to an untyped `Any?`, so a KMP library can't return one to Swift. KotlinResult
is `com.happycodelucky.kotlinresult.Result<T>`: a thin reference class that wraps
`kotlin.Result` and forwards to it.

- **Kotlin** gets the stdlib `Result` API and behaviour (`isSuccess`,
  `getOrThrow`, `fold`, `map`, `recover`, `onFailure`, … with the stdlib's
  contracts) as **members** — no per-operator imports — plus `toStdlibResult()` /
  `toResult()`. One difference: `getOrElse`, `getOrDefault`, `recover` and
  `recoverCatching` return `T`, not any supertype of it (a member can't express
  that bound); widen with `fold` or `getOrNull() ?: default`.
- **Swift** sees it as **`KotlinResult<T>`** (next to `KotlinInt`, `KotlinUnit`, …,
  never shadowing Swift's `Result`) with bundled Swift helpers: `try r.get()`,
  `let s: String = try r.get()`, `r.result(as:)` → `Swift.Result`. A failure is
  thrown as the Kotlin exception itself, so Swift catches it by class and
  switches exhaustively with SKIE's `onEnum(of:)`.
- **Structured concurrency:** a `Result` is a plain value — nothing is classified
  "fatal", and cancellation is never captured. Let `CancellationException`
  propagate and SKIE delivers it to Swift as `CancellationError`.
- **No runtime dependencies** beyond the Kotlin stdlib.

## Modules

| Module | Coordinate | What it is |
|--------|-----------|-----------|
| `:kotlinresult` | `com.happycodelucky.kotlinresult:kotlinresult` | `Result<T>` + its bundled Swift. |
| `:kotlinresult-testing` | `com.happycodelucky.kotlinresult:kotlinresult-testing` | `assertSuccess()` / `assertFailure<E>()` for tests. |

## Quick example

### Defining an API (Kotlin, `commonMain`)

Return a `Result<T>`, failing with the specific exceptions you handle — ideally a
sealed hierarchy, so callers can match every case:

```kotlin
import com.happycodelucky.kotlinresult.Result

sealed class PortException(message: String) : Exception(message) {
    class NotANumber(val text: String) : PortException("not a number: $text")
    class OutOfRange(val port: Int) : PortException("out of range: $port")
}

fun parsePort(text: String): Result<Int> {
    val port = text.toIntOrNull() ?: return Result.failure(PortException.NotANumber(text))
    return if (port in 0..65535) Result.success(port) else Result.failure(PortException.OutOfRange(port))
}
```

### Using it from Kotlin

The same API as `kotlin.Result` — every operator is a member, so there's nothing
to import beyond `Result` itself:

```kotlin
parsePort("8080")
    .onSuccess { port -> println("listening on $port") }
    .onFailure { e ->
        when (e as PortException) {
            is PortException.NotANumber -> println("not a number: ${e.text}")
            is PortException.OutOfRange -> println("out of range: ${e.port}")
        }
    }

val port = parsePort(input).getOrDefault(8080)
```

### Using it from Swift

Through your library's framework (set up [below](#using-it-from-a-kmp-library-required-setup)),
`get()` returns the value (name its type) or throws the Kotlin exception itself:

```swift
do {
    let port: Int = try parsePort(text: "8080").get()
    print("listening on \(port)")
} catch let e as PortException {
    switch onEnum(of: e) {
    case .notANumber(let x): print("not a number: \(x.text)")
    case .outOfRange(let x): print("out of range: \(x.port)")
    }
}
```

## Using it from a KMP library (required setup)

Depend on it with `api` (it's in your public API) and **`export` it into every
Apple framework that links it**:

```kotlin
kotlin {
    sourceSets.commonMain.dependencies { api("com.happycodelucky.kotlinresult:kotlinresult:<version>") }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework>().configureEach {
            export("com.happycodelucky.kotlinresult:kotlinresult:<version>")
        }
    }
}
```

The Swift helpers ship as SKIE-bundled Swift inside the klib, and SKIE compiles
the bundled Swift of *every* linked klib into your framework. That file only
compiles where `KotlinResult` keeps its plain name — i.e. where the module is
exported. Without the export the framework link fails with
`cannot find type 'KotlinResult' in scope` in `bundled.kotlinresult.KotlinResult+Swift.swift`.
Your framework needs SKIE; don't declare your own `extension KotlinThrowable: Error`
(this library provides it).

## Install

### Gradle (KMP / Android / JVM)

<!-- x-release-version-start -->
```kotlin
// gradle/libs.versions.toml
[libraries]
kotlinresult = { module = "com.happycodelucky.kotlinresult:kotlinresult", version = "1.0.1" }

// build.gradle.kts (commonMain)
implementation(libs.kotlinresult)
```
<!-- x-release-version-end -->

There is no Swift package: an iOS or macOS app gets `KotlinResult` from the KMP
library that uses it, exported into that library's framework.

## Development

Everything runs through [mise](https://mise.jdx.dev):

```bash
brew install mise
mise trust && mise install

mise run check         # ktlint + detekt + every test target — the done gate
mise run test:jvm      # fast inner loop
mise run build:src     # assemble the library
mise run test:swift    # link a consumer framework that exports it + run the Swift helpers
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
