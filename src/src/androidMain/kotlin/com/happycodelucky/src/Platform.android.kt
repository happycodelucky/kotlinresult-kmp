package com.happycodelucky.src

internal actual fun platformName(): String = "Android ${android.os.Build.VERSION.SDK_INT}"
