package com.weather.alarm.api.auth.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginWithCodeRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요")
    val name: String,

    @field:NotBlank(message = "인증 코드는 필수입니다")
    @field:Size(min = 6, max = 10, message = "인증 코드는 6자 이상 10자 이하입니다")
    val authCode: String
)
