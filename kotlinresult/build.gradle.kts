/*
 * kotlinresult — :kotlinresult module.
 *
 * `Result<T>` (Swift: `KotlinResult<T>`), a Swift-friendly mirror of
 * `kotlin.Result`, plus its bundled Swift (src/appleMain/swift/). Stdlib-only: no
 * runtime dependencies. The module shape — target matrix (incl. jvm()), apple
 * intermediate source set, Android library block, compiler options — comes from
 * the `kotlinresult.kmp-library` convention plugin; Maven Central publishing
 * comes from `kotlinresult.publish`. This script keeps only what is unique to
 * the module: SKIE's Swift bundling and the POM name/description.
 *
 * Maven Central is the only channel (CLAUDE.md §8): KMP consumers link the
 * klibs into their own Apple frameworks. No XCFramework, no SPM.
 */

plugins {
    id("kotlinresult.kmp-library")
    id("kotlinresult.publish")
    // SKIE here ONLY to bundle src/appleMain/swift into the Apple klibs. This
    // module declares no framework, so SKIE logs "No Apple frameworks
    // configured" — expected: the consumer's SKIE compiles the bundled Swift
    // into the consumer's framework.
    id("co.touchlab.skie")
}

kotlin {
    sourceSets {
        // No main dependencies at all: a Result type ships with the stdlib only.

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        // androidHostTest is created by the convention plugin's
        // withHostTestBuilder. These test source sets don't inherit commonTest's
        // deps, so they're repeated.
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test"))
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

skie {
    analytics {
        disableUpload.set(true)
    }
    // SKIE copies src/appleMain/swift/KotlinResult+Swift.swift into each Apple
    // klib; every consumer framework that applies SKIE and `export`s
    // :kotlinresult compiles the Swift helpers (`get()`, `result(as:)`,
    // KotlinThrowable: Error) into itself. Without the export the file fails to
    // compile there (README, CLAUDE.md §7, LESSONS D-005). `mise run build:swift`
    // proves it on :apple-consumer.
    swiftBundling {
        enabled.set(true)
    }
}

mavenPublishing {
    pom {
        name.set("KotlinResult")
        description.set(
            "A Swift-friendly kotlin.Result for Kotlin Multiplatform: the stdlib " +
                "Result API in Kotlin, and KotlinResult with throwing get() / " +
                "Swift.Result in Swift.",
        )
    }
}
