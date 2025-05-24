plugins {
    alias(libs.plugins.melodiq.android.feature)
    alias(libs.plugins.melodiq.android.library.compose)
}

android {
    namespace = "com.tasnimulhasan.albums"
}

dependencies {
    implementation(libs.androidx.media3.exoplayer.hls)
}