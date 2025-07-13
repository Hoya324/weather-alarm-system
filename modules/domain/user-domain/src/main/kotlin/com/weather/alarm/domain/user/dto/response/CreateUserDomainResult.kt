package com.weather.alarm.domain.user.dto.response

import com.weather.alarm.domain.user.entity.User

/**
 * 사용자 생성 결과
 */
data class CreateUserDomainResult(
    val user: User,
    val authCode: String
) {
    val userId: Long = user.id
    val name: String = user.name
    val authSlackUrl: String = user.authSlackUrl
    val verified: Boolean = user.verified
    val isSuccess: Boolean = true
}
