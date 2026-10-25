plugins {
    alias(libs.plugins.melodiq.android.feature)
    alias(libs.plugins.melodiq.android.library.compose)
}

android {
    namespace = "com.tasnimulhasan.radio"
}

dependencies {
    // HLS (.m3u8) internet radio streams; plain Icecast/Shoutcast needs nothing extra.
    implementation(libs.androidx.media3.exoplayer.hls)

    // android.hardware.radio.* is a @SystemApi: stubs for compiling only, never packaged.
    compileOnly(projects.feature.radioStubs)
}
