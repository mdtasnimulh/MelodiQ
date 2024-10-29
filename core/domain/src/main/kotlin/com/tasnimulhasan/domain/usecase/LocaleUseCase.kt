package com.tasnimulhasan.domain.usecase

import com.tasnimulhasan.domain.base.BaseUseCase

interface CoroutineBaseUseCase<Params, Type>: BaseUseCase {
    suspend fun execute(params: Params):Type
}

interface RoomUseCaseNonParams<Type> : BaseUseCase {
    suspend fun execute(): Type
}