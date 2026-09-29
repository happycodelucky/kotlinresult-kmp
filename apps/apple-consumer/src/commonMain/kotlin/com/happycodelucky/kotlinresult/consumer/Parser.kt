package com.happycodelucky.kotlinresult.consumer

import com.happycodelucky.kotlinresult.Result

/** A consumer-library API that returns [Result], as a downstream module would. */
public object Parser {
    /** [text] as an Int, or a failure holding the [NumberFormatException]. */
    public fun parseInt(text: String): Result<Int> =
        text.toIntOrNull()?.let { Result.success(it) } ?: Result.failure(NumberFormatException("not a number: $text"))

    /** Success with no value — Swift sees `KotlinResult<KotlinUnit>`. */
    public fun check(ok: Boolean): Result<Unit> = if (ok) Result.success(Unit) else Result.failure(IllegalStateException("check failed"))
}
