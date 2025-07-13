package com.weather.alarm.api.auth.dto.response

data class LoginWithCodeResponse(
    val success: Boolean,
    val message: String,
    val userId: Long? = null,
    val userName: String? = null,
    val authSlackUrl: String? = null,
    val isVerified: Boolean = false
) {
    companion object {
        fun success(userId: Long, userName: String, authSlackUrl: String, isVerified: Boolean): LoginWithCodeResponse {
            return LoginWithCodeResponse(
                success = true,
                message = if (isVerified) "로그인 성공! 환영합니다." else "로그인 성공! 인증을 완료해주세요.",
                userId = userId,
                userName = userName,
                authSlackUrl = authSlackUrl,
                isVerified = isVerified
            )
        }

        fun failure(message: String): LoginWithCodeResponse {
            return LoginWithCodeResponse(
                success = false,
                message = message
            )
        }
    }
}
