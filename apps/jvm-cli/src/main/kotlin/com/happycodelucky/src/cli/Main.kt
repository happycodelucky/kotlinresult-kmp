/*
 * __PROJECT_NAME__ — JVM CLI sample.
 *
 * Constructs the library's public API and prints the result. Replace the body
 * with a real harness for your library once the template is rendered.
 *
 * Run: mise run build:jvm && ./gradlew :jvm-cli:run
 */
package com.happycodelucky.src.cli

import com.happycodelucky.src.Greeter

fun main() {
    println(Greeter().greet())
}
