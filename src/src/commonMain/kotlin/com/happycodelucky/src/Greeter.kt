package com.happycodelucky.src

import co.touchlab.kermit.Logger

/**
 * Placeholder public API for the template.
 *
 * Demonstrates the conventions a real library follows:
 *  - a small, headless public surface (no UI types);
 *  - Kermit for logging (wired in the convention plugin, [Logger] is available
 *    in `commonMain` on every target);
 *  - the `expect`/`actual` seam kept tiny — only [platformName] crosses it.
 *
 * Replace this with your library's real entry point once `mise run init` has
 * rendered the template.
 */
public class Greeter {
    private val log = Logger.withTag("Greeter")

    /** Returns a platform-tagged greeting, e.g. `"Hello from iOS"`. */
    public fun greet(): String {
        val message = "Hello from ${platformName()}"
        log.d { message }
        return message
    }
}

/**
 * The human-readable platform name for the current target.
 *
 * The seam is intentionally minimal — one `expect`, four trivial `actual`s
 * (apple/android/jvm). Push everything else into `commonMain`.
 */
internal expect fun platformName(): String
