---
title: JVM
---

# JVM

Nothing beyond the dependency — the stdlib is the only runtime dependency. Java
callers use `Result.success(x)` / `Result.failure(e)` (`@JvmStatic`); the
operators are extension functions in `ResultOperatorsKt`.
