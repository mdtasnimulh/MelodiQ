package com.tasnimulhasan.data.lyrics

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Finds a `.lrc` file with the same base filename as the song, in the same folder - the
 * de facto convention every local-lyrics tool follows (`song.mp3` + `song.lrc`).
 *
 * Read access to an arbitrary sibling file isn't possible via a raw filesystem path under
 * scoped storage unless the app owns it, so this queries MediaStore's generic Files
 * collection (which indexes non-media files too) for a matching entry and reads it through
 * the content:// Uri MediaStore hands back - the scoped-storage-correct way to do this.
 */
class LrcFileLocator @Inject constructor(@ApplicationContext private val context: Context) {

    /** Looks up the song's own MediaStore row for its true DISPLAY_NAME and RELATIVE_PATH
     * (rather than trusting the cached title/folder, which can differ from the actual
     * filename - e.g. a TITLE tag that doesn't match the file on disk), then looks for a
     * same-named .lrc file next to it. */
    fun findAndRead(songId: Long): String? {
        val audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val (displayName, relativePath) = try {
            context.contentResolver.query(
                audioUri,
                arrayOf(MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.RELATIVE_PATH),
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(songId.toString()),
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)) ?: return null
                val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)) ?: return null
                name to path
            } ?: return null
        } catch (_: Exception) {
            return null
        }

        val baseName = displayName.substringBeforeLast('.', displayName)
        val lrcFileName = "$baseName.lrc"

        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
        val args = arrayOf(relativePath, lrcFileName)
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val fileId = try {
            context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)) else null
            }
        } catch (_: Exception) {
            null
        } ?: return null

        val fileUri = ContentUris.withAppendedId(collection, fileId)

        return try {
            context.contentResolver.openInputStream(fileUri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (_: Exception) {
            null
        }
    }
}
