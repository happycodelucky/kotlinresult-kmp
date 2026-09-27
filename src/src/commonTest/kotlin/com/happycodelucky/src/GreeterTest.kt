package com.happycodelucky.src

import kotlin.test.Test
import kotlin.test.assertTrue

class GreeterTest {
    @Test
    fun greetsWithPlatformName() {
        val greeting = Greeter().greet()
        assertTrue(greeting.startsWith("Hello from "), "unexpected greeting: $greeting")
    }
}
