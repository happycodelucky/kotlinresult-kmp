/*
 * Convention plugin: the shared module shape for this library's published KMP
 * modules (`:kotlinresult`, `:kotlinresult-testing`).
 *
 * Owns everything the modules would otherwise duplicate (CLAUDE.md §4, §5): the
 * target matrix, the apple intermediate source set, the Android library block,
 * the jvm() target, compiler options, JVM target wiring, and the SKIE settings
 * that must match across modules. Per-module
 * identity (framework base name, bundle id, Android namespace) is DERIVED from
 * the project name, so adding a module means applying this plugin and nothing
 * else:
 *
 *   src          → framework "SrcKit",        namespace com.happycodelucky.kotlinresult
 *   src-testing  → framework "SrcTestingKit", namespace com.happycodelucky.kotlinresult.testing
 *
 * `mise run init <name>` renames the module directories (src → <name>,
 * src-testing → <name>-testing); the derivations below then produce the right
 * framework name and namespace with zero token replacement. The group prefix
 * `com.happycodelucky` is the at-rest default — init.sh rewrites it only when
 * `--group` differs.
 *
 * Module build scripts keep only what genuinely differs: dependencies, the
 * KMMBridge SPM distribution config (`:kotlinresult` only), and POM name/description.
 */

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("co.touchlab.skie")
    id("org.jetbrains.dokka")
}

// Typed `libs` accessors aren't generated inside precompiled script plugins;
// the named-lookup API reads the same catalog the main build uses.
val libs = the<VersionCatalogsExtension>().named("libs")

// src → "SrcKit"; src-testing → "SrcTestingKit". The "Kit" suffix keeps the Swift
// module name distinct from the library's public types: a module and a type with
// the same name make SKIE rename the type in Swift (`Wake` → `Wake_`) and let the
// bare type shadow the module qualifier in SKIE's generated code (LESSONS D-002).
val frameworkBaseName = name.split("-").joinToString("") { part -> part.replaceFirstChar(Char::uppercase) } + "Kit"

// src → com.happycodelucky.kotlinresult; src-testing → ….src.testing.
// Doubles as the framework bundle id, pinned so SKIE doesn't fall back to the
// framework name. The "com.happycodelucky" prefix is the group default; init.sh
// rewrites it when `--group` differs.
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
    // Static framework binaries with a stable bundle id. In `:kotlinresult`, KMMBridge
    // aggregates these into `SrcKit.xcframework` at config time (no explicit
    // XCFramework declaration — see kotlinresult/build.gradle.kts).
    listOf(iosArm64(), iosSimulatorArm64(), macosArm64()).forEach { target ->
        target.binaries.framework {
            baseName = frameworkBaseName
            isStatic = true
            binaryOption("bundleId", moduleNamespace)
        }
    }

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
    // touch. No SKIE, no KMMBridge — the JVM ships through Maven Central only,
    // like Android.
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

skie {
    // SKIE handles the Kotlin → Swift bridge enhancements (CLAUDE.md §7):
    // exhaustive sealed switching, suspend → async/await, Flow → AsyncSequence,
    // default-arg overloads. All feature defaults stay on; tighten only when
    // something bites.
    analytics {
        // Disable opt-in analytics; revisit if useful.
        disableUpload.set(true)
    }
    // Swift bundling OFF by default (LESSONS D-005). Off, a module's
    // `src/<sourceSet>/swift/` still compiles into ITS OWN framework (the
    // XCFramework SPM consumers get), but isn't copied into the klib — so KMP
    // consumers who link the klib into their own framework don't get it.
    //
    // On (`skie { swiftBundling { enabled.set(true) } }` in a module's build
    // script — `:kotlinresult` does), the Swift ships in the klib and SKIE
    // compiles it into EVERY downstream framework that links the module — with
    // no per-dependency opt-out. That Swift names our types, which only keep
    // their plain Swift names where the module is exported, so every downstream
    // framework must then `export(...)` this module or its link fails ("cannot
    // find type … in scope"). Document that export requirement in the README.
    swiftBundling {
        enabled.set(false)
    }
}
