# KotlinResult

[![Maven Central](https://img.shields.io/maven-central/v/com.happycodelucky.kotlinresult/kotlinresult?style=for-the-badge&logo=apachemaven&label=Maven%20Central)](https://central.sonatype.com/artifact/com.happycodelucky.kotlinresult/kotlinresult)
[![CI](https://img.shields.io/github/actions/workflow/status/happycodelucky/kotlinresult-kmp/ci.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=CI)](https://github.com/happycodelucky/kotlinresult-kmp/actions/workflows/ci.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=for-the-badge)](LICENSE)

![iOS](https://img.shields.io/badge/iOS-arm64-blue.svg?style=for-the-badge&logo=apple)
![macOS](https://img.shields.io/badge/macOS-arm64-blue.svg?style=for-the-badge&logo=apple)
![Android 11+](https://img.shields.io/badge/Android-11%2B-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white)
![JVM 21+](https://img.shields.io/badge/JVM-21%2B-orange.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Kotlin 2.4](https://img.shields.io/badge/Kotlin-2.4-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)

`kotlin.Result` for Kotlin Multiplatform, usable from Swift — for KMP libraries
whose APIs return results to Swift callers.

`kotlin.Result` is a value class, and Kotlin/Native's Objective-C export erases
it to an untyped `Any?`, so a KMP library can't return one to Swift.
KotlinResult's `Result<T>` is a thin reference class that wraps `kotlin.Result`
and forwards to it:

- **Kotlin** gets the stdlib `Result` API and behaviour — `isSuccess`,
  `getOrThrow`, `fold`, `map`, `recover`, `onFailure`, … with the stdlib's
  contracts — as members, so there's nothing to import beyond `Result`.
- **Swift** sees `KotlinResult<T>` with helpers: `try r.get()` returns the value
  or throws the Kotlin exception itself, and `r.result(as:)` gives a
  `Swift.Result`.
- **Structured concurrency** is left alone: nothing is classified "fatal", and
  cancellation is never captured into a result.
- **No dependencies** beyond the Kotlin standard library.

## Install

<!-- x-release-version-start -->
```kotlin
// gradle/libs.versions.toml
[libraries]
kotlinresult = { module = "com.happycodelucky.kotlinresult:kotlinresult", version = "1.1.0" }
kotlinresult-testing = { module = "com.happycodelucky.kotlinresult:kotlinresult-testing", version = "1.1.0" }

// build.gradle.kts
commonMain.dependencies { api(libs.kotlinresult) }                  // it's in your public API
commonTest.dependencies { implementation(libs.kotlinresult.testing) } // assertSuccess / assertFailure
```
<!-- x-release-version-end -->

<a id="using-it-from-a-kmp-library-required-setup"></a>

### Apple frameworks: export it

Swift reaches `KotlinResult` through your library's framework. Build that
framework with [SKIE](https://skie.touchlab.co), and **`export` KotlinResult into
it**:

```kotlin
kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework>().configureEach {
            export(libs.kotlinresult)
        }
    }
}
```

The Swift helpers ship inside the library, and SKIE compiles them into your
framework — but only where `KotlinResult` keeps its plain Swift name, which the
export guarantees. Without it, the link fails with
`cannot find type 'KotlinResult' in scope`. Don't declare your own
`extension KotlinThrowable: Error`; KotlinResult provides it.

## Usage

### Define an API

Return a `Result<T>`, failing with the exceptions you handle — ideally a sealed
hierarchy, so callers can match every case:

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

In files that import it, `Result` means KotlinResult's; write the stdlib type as
`kotlin.Result`. Convert between them with `toStdlibResult()` and `toResult()`.

### Kotlin

The `kotlin.Result` API, as members:

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

One difference from the stdlib: `getOrElse`, `getOrDefault`, `recover` and
`recoverCatching` return `T`, not a supertype of it. To widen, use `fold` or
`getOrNull() ?: default`.

### Swift

`get()` returns the value — name its type — or throws the Kotlin exception, which
you catch by class and switch over exhaustively with SKIE's `onEnum(of:)`:

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

- `try result.get()` on a `KotlinResult<KotlinUnit>` just returns or throws.
- `result.get(as: Int.self)` names the type inline, and `result.result(as: Int.self)`
  gives a `Swift.Result<Int, any Error>`.
- A value of the wrong type throws `KotlinResultTypeMismatchError`.
- Swift task cancellation reaches Kotlin as coroutine cancellation and comes
  back as `CancellationError`, never as a failed result.

## Testing

`kotlinresult-testing` adds assertions that work with any test framework
(kotlin.test, JUnit, Kotest):

```kotlin
val port = parsePort("8080").assertSuccess()                            // the value, or fails with the stack trace
val error = parsePort("nope").assertFailure<PortException.NotANumber>() // the exception, typed
```

In Swift, build a result to return from a fake:

```swift
let ok = KotlinResult<NSString>(value: "hi")
let failed = KotlinResult<KotlinUnit>(failure: KotlinThrowable(message: "boom"))
```

## Requirements

| | |
|---|---|
| Kotlin | 2.4 |
| JVM | 21+. Code that calls `Result`'s inline members (`fold`, `map`, `getOrElse`, …) must compile with `jvmTarget` 21 or higher. |
| Android | `minSdk` 30; compile against SDK 34+. |
| iOS / macOS | `iosArm64`, `iosSimulatorArm64`, `macosArm64`, through your framework (built with SKIE, exporting KotlinResult). Your framework sets the minimum OS. |

## Documentation

The [documentation site](https://happycodelucky.github.io/kotlinresult-kmp/) has
guides, per-platform notes, the changelog, and the
[API reference](https://happycodelucky.github.io/kotlinresult-kmp/reference/).

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

[Apache 2.0](LICENSE)
