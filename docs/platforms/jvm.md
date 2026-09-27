---
title: JVM
---

# JVM

Nothing beyond the dependency — the stdlib is the only runtime dependency. Java
callers use `Result.success(x)` / `Result.failure(e)` (`@JvmStatic`) and call the
operators as instance methods (`result.getOrDefault(0)`).
