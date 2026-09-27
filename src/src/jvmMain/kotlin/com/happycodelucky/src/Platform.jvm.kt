package com.happycodelucky.src

internal actual fun platformName(): String = "JVM ${System.getProperty("java.version")}"
