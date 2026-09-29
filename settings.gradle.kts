@file:Suppress("UnstableApiUsage")

pluginManagement {
    // Convention plugins (`kotlinresult.kmp-library`, `kotlinresult.publish`)
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

rootProject.name = "kotlinresult"

// --- Published library modules ------------------------------------------------
include(":kotlinresult")

// :kotlinresult-testing — public, scriptable test fakes + helpers for consumers of :kotlinresult.
// Headless KMP module; same targets as :kotlinresult; published as a sibling Maven
// Central artifact. Consumers wire it on `testImplementation` (or KMP
// `commonTest` deps).
include(":kotlinresult-testing")

// --- Sample apps (CLAUDE.md §9) -----------------------------------------------
// Every sample is a Gradle subproject depending on :kotlinresult directly:
// Android (Compose) under /apps/android, a JVM CLI under /apps/jvm-cli, and
// /apps/apple-consumer — a stand-in KMP library that exports :kotlinresult into
// its Apple framework, the only way Swift reaches it (CLAUDE.md §7, §8).
include(":androidApp")
project(":androidApp").projectDir = file("apps/android")

include(":jvm-cli")
project(":jvm-cli").projectDir = file("apps/jvm-cli")
include(":apple-consumer")
project(":apple-consumer").projectDir = file("apps/apple-consumer")
