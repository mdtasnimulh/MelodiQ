package com.tasnimulhasan.domain.base

import android.text.TextUtils
import android.util.Patterns
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject

class IoValidation @Inject constructor() {
    /*fun loginDataValidation(params: LoginApiUseCase.Params): DataValidationResult {
        if (params.username.isEmpty() || params.username.length < 5)
            return DataValidationResult.Failure(LoginIoResult.InvalidUsername)

        if (!params.password.isPasswordValid())
            return DataValidationResult.Failure(LoginIoResult.InvalidPassword)

        return DataValidationResult.Success
    }*/
}

fun String.isPhoneNumberValid(): Boolean {
    val pattern: Pattern =
        Pattern.compile("((0|01|\\+88|\\+88\\s*\\(0\\)|\\+88\\s*0)\\s*)?1(\\s*[0-9]){9}")
    val matcher: Matcher = pattern.matcher(this)
    return matcher.matches()
}

fun String.isPasswordValid(): Boolean {
    return this.isNotEmpty() && this.length >= 6
}

fun String.isNameValid(): Boolean {
    return this.isNotEmpty() && this.length >= 5
}

fun String.isEmailValid(): Boolean {
    return !TextUtils.isEmpty(this) && Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.convertToInt(): Int {
    return try {
        this.toInt()
    } catch (ex: Exception) {
        0
    }
}

fun String.convertToDouble(): Double {
    return try {
        this.toDouble()
    } catch (ex: Exception) {
        0.0
    }
}