/*
 * KotlinResult — a Swift-friendly `kotlin.Result`.
 *
 * Why not `kotlin.Result` itself: it is a value class, and ObjC export erases a
 * value class to its underlying type — `kotlin.Result<T>` reaches Swift as an
 * untyped `Any?` (KT-32352). Nor can a platform's native type sit under an
 * `expect class Result`: `actual typealias = kotlin.Result` is a compile error
 * (`kotlin.Result<out T>` has declaration-site variance), and Swift's `Result` is
 * not reachable from Kotlin/Native at all.
 *
 * So [Result] is an ordinary (reference) class that *wraps* a `kotlin.Result` and
 * forwards to it: Kotlin gets the stdlib `Result` API and semantics verbatim (see
 * ResultOperators.kt, and [toResult] / [toStdlibResult] to convert), and Swift
 * gets a real class it can hold — named `KotlinResult` there, alongside
 * `KotlinInt`, `KotlinUnit` and the other Kotlin types, so it never shadows
 * Swift's own `Result`.
 *
 * Inside this package the simple name `Result` means this class (a same-package
 * declaration beats the default `kotlin.*` import), so the stdlib type is always
 * written `kotlin.Result` here. The same applies in consumer files that import
 * `com.happycodelucky.kotlinresult.Result`.
 *
 * The Swift side is written once, in `src/appleMain/swift/KotlinResult+Swift.swift`,
 * which SKIE's Swift bundling compiles into every framework that `export`s this
 * module. It adds `try result.get()` (the value bridged to a Swift type, or the
 * Kotlin exception thrown as a Swift `Error`) and `result.result(as:)`
 * (`Swift.Result`). No per-function or per-error Swift is needed: a library
 * returns `Result<T>` from commonMain and fails with its own sealed exception
 * hierarchy, which SKIE renders for `onEnum(of:)`.
 */
@file:OptIn(ExperimentalObjCRefinement::class, ExperimentalObjCName::class)

package com.happycodelucky.kotlinresult

import kotlin.experimental.ExperimentalObjCName
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.jvm.JvmStatic
import kotlin.native.HiddenFromObjC
import kotlin.native.ObjCName
import kotlin.native.ShouldRefineInSwift

/**
 * A discriminated union of a successful value of type [T] or a failure
 * [Throwable] — `kotlin.Result`, in a form Swift can consume (as `KotlinResult`).
 *
 * Every operation delegates to an underlying `kotlin.Result`, so behavior is the
 * stdlib's exactly. The transforming operators (`map`, `fold`, `onFailure`, …)
 * are extensions with the stdlib's signatures; [toStdlibResult] returns the
 * underlying `kotlin.Result` when an API wants the stdlib type.
 *
 * ```kotlin
 * fun parsePort(text: String): Result<Int> =
 *     text.toIntOrNull()?.takeIf { it in 0..65535 }?.let { Result.success(it) }
 *         ?: Result.failure(IllegalArgumentException("not a port: $text"))
 *
 * parsePort("8080").map { it + 1 }.getOrDefault(0) // 8081
 * ```
 *
 * ### Coroutines and cancellation
 *
 * A [Result] is a plain value, exactly like `kotlin.Result`: constructing one
 * never throws and nothing is classified as "fatal". Cancellation is not a
 * failure — it must propagate as a thrown `CancellationException`, never be
 * captured into a [Result]. So a suspending API returns a [Result] built from the
 * specific exceptions it handles and lets everything else, cancellation included,
 * propagate. Across the Swift boundary SKIE then does the right thing: cancelling
 * the Swift `Task` cancels the coroutine, and the coroutine's cancellation arrives
 * in Swift as `CancellationError`. Two things break that: catching everything
 * (see ResultConversions.kt — there is deliberately no `runCatching` twin) and
 * letting an internal `withTimeout` escape (SKIE reports its
 * `TimeoutCancellationException` to Swift as `CancellationError`, though the
 * caller was never cancelled) — use `withTimeoutOrNull` and fail with a domain
 * exception instead.
 *
 * ### Swift
 *
 * Unwrap with the bundled `get()`, and construct one with the `init(value:)` /
 * `init(failure:)` initializers — e.g. to return a `KotlinResult` from a Swift
 * fake of a Kotlin interface:
 *
 * ```swift
 * let port: Int = try parsePort(text: "8080").get()
 * let ok = KotlinResult<NSString>(value: "hi")
 * let failed = KotlinResult<NSString>(failure: KotlinThrowable(message: "boom"))
 * ```
 */
