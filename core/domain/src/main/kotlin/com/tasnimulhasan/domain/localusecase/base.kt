package com.tasnimulhasan.domain.localusecase

import com.tasnimulhasan.domain.base.BaseUseCase
import kotlinx.coroutines.flow.Flow

interface RoomSuspendableUseCase<Params, ReturnType> : BaseUseCase {
    suspend operator fun invoke(params: Params) : ReturnType
}

interface RoomSuspendableUseCaseNonParams<ReturnType> : BaseUseCase {
    suspend operator fun invoke() : ReturnType
}

interface RoomSuspendableUseCaseNonReturn<Params> : BaseUseCase {
    suspend operator fun invoke(params: Params)
}

interface RoomCollectableUseCase<Params, ReturnType> : BaseUseCase {
    operator fun invoke(params: Params): Flow<ReturnType>
}

interface RoomCollectableUseCaseNoParams< ReturnType> : BaseUseCase {
    operator fun invoke(): Flow<ReturnType>
}