plugins {
    alias(libs.plugins.melodiq.android.feature)
    alias(libs.plugins.melodiq.android.library.compose)
}

android {
    namespace = "com.tasnimulhasan.featurequeue"
}

dependencies {
    implementation(libs.coil.kt.compose)

}