@ObjCName(name = "KotlinResult", swiftName = "KotlinResult")
public class Result<out T> internal constructor(
    @PublishedApi internal val result: kotlin.Result<T>,
    // Unused; disambiguates this constructor from `constructor(value: T)`, which
    // would otherwise clash on the JVM (`kotlin.Result<T>` and `T` both erase to
    // Object).
    @Suppress("UNUSED_PARAMETER") disambiguator: Boolean,
) {
    /**
     * A successful [Result] holding [value]. Kotlin callers should prefer
     * [Result.success], which can't be confused with the failure constructor when
     * [T] is itself a `Throwable`. This constructor exists so Swift can build one
     * (`KotlinResult(value:)`).
     */
    public constructor(value: T) : this(kotlin.Result.success(value), false)

    /**
     * A failed [Result] holding [failure]. Kotlin callers should prefer
     * [Result.failure]. This constructor exists so Swift can build one
     * (`KotlinResult(failure:)`).
     */
    public constructor(failure: Throwable) : this(kotlin.Result.failure(failure), false)

    /** `true` if this is a success. The opposite of [isFailure]. */
    public val isSuccess: Boolean
        get() = result.isSuccess

    /** `true` if this is a failure. The opposite of [isSuccess]. */
    public val isFailure: Boolean
        get() = result.isFailure

    /** The success value, or `null` on failure. Same as `kotlin.Result.getOrNull`. */
    public fun getOrNull(): T? = result.getOrNull()

    /** The failure's exception, or `null` on success. Same as `kotlin.Result.exceptionOrNull`. */
    public fun exceptionOrNull(): Throwable? = result.exceptionOrNull()

    /**
     * The success value, or throws the failure's exception. Same as
     * `kotlin.Result.getOrThrow`.
     *
     * Hidden from Swift, where an undeclared Kotlin throw would abort; Swift uses
     * the bundled `get()` instead.
     */
    @HiddenFromObjC
    public fun getOrThrow(): T = result.getOrThrow()

    /**
     * The success value with its static type erased, for the bundled Swift.
     *
     * Swift extensions of a generic ObjC class cannot touch its generic parameter,
     * so the bundled `get<V>()` reads this untyped `Any?` (exposed as
     * `__anyValue`) and bridges it with `as? V`. Kotlin callers use [getOrNull].
     */
    @InternalKotlinResultApi
    @ShouldRefineInSwift
    public val anyValue: Any?
        get() = result.getOrNull()

    override fun equals(other: Any?): Boolean = other is Result<*> && other.result == result

    override fun hashCode(): Int = result.hashCode()

    /** `Success(v)` or `Failure(x)`, as `kotlin.Result` prints. */
    override fun toString(): String = result.toString()

    /**
     * Factories mirroring `kotlin.Result.success` / `kotlin.Result.failure` — the
     * preferred way to build a [Result] in Kotlin (and, via `@JvmStatic`, from
     * Java as `Result.success(x)`). Hidden from Swift, which uses the initializers.
     */
    @HiddenFromObjC
    public companion object {
        /** A successful [Result] holding [value]. */
        @JvmStatic
        public fun <T> success(value: T): Result<T> = Result(kotlin.Result.success(value), false)

        /** A failed [Result] holding [exception]. */
        @JvmStatic
        public fun <T> failure(exception: Throwable): Result<T> = Result(kotlin.Result.failure(exception), false)
    }
}

/**
 * Marks [Result] members that exist only for the bundled Swift. Not for Kotlin
 * callers — using one is a compile error unless explicitly opted in.
 */
@RequiresOptIn(
    message = "Swift-bridge plumbing for KotlinResult's bundled Swift. Kotlin callers should use the Result API.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
public annotation class InternalKotlinResultApi
