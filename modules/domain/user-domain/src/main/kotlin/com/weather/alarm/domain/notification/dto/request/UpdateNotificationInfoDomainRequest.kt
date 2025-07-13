package com.weather.alarm.domain.notification.dto.request

import com.weather.alarm.domain.notification.type.NotificationType
import java.time.LocalTime

data class UpdateNotificationInfoDomainRequest(
    val slackWebHookUrl: String? = null,
    val address: String? = null,
    val notificationTime: LocalTime? = null,
    val notificationType: NotificationType? = null,
    val weatherTypes: List<String>? = null,
    val temperatureThreshold: Int? = null,
    val notificationEnabled: Boolean? = null
)
