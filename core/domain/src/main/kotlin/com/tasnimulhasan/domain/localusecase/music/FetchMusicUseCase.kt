package com.tasnimulhasan.domain.localusecase.music

import android.content.Context
import com.tasnimulhasan.domain.localusecase.RoomCollectableUseCase
import com.tasnimulhasan.domain.localusecase.RoomCollectableUseCaseNoParams
import com.tasnimulhasan.domain.localusecase.RoomSuspendableUseCase
import com.tasnimulhasan.domain.repository.MusicRepository
import com.tasnimulhasan.domain.usecase.RoomUseCaseNonParams
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchMusicUseCase @Inject constructor(
    private val repository: MusicRepository,
    private val context: Context,
) : RoomSuspendableUseCase<SortType, List<MusicEntity>> {

    override suspend fun invoke(params: SortType): List<MusicEntity> {
        return repository.fetchMusic(context, params)
    }
}
