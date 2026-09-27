---
title: Android
---

# Android

TODO: document anything Android consumers need beyond the dependency.

If the library does networking, note any required permissions (e.g.
`INTERNET`, `ACCESS_NETWORK_STATE`) and — for multicast/SSDP-style discovery —
acquiring a `WifiManager.MulticastLock` so the OS doesn't filter multicast
packets. Mention the minimum supported `minSdk` and any runtime permissions the
host app must request.
