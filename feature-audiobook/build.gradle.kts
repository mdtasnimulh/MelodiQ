plugins {
    alias(libs.plugins.melodiq.android.feature)
    alias(libs.plugins.melodiq.android.library.compose)
}

android {
    namespace = "com.tasnimulhasan.featureaudiobook"
}

dependencies {
    // Reads the cover image embedded in a PDF's first page.
    implementation(libs.pdfbox.android)
}
