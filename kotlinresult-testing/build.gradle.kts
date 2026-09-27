/*
 * kotlinresult — :kotlinresult-testing module (renamed to :<name>-testing by init).
 *
 * Public, scriptable test fakes and helpers for consumers of `:kotlinresult`. Same module
 * shape as `:kotlinresult` via the `kotlinresult.kmp-library` convention plugin;
 * published in lockstep (same group / version / pipeline) via
 * `kotlinresult.publish`. Consumers wire it on `testImplementation` (or KMP
 * `commonTest` deps); the production `:kotlinresult` artifact does not depend on it.
 *
 * No XCFramework and no SKIE `produceDistributableFramework()`: test code is
 * consumed as KMP klibs from Maven Central, not via SPM. The Apple targets exist
 * so KMP consumers can resolve this module from their Apple test source sets, but
 * we don't ship a binary framework for it.
 */

plugins {
    id("kotlinresult.kmp-library")
    id("kotlinresult.publish")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api` so consumers writing `testImplementation(<name>-testing)` get
            // the public `:kotlinresult` types transitively — they will assert against them.
            api(project(":kotlinresult"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        getByName("androidHostTest").dependencies {
            implementation(kotlin("test"))
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Export :kotlinresult into this module's framework. SKIE compiles the bundled
// Swift of every linked klib — including :kotlinresult's KotlinResult+Swift.swift
// — into each framework, and that file only compiles where `KotlinResult` keeps
// its plain Swift name, i.e. where :kotlinresult is exported. Every framework that
// links :kotlinresult needs this (README "Using it from a KMP library").
kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework>().configureEach {
            export(project(":kotlinresult"))
        }
    }
}

mavenPublishing {
    pom {
        name.set("KotlinResult Testing")
        description.set(
            "Test assertions for the kotlinresult library: assertSuccess() / " +
                "assertFailure<E>() for code that returns a KotlinResult Result.",
        )
    }
}
