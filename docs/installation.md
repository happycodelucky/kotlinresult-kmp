---
title: Installation
---

# Installation

KotlinResult is distributed two ways: through Maven Central for Gradle / KMP
consumers, and through GitHub Releases (as an XCFramework) for pure-Swift Swift
Package Manager consumers.

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

## Swift Package Manager

Add this repository as a package dependency, pinned to a release tag:

```swift
dependencies: [
    .package(url: "https://github.com/happycodelucky/kotlinresult-kmp.git", from: "{{ version }}")
]
```

The tagged `Package.swift` references a prebuilt `KotlinresultKit.xcframework`
release asset by URL + checksum — no Gradle build and no authentication required.
