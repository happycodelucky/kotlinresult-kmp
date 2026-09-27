/*
 * :kotlinresult-testing — assertions for tests of code that returns a [Result].
 *
 * Framework-agnostic (they throw `AssertionError`, which kotlin.test, JUnit and
 * Kotest all report as a failure), so consumers can use them from any test
 * source set without pulling in a specific assertion library.
 */
package com.happycodelucky.kotlinresult.testing

import com.happycodelucky.kotlinresult.Result

/**
 * Asserts this [Result] is a success and returns its value.
 *
 * ```kotlin
 * val port = parsePort("8080").assertSuccess()
 * ```
 *
 * @throws AssertionError if this is a failure; the message carries the failure's
 *   stack trace.
 */
public fun <T> Result<T>.assertSuccess(): T {
    val exception = exceptionOrNull()
    if (exception != null) {
        throw AssertionError("Expected a success, but it failed with:\n${exception.stackTraceToString()}")
    }
    return getOrThrow()
}

/**
 * Asserts this [Result] is a failure holding an [E] and returns that exception.
 *
 * ```kotlin
 * val error = parsePort("nope").assertFailure<IllegalArgumentException>()
 * ```
 *
 * @throws AssertionError if this is a success, or a failure of another type.
 */
public inline fun <reified E : Throwable> Result<*>.assertFailure(): E =
    when (val exception = exceptionOrNull()) {
        null -> throw AssertionError("Expected a failure of ${E::class.simpleName}, but it succeeded with ${getOrNull()}")

        is E -> exception

        else -> throw AssertionError(
            "Expected a failure of ${E::class.simpleName}, but it failed with:\n${exception.stackTraceToString()}",
        )
    }
