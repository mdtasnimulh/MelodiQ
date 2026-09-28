package com.tasnimulhasan.data.repoimpl.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.tasnimulhasan.data.library.LibraryChangeNotifier
import com.tasnimulhasan.data.metadata.AudioFileInfoReader
import com.tasnimulhasan.data.metadata.AudioTagReader
import com.tasnimulhasan.data.metadata.AudioTagWriter
import com.tasnimulhasan.data.metadata.MediaStoreWriteAccess
import com.tasnimulhasan.domain.repository.local.MetadataRepository
import com.tasnimulhasan.entity.metadata.AudioFileInfo
import com.tasnimulhasan.entity.metadata.EditableMetadata
import com.tasnimulhasan.entity.metadata.MetadataEditResult
import com.tasnimulhasan.entity.metadata.TagWriteSupport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MetadataRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tagReader: AudioTagReader,
    private val tagWriter: AudioTagWriter,
    private val fileInfoReader: AudioFileInfoReader,
    private val writeAccess: MediaStoreWriteAccess,
    private val changeNotifier: LibraryChangeNotifier,
) : MetadataRepository {

    private fun songUri(songId: Long) = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

    private data class SongRow(val displayName: String, val mimeType: String?, val size: Long, val duration: Long)

    private fun querySong(songId: Long): SongRow? =
        context.contentResolver.query(
            songUri(songId),
            arrayOf(
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DURATION,
            ),
            null, null, null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            SongRow(
                displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)) ?: "",
                mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)),
                size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)),
                duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
            )
        }

    override suspend fun readCurrentMetadata(songId: Long): EditableMetadata? = withContext(Dispatchers.IO) {
        val row = querySong(songId) ?: return@withContext null
        tagReader.read(songUri(songId), row.mimeType)
    }

    override suspend fun readFileInfo(songId: Long): AudioFileInfo? = withContext(Dispatchers.IO) {
        val row = querySong(songId) ?: return@withContext null
        fileInfoReader.read(songUri(songId), row.mimeType, row.size, row.duration, row.displayName)
    }

    override suspend fun writeSupportFor(songId: Long): TagWriteSupport = withContext(Dispatchers.IO) {
        val row = querySong(songId) ?: return@withContext TagWriteSupport.UNSUPPORTED
        tagWriter.supportFor(row.mimeType)
    }

    override suspend fun saveMetadata(songId: Long, metadata: EditableMetadata): MetadataEditResult = withContext(Dispatchers.IO) {
        val row = querySong(songId) ?: return@withContext MetadataEditResult.Error("Song not found")
        val uri = songUri(songId)

        if (tagWriter.supportFor(row.mimeType) == TagWriteSupport.UNSUPPORTED) {
            return@withContext MetadataEditResult.UnsupportedFormat
        }

        // Probe permission WITHOUT opening for write - the truncating open happens inside
        // the tag writer, only after the original file has been fully backed up.
        when (val access = writeAccess.checkWriteAccess(uri)) {
            is MediaStoreWriteAccess.Access.NeedsPermission ->
                return@withContext MetadataEditResult.NeedsPermission(access.intentSender)
            MediaStoreWriteAccess.Access.Failed ->
                return@withContext MetadataEditResult.Error("Could not open file for writing")
            MediaStoreWriteAccess.Access.Granted -> Unit
        }

        val success = tagWriter.write(uri, row.mimeType, metadata) {
            context.contentResolver.openOutputStream(uri, "rwt")
        }

        // No manual rescan call here: writing through a MediaStore-granted Uri (as opposed
        // to a raw file path) is exactly the case the ContentObserver registered by
        // MediaStoreLibraryScanner (Tranche 2) already watches for - closing this stream
        // triggers MediaProvider's own change notification, which that observer picks up
        // and debounces into a rescan a couple of seconds later, refreshing the library
        // cache with the new tags automatically.

        if (success) {
            changeNotifier.notifyChanged()
            MetadataEditResult.Success
        } else {
            MetadataEditResult.Error("Failed to write tags")
        }
    }
}
