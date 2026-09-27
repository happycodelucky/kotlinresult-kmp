/*
 * __PROJECT_NAME__ — JVM CLI sample.
 *
 * A plain JVM application that exercises the real library against the desktop
 * JVM target. Useful as a usage example and as a manual end-to-end harness for
 * behavior that virtual-time unit tests can't cover (real I/O, real timing).
 *
 * Not a published artifact — excluded from the library lint/check gate. Depends
 * on :src via the JVM slice.
 */
plugins {
    // Version omitted: the Kotlin plugin is already on the build classpath from
    // the root build's pluginManagement, so the subproject applies it
    // version-less (re-requesting a version is a hard error in a composite build).
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":src"))
    implementation(libs.kotlinx.coroutines.core)
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.happycodelucky.src.cli.MainKt")
}
