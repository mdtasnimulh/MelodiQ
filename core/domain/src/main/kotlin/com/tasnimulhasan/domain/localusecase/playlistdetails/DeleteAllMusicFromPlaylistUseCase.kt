package com.tasnimulhasan.domain.localusecase.playlistdetails

import com.tasnimulhasan.domain.repository.local.PlaylistsRepository
import javax.inject.Inject

class DeleteAllMusicFromPlaylistUseCase @Inject constructor(
    private val repository: PlaylistsRepository
) {
    suspend operator fun invoke() {
        return repository.deleteAllPlaylist()
    }
}