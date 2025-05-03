package com.tasnimulhasan.melodiq

import org.gradle.api.JavaVersion

object AppConfig {
    const val APPLICATION_ID = "com.tasnimulhasan.melodiq"
    const val MIN_SDK_VERSION = 30
    const val COMPILE_SDK_VERSION = 36
    const val TARGET_SDK_VERSION = 36
    var testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    val compatibilityVersion = JavaVersion.VERSION_21
    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0.0"
}