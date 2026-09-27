# KMP Library Template

A batteries-included **Kotlin Multiplatform library** template for iOS, macOS,
Android, and JVM. Distilled from real libraries (reachable, ssdp-kmp,
backgrounder). Click **Use this template** on GitHub, or clone and render.

> This file (and `scripts/`) are removed when you render the template.

## What you get

- **Two library modules:** `:src` (the library) + `:src-testing` (public test
  fakes for consumers), shaped by a `template.kmp-library` convention plugin.
- **Targets:** iosArm64, iosSimulatorArm64, macosArm64, Android (arm64-v8a), jvm().
- **SKIE** for idiomatic Swift interop; **Kermit** for logging (wired in for free).
- **Dual distribution:** Maven Central (vanniktech) + GitHub Releases/SPM (KMMBridge).
- **Sample apps** under `apps/` (ios, macos, android, jvm-cli) — Apple apps via
  xcodegen + local SPM.
- **Docs** (mkdocs Material + Dokka), **CI/release/docs** GitHub Actions,
  **Renovate**, **detekt** + **ktlint**.
- **`mise.toml`** as the task contract; **`CLAUDE.md`** (+ `AGENTS.md` symlink)
  as the agent guide.

## Render it

```bash
brew install mise
mise trust && mise install

# Rename, retoken, and clean up the template:
mise run init my-library
#   defaults: --group com.happycodelucky  --org happycodelucky
#   override: mise run init my-library --group com.acme --org acme --display-name "My Library"
```

`init`:
1. renames `src/` → `my-library/`, `src-testing/` → `my-library-testing/`, and
   the Kotlin package dirs to your group;
2. renames the convention plugins (`template.*` → `my-library.*`);
3. replaces the `__PROJECT_NAME__` / `__DISPLAY_NAME__` / `__FRAMEWORK__` tokens
   and the group/org defaults;
4. strips its own `[tasks.init]` task, deletes this file and `scripts/`.

Then:

```bash
rm -rf .git && git init && git add -A && git commit -m "Initial commit"
mise run check        # ktlint + detekt + every test target — should be green
```

Fill in `CLAUDE.md` §1 (Scope) and replace the placeholder `Greeter` API.

## Before you publish

- Maven Central + GitHub: see [`.github/PUBLISHING.md`](.github/PUBLISHING.md)
  for the four `MAVEN_CENTRAL_*` secrets and the `continuous-deployment`
  environment.
- The toolchain pins live in `gradle/libs.versions.toml`. The Kotlin pin is
  bounded by SKIE — bump SKIE first.
