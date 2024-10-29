plugins {
    alias(libs.plugins.melodiq.android.library)
    alias(libs.plugins.melodiq.android.library.compose)
    alias(libs.plugins.melodiq.android.hilt)
}

android {
    namespace = "com.tasnimulhasan.database"
}

dependencies {
    implementation(projects.core.domain)

    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.room.dependencies)
    implementation(libs.room.common)
    ksp(libs.room.compiler)
}