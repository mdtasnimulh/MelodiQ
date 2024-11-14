plugins {
    alias(libs.plugins.melodiq.android.feature)
    alias(libs.plugins.melodiq.android.library.compose)
}

android {
    namespace = "com.tasnimulhasan.featureplayer"
}

dependencies {
    implementation(libs.coil.kt.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.bundles.accompanist)
    implementation(libs.bundles.exoplayer)
    implementation(libs.palette.ktx)
}