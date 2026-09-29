---
title: Android consumers compile against API 34 or newer
change: patch
description: The AAR's minCompileSdk rises from 30 to 34 (Android 14); minSdk stays 30.
---

The Android AAR now declares `minCompileSdk=34` (Android 14), up from 30, so the library can build on newer Android APIs. If your app or library compiles against an older SDK, raise `compileSdk` to 34 or newer. Devices are unaffected: `minSdk` is still 30.
