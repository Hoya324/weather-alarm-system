package com.weather.alarm.domain.notification.dto.request

import com.weather.alarm.domain.notification.type.NotificationType
import java.time.LocalTime

data class CreateNotificationInfoDomainRequest(
    val userId: Long,
    val slackWebHookUrl: String,
    val address: String,
    val notificationTime: LocalTime,
    val notificationType: NotificationType,
    val weatherTypes: List<String>? = null,
    val temperatureThreshold: Int? = null,
    val notificationEnabled: Boolean = true
)
