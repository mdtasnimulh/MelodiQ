package com.tasnimulhasan.entity.metadata

/** Editable tag fields. Any field left null/blank when saving is left unchanged rather than
 * cleared - the editor always starts pre-filled from the file's current tags, so "unchanged"
 * only happens if the user genuinely didn't touch that field. */
data class EditableMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: String,
    val trackNumber: String,
    val discNumber: String,
    /** Raw image bytes (JPEG/PNG) for a new embedded cover, or null to leave artwork as-is. */
    val newArtwork: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EditableMetadata) return false
        return title == other.title && artist == other.artist && album == other.album &&
            albumArtist == other.albumArtist && genre == other.genre && year == other.year &&
            trackNumber == other.trackNumber && discNumber == other.discNumber &&
            newArtwork.contentEquals(other.newArtwork)
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + albumArtist.hashCode()
        result = 31 * result + genre.hashCode()
        result = 31 * result + year.hashCode()
        result = 31 * result + trackNumber.hashCode()
        result = 31 * result + discNumber.hashCode()
        result = 31 * result + (newArtwork?.contentHashCode() ?: 0)
        return result
    }
}

data class AudioFileInfo(
    val format: String,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val channels: Int?,
    val durationMs: Long,
    val sizeBytes: Long,
    val filePath: String,
)

/** Whether a file's format is one this app can actually rewrite tags for. Shown to the user
 * instead of silently failing or, worse, attempting something unreliable. */
enum class TagWriteSupport {
    MP3, FLAC, UNSUPPORTED
}

sealed interface MetadataEditResult {
    data object Success : MetadataEditResult
    /** Scoped storage requires user consent before writing a file this app doesn't own -
     * [intentSender] must be launched by the UI; the caller retries after a positive result. */
    data class NeedsPermission(val intentSender: android.content.IntentSender) : MetadataEditResult
    data object UnsupportedFormat : MetadataEditResult
    data class Error(val message: String) : MetadataEditResult
}

sealed interface FileOpResult {
    data object Success : FileOpResult
    data class NeedsPermission(val intentSender: android.content.IntentSender) : FileOpResult
    data class Error(val message: String) : FileOpResult
}
