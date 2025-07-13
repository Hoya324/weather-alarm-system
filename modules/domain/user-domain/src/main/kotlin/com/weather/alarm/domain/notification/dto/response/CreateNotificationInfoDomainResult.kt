package com.weather.alarm.domain.notification.dto.response

data class CreateNotificationInfoDomainResult(
    val notificationInfo: NotificationInfoDomainResponse,
    val hasCoordinate: Boolean,
    val message: String
)
