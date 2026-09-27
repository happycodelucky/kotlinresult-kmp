package com.happycodelucky.src.testing

import kotlin.test.Test
import kotlin.test.assertEquals

class FakeGreeterTest {
    @Test
    fun returnsScriptedGreeting() {
        assertEquals("Hi", FakeGreeter(greeting = "Hi").greet())
    }
}
