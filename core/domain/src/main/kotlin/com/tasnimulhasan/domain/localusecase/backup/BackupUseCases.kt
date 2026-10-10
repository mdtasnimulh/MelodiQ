package com.tasnimulhasan.domain.localusecase.backup

import com.tasnimulhasan.domain.repository.local.BackupRepository
import com.tasnimulhasan.entity.backup.RestoreSummary
import javax.inject.Inject

class CreateBackupUseCase @Inject constructor(private val repo: BackupRepository) {
    suspend operator fun invoke(): String = repo.createBackupJson()
}

class RestoreBackupUseCase @Inject constructor(private val repo: BackupRepository) {
    suspend operator fun invoke(json: String): RestoreSummary = repo.restoreFromJson(json)
}
