---
title: Design
---

# Design

## Why a wrapper class

`kotlin.Result` is a value class. Kotlin/Native's Objective-C export erases value
classes to their underlying type, so `kotlin.Result<T>` reaches Swift as an
untyped `Any?` (KT-32352). An `expect class Result` with
`actual typealias = kotlin.Result` doesn't compile either (`Result<out T>` has
declaration-site variance), and Swift's `Result` isn't reachable from Kotlin/Native.
So `Result<T>` is an ordinary class holding a `kotlin.Result` and delegating every
operation to it — the stdlib's behaviour, in a type Swift can hold.

## Naming

Kotlin calls it `Result` (package `com.happycodelucky.kotlinresult`); in files
that import it, it shadows `kotlin.Result`, which is then written in full. Swift
and Objective-C call it `KotlinResult`, in line with `KotlinInt`, `KotlinUnit`
and the other Kotlin types, so it never shadows Swift's `Result`.

## The Swift half

Kotlin generic classes are exported as Objective-C lightweight-generic classes,
and Swift rejects any (non-`@objc`) member in an extension of one — even members
that don't mention `T`. So the helpers live on a non-generic protocol,
`AnyKotlinResult`, that `KotlinResult` conforms to: its requirements expose the
value type-erased (`__anyValue`, Kotlin's `Any?`), and the generic `get<V>()`
bridges it with `as? V`. That is why Swift names the value type
(`let s: String = try r.get()`): the class's own `T` is the Objective-C-bridged
type (`NSString`, `KotlinInt`), and a wrong type is a runtime
`KotlinResultTypeMismatchError`.

## Distribution

- **Maven Central** — per-target klibs (carrying the bundled Swift), the Android
  AAR, the JVM jar.
- **GitHub Releases** (KMMBridge) — `KotlinresultKit.xcframework` for pure-Swift
  consumers, though most consumers get `KotlinResult` inside the framework of the
  KMP library that exports it.
