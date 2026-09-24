plugins {
    alias(libs.plugins.melodiq.android.feature)
    alias(libs.plugins.melodiq.android.library.compose)
}

android {
    namespace = "com.tasnimulhasan.songdetails"
}

dependencies {
    implementation(libs.coil.kt.compose)
}
