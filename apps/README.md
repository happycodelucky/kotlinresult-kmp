# Sample apps

Three samples that consume the `:kotlinresult` library. They're demo
scaffolding — not published, and outside lint and the library check gate.

| App | Path | Consumes the library via |
|---|---|---|
| Apple consumer | `apps/apple-consumer` (`:apple-consumer`) | Gradle project dependency, `export`ed into its framework |
| Android | `apps/android` (`:androidApp`) | Gradle project dependency (`project(":kotlinresult")`) |
| JVM CLI | `apps/jvm-cli` (`:jvm-cli`) | Gradle project dependency (`project(":kotlinresult")`) |

## Apple consumer

`:kotlinresult` ships no framework: Swift reaches it only through a KMP
library's own framework. `:apple-consumer` stands in for such a library — it
applies SKIE, depends on `:kotlinresult` with `api`, and `export`s it, exactly
as the README's "Apple frameworks: export it" asks. Its `src/appleMain/swift`
wraps a small Kotlin API with the Swift helpers, and `swift/main.swift` checks
them at runtime:

```bash
mise run build:swift    # link the debug frameworks (macOS + iOS simulator)
mise run test:swift     # …then compile + run swift/main.swift against the macOS one
```

CI's Apple leg runs `test:swift`: it's the only place this repo compiles the
Swift `:kotlinresult` bundles into its klibs.

## Android

```bash
mise run open:android   # opens apps/android in Android Studio
./gradlew :androidApp:installDebug   # build + install on a connected device
```

## JVM CLI

```bash
mise run build:jvm
./gradlew :jvm-cli:run
```
