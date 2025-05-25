plugins {
    alias(libs.plugins.melodiq.android.library)
    alias(libs.plugins.melodiq.android.library.compose)
    alias(libs.plugins.melodiq.android.hilt)
}

android {
    namespace = "com.tasnimulhasan.di"
}

dependencies {
    api(projects.core.datastore)
    implementation(libs.timber)
}