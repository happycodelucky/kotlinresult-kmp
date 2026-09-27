@file:Suppress("UnstableApiUsage")

pluginManagement {
    // Convention plugins (`__PROJECT_NAME__.kmp-library`, `__PROJECT_NAME__.publish`)
    // live in gradle/plugins; versions still come from gradle/libs.versions.toml,
    // which gradle/plugins shares.
    includeBuild("gradle/plugins")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Project-level repos win; subprojects must not redeclare.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        // mavenLocal lets a locally-published snapshot (mise run publish:local)
        // resolve ahead of Maven Central during cross-repo development. Listed
        // last so Central wins for everything that isn't an explicit local install.
        mavenLocal()
    }
}

rootProject.name = "__PROJECT_NAME__"

// --- Published library modules ------------------------------------------------
include(":src")

// :src-testing — public, scriptable test fakes + helpers for consumers of :src.
// Headless KMP module; same targets as :src; published as a sibling Maven
// Central artifact. Consumers wire it on `testImplementation` (or KMP
// `commonTest` deps).
include(":src-testing")

// --- Sample apps (CLAUDE.md §9) -----------------------------------------------
// The Android sample is a normal Gradle subproject because Compose + AGP play
// best inside the same Gradle build that produces the AAR. The iOS and macOS
// samples are standalone Xcode projects under /apps/ios and /apps/macos; they
// consume the :src module via SPM, NOT Gradle, and so are deliberately not
// included here. A JVM CLI sample lives under /apps/jvm-cli.
include(":androidApp")
project(":androidApp").projectDir = file("apps/android")

include(":jvm-cli")
project(":jvm-cli").projectDir = file("apps/jvm-cli")
