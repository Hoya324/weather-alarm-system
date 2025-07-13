package com.weather.alarm.api.auth.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateAuthCodeRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(max = 100, message = "이름은 100자 이하여야 합니다")
    val name: String,

    @field:NotBlank(message = "Slack URL은 필수입니다")
    val authSlackUrl: String
)
