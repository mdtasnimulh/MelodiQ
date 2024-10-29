plugins {
    alias(libs.plugins.melodiq.android.library)
    alias(libs.plugins.melodiq.android.library.compose)
}

android {
    namespace = "com.tasnimulhasan.designsystem"
}

dependencies {
    implementation(libs.bundles.androidx.core.dependencies)
    implementation(libs.bundles.androidx.material.dependencies)
    implementation(libs.bundles.androidx.navigation.dependencies)

    implementation(libs.coil.kt.compose)

    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.compose.ui.test)
}