package com.tasnimulhasan.domain.localusecase.music

import android.content.Context
import com.tasnimulhasan.domain.repository.MusicRepository
import javax.inject.Inject

class SyncRoomMusicUseCase @Inject constructor(
    private val repository: MusicRepository,
    private val context: Context
) {
    suspend fun invoke() {
        repository.insertMusicToRoom(context = context)
    }
}
