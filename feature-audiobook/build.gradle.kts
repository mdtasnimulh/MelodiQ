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

    // On-device ML Kit: OCR for scanned pages (so they can be read aloud and selected) and
    // language detection (so the narrator speaks the book's language).
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.language.id)
}
