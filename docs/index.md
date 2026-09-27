---
title: Overview
---

# KotlinResult

`kotlin.Result` for Kotlin Multiplatform, usable from Swift.

`kotlin.Result` is a value class, which Kotlin/Native's Objective-C export erases
to an untyped `Any?`, so a KMP library can't hand one to Swift. KotlinResult's
`Result<T>` is a thin reference class that wraps `kotlin.Result` and forwards to
it:

- **Kotlin** keeps the stdlib `Result` API and behaviour verbatim.
- **Swift** sees `KotlinResult<T>` with bundled helpers — `try r.get()`,
  `r.result(as:)` for a `Swift.Result` — and catches failures as the Kotlin
  exceptions themselves.
- **Structured concurrency** is respected: a `Result` is a plain value and
  cancellation is never captured into one.

## Install

```kotlin
dependencies {
    api("com.happycodelucky.kotlinresult:kotlinresult:{{ version }}")
}
```

A KMP library must also `export` it into its Apple framework — see
[Use it from a KMP library](recipes/kmp-library.md). Full instructions:
[Installation](installation.md).
