package com.tasnimulhasan.domain.repository.local

import com.tasnimulhasan.entity.backup.RestoreSummary

interface BackupRepository {
    /** Builds the whole backup as a JSON string. */
    suspend fun createBackupJson(): String

    /** Merges a backup produced by [createBackupJson] into this device's data. Throws
     * IllegalArgumentException if the text is not a valid MelodiQ backup. */
    suspend fun restoreFromJson(json: String): RestoreSummary
}
