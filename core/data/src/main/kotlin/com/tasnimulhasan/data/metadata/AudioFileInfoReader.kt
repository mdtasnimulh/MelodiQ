package com.tasnimulhasan.data.metadata

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.tasnimulhasan.entity.metadata.AudioFileInfo

class AudioFileInfoReader @Inject constructor(@ApplicationContext private val context: Context) {

    fun read(uri: Uri, mimeType: String?, sizeBytes: Long, durationMs: Long, displayName: String): AudioFileInfo {
        var sampleRate: Int? = null
        var channels: Int? = null
        var bitrateKbps: Int? = null

        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!trackMime.startsWith("audio/")) continue

                sampleRate = format.getIntSafe(MediaFormat.KEY_SAMPLE_RATE)
                channels = format.getIntSafe(MediaFormat.KEY_CHANNEL_COUNT)
                bitrateKbps = format.getIntSafe(MediaFormat.KEY_BIT_RATE)?.let { it / 1000 }
                break
            }
            extractor.release()
        } catch (_: Exception) {
            // Fall through with whatever partial info we have - file info is a display-only
            // feature, never worth failing the whole screen over.
        }

        // MediaExtractor doesn't always expose KEY_BIT_RATE (format-dependent) - derive it
        // from size/duration as a fallback so the field isn't just blank.
        if (bitrateKbps == null && durationMs > 0) {
            bitrateKbps = ((sizeBytes * 8) / durationMs).toInt() // bytes*8 bits / ms = kbps
        }

        return AudioFileInfo(
            format = formatLabel(mimeType, displayName),
            bitrateKbps = bitrateKbps,
            sampleRateHz = sampleRate,
            channels = channels,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            filePath = displayName,
        )
    }

    private fun MediaFormat.getIntSafe(key: String): Int? = try {
        if (containsKey(key)) getInteger(key) else null
    } catch (_: Exception) {
        null
    }

    private fun formatLabel(mimeType: String?, displayName: String): String = when {
        mimeType?.contains("flac", true) == true -> "FLAC"
        mimeType?.contains("mpeg", true) == true || mimeType?.contains("mp3", true) == true -> "MP3"
        mimeType?.contains("mp4", true) == true || mimeType?.contains("m4a", true) == true -> "AAC/M4A"
        mimeType?.contains("ogg", true) == true -> "OGG Vorbis"
        mimeType?.contains("wav", true) == true -> "WAV"
        else -> displayName.substringAfterLast('.', "").uppercase().ifBlank { "Unknown" }
    }
}
