/*
 * kotlinresult — :kotlinresult module.
 *
 * `Result<T>` (Swift: `KotlinResult<T>`), a Swift-friendly mirror of
 * `kotlin.Result`, plus its bundled Swift (src/appleMain/swift/). Stdlib-only: no
 * runtime dependencies. The module shape — target matrix (incl. jvm()), apple
 * intermediate source set, Android library block, compiler options, SKIE
 * settings — comes from the `kotlinresult.kmp-library`
 * convention plugin; Maven Central publishing comes from
 * `kotlinresult.publish`. This script keeps only what is unique to the
 * module: dependencies, the KMMBridge SPM distribution config, and POM
 * name/description.
 */

plugins {
    id("kotlinresult.kmp-library")
    id("kotlinresult.publish")
    // KMMBridge (CLAUDE.md §8): aggregates the per-target Apple frameworks the
    // convention plugin declared into `KotlinresultKit.xcframework`, publishes the
    // release zip as a GitHub Release asset, and regenerates the root
    // Package.swift. The `.github` variant is a superset of the core plugin in
    // 1.2.x — applying both produces a duplicate-extension error, so only this.
    //
    // Do NOT redeclare `XCFramework("KotlinresultKit")` in the kotlin { } block:
    // KMMBridge auto-creates the aggregator from the framework binaries at
    // config time.
    alias(libs.plugins.kmmbridge.github)
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
    // ON for this module (the convention plugin turns it off by default): SKIE
    // copies src/appleMain/swift/KotlinResult+Swift.swift into the klib, so every
    // framework that depends on and `export`s :kotlinresult compiles the Swift
    // helpers (`get()`, `result(as:)`, KotlinThrowable: Error) into itself. The
    // plugin's concern — bundled Swift failing where the type is module-prefixed —
    // is exactly what the `export` requirement rules out (README, CLAUDE.md §7).
    swiftBundling {
        enabled.set(true)
    }
    build {
        // Xcode 26 requires .swiftinterface files in every framework slice
        // before xcodebuild -create-xcframework will accept them (exit 70
        // otherwise). produceDistributableFramework() enables Swift library
        // evolution so SKIE emits .swiftinterface alongside .swiftmodule.
        // `:kotlinresult-testing` doesn't need this — it isn't shipped as an XCFramework.
        produceDistributableFramework()
    }
}

// --- KMMBridge: XCFramework → GitHub Release asset → SPM (CLAUDE.md §8) ------
//
// Two distribution channels run from this module, and they don't overlap:
//   1. Maven Central (`kotlinresult.publish`) — Android AAR, the jvm jar,
//      `kotlinMultiplatform` metadata, and per-target klibs.
//   2. GitHub Releases (this block) — the SKIE-enhanced
//      `KotlinresultKit.xcframework` zip for pure-Swift consumers, referenced from
//      the root /Package.swift by URL + checksum.
//
// Publishing is CI-only: the `kmmBridgePublish` umbrella task is only
// registered when `-PENABLE_PUBLISHING=true` is passed, and the upload reads the
// `GITHUB_REPO` / `GITHUB_PUBLISH_TOKEN` Gradle properties —
// .github/workflows/release.yml supplies all three. Local builds skip the
// publish wiring entirely; `spmDevBuild` (always registered) is the local-dev
// entry point — see mise task `spm:dev`.
gitHubReleaseArtifacts(releasString = "v${project.version}")

// The XCFramework's Swift module name. DERIVED from the module name the same way
// the convention plugin derives each framework binary's baseName (src → "SrcKit";
// after `init`, my-app → "MyAppKit"), so the two can never drift and no token is
// needed. If they disagreed, the generated Package.swift would reference a binary
// that doesn't exist.
val xcframeworkBaseName = project.name.split("-").joinToString("") { it.replaceFirstChar(Char::uppercase) } + "Kit"

kmmbridge {
    frameworkName.set(xcframeworkBaseName)

    // `swiftToolVersion = "6.0"` because the platform constants `.iOS(.v18)` and
    // `.macOS(.v15)` need PackageDescription 6.0; KMMBridge defaults to 5.3,
    // which can't compile them. The floors match gradle/libs.versions.toml
    // (ios-deployment-target = 18.0, macos-deployment-target = 15.0); they're
    // spelled "18"/"15" here because KMMBridge emits `.iOS(.v$value)` verbatim —
    // "18.0" would produce the non-existent constant `.v18.0`.
    spm(swiftToolVersion = "6.0") {
        iOS { v("18") }
        macOS { v("15") }
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
