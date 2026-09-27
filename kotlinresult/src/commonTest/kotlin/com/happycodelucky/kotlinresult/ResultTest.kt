package com.happycodelucky.kotlinresult

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [Result] must behave exactly like `kotlin.Result`; each case checks the
 * [Result] operation against the stdlib one it delegates to. (Inside this package
 * `Result` is ours, so the stdlib type is spelled `kotlin.Result`.)
 */
class ResultTest {
    private val boom = IllegalStateException("boom")

    @Test
    fun success_exposes_its_value() {
        val outcome = Result.success(42)

        assertTrue(outcome.isSuccess)
        assertFalse(outcome.isFailure)
        assertEquals(42, outcome.getOrNull())
        assertEquals(42, outcome.getOrThrow())
        assertNull(outcome.exceptionOrNull())
    }

    @Test
    fun failure_exposes_its_exception() {
        val outcome = Result.failure<Int>(boom)

        assertTrue(outcome.isFailure)
        assertFalse(outcome.isSuccess)
        assertNull(outcome.getOrNull())
        assertSame(boom, outcome.exceptionOrNull())
        assertSame(boom, assertFailsWith<IllegalStateException> { outcome.getOrThrow() })
    }

    @Test
    fun a_null_success_is_still_a_success() {
        val outcome = Result.success<String?>(null)

        assertTrue(outcome.isSuccess)
        assertNull(outcome.getOrNull())
    }

    @Test
    fun converts_to_and_from_kotlin_Result() {
        val result = kotlin.Result.success("x")

        assertEquals(result, result.toResult().toStdlibResult())
        assertSame(
            boom,
            kotlin.Result
                .failure<Int>(boom)
                .toResult()
                .exceptionOrNull(),
        )
    }

    @Test
    fun equality_and_toString_follow_kotlin_Result() {
        assertEquals(Result.success(1), Result.success(1))
        assertEquals(Result.success(1).hashCode(), Result.success(1).hashCode())
        assertEquals(kotlin.Result.success(1).toString(), Result.success(1).toString())
        assertEquals(kotlin.Result.failure<Int>(boom).toString(), Result.failure<Int>(boom).toString())
    }

    @Test
    fun map_and_fold() {
        assertEquals(Result.success(4), Result.success(2).map { it * 2 })
        assertSame(boom, Result.failure<Int>(boom).map { it * 2 }.exceptionOrNull())
        assertEquals("2", Result.success(2).fold({ it.toString() }, { "failed" }))
        assertEquals("failed", Result.failure<Int>(boom).fold({ it.toString() }, { "failed" }))
    }

    @Test
    fun mapCatching_captures_a_throwing_transform() {
        val outcome = Result.success(2).mapCatching { throw boom }

        assertSame(boom, outcome.exceptionOrNull())
    }

    @Test
    fun getOrElse_and_getOrDefault() {
        assertEquals(1, Result.success(1).getOrElse { 0 })
        assertEquals(0, Result.failure<Int>(boom).getOrElse { 0 })
        assertEquals(0, Result.failure<Int>(boom).getOrDefault(0))
    }

    @Test
    fun recover_turns_a_failure_into_a_success() {
        assertEquals(Result.success(0), Result.failure<Int>(boom).recover { 0 })
        assertEquals(Result.success(1), Result.success(1).recover { 0 })
        assertIs<UnsupportedOperationException>(
            Result.failure<Int>(boom).recoverCatching { throw UnsupportedOperationException() }.exceptionOrNull(),
        )
    }

    @Test
    fun onSuccess_and_onFailure_run_only_on_their_branch() {
        val seen = mutableListOf<String>()

        Result.success(1).onSuccess { seen += "success $it" }.onFailure { seen += "failure" }
        Result.failure<Int>(boom).onSuccess { seen += "success" }.onFailure { seen += "failure ${it.message}" }

        assertEquals(listOf("success 1", "failure boom"), seen)
    }

    @Test
    fun constructors_match_the_factories() {
        assertEquals(Result.success(1), Result(value = 1))
        assertEquals(Result.failure<Int>(boom), Result<Int>(failure = boom))
        // A Throwable *value* needs the named argument (or `success`); positional
        // resolves to the more specific failure constructor, as documented.
        assertTrue(Result<Throwable>(value = boom).isSuccess)
    }
}
