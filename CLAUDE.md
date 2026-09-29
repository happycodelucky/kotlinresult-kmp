# CLAUDE.md — KotlinResult Project Guide

Kotlin Multiplatform library for iOS, macOS, Android, and JVM, consumed through
KMP (§8). This file is the
contract a contributor (human or agent) reads first. Start here, then
`gradle/libs.versions.toml`, then `.claude/lessons/LESSONS.md`.

`mise.toml` is the task contract — every build/test/lint/publish action is a
`mise run <task>`. Run `mise tasks` to see them.

## 1. Scope

KotlinResult is **`kotlin.Result` for Kotlin Multiplatform, usable from Swift** —
the shared Result type for our KMP libraries.

- **`:kotlinresult`:** `com.happycodelucky.kotlinresult.Result<T>` — a reference
  class wrapping and delegating to `kotlin.Result` (a value class, which ObjC
  export erases to `Any?`). Kotlin gets the stdlib `Result` API (same names,
  `callsInPlace` contracts) as **members**, so callers import nothing but
  `Result` — the four operators the stdlib types `<R, T : R>` (`getOrElse`,
  `getOrDefault`, `recover`, `recoverCatching`) return `T` instead, since a
  member can't bound on a supertype (LESSONS D-008); Swift/ObjC see it as
  **`KotlinResult`** plus the bundled Swift in `src/appleMain/swift/`
  (`get()`, `get<V>()`, `result(as:)`, `KotlinThrowable: Error`). Stdlib-only — no
  runtime dependencies.
- **`:kotlinresult-testing`:** `assertSuccess()` / `assertFailure<E>()`.
- **Not in scope:** error *classification* (fatal/non-fatal), catch-all helpers
  (`runCatching` twins), typed-error `Result<T, E>`, anything Result doesn't do.
  The API mirrors `kotlin.Result` exactly; divergence needs a written reason.
- **Sample apps** (`apps/`) are demos of the API, not part of the library.

## 2. Decisions (load-bearing)

Record non-obvious, load-bearing decisions in `.claude/lessons/LESSONS.md` as you
make them (terse, one line each), and reference them here.

## 3. Versions

Latest stable only — no EAP/RC/Beta on `main`. K2 only. Single source of truth:
`gradle/libs.versions.toml`. **Before bumping anything, web-search the latest
stable** (training data goes stale). The Kotlin pin is bounded above by SKIE — do
not bump Kotlin past SKIE's supported range; bump SKIE first. JVM bytecode
target 21 (the catalog's `jvm-target`, set explicitly on the android + jvm
targets — never inherited from the build JDK, see LESSONS N-003); build JDK 21
(not 25 until detekt 2.x is stable — N-001). Every other version — Kotlin, AGP,
SKIE, Gradle — is whatever the catalog says, so read it there rather than
trusting a number quoted in prose.
mise pins the non-Gradle tools (JDK, gradle, gh) and
`gradle/wrapper/gradle-wrapper.properties` pins the Gradle distribution; all
three must agree on the Kotlin/AGP/JDK/Gradle story.

## 4. Targets & module layout

- Targets: `iosArm64`, `iosSimulatorArm64`, `macosArm64`, Android (arm64-v8a),
  and **`jvm()`** (the one target the ARM-only rule doesn't touch — serves
  desktop/server/Linux/Windows). No x86, no Intel Macs, no watchOS/tvOS.
- Source sets come from Kotlin's **default hierarchy template**, applied
  implicitly (no `applyDefaultHierarchyTemplate { }` block): commonMain →
  nativeMain → `appleMain` → `iosMain` / `macosMain`, plus `androidMain` and
  `jvmMain`. Code in `appleMain` must compile on **both** iOS and macOS (use
  Foundation); iOS-only code (UIKit) goes in `iosMain`. Don't hand-roll
  source-set wiring — any manual `dependsOn()` edge disables the template.
- Module shape lives in the `kotlinresult.kmp-library` convention plugin
  (`gradle/plugins/`). The Android namespace is DERIVED from the module name
  (`kotlinresult` → `com.happycodelucky.kotlinresult`). The Apple targets build
  klibs only — no framework binaries: consumers link the klibs into their own
  frameworks (§7). Adding a module = apply `kotlinresult.kmp-library` +
  `kotlinresult.publish`.
- Keep the `expect`/`actual` seam tiny; push logic into `commonMain`.

## 5. Libraries — Kotlin-first

