package com.tasnimulhasan.data.lyrics

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import java.io.BufferedInputStream
import java.io.InputStream

/**
 * Best-effort embedded-lyrics extraction, implemented directly against the ID3v2 and Vorbis
 * Comment specs since this project has no audio-tagging library dependency. Deliberately
 * narrow in scope:
 *  - MP3 (ID3v2.2/.3/.4): reads the USLT (unsynchronised lyrics) frame.
 *  - FLAC: reads the Vorbis Comment block's LYRICS/UNSYNCEDLYRICS field.
 *  - M4A/AAC is NOT handled here - its atom-tree metadata format needs a real parser, and
 *    getting a hand-rolled one subtly wrong (rather than just finding nothing) is worse than
 *    not attempting it. Those files fall through to the next resolution step (.lrc file).
 *
 * Every read is wrapped defensively - a malformed or truncated tag returns null rather than
 * throwing, since this must never be able to break playback.
 */
class EmbeddedLyricsReader @Inject constructor(@ApplicationContext private val context: Context) {

    fun read(contentUri: Uri, mimeType: String?): String? = try {
        when {
            mimeType?.contains("flac", ignoreCase = true) == true -> readFlacLyrics(contentUri)
            else -> readId3UsltLyrics(contentUri) ?: run {
                // Mime type was unhelpful/missing - harmless to also try the FLAC reader,
                // it will simply fail the "fLaC" magic-number check and return null.
                readFlacLyrics(contentUri)
            }
        }
    } catch (_: Exception) {
        null
    }

    // --- ID3v2 (MP3) ---

