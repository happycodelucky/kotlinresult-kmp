---
title: Don't force consumers onto compileSdk 37
change: patch
description: The Android AAR declared minCompileSdk 37 (inherited from our build's compileSdk), so Android/KMP consumers compiling against API 36 failed checkAarMetadata. It now declares 30, the library's minSdk; KotlinResult uses no Android APIs.
---

KotlinResult 1.0.0's Android AAR declared `minCompileSdk=37`: AGP stamps the
compileSdk a library is built with, and ours is 37 only for the Compose sample
app. Any consumer compiling against API 36 or lower failed
`checkAndroidMainAarMetadata` (for example wake-kmp's release build).

1.0.1 declares `minCompileSdk=30`, which is the library's `minSdk`. KotlinResult
uses no Android APIs, so nothing newer is required. There are no code changes.
