---
title: Installation
---

# Installation

KotlinResult is published to Maven Central for Gradle / KMP consumers. Swift
reaches it through your library's framework (see
[Use it from a KMP library](recipes/kmp-library.md) for the required `export`).

## Gradle

Add the dependency directly:

```kotlin
dependencies {
    implementation("com.happycodelucky.kotlinresult:kotlinresult:{{ version }}")
}
```

Or, with a version catalog (`gradle/libs.versions.toml`):

```kotlin
[versions]
kotlinresult = "{{ version }}"

[libraries]
kotlinresult = { module = "com.happycodelucky.kotlinresult:kotlinresult", version.ref = "kotlinresult" }
```

```kotlin
dependencies {
    implementation(libs.kotlinresult)
}
```
