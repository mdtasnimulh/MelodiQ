plugins {
    alias(libs.plugins.melodiq.android.library)
    alias(libs.plugins.melodiq.android.library.compose)
    alias(libs.plugins.melodiq.android.hilt)
}

android {
    namespace = "com.tasnimulhasan.datastore"
}

dependencies {
    implementation(libs.androidx.dataStore.core)
    implementation(libs.androidx.datastore.preferences)
    api(projects.core.model.entity)
}