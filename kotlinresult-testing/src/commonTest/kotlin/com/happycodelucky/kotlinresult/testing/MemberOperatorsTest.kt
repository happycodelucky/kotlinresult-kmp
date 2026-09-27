package com.happycodelucky.kotlinresult.testing

// Deliberately imports ONLY the type: this file lives outside the library's
// package, so it compiles only while every operator is a member of `Result`.
import com.happycodelucky.kotlinresult.Result
import kotlin.test.Test
import kotlin.test.assertEquals

class MemberOperatorsTest {
    private val boom = IllegalStateException("boom")

    @Test
    fun operators_need_no_imports() {
        val seen = mutableListOf<String>()

        val doubled =
            Result
                .success(21)
                .map { it * 2 }
                .mapCatching { it + 0 }
                .onSuccess { seen += "success $it" }
                .onFailure { seen += "failure" }

        assertEquals(42, doubled.getOrDefault(0))
        assertEquals(42, doubled.getOrElse { 0 })
        assertEquals("42", doubled.fold(onSuccess = { it.toString() }, onFailure = { "failed" }))
        assertEquals(0, Result.failure<Int>(boom).recover { 0 }.assertSuccess())
        assertEquals(0, Result.failure<Int>(boom).recoverCatching { 0 }.assertSuccess())
        assertEquals(kotlin.Result.success(42), doubled.toStdlibResult())
        assertEquals(listOf("success 42"), seen)
    }
}
