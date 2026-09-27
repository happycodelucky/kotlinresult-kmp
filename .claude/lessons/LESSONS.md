# LESSONS — KotlinResult

Terse log of non-obvious things learned while building this library, one line
each. IDs are stable (CLAUDE.md, code comments and PRs cite them), so append new
ones and don't renumber. Anything a comment at its point of use already explains
belongs in that comment, not here.

- **D-NNN** — Decisions (load-bearing architecture choices).
- **B-NNN** — Bugs / gotchas (the thing that bit, and the fix).
- **N-NNN** — Notes (build-system / toolchain quirks).

## Decisions

- **D-001** — Releases are changeset-driven (`.changeset/`, `scripts/changeset.py`). `version=` in gradle.properties is the single source of the version, bumped only by the rolling release PR; release.yml publishes when a push to main CHANGES it. A changeset's `change` is the author's call and the source of truth (nothing cross-checks it); while 0.x a `major` bumps the minor, and only a `version:` pin leaves 0.x. Stable versions never come from a manual dispatch (pre-releases / retries only), so main, the changelog and Maven Central can't drift.
- **D-002** — The Apple framework / Swift module is `<PascalName>Kit` (`KotlinresultKit`), derived identically in the convention plugin and `kotlinresult/build.gradle.kts` (KMMBridge). A module named like one of its public types makes SKIE rename the type in Swift (`KotlinResult_`) and lets the bare type shadow the module qualifier in SKIE's generated Swift. Renaming a shipped framework breaks every Swift consumer's `import`.
- **D-003** — Line length is 140, set once as `max_line_length` in the root `.editorconfig`: editors show it, ktlint enforces it and `mise run format` wraps to it. detekt's `MaxLineLength` is off (a second copy of the number could drift). ktlint ignores `max_line_length` in EVERY rule when its `max-line-length` rule is disabled, so that rule stays on. ktlint_official's parameter-count forced-multiline class/function signatures are `unset` (it otherwise wraps a 1-param constructor).
- **D-004** — `Result` is a delegating wrapper, not an `expect`/`actual`: `actual typealias Result = kotlin.Result` fails (declaration-site variance), and `kotlin.Result` exports to ObjC as `Any?` (value class, KT-32352). The primary constructor takes an unused `Boolean` so it doesn't clash with `constructor(value: T)` on the JVM (both erase to `Object`).
- **D-005** — SKIE unpacks the bundled Swift of EVERY linked klib into every framework it builds — no per-dependency opt-out. Bundled Swift that names a type only compiles where that type keeps its plain Swift name, i.e. where the module is `export`ed; elsewhere the link fails ("cannot find type … in scope"). `:kotlinresult` bundles its Swift, so every framework linking it must `export` it (CLAUDE.md §7, README).
- **D-006** — Swift can't add non-`@objc` members to an extension of a Kotlin generic class, and rejects protocols whose associated type binds to its `T`. The helpers live on the non-generic `AnyKotlinResult` protocol over the type-erased `__anyValue`; callers name the value type and `as? V` bridges `NSString`/`KotlinInt` → `String`/`Int`.
- **D-007** — No fatal/non-fatal classification and no catch-all (`runCatching`-twin) helpers (kotlinx.coroutines#1814). Cancellation propagates; SKIE maps Swift `Task.cancel()` ↔ coroutine cancellation ↔ `CancellationError`. An escaping `withTimeout` reaches Swift as `CancellationError` (SKIE #140) — use `withTimeoutOrNull`.
- **D-008** — Operators are MEMBERS of `Result`, not extensions: package `kotlin` is default-imported, ours isn't, so extensions would cost callers an import each. Members keep `callsInPlace` contracts. The four stdlib operators typed `<R, T : R>` (getOrElse, getOrDefault, recover, recoverCatching) can't be members with that signature (no lower bounds), so they take/return `@UnsafeVariance T`; widening like `getOrDefault(null)` on a `Result<Int>` needs `getOrNull() ?: x` / `fold`. Only `kotlin.Result<T>.toResult()` stays an extension.

## Bugs

- **B-001** — AGP stamps a library AAR's `minCompileSdk` with the compileSdk it was BUILT with, and every consumer's `check<Variant>AarMetadata` enforces it — our compileSdk (raised for the Compose sample, N-004) was forced on all consumers, surfacing only when they assemble (1.0.0). Fixed by `android { aarMetadata { minCompileSdk = <android-min-compile-sdk> } }` in the convention plugin — a separate catalog key, decoupled from compileSdk.

## Notes

- **N-001** — Build JDK stays 21: detekt 1.23.8's embedded Kotlin compiler crashes on JDK 25 (detekt/detekt#8714), and it's also the remaining Gradle 10 blocker (`ReportingExtension.file(String)`). Both are fixed in detekt 2.x — revisit when it's stable. Check `build/reports/problems/` after Gradle bumps.
- **N-002** — A git worktree doesn't carry the gitignored `local.properties`; AGP fails at task-graph time ("SDK location not found") — copy it from `local.properties.example`.
- **N-003** — AGP's KMP Android target is a `DecoratedExternalKotlinTarget`, NOT a `KotlinJvmTarget` — `targets.withType<KotlinJvmTarget>()` never reaches it, and unset its `jvmTarget` follows the build JDK. Set `jvmTarget` explicitly on `android { compilerOptions {} }` AND `jvm { compilerOptions {} }` (the convention plugin reads the catalog's `jvm-target`).
- **N-004** — AndroidX AARs carry `minCompileSdk` in their aar-metadata, and AGP's `checkAarMetadata` enforces it: a library bump can force a compileSdk bump. `mise run check` never builds the sample apps — `mise run build:samples` does, and CI's fast leg runs it.
- **N-005** — Kotlin 2.4 idioms pass the whole toolchain (every target with allWarningsAsErrors, ktlint, detekt): explicit backing fields, context parameters, `Uuid.random()` (no opt-in), `when` guards, `$$` strings. An explicit-backing-field `StateFlow` exports as a read-only `StateFlow` (SKIE: `SkieKotlinStateFlow`) — the mutable field is invisible.
- **N-006** — A custom `applyDefaultHierarchyTemplate { … group("apple") { withIos(); withMacos() } }` puts targets DIRECTLY under appleMain — no iosMain/macosMain. The implicit default template has them (native → apple → ios/macos).
- **N-007** — PR templates and YAML issue forms apply only in GitHub's web UI; `gh pr/issue create --body` bypasses them, so agents follow them only because CLAUDE.md §11 says to. A submitted form renders as `### <label>` + answer per field (`_No response_` when skipped). HTML comments don't nest: a template can't quote `<!-- AI: … -->` inside a comment.
- **N-008** — Every `- [ ]` in a PR body is live (one click toggles it) and feeds the "N of M tasks" counter, which a pick-one group can never complete. So checkboxes appear only in the done-gate; choices are plain bullets you prune, and human review is signalled by leaving draft.
- **N-009** — A push or PR made with `GITHUB_TOKEN` triggers no workflows (workflow_dispatch excepted), and creating a PR with it needs "Allow GitHub Actions to create and approve pull requests". release-pr.yml therefore dispatches ci.yml + changeset.yml on `release/next` itself — unless a GitHub App token (`RELEASE_APP_CLIENT_ID`) is configured.
- **N-010** — main is branch-protected, so no workflow pushes to it. The release commit carrying the remote-binary Package.swift lives ONLY on its `vX.Y.Z` tag (a detached commit); main keeps the local-dev Package.swift. SPM resolves the manifest from the tag.
