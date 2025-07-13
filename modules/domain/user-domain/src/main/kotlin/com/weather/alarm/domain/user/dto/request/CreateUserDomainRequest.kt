package com.weather.alarm.domain.user.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateUserDomainRequest(
    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    val name: String,

    @NotBlank(message = "Slack 웹훅 URL은 필수입니다.")
    @Pattern(
        regexp = "^https://hooks\\.slack\\.com/services/.*",
        message = "올바른 Slack 웹훅 URL 형식이 아닙니다."
    )
    val authSlackUrl: String
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (name.isBlank()) {
            errors.add("이름은 필수입니다.")
        }

        if (name.length < 2 || name.length > 50) {
            errors.add("이름은 2자 이상 50자 이하여야 합니다.")
        }

        if (authSlackUrl.isBlank()) {
            errors.add("Slack 웹훅 URL은 필수입니다.")
        }

        if (!authSlackUrl.matches(Regex("^https://hooks\\.slack\\.com/services/.*"))) {
            errors.add("올바른 Slack 웹훅 URL 형식이 아닙니다.")
        }

        return errors
    }
}