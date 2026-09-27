---
title: Use it from a KMP library
---

# Use it from a KMP library

`Result` appears in your public API, so depend on it with `api`, and **export it
into every Apple framework that links it**:

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        api("com.happycodelucky.kotlinresult:kotlinresult:{{ version }}")
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework>().configureEach {
            export("com.happycodelucky.kotlinresult:kotlinresult:{{ version }}")
        }
    }
}
```

## Why the export is required

The Swift helpers (`get()`, `result(as:)`, `KotlinThrowable: Error`) ship as
SKIE-bundled Swift inside the klib. SKIE compiles the bundled Swift of *every*
linked klib into your framework — there is no per-dependency opt-out — and that
file only compiles where `KotlinResult` keeps its plain Swift name, which is what
`export` guarantees. Without it the link fails with:

```text
bundled.kotlinresult.KotlinResult+Swift.swift: error: cannot find type 'KotlinResult' in scope
```

Also:

- Your framework must be built with **SKIE**.
- Don't declare your own `extension KotlinThrowable: Error` — this library
  provides it, and a second conformance won't compile.
- Each framework gets its own copy of the type (`WakeKit.KotlinResult` and
  `OtherKit.KotlinResult` are distinct), as with every Kotlin type exported to
  Swift. The helpers work on each.

## Swift fakes

Swift can build a `KotlinResult` to return from a fake of a Kotlin interface:

```swift
let ok = KotlinResult<NSString>(value: "hi")
let failed = KotlinResult<KotlinUnit>(failure: KotlinThrowable(message: "boom"))
```
