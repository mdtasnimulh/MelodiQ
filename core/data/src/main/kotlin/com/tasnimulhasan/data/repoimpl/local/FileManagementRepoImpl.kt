package com.tasnimulhasan.data.repoimpl.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.tasnimulhasan.data.library.LibraryChangeNotifier
import com.tasnimulhasan.data.metadata.MediaStoreWriteAccess
import com.tasnimulhasan.domain.repository.local.FileManagementRepository
import com.tasnimulhasan.entity.metadata.FileOpResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FileManagementRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val writeAccess: MediaStoreWriteAccess,
    private val changeNotifier: LibraryChangeNotifier,
) : FileManagementRepository {

    private fun songUri(songId: Long) = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

    override suspend fun deleteSong(songId: Long): FileOpResult = withContext(Dispatchers.IO) {
        val uri = songUri(songId)
        if (writeAccess.tryDeleteDirect(uri)) {
            changeNotifier.notifyChanged()
            return@withContext FileOpResult.Success
        }

        when (val access = writeAccess.requestDeleteAccess(listOf(uri))) {
            is MediaStoreWriteAccess.Access.NeedsPermission -> FileOpResult.NeedsPermission(access.intentSender)
            MediaStoreWriteAccess.Access.Failed -> FileOpResult.Error("Could not delete file")
            MediaStoreWriteAccess.Access.Granted -> FileOpResult.Success
        }
    }

    override suspend fun renameSong(songId: Long, newTitleOnDisk: String): FileOpResult = withContext(Dispatchers.IO) {
        val uri = songUri(songId)
        val currentExtension = currentExtension(uri)
        val newDisplayName = if (currentExtension != null && !newTitleOnDisk.endsWith(".$currentExtension", ignoreCase = true)) {
            "$newTitleOnDisk.$currentExtension"
        } else {
            newTitleOnDisk
        }

        when (val access = writeAccess.tryRenameDirect(uri, newDisplayName)) {
            is MediaStoreWriteAccess.Access.NeedsPermission -> FileOpResult.NeedsPermission(access.intentSender)
            MediaStoreWriteAccess.Access.Failed -> FileOpResult.Error("Could not rename file")
            MediaStoreWriteAccess.Access.Granted -> {
                changeNotifier.notifyChanged()
                FileOpResult.Success
            }
        }
    }

    override suspend fun moveSong(songId: Long, newFolderRelativePath: String): FileOpResult = withContext(Dispatchers.IO) {
        val uri = songUri(songId)
        val normalized = if (newFolderRelativePath.endsWith("/")) newFolderRelativePath else "$newFolderRelativePath/"

        when (val access = writeAccess.tryMoveDirect(uri, normalized)) {
            is MediaStoreWriteAccess.Access.NeedsPermission -> FileOpResult.NeedsPermission(access.intentSender)
            MediaStoreWriteAccess.Access.Failed -> FileOpResult.Error("Could not move file")
            MediaStoreWriteAccess.Access.Granted -> {
                changeNotifier.notifyChanged()
                FileOpResult.Success
            }
        }
    }

    override suspend fun notifyLibraryChanged() {
        changeNotifier.notifyChanged()
    }

    override suspend fun getShareableUri(songId: Long): Uri = songUri(songId)

    private fun currentExtension(uri: Uri): String? = try {
        context.contentResolver.query(
            uri, arrayOf(MediaStore.Audio.Media.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                    ?.substringAfterLast('.', "")
                    ?.ifBlank { null }
            } else null
        }
    } catch (_: Exception) {
        null
    }
}
