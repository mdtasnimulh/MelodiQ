plugins {
    alias(libs.plugins.melodiq.android.library)
    alias(libs.plugins.melodiq.android.library.compose)
    alias(libs.plugins.melodiq.android.hilt)
    id("kotlinx-serialization")
}

android {
    namespace = "com.tasnimulhasan.data"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.di)
    implementation(projects.core.database)
    implementation(projects.core.common)
    implementation(projects.core.model.entity)
    implementation(projects.core.model.apiResponse)
    implementation(projects.core.notifications)
    implementation(libs.androidx.dataStore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlin.coroutines)
    testImplementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.media3.dependencies)
}