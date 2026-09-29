/*
 * kotlinresult — :kotlinresult-testing module (renamed to :<name>-testing by init).
 *
 * Public, scriptable test fakes and helpers for consumers of `:kotlinresult`. Same module
 * shape as `:kotlinresult` via the `kotlinresult.kmp-library` convention plugin;
 * published in lockstep (same group / version / pipeline) via
 * `kotlinresult.publish`. Consumers wire it on `testImplementation` (or KMP
 * `commonTest` deps); the production `:kotlinresult` artifact does not depend on it.
 *
 * Like `:kotlinresult`, klibs only: the Apple targets exist so KMP consumers can
 * resolve this module from their Apple test source sets.
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

mavenPublishing {
    pom {
        name.set("KotlinResult Testing")
        description.set(
            "Test assertions for the kotlinresult library: assertSuccess() / " +
                "assertFailure<E>() for code that returns a KotlinResult Result.",
        )
    }
}
