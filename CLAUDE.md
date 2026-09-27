# CLAUDE.md — __DISPLAY_NAME__ Project Guide

Kotlin Multiplatform library for iOS, macOS, Android, and JVM. This file is the
contract a contributor (human or agent) reads first. Start here, then
`gradle/libs.versions.toml`, then `.claude/lessons/LESSONS.md`.

`mise.toml` is the task contract — every build/test/lint/publish action is a
`mise run <task>`. Run `mise tasks` to see them.

## 1. Scope

> **TODO (fill this in):** describe what __DISPLAY_NAME__ does — the problem it
> solves, what's *in* the shared library and what's deliberately *not*.

- **Shared (in `:src`):** the library's business logic. Headless — no UI
  dependencies. Each platform app consumes the library's `StateFlow`/`SharedFlow`
  or suspend API.
- **Not shared:** UI. Platform apps under `apps/` have their own native UI.
- **Modules:** `:src` (the library) and `:src-testing` (public test fakes +
  helpers for consumers, e.g. a `FakeX` and a `withX { … }` helper).

## 2. Decisions (load-bearing)

Record non-obvious, load-bearing decisions in `.claude/lessons/LESSONS.md` as you
make them (terse, one line each), and reference them here.

## 3. Versions

Latest stable only — no EAP/RC/Beta on `main`. K2 only. Single source of truth:
`gradle/libs.versions.toml`. **Before bumping anything, web-search the latest
stable** (training data goes stale). The Kotlin pin is bounded above by SKIE — do
not bump Kotlin past SKIE's supported range; bump SKIE first. JVM bytecode
target 21 (the catalog's `jvm-target`, set explicitly on the android + jvm
targets — never inherited from the build JDK, see LESSONS N-005); build JDK 21
(not 25 until detekt 2.x is stable — N-002). Every other version — Kotlin, AGP,
SKIE, Gradle — is whatever the catalog says, so read it there rather than
trusting a number quoted in prose.
mise pins the non-Gradle tools (JDK, gradle, xcodegen, gh) and
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
- Module shape lives in the `template.kmp-library` convention plugin
  (`gradle/plugins/`). Framework base name and namespace are DERIVED from the
  module name (`src` → framework `SrcKit`, namespace `com.happycodelucky.src`).
  The framework / Swift module is always `<Name>Kit`, so it never shares a name
  with a public type (SKIE would rename the type in Swift — LESSONS D-002).
  Adding a module = apply `template.kmp-library` + `template.publish`.
- Keep the `expect`/`actual` seam tiny; push logic into `commonMain`.

## 5. Libraries — Kotlin-first

kotlinx.* family (coroutines, atomicfu, io), **Kermit for logging** (wired into
every module by the convention plugin — `Logger` is available in `commonMain`),
`kotlin.time` for `Duration`/`Instant`/`Clock` (NOT `java.time` in common —
`kotlin.time.Instant`/`Clock` are stable since 2.3.x), `kotlin.uuid.Uuid` for
UUIDs (stable since 2.4.0 — no platform UUID types in common). For HTTP, prefer
Ktor/Ktorfit. Testing: `kotlin.test` + Turbine + `kotlinx-coroutines-test` +
Kotest (property tests). Library code uses **constructor injection only** — no
Koin/service locator inside `:src`.

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
  mutable field never reaches the public API or its dump (LESSONS N-007).
- Shared mutable state across suspend boundaries → `kotlinx.coroutines.sync.Mutex`.
  Non-suspending critical sections → `kotlinx.atomicfu.locks.synchronized`. Never
  `kotlin.synchronized`, `@Synchronized`, `java.util.concurrent.locks.*`,
  `volatile`.
- **Inject `Clock` + `CoroutineScope`** into time-driven code. Never read
  wall-clock in timer logic — it breaks `runTest` virtual time.
- A client that owns work should own a `SupervisorJob` *child* of the scope it's
  given; `close()` cancels only that child, never the caller's scope.

## 7. Swift interop

SKIE mandatory (convention plugin configures it; `produceDistributableFramework()`
in `:src`). `Flow`/`StateFlow` → `AsyncSequence`. Sealed types → exhaustive Swift
enums. **`@Throws` on an `expect` must be replicated verbatim on every `actual`**,
and a `@Throws` on a `suspend fun` must list `CancellationException`. Never
`kotlin.Result<T>` at the boundary. Apple casing everywhere in prose, file names,
and types (`iOS`, `macOS`) except JetBrains spellings (`iosArm64`, `withMacos()`).

## 8. Distribution

Two channels, non-overlapping:
- **Maven Central** (`template.publish` / vanniktech): Android AAR + jvm jar +
  KMP metadata + klibs. For Gradle/KMP consumers. `mise run publish:local`
  installs the next `X.Y.Z-SNAPSHOT` to `~/.m2` (never the released version,
  which would shadow Central's).
- **GitHub Releases** (KMMBridge in `src/build.gradle.kts`): the SKIE-enhanced
  `__FRAMEWORK__.xcframework` for SPM consumers. Don't redeclare `XCFramework("__FRAMEWORK__")` —
  KMMBridge auto-creates it. The released `Package.swift` lives only on each
  `vX.Y.Z` tag; `main` keeps the local-dev form.

**Releases are changeset-driven** (`.changeset/README.md`,
`.github/PUBLISHING.md`; LESSONS D-001, N-013, N-014). Every PR that reaches consumers adds a changeset
(`mise run changeset`: `title`, `change: major|minor|patch`, `description`, then
the full note in place of its Unfilled callout); the Changeset PR check enforces
it (label `no-changeset` to opt out). A changeset's `change` is the source of
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

All shared logic gets `commonTest` coverage with `runTest` virtual time (never
`Thread.sleep`). Turbine for Flow assertions; Kotest for property tests (use
multiplatform arbs only — `Arb.stringPattern` is JVM-only and breaks the native
test compile). Inject a test `Clock`/`TimeSource` reading the test scheduler so
`now` and `delay` stay in lockstep. The done gate is the full
`:src:check :src-testing:check` (`mise run check`) — which compiles *test*
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
   PRs get the `no-changeset` label.
8. Done when `mise run check` passes AND `:src:compileKotlinMacosArm64` /
   `compileKotlinIosSimulatorArm64` / `compileAndroidMain` build clean (common-code
   bugs often only surface on Native — the JVM compile is not a sufficient gate).
   `check` never builds the sample apps — `mise run build:samples` does (CI's
   fast leg runs it); it's what catches AndroidX compileSdk floors (LESSONS N-006).
   `check` also runs the API/ABI check (§8). If a build feels slow, `mise run
   build:profile` writes a local timing report; `build/reports/problems/` lists
   deprecations and configuration-cache problems.
9. Learned something non-obvious? Add it to `.claude/lessons/LESSONS.md` (terse).
10. Opening a PR or filing an issue? GitHub applies the templates only in its web
    UI — `gh … create --body` skips them — so build the body from them yourself
    and pass it with `--body-file` (LESSONS N-011):
    - **PR:** start from `.github/PULL_REQUEST_TEMPLATE.md`. Follow each
      `<!-- AI: … -->` comment, replace every `Unfilled` callout (none may
      remain), prune each choice list to the lines that apply, and tick a
      done-gate box only for what you actually ran or checked. Keep "AI-authored"
      under AI assistance, name the tool + model, and open with `--draft` — a
      human marking it ready is the review sign-off (LESSONS N-012).
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
