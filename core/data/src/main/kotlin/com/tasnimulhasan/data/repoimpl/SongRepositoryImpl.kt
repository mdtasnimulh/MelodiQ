package com.tasnimulhasan.data.repoimpl

import com.tasnimulhasan.common.Resource
import com.tasnimulhasan.data.dto.SongDto
import com.tasnimulhasan.domain.repository.SongRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SongRepositoryImpl @Inject constructor(
    private val musicRemoteDatabase: MusicRemoteDatabase,
) :
    SongRepository {
    override fun getSongs() =
        flow {
            val songs = musicRemoteDatabase.getAllSongs().await().toObjects<SongDto>()

            if (songs.isNotEmpty()) {
                emit(Resource.Success(songs.map { it.toSong() }))
            }

        }

}