---
title: Ship llms.txt and llms-full.txt inside every published artifact
change: patch
description: Every jar and the AAR now carry the module's public API with its KDoc, for AI tools, under META-INF/com.happycodelucky.kotlinresult/<artifactId>/.
---

Every published jar (JVM, metadata, sources, javadoc) and the Android AAR now carry two files for AI coding tools, under `META-INF/com.happycodelucky.kotlinresult/<artifactId>/`:

- `llms.txt` — what the artifact is, its coordinates, and links to the docs site.
- `llms-full.txt` — the module's full public API with its KDoc, for exactly the version it ships in.

They are namespaced by coordinates, so they never collide with another library's, and in the AAR they sit at the archive root, so they never reach your APK. No API or behavior changes.

The docs site also serves `/llms.txt` and `/llms-full.txt`, and gains an [API reference](https://happycodelucky.github.io/kotlinresult-kmp/reference/) page.
