package com.tasnimulhasan.domain.base

sealed class DataValidationResult{
    data object Success : DataValidationResult()
    data class Failure<T>(val ioErrorResult: T) : DataValidationResult()
}

sealed interface LoginIoResult{
    data object InvalidUsername : LoginIoResult
    data object InvalidPassword : LoginIoResult
}