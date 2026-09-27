---
title: Architecture
---

# Architecture

TODO: describe how __DISPLAY_NAME__ is put together. The template ships with an
architecture worth keeping in mind as you flesh this out:

## Headless library

The library is headless — it contains no UI. It exposes a public Kotlin API that
each platform's application layer drives. Keeping it UI-free is what lets the
same code back an Android app, an iOS/macOS app, and a JVM service.

## Apple intermediate source set

iOS and macOS share an Apple intermediate source set. Code common to every Apple
target lives there once, above the per-target (`iosArm64`, `iosSimulatorArm64`,
`macosArm64`) leaves, so Apple-specific behaviour isn't duplicated.

## Dual distribution

The same library ships through two channels:

- **Maven Central** for Gradle / KMP consumers — Android AAR, multiplatform
  metadata, and per-target klibs.
- **GitHub Releases** (via KMMBridge) for pure-Swift SPM consumers — a
  SKIE-enhanced `__FRAMEWORK__.xcframework` zip referenced from `Package.swift`.

See [Publishing](https://github.com/happycodelucky/__PROJECT_NAME__/blob/main/.github/PUBLISHING.md)
for how the two channels are produced.
