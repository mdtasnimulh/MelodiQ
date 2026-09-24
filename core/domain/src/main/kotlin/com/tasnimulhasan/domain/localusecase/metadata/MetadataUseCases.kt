package com.tasnimulhasan.domain.localusecase.metadata

import com.tasnimulhasan.domain.repository.local.FileManagementRepository
import com.tasnimulhasan.domain.repository.local.MetadataRepository
import com.tasnimulhasan.entity.metadata.AudioFileInfo
import com.tasnimulhasan.entity.metadata.EditableMetadata
import com.tasnimulhasan.entity.metadata.FileOpResult
import com.tasnimulhasan.entity.metadata.MetadataEditResult
import com.tasnimulhasan.entity.metadata.TagWriteSupport
import javax.inject.Inject

class ReadCurrentMetadataUseCase @Inject constructor(private val repo: MetadataRepository) {
    suspend operator fun invoke(songId: Long): EditableMetadata? = repo.readCurrentMetadata(songId)
}

class ReadFileInfoUseCase @Inject constructor(private val repo: MetadataRepository) {
    suspend operator fun invoke(songId: Long): AudioFileInfo? = repo.readFileInfo(songId)
}

class GetWriteSupportUseCase @Inject constructor(private val repo: MetadataRepository) {
    suspend operator fun invoke(songId: Long): TagWriteSupport = repo.writeSupportFor(songId)
}

class SaveMetadataUseCase @Inject constructor(private val repo: MetadataRepository) {
    suspend operator fun invoke(songId: Long, metadata: EditableMetadata): MetadataEditResult =
        repo.saveMetadata(songId, metadata)
}

class DeleteSongUseCase @Inject constructor(private val repo: FileManagementRepository) {
    suspend operator fun invoke(songId: Long): FileOpResult = repo.deleteSong(songId)
}

class RenameSongUseCase @Inject constructor(private val repo: FileManagementRepository) {
    suspend operator fun invoke(songId: Long, newTitleOnDisk: String): FileOpResult = repo.renameSong(songId, newTitleOnDisk)
}

class MoveSongUseCase @Inject constructor(private val repo: FileManagementRepository) {
    suspend operator fun invoke(songId: Long, newFolderRelativePath: String): FileOpResult =
        repo.moveSong(songId, newFolderRelativePath)
}

class GetShareableUriUseCase @Inject constructor(private val repo: FileManagementRepository) {
    suspend operator fun invoke(songId: Long): android.net.Uri? = repo.getShareableUri(songId)
}
