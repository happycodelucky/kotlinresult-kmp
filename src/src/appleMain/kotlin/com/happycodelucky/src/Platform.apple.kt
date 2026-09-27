package com.happycodelucky.src

import platform.Foundation.NSProcessInfo

// appleMain is shared by iOS and macOS, so this must compile on both. NSProcessInfo
// is part of Foundation on every Apple platform; UIKit (iOS-only) belongs in
// iosMain. operatingSystemVersionString reads like "Version 18.0 (Build …)".
internal actual fun platformName(): String = "Apple (${NSProcessInfo.processInfo.operatingSystemVersionString})"
