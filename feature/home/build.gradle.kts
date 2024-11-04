plugins {
    alias(libs.plugins.melodiq.android.feature)
    alias(libs.plugins.melodiq.android.library.compose)
}

android {
    namespace = "com.tasnimulhasan.home"
}

dependencies {
    implementation(libs.accompanist.permissions)
    implementation(libs.coil.kt.compose)
}