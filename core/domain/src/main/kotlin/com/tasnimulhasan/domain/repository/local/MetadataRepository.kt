package com.tasnimulhasan.domain.repository.local

import com.tasnimulhasan.entity.metadata.AudioFileInfo
import com.tasnimulhasan.entity.metadata.EditableMetadata
import com.tasnimulhasan.entity.metadata.FileOpResult
import com.tasnimulhasan.entity.metadata.MetadataEditResult
import com.tasnimulhasan.entity.metadata.TagWriteSupport

interface MetadataRepository {
    suspend fun readCurrentMetadata(songId: Long): EditableMetadata?
    suspend fun readFileInfo(songId: Long): AudioFileInfo?
    suspend fun writeSupportFor(songId: Long): TagWriteSupport
    suspend fun saveMetadata(songId: Long, metadata: EditableMetadata): MetadataEditResult
}

interface FileManagementRepository {
    suspend fun deleteSong(songId: Long): FileOpResult
    suspend fun renameSong(songId: Long, newTitleOnDisk: String): FileOpResult
    suspend fun moveSong(songId: Long, newFolderRelativePath: String): FileOpResult
    /** A content:// Uri suitable for ACTION_SEND / opening in another app. */
    suspend fun getShareableUri(songId: Long): android.net.Uri?
}
