package com.tasnimulhasan.featureaudiobook

/**
 * One book in the library. Only this small record is stored by the app - the PDF itself stays
 * where it is on the phone and is read through [uri] (a persisted document-picker grant). Reading
 * progress is stored separately by [AudioBookRepository].
 */
data class Book(
    val id: String,
    val uri: String,
    val title: String,
    val sizeBytes: Long,
    val pages: Int,
    val favourite: Boolean,
    val addedAt: Long,
    /** "pdf" or "epub". Null on books saved before EPUB support, which are PDFs. */
    val format: String? = null,
) {
    val isEpub: Boolean get() = format == "epub"
    val formatLabel: String get() = if (isEpub) "EPUB" else "PDF"
}
