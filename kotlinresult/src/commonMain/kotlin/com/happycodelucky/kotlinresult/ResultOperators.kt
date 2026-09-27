/*
 * KotlinResult — the `kotlin.Result` operator set, verbatim.
 *
 * Each function has the stdlib `kotlin.Result` extension's name and signature and
 * delegates to it, so moving code between `kotlin.Result` and [Result] is a type
 * change only. All are `@HiddenFromObjC`: most take Kotlin lambdas, which bridge
 * poorly, and Swift converts once with `result(as:)` (bundled Swift) and then uses
 * `Swift.Result`'s own operators.
 *
 * ExperimentalContracts opt-in: `callsInPlace` is declared exactly where the
 * stdlib operators declare it (getOrElse, fold, map, recover, onSuccess,
 * onFailure — not the *Catching ones), so Kotlin's flow analysis treats [Result]
 * lambdas like `kotlin.Result` ones. Rollback: delete the `contract` blocks.
 */
@file:OptIn(ExperimentalObjCRefinement::class, ExperimentalContracts::class)

package com.happycodelucky.kotlinresult

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

/** The value, or [onFailure]'s result for the exception. Same as `kotlin.Result.getOrElse`. */
@HiddenFromObjC
public inline fun <R, T : R> Result<T>.getOrElse(onFailure: (exception: Throwable) -> R): R {
    contract { callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE) }
    return result.getOrElse(onFailure)
}

/** The value, or [defaultValue] on failure. Same as `kotlin.Result.getOrDefault`. */
@HiddenFromObjC
public fun <R, T : R> Result<T>.getOrDefault(defaultValue: R): R = result.getOrDefault(defaultValue)

/** [onSuccess] of the value or [onFailure] of the exception. Same as `kotlin.Result.fold`. */
@HiddenFromObjC
public inline fun <R, T> Result<T>.fold(
    onSuccess: (value: T) -> R,
    onFailure: (exception: Throwable) -> R,
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return result.fold(onSuccess, onFailure)
}

/** Transform the value; a failure passes through. Same as `kotlin.Result.map`. */
@HiddenFromObjC
public inline fun <R, T> Result<T>.map(transform: (value: T) -> R): Result<R> {
    contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }
    return result.map(transform).toResult()
}

/**
 * Like [map], but an exception from [transform] becomes a failure. Same as
 * `kotlin.Result.mapCatching` — including that it captures *every* exception, so
 * don't call suspending code in [transform] (a cancellation would become a
 * failure).
 */
@HiddenFromObjC
public inline fun <R, T> Result<T>.mapCatching(transform: (value: T) -> R): Result<R> {
    val caught = result.mapCatching(transform)
    return caught.toResult()
}

/** Turn a failure into a success; a success passes through. Same as `kotlin.Result.recover`. */
@HiddenFromObjC
public inline fun <R, T : R> Result<T>.recover(transform: (exception: Throwable) -> R): Result<R> {
    contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }
    return result.recover(transform).toResult()
}

/**
 * Like [recover], but an exception from [transform] becomes a failure. Same as
 * `kotlin.Result.recoverCatching` — and, like it, captures every exception: keep
 * suspending code out of [transform].
 */
@HiddenFromObjC
public inline fun <R, T : R> Result<T>.recoverCatching(transform: (exception: Throwable) -> R): Result<R> {
    val caught = result.recoverCatching(transform)
    return caught.toResult()
}

/** Run [action] on the exception if this is a failure; returns this. Same as `kotlin.Result.onFailure`. */
@HiddenFromObjC
public inline fun <T> Result<T>.onFailure(action: (exception: Throwable) -> Unit): Result<T> {
    contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
    result.onFailure(action)
    return this
}

/** Run [action] on the value if this is a success; returns this. Same as `kotlin.Result.onSuccess`. */
@HiddenFromObjC
public inline fun <T> Result<T>.onSuccess(action: (value: T) -> Unit): Result<T> {
    contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
    result.onSuccess(action)
    return this
}