**`:kotlinresult` has no runtime dependencies — stdlib only.** A Result type is
depended on by every library that uses it, so it must not drag anything along;
the template's Kermit wiring was removed from the convention plugin for that
reason. (Test source sets may use test libraries.) Otherwise the usual picks: the
kotlinx.* family (coroutines, atomicfu, io),
`kotlin.time` for `Duration`/`Instant`/`Clock` (NOT `java.time` in common —
`kotlin.time.Instant`/`Clock` are stable since 2.3.x), `kotlin.uuid.Uuid` for
UUIDs (stable since 2.4.0 — no platform UUID types in common). For HTTP, prefer
Ktor/Ktorfit. Testing: `kotlin.test` + Turbine + `kotlinx-coroutines-test` +
Kotest (property tests). Library code uses **constructor injection only** — no
Koin/service locator inside `:kotlinresult`.

**Finding a library.** Before writing platform glue or pulling a JVM-only / `expect`-`actual`-heavy
dependency, look for an existing multiplatform one — in this order:

1. **Official Kotlin / JetBrains** — the kotlinx.* family (coroutines,
   serialization, datetime, io, atomicfu) and Ktor. First-party, KMP-native, and
   what the rest of this guide assumes.
2. **Google official KMP libraries** — AndroidX/Jetpack artifacts that publish
   real multiplatform targets (e.g. Room KMP, DataStore, Lifecycle, Paging,
   Collections, Annotations). Prefer these over Android-only equivalents so the
   code stays in `commonMain`.
