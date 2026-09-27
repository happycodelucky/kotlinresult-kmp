---
title: Android
---

# Android

Nothing beyond the dependency: no permissions, no manifest entries, no runtime
dependencies besides the Kotlin stdlib. Kotlin callers use the `kotlin.Result`
API; Java callers get `Result.success(x)` / `Result.failure(e)` (`@JvmStatic`).
