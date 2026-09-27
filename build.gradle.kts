/*
 * __PROJECT_NAME__ — root build script.
 *
 * Plugins are declared here with `apply false`; they're applied in :src and
 * :src-testing (mostly via the `__PROJECT_NAME__.kmp-library` convention
 * plugin). This keeps `gradle/libs.versions.toml` as the single source of truth
 * for versions (CLAUDE.md §3).
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import nl.littlerobots.vcu.plugin.versionSelector

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.kmmbridge.github) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false

    // Dokka v2: Kotlin API doc generator. Produces HTML for the public API of
    // every source set. The HTML is copied into docs/api/ for mkdocs to bundle.
    alias(libs.plugins.dokka)

    // Dependency-update tooling (mise dependencies:outdated / dependencies:update).
    // ben-manes reports updates; version-catalog-update rewrites libs.versions.toml.
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.version.catalog.update)

    // Build-health tooling. dependency-analysis adds the root `buildHealth` task
    // (mise dependencies:analyze) — unused/misused/transitive dependency advice.
    alias(libs.plugins.dependency.analysis)
}

allprojects {
    group = "com.happycodelucky.__PROJECT_NAME__"
    // `version` lives in gradle.properties: the last version released from
    // main, bumped only by the release PR (scripts/changeset.py). CI stamps
    // non-release builds with `-Pversion=…-ci.N`; a pre-release passes its own
    // `-Pversion`. Nothing ever writes an override back.
    version = providers.gradleProperty("version").get()
}

// dependency-analysis (`mise run dependencies:analyze` → buildHealth). The
// project plugin is applied per published KMP module in `subprojects {}` below.
//
// KMP analysis is real but still noisy on modules with SHARED (hierarchical)
// source sets — a dependency declared once in commonMain is *visible* in the
// jvm/android/apple leaf sets, and the plugin's per-leaf analysis emits advice
// that contradicts the common-set declaration. The tuning below was derived by
// RUNNING buildHealth on this repo at DAGP 3.18.0, not copied forward:
//
//   * usedTransitiveDependencies → ignore. Structurally wrong on hierarchical
//     KMP. It wants curated single deps re-declared in every leaf set —
//     kermit's internal `kermit-core` in androidMain/jvmMain, `kotlin-test-junit`
//     in every JVM-ish test set, `project(":src")` in jvmTest. Splitting curated
//     deps into their transitive internals would churn the catalog (CLAUDE.md §5).
//   * incorrectConfiguration → warn. RE-ENABLED. The api-vs-implementation
//     over-suggestion on kotlinx.coroutines.core (DAGP issue #1700) was fixed in
//     3.16.1 ("don't advise moving a dependency from commonMainApi to
//     jvmMainApi"); this category now reports nothing on this repo, so silencing
//     it would only hide future real advice.
//   * runtimeOnly → warn, with ONE exclusion rather than a blanket ignore.
//     coroutines-android is compile-invisible because its MainDispatcherFactory
//     loads via ServiceLoader — kept as the conventional `implementation`.
//     Narrowing to that single coordinate keeps the category live for new deps.
//   * unusedDependencies → warn, excluding only the deps whose purpose DAGP
//     structurally cannot see. Those are the wiring, not ordinary libraries:
//       - `co.touchlab:kermit` — the convention plugin injects it into every
//         module on purpose, so `Logger` is available in commonMain whether or
//         not that module logs today (CLAUDE.md §5).
//       - `:src` — `:src-testing` declares it as `api` to re-export the public
//         types transitively to consumers writing `testImplementation(…-testing)`
//         (see src-testing/build.gradle.kts). A deliberate re-export reads as
//         "unused" to a compile-usage analysis, permanently.
//       - `:src-testing` — the fakes are consumed from `:src`'s test source sets;
//         DAGP under-detects cross-source-set test usage on KMP (issue #1345).
//
// NOTE (template): turbine, kotest-assertions-core, kotest-property and atomicfu
// WILL be reported as unused until you replace the placeholder `Greeter` with
// real code. That advice is CORRECT — they're scaffolding for the library you
// haven't written yet — and it clears itself as you start using them.
// Deliberately NOT excluded: a permanent exclusion would also hide the case
// where you genuinely never use them. These are warnings; buildHealth exits 0,
// so CI stays green either way.
dependencyAnalysis {
    issues {
        all {
            onUsedTransitiveDependencies {
                severity("ignore")
            }
            onIncorrectConfiguration {
                severity("warn")
            }
            // compile→runtimeOnly downgrades (a SEPARATE handler from
            // onIncorrectConfiguration — verified against DAGP 3.18.0's DSL).
            onRuntimeOnly {
                severity("warn")
                exclude("org.jetbrains.kotlinx:kotlinx-coroutines-android")
            }
            onUnusedDependencies {
                severity("warn")
                exclude(
                    "co.touchlab:kermit",
                    ":src",
                    ":src-testing",
                )
            }
        }
    }
}

subprojects {
    // ktlint + detekt wire onto the KMP plugin — i.e. onto the published
    // library modules only (CLAUDE.md §3). The sample apps (`:androidApp`,
    // `:jvm-cli`) are demo scaffolding, not shipped code, and are intentionally
    // excluded from Kotlin lint and from CI's check task.
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        apply(plugin = "io.gitlab.arturbosch.detekt")
        // dependency-analysis's project plugin does NOT auto-apply from the root
        // `plugins {}` block (only the `com.autonomousapps.build-health` settings
        // plugin fans out) — without a per-module apply, `buildHealth` runs but
        // analyzes ZERO projects ("No project health reports found"). Applying it
        // to the published KMP modules only (alongside ktlint/detekt) makes
        // buildHealth actually inspect `:src` / `:src-testing`; the advice is then
        // tuned in the root `dependencyAnalysis { }` block below.
        //
        // This used to be gated behind `-PenableDependencyAnalysis=true`: DAGP
        // 3.16.0 bundled a kotlin-metadata-jvm that could not read Kotlin 2.4.0
        // bytecode metadata (format 2.4.0 > its max 2.3.0), so applying it made
        // `explodeJar*` — and therefore `buildHealth` — HARD-FAIL. Fixed in
        // 3.18.0, which isolates kotlin-metadata-jvm into workers
        // (autonomousapps/dependency-analysis-gradle-plugin#1724, closed
        // 2026-06-04). The gate is gone and CI runs `dependencies:analyze` again.
        apply(plugin = "com.autonomousapps.dependency-analysis")
    }

    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set(libs.versions.ktlint.cli.get())
            android.set(false)
            outputToConsole.set(true)
            ignoreFailures.set(false)
            filter {
                exclude { element -> element.file.path.contains("/build/generated/") }
                exclude("**/build/**")
                exclude("**/generated/**")
            }
        }

        tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask>().configureEach {
            exclude { element -> element.file.path.contains("/build/generated/") }
        }
    }

    plugins.withId("io.gitlab.arturbosch.detekt") {
        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            // Project overrides layered on the defaults live in config/detekt.
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            // detekt's default source resolution only knows JVM layouts
            // (src/main/kotlin); point it at the module root so every KMP source
            // set (commonMain, appleMain, jvmMain, androidHostTest, …) is
            // scanned. The task filters to *.kt, and build/ output is excluded.
            source.setFrom(files("src"))
        }
    }
}

// Apply Dokka to the published modules and aggregate into docs/api/.
dokka {
    moduleName.set("__DISPLAY_NAME__")
}

dependencies {
    // Aggregate Dokka HTML from the published modules into the root build
    // (Dokka v2 pattern). `:src-testing` is a public-API module too — consumers
    // writing tests want its fakes documented next to the main library.
    dokka(project(":src"))
    dokka(project(":src-testing"))
}

/**
 * Copies Dokka v2 HTML output into docs/api/, where mkdocs picks it up.
 *
 * The aggregated HTML lives at build/dokka/html after
 * dokkaGeneratePublicationHtml. mkdocs looks at docs/api/ when it builds the
 * site; CI runs Dokka before mkdocs.
 */
