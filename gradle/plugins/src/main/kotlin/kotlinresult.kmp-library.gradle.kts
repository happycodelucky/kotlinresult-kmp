/*
 * Convention plugin: the shared module shape for this library's published KMP
 * modules (`:kotlinresult`, `:kotlinresult-testing`).
 *
 * Owns everything the modules would otherwise duplicate (CLAUDE.md §4, §5): the
 * target matrix, the apple intermediate source set, the Android library block,
 * the jvm() target, compiler options and JVM target wiring. The Android
 * namespace is DERIVED from the project name, so adding a module means applying
 * this plugin and nothing else:
 *
 *   kotlinresult          → com.happycodelucky.kotlinresult
 *   kotlinresult-testing  → com.happycodelucky.kotlinresult.testing
 *
 * The Apple targets publish klibs only — no framework is built or shipped here
 * (CLAUDE.md §8): consumers link the klibs into their own frameworks.
 *
 * Module build scripts keep only what genuinely differs: dependencies, SKIE's
 * Swift bundling (`:kotlinresult` only), and POM name/description.
 */

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.dokka")
}

// Typed `libs` accessors aren't generated inside precompiled script plugins;
// the named-lookup API reads the same catalog the main build uses.
val libs = the<VersionCatalogsExtension>().named("libs")

// kotlinresult → com.happycodelucky.kotlinresult; kotlinresult-testing → ….kotlinresult.testing.
val moduleNamespace = "com.happycodelucky." + name.replace("-", ".")

// Bytecode level for BOTH JVM-flavored targets (android + jvm) — a consumer
// contract, deliberately independent of the JDK that runs the build.
val jvmBytecodeTarget =
    JvmTarget.fromTarget(
        libs
            .findVersion("jvm-target")
            .get()
            .requiredVersion,
    )

kotlin {
    // CLAUDE.md §4: source-set wiring is Kotlin's DEFAULT hierarchy template,
    // applied implicitly — no `applyDefaultHierarchyTemplate { }` block. For these
    // targets it yields commonMain → nativeMain → appleMain → {iosMain, macosMain},
    // plus jvmMain / androidMain siblings. appleMain holds code shared by iOS and
    // macOS (Foundation); iosMain / macosMain hold the platform-only remainder
    // (e.g. UIKit). Declaring any manual dependsOn() edge disables the template.

    // --- Apple targets (CLAUDE.md §4) ---------------------------------------
    // klibs only: no framework binaries. This library is consumed through KMP —
    // a downstream module links these klibs into ITS framework (and must
    // `export` :kotlinresult there, CLAUDE.md §7). Nothing here ships to SPM.
    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    // --- Android target (CLAUDE.md §4) --------------------------------------
    // The new com.android.kotlin.multiplatform.library plugin's android {} block.
    // arm64-v8a only: consumers' app modules pin the ABI splits; we test
    // arm64-v8a only (documented in README).
    android {
        namespace = moduleNamespace
        compileSdk =
            libs
                .findVersion("android-compile-sdk")
                .get()
                .requiredVersion
                .toInt()
        minSdk =
            libs
                .findVersion("android-min-sdk")
                .get()
                .requiredVersion
                .toInt()

        withHostTestBuilder { /* enables the androidHostTest source set */ }

        // What CONSUMERS must compile against, declared rather than inherited
        // (LESSONS B-001). Left unset, AGP stamps the AAR's `minCompileSdk` with
        // our compileSdk — 37, raised only for the Compose sample (N-004) — and
        // every consumer's `check<Variant>AarMetadata` then demands compileSdk 37.
        // The catalog's `android-min-compile-sdk` is the deliberate floor instead.
        aarMetadata {
            minCompileSdk =
                libs
                    .findVersion("android-min-compile-sdk")
                    .get()
                    .requiredVersion
                    .toInt()
        }

        // Explicit, never inherited. Left unset, AGP wires this target's
        // jvmTarget to the JDK running the build — so building on a newer JDK
        // would silently ship newer bytecode in the AAR. (This target is not a
        // KotlinJvmTarget, so a `targets.withType<KotlinJvmTarget>()` block
        // never reaches it.)
        compilerOptions {
            jvmTarget.set(jvmBytecodeTarget)
        }
    }

    // --- JVM target (desktop / server / Linux / Windows) --------------------
    // Architecture-neutral bytecode — the one target the ARM-only rule doesn't
    // touch. Ships through Maven Central, like every target.
    jvm {
        compilerOptions {
            jvmTarget.set(jvmBytecodeTarget)
        }
    }

    // --- Compiler options (CLAUDE.md §3) ------------------------------------
    compilerOptions {
        // K2 stable APIs only. Keep in lockstep with the `kotlin` pin in
        // gradle/libs.versions.toml.
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        allWarningsAsErrors.set(true)
    }

    // --- Public-API / ABI validation (CLAUDE.md §8) -------------------------
    // The Kotlin Gradle plugin's built-in ABI validation tracks the public API
    // surface across ALL targets (JVM + KLib/native) in one checked-in dump.
    // `mise run apiCheck` (wired into `check`) fails CI if the public surface
    // changes without an explicit `mise run apiDump` — so breaking changes to a
    // published library are always deliberate and reviewed.
    //
    // When the host can't compile every target (e.g. a Linux CI box can't build
    // the Apple slices), the plugin infers their ABI from the prior dump instead
    // of failing — so the checked-in dump stays complete. The Apple-target ABI is
    // verified on the macOS leg of CI, which can build those slices.
    //
    // As of Kotlin 2.4.0 the presence of this block enables validation; the old
    // `enabled.set(true)` property was removed.
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation { }
}
