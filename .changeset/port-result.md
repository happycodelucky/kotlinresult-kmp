---
title: "Result: a Swift-friendly kotlin.Result for KMP"
change: minor
description: "First release: Result<T> mirrors the kotlin.Result API in Kotlin and appears as KotlinResult in Swift, with bundled get() / result(as:) helpers; plus assertSuccess() / assertFailure<E>() in kotlinresult-testing."
---

The first release of KotlinResult: `kotlin.Result` for Kotlin Multiplatform,
usable from Swift.

- **`com.happycodelucky.kotlinresult.Result<T>`** wraps and delegates to
  `kotlin.Result`. It has the stdlib API with the same names, signatures and
  contracts: `isSuccess`, `getOrNull`, `getOrThrow`, `exceptionOrNull`, `fold`,
  `map`, `mapCatching`, `recover`, `recoverCatching`, `onSuccess`, `onFailure`,
  `getOrElse` and `getOrDefault`. Convert with `toResult()` / `toStdlibResult()`.
- **Swift sees `KotlinResult<T>`.** Build one with `init(value:)` /
  `init(failure:)`, and unwrap it with `try r.get()` or
  `let v: String = try r.get()`. `r.result(as:)` gives a `Swift.Result`. A failure
  is thrown as the Kotlin exception itself.
- **`kotlinresult-testing`** adds `assertSuccess()` and `assertFailure<E>()`.

**Setup:** a KMP library must `export` `kotlinresult` into every Apple framework
that links it. See the README, "Using it from a KMP library".
