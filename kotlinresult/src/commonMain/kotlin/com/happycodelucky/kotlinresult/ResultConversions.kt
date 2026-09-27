/*
 * KotlinResult — conversion to and from the stdlib `kotlin.Result`.
 *
 * `toResult` / `toStdlibResult` move between the two freely (a [Result] is a thin
 * wrapper). Kotlin-only (`@HiddenFromObjC`).
 *
 * There is deliberately no `runCatching` twin. A catch-everything block is the
 * wrong tool around coroutine code: it captures `CancellationException`, turning
 * cancellation into an ordinary failure, and the kotlinx.coroutines maintainers'
 * position (kotlinx.coroutines#1814) is that no catch-all variant is correct in
 * general — a `CancellationException` neither proves the current coroutine was
 * cancelled (`Deferred.await` rethrows another coroutine's) nor is safe to swallow
 * when it wasn't (`Flow.emit` uses one to end collection). Build failures from the
 * specific exceptions you can handle:
 *
 *     try { … } catch (e: IOException) { Result.failure(MyException.Io(e)) }
 *
 * `runCatching { … }.toResult()` remains available for non-suspending code, where
 * the trade-off is explicit at the call site.
 */
@file:OptIn(ExperimentalObjCRefinement::class)

package com.happycodelucky.kotlinresult

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

/** Wrap this stdlib `kotlin.Result` as a [Result]. */
@HiddenFromObjC
public fun <T> kotlin.Result<T>.toResult(): Result<T> = Result(this, false)

/** The underlying stdlib `kotlin.Result`. */
@HiddenFromObjC
public fun <T> Result<T>.toStdlibResult(): kotlin.Result<T> = result
