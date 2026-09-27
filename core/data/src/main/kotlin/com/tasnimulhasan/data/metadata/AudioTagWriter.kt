package com.tasnimulhasan.data.metadata

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.net.Uri
import com.tasnimulhasan.entity.metadata.EditableMetadata
import com.tasnimulhasan.entity.metadata.TagWriteSupport
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

/**
 * Writes tags to MP3 (ID3v2.3) and FLAC (Vorbis Comment) files.
 *
 * Safety principle this whole class is built around: audio sample data is NEVER parsed,
 * decoded, or reinterpreted. Every write is "rebuild the metadata region at the head of the
 * file, then copy the remaining audio bytes through unchanged" - the same file bytes that
 * were already playing correctly before the edit remain byte-for-byte identical after it,
 * just relocated. A bug here can produce a wrong TAG, but cannot corrupt the audio itself.
 *
 * M4A/AAC is intentionally unsupported - its atom-tree metadata format requires recomputing
 * chunk-offset tables when the header size changes, which is a well-known source of real
 * file corruption in hand-rolled implementations, and this project has no verified tagging
 * library to lean on instead.
 */
class AudioTagWriter @Inject constructor(@ApplicationContext private val context: Context) {

    fun supportFor(mimeType: String?): TagWriteSupport = when {
        mimeType?.contains("mpeg", ignoreCase = true) == true -> TagWriteSupport.MP3
        mimeType?.contains("mp3", ignoreCase = true) == true -> TagWriteSupport.MP3
        mimeType?.contains("flac", ignoreCase = true) == true -> TagWriteSupport.FLAC
        else -> TagWriteSupport.UNSUPPORTED
    }

    /**
     * Writes [metadata] into the file at [uri] via [outputStream] (already opened by the
     * caller in truncate mode, after permission is confirmed). Reads the original file
     * first through a *separate* read of [uri] to extract the audio-data portion into a
     * temp file, before anything is written - this ordering matters: opening the output
     * stream in truncate mode destroys the original content, so the audio data must be
     * safely copied out before that happens.
     */
    fun write(uri: Uri, mimeType: String?, metadata: EditableMetadata, outputStream: OutputStream): Boolean {
        val support = supportFor(mimeType)
        if (support == TagWriteSupport.UNSUPPORTED) return false

        val tempAudioFile = File.createTempFile("melodiq_audio_", ".tmp", context.cacheDir)
        return try {
            extractAudioDataTo(uri, support, tempAudioFile) ?: return false

            val newTagBytes = when (support) {
                TagWriteSupport.MP3 -> buildId3v2Tag(metadata)
                TagWriteSupport.FLAC -> buildFlacHeader(uri, metadata) ?: return false
                TagWriteSupport.UNSUPPORTED -> return false
            }

            outputStream.use { out ->
                out.write(newTagBytes)
                tempAudioFile.inputStream().use { audioIn -> audioIn.copyTo(out) }
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            tempAudioFile.delete()
        }
    }

    // --- Step 1: safely extract the audio-data portion (everything after the metadata
    // region) into a temp file, BEFORE any destructive write happens. ---

    private fun extractAudioDataTo(uri: Uri, support: TagWriteSupport, destination: File): Long? {
        context.contentResolver.openInputStream(uri)?.use { raw ->
            val input = BufferedInputStream(raw)
            val skipBytes = when (support) {
                TagWriteSupport.MP3 -> id3v2TagSize(input)
                TagWriteSupport.FLAC -> flacMetadataTotalSize(input) ?: return null
                TagWriteSupport.UNSUPPORTED -> return null
            }
            // id3v2TagSize/flacMetadataTotalSize already consumed exactly that many bytes
            // (or, for mp3, consumed nothing if there was no tag) - `input` is now
            // positioned at the start of the actual audio data.
            destination.outputStream().use { out -> input.copyTo(out) }
            return skipBytes
        }
        return null
    }

    /** Reads (and consumes) an existing ID3v2 header if present, returning how many bytes
     * it and its frames occupied. Returns 0, with the stream unconsumed, if there is none -
     * plenty of MP3s have no ID3v2 tag at all yet. */
    private fun id3v2TagSize(input: BufferedInputStream): Long {
        input.mark(10)
        val header = ByteArray(10)
        val read = input.read(header)
        if (read != 10 || header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
            input.reset()
            return 0L
        }
        val tagSize = synchsafeToInt(header[6], header[7], header[8], header[9])
        var remaining = tagSize.toLong()
        val buffer = ByteArray(8192)
        while (remaining > 0) {
            val n = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt().coerceAtLeast(1))
            if (n <= 0) break
            remaining -= n
        }
        return 10L + tagSize
    }

