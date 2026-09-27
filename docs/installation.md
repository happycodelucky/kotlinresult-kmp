---
title: Installation
---

# Installation

__DISPLAY_NAME__ is distributed two ways: through Maven Central for Gradle / KMP
consumers, and through GitHub Releases (as an XCFramework) for pure-Swift Swift
Package Manager consumers.

## Gradle

Add the dependency directly:

```kotlin
dependencies {
    implementation("com.happycodelucky.__PROJECT_NAME__:__PROJECT_NAME__:{{ version }}")
}
```

Or, with a version catalog (`gradle/libs.versions.toml`):

```kotlin
[versions]
__PROJECT_NAME__ = "{{ version }}"

[libraries]
__PROJECT_NAME__ = { module = "com.happycodelucky.__PROJECT_NAME__:__PROJECT_NAME__", version.ref = "__PROJECT_NAME__" }
```

```kotlin
dependencies {
    implementation(libs.__PROJECT_NAME__)
}
```

## Swift Package Manager

Add this repository as a package dependency, pinned to a release tag:

```swift
dependencies: [
    .package(url: "https://github.com/happycodelucky/__PROJECT_NAME__.git", from: "{{ version }}")
]
```

The tagged `Package.swift` references a prebuilt `__FRAMEWORK__.xcframework`
release asset by URL + checksum — no Gradle build and no authentication required.
