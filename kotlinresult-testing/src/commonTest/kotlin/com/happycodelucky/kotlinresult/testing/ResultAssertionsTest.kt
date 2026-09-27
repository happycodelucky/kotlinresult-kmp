package com.happycodelucky.kotlinresult.testing

import com.happycodelucky.kotlinresult.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ResultAssertionsTest {
    private val boom = IllegalStateException("boom")

    @Test
    fun assertSuccess_returns_the_value() {
        assertEquals(42, Result.success(42).assertSuccess())
    }

    @Test
    fun assertSuccess_fails_on_a_failure() {
        val error = assertFailsWith<AssertionError> { Result.failure<Int>(boom).assertSuccess() }

        assertEquals(true, error.message?.contains("boom"))
    }

    @Test
    fun assertFailure_returns_the_matching_exception() {
        assertSame(boom, Result.failure<Int>(boom).assertFailure<IllegalStateException>())
    }

    @Test
    fun assertFailure_fails_on_a_success_or_another_type() {
        assertFailsWith<AssertionError> { Result.success(1).assertFailure<IllegalStateException>() }
        assertFailsWith<AssertionError> { Result.failure<Int>(boom).assertFailure<IllegalArgumentException>() }
    }
}
