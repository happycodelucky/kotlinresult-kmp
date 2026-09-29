---
title: Drop the XCFramework / Swift Package Manager distribution
change: minor
description: KotlinResult now ships on Maven Central only. Swift keeps getting KotlinResult and its helpers through your KMP library's framework.
---

KotlinResult is a building block for KMP libraries, so it now ships through **Maven Central only**. There is no more `KotlinresultKit.xcframework` GitHub Release asset, and no Swift package: the tagged `Package.swift` is gone.

**If you depend on it from a KMP library** (the supported way), nothing changes. The Apple klibs still carry the SKIE-bundled Swift helpers (`get()`, `get(as:)`, `result(as:)`, `KotlinThrowable: Error`), your framework still compiles them, and it still has to `export` `kotlinresult`.

**If you added this repository as a Swift package**, remove that dependency and get `KotlinResult` from the KMP library that uses it instead. Releases up to 1.0.x keep their tags and assets, so an existing pin keeps resolving.
