plugins {
    alias(libs.plugins.melodiq.android.feature)
    alias(libs.plugins.melodiq.android.library.compose)
}

android {
    namespace = "com.tasnimulhasan.settings"
}

dependencies {
    implementation(libs.accompanist.permissions)
}