package com.weather.alarm.api.auth.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class VerifyAuthCodeRequest(
    @field:NotBlank(message = "Slack URL은 필수입니다")
    val authSlackUrl: String,

    @field:NotBlank(message = "인증 코드는 필수입니다")
    @field:Size(min = 6, max = 10, message = "인증 코드는 6-10자리여야 합니다")
    val authCode: String
)
