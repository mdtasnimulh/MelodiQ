package com.tasnimulhasan.data.metadata

import android.content.Context
import android.net.Uri
import com.tasnimulhasan.entity.metadata.EditableMetadata
import com.tasnimulhasan.entity.metadata.TagWriteSupport
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import javax.inject.Inject

/** Companion to [AudioTagWriter] - reads the same tag formats it writes, so the editor
 * always opens pre-filled with what's genuinely in the file rather than MediaStore's index,
 * which for fields like album artist/genre/disc number is often incomplete or absent. */
class AudioTagReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tagWriter: AudioTagWriter,
) {
    fun read(uri: Uri, mimeType: String?): EditableMetadata? {
        return when (tagWriter.supportFor(mimeType)) {
            TagWriteSupport.MP3 -> readId3(uri)
            TagWriteSupport.FLAC -> readVorbisComment(uri)
            TagWriteSupport.UNSUPPORTED -> null
        }
    }

    private fun readId3(uri: Uri): EditableMetadata {
        val frames = mutableMapOf<String, String>()
        context.contentResolver.openInputStream(uri)?.use { raw ->
            val input = BufferedInputStream(raw)
            val header = ByteArray(10)
            if (input.read(header) == 10 &&
                header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()
            ) {
                val majorVersion = header[3].toInt()
                val tagSize = synchsafeToInt(header[6], header[7], header[8], header[9])
                val body = ByteArray(tagSize)
                var read = 0
                while (read < tagSize) {
                    val n = input.read(body, read, tagSize - read)
                    if (n <= 0) break
                    read += n
                }
                walkId3Frames(body, read, majorVersion, frames)
            }
        }
        return EditableMetadata(
            title = frames["TIT2"].orEmpty(),
            artist = frames["TPE1"].orEmpty(),
            album = frames["TALB"].orEmpty(),
            albumArtist = frames["TPE2"].orEmpty(),
            genre = frames["TCON"].orEmpty(),
            year = frames["TYER"].orEmpty(),
            trackNumber = frames["TRCK"].orEmpty(),
            discNumber = frames["TPOS"].orEmpty(),
        )
    }

    private fun walkId3Frames(tag: ByteArray, tagLength: Int, majorVersion: Int, out: MutableMap<String, String>) {
        var offset = 0
        val frameIdSize = if (majorVersion == 2) 3 else 4
        val frameHeaderSize = if (majorVersion == 2) 6 else 10
        val wanted = setOf("TIT2", "TPE1", "TALB", "TPE2", "TCON", "TYER", "TDRC", "TRCK", "TPOS")

        while (offset + frameHeaderSize <= tagLength) {
            val frameId = String(tag, offset, frameIdSize, Charsets.US_ASCII)
            if (frameId.isBlank() || frameId[0] == '\u0000') break

            val frameSize = if (majorVersion == 2) {
                (tag[offset + 3].toInt() and 0xFF shl 16) or (tag[offset + 4].toInt() and 0xFF shl 8) or (tag[offset + 5].toInt() and 0xFF)
            } else if (majorVersion == 4) {
                synchsafeToInt(tag[offset + 4], tag[offset + 5], tag[offset + 6], tag[offset + 7])
            } else {
                (tag[offset + 4].toInt() and 0xFF shl 24) or (tag[offset + 5].toInt() and 0xFF shl 16) or
                    (tag[offset + 6].toInt() and 0xFF shl 8) or (tag[offset + 7].toInt() and 0xFF)
            }

            val frameDataStart = offset + frameHeaderSize
            if (frameSize <= 0 || frameDataStart + frameSize > tagLength) break

            if (frameId in wanted) {
                decodeTextFrame(tag, frameDataStart, frameSize)?.let { value ->
                    // TDRC (v2.4 recording date) is the v2.4 equivalent of TYER - surface it
                    // under the same key so the year field is populated either way.
                    out[if (frameId == "TDRC") "TYER" else frameId] = value
                }
            }
            offset = frameDataStart + frameSize
        }
    }

    /** Text frame payload: [1 byte encoding][text, in that encoding]. */
    private fun decodeTextFrame(data: ByteArray, start: Int, length: Int): String? {
        if (length < 1) return null
        val encoding = data[start].toInt()
        val textStart = start + 1
        val end = start + length
        val raw = when (encoding) {
            0 -> String(data, textStart, end - textStart, Charsets.ISO_8859_1)
            3 -> String(data, textStart, end - textStart, Charsets.UTF_8)
            1, 2 -> String(data, textStart, end - textStart, Charsets.UTF_16)
            else -> return null
        }
        // Text frames can contain trailing null padding and, for TCON, an ID3v1-style
        // "(13)" genre code prefix - strip both for a clean display value.
        return raw.trim('\u0000', ' ').trim()
    }

    private fun readVorbisComment(uri: Uri): EditableMetadata {
        val comments = mutableMapOf<String, String>()
        context.contentResolver.openInputStream(uri)?.use { raw ->
            val input = BufferedInputStream(raw)
            val magic = ByteArray(4)
            if (input.read(magic) == 4 && String(magic, Charsets.US_ASCII) == "fLaC") {
                while (true) {
                    val blockHeader = ByteArray(4)
                    if (input.read(blockHeader) != 4) break
                    val isLast = (blockHeader[0].toInt() and 0x80) != 0
                    val type = blockHeader[0].toInt() and 0x7F
                    val length = (blockHeader[1].toInt() and 0xFF shl 16) or
                        (blockHeader[2].toInt() and 0xFF shl 8) or (blockHeader[3].toInt() and 0xFF)
                    val body = ByteArray(length)
                    var read = 0
                    while (read < length) {
                        val n = input.read(body, read, length - read)
                        if (n <= 0) break
                        read += n
                    }
                    if (type == 4) parseVorbisCommentBody(body, comments)
                    if (isLast) break
                }
            }
        }
        return EditableMetadata(
            title = comments["TITLE"].orEmpty(),
            artist = comments["ARTIST"].orEmpty(),
            album = comments["ALBUM"].orEmpty(),
            albumArtist = comments["ALBUMARTIST"].orEmpty(),
            genre = comments["GENRE"].orEmpty(),
            year = comments["DATE"].orEmpty(),
            trackNumber = comments["TRACKNUMBER"].orEmpty(),
            discNumber = comments["DISCNUMBER"].orEmpty(),
        )
    }

    private fun parseVorbisCommentBody(block: ByteArray, out: MutableMap<String, String>) {
        var pos = 0
        fun readLEInt(): Int {
            if (pos + 4 > block.size) return -1
            val v = (block[pos].toInt() and 0xFF) or (block[pos + 1].toInt() and 0xFF shl 8) or
                (block[pos + 2].toInt() and 0xFF shl 16) or (block[pos + 3].toInt() and 0xFF shl 24)
            pos += 4
            return v
        }
        val vendorLength = readLEInt()
        if (vendorLength < 0) return
        pos += vendorLength
        val commentCount = readLEInt()
        if (commentCount < 0) return
        repeat(commentCount) {
            val len = readLEInt()
            if (len < 0 || pos + len > block.size) return
            val comment = String(block, pos, len, Charsets.UTF_8)
            pos += len
            val eq = comment.indexOf('=')
            if (eq > 0) out[comment.substring(0, eq).uppercase()] = comment.substring(eq + 1)
        }
    }

    private fun synchsafeToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        (b0.toInt() and 0x7F shl 21) or (b1.toInt() and 0x7F shl 14) or
            (b2.toInt() and 0x7F shl 7) or (b3.toInt() and 0x7F)
}
