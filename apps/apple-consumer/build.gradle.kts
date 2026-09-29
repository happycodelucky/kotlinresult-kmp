/*
 * kotlinresult — :apple-consumer, a stand-in for a downstream KMP library.
 *
 * `:kotlinresult` ships no framework (CLAUDE.md §8): Swift reaches it only through
 * a consumer's own framework. This module is that consumer, built the way the
 * README tells real ones to be: it depends on `:kotlinresult` with `api`, applies
 * SKIE, and `export`s `:kotlinresult` into its framework. Linking the framework
 * proves the Swift that `:kotlinresult` bundles into its klibs still compiles
 * there, and src/appleMain/swift exercises the helpers from consumer Swift
 * (`mise run build:swift`); `mise run test:swift` also runs swift/main.swift
 * against the macOS framework.
 *
 * Not published, and outside lint/check like the other sample apps.
 */

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("co.touchlab.skie")
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64(), macosArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "AppleConsumerKit"
            isStatic = true
            // Without this export the bundled KotlinResult+Swift.swift fails to
            // compile: `KotlinResult` would get a module-prefixed Swift name.
            export(project(":kotlinresult"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":kotlinresult"))
        }
    }
}

skie {
    analytics {
        disableUpload.set(true)
    }
}