3. **The community catalog** — [terrakok/kmp-awesome](https://github.com/terrakok/kmp-awesome),
   a curated index of KMP libraries by category. Use it to discover an existing,
   maintained multiplatform option before rolling your own.

Vet any candidate against the rules here: it must publish the targets we ship
(Apple + Android + jvm), be **stable** (no EAP/RC/Beta — §3), not pull in
CocoaPods or Compose Multiplatform (§12), and not exceed SKIE's Kotlin range.
Add it to `gradle/libs.versions.toml` only (§11), web-searching the latest stable
first. When nothing suitable exists, keep the `expect`/`actual` seam tiny (§4).

## 6. Concurrency

- `kotlinx.coroutines` only. No `GlobalScope`.
- `Flow`/`StateFlow`/`SharedFlow` over callbacks. No callback APIs in common.
- Expose state with an **explicit backing field** (stable since Kotlin 2.4), not
  a `_state`/`state` pair:
  `val state: StateFlow<S>` + `field = MutableStateFlow(initial)` on the next
  line; inside the class `state.value = …` smart-casts to the mutable type.
  Swift sees only the read-only `StateFlow` (SKIE: `SkieKotlinStateFlow`); the
  mutable field never reaches the public API or its dump (LESSONS N-005).
- Shared mutable state across suspend boundaries → `kotlinx.coroutines.sync.Mutex`.
  Non-suspending critical sections → `kotlinx.atomicfu.locks.synchronized`. Never
  `kotlin.synchronized`, `@Synchronized`, `java.util.concurrent.locks.*`,
  `volatile`.
- **Inject `Clock` + `CoroutineScope`** into time-driven code. Never read
  wall-clock in timer logic — it breaks `runTest` virtual time.
- A client that owns work should own a `SupervisorJob` *child* of the scope it's
  given; `close()` cancels only that child, never the caller's scope.

## 7. Swift interop

Swift reaches this library only through a KMP consumer's framework, built with
SKIE (`Flow`/`StateFlow` → `AsyncSequence`, sealed types → exhaustive Swift
enums). SKIE is applied here ONLY in `:kotlinresult`, to bundle its Swift into
the Apple klibs (below); nothing here builds a framework. **`@Throws` on an `expect` must be replicated verbatim on every `actual`**,
and a `@Throws` on a `suspend fun` must list `CancellationException`. Never
`kotlin.Result<T>` at the boundary — that is what this library's `Result` is for.

**Naming.** Kotlin `Result` (in-package, the simple name means ours, so the stdlib
type is always written `kotlin.Result`); ObjC/Swift `KotlinResult` via
`@ObjCName(name = "KotlinResult", swiftName = "KotlinResult")`, like `KotlinInt`.

**Swift bundling is ON for `:kotlinresult`**: SKIE copies
`src/appleMain/swift/` into each Apple klib, and the consumer's SKIE compiles the
bundled Swift of *every* linked klib into its framework (no per-dependency
opt-out — `UnpackSwiftSourcesTask`). `KotlinResult+Swift.swift` compiles only
where `KotlinResult` keeps its plain name, so **every framework that links
`:kotlinresult` must `export` it** (README; `apps/apple-consumer` here). Missing
export ⇒ `cannot find type 'KotlinResult' in scope` at link (LESSONS D-005).
Dropping SKIE here would silently drop the helpers for every consumer.

**Android consumers** compile against at least `android-min-compile-sdk` (the
AAR's `minCompileSdk`, LESSONS B-001) — not our `compileSdk`.

**No extensions on the generic class in Swift.** Swift rejects any non-`@objc`
member in an extension of a generic ObjC class ("cannot access the class's generic
parameters at runtime"). Helpers go on the non-generic `AnyKotlinResult` protocol,
whose requirements never mention `T` (`__anyValue` is the Kotlin `anyValue: Any?`,
`@ShouldRefineInSwift`, opt-in gated by `@InternalKotlinResultApi`).

**Structured concurrency.** A `Result` is a plain value: nothing is classified
"fatal", constructing one never throws, and cancellation is never captured into
one. No catch-all helpers (kotlinx.coroutines#1814). `get()` rethrows exactly what
the result holds. Document, don't add, anything that would reclassify exceptions. Apple casing everywhere in prose, file names,
and types (`iOS`, `macOS`) except JetBrains spellings (`iosArm64`, `withMacos()`).

## 8. Distribution

- **Maven Central** (`kotlinresult.publish` / vanniktech): Android AAR + jvm jar +
  KMP metadata + klibs (the Apple ones carry the bundled Swift). For Gradle/KMP
  consumers — the only channel (LESSONS D-010). `mise run publish:local`
  installs the next `X.Y.Z-SNAPSHOT` to `~/.m2` (never the released version,
  which would shadow Central's).
- **llms.txt for AI tools**: every published jar and the AAR carry `llms.txt` +
  `llms-full.txt` (the module's public API with KDoc) under
  `META-INF/<groupId>/<artifactId>/`, generated from Dokka by any publishing
  build (LESSONS D-009). `mise run llms:generate` previews them; `mise run
  llms:check` verifies a local publish. The docs site serves its own pair.
- **GitHub Releases**: each release is tagged `vX.Y.Z` with a Release holding its
  changelog notes.

**Releases are changeset-driven** (`.changeset/README.md`,
`.github/PUBLISHING.md`; LESSONS D-001, N-009). Every PR that reaches consumers adds a changeset
(`mise run changeset`: `title`, `change: major|minor|patch`, `description`, then
the full note in place of its Unfilled callout); the Changeset PR check enforces
it for any PR that changes a file in release scope — `include`/`exclude` globs in
`.changeset/config.toml` (label `no-changeset` to opt out). A changeset's `change` is the source of
truth for the version — the author's call, which neither the PR nor tooling
overrides. Merges to `main` keep one rolling **Release vX.Y.Z** PR up to date — it
bumps `version=` in `gradle.properties` (the single source of the version),
rewrites every `x-release-version`-marked copy, and writes the changelog.
Merging it runs `.github/workflows/release.yml`, which publishes exactly that
version and then deploys the docs site. While 0.x a `major` change bumps the
minor; `version: X.Y.Z` in a changeset pins the version (the way to 1.0.0; it
may even sit below what the levels imply, as long as it moves forward).
Never edit `version=` by hand. Pre-releases and retries: dispatch `release.yml`
with a `version` (e.g. `0.4.0-rc.1`), or `mise run publish:maven` by hand.

**Public-API stability.** The committed dumps under `<module>/api/` are the
reference for the public surface, across every target. `mise run check` (and CI)
runs `api:check` and fails on any unintended change — so a breaking change to a
published library is always deliberate. After an *intentional* public-API change,
run `mise run api:dump` and commit the `api/` diff alongside the code; review it
like any other change. This is the single most important guard for a library:
it's what stops an accidental rename or removed function from breaking consumers.

## 9. Platform notes

Fill in as your library's platform needs become concrete. Common gotchas:
- **iOS:** capabilities like multicast networking need entitlements (Apple gates
  some behind a request form); plain-HTTP LAN access needs an ATS exception in
  the *host app's* Info.plist. The library can't set these.
- **Android:** networking may need a `WifiManager.MulticastLock` and permissions;
  the library manifest contributes permissions to consumers via Manifest Merger.
- **macOS:** a sandboxed app needs `network.client` (outbound) and/or
  `network.server` (bind/listen) entitlements; they apply only to a *signed* app.
- **JVM:** the architecture-neutral target; serves desktop/server.

## 10. Testing

Every `Result` operation has a `commonTest` case checking it against the
`kotlin.Result` operation it mirrors (`ResultTest`). The Swift half isn't covered
by Gradle tests: `mise run test:swift` links `apps/apple-consumer`'s framework
(which exports `:kotlinresult`, so SKIE compiles the bundled Swift) and runs
`swift/main.swift` against it (construction, `get()`, `result(as:)`, type
mismatch) — run it after changing the Swift or the exported surface; CI's Apple
leg does. The done gate is the full
`:kotlinresult:check :kotlinresult-testing:check` (`mise run check`) — which compiles *test*
sources for every target and runs detekt. A JVM-only run hides native-test-compile
and detekt failures.

## 11. Task workflow (mise)

1. Read this file, then `gradle/libs.versions.toml`, then `.claude/lessons/LESSONS.md`.
2. Run `mise tasks` to see every command you can execute — it's the task
   contract. Drive build/test/lint/publish through `mise run <task>`, not raw
   `./gradlew` or ad-hoc scripts, so humans and agents share one surface.
3. Adding a dependency? First hunt for an existing multiplatform one (§5:
   official Kotlin/JetBrains → Google official KMP → terrakok/kmp-awesome).
   Then web-search the latest stable; add to the catalog only.
   `mise run dependencies:outdated` lists candidates; `dependencies:update`
   rewrites the catalog (review the diff); `dependencies:analyze` flags unused or
   misdeclared deps (api vs implementation).
4. Platform-specific? Keep the `expect`/`actual` seam tiny; push logic to common.
5. Public API crossing to Swift? Apply §7 at design time.
6. Changed the public API on purpose? `mise run api:dump` and commit the `api/`
   diff (§8) — otherwise `check` fails on the surface change.
7. Add a changeset (`mise run changeset`, §8) when the change reaches
   consumers, and replace its Unfilled callout with the release note. Its
   `change` level is the version decision; the PR's "Type of change" only
   restates it. The usual reading — removed/renamed public API is `major` (even
   while 0.x), new API `minor`, a fix `patch` — is a default, not a rule: a
   different level is the author's call (say why in the body). Docs/CI/test-only
   PRs are out of release scope and need none (`mise run changeset:scope`);
   label an in-scope PR that still reaches no consumer `no-changeset`.
8. Done when `mise run check` passes AND `:kotlinresult:compileKotlinMacosArm64` /
   `compileKotlinIosSimulatorArm64` / `compileAndroidMain` build clean (common-code
   bugs often only surface on Native — the JVM compile is not a sufficient gate).
   `check` never builds the sample apps — `mise run build:samples` does (CI's
   fast leg runs it); it's what catches AndroidX compileSdk floors (LESSONS N-004).
   Touched the Swift or the public API? Also `mise run test:swift` (§10).
   `check` also runs the API/ABI check (§8). If a build feels slow, `mise run
   build:profile` writes a local timing report; `build/reports/problems/` lists
   deprecations and configuration-cache problems.
9. Learned something non-obvious? Add it to `.claude/lessons/LESSONS.md` (terse).
10. Opening a PR or filing an issue? GitHub applies the templates only in its web
    UI — `gh … create --body` skips them — so build the body from them yourself
    and pass it with `--body-file` (LESSONS N-007):
    - **PR:** start from `.github/PULL_REQUEST_TEMPLATE.md`. Follow each
      `<!-- AI: … -->` comment, replace every `Unfilled` callout (none may
      remain), prune each choice list to the lines that apply, and tick a
      done-gate box only for what you actually ran or checked. Keep "AI-authored"
      under AI assistance, name the tool + model, and open with `--draft` — a
      human marking it ready is the review sign-off (LESSONS N-008).
    - **Issue:** read the matching form in `.github/ISSUE_TEMPLATE/`. Write each
      field's `label` as a `### ` heading in form order, with `_No response_`
      under a skipped optional field — the exact shape the web form produces.
      Use its `title:` prefix and `labels:` (drop any the repo lacks — `gh`
      rejects them). Tick a required checkbox only if it's true (e.g. search
      with `gh issue list --search` first).

## 12. Hard rules

No Compose Multiplatform in the library. No CocoaPods. No x86/Intel Macs/watchOS/
tvOS. No `GlobalScope`, no `!!` in production, no `java.time` in common, no
`kotlin.synchronized`/`@Synchronized`/`volatile`. No callback-based public APIs.
No `kotlin.Result<T>` at the Swift boundary. No EAP/RC/Beta on `main`.
