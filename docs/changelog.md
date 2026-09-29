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

## 1.1.0 — 2026-09-29

### Features

#### Drop the XCFramework / Swift Package Manager distribution ([#8](https://github.com/happycodelucky/kotlinresult-kmp/pull/8))

KotlinResult now ships on Maven Central only. Swift keeps getting KotlinResult and its helpers through your KMP library's framework.

KotlinResult is a building block for KMP libraries, so it now ships through **Maven Central only**. There is no more `KotlinresultKit.xcframework` GitHub Release asset, and no Swift package: the tagged `Package.swift` is gone.

**If you depend on it from a KMP library** (the supported way), nothing changes. The Apple klibs still carry the SKIE-bundled Swift helpers (`get()`, `get(as:)`, `result(as:)`, `KotlinThrowable: Error`), your framework still compiles them, and it still has to `export` `kotlinresult`.

**If you added this repository as a Swift package**, remove that dependency and get `KotlinResult` from the KMP library that uses it instead. Releases up to 1.0.x keep their tags and assets, so an existing pin keeps resolving.

### Fixes

#### Ship llms.txt and llms-full.txt inside every published artifact ([#7](https://github.com/happycodelucky/kotlinresult-kmp/pull/7))

Every jar and the AAR now carry the module's public API with its KDoc, for AI tools, under META-INF/com.happycodelucky.kotlinresult/<artifactId>/.

Every published jar (JVM, metadata, sources, javadoc) and the Android AAR now carry two files for AI coding tools, under `META-INF/com.happycodelucky.kotlinresult/<artifactId>/`:

- `llms.txt` — what the artifact is, its coordinates, and links to the docs site.
- `llms-full.txt` — the module's full public API with its KDoc, for exactly the version it ships in.

They are namespaced by coordinates, so they never collide with another library's, and in the AAR they sit at the archive root, so they never reach your APK. No API or behavior changes.

The docs site also serves `/llms.txt` and `/llms-full.txt`, and gains an [API reference](https://happycodelucky.github.io/kotlinresult-kmp/reference/) page.

#### Android consumers compile against API 34 or newer ([#7](https://github.com/happycodelucky/kotlinresult-kmp/pull/7))

The AAR's minCompileSdk rises from 30 to 34 (Android 14); minSdk stays 30.

The Android AAR now declares `minCompileSdk=34` (Android 14), up from 30, so the library can build on newer Android APIs. If your app or library compiles against an older SDK, raise `compileSdk` to 34 or newer. Devices are unaffected: `minSdk` is still 30.

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
