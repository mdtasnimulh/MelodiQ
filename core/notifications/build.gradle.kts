plugins {
    alias(libs.plugins.melodiq.android.library)
    alias(libs.plugins.melodiq.android.hilt)
}

android {
    namespace = "com.tasnimulhasan.notifications"
}

dependencies {
    //api(projects.core.model)

    implementation(projects.core.common)
    implementation(libs.bundles.media3.dependencies)

    compileOnly(platform(libs.androidx.compose.bom))
    compileOnly(libs.androidx.compose.runtime)
}