    /** Walks (and consumes) every FLAC metadata block, returning the total byte size of the
     * "fLaC" magic plus every metadata block - i.e. exactly where the audio frames begin. */
    private fun flacMetadataTotalSize(input: BufferedInputStream): Long? {
        val magic = ByteArray(4)
        if (input.read(magic) != 4 || String(magic, Charsets.US_ASCII) != "fLaC") return null
        var total = 4L
        while (true) {
            val blockHeader = ByteArray(4)
            if (input.read(blockHeader) != 4) return null
            val isLast = (blockHeader[0].toInt() and 0x80) != 0
            val length = (blockHeader[1].toInt() and 0xFF shl 16) or
                (blockHeader[2].toInt() and 0xFF shl 8) or (blockHeader[3].toInt() and 0xFF)
            total += 4 + length
            var remaining = length.toLong()
            val buffer = ByteArray(8192)
            while (remaining > 0) {
                val n = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt().coerceAtLeast(1))
                if (n <= 0) return null
                remaining -= n
            }
            if (isLast) return total
        }
    }

    // --- Step 2: build the new metadata region. ---

    private fun buildId3v2Tag(metadata: EditableMetadata): ByteArray {
        val frames = ByteArrayOutputStream()
        writeTextFrame(frames, "TIT2", metadata.title)
        writeTextFrame(frames, "TPE1", metadata.artist)
        writeTextFrame(frames, "TALB", metadata.album)
        writeTextFrame(frames, "TPE2", metadata.albumArtist)
        writeTextFrame(frames, "TCON", metadata.genre)
        writeTextFrame(frames, "TYER", metadata.year)
        writeTextFrame(frames, "TRCK", metadata.trackNumber)
        writeTextFrame(frames, "TPOS", metadata.discNumber)
        metadata.newArtwork?.let { art -> writeApicFrame(frames, art) }

        val frameBytes = frames.toByteArray()
        val header = ByteArrayOutputStream()
        header.write('I'.code); header.write('D'.code); header.write('3'.code)
        header.write(3); header.write(0) // version 2.3.0
        header.write(0) // flags
        header.write(intToSynchsafe(frameBytes.size))
        return header.toByteArray() + frameBytes
    }

    private fun writeTextFrame(out: ByteArrayOutputStream, frameId: String, value: String) {
        if (value.isBlank()) return
        // Encoding 0x01 = UTF-16 with BOM - handles non-ASCII titles/artists correctly,
        // and is valid per the ID3v2.3 spec (unlike UTF-8, which is v2.4-only).
        val textBytes = ByteArrayOutputStream()
        textBytes.write(0xFF); textBytes.write(0xFE) // UTF-16LE BOM
        textBytes.write(value.toByteArray(Charsets.UTF_16LE))

        val frameData = byteArrayOf(1) + textBytes.toByteArray()
        out.write(frameId.toByteArray(Charsets.US_ASCII))
        out.write(intToBigEndian(frameData.size)) // ID3v2.3 frame sizes are NOT synchsafe
        out.write(0); out.write(0) // flags
        out.write(frameData)
    }

    private fun writeApicFrame(out: ByteArrayOutputStream, artwork: ByteArray) {
        val mime = "image/jpeg\u0000".toByteArray(Charsets.US_ASCII)
        val frameData = ByteArrayOutputStream()
        frameData.write(0) // encoding: ISO-8859-1 (description is empty, encoding barely matters)
        frameData.write(mime)
        frameData.write(3) // picture type: cover (front)
        frameData.write(0) // empty description, null-terminated
        frameData.write(artwork)

        val data = frameData.toByteArray()
        out.write("APIC".toByteArray(Charsets.US_ASCII))
        out.write(intToBigEndian(data.size))
        out.write(0); out.write(0)
        out.write(data)
    }

    private fun buildFlacHeader(originalUri: Uri, metadata: EditableMetadata): ByteArray? {
        val preserved = mutableListOf<ByteArray>() // other block types, kept byte-identical
        var existingPicture: ByteArray? = null
        var streamInfo: ByteArray? = null

        context.contentResolver.openInputStream(originalUri)?.use { raw ->
            val input = BufferedInputStream(raw)
            val magic = ByteArray(4)
            if (input.read(magic) != 4 || String(magic, Charsets.US_ASCII) != "fLaC") return null

            while (true) {
                val blockHeader = ByteArray(4)
                if (input.read(blockHeader) != 4) return null
                val isLast = (blockHeader[0].toInt() and 0x80) != 0
                val type = blockHeader[0].toInt() and 0x7F
                val length = (blockHeader[1].toInt() and 0xFF shl 16) or
                    (blockHeader[2].toInt() and 0xFF shl 8) or (blockHeader[3].toInt() and 0xFF)
                val body = ByteArray(length)
                var read = 0
                while (read < length) {
                    val n = input.read(body, read, length - read)
                    if (n <= 0) return null
                    read += n
                }

                when (type) {
                    0 -> streamInfo = blockHeader.copyOf().also { it[0] = 0 } + body // clear last-bit, fixed below
                    4 -> Unit // VORBIS_COMMENT - dropped, rebuilt fresh below
                    6 -> if (metadata.newArtwork == null) existingPicture = blockHeader.copyOf().also { it[0] = (it[0].toInt() and 0x7F).toByte() } + body
                    else -> preserved.add(blockHeader.copyOf().also { it[0] = (it[0].toInt() and 0x7F).toByte() } + body)
                }

                if (isLast) break
            }
        } ?: return null

        val streamInfoBlock = streamInfo ?: return null // STREAMINFO is mandatory; malformed file otherwise
        val vorbisCommentBlock = buildVorbisCommentBlock(metadata)
        val pictureBlock = metadata.newArtwork?.let { buildPictureBlock(it) } ?: existingPicture

        val blocks = mutableListOf(streamInfoBlock, vorbisCommentBlock)
        pictureBlock?.let { blocks.add(it) }
        blocks.addAll(preserved)

        // Mark only the final block's is-last bit.
        val result = ByteArrayOutputStream()
        result.write("fLaC".toByteArray(Charsets.US_ASCII))
        blocks.forEachIndexed { index, block ->
            val isLast = index == blocks.lastIndex
            val header = block.copyOfRange(0, 4)
            if (isLast) header[0] = (header[0].toInt() or 0x80).toByte()
            result.write(header)
            result.write(block, 4, block.size - 4)
        }
        return result.toByteArray()
    }

    private fun buildVorbisCommentBlock(metadata: EditableMetadata): ByteArray {
        val vendor = "MelodiQ".toByteArray(Charsets.UTF_8)
        val comments = mutableListOf<String>()
        if (metadata.title.isNotBlank()) comments.add("TITLE=${metadata.title}")
        if (metadata.artist.isNotBlank()) comments.add("ARTIST=${metadata.artist}")
        if (metadata.album.isNotBlank()) comments.add("ALBUM=${metadata.album}")
        if (metadata.albumArtist.isNotBlank()) comments.add("ALBUMARTIST=${metadata.albumArtist}")
        if (metadata.genre.isNotBlank()) comments.add("GENRE=${metadata.genre}")
        if (metadata.year.isNotBlank()) comments.add("DATE=${metadata.year}")
        if (metadata.trackNumber.isNotBlank()) comments.add("TRACKNUMBER=${metadata.trackNumber}")
        if (metadata.discNumber.isNotBlank()) comments.add("DISCNUMBER=${metadata.discNumber}")

        val body = ByteArrayOutputStream()
        writeLEInt(body, vendor.size); body.write(vendor)
        writeLEInt(body, comments.size)
        comments.forEach { comment ->
            val bytes = comment.toByteArray(Charsets.UTF_8)
            writeLEInt(body, bytes.size)
            body.write(bytes)
        }

        val bodyBytes = body.toByteArray()
        val header = byteArrayOf(
            4, // type 4 = VORBIS_COMMENT, last-bit cleared here (fixed up by the caller)
            (bodyBytes.size shr 16).toByte(),
            (bodyBytes.size shr 8).toByte(),
            bodyBytes.size.toByte(),
        )
        return header + bodyBytes
    }

    private fun buildPictureBlock(artwork: ByteArray): ByteArray {
        val mime = "image/jpeg".toByteArray(Charsets.UTF_8)
        val body = ByteArrayOutputStream()
        writeBEInt(body, 3) // picture type: cover (front)
        writeBEInt(body, mime.size); body.write(mime)
        writeBEInt(body, 0) // description length
        writeBEInt(body, 0); writeBEInt(body, 0) // width, height (unknown - decoders don't require it)
        writeBEInt(body, 0); writeBEInt(body, 0) // color depth, colors used
        writeBEInt(body, artwork.size)
        body.write(artwork)

        val bodyBytes = body.toByteArray()
        val header = byteArrayOf(
            6, // type 6 = PICTURE
            (bodyBytes.size shr 16).toByte(),
            (bodyBytes.size shr 8).toByte(),
            bodyBytes.size.toByte(),
        )
        return header + bodyBytes
    }

    private fun writeLEInt(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
        out.write((value shr 16) and 0xFF)
        out.write((value shr 24) and 0xFF)
    }

    private fun writeBEInt(out: ByteArrayOutputStream, value: Int) {
        out.write((value shr 24) and 0xFF)
        out.write((value shr 16) and 0xFF)
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun intToBigEndian(value: Int): ByteArray = byteArrayOf(
        (value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte()
    )

    private fun intToSynchsafe(value: Int): ByteArray = byteArrayOf(
        ((value shr 21) and 0x7F).toByte(),
        ((value shr 14) and 0x7F).toByte(),
        ((value shr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )

    private fun synchsafeToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        (b0.toInt() and 0x7F shl 21) or (b1.toInt() and 0x7F shl 14) or
            (b2.toInt() and 0x7F shl 7) or (b3.toInt() and 0x7F)
}
