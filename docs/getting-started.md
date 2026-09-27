---
title: Getting started
---

# Getting started

Return a `Result<T>` from your API, failing with the specific exceptions you
handle — ideally a sealed hierarchy, so callers can match every case:

```kotlin
import com.happycodelucky.kotlinresult.Result

sealed class PortException(message: String) : Exception(message) {
    class NotANumber(val text: String) : PortException("not a number: $text")
    class OutOfRange(val port: Int) : PortException("out of range: $port")
}

fun parsePort(text: String): Result<Int> {
    val port = text.toIntOrNull() ?: return Result.failure(PortException.NotANumber(text))
    return if (port in 0..65535) Result.success(port) else Result.failure(PortException.OutOfRange(port))
}
```

Kotlin callers use the familiar `kotlin.Result` API:

```kotlin
parsePort("8080")
    .map { it + 1 }
    .onFailure { e -> println("bad port: ${e.message}") }
    .getOrDefault(0)
```

Swift callers unwrap with `get()`, naming the value type:

```swift
do {
    let port: Int = try parsePort(text: "8080").get()
} catch let e as PortException {
    switch onEnum(of: e) {
    case .notANumber(let x): print("not a number: \(x.text)")
    case .outOfRange(let x): print("out of range: \(x.port)")
    }
}
```

Two rules keep this working:

1. **`export` the module** into every Apple framework that links it
   ([recipe](recipes/kmp-library.md)).
2. **Never capture cancellation** into a `Result` ([recipe](recipes/cancellation.md)).
