package com.weather.alarm.domain.user.type

enum class UserStatus {
    ACTIVE,     // 활성 사용자
    INACTIVE,   // 비활성 사용자 (알림 중지)
    DELETED     // 삭제된 사용자
}
