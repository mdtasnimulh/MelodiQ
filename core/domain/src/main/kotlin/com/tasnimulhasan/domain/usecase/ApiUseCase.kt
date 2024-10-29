package com.tasnimulhasan.domain.usecase

import com.tasnimulhasan.domain.base.ApiResult
import com.tasnimulhasan.domain.base.BaseUseCase
import kotlinx.coroutines.flow.Flow

interface ApiUseCaseParams<Params, Type> : BaseUseCase {
    suspend fun execute(params: Params): Flow<ApiResult<Type>>
}

interface ApiUseCaseNonParams<Type> : BaseUseCase {
    suspend fun execute(): Flow<ApiResult<Type>>
}