/*
 * __PROJECT_NAME__ — :src-testing module (renamed to :<name>-testing by init).
 *
 * Public, scriptable test fakes and helpers for consumers of `:src`. Same module
 * shape as `:src` via the `__PROJECT_NAME__.kmp-library` convention plugin;
 * published in lockstep (same group / version / pipeline) via
 * `__PROJECT_NAME__.publish`. Consumers wire it on `testImplementation` (or KMP
 * `commonTest` deps); the production `:src` artifact does not depend on it.
 *
 * No XCFramework and no SKIE `produceDistributableFramework()`: test code is
 * consumed as KMP klibs from Maven Central, not via SPM. The Apple targets exist
 * so KMP consumers can resolve this module from their Apple test source sets, but
 * we don't ship a binary framework for it.
 */

plugins {
    id("template.kmp-library")
    id("template.publish")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api` so consumers writing `testImplementation(<name>-testing)` get
            // the public `:src` types transitively — they will assert against them.
            api(project(":src"))

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.atomicfu)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }

        getByName("androidHostTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

mavenPublishing {
    pom {
        name.set("__DISPLAY_NAME__ Testing")
        description.set(
            "Test fakes and helpers for the __PROJECT_NAME__ library: scriptable " +
                "doubles for consumers writing tests against the public API.",
        )
    }
}
