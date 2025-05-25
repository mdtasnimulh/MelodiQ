plugins {
    alias(libs.plugins.melodiq.android.library)
    alias(libs.plugins.melodiq.android.library.compose)
    alias(libs.plugins.melodiq.android.hilt)
}

android {
    namespace = "com.tasnimulhasan.common"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.designSystem)
    implementation(projects.core.model.entity)
    implementation(libs.timber)
    implementation(libs.bundles.androidx.core.dependencies)
    implementation(libs.bundles.androidx.material.dependencies)
    implementation(libs.bundles.androidx.lifecycle.dependencies)
    implementation(libs.bundles.androidx.navigation.dependencies)
    implementation(libs.bundles.media3.dependencies)
    implementation(libs.compose)
}