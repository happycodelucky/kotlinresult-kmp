---
title: Changelog
# Changeset bodies are arbitrary Markdown; don't let mkdocs-macros read a
# `{{ … }}` in one as template syntax.
render_macros: false
---

# Changelog

Every release of KotlinResult, newest first. Each entry is assembled from
the changesets merged since the previous release, when its release PR is
opened.

<!-- changesets: the Release PR workflow inserts each new release below this line. Keep it. -->

## 1.0.1 — 2026-09-27

### Fixes

#### Don't force consumers onto compileSdk 37 ([#5](https://github.com/happycodelucky/kotlinresult-kmp/pull/5))

The Android AAR declared minCompileSdk 37 (inherited from our build's compileSdk), so Android/KMP consumers compiling against API 36 failed checkAarMetadata. It now declares 30, the library's minSdk; KotlinResult uses no Android APIs.

KotlinResult 1.0.0's Android AAR declared `minCompileSdk=37`: AGP stamps the
compileSdk a library is built with, and ours is 37 only for the Compose sample
app. Any consumer compiling against API 36 or lower failed
`checkAndroidMainAarMetadata` (for example wake-kmp's release build).

1.0.1 declares `minCompileSdk=30`, which is the library's `minSdk`. KotlinResult
uses no Android APIs, so nothing newer is required. There are no code changes.

## 1.0.0 — 2026-09-27

### Features

#### Result: a Swift-friendly kotlin.Result for KMP ([#1](https://github.com/happycodelucky/kotlinresult-kmp/pull/1))

First release: Result<T> mirrors the kotlin.Result API in Kotlin and appears as KotlinResult in Swift, with bundled get() / result(as:) helpers; plus assertSuccess() / assertFailure<E>() in kotlinresult-testing.

The first release of KotlinResult, shipped as a stable 1.0.0: `kotlin.Result` for Kotlin Multiplatform,
usable from Swift.

- **`com.happycodelucky.kotlinresult.Result<T>`** wraps and delegates to
  `kotlin.Result`. It has the stdlib API with the same names and contracts, as
  members, so nothing needs importing beyond `Result`: `isSuccess`, `getOrNull`,
  `getOrThrow`, `exceptionOrNull`, `fold`, `map`, `mapCatching`, `recover`,
  `recoverCatching`, `onSuccess`, `onFailure`, `getOrElse` and `getOrDefault`.
  `getOrElse`, `getOrDefault`, `recover` and `recoverCatching` return `T` rather
  than any supertype of it. Convert with `toResult()` / `toStdlibResult()`.
- **Swift sees `KotlinResult<T>`.** Build one with `init(value:)` /
  `init(failure:)`, and unwrap it with `try r.get()` or
  `let v: String = try r.get()`. `r.result(as:)` gives a `Swift.Result`. A failure
  is thrown as the Kotlin exception itself.
- **`kotlinresult-testing`** adds `assertSuccess()` and `assertFailure<E>()`.

**Setup:** a KMP library must `export` `kotlinresult` into every Apple framework
that links it. See the README, "Using it from a KMP library".
