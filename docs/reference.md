---
title: API reference
---

# API reference

Every public declaration of each published module, read from the ABI dumps
committed under `<module>/api/`. `mise run check` fails whenever the code and
its dump disagree, so this page always matches the released code. It's built
for scanning, and for AI tools: the site's `/llms-full.txt` inlines it.

It lists signatures only. Doc comments, and declarations that exist only on
Android or the JVM, are in the [Dokka reference]({{ config.site_url }}api/).
Swift consumers see `Result` as `KotlinResult`, plus the bundled Swift helpers
(`get()`, `result(as:)`), which aren't in the dump (see [iOS](platforms/ios.md)).

Reading the dumps: `com.example/Type` is the type `Type` in package
`com.example`, `<init>` is a constructor, and `= ...` marks a parameter with a
default value.

{{ api_reference() }}
