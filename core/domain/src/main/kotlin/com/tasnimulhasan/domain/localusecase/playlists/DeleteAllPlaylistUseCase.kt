package com.tasnimulhasan.domain.localusecase.playlists

import com.tasnimulhasan.domain.repository.local.PlaylistsRepository
import javax.inject.Inject

class DeleteAllPlaylistUseCase @Inject constructor(
    private val repository: PlaylistsRepository
) {
    suspend operator fun invoke() {
        return repository.deleteAllPlaylist()
    }
}