package com.weather.alarm.api.auth.dto.response

data class VerifyAuthCodeResponse(
    val success: Boolean,
    val message: String,
    val userId: Long? = null
)
