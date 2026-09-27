package com.happycodelucky.src.testing

/**
 * A scriptable fake mirroring the main module's public API, for use in consumer
 * tests without touching real platform code.
 *
 * Pattern: a fake returns canned data the test controls, so a consumer can
 * assert against the library's public types under `runTest` virtual time. Real
 * libraries grow this into fakes for clients, registries, or flows.
 *
 * Replace with fakes for your real API once the template is rendered.
 */
public class FakeGreeter(private val greeting: String = "Hello from Fake") {
    public fun greet(): String = greeting
}