    private fun readId3UsltLyrics(uri: Uri): String? {
        context.contentResolver.openInputStream(uri)?.use { raw ->
            val input = BufferedInputStream(raw)
            val header = ByteArray(10)
            if (input.read(header) != 10) return null
            if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
                return null
            }
            val majorVersion = header[3].toInt()
            val tagSize = synchsafeToInt(header[6], header[7], header[8], header[9])
            val tagBody = ByteArray(tagSize)
            var read = 0
            while (read < tagSize) {
                val n = input.read(tagBody, read, tagSize - read)
                if (n <= 0) break
                read += n
            }
            return findUsltFrame(tagBody, read, majorVersion)
        }
        return null
    }

    private fun findUsltFrame(tag: ByteArray, tagLength: Int, majorVersion: Int): String? {
        var offset = 0
        val frameIdSize = if (majorVersion == 2) 3 else 4
        val frameHeaderSize = if (majorVersion == 2) 6 else 10

        while (offset + frameHeaderSize <= tagLength) {
            val frameId = String(tag, offset, frameIdSize, Charsets.US_ASCII)
            if (frameId.isBlank() || frameId[0] == '\u0000') break // padding reached

            val frameSize: Int
            if (majorVersion == 2) {
                frameSize = (tag[offset + 3].toInt() and 0xFF shl 16) or
                    (tag[offset + 4].toInt() and 0xFF shl 8) or
                    (tag[offset + 5].toInt() and 0xFF)
            } else if (majorVersion == 4) {
                frameSize = synchsafeToInt(tag[offset + 4], tag[offset + 5], tag[offset + 6], tag[offset + 7])
            } else {
                // v2.3 frame sizes are a regular (non-synchsafe) big-endian uint32.
                frameSize = (tag[offset + 4].toInt() and 0xFF shl 24) or
                    (tag[offset + 5].toInt() and 0xFF shl 16) or
                    (tag[offset + 6].toInt() and 0xFF shl 8) or
                    (tag[offset + 7].toInt() and 0xFF)
            }

            val frameDataStart = offset + frameHeaderSize
            if (frameSize <= 0 || frameDataStart + frameSize > tagLength) break

            // USLT (v2.3/2.4) / ULT (v2.2) = unsynchronised lyrics/text transcription.
            if (frameId == "USLT" || frameId == "ULT") {
                return decodeUsltFrameData(tag, frameDataStart, frameSize)
            }

            offset = frameDataStart + frameSize
        }
        return null
    }

    /** USLT payload: [1 byte encoding][3 byte language][content descriptor, terminated][lyrics text]. */
    private fun decodeUsltFrameData(data: ByteArray, start: Int, length: Int): String? {
        if (length < 5) return null
        val encoding = data[start].toInt()
        val textStart = start + 1 + 3 // skip encoding byte + 3-byte language code
        val end = start + length

        return when (encoding) {
            0 -> { // ISO-8859-1, null-terminated descriptor (1 byte)
                val descriptorEnd = indexOfByte(data, textStart, end, 0) ?: return null
                String(data, descriptorEnd + 1, end - (descriptorEnd + 1), Charsets.ISO_8859_1)
            }
            3 -> { // UTF-8 (v2.4 only), null-terminated descriptor (1 byte)
                val descriptorEnd = indexOfByte(data, textStart, end, 0) ?: return null
                String(data, descriptorEnd + 1, end - (descriptorEnd + 1), Charsets.UTF_8)
            }
            1, 2 -> { // UTF-16 (with or without BOM), descriptor terminated by a 2-byte null
                val descriptorEnd = indexOfDoubleNull(data, textStart, end) ?: return null
                val textBytesStart = descriptorEnd + 2
                if (textBytesStart >= end) return null
                String(data, textBytesStart, end - textBytesStart, Charsets.UTF_16)
            }
            else -> null
        }?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun indexOfByte(data: ByteArray, start: Int, end: Int, value: Byte): Int? {
        for (i in start until end) if (data[i] == value) return i
        return null
    }

    private fun indexOfDoubleNull(data: ByteArray, start: Int, end: Int): Int? {
        var i = start
        while (i + 1 < end) {
            if (data[i] == 0.toByte() && data[i + 1] == 0.toByte()) return i
            i += 2
        }
        return null
    }

    private fun synchsafeToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        (b0.toInt() and 0x7F shl 21) or (b1.toInt() and 0x7F shl 14) or
            (b2.toInt() and 0x7F shl 7) or (b3.toInt() and 0x7F)

    // --- FLAC (Vorbis Comment) ---

    private fun readFlacLyrics(uri: Uri): String? {
        context.contentResolver.openInputStream(uri)?.use { raw ->
            val input = BufferedInputStream(raw)
            val magic = ByteArray(4)
            if (input.read(magic) != 4 || String(magic, Charsets.US_ASCII) != "fLaC") return null

            while (true) {
                val blockHeader = ByteArray(4)
                if (input.read(blockHeader) != 4) return null
                val isLast = (blockHeader[0].toInt() and 0x80) != 0
                val blockType = blockHeader[0].toInt() and 0x7F
                val blockLength = (blockHeader[1].toInt() and 0xFF shl 16) or
                    (blockHeader[2].toInt() and 0xFF shl 8) or (blockHeader[3].toInt() and 0xFF)

                if (blockType == 4) { // VORBIS_COMMENT
                    val block = ByteArray(blockLength)
                    if (!readFully(input, block)) return null
                    return parseVorbisComment(block)
                } else {
                    if (!skipFully(input, blockLength.toLong())) return null
                }

                if (isLast) return null
            }
        }
        return null
    }

    private fun parseVorbisComment(block: ByteArray): String? {
        var pos = 0
        fun readLEInt(): Int {
            val v = (block[pos].toInt() and 0xFF) or
                (block[pos + 1].toInt() and 0xFF shl 8) or
                (block[pos + 2].toInt() and 0xFF shl 16) or
                (block[pos + 3].toInt() and 0xFF shl 24)
            pos += 4
            return v
        }

        if (pos + 4 > block.size) return null
        val vendorLength = readLEInt()
        pos += vendorLength
        if (pos + 4 > block.size) return null
        val commentCount = readLEInt()

        repeat(commentCount) {
            if (pos + 4 > block.size) return null
            val commentLength = readLEInt()
            if (pos + commentLength > block.size) return null
            val comment = String(block, pos, commentLength, Charsets.UTF_8)
            pos += commentLength

            val eq = comment.indexOf('=')
            if (eq > 0) {
                val key = comment.substring(0, eq).uppercase()
                if (key == "LYRICS" || key == "UNSYNCEDLYRICS" || key == "SYNCEDLYRICS") {
                    val value = comment.substring(eq + 1).trim()
                    if (value.isNotBlank()) return value
                }
            }
        }
        return null
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Boolean {
        var read = 0
        while (read < buffer.size) {
            val n = input.read(buffer, read, buffer.size - read)
            if (n <= 0) return false
            read += n
        }
        return true
    }

    private fun skipFully(input: InputStream, count: Long): Boolean {
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                // Some stream implementations return 0 from skip() near EOF even when more
                // data is technically available - fall back to reading-and-discarding.
                if (input.read() == -1) return false
                remaining -= 1
            } else {
                remaining -= skipped
            }
        }
        return true
    }
}
