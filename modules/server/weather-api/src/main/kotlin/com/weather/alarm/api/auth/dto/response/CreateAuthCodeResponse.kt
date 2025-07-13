package com.weather.alarm.api.auth.dto.response

data class CreateAuthCodeResponse(
    val message: String,
    val authSlackUrl: String,
    val authCodeExpiresAt: String
)