tasks.register<Copy>("copyDokkaToDocs") {
    group = "documentation"
    description = "Copies aggregated Dokka HTML into docs/api/ for mkdocs."

    dependsOn("dokkaGeneratePublicationHtml")
    from(layout.buildDirectory.dir("dokka/html"))
    into(layout.projectDirectory.dir("docs/api"))
}

// Stable-only dependency updates (CLAUDE.md §3: no EAP/RC/Beta on main).
//
// `-Drevision=release` only chooses which Maven metadata channel ben-manes
// reads — it does NOT reject versions whose string is a pre-release, so without
// this rule `dependencyUpdates` happily suggests 1.5.0-alpha22 over 1.4.0. The
// `stableVersion` predicate below filters every candidate whose version carries
// a pre-release qualifier, and it is handed to BOTH plugins:
// `rejectVersionIf` on ben-manes (`dependencies:outdated`, the report) and
// `versionSelector` on version-catalog-update (`dependencies:update`, the
// rewrite). VCU has resolved versions itself since 1.0 — it no longer reads the
// ben-manes report — and its built-in default selector uses a DIFFERENT
// stability rule, so it must be given this one explicitly to stay in lockstep.
//
// A version is considered STABLE only if it is digits-and-dots and nothing else.
// Accepts: 1.2.3, 2026.06.01, 1.2.3.4. Rejects everything carrying a qualifier —
// -alpha/-beta/-rc/-eap/-m1/-snapshot/-dev/-preview, any case, separator or not —
// because a qualifier necessarily introduces a non-digit, non-dot character.
//
// That whitelist IS the whole stability test, so there is deliberately no second
// "does it look like a pre-release?" regex: no string can satisfy this pattern
// and still contain an alphabetic qualifier, so such a check would be
// unreachable. If you ever loosen this pattern (e.g. to allow a `-jre`-style
// classifier), you must add the qualifier check back — it is load-bearing only
// in that world.
val stableVersion = "^[0-9][0-9.]*$".toRegex()

tasks.withType<DependencyUpdatesTask>().configureEach {
    // Read the stable release channel, not integration/milestone metadata.
    revision = "release"
    rejectVersionIf {
        // The current version is never rejected here — ben-manes only feeds
        // candidate upgrades through this predicate.
        !stableVersion.matches(candidate.version)
    }
}

versionCatalogUpdate {
    versionSelector { stableVersion.matches(it.candidate.version) }
    // Keep the catalog's hand-grouped sections (Toolchain, kotlinx, Testing, …)
    // instead of alphabetizing them.
    sortByKey.set(false)
    keep {
        // Keys no library/plugin references: android-compile-sdk, android-min-sdk
        // and jvm-target (read via the string-based findVersion("…") API in the
        // convention plugin, invisible to VCU's usage scan), and the Apple
        // deployment targets (documentation for the floors spelled out in
        // src/build.gradle.kts and Package.swift). Without this, VCU prunes them.
        keepUnusedVersions.set(true)
    }
    pin {
        // Kotlin is bounded above by SKIE (CLAUDE.md §3): a Kotlin bump is a
        // manual, SKIE-paired change — the same policy renovate.json5 encodes.
        // Pinning the `kotlin` ref also holds the compose-compiler plugin, which
        // versions in lockstep with it. To bump Kotlin, edit the catalog by hand
        // once SKIE's changelog lists support; this task only reports it.
        versions.add("kotlin")
    }
}
