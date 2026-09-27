/*
 * kotlinresult — JVM CLI sample.
 *
 * Parses each argument as a TCP/UDP port, returning a `Result<Int>`, and prints
 * the outcome with `fold` — the same API as `kotlin.Result`.
 *
 * Run: ./gradlew :jvm-cli:run --args="8080 nope 70000"
 */
package com.happycodelucky.kotlinresult.cli

import com.happycodelucky.kotlinresult.Result
import com.happycodelucky.kotlinresult.fold

private const val MAX_PORT = 65_535

/** Parse [text] as a port number, failing with a descriptive exception. */
fun parsePort(text: String): Result<Int> {
    val port = text.toIntOrNull() ?: return Result.failure(IllegalArgumentException("not a number: \"$text\""))
    if (port !in 0..MAX_PORT) return Result.failure(IllegalArgumentException("out of range: $port"))
    return Result.success(port)
}

fun main(args: Array<String>) {
    for (arg in args.ifEmpty { arrayOf("8080", "nope", "70000") }) {
        parsePort(arg).fold(
            onSuccess = { println("$arg -> port $it") },
            onFailure = { println("$arg -> ${it.message}") },
        )
    }
}
