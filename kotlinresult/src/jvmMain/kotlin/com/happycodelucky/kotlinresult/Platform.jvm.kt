package com.happycodelucky.kotlinresult

internal actual fun platformName(): String = "JVM ${System.getProperty("java.version")}"
