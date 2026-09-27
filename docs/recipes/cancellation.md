---
title: Cancellation and structured concurrency
---

# Cancellation and structured concurrency

A `Result` is a plain value, exactly like `kotlin.Result`: constructing one never
throws, and nothing is classified as "fatal". **Cancellation is not a failure** —
it must propagate as a thrown `CancellationException`, never be captured into a
`Result`. Then SKIE does the right thing at the Swift boundary: cancelling the
Swift `Task` cancels the coroutine, and the coroutine's cancellation arrives in
Swift as `CancellationError`.

Build failures from the **specific** exceptions you handle; let everything else
propagate:

```kotlin
suspend fun fetch(url: String): Result<String> =
    try {
        Result.success(client.get(url))
    } catch (e: IOException) {
        Result.failure(FetchException.Network(e))
    }
```

## Don't

- **Catch everything around suspending code** — `catch (e: Throwable)`,
  `runCatching { }`, `mapCatching { }`. It captures `CancellationException` and
  turns cancellation into an ordinary failure. There is deliberately no
  `runCatching` twin in this library: the kotlinx.coroutines maintainers hold that
  no catch-all variant is correct in general
  ([kotlinx.coroutines#1814](https://github.com/Kotlin/kotlinx.coroutines/issues/1814)).
- **Let an internal `withTimeout` escape** a public suspend function. SKIE reports
  its `TimeoutCancellationException` to Swift as `CancellationError`, although the
  caller was never cancelled. Use `withTimeoutOrNull` and fail with a domain
  exception:

```kotlin
suspend fun fetch(url: String): Result<String> =
    withTimeoutOrNull(5.seconds) { client.get(url) }
        ?.let { Result.success(it) }
        ?: Result.failure(FetchException.Timeout(url))
```
